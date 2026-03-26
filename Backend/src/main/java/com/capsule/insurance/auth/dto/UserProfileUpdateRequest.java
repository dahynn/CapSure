package com.capsule.insurance.auth.dto;

import java.time.LocalDate;

public record UserProfileUpdateRequest(
    String name,
    String phone,
    LocalDate birthDate,
    String gender
) {
}
