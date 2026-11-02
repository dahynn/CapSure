package com.capsule.insurance.auth.infra;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthStateCleanup {
    private final JdbcTemplate jdbc;
    public AuthStateCleanup(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Scheduled(fixedDelayString = "${auth.cleanup-delay-ms:3600000}", initialDelayString = "${auth.cleanup-delay-ms:3600000}")
    public void deleteExpiredState() {
        jdbc.update("DELETE FROM auth_refresh_session WHERE expires_at <= now()");
        jdbc.update("DELETE FROM auth_revoked_token WHERE expires_at <= now()");
        jdbc.update("""
                DELETE FROM auth_email_verification WHERE next_send_at <= now()
                AND (code_expires_at IS NULL OR code_expires_at <= now())
                AND (verified_until IS NULL OR verified_until <= now())
                """);
    }
}
