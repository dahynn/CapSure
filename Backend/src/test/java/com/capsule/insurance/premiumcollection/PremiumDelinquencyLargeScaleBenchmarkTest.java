package com.capsule.insurance.premiumcollection;

import static org.assertj.core.api.Assertions.assertThat;

import com.capsule.insurance.premiumcollection.application.PremiumDelinquencyService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Deliberately opt-in: it creates 1,000/10,000 synthetic policies and is not part of CI test.
 * Elapsed time starts after setup, so data generation and Testcontainers startup are excluded.
 */
@Tag("large-benchmark")
@Testcontainers(disabledWithoutDocker = true)
class PremiumDelinquencyLargeScaleBenchmarkTest {

    private static final int[] SCALES = {1_000, 10_000};
    private static final int[] WORKERS = {1, 2};
    private static final int MEASURED_REPETITIONS = 3;
    private static final int WARMUPS = 1;

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_large_delinquency_benchmark")
            .withUsername("capsure")
            .withPassword("capsure");

    private static JdbcTemplate jdbc;
    private static DataSourceTransactionManager transactions;

    @BeforeAll
    static void database() throws Exception {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        try (InputStream input = new ClassPathResource("db/schema/schema.sql").getInputStream()) {
            jdbc.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("0")
                .locations("classpath:db/migration").load().migrate();
        transactions = new DataSourceTransactionManager(dataSource);
    }

    @Test
    void measuresSameLogicalBatchForOneAndTwoWorkersAndResumesTenThousandTargets() throws Exception {
        List<Map<String, Object>> samples = new ArrayList<>();
        for (int scale : SCALES) {
            for (int workers : WORKERS) {
                for (int repetition = 0; repetition < WARMUPS + MEASURED_REPETITIONS; repetition++) {
                    samples.add(measure(scale, workers, repetition < WARMUPS, repetition));
                }
            }
        }
        Map<String, Object> recovery = measureRecovery(10_000);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", 1);
        report.put("measuredAt", Instant.now().toString());
        report.put("clock", "System.nanoTime");
        report.put("databaseImage", "postgres:16-alpine");
        report.put("scales", List.of(1_000, 10_000));
        report.put("workers", List.of(1, 2));
        report.put("warmupsPerConfiguration", WARMUPS);
        report.put("measuredRepetitionsPerConfiguration", MEASURED_REPETITIONS);
        report.put("samples", samples);
        report.put("recovery", recovery);
        report.put("implementation", "same run instance key; concurrent callers share one target set");
        report.put("limitations", List.of(
                "Synthetic PostgreSQL/Testcontainers data only; no real SMS, payment or production traffic.",
                "Elapsed time excludes fixture setup and container startup but includes batch database work and simulated notices.",
                "A controlled exception after committed chunks is not a process kill, database outage or disaster recovery test."
        ));
        Path output = Path.of(System.getProperty("capsure.large-benchmark.output"));
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter().writeValueAsString(report));
        System.out.println("CAPSURE_LARGE_BENCHMARK samples=" + samples.size() + " output=" + output);
    }

    private Map<String, Object> measure(int scale, int workers, boolean warmup, int repetition) throws Exception {
        String prefix = "throughput-" + scale + "-" + workers + "-" + repetition + "-" + UUID.randomUUID();
        seed(scale, prefix);
        PremiumDelinquencyService service = service(id -> true);
        long start = System.nanoTime();
        runConcurrently(service, prefix, workers);
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        assertCompletedExactly(prefix, scale);
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("scenario", "throughput");
        sample.put("scale", scale);
        sample.put("workers", workers);
        sample.put("warmup", warmup);
        sample.put("repetition", repetition);
        sample.put("elapsedMs", elapsedMs);
        sample.put("runCount", count("ops_premium_delinquency_run"));
        sample.put("targetCount", count("ops_premium_delinquency_target"));
        sample.put("noticeCount", count("ins_premium_notice"));
        sample.put("transitionCount", count("ins_policy_delinquency_history"));
        sample.put("duplicateNotices", count("ins_premium_notice") - scale);
        sample.put("duplicateTransitions", count("ins_policy_delinquency_history") - scale);
        return sample;
    }

