package com.capsule.insurance.operations.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.information.application.FinancialEventAuditProjector;
import com.capsule.insurance.information.application.PolicyTimelineService;
import com.capsule.insurance.information.infra.JdbcFinancialEventAuditRepository;
import com.capsule.insurance.operations.outbox.application.port.OutboxEventHandler;
import com.capsule.insurance.operations.outbox.dto.OutboxRelayRunResponse;
import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;
import com.capsule.insurance.operations.outbox.infra.JdbcOutboxRepository;
import com.capsule.insurance.policy.infra.JdbcPolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class OutboxRelayIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_outbox_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbcTemplate;
    private static JdbcOutboxRepository outboxRepository;
    private static JdbcFinancialEventAuditRepository auditRepository;
    private static FinancialEventAuditProjector projector;
    private static DataSourceTransactionManager transactionManager;

    @BeforeAll
    static void setUp() throws Exception {
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
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        outboxRepository = new JdbcOutboxRepository(jdbcTemplate);
        auditRepository = new JdbcFinancialEventAuditRepository(jdbcTemplate);
        projector = new FinancialEventAuditProjector(auditRepository, OBJECT_MAPPER);
    }

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.execute("TRUNCATE TABLE public.ops_financial_event_audit RESTART IDENTITY");
        jdbcTemplate.execute("TRUNCATE TABLE public.ops_outbox_dead_letter RESTART IDENTITY");
        jdbcTemplate.execute("TRUNCATE TABLE public.ops_outbox_event RESTART IDENTITY");
    }

    @Test
    @DisplayName("동시 worker 두 개가 100개 이벤트를 중복 없이 한 번씩 감사 원장에 투영한다")
    void relaysOneHundredEventsExactlyOnceWithConcurrentWorkers() throws Exception {
        for (int sequence = 1; sequence <= 100; sequence++) {
            insertEvent(
                    "CONCURRENT-" + sequence,
                    "PAYMENT_ORDER",
                    Integer.toString(sequence),
                    "PAYMENT_PAID",
                    "{\"sequence\":" + sequence + "}"
            );
        }

        OutboxRelayService firstWorker = service(projector);
        OutboxRelayService secondWorker = service(projector);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<OutboxRelayRunResponse> first = executor.submit(() -> {
                start.await();
                return firstWorker.relay(60);
            });
            Future<OutboxRelayRunResponse> second = executor.submit(() -> {
                start.await();
                return secondWorker.relay(60);
            });
            start.countDown();

            OutboxRelayRunResponse firstResult = first.get();
            OutboxRelayRunResponse secondResult = second.get();
            assertThat(firstResult.claimedCount() + secondResult.claimedCount()).isEqualTo(100);
            assertThat(firstResult.publishedCount() + secondResult.publishedCount()).isEqualTo(100);
        } finally {
            executor.shutdownNow();
        }

        assertThat(count("ops_outbox_event", "status = 'PUBLISHED'")).isEqualTo(100);
        assertThat(count("ops_financial_event_audit", "event_id LIKE 'CONCURRENT-%'"))
                .isEqualTo(100);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM (
                    SELECT event_id
                    FROM public.ops_financial_event_audit
                    GROUP BY event_id
                    HAVING COUNT(*) > 1
                ) duplicated
                """, Integer.class)).isZero();
    }

    @Test
    @DisplayName("일시 장애는 backoff 후 재시도하고 두 번째 시도에서 중복 없이 발행한다")
    void retriesTransientFailureAndPublishesOnSecondAttempt() {
        insertEvent(
                "RETRY-ONCE",
                "POLICY",
                "1",
                "POLICY_ACTIVATED",
                "{\"policyId\":null}"
        );
        AtomicInteger attempts = new AtomicInteger();
        OutboxEventHandler failsOnce = event -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("검증용 일시 장애");
            }
            projector.handle(event);
        };
        OutboxRelayService service = service(failsOnce);

        OutboxRelayRunResponse first = service.relay(1);
        assertThat(first.retryScheduledCount()).isEqualTo(1);
        assertThat(status("RETRY-ONCE")).isEqualTo("FAILED");
        assertThat(attemptCount("RETRY-ONCE")).isEqualTo(1);
        assertThat(count("ops_financial_event_audit", "event_id = 'RETRY-ONCE'")).isZero();

        makeAvailable("RETRY-ONCE");
        OutboxRelayRunResponse second = service.relay(1);

        assertThat(second.publishedCount()).isEqualTo(1);
        assertThat(status("RETRY-ONCE")).isEqualTo("PUBLISHED");
        assertThat(attemptCount("RETRY-ONCE")).isEqualTo(2);
        assertThat(count("ops_financial_event_audit", "event_id = 'RETRY-ONCE'")).isEqualTo(1);
    }

    @Test
    @DisplayName("세 번 실패한 이벤트는 DLQ로 이동하고 관리자 사유를 남긴 replay 후 복구한다")
    void movesToDeadLetterAndRecoversWithAuditedReplay() {
        insertEvent(
                "DLQ-RECOVERY",
                "CLAIM",
                "1",
                "CLAIM_BENEFIT_PAID",
                "{\"claimId\":1}"
        );
        OutboxRelayService failingService = service(event -> {
            throw new IllegalStateException("검증용 지속 장애");
        });

        for (int attempt = 1; attempt <= OutboxRelayService.MAX_ATTEMPTS; attempt++) {
            makeAvailable("DLQ-RECOVERY");
            OutboxRelayRunResponse result = failingService.relay(1);
            if (attempt < OutboxRelayService.MAX_ATTEMPTS) {
                assertThat(result.retryScheduledCount()).isEqualTo(1);
            } else {
                assertThat(result.deadLetterCount()).isEqualTo(1);
            }
        }

        assertThat(status("DLQ-RECOVERY")).isEqualTo("DEAD_LETTER");
        assertThat(attemptCount("DLQ-RECOVERY")).isEqualTo(3);
        assertThat(count("ops_outbox_dead_letter", "event_id = 'DLQ-RECOVERY'")).isEqualTo(1);

        Long adminUserId = insertUser("outbox-admin@capsure.test", "운영자", "010-9000-0001");
        OutboxReplayResponse replayed = failingService.replay(
                "DLQ-RECOVERY",
                adminUserId,
                "연계 시스템 복구 확인 후 수동 재처리"
        );

        assertThat(replayed.outboxStatus()).isEqualTo("PENDING");
        assertThat(replayed.replayStatus()).isEqualTo("REPLAYED");
        assertThat(replayed.attemptCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT replay_reason
                FROM public.ops_outbox_dead_letter
                WHERE event_id = 'DLQ-RECOVERY'
                """, String.class)).isEqualTo("연계 시스템 복구 확인 후 수동 재처리");

        OutboxRelayRunResponse recovered = service(projector).relayEvent("DLQ-RECOVERY");
        assertThat(recovered.publishedCount()).isEqualTo(1);
        assertThat(status("DLQ-RECOVERY")).isEqualTo("PUBLISHED");
        assertThat(attemptCount("DLQ-RECOVERY")).isEqualTo(1);
        assertThat(count("ops_financial_event_audit", "event_id = 'DLQ-RECOVERY'")).isEqualTo(1);
    }

    @Test
    @DisplayName("재시도해도 성공할 수 없는 payload는 첫 시도에 DLQ로 격리한다")
    void deadLettersNonRetryablePayloadImmediately() {
        insertEvent("INVALID-PAYLOAD", "POLICY", "1", "POLICY_ACTIVATED", "[]");

        OutboxRelayRunResponse result = service(projector).relay(1);

        assertThat(result.deadLetterCount()).isEqualTo(1);
        assertThat(status("INVALID-PAYLOAD")).isEqualTo("DEAD_LETTER");
        assertThat(attemptCount("INVALID-PAYLOAD")).isEqualTo(1);
        assertThat(count("ops_outbox_dead_letter", "event_id = 'INVALID-PAYLOAD'")).isEqualTo(1);
    }

    @Test
    @DisplayName("발행된 계약 이벤트는 계약자에게만 payload hash와 함께 타임라인으로 조회된다")
    void exposesProjectedTimelineOnlyToPolicyOwner() {
        Long ownerUserId = insertUser("timeline-owner@capsure.test", "계약자", "010-9000-0002");
        Long strangerUserId = insertUser("timeline-stranger@capsure.test", "다른사용자", "010-9000-0003");
        Long policyId = insertPendingPolicy(ownerUserId);
        insertEvent(
                "POLICY-TIMELINE-" + policyId,
                "POLICY",
                policyId.toString(),
                "POLICY_ACTIVATED",
                "{\"policyId\":" + policyId + ",\"policyVersion\":1}"
        );

        service(projector).relay(1);
        PolicyTimelineService timelineService = new PolicyTimelineService(
                new JdbcPolicyRepository(jdbcTemplate, OBJECT_MAPPER),
                auditRepository,
                OBJECT_MAPPER
        );

        var timeline = timelineService.get(ownerUserId, policyId);
        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).eventType()).isEqualTo("POLICY_ACTIVATED");
        assertThat(timeline.get(0).payload().path("policyId").asLong()).isEqualTo(policyId);
        assertThat(timeline.get(0).payloadHash()).matches("[0-9a-f]{64}");

        assertThatThrownBy(() -> timelineService.get(strangerUserId, policyId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    private static OutboxRelayService service(OutboxEventHandler handler) {
        return new OutboxRelayService(
                outboxRepository,
                List.of(handler),
                transactionManager,
                Clock.systemUTC()
        );
    }

    private static void insertEvent(
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadJson
    ) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_event (
                    event_id,
                    aggregate_type,
                    aggregate_id,
                    event_type,
                    payload_json
                ) VALUES (?, ?, ?, ?, CAST(? AS JSONB))
                """, eventId, aggregateType, aggregateId, eventType, payloadJson);
    }

    private static void makeAvailable(String eventId) {
        jdbcTemplate.update("""
                UPDATE public.ops_outbox_event
                SET available_at = NOW() - INTERVAL '1 second'
                WHERE event_id = ?
                """, eventId);
    }

    private static String status(String eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM public.ops_outbox_event WHERE event_id = ?",
                String.class,
                eventId
        );
    }

    private static int attemptCount(String eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM public.ops_outbox_event WHERE event_id = ?",
                Integer.class,
                eventId
        );
    }

    private static int count(String table, String condition) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public." + table + " WHERE " + condition,
                Integer.class
        );
    }

    private static Long insertPendingPolicy(Long userId) {
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
                    currency_code,
                    snapshot_json,
                    terms_document_hash,
                    expires_at
                ) VALUES (?, ?, ?, 'USED', 29900.00, 'KRW', '{}'::JSONB, ?, NOW() + INTERVAL '1 hour')
                RETURNING quote_id
                """, Long.class, "Q-TIMELINE-" + userId, userId, productVersionId, termsHash);
        Long applicationId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_application (
                    application_no,
                    quote_id,
                    applicant_user_id,
                    insured_user_id,
                    status,
                    disclosure_json,
                    submitted_at
                ) VALUES (?, ?, ?, ?, 'APPROVED', '{}'::JSONB, NOW())
                RETURNING application_id
                """, Long.class, "A-TIMELINE-" + userId, quoteId, userId, userId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_policy (
                    policy_no,
                    application_id,
                    policyholder_user_id,
                    insured_user_id,
                    beneficiary_user_id,
                    status
                ) VALUES (?, ?, ?, ?, ?, 'PENDING_INITIAL_PREMIUM')
                RETURNING policy_id
                """, Long.class, "P-TIMELINE-" + userId, applicationId, userId, userId, userId);
    }

    private static Long insertUser(String email, String name, String phone) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.usr_user (email, name, phone, user_status)
                VALUES (?, ?, ?, 'ACTIVE')
                RETURNING user_id
                """, Long.class, email, name, phone);
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
