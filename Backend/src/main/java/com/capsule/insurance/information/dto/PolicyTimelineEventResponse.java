package com.capsule.insurance.information.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record PolicyTimelineEventResponse(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        Long policyId,
        JsonNode payload,
        String payloadHash,
        Instant occurredAt,
        Instant projectedAt
) {
}
