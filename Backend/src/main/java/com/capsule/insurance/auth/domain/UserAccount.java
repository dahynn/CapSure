// #Demo Setting
package com.capsule.insurance.auth.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserAccount {

    private Long userId;
    private String email;
    private String passwordEncrypted;
    private String name;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
    private UserStatus userStatus;
    private LocalDateTime onboardingCompletedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateProfile(String name, String phone, java.time.LocalDate birthDate, Gender gender) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
    }
}
