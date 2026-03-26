package com.capsule.insurance.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SmsAuthVerifyRequest(
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phone,
        
        @NotBlank(message = "인증 번호를 입력해주세요.")
        String authCode
) {
}
