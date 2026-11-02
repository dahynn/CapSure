package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.RefreshTokenRepository;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRefreshTokenRepository implements RefreshTokenRepository {
    private final JdbcTemplate jdbc;
    private final JwtTokenProvider tokens;
    public JdbcRefreshTokenRepository(JdbcTemplate jdbc, JwtTokenProvider tokens) {
        this.jdbc = jdbc;
        this.tokens = tokens;
    }
    @Override
    public void save(String userId, String refreshToken) {
        jdbc.update("""
                INSERT INTO auth_refresh_session(user_id, token_hash, expires_at) VALUES (?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET token_hash = EXCLUDED.token_hash,
                    expires_at = EXCLUDED.expires_at, updated_at = now()
                """, Long.valueOf(userId), TokenHash.of(refreshToken), expiry(refreshToken));
    }
    @Override
    public boolean replaceIfMatches(String userId, String previousToken, String nextToken) {
        return jdbc.update("""
                UPDATE auth_refresh_session SET token_hash = ?, expires_at = ?, updated_at = now()
                WHERE user_id = ? AND token_hash = ? AND expires_at > now()
                """, TokenHash.of(nextToken), expiry(nextToken), Long.valueOf(userId), TokenHash.of(previousToken)) == 1;
    }
    @Override
    public void deleteByUserId(String userId) {
        jdbc.update("DELETE FROM auth_refresh_session WHERE user_id = ?", Long.valueOf(userId));
    }
    private Timestamp expiry(String token) {
        return Timestamp.from(Instant.now().plusMillis(tokens.getExpirationRemaining(token)));
    }
}