    private Map<String, Object> measureRecovery(int scale) throws Exception {
        String prefix = "recovery-" + scale + "-" + UUID.randomUUID();
        seed(scale, prefix);
        AtomicInteger notices = new AtomicInteger();
        PremiumDelinquencyService interrupted = service(id -> {
            if (notices.incrementAndGet() == 2_021) throw new IllegalStateException("BENCHMARK_INJECTED_FAILURE");
            return true;
        });
        long start = System.nanoTime();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> interrupted.run(prefix, null, "synthetic benchmark"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("BENCHMARK_INJECTED_FAILURE");
        long failedAfterMs = System.nanoTime();
        long runId = jdbc.queryForObject("SELECT run_id FROM ops_premium_delinquency_run WHERE instance_key = ?", Long.class, prefix);
        long committedBeforeResume = count("ins_premium_notice");
        // Chunks have 20 targets; failure is injected on attempt 2,021, after 101 durable chunks.
        assertThat(committedBeforeResume).isEqualTo(2_020);
        PremiumDelinquencyService resumed = service(id -> true);
        resumed.resume(runId, null, "synthetic benchmark resume");
        double totalElapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        double resumeElapsedMs = (System.nanoTime() - failedAfterMs) / 1_000_000.0;
        assertCompletedExactly(prefix, scale);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scenario", "controlled-checkpoint-recovery");
        result.put("scale", scale);
        result.put("failureAtNoticeAttempt", 2_021);
        result.put("committedBeforeResume", committedBeforeResume);
        result.put("totalElapsedMs", totalElapsedMs);
        result.put("resumeElapsedMs", resumeElapsedMs);
        result.put("noticeCount", count("ins_premium_notice"));
        result.put("transitionCount", count("ins_policy_delinquency_history"));
        result.put("duplicateNotices", count("ins_premium_notice") - scale);
        result.put("duplicateTransitions", count("ins_policy_delinquency_history") - scale);
        return result;
    }

    private void runConcurrently(PremiumDelinquencyService service, String key, int workers) throws Exception {
        try (var pool = Executors.newFixedThreadPool(workers)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                futures.add(pool.submit(() -> {
                    if (!start.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("start barrier timeout");
                    return service.run(key, null, "synthetic benchmark");
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(10, TimeUnit.MINUTES);
        }
    }

    private PremiumDelinquencyService service(com.capsule.insurance.premiumcollection.application.PremiumNoticeGateway notices) {
        return new PremiumDelinquencyService(jdbc, transactions, notices,
                Clock.fixed(Instant.parse("2020-01-02T03:00:00Z"), ZoneOffset.UTC), 14);
    }

    private void seed(int scale, String prefix) {
        jdbc.execute("TRUNCATE usr_user RESTART IDENTITY CASCADE");
        Long userId = jdbc.queryForObject("""
                INSERT INTO usr_user(email, name, phone, user_status) VALUES (?, '벤치마크', '01000000000', 'ACTIVE')
                RETURNING user_id
                """, Long.class, prefix + "@capsure.test");
        Long productVersionId = jdbc.queryForObject("""
                SELECT product_version_id FROM ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER' AND version = '1.0.0'
                """, Long.class);
        String termsHash = jdbc.queryForObject("""
                SELECT source_hash FROM ins_terms_document
                WHERE document_code = 'CAPSURE-DEMO-CANCER-TERMS' AND document_version = '1.0.0'
                """, String.class);
        jdbc.update("""
                INSERT INTO ins_quote(quote_no, user_id, product_version_id, status, monthly_premium, snapshot_json, terms_document_hash, expires_at)
                SELECT ? || '-Q-' || number, ?, ?, 'USED', 100, '{}'::jsonb, ?, NOW() + INTERVAL '1 day'
                FROM generate_series(1, ?) number
                """, prefix, userId, productVersionId, termsHash, scale);
        jdbc.update("""
                INSERT INTO ins_application(application_no, quote_id, applicant_user_id, insured_user_id, status, submitted_at)
                SELECT ? || '-A-' || substring(quote_no FROM '-Q-(.*)$'), quote_id, ?, ?, 'APPROVED', NOW()
                FROM ins_quote WHERE quote_no LIKE ?
                """, prefix, userId, userId, prefix + "-Q-%");
        jdbc.update("""
                INSERT INTO ins_policy(policy_no, application_id, policyholder_user_id, insured_user_id, beneficiary_user_id, status, activated_at)
                SELECT ? || '-P-' || substring(application_no FROM '-A-(.*)$'), application_id, ?, ?, ?, 'ACTIVE', NOW()
                FROM ins_application WHERE application_no LIKE ?
                """, prefix, userId, userId, userId, prefix + "-A-%");
        jdbc.update("""
                INSERT INTO ins_premium_receivable(policy_id, billing_cycle, due_date, grace_ends_on, amount_due, status)
                SELECT policy_id, DATE '2020-01-01', DATE '2020-01-01', DATE '2020-01-15', 100, 'DUE'
                FROM ins_policy WHERE policy_no LIKE ?
                """, prefix + "-P-%");
        assertThat(count("ins_policy")).isEqualTo(scale);
        assertThat(count("ins_premium_receivable")).isEqualTo(scale);
    }

    private void assertCompletedExactly(String key, int expected) {
        assertThat(jdbc.queryForObject("SELECT status FROM ops_premium_delinquency_run WHERE instance_key = ?", String.class, key))
                .isEqualTo("COMPLETED");
        assertThat(count("ops_premium_delinquency_run")).isEqualTo(1);
        assertThat(count("ops_premium_delinquency_target")).isEqualTo(expected);
        assertThat(count("ins_premium_notice")).isEqualTo(expected);
        assertThat(count("ins_policy_delinquency_history")).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ops_premium_delinquency_target WHERE outcome IS NULL", Long.class))
                .isZero();
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
