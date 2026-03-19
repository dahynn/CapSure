// #Demo Setting
package com.capsule.insurance.subscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuoteRequest(
        @NotBlank(message = "productCode is required")
        String productCode,
        @NotNull(message = "insuredAge is required")
        @Min(value = 0, message = "insuredAge must be positive")
        Integer insuredAge
) {
}
