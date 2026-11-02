package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTokenBlacklistRepository implements TokenBlacklistRepository {
    private final JdbcTemplate jdbc;
    public JdbcTokenBlacklistRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    public void save(String token, long expireTimeMillis) {
        if (expireTimeMillis <= 0) return;
        jdbc.update("""
                INSERT INTO auth_revoked_token(token_hash, expires_at) VALUES (?, ?)
                ON CONFLICT (token_hash) DO UPDATE
                SET expires_at = GREATEST(auth_revoked_token.expires_at, EXCLUDED.expires_at)
                """, TokenHash.of(token), Timestamp.from(Instant.now().plusMillis(expireTimeMillis)));
    }
    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM auth_revoked_token WHERE token_hash = ? AND expires_at > now())
                """, Boolean.class, TokenHash.of(token)));
    }
}
