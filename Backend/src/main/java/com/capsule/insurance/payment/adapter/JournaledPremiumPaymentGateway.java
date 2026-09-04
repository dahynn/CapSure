package com.capsule.insurance.payment.adapter;

import com.capsule.insurance.payment.application.port.FinancialInterfaceJournalRepository;
import com.capsule.insurance.payment.application.port.PaymentInterfaceCircuitStatusProvider;
import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.FinancialInterfaceMessage;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Keeps the core payment service independent from a provider's HTTP protocol while retaining
 * auditable request/response messages. Repeated transport timeouts open a short-lived circuit
 * and leave the order in the existing reconciliation flow rather than retrying approval blindly.
 */
@Component
@Primary
public class JournaledPremiumPaymentGateway
        implements PremiumPaymentGateway, PaymentInterfaceCircuitStatusProvider {

    private static final String INTERFACE_NAME = "FAKE_PREMIUM_PAYMENT";
    private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(30);

    private final PremiumPaymentGateway delegate;
    private final FinancialInterfaceJournalRepository journalRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int circuitFailureThreshold;
    private final Duration circuitOpenDuration;

    private int consecutiveTimeouts;
    private Instant circuitOpenedUntil;

    @Autowired
    public JournaledPremiumPaymentGateway(
            @Qualifier("fakePremiumPaymentGateway") PremiumPaymentGateway delegate,
            FinancialInterfaceJournalRepository journalRepository,
            ObjectMapper objectMapper
    ) {
        this(
                delegate,
                journalRepository,
                objectMapper,
                Clock.systemUTC(),
                CIRCUIT_FAILURE_THRESHOLD,
                CIRCUIT_OPEN_DURATION
        );
    }

    public JournaledPremiumPaymentGateway(
            PremiumPaymentGateway delegate,
            FinancialInterfaceJournalRepository journalRepository,
            ObjectMapper objectMapper,
            Clock clock,
            int circuitFailureThreshold,
            Duration circuitOpenDuration
    ) {
        this.delegate = delegate;
        this.journalRepository = journalRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitOpenDuration = circuitOpenDuration;
    }

    @Override
    public GatewayPaymentResult confirm(ConfirmCommand command) {
        String correlationId = "PAYMENT-CONFIRM:" + command.orderNo() + ":" + command.idempotencyKey();
        Instant requestedAt = Instant.now(clock);
        append(
                "PREMIUM_PAYMENT_CONFIRM",
                "OUTBOUND_REQUEST",
                correlationId,
                command.idempotencyKey(),
                command.orderNo(),
                "REQUESTED",
                null,
                command,
                requestedAt
        );

        GatewayPaymentResult result = isCircuitOpen(requestedAt)
                ? GatewayPaymentResult.unknown(command.providerPaymentKey(), "PAYMENT_INTERFACE_CIRCUIT_OPEN")
                : invokeConfirmation(command);
        append(
                "PREMIUM_PAYMENT_CONFIRM",
                "INBOUND_RESPONSE",
                correlationId,
                command.idempotencyKey(),
                command.orderNo(),
                responseStatus(result),
                result.errorCode(),
                result,
                Instant.now(clock)
        );
        return result;
    }

    @Override
    public GatewayPaymentResult inquire(String providerPaymentKey) {
        String correlationId = "PAYMENT-INQUIRY:" + providerPaymentKey + ":" + UUID.randomUUID();
        Instant requestedAt = Instant.now(clock);
        append(
                "PREMIUM_PAYMENT_INQUIRY",
                "OUTBOUND_REQUEST",
                correlationId,
                null,
                providerPaymentKey,
                "REQUESTED",
                null,
                Map.of("providerPaymentKey", providerPaymentKey),
                requestedAt
        );
        GatewayPaymentResult result;
        try {
            result = delegate.inquire(providerPaymentKey);
        } catch (RuntimeException exception) {
            result = GatewayPaymentResult.unknown(providerPaymentKey, "PAYMENT_INTERFACE_INQUIRY_ERROR");
        }
        append(
                "PREMIUM_PAYMENT_INQUIRY",
                "INBOUND_RESPONSE",
                correlationId,
                null,
                providerPaymentKey,
                responseStatus(result),
                result.errorCode(),
                result,
                Instant.now(clock)
        );
        return result;
    }

    private synchronized GatewayPaymentResult invokeConfirmation(ConfirmCommand command) {
        try {
            GatewayPaymentResult result = delegate.confirm(command);
            if ("FAKE_GATEWAY_TIMEOUT".equals(result.errorCode())) {
                consecutiveTimeouts++;
                if (consecutiveTimeouts >= circuitFailureThreshold) {
                    circuitOpenedUntil = Instant.now(clock).plus(circuitOpenDuration);
                }
            } else {
                consecutiveTimeouts = 0;
                circuitOpenedUntil = null;
            }
            return result;
        } catch (RuntimeException exception) {
            consecutiveTimeouts++;
            if (consecutiveTimeouts >= circuitFailureThreshold) {
                circuitOpenedUntil = Instant.now(clock).plus(circuitOpenDuration);
            }
            return GatewayPaymentResult.unknown(command.providerPaymentKey(), "PAYMENT_INTERFACE_CONFIRM_ERROR");
        }
    }

    private synchronized boolean isCircuitOpen(Instant now) {
        if (circuitOpenedUntil == null) {
            return false;
        }
        if (now.isBefore(circuitOpenedUntil)) {
            return true;
        }
        circuitOpenedUntil = null;
        consecutiveTimeouts = 0;
        return false;
    }

    @Override
    public synchronized CircuitStatus currentStatus() {
        Instant now = Instant.now(clock);
        boolean open = isCircuitOpen(now);
        return new CircuitStatus(
                INTERFACE_NAME,
                open,
                consecutiveTimeouts,
                circuitFailureThreshold,
                open ? circuitOpenedUntil : null
        );
    }

    private String responseStatus(GatewayPaymentResult result) {
        if ("PAYMENT_INTERFACE_CIRCUIT_OPEN".equals(result.errorCode())) {
            return "CIRCUIT_OPEN";
        }
        return switch (result.status()) {
            case "PAID" -> "SUCCEEDED";
            case "FAILED" -> "REJECTED";
            case "UNKNOWN" -> "TIMEOUT";
            default -> "ERROR";
        };
    }

    private void append(
            String messageType,
            String direction,
            String correlationId,
            String idempotencyKey,
            String businessKey,
            String status,
            String errorCode,
            Object payload,
            Instant occurredAt
    ) {
        journalRepository.append(new FinancialInterfaceMessage(
                INTERFACE_NAME,
                messageType,
                direction,
                correlationId,
                idempotencyKey,
                businessKey,
                status,
                errorCode,
                toJson(payload),
                occurredAt
        ));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("금융 인터페이스 전문을 직렬화하지 못했습니다.", exception);
        }
    }
}
