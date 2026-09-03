package com.capsule.insurance.operations.reconciliation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartPaymentReconciliationRequest(
        @NotBlank @Size(max = 255) String instanceKey,
        @NotNull @Min(1) @Max(500) Integer chunkSize,
        @NotNull @Min(0) @Max(604800) Long staleAfterSeconds
) {
}
