package com.capsule.insurance.operations.recovery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capsule.insurance.operations.outbox.application.OutboxRelayService;
import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;
import com.capsule.insurance.operations.outbox.dto.OutboxRelayRunResponse;
import com.capsule.insurance.operations.reconciliation.application.PaymentReconciliationBatchService;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationRunOptions;
import com.capsule.insurance.operations.reconciliation.dto.PaymentReconciliationExecutionResponse;
import com.capsule.insurance.operations.recovery.dto.DlqRecoveryResponse;
import com.capsule.insurance.operations.recovery.dto.PaymentReconciliationRecoveryResponse;
import com.capsule.insurance.operations.recovery.infra.JdbcOperationsRecoveryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class OperationsRecoveryServiceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-03T08:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_operations_recovery_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static JdbcTemplate jdbcTemplate;

    private OutboxRelayService outboxRelayService;
    private PaymentReconciliationBatchService reconciliationBatchService;
    private OperationsRecoveryService service;
    private Long adminUserId;

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
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE public.usr_user RESTART IDENTITY CASCADE");
        outboxRelayService = mock(OutboxRelayService.class);
        reconciliationBatchService = mock(PaymentReconciliationBatchService.class);
        service = new OperationsRecoveryService(
                new JdbcOperationsRecoveryRepository(jdbcTemplate),
                outboxRelayService,
                reconciliationBatchService,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        adminUserId = jdbcTemplate.queryForObject("""
                INSERT INTO public.usr_user (email, name, phone, user_status, access_role)
                VALUES ('recovery-admin@capsure.test', '복구담당자', '01000000000', 'ACTIVE', 'ROLE_ADMIN')
                RETURNING user_id
                """, Long.class);
    }

    @Test
    @DisplayName("DLQ 재처리는 관리자와 사유, 감지부터 재투입까지 걸린 시간을 원장에 남긴다")
    void auditsDlqReplayRecoveryTime() {
        seedPendingDeadLetter("DLQ-AUDIT", NOW.minusSeconds(120));
        when(outboxRelayService.replay("DLQ-AUDIT", adminUserId, "연계 복구 확인 후 재처리"))
                .thenReturn(new OutboxReplayResponse(
                        "DLQ-AUDIT",
                        "PENDING",
                        "REPLAYED",
                        0,
                        adminUserId,
                        "연계 복구 확인 후 재처리",
                        NOW,
                        NOW
                ));
        when(outboxRelayService.relayEvent("DLQ-AUDIT"))
                .thenReturn(new OutboxRelayRunResponse(
                        "OUTBOX-RECOVERY-1",
                        1,
                        1,
                        0,
                        0,
                        NOW,
                        NOW
                ));

        DlqRecoveryResponse result = service.replayDlq(
                "DLQ-AUDIT",
                adminUserId,
                "연계 복구 확인 후 재처리"
        );

        assertThat(result.recovery().status()).isEqualTo("SUCCEEDED");
        assertThat(result.recovery().recoveryTimeMs()).isEqualTo(120000);
        assertThat(result.recovery().actionDurationMs()).isZero();
        assertThat(result.replay().replayStatus()).isEqualTo("REPLAYED");
        assertThat(result.relay().publishedCount()).isEqualTo(1);
        assertThat(actionValue("actor_user_id")).isEqualTo(adminUserId.toString());
        assertThat(actionValue("reason")).isEqualTo("연계 복구 확인 후 재처리");
        assertThat(actionValue("result_json ->> 'outboxStatus'")).isEqualTo("PENDING");
        assertThat(actionValue("result_json ->> 'publishedCount'")).isEqualTo("1");
    }

    @Test
    @DisplayName("수동 결제 대사는 복구 원장 ID로 배치 instance를 만들고 control total을 기록한다")
    void auditsManualPaymentReconciliation() {
        PaymentReconciliationExecutionResponse execution = new PaymentReconciliationExecutionResponse(
                41L,
                "PAYMENT_RECONCILIATION",
                "MANUAL-RECOVERY-1",
                1,
                "COMPLETED",
                NOW,
                25L,
                3,
                "worker",
                12,
                10,
                2,
                0,
                true,
                NOW,
                NOW,
                null
        );
        when(reconciliationBatchService.run(
                eq("MANUAL-RECOVERY-1"),
                any(PaymentReconciliationRunOptions.class)
        )).thenReturn(execution);

        PaymentReconciliationRecoveryResponse result = service.runPaymentReconciliation(
                adminUserId,
                "미확정 결제 12건 수동 대사",
                5,
                0
        );

        assertThat(result.recovery().status()).isEqualTo("SUCCEEDED");
        assertThat(result.recovery().targetId()).isEqualTo("41");
        assertThat(result.execution().processedCount()).isEqualTo(12);
        assertThat(actionValue("result_json ->> 'controlTotalMatched'")).isEqualTo("true");
        assertThat(actionValue("result_json ->> 'resolvedCount'")).isEqualTo("10");
        verify(reconciliationBatchService).run(
                eq("MANUAL-RECOVERY-1"),
                any(PaymentReconciliationRunOptions.class)
        );
    }

    @Test
    @DisplayName("수동 조치가 실패해도 실패 사유와 종료 시각은 복구 원장에 보존된다")
    void preservesFailedRecoveryAction() {
        when(reconciliationBatchService.run(anyString(), any(PaymentReconciliationRunOptions.class)))
                .thenThrow(new IllegalStateException("PG inquiry unavailable"));

        assertThatThrownBy(() -> service.runPaymentReconciliation(
                adminUserId,
                "PG 장애 확인",
                10,
                0
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PG inquiry unavailable");

        assertThat(actionValue("status")).isEqualTo("FAILED");
        assertThat(actionValue("error_reason")).contains("PG inquiry unavailable");
        assertThat(actionValue("completed_at IS NOT NULL")).isEqualTo("true");
    }

    private void seedPendingDeadLetter(String eventId, Instant detectedAt) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_event (
                    event_id, aggregate_type, aggregate_id, event_type,
                    payload_json, status, attempt_count, last_error
                ) VALUES (?, 'PAYMENT', '1', 'PAYMENT_STATUS_CHANGED',
                          '{}'::JSONB, 'DEAD_LETTER', 3, 'retry exhausted')
                """, eventId);
        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_dead_letter (
                    event_id, payload_json, error_reason, replay_status, created_at
                ) VALUES (?, '{}'::JSONB, 'retry exhausted', 'PENDING', ?)
                """, eventId, java.sql.Timestamp.from(detectedAt));
    }

    private String actionValue(String expression) {
        return jdbcTemplate.queryForObject(
                "SELECT (" + expression + ")::TEXT FROM public.ops_recovery_action ORDER BY recovery_action_id DESC LIMIT 1",
                String.class
        );
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
