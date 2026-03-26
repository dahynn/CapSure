package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "password is required")
        String password,
        @NotBlank(message = "fullName is required")
        String fullName,
        @NotBlank(message = "passwordConfirm is required")
        String passwordConfirm,
        @NotBlank(message = "phone is required")
        String phone,
        java.time.LocalDate birthDate,
        com.capsule.insurance.auth.domain.Gender gender
) {
}
