package com.capsule.insurance.auth.dto;

import com.capsule.insurance.auth.domain.UserAccount;
import java.time.LocalDate;

public record UserProfileResponse(
    Long userId,
    String email,
    String name,
    String phone,
    LocalDate birthDate,
    String gender
) {
    public static UserProfileResponse from(UserAccount userAccount) {
        return new UserProfileResponse(
            userAccount.getUserId(),
            userAccount.getEmail(),
            userAccount.getName(),
            userAccount.getPhone(),
            userAccount.getBirthDate(),
            userAccount.getGender() != null ? userAccount.getGender().name() : null
        );
    }
}
