package com.capsule.insurance.auth.api;

import com.capsule.insurance.auth.application.AuthService;
import com.capsule.insurance.auth.application.EmailService;
import com.capsule.insurance.auth.dto.AuthResult;
import com.capsule.insurance.auth.dto.EmailAuthSendRequest;
import com.capsule.insurance.auth.dto.EmailAuthVerifyRequest;
import com.capsule.insurance.auth.dto.LoginRequest;
import com.capsule.insurance.auth.dto.SignupRequest;
import com.capsule.insurance.auth.dto.UserProfileResponse;
import com.capsule.insurance.auth.dto.UserProfileUpdateRequest;
import com.capsule.insurance.auth.dto.TokenRefreshRequest;
import com.capsule.insurance.auth.dto.SmsAuthRequest;
import com.capsule.insurance.auth.dto.SmsAuthVerifyRequest;
import com.capsule.insurance.auth.application.SmsService;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final SmsService smsService;

    public AuthController(AuthService authService, EmailService emailService, SmsService smsService) {
        this.authService = authService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/signup")
    public ApiResponse<String> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.success("회원가입이 완료되었습니다.");
    }

    @PostMapping("/email/send-code")
    public ApiResponse<String> sendEmailVerificationCode(@Valid @RequestBody EmailAuthSendRequest request) {
        emailService.sendVerificationCode(request.email());
        return ApiResponse.success("이메일로 인증 번호가 전송되었습니다.");
    }

    @PostMapping("/email/verify-code")
    public ApiResponse<String> verifyEmailCode(@Valid @RequestBody EmailAuthVerifyRequest request) {
        boolean verified = emailService.verifyCode(request.email(), request.authCode());
        if (!verified) {
            throw new BusinessException(ErrorCode.INVALID_AUTH_CODE);
        }
        return ApiResponse.success("인증 번호가 일치합니다.");
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success("프로필 정보 불러오기를 성공했습니다.", authService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(org.springframework.security.core.Authentication authentication, @Valid @RequestBody UserProfileUpdateRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success("PUT - 프로필 수정을 성공했습니다.", authService.updateProfile(authentication.getName(), request));
    }
    
    @PostMapping("/phone/send-code")
    public ApiResponse<String> sendPhoneVerificationCode(@Valid @RequestBody SmsAuthRequest request) {
        smsService.sendVerificationCode(request.phone());
        return ApiResponse.success("휴대폰으로 인증 번호가 전송되었습니다.");
    }

    @PostMapping("/phone/verify-code")
    public ApiResponse<String> verifyPhoneCode(@Valid @RequestBody SmsAuthVerifyRequest request) {
        boolean verified = smsService.verifyCode(request.phone(), request.authCode());
        if (!verified) {
            throw new BusinessException(ErrorCode.INVALID_AUTH_CODE, "인증 번호가 일치하지 않습니다.");
        }
        return ApiResponse.success("휴대폰 인증이 완료되었습니다.");
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String authorization = request.getHeader("Authorization");
        String accessToken = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        
        authService.logout(authentication.getName(), accessToken);
        return ApiResponse.success("로그아웃 되었습니다.");
    }

    @DeleteMapping("/withdraw")
    public ApiResponse<String> withdraw(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String authorization = request.getHeader("Authorization");
        String accessToken = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        
        authService.withdraw(authentication.getName(), accessToken);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다.");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResult> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResult result = authService.refresh(request);
        return ApiResponse.success(result);
    }
}
