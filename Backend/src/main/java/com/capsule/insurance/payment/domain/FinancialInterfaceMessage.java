package com.capsule.insurance.payment.domain;

import java.time.Instant;

public record FinancialInterfaceMessage(
        String interfaceName,
        String messageType,
        String direction,
        String correlationId,
        String idempotencyKey,
        String businessKey,
        String status,
        String errorCode,
        String payloadJson,
        Instant occurredAt
) {
}
