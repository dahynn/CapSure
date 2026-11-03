package com.capsule.insurance.payment.adapter;

import com.capsule.insurance.payment.application.port.FinancialInterfaceJournalRepository;
import com.capsule.insurance.payment.application.port.PaymentInterfaceCircuitStatusProvider;
import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.application.port.PaymentCircuitStateStore;
import com.capsule.insurance.payment.application.PaymentCircuitBreaker;
import com.capsule.insurance.payment.domain.FinancialInterfaceMessage;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import com.capsule.insurance.payment.application.port.PaymentConfirmationAdmission;

/**
 * Keeps the core payment service independent from a provider's HTTP protocol while retaining
 * auditable request/response messages. Repeated transport timeouts open a short-lived circuit
 * and leave the order in the existing reconciliation flow rather than retrying approval blindly.
 */
@Component
@Primary
public class JournaledPremiumPaymentGateway
        implements PremiumPaymentGateway, PaymentInterfaceCircuitStatusProvider, PaymentConfirmationAdmission {

    private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final int DEFAULT_MAX_CONCURRENT_CALLS = 16;
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(30);

    private final PremiumPaymentGateway delegate;
    private final FinancialInterfaceJournalRepository journalRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final PaymentCircuitBreaker circuit;
    private final Semaphore concurrentCalls;

    @Autowired
    public JournaledPremiumPaymentGateway(
            @Qualifier("selectedPremiumPaymentGateway") PremiumPaymentGateway delegate,
            FinancialInterfaceJournalRepository journalRepository,
            ObjectMapper objectMapper,
            PaymentCircuitStateStore circuitStore,
            @Value("${payment.max-concurrent-calls:16}") int maxConcurrentCalls
    ) {
        this(
                delegate,
                journalRepository,
                objectMapper,
                Clock.systemUTC(),
                CIRCUIT_FAILURE_THRESHOLD,
                CIRCUIT_OPEN_DURATION,
                circuitStore,
                maxConcurrentCalls
        );
    }

    @Override
    public String providerCode() {
        return delegate.providerCode();
    }

    public JournaledPremiumPaymentGateway(
            PremiumPaymentGateway delegate,
            FinancialInterfaceJournalRepository journalRepository,
            ObjectMapper objectMapper,
            Clock clock,
            int circuitFailureThreshold,
            Duration circuitOpenDuration
    ) {
        this(delegate, journalRepository, objectMapper, clock, circuitFailureThreshold, circuitOpenDuration,
                new InMemoryPaymentCircuitStateStore(clock));
    }

    public JournaledPremiumPaymentGateway(
            PremiumPaymentGateway delegate, FinancialInterfaceJournalRepository journalRepository,
            ObjectMapper objectMapper, Clock clock, int circuitFailureThreshold,
            Duration circuitOpenDuration, PaymentCircuitStateStore circuitStore
    ) {
        this(delegate, journalRepository, objectMapper, clock, circuitFailureThreshold, circuitOpenDuration,
                circuitStore, DEFAULT_MAX_CONCURRENT_CALLS);
    }

    public JournaledPremiumPaymentGateway(
            PremiumPaymentGateway delegate, FinancialInterfaceJournalRepository journalRepository,
            ObjectMapper objectMapper, Clock clock, int circuitFailureThreshold,
            Duration circuitOpenDuration, PaymentCircuitStateStore circuitStore, int maxConcurrentCalls
    ) {
        if (maxConcurrentCalls < 1) throw new IllegalArgumentException("payment.max-concurrent-calls must be positive");
        this.concurrentCalls = new Semaphore(maxConcurrentCalls);
        this.delegate = delegate;
        this.journalRepository = journalRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.circuit = new PaymentCircuitBreaker(circuitStore, circuitFailureThreshold, circuitOpenDuration, Duration.ofMinutes(2));
    }

    @Override
    public PaymentConfirmationAdmission.Permit tryReserveConfirmation() {
        if (!concurrentCalls.tryAcquire()) return null;
        return new PaymentConfirmationAdmission.Permit() {
            private final AtomicBoolean closed = new AtomicBoolean();
            private final AtomicBoolean used = new AtomicBoolean();
            public GatewayPaymentResult confirm(ConfirmCommand command) {
                if (closed.get() || !used.compareAndSet(false, true)) throw new IllegalStateException("Payment admission permit is closed");
                return confirmWithAdmission(command, true);
            }
            public void close() { if (closed.compareAndSet(false, true)) concurrentCalls.release(); }
        };
    }

    @Override
    public GatewayPaymentResult confirm(ConfirmCommand command) {
        var permit = tryReserveConfirmation();
        if (permit == null) return confirmWithAdmission(command, false);
        try (permit) { return permit.confirm(command); }
    }

    private GatewayPaymentResult confirmWithAdmission(ConfirmCommand command, boolean admitted) {
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
                sanitizedCommand(command),
                requestedAt
        );

        GatewayPaymentResult result;
        if (!admitted) {
            result = GatewayPaymentResult.unknown(command.providerPaymentKey(), "PAYMENT_INTERFACE_BUSY");
        } else {
            var permit = circuit.acquire(interfaceName());
            result = permit == null
                    ? GatewayPaymentResult.unknown(command.providerPaymentKey(), "PAYMENT_INTERFACE_CIRCUIT_OPEN")
                    : invokeConfirmation(command, permit);
        }
        append(
                "PREMIUM_PAYMENT_CONFIRM",
                "INBOUND_RESPONSE",
                correlationId,
                command.idempotencyKey(),
                command.orderNo(),
                responseStatus(result),
                result.errorCode(),
                sanitizedResult(result),
                Instant.now(clock)
        );
        return result;
    }

    @Override
    public GatewayPaymentResult inquire(InquiryCommand command) {
        String providerPaymentKey = command.providerPaymentKey();
        String correlationId = "PAYMENT-INQUIRY:" + command.orderNo() + ":" + UUID.randomUUID();
        Instant requestedAt = Instant.now(clock);
        append(
                "PREMIUM_PAYMENT_INQUIRY",
                "OUTBOUND_REQUEST",
                correlationId,
                null,
                command.orderNo(),
                "REQUESTED",
                null,
                Map.of("providerPaymentKeyHash", hash(providerPaymentKey)),
                requestedAt
        );
        GatewayPaymentResult result;
        if (!concurrentCalls.tryAcquire()) {
            result = GatewayPaymentResult.unknown(providerPaymentKey, "PAYMENT_INTERFACE_BUSY");
        } else {
            try {
                result = delegate.inquire(command);
            } catch (RuntimeException exception) {
                result = GatewayPaymentResult.unknown(providerPaymentKey, "PAYMENT_INTERFACE_INQUIRY_ERROR");
            } finally {
                concurrentCalls.release();
            }
        }
        append(
                "PREMIUM_PAYMENT_INQUIRY",
                "INBOUND_RESPONSE",
                correlationId,
                null,
                command.orderNo(),
                responseStatus(result),
                result.errorCode(),
                sanitizedResult(result),
                Instant.now(clock)
        );
        return result;
    }

    private GatewayPaymentResult invokeConfirmation(ConfirmCommand command, PaymentCircuitBreaker.Permit permit) {
        GatewayPaymentResult result;
        try {
            result = delegate.confirm(command);
        } catch (RuntimeException exception) {
            result = GatewayPaymentResult.unknown(command.providerPaymentKey(), "PAYMENT_INTERFACE_CONFIRM_ERROR");
        }
        circuit.complete(permit, "UNKNOWN".equals(result.status()));
        return result;
    }

    @Override
    public CircuitStatus currentStatus() {
        return circuit.status(interfaceName());
    }

    private String responseStatus(GatewayPaymentResult result) {
        if ("PAYMENT_INTERFACE_BUSY".equals(result.errorCode())) return "ERROR";
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
                interfaceName(),
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

    private String interfaceName() {
        return providerCode() + "_PREMIUM_PAYMENT";
    }

    private Map<String, Object> sanitizedCommand(ConfirmCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", command.orderNo());
        payload.put("providerPaymentKeyHash", hash(command.providerPaymentKey()));
        payload.put("amount", command.amount());
        payload.put("currencyCode", command.currencyCode());
        payload.put("idempotencyKey", command.idempotencyKey());
        return payload;
    }

    private Map<String, Object> sanitizedResult(GatewayPaymentResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", result.status());
        payload.put("providerPaymentKeyHash", hash(result.providerPaymentKey()));
        payload.put("providerTransactionIdHash", hash(result.providerTransactionId()));
        payload.put("errorCode", result.errorCode());
        payload.put("message", result.message());
        return payload;
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("금융 인터페이스 전문을 직렬화하지 못했습니다.", exception);
        }
    }
}
