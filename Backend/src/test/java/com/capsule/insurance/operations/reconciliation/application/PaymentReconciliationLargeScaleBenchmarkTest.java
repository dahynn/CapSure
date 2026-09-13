package com.capsule.insurance.operations.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationInterruptedException;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationRunOptions;
import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;
import com.capsule.insurance.operations.reconciliation.infra.JdbcPaymentReconciliationJobRepository;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import com.capsule.insurance.payment.infra.JdbcPaymentRepository;
import com.capsule.insurance.policy.infra.JdbcPolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Opt-in benchmark for the real payment reconciliation repository/service path.
 * Fixture insertion, reconciliation workload, and verification are timed separately.
 */
@Tag("large-benchmark")
@Testcontainers(disabledWithoutDocker = true)
class PaymentReconciliationLargeScaleBenchmarkTest {

    private static final int PROBE_SCALE = 1_000;
    private static final int MEASURED_SCALE = 10_000;
    private static final int CHUNK_SIZE = 100;
    private static final int MEASURED_REPETITIONS = 3;
    private static final int[] WORKERS = {1, 2};

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_large_reconciliation_benchmark")
            .withUsername("capsure")
            .withPassword("capsure");

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static DataSourceTransactionManager transactions;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void database() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
        jdbc = new JdbcTemplate(dataSource);
        try (InputStream input = new ClassPathResource("db/schema/schema.sql").getInputStream()) {
            jdbc.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("0")
                .locations("classpath:db/migration").load().migrate();
        transactions = new DataSourceTransactionManager(dataSource);
        objectMapper = new ObjectMapper();
    }

    @AfterAll
    static void closePool() {
        if (dataSource != null) dataSource.close();
    }

    @Test
    void measuresTenThousandUnknownOrdersWithCompetingWorkersAndCheckpointResume() throws Throwable {
        Path output = Path.of(System.getProperty("capsure.large-reconciliation-benchmark.output"));
        Map<String, Object> report = reportHeader();
        List<Map<String, Object>> samples = new ArrayList<>();
        report.put("samples", samples);
        try {
            report.put("probe", measure(PROBE_SCALE, 2, true, 0));
            for (int workers : WORKERS) {
                for (int repetition = 1; repetition <= MEASURED_REPETITIONS; repetition++) {
                    samples.add(measure(MEASURED_SCALE, workers, false, repetition));
                }
            }
            report.put("recovery", measureRecovery(MEASURED_SCALE));
            report.put("status", "PASS");
        } catch (Throwable failure) {
            report.put("status", "FAIL");
            report.put("failureType", failure.getClass().getSimpleName());
            throw failure;
        } finally {
            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
            System.out.println("CAPSURE_LARGE_RECONCILIATION status=" + report.get("status") + " output=" + output);
        }
    }

