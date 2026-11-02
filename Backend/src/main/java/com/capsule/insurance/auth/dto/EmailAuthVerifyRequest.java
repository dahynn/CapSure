package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailAuthVerifyRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "authCode is required")
        @jakarta.validation.constraints.Pattern(regexp = "[0-9]{6}", message = "인증번호는 숫자 6자리입니다.")
        String authCode
) {
}
