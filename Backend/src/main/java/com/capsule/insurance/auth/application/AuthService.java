// #Demo Setting
package com.capsule.insurance.auth.application;

import com.capsule.insurance.auth.domain.UserAccount;
import com.capsule.insurance.auth.domain.UserStatus;
import com.capsule.insurance.auth.dto.AuthResult;
import com.capsule.insurance.auth.dto.LoginRequest;
import com.capsule.insurance.auth.dto.SignupRequest;
import com.capsule.insurance.auth.infra.UserAccountMapper;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public AuthResult login(LoginRequest request) {
        // TODO: 실제 인증 로직과 사용자 조회, 비밀번호 검증을 구현해야 합니다.
        return new AuthResult("stub-user-1", "stub-refresh-user-1", "Bearer", request.username());
    }

    public AuthResult signup(SignupRequest request) {
        
        // 1. 비밀번호 일치 확인
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        
        // 2. 이메일 중복 체크
        if (userAccountMapper.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }
        
        // 3. 이메일 인증 확인
        if (!emailService.isEmailVerified(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        
        // 4. 유저 생성 및 비밀번호 암호화
        UserAccount userAccount = UserAccount.builder()
                .email(request.email())
                .passwordEncrypted(passwordEncoder.encode(request.password()))
                .name(request.fullName())
                .phone(request.phone())
                .birthDate(request.birthDate())
                .gender(request.gender())
                .userStatus(UserStatus.PENDING_ONBOARDING)
                .build();
                
        userAccountMapper.insert(userAccount);
        
        // 5. 사용된 이메일 인증 상태 제거
        emailService.completeSignup(request.email());
        
        return new AuthResult("stub-user-1", "stub-refresh-user-1", "Bearer", request.email());
    }
}
