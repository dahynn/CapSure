package com.capsule.insurance.operations.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.common.exception.GlobalExceptionHandler;
import com.capsule.insurance.operations.reconciliation.api.PaymentReconciliationOperationsController;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationInterruptedException;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationRunOptions;
import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;
import com.capsule.insurance.operations.reconciliation.infra.JdbcPaymentReconciliationJobRepository;
import com.capsule.insurance.payment.adapter.FakePremiumPaymentGateway;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.capsule.insurance.payment.infra.JdbcPaymentRepository;
import com.capsule.insurance.policy.infra.JdbcPolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PaymentReconciliationBatchServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_payment_reconciliation_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static DriverManagerDataSource dataSource;
    private static DataSourceTransactionManager transactionManager;
    private static JdbcTemplate jdbcTemplate;
    private static ObjectMapper objectMapper;
    private static FakePremiumPaymentGateway gateway;
    private static JdbcPaymentReconciliationJobRepository repository;

    private PaymentReconciliationBatchService service;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        createLegacySchema();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .cleanDisabled(true)
                .load()
                .migrate();

        dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        transactionManager = new DataSourceTransactionManager(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        objectMapper = new ObjectMapper();
        repository = new JdbcPaymentReconciliationJobRepository(jdbcTemplate);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE public.usr_user RESTART IDENTITY CASCADE
                """);
        jdbcTemplate.execute("""
                TRUNCATE TABLE public.ops_job_execution RESTART IDENTITY CASCADE
                """);
        jdbcTemplate.execute("""
                TRUNCATE TABLE public.ops_reconciliation RESTART IDENTITY
                """);
        gateway = new FakePremiumPaymentGateway();
        service = newService(gateway);
    }

    @Test
    @DisplayName("UNKNOWN 100건은 경쟁 작업자 두 개가 중복 없이 정확히 한 번씩 해결한다")
    void reconcilesOneHundredTargetsWithoutDuplicateClaims() throws Exception {
        Fixture fixture = createFixture("concurrent");
        List<Long> orderIds = insertUnknownOrders(fixture, "concurrent", 100, true);
        CoordinatedGateway coordinatedGateway = new CoordinatedGateway(gateway);
        PaymentReconciliationBatchService workerOne = newService(coordinatedGateway);
        PaymentReconciliationBatchService workerTwo = newService(coordinatedGateway);
        PaymentReconciliationRunOptions options =
                PaymentReconciliationRunOptions.production(5, Duration.ZERO);

        CompletableFuture<PaymentReconciliationExecutionResponse> first = CompletableFuture.supplyAsync(
                () -> workerOne.run("CONCURRENT-WORKER-1", options)
        );
        CompletableFuture<PaymentReconciliationExecutionResponse> second = CompletableFuture.supplyAsync(
                () -> workerTwo.run("CONCURRENT-WORKER-2", options)
        );
        PaymentReconciliationExecutionResponse firstResult = first.get(30, TimeUnit.SECONDS);
        PaymentReconciliationExecutionResponse secondResult = second.get(30, TimeUnit.SECONDS);

        assertThat(firstResult.status()).isEqualTo("COMPLETED");
        assertThat(secondResult.status()).isEqualTo("COMPLETED");
        assertThat(firstResult.processedCount()).isPositive();
        assertThat(secondResult.processedCount()).isPositive();
        assertThat(firstResult.processedCount() + secondResult.processedCount()).isEqualTo(100);
        assertThat(firstResult.resolvedCount() + secondResult.resolvedCount()).isEqualTo(100);
        assertThat(firstResult.stillUnknownCount() + secondResult.stillUnknownCount()).isZero();
        assertThat(firstResult.failedCount() + secondResult.failedCount()).isZero();
        assertThat(firstResult.controlTotalMatched()).isTrue();
        assertThat(secondResult.controlTotalMatched()).isTrue();

        assertThat(countOrders(orderIds, "FAILED")).isEqualTo(100);
        assertThat(sumReconciliationAttempts(orderIds)).isEqualTo(100);
        assertThat(countReconciliations(orderIds)).isEqualTo(100);
    }

    @Test
    @DisplayName("chunk 장애 뒤 같은 instance를 재실행하면 checkpoint 다음 주문부터 완료한다")
    void resumesAfterInjectedFailureFromDurableCheckpoint() throws Exception {
        Fixture fixture = createFixture("restart");
        List<Long> orderIds = insertUnknownOrders(fixture, "restart", 12, true);

        assertThatThrownBy(() -> service.run(
                "RESTARTABLE-INSTANCE",
                new PaymentReconciliationRunOptions(5, Duration.ZERO, 1)
        )).isInstanceOf(PaymentReconciliationInterruptedException.class)
                .hasMessageContaining("chunk 1");

        PaymentReconciliationExecutionResponse failed = repository
                .findLatest(PaymentReconciliationBatchService.JOB_NAME, "RESTARTABLE-INSTANCE")
                .map(execution -> service.getExecution(execution.jobExecutionId()))
                .orElseThrow();
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.processedCount()).isEqualTo(5);
        assertThat(failed.resolvedCount()).isEqualTo(5);
        assertThat(failed.processedChunks()).isEqualTo(1);
        assertThat(failed.lastPaymentOrderId()).isEqualTo(orderIds.get(4));

        PaymentReconciliationExecutionResponse completed = service.run(
                "RESTARTABLE-INSTANCE",
                PaymentReconciliationRunOptions.production(5, Duration.ZERO)
        );
        PaymentReconciliationExecutionResponse rerun = service.run(
                "RESTARTABLE-INSTANCE",
                PaymentReconciliationRunOptions.production(5, Duration.ZERO)
        );

        assertThat(completed.jobExecutionId()).isEqualTo(failed.jobExecutionId());
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.processedCount()).isEqualTo(12);
        assertThat(completed.resolvedCount()).isEqualTo(12);
        assertThat(completed.processedChunks()).isEqualTo(3);
        assertThat(completed.controlTotalMatched()).isTrue();
        assertThat(rerun.jobExecutionId()).isEqualTo(completed.jobExecutionId());
        assertThat(rerun.processedCount()).isEqualTo(12);
        assertThat(sumReconciliationAttempts(orderIds)).isEqualTo(12);
        assertThat(countReconciliations(orderIds)).isEqualTo(12);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentReconciliationOperationsController(service, null))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mockMvc.perform(get(
                        "/api/v1/ops/jobs/payment-reconciliation/executions/{jobExecutionId}",
                        completed.jobExecutionId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.processedCount").value(12))
                .andExpect(jsonPath("$.data.resolvedCount").value(12))
                .andExpect(jsonPath("$.data.controlTotalMatched").value(true));
    }

    @Test
    @DisplayName("PG가 계속 UNKNOWN이면 지수 백오프를 걸고 바로 다음 실행에서는 다시 선점하지 않는다")
    void backsOffStillUnknownTargets() {
        Fixture fixture = createFixture("unknown");
        List<Long> orderIds = insertUnknownOrders(fixture, "unknown", 3, false);

        PaymentReconciliationExecutionResponse first = service.run(
                "UNKNOWN-FIRST",
                PaymentReconciliationRunOptions.production(10, Duration.ZERO)
        );
        PaymentReconciliationExecutionResponse immediateRetry = service.run(
                "UNKNOWN-IMMEDIATE-RETRY",
                PaymentReconciliationRunOptions.production(10, Duration.ZERO)
        );

        assertThat(first.processedCount()).isEqualTo(3);
        assertThat(first.resolvedCount()).isZero();
        assertThat(first.stillUnknownCount()).isEqualTo(3);
        assertThat(first.failedCount()).isZero();
        assertThat(immediateRetry.processedCount()).isZero();
        assertThat(countOrders(orderIds, "UNKNOWN")).isEqualTo(3);
        assertThat(sumReconciliationAttempts(orderIds)).isEqualTo(3);
        assertThat(countReconciliations(orderIds)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.pay_order
                WHERE payment_order_id BETWEEN ? AND ?
                  AND reconciliation_available_at >= NOW() + INTERVAL '25 seconds'
                """, Integer.class, first(orderIds), last(orderIds))).isEqualTo(3);
    }

    private PaymentReconciliationBatchService newService(PremiumPaymentGateway selectedGateway) {
        PaymentService paymentService = new PaymentService(
                new JdbcPaymentRepository(jdbcTemplate),
                new JdbcPolicyRepository(jdbcTemplate, objectMapper),
                selectedGateway,
                transactionManager,
                objectMapper
        );
        return new PaymentReconciliationBatchService(
                repository,
                paymentService,
                transactionManager
        );
    }

    @Test
    void otherProviderApprovingOrdersRemainUnresolved() {
        var ids = insertUnknownOrders(createFixture("provider-boundary"), "provider-boundary", 1, false);
        jdbcTemplate.update("UPDATE pay_order SET status = 'APPROVING' WHERE payment_order_id = ?", ids.getFirst());
        jdbcTemplate.update("UPDATE pay_attempt SET provider = 'TOSS_PREMIUM_PAYMENT' WHERE payment_order_id = ?", ids.getFirst());
        var result = service.run("OTHER-PROVIDER", PaymentReconciliationRunOptions.production(5, Duration.ZERO));
        assertThat(result.resolvedCount()).isZero();
        assertThat(result.stillUnknownCount()).isEqualTo(1);
        assertThat(countOrders(ids, "APPROVING")).isEqualTo(1);
    }

    @Test
    void inquiryFailureStoresOnlySafeTypeAndActualProvider() {
        var ids = insertUnknownOrders(createFixture("private-failure"), "private-failure", 1, false);
        jdbcTemplate.update("UPDATE pay_attempt SET provider = 'TEST_PROVIDER' WHERE payment_order_id = ?", ids.getFirst());
        PremiumPaymentGateway failing = new PremiumPaymentGateway() {
            public String providerCode() { return "TEST_PROVIDER"; }
            public GatewayPaymentResult confirm(ConfirmCommand command) { throw new AssertionError("No charge in inquiry test"); }
            public GatewayPaymentResult inquire(InquiryCommand command) {
                throw new IllegalStateException("paymentKey=synthetic-private-key email=private@example.test");
            }
        };
        var result = newService(failing).run("SAFE-FAILURE", PaymentReconciliationRunOptions.production(5, Duration.ZERO));
        assertThat(result.failedCount()).isEqualTo(1);
        var row = jdbcTemplate.queryForMap("SELECT provider, details_json::text AS details FROM ops_reconciliation WHERE target_id = ?", ids.getFirst().toString());
        assertThat(row.get("provider")).isEqualTo("TEST_PROVIDER");
        assertThat(row.get("details").toString()).contains("IllegalStateException")
                .doesNotContain("synthetic-private-key", "private@example.test");
        assertThat(countOrders(ids, "UNKNOWN")).isEqualTo(1);
    }

    @Test
    @org.junit.jupiter.api.Tag("measurement")
    void measureFixedLatencyAndCheckpointRecovery() throws Exception {
        // A pooled datasource for BOTH configurations; this is not a pool optimization claim.
        var config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl()); config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword()); config.setMaximumPoolSize(8); config.setMinimumIdle(8);
        var samples = new java.util.ArrayList<java.util.Map<String, Object>>();
        try (var pool = new com.zaxxer.hikari.HikariDataSource(config)) {
            jdbcTemplate = new JdbcTemplate(pool);
            transactionManager = new DataSourceTransactionManager(pool);
            repository = new JdbcPaymentReconciliationJobRepository(jdbcTemplate);
            for (int repetition = 0; repetition <= 5; repetition++) {
                // Alternate order to reduce a systematic cache/thermal order advantage.
                int[] order = repetition % 2 == 0 ? new int[]{1, 2} : new int[]{2, 1};
                for (int workers : order) {
                    samples.add(measureSample(repetition, workers, false));
                    samples.add(measureSample(repetition, workers, true));
                }
            }
        }
        var report = new java.util.LinkedHashMap<String, Object>();
        report.put("schemaVersion", 1); report.put("measuredAt", java.time.Instant.now().toString());
        report.put("clock", "System.nanoTime"); report.put("java", System.getProperty("java.version"));
        report.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        report.put("databaseImage", "postgres:16-alpine"); report.put("connectionPoolSize", 8);
        report.put("comparison", "same implementation; workers=1 versus workers=2; not production throughput");
        report.put("samples", samples);
        var output = java.nio.file.Path.of(System.getProperty("capsure.measurement.output"));
        java.nio.file.Files.createDirectories(output.toAbsolutePath().getParent());
        java.nio.file.Files.writeString(output, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        System.out.println("CAPSURE_MEASUREMENT samples=" + samples.size() + " output=" + output);
    }

    private java.util.Map<String, Object> measureSample(int repetition, int workers, boolean interrupted) throws Exception {
        setUp();
        String prefix = "measured-" + repetition + "-" + workers + "-" + interrupted;
        var ids = insertUnknownOrders(createFixture(prefix), prefix, 40, true);
        var counts = new java.util.concurrent.ConcurrentHashMap<String, AtomicInteger>();
        var durations = new java.util.concurrent.ConcurrentLinkedQueue<Double>();
        PremiumPaymentGateway delayed = new PremiumPaymentGateway() {
            public GatewayPaymentResult confirm(ConfirmCommand command) { throw new AssertionError("Never charge during measurement"); }
            public GatewayPaymentResult inquire(InquiryCommand command) {
                long start = System.nanoTime();
                counts.computeIfAbsent(command.providerPaymentKey(), key -> new AtomicInteger()).incrementAndGet();
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                durations.add((System.nanoTime() - start) / 1_000_000.0);
                int index = Integer.parseInt(command.providerPaymentKey().substring(command.providerPaymentKey().lastIndexOf('-') + 1));
                return !interrupted && index % 4 == 0
                        ? GatewayPaymentResult.unknown(command.providerPaymentKey(), "FIXED_UNKNOWN")
                        : GatewayPaymentResult.failed(command.providerPaymentKey(), "FIXED_DECLINED");
            }
        };
        long start = System.nanoTime();
        double downtimeMs = 0;
        double recoveryMs = 0;
        if (interrupted) {
            runMeasuredWorkers(prefix, workers, delayed, 2 / workers);
            assertThat(jdbcTemplate.queryForObject("SELECT SUM(processed_count) FROM ops_job_execution", Long.class)).isEqualTo(10);
            long stopped = System.nanoTime();
            Thread.sleep(100); // Real injected application downtime, never a fixed Clock offset.
            long resumed = System.nanoTime();
            downtimeMs = (resumed - stopped) / 1_000_000.0;
            runMeasuredWorkers(prefix, workers, delayed, null);
            recoveryMs = (System.nanoTime() - resumed) / 1_000_000.0;
        } else {
            runMeasuredWorkers(prefix, workers, delayed, null);
        }
        double processingMs = (System.nanoTime() - start) / 1_000_000.0;
        int failed = countOrders(ids, "FAILED");
        int unknown = countOrders(ids, "UNKNOWN");
        assertThat(failed).isEqualTo(interrupted ? 40 : 30);
        assertThat(unknown).isEqualTo(interrupted ? 0 : 10);
        assertThat(countOrders(ids, "PAID")).isZero();
        assertThat(countReconciliations(ids)).isEqualTo(40);
        assertThat(sumReconciliationAttempts(ids)).isEqualTo(40);
        assertThat(counts).hasSize(40);
        assertThat(counts.values()).allSatisfy(value -> assertThat(value.get()).isEqualTo(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ops_job_execution WHERE status <> 'COMPLETED' OR processed_count <> resolved_count + still_unknown_count + failed_count", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT SUM(processed_count) FROM ops_job_execution", Long.class)).isEqualTo(40);
        // Same instance replay is a no-op, including after a reconstructed service resumes.
        runMeasuredWorkers(prefix, workers, delayed, null);
        assertThat(counts.values()).allSatisfy(value -> assertThat(value.get()).isEqualTo(1));
        var sample = new java.util.LinkedHashMap<String, Object>();
        sample.put("scenario", interrupted ? "checkpoint-recovery" : "mixed-provider-results");
        sample.put("repetition", repetition); sample.put("warmup", repetition == 0);
        sample.put("workers", workers); sample.put("orders", 40); sample.put("chunkSize", 5);
        sample.put("configuredPgDelayMs", 10); sample.put("elapsedMs", processingMs);
        sample.put("injectedDowntimeMs", downtimeMs); sample.put("recoveryAfterResumeMs", recoveryMs);
        sample.put("paid", 0); sample.put("failed", failed); sample.put("unknown", unknown);
        sample.put("inquiryCalls", 40); sample.put("duplicates", 0); sample.put("controlTotalMatched", true);
        sample.put("pgCallElapsedMs", new java.util.ArrayList<>(durations));
        return sample;
    }

    private void runMeasuredWorkers(String prefix, int workers, PremiumPaymentGateway delayed, Integer failAfterChunks) throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(workers)) {
            CountDownLatch ready = new CountDownLatch(workers);
            CountDownLatch release = new CountDownLatch(1);
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int worker = 0; worker < workers; worker++) {
                String instance = prefix + "-worker-" + worker;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try { if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start barrier timeout"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                    try {
                        newService(delayed).run(instance, new PaymentReconciliationRunOptions(5, Duration.ZERO, failAfterChunks));
                        if (failAfterChunks != null) throw new AssertionError("Expected interruption");
                    } catch (PaymentReconciliationInterruptedException e) {
                        if (failAfterChunks == null) throw e;
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); release.countDown();
            for (var future : futures) future.get(60, TimeUnit.SECONDS);
        }
    }

    private Fixture createFixture(String suffix) {
        Long userId = jdbcTemplate.queryForObject("""
                INSERT INTO public.usr_user (email, name, phone, user_status)
                VALUES (?, '배치', '01000000000', 'ACTIVE')
                RETURNING user_id
                """, Long.class, suffix + "@capsure.test");
        Long productVersionId = jdbcTemplate.queryForObject("""
                SELECT product_version_id
                FROM public.ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = '1.0.0'
                """, Long.class);
        String termsHash = jdbcTemplate.queryForObject("""
                SELECT source_hash
                FROM public.ins_terms_document
                WHERE document_code = 'CAPSURE-DEMO-CANCER-TERMS'
                  AND document_version = '1.0.0'
                """, String.class);
        Long quoteId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_quote (
                    quote_no,
                    user_id,
                    product_version_id,
                    status,
                    monthly_premium,
                    snapshot_json,
                    terms_document_hash,
                    expires_at
                ) VALUES (?, ?, ?, 'USED', 29900.00, '{}'::JSONB, ?, NOW() + INTERVAL '1 day')
                RETURNING quote_id
                """, Long.class, "Q-" + suffix, userId, productVersionId, termsHash);
        Long applicationId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_application (
                    application_no,
                    quote_id,
                    applicant_user_id,
                    insured_user_id,
                    status,
                    submitted_at
                ) VALUES (?, ?, ?, ?, 'APPROVED', NOW())
                RETURNING application_id
                """, Long.class, "A-" + suffix, quoteId, userId, userId);
        Long policyId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_policy (
                    policy_no,
                    application_id,
                    policyholder_user_id,
                    insured_user_id,
                    beneficiary_user_id,
                    status
                ) VALUES (?, ?, ?, ?, ?, 'PENDING_INITIAL_PREMIUM')
                RETURNING policy_id
                """, Long.class, "P-" + suffix, applicationId, userId, userId, userId);
        return new Fixture(applicationId, policyId);
    }

    private List<Long> insertUnknownOrders(
            Fixture fixture,
            String prefix,
            int count,
            boolean settleAsFailed
    ) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    String paymentKey = "fake-timeout-" + prefix + "-" + index;
                    Long paymentOrderId = jdbcTemplate.queryForObject("""
                            INSERT INTO public.pay_order (
                                order_no,
                                business_key,
                                application_id,
                                policy_id,
                                purpose,
                                amount,
                                status,
                                idempotency_key,
                                expires_at,
                                reconciliation_available_at
                            ) VALUES (
                                ?, ?, ?, ?, 'INITIAL_PREMIUM', 29900.00, 'UNKNOWN', ?,
                                NOW() + INTERVAL '30 minutes',
                                NOW() - INTERVAL '10 minutes'
                            )
                            RETURNING payment_order_id
                            """,
                            Long.class,
                            "PAY-" + prefix + "-" + index,
                            "BIZ-" + prefix + "-" + index,
                            fixture.applicationId(),
                            fixture.policyId(),
                            "ORDER-IDEMP-" + prefix + "-" + index
                    );
                    jdbcTemplate.update("""
                            INSERT INTO public.pay_attempt (
                                payment_order_id,
                                attempt_no,
                                provider,
                                provider_payment_key,
                                idempotency_key,
                                status,
                                completed_at
                            ) VALUES (?, 1, 'FAKE', ?, ?, 'UNKNOWN', NOW())
                            """,
                            paymentOrderId,
                            paymentKey,
                            "ATTEMPT-IDEMP-" + prefix + "-" + index
                    );
                    if (settleAsFailed) {
                        gateway.settleAsFailed(paymentKey, "FAKE_BATCH_DECLINED");
                    }
                    return paymentOrderId;
                })
                .toList();
    }

    private int countOrders(List<Long> orderIds, String status) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.pay_order
                WHERE payment_order_id BETWEEN ? AND ?
                  AND status = ?
                """, Integer.class, first(orderIds), last(orderIds), status);
    }

    private long sumReconciliationAttempts(List<Long> orderIds) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(reconciliation_attempt_count), 0)
                FROM public.pay_order
                WHERE payment_order_id BETWEEN ? AND ?
                """, Long.class, first(orderIds), last(orderIds));
    }

    private int countReconciliations(List<Long> orderIds) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ops_reconciliation
                WHERE target_type = 'PAYMENT_ORDER'
                  AND target_id::BIGINT BETWEEN ? AND ?
                """, Integer.class, first(orderIds), last(orderIds));
    }

    private long first(List<Long> orderIds) {
        return orderIds.getFirst();
    }

    private long last(List<Long> orderIds) {
        return orderIds.getLast();
    }

    private static void createLegacySchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
             InputStream input = new ClassPathResource("db/schema/schema.sql").getInputStream();
             Statement statement = connection.createStatement()) {
            statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private record Fixture(Long applicationId, Long policyId) {
    }

    private static final class CoordinatedGateway implements PremiumPaymentGateway {

        private final PremiumPaymentGateway delegate;
        private final CountDownLatch firstTwoInquiries = new CountDownLatch(2);
        private final AtomicInteger coordinatedCalls = new AtomicInteger();

        private CoordinatedGateway(PremiumPaymentGateway delegate) {
            this.delegate = delegate;
        }

        @Override
        public GatewayPaymentResult confirm(ConfirmCommand command) {
            return delegate.confirm(command);
        }

        @Override
        public GatewayPaymentResult inquire(InquiryCommand command) {
            if (coordinatedCalls.getAndIncrement() < 2) {
                firstTwoInquiries.countDown();
                try {
                    if (!firstTwoInquiries.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("경쟁 작업자 동기화 시간이 초과되었습니다.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("경쟁 작업자 동기화가 중단되었습니다.", exception);
                }
            }
            return delegate.inquire(command);
        }
    }
}
