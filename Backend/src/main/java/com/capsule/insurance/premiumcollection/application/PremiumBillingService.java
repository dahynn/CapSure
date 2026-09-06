package com.capsule.insurance.premiumcollection.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.premiumcollection.dto.PremiumBillingRunResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PremiumBillingService {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private static final int CHUNK_SIZE = 20;
    private static final List<String> BILLABLE_POLICY_STATUSES =
            List.of("ACTIVE", "GRACE", "CHANGE_SCHEDULED");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final int graceDays;

    @Autowired
    public PremiumBillingService(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            @Value("${operations.premium-billing.grace-days:14}") int graceDays
    ) {
        this(jdbc, transactionManager, Clock.systemUTC(), graceDays);
    }

    PremiumBillingService(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            Clock clock,
            int graceDays
    ) {
        if (graceDays < 1 || graceDays > 365) {
            throw new IllegalArgumentException("graceDays must be 1..365");
        }
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.graceDays = graceDays;
    }

    public PremiumBillingRunResponse run(
            String instanceKey,
            LocalDate billingCycle,
            Long actorUserId,
            String reason
    ) {
        validate(instanceKey, billingCycle, reason);
        long runId = Objects.requireNonNull(transaction.execute(status -> initializeRun(
                instanceKey, billingCycle, actorUserId, reason
        )));
        return resume(runId, actorUserId, reason);
    }

    public PremiumBillingRunResponse resume(long runId, Long actorUserId, String reason) {
        validateReason(reason);
        get(runId);
        jdbc.update("""
                INSERT INTO ops_premium_billing_attempt(run_id, actor_user_id, reason)
                VALUES (?, ?, ?)
                """, runId, actorUserId, reason);
        return resume(runId);
    }

    public PremiumBillingRunResponse resume(long runId) {
        try {
            while (Boolean.TRUE.equals(transaction.execute(status -> processChunk(runId)))) {
                // Each iteration commits a bounded checkpoint.
            }
        } catch (RuntimeException exception) {
            transaction.executeWithoutResult(status -> jdbc.update("""
                    UPDATE ops_premium_billing_run
                    SET status = 'FAILED', error_reason = ?, finished_at = NOW()
                    WHERE run_id = ? AND status <> 'COMPLETED'
                    """, exception.getClass().getSimpleName(), runId));
            throw exception;
        }
        return get(runId);
    }

    public PremiumBillingRunResponse get(long runId) {
        return jdbc.query("""
                SELECT r.*,
                       COUNT(t.policy_id) AS targets,
                       COUNT(t.outcome) AS processed,
                       COUNT(*) FILTER (WHERE t.outcome = 'CREATED') AS created,
                       COUNT(*) FILTER (WHERE t.outcome = 'EXISTING') AS existing,
                       COUNT(*) FILTER (WHERE t.outcome = 'INELIGIBLE') AS ineligible
                FROM ops_premium_billing_run r
                LEFT JOIN ops_premium_billing_target t USING(run_id)
                WHERE r.run_id = ?
                GROUP BY r.run_id
                """, (resultSet, rowNumber) -> {
            long targets = resultSet.getLong("targets");
            long processed = resultSet.getLong("processed");
            long created = resultSet.getLong("created");
            long existing = resultSet.getLong("existing");
            long ineligible = resultSet.getLong("ineligible");
            return new PremiumBillingRunResponse(
                    runId,
                    resultSet.getString("instance_key"),
                    resultSet.getObject("billing_cycle", LocalDate.class),
                    resultSet.getObject("business_date", LocalDate.class),
                    resultSet.getString("status"),
                    targets,
                    processed,
                    created,
                    existing,
                    ineligible,
                    targets - processed,
                    processed == created + existing + ineligible && targets >= processed,
                    resultSet.getString("error_reason")
            );
        }, runId).stream().findFirst().orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "정기 보험료 채권 실행을 찾을 수 없습니다."));
    }

    public List<PremiumBillingRunResponse> recent() {
        return jdbc.query("""
                SELECT run_id FROM ops_premium_billing_run ORDER BY run_id DESC LIMIT 8
                """, (resultSet, rowNumber) -> resultSet.getLong(1)).stream().map(this::get).toList();
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }

    private long initializeRun(
            String instanceKey,
            LocalDate billingCycle,
            Long actorUserId,
            String reason
    ) {
        List<Long> created = jdbc.query("""
                INSERT INTO ops_premium_billing_run(
                    instance_key, billing_cycle, business_date, actor_user_id, reason, status
                ) VALUES (?, ?, ?, ?, ?, 'RUNNING')
                ON CONFLICT (instance_key) DO NOTHING
                RETURNING run_id
                """, (resultSet, rowNumber) -> resultSet.getLong(1),
                instanceKey, billingCycle, today(), actorUserId, reason);

        if (created.isEmpty()) {
            ExistingRun existing = jdbc.query("""
                    SELECT run_id, billing_cycle FROM ops_premium_billing_run WHERE instance_key = ?
                    """, (resultSet, rowNumber) -> new ExistingRun(
                    resultSet.getLong("run_id"),
                    resultSet.getObject("billing_cycle", LocalDate.class)
            ), instanceKey).stream().findFirst().orElseThrow();
            if (!existing.billingCycle().equals(billingCycle)) {
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "같은 실행 키를 다른 청구월에 사용할 수 없습니다."
                );
            }
            return existing.runId();
        }

        long runId = created.getFirst();
        YearMonth cycle = YearMonth.from(billingCycle);
        List<BillingCandidate> candidates = jdbc.query("""
                SELECT p.policy_id,
                       (p.activated_at AT TIME ZONE 'Asia/Seoul')::DATE AS activated_on,
                       (v.snapshot_json #>> '{quote,monthlyPremium}')::NUMERIC AS amount_due,
                       COALESCE(v.snapshot_json #>> '{quote,currencyCode}', 'KRW') AS currency_code
                FROM ins_policy p
                JOIN LATERAL (
                    SELECT snapshot_json
                    FROM ins_policy_version
                    WHERE policy_id = p.policy_id
                    ORDER BY version DESC
                    LIMIT 1
                ) v ON TRUE
                WHERE p.status IN ('ACTIVE', 'GRACE', 'CHANGE_SCHEDULED')
                  AND p.activated_at IS NOT NULL
                  AND v.snapshot_json #>> '{quote,monthlyPremium}' IS NOT NULL
                  AND (v.snapshot_json #>> '{quote,monthlyPremium}')::NUMERIC > 0
                ORDER BY p.policy_id
                """, (resultSet, rowNumber) -> new BillingCandidate(
                resultSet.getLong("policy_id"),
                resultSet.getObject("activated_on", LocalDate.class),
                resultSet.getBigDecimal("amount_due"),
                resultSet.getString("currency_code")
        ));

        for (BillingCandidate candidate : candidates) {
            if (!YearMonth.from(candidate.activatedOn()).isBefore(cycle)) {
                continue;
            }
            LocalDate dueDate = cycle.atDay(Math.min(candidate.activatedOn().getDayOfMonth(), cycle.lengthOfMonth()));
            jdbc.update("""
                    INSERT INTO ops_premium_billing_target(
                        run_id, policy_id, amount_due, currency_code, due_date, grace_ends_on
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """, runId, candidate.policyId(), candidate.amountDue(), candidate.currencyCode(),
                    dueDate, dueDate.plusDays(graceDays));
        }
        return runId;
    }

    private boolean processChunk(long runId) {
        List<LocalDate> cycles = jdbc.query("""
                SELECT billing_cycle
                FROM ops_premium_billing_run
                WHERE run_id = ? AND status <> 'COMPLETED'
                FOR UPDATE SKIP LOCKED
                """, (resultSet, rowNumber) -> resultSet.getObject(1, LocalDate.class), runId);
        if (cycles.isEmpty()) {
            return false;
        }
        LocalDate billingCycle = cycles.getFirst();
        jdbc.update("""
                UPDATE ops_premium_billing_run
                SET status = 'RUNNING', error_reason = NULL, finished_at = NULL
                WHERE run_id = ?
                """, runId);

        List<BillingTarget> targets = jdbc.query("""
                SELECT policy_id, amount_due, currency_code, due_date, grace_ends_on
                FROM ops_premium_billing_target
                WHERE run_id = ? AND outcome IS NULL
                ORDER BY policy_id
                LIMIT ?
                """, (resultSet, rowNumber) -> new BillingTarget(
                resultSet.getLong("policy_id"),
                resultSet.getBigDecimal("amount_due"),
                resultSet.getString("currency_code"),
                resultSet.getObject("due_date", LocalDate.class),
                resultSet.getObject("grace_ends_on", LocalDate.class)
        ), runId, CHUNK_SIZE);

        for (BillingTarget target : targets) {
            BillingOutcome outcome = createReceivable(target, billingCycle);
            jdbc.update("""
                    UPDATE ops_premium_billing_target
                    SET outcome = ?, premium_receivable_id = ?, processed_at = NOW()
                    WHERE run_id = ? AND policy_id = ?
                    """, outcome.outcome(), outcome.receivableId(), runId, target.policyId());
        }

        boolean pending = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(
                    SELECT 1 FROM ops_premium_billing_target WHERE run_id = ? AND outcome IS NULL
                )
                """, Boolean.class, runId));
        jdbc.update("""
                UPDATE ops_premium_billing_run
                SET status = ?, finished_at = CASE WHEN ? THEN NULL ELSE NOW() END
                WHERE run_id = ?
                """, pending ? "RUNNING" : "COMPLETED", pending, runId);
        return pending;
    }

    private BillingOutcome createReceivable(BillingTarget target, LocalDate billingCycle) {
        List<String> statuses = jdbc.query("""
                SELECT status FROM ins_policy WHERE policy_id = ? FOR UPDATE
                """, (resultSet, rowNumber) -> resultSet.getString(1), target.policyId());
        if (statuses.isEmpty() || !BILLABLE_POLICY_STATUSES.contains(statuses.getFirst())) {
            return new BillingOutcome("INELIGIBLE", null);
        }

        List<Long> existing = jdbc.query("""
                SELECT premium_receivable_id
                FROM ins_premium_receivable
                WHERE policy_id = ? AND billing_cycle = ?
                """, (resultSet, rowNumber) -> resultSet.getLong(1), target.policyId(), billingCycle);
        if (!existing.isEmpty()) {
            return new BillingOutcome("EXISTING", existing.getFirst());
        }

        Long receivableId = jdbc.queryForObject("""
                INSERT INTO ins_premium_receivable(
                    policy_id, billing_cycle, due_date, grace_ends_on,
                    amount_due, currency_code, status
                ) VALUES (?, ?, ?, ?, ?, ?, 'DUE')
                RETURNING premium_receivable_id
                """, Long.class, target.policyId(), billingCycle, target.dueDate(),
                target.graceEndsOn(), target.amountDue(), target.currencyCode());
        String cycleKey = billingCycle.toString().replace("-", "");
        jdbc.update("""
                INSERT INTO pay_collection_instruction(
                    premium_receivable_id, instruction_no, collection_method, amount,
                    provider, idempotency_key, status
                ) VALUES (?, ?, 'AUTO_DEBIT', ?, 'FAKE_PREMIUM_PAYMENT', ?, 'SCHEDULED')
                """, receivableId, "COL-RECURRING-" + target.policyId() + "-" + cycleKey,
                target.amountDue(), "auto-debit:" + receivableId);
        return new BillingOutcome("CREATED", receivableId);
    }

    private void validate(String instanceKey, LocalDate billingCycle, String reason) {
        if (instanceKey == null || instanceKey.isBlank() || instanceKey.length() > 150) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "150자 이내 실행 키가 필요합니다.");
        }
        validateReason(reason);
        if (billingCycle == null || billingCycle.getDayOfMonth() != 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "청구월은 해당 월의 1일이어야 합니다.");
        }
        if (billingCycle.isAfter(today().withDayOfMonth(1))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "미래 청구월의 채권은 미리 생성할 수 없습니다.");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "500자 이내 실행 사유가 필요합니다.");
        }
    }

    private record BillingCandidate(
            long policyId,
            LocalDate activatedOn,
            BigDecimal amountDue,
            String currencyCode
    ) {
    }

    private record BillingTarget(
            long policyId,
            BigDecimal amountDue,
            String currencyCode,
            LocalDate dueDate,
            LocalDate graceEndsOn
    ) {
    }

    private record BillingOutcome(String outcome, Long receivableId) {
    }

    private record ExistingRun(long runId, LocalDate billingCycle) {
    }
}
