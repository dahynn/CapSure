package com.capsule.insurance.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateConsentRequest(
        @NotBlank String consentType,
        @NotBlank String documentHash,
        boolean agreed
) {
}
