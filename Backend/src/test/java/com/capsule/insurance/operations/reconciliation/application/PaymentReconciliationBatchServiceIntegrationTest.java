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
        public GatewayPaymentResult inquire(String providerPaymentKey) {
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
            return delegate.inquire(providerPaymentKey);
        }
    }
}
