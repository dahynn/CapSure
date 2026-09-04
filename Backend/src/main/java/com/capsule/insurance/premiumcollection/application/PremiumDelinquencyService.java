package com.capsule.insurance.premiumcollection.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.premiumcollection.dto.DelinquencyRunResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
public class PremiumDelinquencyService {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PremiumNoticeGateway notices;
    private final Clock clock;
    private final int graceDays;

    @Autowired
    public PremiumDelinquencyService(JdbcTemplate jdbc, PlatformTransactionManager manager,
            PremiumNoticeGateway notices,
            @Value("${operations.premium-delinquency.simulated-grace-days:14}") int graceDays) {
        this(jdbc, manager, notices, Clock.systemUTC(), graceDays);
    }

    public PremiumDelinquencyService(JdbcTemplate jdbc, PlatformTransactionManager manager,
            PremiumNoticeGateway notices, Clock clock, int graceDays) {
        if (graceDays < 1 || graceDays > 365) throw new IllegalArgumentException("graceDays must be 1..365");
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
        this.notices = notices;
        this.clock = clock;
        this.graceDays = graceDays;
    }

    public DelinquencyRunResponse run(String key, Long actor, String reason) {
        if (key == null || key.isBlank() || key.length() > 150 || reason == null
                || reason.isBlank() || reason.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "실행 키와 500자 이내 사유가 필요합니다.");
        }
        long id = Objects.requireNonNull(tx.execute(s -> {
            List<Long> created = jdbc.query("""
                    INSERT INTO ops_premium_delinquency_run(instance_key, business_date, actor_user_id, reason, status)
                    VALUES (?, ?, ?, ?, 'RUNNING') ON CONFLICT (instance_key) DO NOTHING RETURNING run_id
                    """, (rs, n) -> rs.getLong(1), key, today(), actor, reason);
            if (!created.isEmpty()) {
                jdbc.update("""
                        INSERT INTO ops_premium_delinquency_target(run_id, policy_id)
                        SELECT ?, p.policy_id FROM ins_policy p
                        WHERE p.status IN ('ACTIVE', 'GRACE') AND EXISTS (
                            SELECT 1 FROM ins_premium_receivable r WHERE r.policy_id = p.policy_id
                              AND r.due_date < ?) ORDER BY p.policy_id
                        """, created.getFirst(), today());
                return created.getFirst();
            }
            return jdbc.queryForObject("SELECT run_id FROM ops_premium_delinquency_run WHERE instance_key = ?", Long.class, key);
        }));
        return resume(id, actor, reason);
    }

    public DelinquencyRunResponse resume(long id, Long actor, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "500자 이내 재개 사유가 필요합니다.");
        }
        get(id);
        jdbc.update("INSERT INTO ops_premium_delinquency_attempt(run_id, actor_user_id, reason) VALUES (?, ?, ?)", id, actor, reason);
        return resume(id);
    }

    public DelinquencyRunResponse resume(long id) {
        // Each chunk owns the execution row until commit. Crashes roll back the current chunk;
        // RUNNING is safely resumable without a time-based lease or a high-water-mark skip.
        try {
            while (Boolean.TRUE.equals(tx.execute(s -> processChunk(id)))) { /* bounded transactions */ }
        } catch (RuntimeException error) {
            tx.executeWithoutResult(s -> jdbc.update("""
                    UPDATE ops_premium_delinquency_run SET status = 'FAILED', error_reason = ?, finished_at = NOW()
                    WHERE run_id = ? AND status <> 'COMPLETED'
                    """, error.getClass().getSimpleName(), id));
            throw error;
        }
        return get(id);
    }

    private boolean processChunk(long id) {
        var dates = jdbc.query("""
                SELECT business_date FROM ops_premium_delinquency_run
                WHERE run_id = ? AND status <> 'COMPLETED' FOR UPDATE SKIP LOCKED
                """, (rs, n) -> rs.getObject(1, LocalDate.class), id);
        if (dates.isEmpty()) return false;
        LocalDate date = dates.getFirst();
        List<Long> targets = jdbc.query("""
                SELECT policy_id FROM ops_premium_delinquency_target
                WHERE run_id = ? AND outcome IS NULL ORDER BY policy_id LIMIT 20
                """, (rs, n) -> rs.getLong(1), id);
        for (long policy : targets) {
            String outcome = evaluate(policy, date, id);
            jdbc.update("""
                    UPDATE ops_premium_delinquency_target SET outcome = ?, processed_at = NOW()
                    WHERE run_id = ? AND policy_id = ?
                    """, outcome, id, policy);
        }
        boolean pending = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM ops_premium_delinquency_target WHERE run_id = ? AND outcome IS NULL)
                """, Boolean.class, id));
        jdbc.update("""
                UPDATE ops_premium_delinquency_run SET status = ?, error_reason = NULL,
                finished_at = CASE WHEN ? THEN NULL ELSE NOW() END WHERE run_id = ?
                """, pending ? "RUNNING" : "COMPLETED", pending, id);
        return pending;
    }

    private String evaluate(long policyId, LocalDate date, long runId) {
        String current = lockPolicy(policyId);
        if (!List.of("ACTIVE", "GRACE").contains(current)) return "UNCHANGED";
        List<Due> unpaid = jdbc.query("""
                SELECT premium_receivable_id, grace_ends_on FROM ins_premium_receivable
                WHERE policy_id = ? AND due_date < ? AND amount_settled < amount_due
                ORDER BY premium_receivable_id FOR UPDATE
                """, (rs, n) -> new Due(rs.getLong(1), rs.getObject(2, LocalDate.class)), policyId, date);
        boolean noticeFailed = false;
        boolean changed = false;
        LocalDate lapseOn = null;
        for (Due due : unpaid) {
            List<LocalDate> delivered = jdbc.query("""
                    SELECT effective_grace_ends_on FROM ins_premium_notice
                    WHERE premium_receivable_id = ? AND status = 'SIMULATED_DELIVERED'
                    """, (rs, n) -> rs.getObject(1, LocalDate.class), due.id());
            LocalDate end;
            if (delivered.isEmpty()) {
                boolean success = notices.deliverSimulation(due.id());
                LocalDate noticeDate = today();
                end = due.end().isAfter(noticeDate.plusDays(graceDays)) ? due.end() : noticeDate.plusDays(graceDays);
                jdbc.update("""
                        INSERT INTO ins_premium_notice(premium_receivable_id, status, policy_version, grace_days,
                            notified_on, effective_grace_ends_on, attempts)
                        VALUES (?, ?, 'SIMULATED-V1', ?, ?, ?, 1)
                        ON CONFLICT (premium_receivable_id) DO UPDATE SET status = EXCLUDED.status,
                            policy_version = EXCLUDED.policy_version, grace_days = EXCLUDED.grace_days,
                            notified_on = EXCLUDED.notified_on, effective_grace_ends_on = EXCLUDED.effective_grace_ends_on,
                            attempts = ins_premium_notice.attempts + 1, updated_at = NOW()
                        """, due.id(), success ? "SIMULATED_DELIVERED" : "FAILED", graceDays,
                        success ? noticeDate : null, success ? end : null);
                noticeFailed |= !success;
                changed = true;
                if (!success) end = null;
            } else end = delivered.getFirst();
            if (end != null && date.isAfter(end)) {
                LocalDate candidate = end.plusDays(1);
                if (lapseOn == null || candidate.isBefore(lapseOn)) lapseOn = candidate;
            }
            changed |= jdbc.update("""
                    UPDATE ins_premium_receivable SET status = 'GRACE', updated_at = NOW()
                    WHERE premium_receivable_id = ? AND status <> 'GRACE'
                    """, due.id()) > 0;
        }
        String next = unpaid.isEmpty() ? "ACTIVE" : lapseOn != null ? "LAPSED" : "GRACE";
        // An old execution must not restore a policy while newer overdue cycles still exist.
        if (next.equals("ACTIVE") && hasArrears(policyId, today())) next = current;
        if (next.equals("LAPSED")) {
            jdbc.update("""
                    UPDATE ins_premium_receivable SET status = 'LAPSED', updated_at = NOW()
                    WHERE policy_id = ? AND amount_settled < amount_due AND due_date < ?
                    """, policyId, date);
        }
        Instant effective = next.equals("LAPSED")
                ? lapseOn.atStartOfDay(BUSINESS_ZONE).toInstant() : Instant.now(clock);
        String reason = next.equals("LAPSED") ? "GRACE_EXPIRED_UNPAID"
                : next.equals("GRACE") ? "OVERDUE_PREMIUM" : "ARREARS_SETTLED";
        changed |= transition(policyId, current, next, runId, reason, effective);
        return noticeFailed ? "NOTICE_FAILED" : changed ? "CHANGED" : "UNCHANGED";
    }

    public String lockPolicy(long policyId) {
        return jdbc.query("SELECT status FROM ins_policy WHERE policy_id = ? FOR UPDATE",
                (rs, n) -> rs.getString(1), policyId).stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "계약을 찾을 수 없습니다."));
    }

    /** Same expiry rule as the batch, before applying new money; scheduler delay cannot grant revival. */
    public void beforeSettlement(long policyId) {
        String current = lockPolicy(policyId);
        if (!List.of("ACTIVE", "GRACE").contains(current)) return;
        LocalDate end = jdbc.queryForObject("""
                SELECT MIN(n.effective_grace_ends_on) FROM ins_premium_receivable r
                JOIN ins_premium_notice n USING(premium_receivable_id)
                WHERE r.policy_id = ? AND r.amount_settled < r.amount_due
                  AND n.status = 'SIMULATED_DELIVERED' AND n.effective_grace_ends_on < ?
                """, LocalDate.class, policyId, today());
        if (end == null) return;
        jdbc.update("""
                UPDATE ins_premium_receivable SET status = 'LAPSED', updated_at = NOW()
                WHERE policy_id = ? AND due_date < ? AND amount_settled < amount_due
                """, policyId, today());
        transition(policyId, current, "LAPSED", null, "GRACE_EXPIRED_UNPAID",
                end.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
    }

    /** Called inside the settlement transaction after the policy/receivable locks. */
    public void afterSettlement(long policyId) {
        String status = lockPolicy(policyId);
        if (status.equals("GRACE") && !hasArrears(policyId, today())) {
            transition(policyId, status, "ACTIVE", null, "ARREARS_SETTLED", Instant.now(clock));
        } else if (status.equals("LAPSED")) {
            jdbc.update("""
                    INSERT INTO pay_late_settlement_review(premium_settlement_id, policy_id)
                    SELECT s.premium_settlement_id, r.policy_id FROM pay_premium_settlement s
                    JOIN ins_premium_receivable r USING (premium_receivable_id)
                    JOIN ins_policy p USING (policy_id)
                    WHERE r.policy_id = ? AND s.settled_at >= p.lapsed_at
                    ON CONFLICT (premium_settlement_id) DO NOTHING
                    """, policyId);
        }
    }

    private boolean hasArrears(long policy, LocalDate date) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM ins_premium_receivable
                WHERE policy_id = ? AND due_date < ? AND amount_settled < amount_due)
                """, Boolean.class, policy, date));
    }

    private boolean transition(long policy, String from, String to, Long run, String reason, Instant at) {
        if (from.equals(to)) return false;
        jdbc.update("""
                UPDATE ins_policy SET status = ?, updated_at = NOW(),
                    lapsed_at = CASE WHEN ? = 'LAPSED' THEN ? ELSE lapsed_at END WHERE policy_id = ?
                """, to, to, Timestamp.from(at), policy);
        jdbc.update("""
                INSERT INTO ins_policy_delinquency_history(policy_id, run_id, from_status, to_status, reason_code, effective_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, policy, run, from, to, reason, Timestamp.from(at));
        return true;
    }

    public DelinquencyRunResponse get(long id) {
        return jdbc.query("""
                SELECT r.*, COUNT(t.policy_id) AS targets, COUNT(t.outcome) AS processed,
                    COUNT(*) FILTER (WHERE t.outcome = 'CHANGED') AS changed,
                    COUNT(*) FILTER (WHERE t.outcome = 'UNCHANGED') AS unchanged,
                    COUNT(*) FILTER (WHERE t.outcome = 'NOTICE_FAILED') AS notice_failed
                FROM ops_premium_delinquency_run r LEFT JOIN ops_premium_delinquency_target t USING(run_id)
                WHERE r.run_id = ? GROUP BY r.run_id
                """, (rs, n) -> {
                    long total = rs.getLong("targets"), processed = rs.getLong("processed");
                    long changed = rs.getLong("changed"), unchanged = rs.getLong("unchanged"), failed = rs.getLong("notice_failed");
                    return new DelinquencyRunResponse(id, rs.getString("instance_key"), rs.getObject("business_date", LocalDate.class),
                            rs.getString("status"), total, processed, changed, unchanged, failed, total - processed,
                            processed == changed + unchanged + failed && total >= processed, rs.getString("error_reason"));
                }, id).stream().findFirst().orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "미납 배치 실행을 찾을 수 없습니다."));
    }

    public List<DelinquencyRunResponse> recent() {
        return jdbc.query("SELECT run_id FROM ops_premium_delinquency_run ORDER BY run_id DESC LIMIT 8",
                (rs, n) -> rs.getLong(1)).stream().map(this::get).toList();
    }

    public LocalDate today() { return LocalDate.now(clock.withZone(BUSINESS_ZONE)); }
    private record Due(long id, LocalDate end) { }
}
