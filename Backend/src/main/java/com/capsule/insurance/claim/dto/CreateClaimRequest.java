package com.capsule.insurance.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateClaimRequest(
        @NotNull Long policyCoverageId,
        @NotNull Instant incidentAt,
        @NotBlank String diagnosisCategory
) {
}
