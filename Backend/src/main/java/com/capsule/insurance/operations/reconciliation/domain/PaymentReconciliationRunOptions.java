package com.capsule.insurance.operations.reconciliation.domain;

import java.time.Duration;

public record PaymentReconciliationRunOptions(
        int chunkSize,
        Duration staleAfter,
        Integer failAfterChunks
) {

    public PaymentReconciliationRunOptions {
        if (chunkSize < 1 || chunkSize > 500) {
            throw new IllegalArgumentException("chunkSize는 1 이상 500 이하여야 합니다.");
        }
        if (staleAfter == null || staleAfter.isNegative() || staleAfter.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("staleAfter는 0초 이상 7일 이하여야 합니다.");
        }
        if (failAfterChunks != null && failAfterChunks <= 0) {
            throw new IllegalArgumentException("failAfterChunks는 양수여야 합니다.");
        }
    }

    public static PaymentReconciliationRunOptions production(int chunkSize, Duration staleAfter) {
        return new PaymentReconciliationRunOptions(chunkSize, staleAfter, null);
    }
}
