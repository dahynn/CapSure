package com.capsule.insurance.auth.application;

import com.capsule.insurance.auth.domain.UserAccount;
import com.capsule.insurance.auth.domain.UserStatus;
import com.capsule.insurance.auth.domain.Gender;
import com.capsule.insurance.auth.dto.AuthResult;
import com.capsule.insurance.auth.dto.LoginRequest;
import com.capsule.insurance.auth.dto.SignupRequest;
import com.capsule.insurance.auth.dto.UserProfileResponse;
import com.capsule.insurance.auth.dto.UserProfileUpdateRequest;
import com.capsule.insurance.auth.dto.TokenRefreshRequest;
import com.capsule.insurance.auth.infra.UserAccountMapper;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.auth.domain.RefreshTokenRepository;
import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public AuthService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, EmailService emailService, SmsService smsService, JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository, TokenBlacklistRepository tokenBlacklistRepository) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    public void logout(String userId, String accessToken) {
        // 1. Refresh Token 무효화 (저장소에서 삭제)
        refreshTokenRepository.deleteByUserId(userId);
        
        // 2. Access Token 블랙리스트 추가 (남은 만료 시간만큼만 저장)
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            long remaining = jwtTokenProvider.getExpirationRemaining(accessToken);
            if (remaining > 0) {
                tokenBlacklistRepository.save(accessToken, remaining);
            }
        }
    }

    public void withdraw(String userId, String accessToken) {
        UserAccount user = userAccountMapper.findByUserId(Long.valueOf(userId));
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "계정 정보를 찾을 수 없습니다.");
        }
        if (user.getUserStatus() == com.capsule.insurance.auth.domain.UserStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "이미 탈퇴 처리된 계정입니다.");
        }

        userAccountMapper.withdraw(Long.valueOf(userId));
        this.logout(userId, accessToken);
    }

    public UserProfileResponse getProfile(String userId) {
        UserAccount user = userAccountMapper.findByUserId(Long.valueOf(userId));
        if (Objects.isNull(user)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "계정 정보를 찾을 수 없습니다.");
        }
        return UserProfileResponse.from(user);
    }

    public UserProfileResponse updateProfile(String userId, UserProfileUpdateRequest request) {
        UserAccount user = userAccountMapper.findByUserId(Long.valueOf(userId));
        if (Objects.isNull(user)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "계정 정보를 찾을 수 없습니다.");
        }

        user.updateProfile(request.name(), request.phone(), request.birthDate(), Gender.valueOf(request.gender()));
        userAccountMapper.updateProfile(user);

        return UserProfileResponse.from(user);
    }

    public AuthResult login(LoginRequest request) {
        UserAccount user = userAccountMapper.findByEmail(request.email());
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 이메일입니다.");
        }
        
        if (user.getUserStatus() == UserStatus.WITHDRAWN || user.getUserStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "접근이 제한된 계정입니다.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordEncrypted())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH, "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(user.getUserId()), user.getEmail(), "ROLE_USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(user.getUserId()));

        refreshTokenRepository.save(String.valueOf(user.getUserId()), refreshToken);

        return new AuthResult(accessToken, refreshToken, "Bearer", String.valueOf(user.getUserId()));
    }

    public AuthResult refresh(TokenRefreshRequest request) {
        String providedToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(providedToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 Refresh Token입니다.");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(providedToken);
        String storedToken = refreshTokenRepository.findByUserId(userId);
        
        if (storedToken == null || !providedToken.equals(storedToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰 정보가 일치하지 않습니다. 다시 로그인해주세요.");
        }

        UserAccount user = userAccountMapper.findByUserId(Long.valueOf(userId));
        if (user == null || user.getUserStatus() == com.capsule.insurance.auth.domain.UserStatus.WITHDRAWN || user.getUserStatus() == com.capsule.insurance.auth.domain.UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "접근이 제한된 계정입니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, user.getEmail(), "ROLE_USER");
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenRepository.save(userId, newRefreshToken);

        return new AuthResult(newAccessToken, newRefreshToken, "Bearer", userId);
    }

    public void signup(SignupRequest request) {
        
        // 1. 비밀번호 일치 확인
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        
        // 2. 이메일 중복 체크
        if (userAccountMapper.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }
        
        // 3. 이메일 인증 확인 - TODO: 프론트 이메일 인증 UI 연동 후 주석 해제
        // if (!emailService.isEmailVerified(request.email())) {
        //     throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        // }
        // if (!smsService.isPhoneVerified(request.phone())) {
        //    throw new BusinessException(ErrorCode.UNAUTHORIZED, "휴대폰 인증이 완료되지 않았습니다.");
        // }
        
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
        
        // 5. 사용된 이메일 및 휴대폰 인증 상태 제거
        emailService.completeSignup(request.email());
        smsService.completeSignup(request.phone());
        
    }
}
