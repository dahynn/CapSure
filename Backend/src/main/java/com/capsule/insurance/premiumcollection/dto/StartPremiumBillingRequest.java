package com.capsule.insurance.premiumcollection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StartPremiumBillingRequest(
        @NotBlank @Size(max = 150) String instanceKey,
        @NotNull LocalDate billingCycle,
        @NotBlank @Size(max = 500) String reason
) {
}
