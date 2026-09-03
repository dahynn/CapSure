package com.capsule.insurance.information.domain;

import java.time.Instant;

public record FinancialEventAudit(
        Long financialEventAuditId,
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        Long policyId,
        String payloadJson,
        String payloadHash,
        Instant occurredAt,
        Instant projectedAt
) {
}
