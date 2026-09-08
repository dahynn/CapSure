package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.EmailVerificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcEmailVerificationRepository implements EmailVerificationRepository {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    public JdbcEmailVerificationRepository(JdbcTemplate jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
    }
    @Override
    public boolean reserveCode(String email, String codeHash, long expirationMinutes) {
        return jdbc.update("""
                INSERT INTO auth_email_verification(email, code_hash, code_expires_at, next_send_at)
                VALUES (?, ?, now() + (? * interval '1 minute'), now() + interval '60 seconds')
                ON CONFLICT (email) DO UPDATE SET code_hash = EXCLUDED.code_hash,
                    code_expires_at = EXCLUDED.code_expires_at, next_send_at = EXCLUDED.next_send_at,
                    failed_attempts = 0, verified_until = NULL
                WHERE auth_email_verification.next_send_at <= now()
                """, email, codeHash, expirationMinutes) == 1;
    }
    @Override
    public void invalidateCode(String email, String codeHash) {
        jdbc.update("UPDATE auth_email_verification SET code_hash = NULL WHERE email = ? AND code_hash = ?", email, codeHash);
    }
    @Override
    @Transactional
    public boolean verifyCode(String email, String code, long verifiedMinutes) {
        var hashes = jdbc.query("""
                SELECT code_hash FROM auth_email_verification
                WHERE email = ? AND code_hash IS NOT NULL AND code_expires_at > now() AND failed_attempts < 5
                FOR UPDATE
                """, (rs, row) -> rs.getString(1), email);
        if (hashes.isEmpty()) return false;
        if (!passwords.matches(code, hashes.getFirst())) {
            jdbc.update("UPDATE auth_email_verification SET failed_attempts = failed_attempts + 1 WHERE email = ?", email);
            return false;
        }
        jdbc.update("""
                UPDATE auth_email_verification SET code_hash = NULL, code_expires_at = NULL,
                    verified_until = now() + (? * interval '1 minute') WHERE email = ?
                """, verifiedMinutes, email);
        return true;
    }
    @Override
    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM auth_email_verification WHERE email = ? AND verified_until > now())
                """, Boolean.class, email));
    }
    @Override
    public boolean consumeVerified(String email) {
        return jdbc.update("""
                UPDATE auth_email_verification SET verified_until = NULL WHERE email = ? AND verified_until > now()
                """, email) == 1;
    }
}
