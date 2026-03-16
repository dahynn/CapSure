// #Demo Setting
package com.capsule.insurance.auth.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserAccount {

    private final Long userId;
    private final String email;
    private final String passwordEncrypted;
    private final String name;
    private final String phone;
    private final LocalDate birthDate;
    private final Gender gender;
    private final UserStatus userStatus;
    private final LocalDateTime onboardingCompletedAt;
    private final LocalDateTime withdrawnAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
