// #Demo Setting
package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "password is required")
        String password
) {
}
