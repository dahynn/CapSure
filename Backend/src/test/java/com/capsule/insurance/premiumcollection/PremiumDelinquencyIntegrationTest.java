package com.capsule.insurance.premiumcollection;

import static org.assertj.core.api.Assertions.*;

import com.capsule.insurance.premiumcollection.application.*;
import com.capsule.insurance.premiumcollection.dto.*;
import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import com.capsule.insurance.premiumcollection.infra.JdbcPremiumCollectionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class PremiumDelinquencyIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static JdbcTemplate jdbc;
    static DataSourceTransactionManager manager;
    static TransactionTemplate tx;
    JdbcPremiumCollectionRepository repository;
    PremiumDelinquencyService batch;
    PremiumCollectionService collections;

    @BeforeAll static void database() throws Exception {
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(ds);
        try (var input = new ClassPathResource("db/schema/schema.sql").getInputStream()) {
            jdbc.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        Flyway.configure().dataSource(ds).baselineOnMigrate(true).baselineVersion("0")
                .locations("classpath:db/migration").load().migrate();
        manager = new DataSourceTransactionManager(ds);
        tx = new TransactionTemplate(manager);
    }

    @BeforeEach void reset() {
        jdbc.execute("TRUNCATE usr_user RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE ops_premium_delinquency_run RESTART IDENTITY CASCADE");
        repository = new JdbcPremiumCollectionRepository(jdbc);
        on("2020-01-02");
    }

    void on(String day) { on(day, id -> true); }
    void on(String day, PremiumNoticeGateway notice) {
        batch = new PremiumDelinquencyService(jdbc, manager, notice,
                Clock.fixed(LocalDate.parse(day).atTime(12, 0).atZone(PremiumDelinquencyService.BUSINESS_ZONE).toInstant(), ZoneOffset.UTC), 14);
        collections = new PremiumCollectionService(repository, batch);
    }

    @Test void dueDateAndGraceBoundaryAreInclusive() {
        long p = policy(); var r = due(p, "2020-01-02");
        assertThat(run("due").targetCount()).isZero();
        assertThat(state(p)).isEqualTo("ACTIVE");
        on("2020-01-03"); run("overdue");
        assertThat(state(p)).isEqualTo("GRACE");
        assertThat(jdbc.queryForObject("SELECT effective_grace_ends_on FROM ins_premium_notice", LocalDate.class)).isEqualTo(LocalDate.parse("2020-01-17"));
        on("2020-01-16"); run("before-end"); assertThat(state(p)).isEqualTo("GRACE");
        on("2020-01-17"); run("end"); assertThat(state(p)).isEqualTo("GRACE");
        on("2020-01-18"); run("after-end"); assertThat(state(p)).isEqualTo("LAPSED");
        assertThat(jdbc.queryForObject("SELECT lapsed_at FROM ins_policy", java.sql.Timestamp.class).toInstant())
                .isEqualTo(Instant.parse("2020-01-17T15:00:00Z"));
        assertThat(repository.loadTimeline(8).items().getFirst().noticeStatus()).isEqualTo("SIMULATED_DELIVERED");
        assertThat(repository.loadTimeline(8).lapsedPolicyCount()).isEqualTo(1);
    }

    @Test void lateNoticeDoesNotImmediatelyLapseAndFreezesPolicy() {
        long p = policy(); due(p, "2020-01-01");
        on("2020-03-01"); run("late");
        assertThat(state(p)).isEqualTo("GRACE");
        assertThat(jdbc.queryForObject("SELECT effective_grace_ends_on FROM ins_premium_notice", LocalDate.class))
                .isEqualTo(LocalDate.parse("2020-03-15"));
        on("2020-03-02"); run("again");
        assertThat(count("ins_premium_notice")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempts FROM ins_premium_notice", Integer.class)).isEqualTo(1);
    }

    @Test void failedNoticeBlocksLapseAndNextRunRetriesWithFullGrace() {
        long p = policy(); due(p, "2020-01-01");
        on("2020-04-01", id -> false);
        assertThat(run("failed-notice").noticeFailedCount()).isEqualTo(1);
        assertThat(state(p)).isEqualTo("GRACE");
        on("2020-05-01"); run("retry-notice");
        assertThat(state(p)).isEqualTo("GRACE");
        assertThat(jdbc.queryForObject("SELECT attempts FROM ins_premium_notice", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT effective_grace_ends_on FROM ins_premium_notice", LocalDate.class)).isEqualTo(LocalDate.parse("2020-05-15"));
    }

    @Test void partialSettlementDoesNotClearArrearsOrCancelDebit() {
        long p = policy(); var r = due(p, "2020-01-01"); run("grace");
        var partial = settle(r, "40", "part");
        assertThat(partial.status()).isEqualTo("GRACE");
        assertThat(partial.collectionStatus()).isEqualTo("SCHEDULED");
        assertThat(state(p)).isEqualTo("GRACE");
        assertThat(settle(r, "60", "rest").status()).isEqualTo("SETTLED");
        assertThat(state(p)).isEqualTo("ACTIVE");
        assertThat(count("ins_policy_delinquency_history")).isEqualTo(2);
    }

    @Test void multipleCyclesMustAllBePaidBeforeRestoration() {
        long p = policy(); var first = due(p, "2019-12-01"); var second = due(p, "2020-01-01");
        run("two-cycles"); settle(first, "100", "first");
        assertThat(state(p)).isEqualTo("GRACE");
        settle(second, "100", "second"); assertThat(state(p)).isEqualTo("ACTIVE");
    }

    @Test void paymentAfterGraceDeadlineCannotReviveEvenBeforeDailyBatchRuns() {
        long p = policy(); var r = due(p, "2020-01-01"); run("notice");
        on("2020-01-17");
        settle(r, "100", "late-before-batch");
        assertThat(state(p)).isEqualTo("LAPSED");
        assertThat(count("pay_late_settlement_review")).isEqualTo(1);
        assertThat(count("ins_policy_delinquency_history")).isEqualTo(2);
    }

    @Test void paymentOnGraceDeadlineRestoresWithoutLapse() {
        long p = policy(); var r = due(p, "2020-01-01"); run("notice");
        on("2020-01-16"); settle(r, "100", "on-deadline");
        assertThat(state(p)).isEqualTo("ACTIVE");
        assertThat(count("pay_late_settlement_review")).isZero();
    }

    @Test void lateUnknownDebitIsPreservedAndSettlementNeverRevivesLapsedPolicy() {
        long p = policy(); var r = due(p, "2020-01-01");
        jdbc.update("UPDATE pay_collection_instruction SET status = 'UNKNOWN'");
        run("grace"); on("2020-01-17"); run("lapse");
        assertThat(state(p)).isEqualTo("LAPSED");
        assertThat(jdbc.queryForObject("SELECT status FROM pay_collection_instruction", String.class)).isEqualTo("UNKNOWN");
        tx.execute(s -> collections.captureAutomaticDebit(r.collectionInstructionId(), "late-auto"));
        tx.execute(s -> collections.captureAutomaticDebit(r.collectionInstructionId(), "late-auto"));
        assertThat(state(p)).isEqualTo("LAPSED");
        assertThat(count("pay_premium_settlement")).isEqualTo(1);
        assertThat(count("pay_late_settlement_review")).isEqualTo(1);
        assertThat(repository.loadTimeline(8).lateSettlementReviewCount()).isEqualTo(1);
    }

    @Test void repeatedKeysCannotDuplicateOrRedirectSettlement() {
        long p = policy(); var r = due(p, "2020-01-01");
        settle(r, "40", "same"); settle(r, "40", "same");
        assertThat(count("pay_premium_settlement")).isEqualTo(1);
        assertThatThrownBy(() -> settle(r, "60", "same")).hasMessageContaining("거래 키");
        assertThatThrownBy(() -> tx.execute(s -> collections.captureAutomaticDebit(r.collectionInstructionId(), "scheduled")))
                .hasMessageContaining("처리 중");
        assertThat(count("pay_premium_settlement")).isEqualTo(1);
    }

    @Test void prepaymentAndInFlightDebitStillCreateOneRefundCase() {
        long p = policy(); var r = due(p, "2020-01-01");
        jdbc.update("UPDATE pay_collection_instruction SET status = 'SUBMITTED'");
        assertThat(settle(r, "100", "instant").collectionStatus()).isEqualTo("CANCEL_REQUESTED");
        assertThat(repository.loadTimeline(8).items().getFirst().instructionStatus()).isEqualTo("CANCEL_REQUESTED");
        var captured = tx.execute(s -> collections.captureAutomaticDebit(r.collectionInstructionId(), "auto"));
        assertThat(captured.status()).isEqualTo("OVERPAID");
        tx.execute(s -> collections.createDuplicateDebitRefundCases());
        tx.execute(s -> collections.createDuplicateDebitRefundCases());
        assertThat(count("pay_refund_case")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT amount FROM pay_refund_case", BigDecimal.class)).isEqualByComparingTo("100");
    }

    @Test void twoWorkersAndRepeatedRunsDoNotDuplicate100NoticesOrTransitions() throws Exception {
        for (int i = 0; i < 100; i++) due(policy(), "2020-01-01");
        var results = parallel(() -> run("worker-a"), () -> run("worker-b"));
        for (var result : results) {
            assertThat(result.processedCount()).isEqualTo(100);
            assertThat(result.controlTotalMatched()).isTrue();
        }
        assertThat(run("worker-a").processedCount()).isEqualTo(100);
        assertThat(count("ins_premium_notice")).isEqualTo(100);
        assertThat(count("ins_policy_delinquency_history")).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT SUM(attempts) FROM ins_premium_notice", Long.class)).isEqualTo(100);
    }

    @Test void sameExecutionConcurrentRequestsAreSafe() throws Exception {
        for (int i = 0; i < 25; i++) due(policy(), "2020-01-01");
        parallel(() -> run("same-run"), () -> run("same-run"));
        assertThat(run("same-run").processedCount()).isEqualTo(25);
        assertThat(count("ops_premium_delinquency_run")).isEqualTo(1);
        assertThat(count("ins_policy_delinquency_history")).isEqualTo(25);
    }

    @Test void failedChunkRollsBackAndResumeKeepsCommittedCheckpoint() {
        for (int i = 0; i < 45; i++) due(policy(), "2020-01-01");
        AtomicInteger calls = new AtomicInteger();
        on("2020-01-02", id -> { if (calls.incrementAndGet() == 23) throw new IllegalStateException("injected"); return true; });
        assertThatThrownBy(() -> run("restart")).hasMessage("injected");
        var failed = batch.recent().getFirst();
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.processedCount()).isEqualTo(20);
        assertThat(failed.remainingCount()).isEqualTo(25);
        assertThat(count("ins_premium_notice")).isEqualTo(20);
        on("2020-01-02"); var done = batch.resume(failed.runId());
        assertThat(done.runId()).isEqualTo(failed.runId());
        assertThat(done.processedCount()).isEqualTo(45);
        assertThat(done.controlTotalMatched()).isTrue();
        assertThat(done.remainingCount()).isZero();
        assertThat(count("ins_premium_notice")).isEqualTo(45);
        assertThat(count("ins_policy_delinquency_history")).isEqualTo(45);
    }

    @Test void concurrentPaymentAndBatchProduceSerializableBalances() throws Exception {
        long p = policy(); var r = due(p, "2020-01-01");
        parallel(() -> { run("race"); return true; }, () -> { settle(r, "100", "race-payment"); return true; });
        assertThat(state(p)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT status FROM ins_premium_receivable", String.class)).isEqualTo("SETTLED");
        assertThat(count("pay_premium_settlement")).isEqualTo(1);
    }

    @Test void terminalPoliciesAreNotChangedAndPaidReceivablesAreNotNotified() {
        long p = policy(); due(p, "2020-01-01"); jdbc.update("UPDATE ins_policy SET status = 'CANCELED' WHERE policy_id = ?", p);
        long paid = policy(); settle(due(paid, "2020-01-01"), "100", "paid");
        run("excluded");
        assertThat(state(p)).isEqualTo("CANCELED"); assertThat(state(paid)).isEqualTo("ACTIVE");
        assertThat(count("ins_premium_notice")).isZero();
    }

    @Test void businessDateUsesSeoulMidnight() {
        var service = new PremiumDelinquencyService(jdbc, manager, id -> true,
                Clock.fixed(Instant.parse("2020-01-01T15:00:00Z"), ZoneOffset.UTC), 14);
        assertThat(service.today()).isEqualTo(LocalDate.parse("2020-01-02"));
    }

    DelinquencyRunResponse run(String key) { return batch.run(key, null, "합성 테스트"); }
    String state(long p) { return jdbc.queryForObject("SELECT status FROM ins_policy WHERE policy_id = ?", String.class, p); }
    long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    PremiumCollectionSnapshot settle(PremiumCollectionSnapshot r, String amount, String key) {
        return tx.execute(s -> collections.settleImmediately(new InstantSettlementRequest(r.premiumReceivableId(), new BigDecimal(amount), key, key)));
    }
    PremiumCollectionSnapshot due(long p, String date) {
        LocalDate d = LocalDate.parse(date);
        return tx.execute(s -> collections.createDue(new CreatePremiumReceivableRequest(p, d.withDayOfMonth(1), d, d.plusDays(14), new BigDecimal("100"))));
    }
    long policy() {
        String suffix = UUID.randomUUID().toString();
        Long u = jdbc.queryForObject("INSERT INTO usr_user(email, name, phone, user_status) VALUES (?, '합성 사용자', '01000000000', 'ACTIVE') RETURNING user_id", Long.class, suffix + "@capsure.test");
        Long q = jdbc.queryForObject("""
                INSERT INTO ins_quote(quote_no, user_id, product_version_id, status, monthly_premium, snapshot_json, terms_document_hash, expires_at)
                SELECT ?, ?, product_version_id, 'USED', 100, '{}'::jsonb, repeat('a', 64), NOW() + INTERVAL '1 day'
                FROM ins_product_version WHERE product_code = 'CAPSURE-DEMO-CANCER' RETURNING quote_id
                """, Long.class, "q" + suffix, u);
        Long a = jdbc.queryForObject("INSERT INTO ins_application(application_no, quote_id, applicant_user_id, insured_user_id, status) VALUES (?, ?, ?, ?, 'APPROVED') RETURNING application_id", Long.class, "a" + suffix, q, u, u);
        return jdbc.queryForObject("""
                INSERT INTO ins_policy(policy_no, application_id, policyholder_user_id, insured_user_id, beneficiary_user_id, status, activated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', '2019-01-01'::timestamptz) RETURNING policy_id
                """, Long.class, "p" + suffix, a, u, u, u);
    }
    <T> List<T> parallel(Callable<T> a, Callable<T> b) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<T> first = pool.submit(() -> { start.await(); return a.call(); });
            Future<T> second = pool.submit(() -> { start.await(); return b.call(); });
            start.countDown();
            return List.of(first.get(45, TimeUnit.SECONDS), second.get(45, TimeUnit.SECONDS));
        }
    }
}
