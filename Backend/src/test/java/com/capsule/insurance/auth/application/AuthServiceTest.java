package com.capsule.insurance.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.capsule.insurance.auth.domain.AccessRole;
import com.capsule.insurance.auth.domain.Gender;
import com.capsule.insurance.auth.domain.UserAccount;
import com.capsule.insurance.auth.domain.UserStatus;
import com.capsule.insurance.auth.dto.AuthResult;
import com.capsule.insurance.auth.dto.LoginRequest;
import com.capsule.insurance.auth.dto.UserProfileResponse;
import com.capsule.insurance.auth.dto.UserProfileUpdateRequest;
import com.capsule.insurance.auth.infra.UserAccountMapper;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.auth.domain.RefreshTokenRepository;
import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountMapper userAccountMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private SmsService smsService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("운영자 로그인은 DB에 저장된 관리자 역할로 토큰을 발급한다")
    void login_IssuesAdminRoleToken() {
        UserAccount admin = UserAccount.builder()
                .userId(7L)
                .email("demo@example.com")
                .passwordEncrypted("encoded-password")
                .userStatus(UserStatus.ACTIVE)
                .accessRole(AccessRole.ROLE_ADMIN)
                .build();
        given(userAccountMapper.findByEmail("demo@example.com")).willReturn(admin);
        given(passwordEncoder.matches("Passw0rd!", "encoded-password")).willReturn(true);
        given(jwtTokenProvider.createAccessToken("7", "demo@example.com", "ROLE_ADMIN"))
                .willReturn("admin-access-token");
        given(jwtTokenProvider.createRefreshToken("7")).willReturn("admin-refresh-token");

        AuthResult result = authService.login(new LoginRequest("demo@example.com", "Passw0rd!"));

        assertThat(result.accessToken()).isEqualTo("admin-access-token");
        assertThat(result.role()).isEqualTo("ROLE_ADMIN");
        verify(refreshTokenRepository).save("7", "admin-refresh-token");
    }

    @Test
    @DisplayName("정상적인 userId로 프로필을 조회하면 UserProfileResponse를 반환한다")
    void getProfile_Success() {
        // given
        Long userId = 1L;
        UserAccount mockUser = UserAccount.builder()
                .userId(userId)
                .email("test@example.com")
                .name("tester")
                .phone("010-1234-5678")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        
        given(userAccountMapper.findByUserId(userId)).willReturn(mockUser);

        // when
        UserProfileResponse response = authService.getProfile(String.valueOf(userId));

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("tester");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.gender()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 userId로 프로필을 조회하면 BusinessException을 던진다")
    void getProfile_NotFound() {
        // given
        Long userId = 999L;
        given(userAccountMapper.findByUserId(userId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.getProfile(String.valueOf(userId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("계정 정보");
    }

    @Test
    @DisplayName("정상적인 userId와 갱신 정보로 프로필을 수정하면 UserProfileResponse를 반환한다")
    void updateProfile_Success() {
        // given
        Long userId = 1L;
        UserAccount mockUser = UserAccount.builder()
                .userId(userId)
                .email("test@example.com")
                .name("tester")
                .phone("010-1234-5678")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.M)
                .build();
        
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
            "newTester",
            "010-9999-8888",
            LocalDate.of(1995, 5, 5),
            "M"
        );

        given(userAccountMapper.findByUserId(userId)).willReturn(mockUser);

        // when
        UserProfileResponse response = authService.updateProfile(String.valueOf(userId), request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("newTester");
        assertThat(response.phone()).isEqualTo("010-9999-8888");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 5, 5));
        assertThat(response.gender()).isEqualTo("M");
        assertThat(mockUser.getName()).isEqualTo("newTester");
        assertThat(mockUser.getPhone()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("존재하지 않는 userId로 프로필 수정을 시도하면 BusinessException을 던진다")
    void updateProfile_NotFound() {
        // given
        Long userId = 999L;
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
            "newTester",
            "010-9999-8888",
            LocalDate.of(1995, 5, 5),
            "M"
        );
        given(userAccountMapper.findByUserId(userId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.updateProfile(String.valueOf(userId), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("계정 정보");
    }
}
