package com.capsule.insurance.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.capsule.insurance.auth.domain.UserAccount;
import com.capsule.insurance.auth.dto.UserProfileResponse;
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
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @InjectMocks
    private AuthService authService;

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
}
