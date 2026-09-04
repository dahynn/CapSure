package com.capsule.insurance.premiumcollection.dto;

import java.time.LocalDate;

public record DelinquencyRunResponse(
        long runId, String instanceKey, LocalDate businessDate, String status,
        long targetCount, long processedCount, long changedCount, long unchangedCount,
        long noticeFailedCount, long remainingCount, boolean controlTotalMatched, String errorReason
) { }
