package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SmsAuthRequest(
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phone
) {
}
