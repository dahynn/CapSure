package com.capsule.insurance.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

public record UpsertClaimEvidenceRequest(
        @NotBlank String evidenceType,
        @NotBlank String syntheticReference,
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String checksum,
        @NotNull Map<String, Object> metadata,
        boolean verified
) {
}