    private Map<String, Object> reportHeader() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", 1);
        report.put("measuredAt", Instant.now().toString());
        report.put("clock", "System.nanoTime");
        report.put("databaseImage", "postgres:16-alpine");
        report.put("java", System.getProperty("java.version"));
        report.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        report.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        report.put("maxHeapBytes", Runtime.getRuntime().maxMemory());
        report.put("connectionPoolSize", 8);
        report.put("probeScale", PROBE_SCALE);
        report.put("measuredScale", MEASURED_SCALE);
        report.put("workers", List.of(1, 2));
        report.put("chunkSize", CHUNK_SIZE);
        report.put("measuredRepetitionsPerWorkerConfiguration", MEASURED_REPETITIONS);
        report.put("latencyDefinition", "per-target PaymentService.reconcile wall-clock latency");
        report.put("phaseDefinition", "seed, workload, and verification are measured separately");
        report.put("limitations", List.of(
                "Synthetic PostgreSQL/Testcontainers data only; no production customers, traffic, PG calls, or settlement files.",
                "Resolved means the synthetic provider inquiry returned FAILED and the local UNKNOWN order was reconciled; it is not a successful payment.",
                "Times are hardware, JVM, container, connection-pool, and local-load dependent; they are not TPS, SLA, or capacity guarantees.",
                "Checkpoint recovery injects a controlled application exception after committed chunks; it is not a process kill, database failover, or disaster recovery test."
        ));
        return report;
    }

    private Map<String, Object> measure(int scale, int workers, boolean probe, int repetition) throws Exception {
        String prefix = "recon-" + scale + "-" + workers + "-" + repetition + "-" + UUID.randomUUID();
        long totalStarted = System.nanoTime();
        long seedStarted = System.nanoTime();
        Dataset dataset = seed(scale, prefix);
        double seedElapsedMs = elapsedMs(seedStarted);

        ConcurrentLinkedQueue<Long> targetLatencies = new ConcurrentLinkedQueue<>();
        ResolvingGateway gateway = new ResolvingGateway(workers);
        long workloadStarted = System.nanoTime();
        List<PaymentReconciliationExecutionResponse> executions = runWorkers(
                workers, prefix, gateway, targetLatencies,
                PaymentReconciliationRunOptions.production(CHUNK_SIZE, Duration.ZERO));
        double workloadElapsedMs = elapsedMs(workloadStarted);

        long verificationStarted = System.nanoTime();
        Verification verification = verify(dataset, executions, targetLatencies);
        double verificationElapsedMs = elapsedMs(verificationStarted);

        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("scenario", probe ? "environment-probe" : "unknown-reconciliation");
        sample.put("scale", scale);
        sample.put("workers", workers);
        sample.put("repetition", repetition);
        sample.put("seedElapsedMs", seedElapsedMs);
        sample.put("workloadElapsedMs", workloadElapsedMs);
        sample.put("verificationElapsedMs", verificationElapsedMs);
        sample.put("totalElapsedMs", elapsedMs(totalStarted));
        sample.put("throughputPerSecond", scale / (workloadElapsedMs / 1_000.0));
        sample.put("targetLatencyP95Ms", percentileMs(targetLatencies, 0.95));
        sample.put("targetLatencyMaxMs", percentileMs(targetLatencies, 1.0));
        sample.put("workerProcessedCounts", executions.stream()
                .map(PaymentReconciliationExecutionResponse::processedCount).toList());
        sample.put("resolvedCount", verification.resolvedCount());
        sample.put("finalFailedOrderCount", verification.finalFailedOrderCount());
        sample.put("missingCount", verification.missingCount());
        sample.put("duplicateAttemptCount", verification.duplicateAttemptCount());
        sample.put("reconciliationRowCount", verification.reconciliationRowCount());
        sample.put("controlTotalsMatched", verification.controlTotalsMatched());
        return sample;
    }

    private Map<String, Object> measureRecovery(int scale) throws Exception {
        String prefix = "recon-recovery-" + scale + "-" + UUID.randomUUID();
        long totalStarted = System.nanoTime();
        long seedStarted = System.nanoTime();
        Dataset dataset = seed(scale, prefix);
        double seedElapsedMs = elapsedMs(seedStarted);
        ConcurrentLinkedQueue<Long> targetLatencies = new ConcurrentLinkedQueue<>();
        ResolvingGateway gateway = new ResolvingGateway(1);
        PaymentReconciliationBatchService interrupted = service(gateway, targetLatencies);

        long failedPhaseStarted = System.nanoTime();
        assertThatThrownBy(() -> interrupted.run(
                prefix,
                new PaymentReconciliationRunOptions(CHUNK_SIZE, Duration.ZERO, 20)
        )).isInstanceOf(PaymentReconciliationInterruptedException.class);
        double failedPhaseElapsedMs = elapsedMs(failedPhaseStarted);

        JdbcPaymentReconciliationJobRepository repository = repository();
        PaymentReconciliationExecutionResponse failed = repository
                .findLatest(PaymentReconciliationBatchService.JOB_NAME, prefix)
                .map(execution -> interrupted.getExecution(execution.jobExecutionId()))
                .orElseThrow();
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.processedCount()).isEqualTo(2_000);

        long resumeStarted = System.nanoTime();
        PaymentReconciliationExecutionResponse completed = service(gateway, targetLatencies).run(
                prefix,
                PaymentReconciliationRunOptions.production(CHUNK_SIZE, Duration.ZERO)
        );
        double resumeElapsedMs = elapsedMs(resumeStarted);

        long verificationStarted = System.nanoTime();
        Verification verification = verify(dataset, List.of(completed), targetLatencies);
        double verificationElapsedMs = elapsedMs(verificationStarted);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scenario", "controlled-checkpoint-recovery");
        result.put("scale", scale);
        result.put("failureAfterChunks", 20);
        result.put("committedBeforeResume", failed.processedCount());
        result.put("seedElapsedMs", seedElapsedMs);
        result.put("failedPhaseElapsedMs", failedPhaseElapsedMs);
        result.put("resumeElapsedMs", resumeElapsedMs);
        result.put("verificationElapsedMs", verificationElapsedMs);
        result.put("totalElapsedMs", elapsedMs(totalStarted));
        result.put("resumeThroughputPerSecond", (scale - failed.processedCount()) / (resumeElapsedMs / 1_000.0));
        result.put("targetLatencyP95Ms", percentileMs(targetLatencies, 0.95));
        result.put("resolvedCount", verification.resolvedCount());
        result.put("finalFailedOrderCount", verification.finalFailedOrderCount());
        result.put("missingCount", verification.missingCount());
        result.put("duplicateAttemptCount", verification.duplicateAttemptCount());
        result.put("reconciliationRowCount", verification.reconciliationRowCount());
        result.put("controlTotalsMatched", verification.controlTotalsMatched());
        return result;
    }

    private Dataset seed(int scale, String prefix) {
        jdbc.execute("TRUNCATE TABLE public.usr_user RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE public.ops_job_execution RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE public.ops_reconciliation RESTART IDENTITY");
        Long userId = jdbc.queryForObject("""
                INSERT INTO public.usr_user(email, name, phone, user_status)
                VALUES (?, '대용량 합성', '01000000000', 'ACTIVE') RETURNING user_id
                """, Long.class, prefix + "@capsure.test");
        Long productVersionId = jdbc.queryForObject("""
                SELECT product_version_id FROM public.ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER' AND version = '1.0.0'
                """, Long.class);
        String termsHash = jdbc.queryForObject("""
                SELECT source_hash FROM public.ins_terms_document
                WHERE document_code = 'CAPSURE-DEMO-CANCER-TERMS' AND document_version = '1.0.0'
                """, String.class);
        Long quoteId = jdbc.queryForObject("""
                INSERT INTO public.ins_quote(quote_no, user_id, product_version_id, status, monthly_premium,
                    snapshot_json, terms_document_hash, expires_at)
                VALUES (?, ?, ?, 'USED', 29900.00, '{}'::JSONB, ?, NOW() + INTERVAL '1 day') RETURNING quote_id
                """, Long.class, prefix + "-Q", userId, productVersionId, termsHash);
        Long applicationId = jdbc.queryForObject("""
                INSERT INTO public.ins_application(application_no, quote_id, applicant_user_id, insured_user_id,
                    status, submitted_at)
                VALUES (?, ?, ?, ?, 'APPROVED', NOW()) RETURNING application_id
                """, Long.class, prefix + "-A", quoteId, userId, userId);
        Long policyId = jdbc.queryForObject("""
                INSERT INTO public.ins_policy(policy_no, application_id, policyholder_user_id, insured_user_id,
                    beneficiary_user_id, status)
                VALUES (?, ?, ?, ?, ?, 'PENDING_INITIAL_PREMIUM') RETURNING policy_id
                """, Long.class, prefix + "-P", applicationId, userId, userId, userId);
        jdbc.update("""
                INSERT INTO public.pay_order(order_no, business_key, application_id, policy_id, purpose, amount,
                    status, idempotency_key, expires_at, reconciliation_available_at)
                SELECT ? || '-PAY-' || number, ? || '-BIZ-' || number, ?, ?, 'INITIAL_PREMIUM', 29900.00,
                    'UNKNOWN', ? || '-ORDER-IDEMP-' || number, NOW() + INTERVAL '30 minutes',
                    NOW() - INTERVAL '10 minutes'
                FROM generate_series(1, ?) number
                """, prefix, prefix, applicationId, policyId, prefix, scale);
        jdbc.update("""
                INSERT INTO public.pay_attempt(payment_order_id, attempt_no, provider, provider_payment_key,
                    idempotency_key, status, completed_at)
                SELECT payment_order_id, 1, 'FAKE', ? || '-PROVIDER-' || payment_order_id,
                    ? || '-ATTEMPT-IDEMP-' || payment_order_id, 'UNKNOWN', NOW()
                FROM public.pay_order WHERE order_no LIKE ?
                """, prefix, prefix, prefix + "-PAY-%");
        Long firstId = jdbc.queryForObject("SELECT MIN(payment_order_id) FROM public.pay_order", Long.class);
        Long lastId = jdbc.queryForObject("SELECT MAX(payment_order_id) FROM public.pay_order", Long.class);
        assertThat(countOrders(firstId, lastId)).isEqualTo(scale);
        return new Dataset(firstId, lastId, scale);
    }

    private List<PaymentReconciliationExecutionResponse> runWorkers(
            int workers,
            String prefix,
            ResolvingGateway gateway,
            ConcurrentLinkedQueue<Long> targetLatencies,
            PaymentReconciliationRunOptions options
    ) throws Exception {
        try (var pool = Executors.newFixedThreadPool(workers)) {
            CountDownLatch ready = new CountDownLatch(workers);
            CountDownLatch release = new CountDownLatch(1);
            List<Future<PaymentReconciliationExecutionResponse>> futures = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                int workerNumber = worker + 1;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!release.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("worker release timeout");
                    return service(gateway, targetLatencies).run(prefix + "-WORKER-" + workerNumber, options);
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            List<PaymentReconciliationExecutionResponse> results = new ArrayList<>();
            for (Future<PaymentReconciliationExecutionResponse> future : futures) {
                results.add(future.get(20, TimeUnit.MINUTES));
            }
            return results;
        }
    }

    private PaymentReconciliationBatchService service(
            PremiumPaymentGateway gateway,
            ConcurrentLinkedQueue<Long> targetLatencies
    ) {
        PaymentService paymentService = new TimedPaymentService(
                new JdbcPaymentRepository(jdbc),
                new JdbcPolicyRepository(jdbc, objectMapper),
                gateway,
                transactions,
                objectMapper,
                targetLatencies
        );
        return new PaymentReconciliationBatchService(repository(), paymentService, transactions);
    }

    private JdbcPaymentReconciliationJobRepository repository() {
        return new JdbcPaymentReconciliationJobRepository(jdbc);
    }

    private Verification verify(
            Dataset dataset,
            List<PaymentReconciliationExecutionResponse> executions,
            ConcurrentLinkedQueue<Long> targetLatencies
    ) {
        assertThat(executions).allSatisfy(execution -> {
            assertThat(execution.status()).isEqualTo("COMPLETED");
            assertThat(execution.controlTotalMatched()).isTrue();
        });
        if (executions.size() > 1) {
            assertThat(executions).allSatisfy(execution -> assertThat(execution.processedCount()).isPositive());
        }
        long resolved = executions.stream().mapToLong(PaymentReconciliationExecutionResponse::resolvedCount).sum();
        long batchFailures = executions.stream().mapToLong(PaymentReconciliationExecutionResponse::failedCount).sum();
        long stillUnknown = executions.stream().mapToLong(PaymentReconciliationExecutionResponse::stillUnknownCount).sum();
        long finalFailed = countWhere(dataset, "status = 'FAILED'");
        long missing = countWhere(dataset, "reconciliation_attempt_count = 0");
        long duplicateAttempts = countWhere(dataset, "reconciliation_attempt_count > 1");
        long reconciliationRows = jdbc.queryForObject("""
                SELECT COUNT(*) FROM public.ops_reconciliation
                WHERE target_type = 'PAYMENT_ORDER' AND target_id::BIGINT BETWEEN ? AND ?
                """, Long.class, dataset.firstId(), dataset.lastId());
        assertThat(resolved).isEqualTo(dataset.scale());
        assertThat(batchFailures).isZero();
        assertThat(stillUnknown).isZero();
        assertThat(finalFailed).isEqualTo(dataset.scale());
        assertThat(missing).isZero();
        assertThat(duplicateAttempts).isZero();
        assertThat(reconciliationRows).isEqualTo(dataset.scale());
        assertThat(targetLatencies).hasSize(dataset.scale());
        return new Verification(resolved, finalFailed, missing, duplicateAttempts, reconciliationRows, true);
    }

    private long countOrders(long firstId, long lastId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.pay_order WHERE payment_order_id BETWEEN ? AND ?",
                Long.class, firstId, lastId);
    }

    private long countWhere(Dataset dataset, String predicate) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.pay_order WHERE payment_order_id BETWEEN ? AND ? AND " + predicate,
                Long.class, dataset.firstId(), dataset.lastId());
    }

    private double percentileMs(ConcurrentLinkedQueue<Long> values, double percentile) {
        assertThat(values).isNotEmpty();
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index) / 1_000_000.0;
    }

    private double elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000.0;
    }

    private record Dataset(long firstId, long lastId, int scale) {
    }

    private record Verification(
            long resolvedCount,
            long finalFailedOrderCount,
            long missingCount,
            long duplicateAttemptCount,
            long reconciliationRowCount,
            boolean controlTotalsMatched
    ) {
    }

    private static final class ResolvingGateway implements PremiumPaymentGateway {
        private final CountDownLatch firstCalls;
        private final AtomicInteger inquiryCalls = new AtomicInteger();

        private ResolvingGateway(int workers) {
            this.firstCalls = new CountDownLatch(workers);
        }

        @Override
        public String providerCode() {
            return "FAKE";
        }

        @Override
        public GatewayPaymentResult confirm(ConfirmCommand command) {
            throw new AssertionError("The large reconciliation benchmark must not submit payment approvals.");
        }

        @Override
        public GatewayPaymentResult inquire(InquiryCommand command) {
            inquiryCalls.incrementAndGet();
            if (firstCalls.getCount() > 0) {
                firstCalls.countDown();
                try {
                    if (!firstCalls.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("competing workers did not reach the provider");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("provider barrier interrupted", interrupted);
                }
            }
            return GatewayPaymentResult.failed(command.providerPaymentKey(), "SYNTHETIC_DECLINED");
        }
    }

    private static final class TimedPaymentService extends PaymentService {
        private final ConcurrentLinkedQueue<Long> targetLatencies;

        private TimedPaymentService(
                JdbcPaymentRepository paymentRepository,
                JdbcPolicyRepository policyRepository,
                PremiumPaymentGateway gateway,
                DataSourceTransactionManager transactions,
                ObjectMapper objectMapper,
                ConcurrentLinkedQueue<Long> targetLatencies
        ) {
            super(paymentRepository, policyRepository, gateway, transactions, objectMapper);
            this.targetLatencies = targetLatencies;
        }

        @Override
        public PaymentOrderResponse reconcile(Long paymentOrderId) {
            long startedAt = System.nanoTime();
            try {
                return super.reconcile(paymentOrderId);
            } finally {
                targetLatencies.add(System.nanoTime() - startedAt);
            }
        }
    }
}
