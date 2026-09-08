package com.capsule.insurance.common.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtTokenProviderTest {
    private final JwtTokenProvider provider = new JwtTokenProvider("unit-test-secret-only-".repeat(4));

    @Test void accessAndRefreshTokensAreNotInterchangeable() throws Exception {
        String access = provider.createAccessToken("42", "synthetic@example.test", "ROLE_ADMIN");
        String refresh = provider.createRefreshToken("42");
        assertThat(provider.validateAccessToken(access)).isTrue();
        assertThat(provider.validateRefreshToken(refresh)).isTrue();
        assertThat(provider.validateAccessToken(refresh)).isFalse();
        assertThat(provider.validateRefreshToken(access)).isFalse();
        assertThatThrownBy(() -> provider.getAuthentication(refresh)).isInstanceOf(IllegalArgumentException.class);

        var filter = new JwtAuthenticationFilter(provider, mock(TokenBlacklistRepository.class));
        try {
            for (String token : new String[]{refresh, access}) {
                SecurityContextHolder.clearContext();
                var request = new MockHttpServletRequest();
                request.addHeader("Authorization", "Bearer " + token);
                filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
                assertThat(SecurityContextHolder.getContext().getAuthentication() != null).isEqualTo(token.equals(access));
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test void refreshRotationAlwaysProducesADifferentToken() {
        assertThat(provider.createRefreshToken("42")).isNotEqualTo(provider.createRefreshToken("42"));
    }

    @Test void emailProofIsBoundToRecipientAndCannotAuthenticateOrRefresh() {
        String proof = provider.createEmailVerificationToken("owner@example.test");
        assertThat(provider.validateEmailVerificationToken(proof, "owner@example.test")).isTrue();
        assertThat(provider.validateEmailVerificationToken(proof, "attacker@example.test")).isFalse();
        assertThat(provider.validateEmailVerificationToken("invalid", "owner@example.test")).isFalse();
        assertThat(provider.validateEmailVerificationToken(provider.createAccessToken("1", "owner@example.test", "ROLE_USER"), "owner@example.test")).isFalse();
        assertThat(provider.validateAccessToken(proof)).isFalse();
        assertThat(provider.validateRefreshToken(proof)).isFalse();
    }

    @Test void absentConfigurationDoesNotReuseAPublicSigningKey() {
        var firstProcess = new JwtTokenProvider("");
        var secondProcess = new JwtTokenProvider("");
        String access = firstProcess.createAccessToken("42", "synthetic@example.test", "ROLE_USER");
        assertThat(firstProcess.validateAccessToken(access)).isTrue();
        assertThat(secondProcess.validateAccessToken(access)).isFalse();
    }
}
