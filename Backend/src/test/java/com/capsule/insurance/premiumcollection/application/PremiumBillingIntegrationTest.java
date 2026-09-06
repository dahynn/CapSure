package com.capsule.insurance.premiumcollection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.premiumcollection.dto.PremiumBillingRunResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PremiumBillingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static DataSourceTransactionManager transactionManager;
    PremiumBillingService service;

    @BeforeAll
    static void database() throws Exception {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        try (var input = new ClassPathResource("db/schema/schema.sql").getInputStream()) {
            jdbc.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .locations("classpath:db/migration")
                .load()
                .migrate();
        transactionManager = new DataSourceTransactionManager(dataSource);
    }

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE usr_user RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE ops_premium_billing_run RESTART IDENTITY CASCADE");
        service = on("2026-03-15");
    }

    @Test
    void createsOneMonthlyReceivableAndAutomaticDebitFromPolicySnapshot() {
        long policyId = policy("ACTIVE", "2025-01-31", "29900");

        PremiumBillingRunResponse result = run("february", "2026-02-01");

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.controlTotalMatched()).isTrue();
        assertThat(jdbc.queryForObject("SELECT amount_due FROM ins_premium_receivable", BigDecimal.class))
                .isEqualByComparingTo("29900");
        assertThat(jdbc.queryForObject("SELECT due_date FROM ins_premium_receivable", LocalDate.class))
                .isEqualTo(LocalDate.parse("2026-02-28"));
        assertThat(jdbc.queryForObject("SELECT grace_ends_on FROM ins_premium_receivable", LocalDate.class))
                .isEqualTo(LocalDate.parse("2026-03-14"));
        assertThat(jdbc.queryForObject("SELECT policy_id FROM ins_premium_receivable", Long.class))
                .isEqualTo(policyId);
        assertThat(jdbc.queryForObject("SELECT status FROM pay_collection_instruction", String.class))
                .isEqualTo("SCHEDULED");
    }

    @Test
    void repeatedAndCompetingRunsDoNotDuplicatePolicyCycle() {
        policy("ACTIVE", "2025-01-10", "100");

        PremiumBillingRunResponse first = run("monthly-a", "2026-03-01");
        PremiumBillingRunResponse repeated = run("monthly-a", "2026-03-01");
        PremiumBillingRunResponse competing = run("monthly-b", "2026-03-01");

        assertThat(first.createdCount()).isEqualTo(1);
        assertThat(repeated.createdCount()).isEqualTo(1);
        assertThat(competing.existingCount()).isEqualTo(1);
        assertThat(count("ins_premium_receivable")).isEqualTo(1);
        assertThat(count("pay_collection_instruction")).isEqualTo(1);
    }

    @Test
    void targetsOnlyInForcePoliciesActivatedBeforeBillingMonth() {
        policy("ACTIVE", "2025-01-01", "100");
        policy("GRACE", "2025-01-02", "200");
        policy("CHANGE_SCHEDULED", "2025-01-03", "300");
        policy("CANCELED", "2025-01-04", "400");
        policy("LAPSED", "2025-01-05", "500");
        policy("EXPIRED", "2025-01-06", "600");
        policy("ACTIVE", "2026-03-10", "700");

        PremiumBillingRunResponse result = run("eligible", "2026-03-01");

        assertThat(result.targetCount()).isEqualTo(3);
        assertThat(result.createdCount()).isEqualTo(3);
        assertThat(jdbc.queryForList(
                "SELECT amount_due FROM ins_premium_receivable ORDER BY amount_due", BigDecimal.class))
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("300.00"));
    }

    @Test
    void failedExecutionTargetsCanResumeWithoutCreatingDuplicates() {
        long policyId = policy("ACTIVE", "2025-01-15", "100");
        long runId = jdbc.queryForObject("""
                INSERT INTO ops_premium_billing_run(
                    instance_key, billing_cycle, business_date, reason, status, error_reason
                ) VALUES ('resume', DATE '2026-03-01', DATE '2026-03-15', '합성 실패 후 재개',
                          'FAILED', 'InjectedFailure')
                RETURNING run_id
                """, Long.class);
        jdbc.update("""
                INSERT INTO ops_premium_billing_target(
                    run_id, policy_id, amount_due, currency_code, due_date, grace_ends_on
                ) VALUES (?, ?, 100, 'KRW', DATE '2026-03-15', DATE '2026-03-29')
                """, runId, policyId);

        PremiumBillingRunResponse result = service.resume(runId, null, "원인 제거 후 재개");
        PremiumBillingRunResponse repeated = service.resume(runId, null, "결과 재확인");

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(repeated.createdCount()).isEqualTo(1);
        assertThat(count("ins_premium_receivable")).isEqualTo(1);
        assertThat(count("ops_premium_billing_attempt")).isEqualTo(2);
    }

    @Test
    void concurrentRequestsForSameExecutionKeepOneTargetAndReceivablePerPolicy() throws Exception {
        for (int index = 0; index < 25; index++) {
            policy("ACTIVE", "2025-01-15", "100");
        }

        parallel(
                () -> run("same-run", "2026-03-01"),
                () -> run("same-run", "2026-03-01")
        );
        PremiumBillingRunResponse result = run("same-run", "2026-03-01");

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.targetCount()).isEqualTo(25);
        assertThat(result.createdCount()).isEqualTo(25);
        assertThat(count("ins_premium_receivable")).isEqualTo(25);
        assertThat(count("pay_collection_instruction")).isEqualTo(25);
    }

    @Test
    void rejectsNonMonthlyFutureAndConflictingExecutionKeys() {
        policy("ACTIVE", "2025-01-01", "100");
        assertThatThrownBy(() -> run("day-two", "2026-03-02"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1일");
        assertThatThrownBy(() -> run("future", "2026-04-01"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("미래");

        run("same-key", "2026-02-01");
        assertThatThrownBy(() -> run("same-key", "2026-03-01"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("다른 청구월");
    }

    private PremiumBillingService on(String day) {
        return new PremiumBillingService(
                jdbc,
                transactionManager,
                Clock.fixed(Instant.parse(day + "T03:00:00Z"), ZoneOffset.UTC),
                14
        );
    }

    private PremiumBillingRunResponse run(String key, String cycle) {
        return service.run(key, LocalDate.parse(cycle), null, "합성 월 청구");
    }

    private long policy(String status, String activatedOn, String premium) {
        String suffix = UUID.randomUUID().toString();
        Long userId = jdbc.queryForObject("""
                INSERT INTO usr_user(email, name, phone, user_status)
                VALUES (?, '합성 사용자', '01000000000', 'ACTIVE')
                RETURNING user_id
                """, Long.class, suffix + "@capsure.test");
        Long quoteId = jdbc.queryForObject("""
                INSERT INTO ins_quote(
                    quote_no, user_id, product_version_id, status, monthly_premium,
                    snapshot_json, terms_document_hash, expires_at
                )
                SELECT ?, ?, product_version_id, 'USED', ?, '{}'::JSONB, REPEAT('a', 64), NOW() + INTERVAL '1 day'
                FROM ins_product_version WHERE product_code = 'CAPSURE-DEMO-CANCER'
                RETURNING quote_id
                """, Long.class, "q" + suffix, userId, new BigDecimal(premium));
        Long applicationId = jdbc.queryForObject("""
                INSERT INTO ins_application(
                    application_no, quote_id, applicant_user_id, insured_user_id, status
                ) VALUES (?, ?, ?, ?, 'APPROVED')
                RETURNING application_id
                """, Long.class, "a" + suffix, quoteId, userId, userId);
        Instant activatedAt = LocalDate.parse(activatedOn)
                .atStartOfDay(PremiumBillingService.BUSINESS_ZONE).toInstant();
        Long policyId = jdbc.queryForObject("""
                INSERT INTO ins_policy(
                    policy_no, application_id, policyholder_user_id, insured_user_id,
                    beneficiary_user_id, status, activated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING policy_id
                """, Long.class, "p" + suffix, applicationId, userId, userId, userId,
                status, Timestamp.from(activatedAt));
        jdbc.update("""
                INSERT INTO ins_policy_version(
                    policy_id, version, product_version_id, terms_document_id, valid_from, snapshot_json
                )
                SELECT ?, 1, product_version_id, terms_document_id, ?,
                       JSONB_BUILD_OBJECT('quote', JSONB_BUILD_OBJECT(
                           'monthlyPremium', CAST(? AS NUMERIC), 'currencyCode', 'KRW'
                       ))
                FROM ins_product_version WHERE product_code = 'CAPSURE-DEMO-CANCER'
                """, policyId, Timestamp.from(activatedAt), premium);
        return policyId;
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private <T> List<T> parallel(Callable<T> firstCall, Callable<T> secondCall) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<T> first = executor.submit(() -> {
                start.await();
                return firstCall.call();
            });
            Future<T> second = executor.submit(() -> {
                start.await();
                return secondCall.call();
            });
            start.countDown();
            return List.of(first.get(45, TimeUnit.SECONDS), second.get(45, TimeUnit.SECONDS));
        }
    }
}
