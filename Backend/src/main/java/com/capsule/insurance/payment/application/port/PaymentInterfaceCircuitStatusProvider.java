package com.capsule.insurance.payment.application.port;

import java.time.Instant;

public interface PaymentInterfaceCircuitStatusProvider {

    CircuitStatus currentStatus();

    record CircuitStatus(
            String interfaceName,
            boolean open,
            int consecutiveTimeouts,
            int failureThreshold,
            Instant openUntil
    ) {
    }
}
