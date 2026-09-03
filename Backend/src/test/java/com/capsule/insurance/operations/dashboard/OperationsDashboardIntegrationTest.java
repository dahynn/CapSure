package com.capsule.insurance.operations.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.common.exception.GlobalExceptionHandler;
import com.capsule.insurance.operations.dashboard.api.OperationsDashboardController;
import com.capsule.insurance.operations.dashboard.application.OperationsDashboardService;
import com.capsule.insurance.operations.dashboard.infra.JdbcOperationsDashboardRepository;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class OperationsDashboardIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_operations_dashboard_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static JdbcTemplate jdbcTemplate;
    private static MockMvc mockMvc;

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

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        OperationsDashboardService service = new OperationsDashboardService(
                new JdbcOperationsDashboardRepository(jdbcTemplate)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OperationsDashboardController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @BeforeEach
    void resetLedgers() {
        jdbcTemplate.execute("TRUNCATE TABLE public.usr_user RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public.ops_job_execution RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public.ops_outbox_event RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public.ops_reconciliation RESTART IDENTITY");
    }

    @Test
    @DisplayName("운영 대시보드는 원장 집계와 최근 장애 이력을 한 응답으로 제공한다")
    void aggregatesOperationalLedgers() throws Exception {
        seedOutboxLedgers();
        seedJobLedgers();
        seedPaymentTargets();
        seedReconciliations();

        mockMvc.perform(get("/api/v1/ops/dashboard").param("recentLimit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallStatus").value("CRITICAL"))
                .andExpect(jsonPath("$.data.outbox.pendingCount").value(2))
                .andExpect(jsonPath("$.data.outbox.processingCount").value(0))
                .andExpect(jsonPath("$.data.outbox.publishedCount").value(1))
                .andExpect(jsonPath("$.data.outbox.failedCount").value(1))
                .andExpect(jsonPath("$.data.outbox.pendingDeadLetterCount").value(1))
                .andExpect(jsonPath("$.data.outbox.projectedAuditCount").value(2))
                .andExpect(jsonPath("$.data.reconciliation.waitingOrderCount").value(2))
                .andExpect(jsonPath("$.data.reconciliation.dueOrderCount").value(1))
                .andExpect(jsonPath("$.data.reconciliation.lockedOrderCount").value(1))
                .andExpect(jsonPath("$.data.reconciliation.totalExecutionCount").value(2))
                .andExpect(jsonPath("$.data.reconciliation.failedLatestExecutionCount").value(1))
                .andExpect(jsonPath("$.data.reconciliation.processedCount").value(12))
                .andExpect(jsonPath("$.data.reconciliation.resolvedCount").value(9))
                .andExpect(jsonPath("$.data.reconciliation.stillUnknownCount").value(1))
                .andExpect(jsonPath("$.data.reconciliation.failedCount").value(2))
                .andExpect(jsonPath("$.data.recentJobs.length()").value(2))
                .andExpect(jsonPath("$.data.deadLetters.length()").value(1))
                .andExpect(jsonPath("$.data.deadLetters[0].eventId").value("event-dead"))
                .andExpect(jsonPath("$.data.recentReconciliations.length()").value(2))
                .andExpect(jsonPath("$.data.recentReconciliations[0].result").value("FAILED"));
    }

    @Test
    @DisplayName("처리할 원장이 없으면 운영 상태는 정상이다")
    void returnsHealthyForEmptyLedgers() throws Exception {
        mockMvc.perform(get("/api/v1/ops/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.outbox.pendingCount").value(0))
                .andExpect(jsonPath("$.data.reconciliation.waitingOrderCount").value(0))
                .andExpect(jsonPath("$.data.recentJobs").isEmpty())
                .andExpect(jsonPath("$.data.deadLetters").isEmpty())
                .andExpect(jsonPath("$.data.recentReconciliations").isEmpty());
    }

    @Test
    @DisplayName("최근 이력 조회 건수는 운영 API 허용 범위를 벗어날 수 없다")
    void rejectsOutOfRangeRecentLimit() throws Exception {
        mockMvc.perform(get("/api/v1/ops/dashboard").param("recentLimit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    private void seedOutboxLedgers() {
        insertOutbox("event-pending-1", "PENDING", null);
        insertOutbox("event-pending-2", "PENDING", null);
        insertOutbox("event-published", "PUBLISHED", null);
        insertOutbox("event-failed", "FAILED", "relay timeout");
        insertOutbox("event-dead", "DEAD_LETTER", "retry exhausted");

        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_dead_letter (
                    event_id, payload_json, error_reason, replay_status
                ) VALUES ('event-dead', '{}'::JSONB, 'retry exhausted', 'PENDING')
                """);
        jdbcTemplate.update("""
                INSERT INTO public.ops_financial_event_audit (
                    event_id, aggregate_type, aggregate_id, event_type,
                    payload_json, payload_hash, occurred_at
                ) VALUES
                    ('audit-1', 'PAYMENT', '1', 'PAYMENT_PAID', '{}'::JSONB, ?, NOW()),
                    ('audit-2', 'CLAIM', '2', 'CLAIM_PAID', '{}'::JSONB, ?, NOW())
                """, "a".repeat(64), "b".repeat(64));
    }

    private void insertOutbox(String eventId, String status, String lastError) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_event (
                    event_id, aggregate_type, aggregate_id, event_type,
                    payload_json, status, last_error
                ) VALUES (?, 'PAYMENT', '1', 'PAYMENT_STATUS_CHANGED', '{}'::JSONB, ?, ?)
                """, eventId, status, lastError);
    }

    private void seedJobLedgers() {
        jdbcTemplate.update("""
                INSERT INTO public.ops_job_execution (
                    job_name, instance_key, execution_no, status,
                    processed_count, resolved_count, still_unknown_count, failed_count,
                    finished_at
                ) VALUES
                    ('PAYMENT_RECONCILIATION', 'PAY-RUN-1', 1, 'COMPLETED', 10, 8, 1, 1, NOW() - INTERVAL '2 minutes'),
                    ('PAYMENT_RECONCILIATION', 'PAY-RUN-2', 1, 'FAILED', 2, 1, 0, 1, NOW() - INTERVAL '1 minute'),
                    ('CATALOG_IMPORT', 'CATALOG-RUN-1', 1, 'COMPLETED', 0, 0, 0, 0, NOW())
                """);
    }

    private void seedPaymentTargets() {
        Long userId = jdbcTemplate.queryForObject("""
                INSERT INTO public.usr_user (email, name, phone, user_status)
                VALUES ('ops-dashboard@capsure.test', '운영자', '01000000000', 'ACTIVE')
                RETURNING user_id
                """, Long.class);
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
                    quote_no, user_id, product_version_id, status, monthly_premium,
                    snapshot_json, terms_document_hash, expires_at
                ) VALUES ('Q-OPS-DASH', ?, ?, 'USED', 29900.00, '{}'::JSONB, ?, NOW() + INTERVAL '1 day')
                RETURNING quote_id
                """, Long.class, userId, productVersionId, termsHash);
        Long applicationId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_application (
                    application_no, quote_id, applicant_user_id, insured_user_id, status, submitted_at
                ) VALUES ('A-OPS-DASH', ?, ?, ?, 'APPROVED', NOW())
                RETURNING application_id
                """, Long.class, quoteId, userId, userId);

        jdbcTemplate.update("""
                INSERT INTO public.pay_order (
                    order_no, business_key, application_id, purpose, amount, status,
                    idempotency_key, reconciliation_available_at
                ) VALUES
                    ('PAY-OPS-DUE', 'BIZ-OPS-DUE', ?, 'INITIAL_PREMIUM', 29900.00,
                     'UNKNOWN', 'IDEMP-OPS-DUE', NOW() - INTERVAL '10 minutes'),
                    ('PAY-OPS-LOCKED', 'BIZ-OPS-LOCKED', ?, 'INITIAL_PREMIUM', 29900.00,
                     'APPROVING', 'IDEMP-OPS-LOCKED', NOW() + INTERVAL '10 minutes')
                """, applicationId, applicationId);
        jdbcTemplate.update("""
                UPDATE public.pay_order
                SET reconciliation_locked_at = NOW(),
                    reconciliation_locked_by = 'OPS-WORKER'
                WHERE order_no = 'PAY-OPS-LOCKED'
                """);
    }

    private void seedReconciliations() {
        jdbcTemplate.update("""
                INSERT INTO public.ops_reconciliation (
                    target_type, target_id, provider, local_status, provider_status,
                    result, details_json, executed_at
                ) VALUES
                    ('PAYMENT_ORDER', '1', 'FAKE', 'UNKNOWN', 'PAID', 'CORRECTED', '{}'::JSONB, NOW() - INTERVAL '1 minute'),
                    ('PAYMENT_ORDER', '2', 'FAKE', 'UNKNOWN', 'INQUIRY_ERROR', 'FAILED', '{}'::JSONB, NOW())
                """);
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
}
