// #Demo Setting
package com.capsule.insurance.auth.application;

import com.capsule.insurance.auth.dto.AuthResult;
import com.capsule.insurance.auth.dto.LoginRequest;
import com.capsule.insurance.auth.dto.SignupRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public AuthResult login(LoginRequest request) {
        // TODO: 실제 인증 로직과 사용자 조회, 비밀번호 검증을 구현해야 합니다.
        return new AuthResult("stub-user-1", "stub-refresh-user-1", "Bearer", request.username());
    }

    public AuthResult signup(SignupRequest request) {
        // TODO: 실제 회원가입 로직과 중복 검증, 비밀번호 암호화를 구현해야 합니다.
        return new AuthResult("stub-user-1", "stub-refresh-user-1", "Bearer", request.email());
    }
}
