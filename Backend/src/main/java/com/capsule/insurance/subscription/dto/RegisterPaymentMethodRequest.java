package com.capsule.insurance.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterPaymentMethodRequest(
        @NotBlank(message = "provider는 필수입니다.")
        String provider,
        @NotBlank(message = "methodType은 필수입니다.")
        String methodType,
        @NotBlank(message = "maskedNumber는 필수입니다.")
        String maskedNumber
) {
}
