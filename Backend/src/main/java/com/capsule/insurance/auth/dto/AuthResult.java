// #Demo Setting
package com.capsule.insurance.auth.dto;

public record AuthResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        String memberId
) {
}
