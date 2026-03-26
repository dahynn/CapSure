package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {
}
