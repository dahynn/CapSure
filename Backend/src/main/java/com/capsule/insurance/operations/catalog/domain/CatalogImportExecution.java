package com.capsule.insurance.operations.catalog.domain;

import java.time.Instant;

public record CatalogImportExecution(
        Long jobExecutionId,
        String jobName,
        String instanceKey,
        int executionNo,
        String status,
        String sourceChecksum,
        String mappingRuleVersion,
        int nextIndex,
        int processedChunks,
        long inputCount,
        long acceptedCount,
        long duplicateCount,
        long quarantinedCount,
        boolean controlTotalMatched,
        Instant startedAt,
        Instant finishedAt,
        String errorReason
) {
}
