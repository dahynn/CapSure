package com.capsule.insurance.auth.infra;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.time.Duration;
import com.capsule.insurance.payment.application.PaymentCircuitBreaker;
import com.capsule.insurance.payment.infra.JdbcPaymentCircuitStateStore;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import com.capsule.insurance.dashboard.infra.JdbcDashboardRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class SharedAuthStateIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static JdbcTemplate jdbc;
    static TransactionTemplate tx;
    final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    final JwtTokenProvider tokens = new JwtTokenProvider("shared-auth-test-signing-key-at-least-thirty-two-bytes-long");

    @BeforeAll
    static void init() throws Exception {
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        try (var input = new ClassPathResource("db/schema/schema.sql").getInputStream()) {
            jdbc.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("0").load().migrate();
    }

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE usr_user RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE auth_revoked_token, auth_email_verification, ifc_payment_circuit_state");
        jdbc.update("""
                INSERT INTO usr_user(user_id,email,name,phone) VALUES
                (1,'one@example.test','사용자1','01000000001'), (2,'two@example.test','사용자2','01000000002')
                """);
    }

    @Test
    void twoInstancesRotateExactlyOnceAndStoreOnlyHashes() throws Exception {
        var first = new JdbcRefreshTokenRepository(jdbc, tokens);
        var second = new JdbcRefreshTokenRepository(jdbc, tokens);
        String original = tokens.createRefreshToken("1");
        String nextA = tokens.createRefreshToken("1");
        String nextB = tokens.createRefreshToken("1");
        first.save("1", original);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> a = () -> { start.await(); return first.replaceIfMatches("1", original, nextA); };
        Callable<Boolean> b = () -> { start.await(); return second.replaceIfMatches("1", original, nextB); };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var fa = executor.submit(a); var fb = executor.submit(b); start.countDown();
            var results = List.of(fa.get(), fb.get());
            assertThat(results).containsExactlyInAnyOrder(true, false);
            String winner = results.getFirst() ? nextA : nextB;
            assertThat(jdbc.queryForObject("SELECT token_hash FROM auth_refresh_session", String.class))
                    .hasSize(64).isNotEqualTo(winner).isNotEqualTo(original);
            assertThat(new JdbcRefreshTokenRepository(jdbc, tokens)
                    .replaceIfMatches("1", winner, tokens.createRefreshToken("1"))).isTrue();
        }
    }

    @Test
    void expiredOrLoggedOutSessionsCannotRotate() {
        var sessions = new JdbcRefreshTokenRepository(jdbc, tokens);
        String token = tokens.createRefreshToken("1");
        sessions.save("1", token);
        jdbc.update("UPDATE auth_refresh_session SET expires_at = now() - interval '1 second'");
        assertThat(sessions.replaceIfMatches("1", token, tokens.createRefreshToken("1"))).isFalse();
        sessions.save("1", token);
        sessions.deleteByUserId("1");
        assertThat(sessions.replaceIfMatches("1", token, tokens.createRefreshToken("1"))).isFalse();
    }

    @Test
    void logoutRevocationIsSharedAndExpires() {
        String access = tokens.createAccessToken("1", "one@example.test", "ROLE_USER");
        var first = new JdbcTokenBlacklistRepository(jdbc);
        var second = new JdbcTokenBlacklistRepository(jdbc);
        first.save(access, 60_000);
        assertThat(second.isBlacklisted(access)).isTrue();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM auth_revoked_token", String.class)).hasSize(64);
        jdbc.update("UPDATE auth_revoked_token SET expires_at = now() - interval '1 second'");
        assertThat(second.isBlacklisted(access)).isFalse();
        new AuthStateCleanup(jdbc).deleteExpiredState();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_revoked_token", Integer.class)).isZero();
    }

    @Test
    void emailCodeIsRateLimitedOneTimeAndPersistsAcrossInstances() {
        var first = new JdbcEmailVerificationRepository(jdbc, passwords);
        var second = new JdbcEmailVerificationRepository(jdbc, passwords);
        String email = "verify@example.test";
        assertThat(first.reserveCode(email, passwords.encode("123456"), 3)).isTrue();
        assertThat(second.reserveCode(email, passwords.encode("654321"), 3)).isFalse();
        boolean verified = Boolean.TRUE.equals(tx.execute(s -> second.verifyCode(email, "123456", 30)));
        assertThat(verified).isTrue();
        boolean replay = Boolean.TRUE.equals(tx.execute(s -> first.verifyCode(email, "123456", 30)));
        assertThat(replay).isFalse();
        assertThat(first.isVerified(email)).isTrue();
        assertThat(second.consumeVerified(email)).isTrue();
        assertThat(first.consumeVerified(email)).isFalse();
    }

    @Test
    void fiveWrongCodesAndExpiredCodesCannotVerify() {
        var repository = new JdbcEmailVerificationRepository(jdbc, passwords);
        repository.reserveCode("limited@example.test", passwords.encode("123456"), 3);
        for (int i = 0; i < 5; i++) {
            boolean verified = Boolean.TRUE.equals(tx.execute(s -> repository.verifyCode("limited@example.test", "000000", 30)));
            assertThat(verified).isFalse();
        }
        boolean limited = Boolean.TRUE.equals(tx.execute(s -> repository.verifyCode("limited@example.test", "123456", 30)));
        assertThat(limited).isFalse();
        repository.reserveCode("expired@example.test", passwords.encode("123456"), 3);
        jdbc.update("UPDATE auth_email_verification SET code_expires_at = now() - interval '1 second'");
        boolean expired = Boolean.TRUE.equals(tx.execute(s -> repository.verifyCode("expired@example.test", "123456", 30)));
        assertThat(expired).isFalse();
    }

    @Test
    void verificationConsumptionRollsBackWhenSignupFails() {
        var repository = new JdbcEmailVerificationRepository(jdbc, passwords);
        repository.reserveCode("rollback@example.test", passwords.encode("123456"), 3);
        tx.executeWithoutResult(s -> repository.verifyCode("rollback@example.test", "123456", 30));
        tx.executeWithoutResult(s -> { assertThat(repository.consumeVerified("rollback@example.test")).isTrue(); s.setRollbackOnly(); });
        assertThat(repository.consumeVerified("rollback@example.test")).isTrue();
    }

    @Test
    void dashboardCountsOwnedRowsAndTracksReadCursor() {
        jdbc.update("""
                INSERT INTO subscription(user_id,subscription_status,next_billing_at) VALUES
                (1,'ACTIVE',now()+interval '1 day'),(1,'ACTIVE',now()+interval '10 days'),
                (1,'CANCELLED',now()+interval '1 day'),(2,'ACTIVE',now()+interval '1 day')
                """);
        jdbc.update("""
                INSERT INTO audit_event_log(event_type,actor_user_id,target_type,target_id) VALUES
                ('SUBSCRIPTION_CHANGED',1,'SUBSCRIPTION',1),('SUBSCRIPTION_CHANGED',2,'SUBSCRIPTION',2)
                """);
        var repository = new JdbcDashboardRepository(jdbc);
        var summary = repository.summary(1L);
        assertThat(summary.activeSubscriptions()).isEqualTo(2);
        assertThat(summary.expiringSoon()).isEqualTo(1);
        assertThat(summary.unreadAudits()).isEqualTo(1);
        repository.markAuditsRead(1L);
        assertThat(repository.summary(1L).unreadAudits()).isZero();
        assertThat(repository.summary(2L).unreadAudits()).isEqualTo(1);
        jdbc.update("INSERT INTO audit_event_log(event_type,actor_user_id,target_type,target_id) VALUES ('SUBSCRIPTION_CHANGED',1,'SUBSCRIPTION',1)");
        assertThat(repository.summary(1L).unreadAudits()).isEqualTo(1);
    }

    private PaymentCircuitBreaker circuit() {
        return new PaymentCircuitBreaker(new JdbcPaymentCircuitStateStore(jdbc,
                new DataSourceTransactionManager(jdbc.getDataSource())), 3, Duration.ofSeconds(30), Duration.ofMinutes(2));
    }

    @Test
    void circuitStateSurvivesInstancesAndIgnoresLateSuccesses() {
        var first = circuit(); var second = circuit();
        assertThat(first.status("TOSS_PREMIUM_PAYMENT").open()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ifc_payment_circuit_state", Integer.class)).isZero();
        var late = first.acquire("TOSS_PREMIUM_PAYMENT");
        for (int i = 0; i < 3; i++) second.complete(second.acquire("TOSS_PREMIUM_PAYMENT"), true);
        first.complete(late, false);
        assertThat(circuit().status("TOSS_PREMIUM_PAYMENT").open()).isTrue();
        assertThat(circuit().acquire("TOSS_PREMIUM_PAYMENT")).isNull();
        assertThat(circuit().acquire("FAKE_PREMIUM_PAYMENT")).isNotNull();
    }

    @Test
    void onlyOneReplicaCanProbeAfterCooldown() throws Exception {
        var first = circuit(); var second = circuit();
        for (int i = 0; i < 3; i++) first.complete(first.acquire("TOSS_PREMIUM_PAYMENT"), true);
        jdbc.update("UPDATE ifc_payment_circuit_state SET open_until = now() - interval '1 second'");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(); return first.acquire("TOSS_PREMIUM_PAYMENT"); });
            var b = executor.submit(() -> { start.await(); return second.acquire("TOSS_PREMIUM_PAYMENT"); });
            start.countDown();
            var pa = a.get(); var pb = b.get();
            assertThat(java.util.stream.Stream.of(pa, pb).filter(java.util.Objects::nonNull).count()).isEqualTo(1);
            assertThat(first.status("TOSS_PREMIUM_PAYMENT").open()).isTrue();
            second.complete(pa == null ? pb : pa, false);
            assertThat(first.status("TOSS_PREMIUM_PAYMENT").open()).isFalse();
            assertThat(first.status("TOSS_PREMIUM_PAYMENT").consecutiveTimeouts()).isZero();
        }
    }

    @Test
    void crashedProbeExpiresAndItsLateOutcomeCannotResetNewProbe() {
        var breaker = circuit();
        for (int i = 0; i < 3; i++) breaker.complete(breaker.acquire("TOSS_PREMIUM_PAYMENT"), true);
        jdbc.update("UPDATE ifc_payment_circuit_state SET open_until = now() - interval '1 second'");
        var oldProbe = breaker.acquire("TOSS_PREMIUM_PAYMENT");
        jdbc.update("UPDATE ifc_payment_circuit_state SET probe_until = now() - interval '1 second'");
        var newProbe = circuit().acquire("TOSS_PREMIUM_PAYMENT");
        breaker.complete(oldProbe, false);
        assertThat(breaker.status("TOSS_PREMIUM_PAYMENT").open()).isTrue();
        breaker.complete(newProbe, true);
        assertThat(breaker.acquire("TOSS_PREMIUM_PAYMENT")).isNull();
    }
}
