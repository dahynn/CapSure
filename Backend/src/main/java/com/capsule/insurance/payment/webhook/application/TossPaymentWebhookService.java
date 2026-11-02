package com.capsule.insurance.payment.webhook.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import com.capsule.insurance.payment.webhook.application.port.PaymentWebhookRepository;
import com.capsule.insurance.payment.webhook.domain.PaymentWebhookEvent;
import com.capsule.insurance.payment.webhook.dto.PaymentWebhookResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Toss 일반결제 webhook inbox. 일반결제 webhook에는 서명이 없으므로 payload의 상태를 확정값으로
 * 사용하지 않고, 같은 paymentKey를 Toss 조회 API로 재검증한 결과만 반영합니다.
 */
@Service
@ConditionalOnProperty(name = "payment.gateway", havingValue = "toss")
public class TossPaymentWebhookService {

    private static final String PROVIDER = "TOSS";
    private static final String SUPPORTED_EVENT_TYPE = "PAYMENT_STATUS_CHANGED";

    private final PaymentWebhookRepository webhookRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public TossPaymentWebhookService(
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

    public PaymentWebhookResponse receive(String transmissionId, String rawPayload) {
        if (transmissionId == null || transmissionId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Toss webhook transmission ID가 필요합니다.");
        }
        TossEvent event = parse(rawPayload);
        String storedPayload = toJson(Map.of(
                "eventType", event.eventType(),
                "paymentKeyHash", sha256(event.paymentKey())
        ));
        String payloadHash = sha256(storedPayload);
        InboxReservation reservation = Objects.requireNonNull(transactionTemplate.execute(status -> {
            PaymentWebhookEvent existing = webhookRepository.findByProviderEventId(PROVIDER, transmissionId).orElse(null);
            if (existing != null) {
                if (!existing.payloadHash().equals(payloadHash)) {
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT,
                            "같은 Toss webhook ID에 다른 payload가 전달되었습니다.");
                }
                return new InboxReservation(existing, "PROCESSED".equals(existing.processingStatus()));
            }
            PaymentWebhookEvent received = webhookRepository.saveReceived(
                    PROVIDER, transmissionId, event.paymentKey(), event.eventType(), storedPayload, payloadHash);
            return new InboxReservation(received, false);
        }));
        if (reservation.alreadyProcessed()) return toResponse(reservation.event(), true, null);

        try {
            PaymentOrderResponse payment = paymentService.reconcileByProviderPaymentKey(PROVIDER, event.paymentKey());
            PaymentWebhookEvent processed = Objects.requireNonNull(transactionTemplate.execute(status ->
                    webhookRepository.markProcessed(reservation.event().paymentWebhookEventId())));
            return toResponse(processed, false, payment);
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status -> webhookRepository.markFailed(
                    reservation.event().paymentWebhookEventId(), safeErrorReason(exception)));
            throw exception;
        }
    }

    private TossEvent parse(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventType = root.path("eventType").asText();
            String paymentKey = root.path("data").path("paymentKey").asText();
            if (!SUPPORTED_EVENT_TYPE.equals(eventType)) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "지원하지 않는 Toss webhook event type입니다.");
            }
            if (paymentKey.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Toss webhook paymentKey가 필요합니다.");
            }
            return new TossEvent(eventType, paymentKey);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Toss webhook JSON 형식이 올바르지 않습니다.");
        }
    }

    private PaymentWebhookResponse toResponse(PaymentWebhookEvent event, boolean duplicate, PaymentOrderResponse payment) {
        return new PaymentWebhookResponse(
                event.paymentWebhookEventId(), event.providerEventId(), event.processingStatus(), duplicate,
                payment == null ? null : payment.paymentOrderId(), payment == null ? null : payment.status(),
                payment == null ? null : payment.policyStatus(), event.receivedAt(), event.processedAt());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Toss webhook audit payload를 직렬화하지 못했습니다.", exception);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", exception);
        }
    }

    private String safeErrorReason(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 500));
    }

    private record TossEvent(String eventType, String paymentKey) { }
    private record InboxReservation(PaymentWebhookEvent event, boolean alreadyProcessed) { }
}
