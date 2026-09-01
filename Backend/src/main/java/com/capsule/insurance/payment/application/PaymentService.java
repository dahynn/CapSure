package com.capsule.insurance.payment.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.payment.application.port.PaymentRepository;
import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.ApprovedApplication;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.capsule.insurance.payment.domain.PaymentAttempt;
import com.capsule.insurance.payment.domain.PaymentOrder;
import com.capsule.insurance.payment.dto.ConfirmPaymentRequest;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import com.capsule.insurance.policy.application.port.PolicyRepository;
import com.capsule.insurance.policy.domain.InsurancePolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class PaymentService {

    private static final Duration ORDER_VALIDITY = Duration.ofMinutes(30);
    private static final String PROVIDER = "FAKE";

    private final PaymentRepository paymentRepository;
    private final PolicyRepository policyRepository;
    private final PremiumPaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PaymentService(
            PaymentRepository paymentRepository,
            PolicyRepository policyRepository,
            PremiumPaymentGateway paymentGateway,
            PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper
    ) {
        this(
                paymentRepository,
                policyRepository,
                paymentGateway,
                transactionManager,
                objectMapper,
                Clock.systemUTC()
        );
    }

    PaymentService(
            PaymentRepository paymentRepository,
            PolicyRepository policyRepository,
            PremiumPaymentGateway paymentGateway,
            PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.policyRepository = policyRepository;
        this.paymentGateway = paymentGateway;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public PaymentOrderResponse createInitialPremiumOrder(
            Long userId,
            Long applicationId,
            String idempotencyKey
    ) {
        validateIdempotencyKey(idempotencyKey);
        PaymentOrder order = Objects.requireNonNull(transactionTemplate.execute(status -> {
            ApprovedApplication application = paymentRepository
                    .lockOwnedApplication(applicationId, userId)
                    .orElseThrow(() -> notFound("결제 주문을 만들 청약을 찾을 수 없습니다."));

            PaymentOrder sameRequest = paymentRepository
                    .findByCreationIdempotencyKey(idempotencyKey)
                    .orElse(null);
            if (sameRequest != null) {
                if (sameRequest.applicationId().equals(applicationId)
                        && sameRequest.ownerUserId().equals(userId)) {
                    return sameRequest;
                }
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "다른 초회 보험료 주문에 사용된 Idempotency-Key입니다."
                );
            }

            String businessKey = "INITIAL_PREMIUM:" + applicationId;
            PaymentOrder existing = paymentRepository.findByBusinessKey(businessKey).orElse(null);
            if (existing != null) {
                return existing;
            }
            if (!"APPROVED".equals(application.applicationStatus())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "인수 승인된 청약만 초회 보험료 주문을 만들 수 있습니다."
                );
            }

            InsurancePolicy policy = policyRepository.createPending(
                    "P-" + UUID.randomUUID(),
                    application.applicationId(),
                    application.applicantUserId(),
                    application.insuredUserId(),
                    application.insuredUserId()
            );
            return paymentRepository.createOrder(
                    "PAY-" + UUID.randomUUID(),
                    businessKey,
                    application.applicationId(),
                    policy.policyId(),
                    application.initialPremium(),
                    application.currencyCode(),
                    idempotencyKey,
                    Instant.now(clock).plus(ORDER_VALIDITY)
            );
        }));
        return toResponse(order);
    }

    public PaymentOrderResponse confirm(
            Long userId,
            Long paymentOrderId,
            String idempotencyKey,
            ConfirmPaymentRequest request
    ) {
        validateIdempotencyKey(idempotencyKey);
        ConfirmationReservation reservation = Objects.requireNonNull(transactionTemplate.execute(status -> {
            PaymentOrder order = paymentRepository.lockOwned(paymentOrderId, userId)
                    .orElseThrow(() -> notFound("결제 주문을 찾을 수 없습니다."));
            if (order.amount().compareTo(request.amount()) != 0) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "결제 승인 금액이 서버 주문 금액과 다릅니다."
                );
            }

            PaymentAttempt sameRequest = paymentRepository
                    .findAttemptByIdempotencyKey(idempotencyKey)
                    .orElse(null);
            if (sameRequest != null) {
                if (!sameRequest.paymentOrderId().equals(paymentOrderId)) {
                    throw new BusinessException(
                            ErrorCode.IDEMPOTENCY_CONFLICT,
                            "다른 결제 주문에 사용된 Idempotency-Key입니다."
                    );
                }
                return new ConfirmationReservation(order, sameRequest, false);
            }
            if (!"CREATED".equals(order.status())) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "CREATED 결제 주문만 새 승인 시도를 시작할 수 있습니다."
                );
            }
            if (order.expiresAt() != null && !order.expiresAt().isAfter(Instant.now(clock))) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "만료된 초회 보험료 주문입니다."
                );
            }
            PaymentAttempt providerKeyOwner = paymentRepository
                    .findAttemptByProviderPaymentKey(PROVIDER, request.providerPaymentKey())
                    .orElse(null);
            if (providerKeyOwner != null) {
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "이미 다른 승인 시도에 사용된 provider payment key입니다."
                );
            }

            PremiumPaymentGateway.ConfirmCommand command = new PremiumPaymentGateway.ConfirmCommand(
                    order.orderNo(),
                    request.providerPaymentKey(),
                    order.amount(),
                    order.currencyCode(),
                    idempotencyKey
            );
            PaymentAttempt attempt = paymentRepository.createProcessingAttempt(
                    order.paymentOrderId(),
                    PROVIDER,
                    request.providerPaymentKey(),
                    idempotencyKey,
                    toJson(command)
            );
            paymentRepository.markApproving(order.paymentOrderId());
            return new ConfirmationReservation(order, attempt, true);
        }));

        if (!reservation.requiresGatewayCall()) {
            return get(userId, paymentOrderId);
        }

        GatewayPaymentResult result = paymentGateway.confirm(new PremiumPaymentGateway.ConfirmCommand(
                reservation.order().orderNo(),
                request.providerPaymentKey(),
                reservation.order().amount(),
                reservation.order().currencyCode(),
                idempotencyKey
        ));
        PaymentOrder completed = finalizeResult(
                reservation.order().paymentOrderId(),
                reservation.attempt().paymentAttemptId(),
                result,
                null
        );
        return toResponse(completed);
    }

    public PaymentOrderResponse reconcile(Long paymentOrderId) {
        ReconciliationTarget target = Objects.requireNonNull(transactionTemplate.execute(status -> {
            PaymentOrder order = paymentRepository.lockById(paymentOrderId)
                    .orElseThrow(() -> notFound("대사할 결제 주문을 찾을 수 없습니다."));
            if (!List.of("APPROVING", "UNKNOWN").contains(order.status())) {
                return new ReconciliationTarget(order, null);
            }
            PaymentAttempt attempt = paymentRepository.findLatestAttempt(paymentOrderId)
                    .orElseThrow(() -> new IllegalStateException("대상 결제 시도가 없습니다."));
            return new ReconciliationTarget(order, attempt);
        }));
        if (target.attempt() == null) {
            return toResponse(target.order());
        }
        GatewayPaymentResult result = paymentGateway.inquire(target.attempt().providerPaymentKey());
        String reconciliationResult = "UNKNOWN".equals(result.status())
                ? "STILL_UNKNOWN"
                : (target.order().status().equals(result.status()) ? "MATCHED" : "CORRECTED");
        PaymentOrder completed = finalizeResult(
                target.order().paymentOrderId(),
                target.attempt().paymentAttemptId(),
                result,
                new ReconciliationRecord(
                        target.order().status(),
                        result.status(),
                        reconciliationResult,
                        toJson(result)
                )
        );
        return toResponse(completed);
    }

    public PaymentOrderResponse applyProviderNotification(
            String providerPaymentKey,
            GatewayPaymentResult result
    ) {
        PaymentAttempt attempt = paymentRepository
                .findAttemptByProviderPaymentKey(PROVIDER, providerPaymentKey)
                .orElseThrow(() -> notFound("webhook 대상 결제 시도를 찾을 수 없습니다."));
        PaymentOrder completed = finalizeResult(
                attempt.paymentOrderId(),
                attempt.paymentAttemptId(),
                result,
                null
        );
        return toResponse(completed);
    }

    public PaymentOrderResponse get(Long userId, Long paymentOrderId) {
        return paymentRepository.findOwned(paymentOrderId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> notFound("결제 주문을 찾을 수 없습니다."));
    }

    private PaymentOrder finalizeResult(
            Long paymentOrderId,
            Long paymentAttemptId,
            GatewayPaymentResult result,
            ReconciliationRecord reconciliation
    ) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            PaymentOrder locked = paymentRepository.lockById(paymentOrderId)
                    .orElseThrow(() -> notFound("결제 주문을 찾을 수 없습니다."));
            if ("PAID".equals(locked.status())) {
                return locked;
            }
            if (!List.of("APPROVING", "UNKNOWN").contains(locked.status())) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "승인 중이거나 미확정인 주문만 결제 결과를 확정할 수 있습니다."
                );
            }
            paymentRepository.completeAttempt(paymentAttemptId, result, toJson(result));
            PaymentOrder completed = paymentRepository.completeOrder(paymentOrderId, result);
            if ("PAID".equals(result.status())) {
                policyRepository.activateFromPaidOrder(paymentOrderId);
            }
            if (reconciliation != null) {
                paymentRepository.recordReconciliation(
                        paymentOrderId,
                        PROVIDER,
                        reconciliation.localStatus(),
                        reconciliation.providerStatus(),
                        reconciliation.result(),
                        reconciliation.detailsJson()
                );
            }
            return completed;
        }));
    }

    private PaymentOrderResponse toResponse(PaymentOrder order) {
        List<PaymentOrderResponse.AttemptResponse> attempts = paymentRepository
                .findAttempts(order.paymentOrderId())
                .stream()
                .map(this::toAttemptResponse)
                .toList();
        String policyStatus = policyRepository.findById(order.policyId())
                .map(InsurancePolicy::status)
                .orElse(null);
        return new PaymentOrderResponse(
                order.paymentOrderId(),
                order.orderNo(),
                order.applicationId(),
                order.policyId(),
                order.purpose(),
                order.amount(),
                order.currencyCode(),
                order.status(),
                attempts,
                policyStatus,
                order.expiresAt(),
                order.paidAt(),
                order.createdAt(),
                order.updatedAt()
        );
    }

    private PaymentOrderResponse.AttemptResponse toAttemptResponse(PaymentAttempt attempt) {
        return new PaymentOrderResponse.AttemptResponse(
                attempt.paymentAttemptId(),
                attempt.attemptNo(),
                attempt.provider(),
                attempt.providerPaymentKey(),
                attempt.status(),
                attempt.errorCode(),
                attempt.requestedAt(),
                attempt.completedAt()
        );
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Idempotency-Key가 필요합니다.");
        }
        if (idempotencyKey.length() > 150) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Idempotency-Key는 150자 이하여야 합니다.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("결제 원장 JSON을 직렬화하지 못했습니다.", exception);
        }
    }

    private BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    private record ConfirmationReservation(
            PaymentOrder order,
            PaymentAttempt attempt,
            boolean requiresGatewayCall
    ) {
    }

    private record ReconciliationTarget(PaymentOrder order, PaymentAttempt attempt) {
    }

    private record ReconciliationRecord(
            String localStatus,
            String providerStatus,
            String result,
            String detailsJson
    ) {
    }
}
