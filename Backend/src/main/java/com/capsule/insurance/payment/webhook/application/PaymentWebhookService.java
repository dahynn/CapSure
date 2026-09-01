package com.capsule.insurance.payment.webhook.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import com.capsule.insurance.payment.webhook.application.port.PaymentWebhookRepository;
import com.capsule.insurance.payment.webhook.domain.PaymentWebhookEvent;
import com.capsule.insurance.payment.webhook.dto.FakePaymentWebhookRequest;
import com.capsule.insurance.payment.webhook.dto.PaymentWebhookResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentWebhookService {

    private static final String PROVIDER = "FAKE";
    private static final String SUPPORTED_EVENT_TYPE = "PAYMENT_STATUS_CHANGED";

    private final PaymentWebhookRepository webhookRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PaymentWebhookService(
            PaymentWebhookRepository webhookRepository,
            PaymentService paymentService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.webhookRepository = webhookRepository;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PaymentWebhookResponse receiveFakeWebhook(FakePaymentWebhookRequest request) {
        if (!SUPPORTED_EVENT_TYPE.equals(request.eventType())) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "지원하지 않는 결제 webhook event type입니다."
            );
        }
        String payloadJson = toJson(request);
        String payloadHash = sha256(payloadJson);
        InboxReservation reservation = Objects.requireNonNull(transactionTemplate.execute(status -> {
            PaymentWebhookEvent existing = webhookRepository
                    .findByProviderEventId(PROVIDER, request.providerEventId())
                    .orElse(null);
            if (existing != null) {
                if (!existing.payloadHash().equals(payloadHash)) {
                    throw new BusinessException(
                            ErrorCode.IDEMPOTENCY_CONFLICT,
                            "같은 provider event ID에 다른 payload가 전달되었습니다."
                    );
                }
                return new InboxReservation(existing, "PROCESSED".equals(existing.processingStatus()));
            }
            PaymentWebhookEvent received = webhookRepository.saveReceived(
                    PROVIDER,
                    request.providerEventId(),
                    request.providerPaymentKey(),
                    request.eventType(),
                    payloadJson,
                    payloadHash
            );
            if (!received.payloadHash().equals(payloadHash)) {
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "같은 provider event ID에 다른 payload가 전달되었습니다."
                );
            }
            return new InboxReservation(received, "PROCESSED".equals(received.processingStatus()));
        }));

        if (reservation.alreadyProcessed()) {
            return toResponse(reservation.event(), true, null);
        }

        try {
            PaymentOrderResponse payment = paymentService.applyProviderNotification(
                    request.providerPaymentKey(),
                    toGatewayResult(request)
            );
            PaymentWebhookEvent processed = Objects.requireNonNull(transactionTemplate.execute(status ->
                    webhookRepository.markProcessed(reservation.event().paymentWebhookEventId())
            ));
            return toResponse(processed, false, payment);
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status -> webhookRepository.markFailed(
                    reservation.event().paymentWebhookEventId(),
                    safeErrorReason(exception)
            ));
            throw exception;
        }
    }

    private GatewayPaymentResult toGatewayResult(FakePaymentWebhookRequest request) {
        return switch (request.status()) {
            case "PAID" -> GatewayPaymentResult.paid(
                    request.providerPaymentKey(),
                    "FAKE-WEBHOOK-TX-" + request.providerEventId()
            );
            case "FAILED" -> GatewayPaymentResult.failed(
                    request.providerPaymentKey(),
                    "FAKE_WEBHOOK_PAYMENT_FAILED"
            );
            case "UNKNOWN" -> GatewayPaymentResult.unknown(
                    request.providerPaymentKey(),
                    "FAKE_WEBHOOK_UNKNOWN"
            );
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 상태입니다.");
        };
    }

    private PaymentWebhookResponse toResponse(
            PaymentWebhookEvent event,
            boolean duplicate,
            PaymentOrderResponse payment
    ) {
        return new PaymentWebhookResponse(
                event.paymentWebhookEventId(),
                event.providerEventId(),
                event.processingStatus(),
                duplicate,
                payment == null ? null : payment.paymentOrderId(),
                payment == null ? null : payment.status(),
                payment == null ? null : payment.policyStatus(),
                event.receivedAt(),
                event.processedAt()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("webhook payload를 직렬화하지 못했습니다.", exception);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private String safeErrorReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private record InboxReservation(PaymentWebhookEvent event, boolean alreadyProcessed) {
    }
}
