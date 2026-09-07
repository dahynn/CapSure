package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        @jakarta.validation.constraints.Size(max = 320)
        String email,
        @NotBlank(message = "password is required")
        @jakarta.validation.constraints.Size(min = 8, max = 72, message = "비밀번호는 8~72자로 입력해주세요.")
        String password,
        @NotBlank(message = "fullName is required")
        @jakarta.validation.constraints.Size(max = 100)
        String fullName,
        @NotBlank(message = "passwordConfirm is required")
        String passwordConfirm,
        @NotBlank(message = "phone is required")
        @jakarta.validation.constraints.Size(max = 20)
        String phone,
        @jakarta.validation.constraints.NotNull
        @jakarta.validation.constraints.Past
        java.time.LocalDate birthDate,
        @jakarta.validation.constraints.NotNull
        com.capsule.insurance.auth.domain.Gender gender
) {
}
