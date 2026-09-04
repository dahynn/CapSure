package com.capsule.insurance.premiumcollection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartDelinquencyRequest(
        @NotBlank @Size(max = 150) String instanceKey,
        @NotBlank @Size(max = 500) String reason
) { }
