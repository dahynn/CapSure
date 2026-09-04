package com.capsule.insurance.premiumcollection.infra;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import com.capsule.insurance.premiumcollection.dto.CreatePremiumReceivableRequest;
import com.capsule.insurance.premiumcollection.dto.InstantSettlementRequest;
import com.capsule.insurance.premiumcollection.dto.PremiumCollectionTimelineResponse;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPremiumCollectionRepository {
    private final JdbcTemplate jdbcTemplate;
    public JdbcPremiumCollectionRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public PremiumCollectionSnapshot createDueWithAutomaticInstruction(CreatePremiumReceivableRequest request, String instructionNo) {
        lockPolicy(request.policyId());
        if (request.graceEndsOn().isBefore(request.dueDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유예 종료일은 납부기일 이전일 수 없습니다.");
        }
        Long receivableId = jdbcTemplate.queryForObject("""
                INSERT INTO ins_premium_receivable (policy_id, billing_cycle, due_date, grace_ends_on, amount_due, status)
                VALUES (?, ?, ?, ?, ?, 'DUE') RETURNING premium_receivable_id
                """, Long.class, request.policyId(), request.billingCycle(), request.dueDate(), request.graceEndsOn(), request.amount());
        jdbcTemplate.update("""
                INSERT INTO pay_collection_instruction (premium_receivable_id, instruction_no, collection_method, amount, provider, idempotency_key, status)
                VALUES (?, ?, 'AUTO_DEBIT', ?, 'FAKE_PREMIUM_PAYMENT', ?, 'SCHEDULED')
                """, receivableId, instructionNo, request.amount(), "auto-debit:" + receivableId);
        return find(receivableId);
    }

    public PremiumCollectionSnapshot settleImmediately(InstantSettlementRequest request, String instructionNo) {
        lockPolicyForReceivable(request.premiumReceivableId());
        PremiumCollectionSnapshot before = lock(request.premiumReceivableId());
        List<Long> repeated = jdbcTemplate.query("""
                SELECT collection_instruction_id FROM pay_collection_instruction
                WHERE idempotency_key = ? AND premium_receivable_id = ? AND amount = ? AND provider_request_key = ?
                """, (rs, n) -> rs.getLong(1), request.idempotencyKey(), before.premiumReceivableId(), request.amount(), request.providerTransactionKey());
        if (!repeated.isEmpty()) return find(before.premiumReceivableId());
        if (Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM pay_collection_instruction WHERE idempotency_key = ?)
                    OR EXISTS(SELECT 1 FROM pay_premium_settlement WHERE provider_transaction_key = ?)
                """, Boolean.class, request.idempotencyKey(), request.providerTransactionKey()))) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "다른 수납에 사용된 거래 키입니다.");
        }
        jdbcTemplate.update("""
                INSERT INTO pay_collection_instruction (premium_receivable_id, instruction_no, collection_method, amount, provider, provider_request_key, idempotency_key, status, captured_at)
                VALUES (?, ?, 'INSTANT_PAYMENT', ?, 'FAKE_PREMIUM_PAYMENT', ?, ?, 'CAPTURED', NOW())
                """, before.premiumReceivableId(), instructionNo, request.amount(), request.providerTransactionKey(), request.idempotencyKey());
        Long instructionId = jdbcTemplate.queryForObject("SELECT collection_instruction_id FROM pay_collection_instruction WHERE idempotency_key = ?", Long.class, request.idempotencyKey());
        jdbcTemplate.update("""
                INSERT INTO pay_premium_settlement (premium_receivable_id, collection_instruction_id, amount, provider_transaction_key, settled_at)
                VALUES (?, ?, ?, ?, NOW())
                """, before.premiumReceivableId(), instructionId, request.amount(), request.providerTransactionKey());
        refreshBalance(before.premiumReceivableId());
        PremiumCollectionSnapshot after = find(before.premiumReceivableId());
        if (after.amountSettled().compareTo(after.amountDue()) >= 0) {
            jdbcTemplate.update("UPDATE pay_collection_instruction SET status = 'CANCELED', canceled_at = NOW(), updated_at = NOW() WHERE premium_receivable_id = ? AND collection_method = 'AUTO_DEBIT' AND status = 'SCHEDULED'", before.premiumReceivableId());
            jdbcTemplate.update("UPDATE pay_collection_instruction SET status = 'CANCEL_REQUESTED', updated_at = NOW() WHERE premium_receivable_id = ? AND collection_method = 'AUTO_DEBIT' AND status = 'SUBMITTED'", before.premiumReceivableId());
        }
        return find(before.premiumReceivableId());
    }

    public PremiumCollectionSnapshot captureAutomaticDebit(Long instructionId, String providerTransactionKey) {
        Long receivableId = jdbcTemplate.query("SELECT premium_receivable_id FROM pay_collection_instruction WHERE collection_instruction_id = ?",
                (rs, n) -> rs.getLong(1), instructionId).stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "출금 지시를 찾을 수 없습니다."));
        lockPolicyForReceivable(receivableId);
        lock(receivableId);
        String state = jdbcTemplate.queryForObject("SELECT status FROM pay_collection_instruction WHERE collection_instruction_id = ? FOR UPDATE", String.class, instructionId);
        List<Long> existing = jdbcTemplate.query("SELECT collection_instruction_id FROM pay_premium_settlement WHERE provider_transaction_key = ?",
                (rs, n) -> rs.getLong(1), providerTransactionKey);
        if (!existing.isEmpty()) {
            if (existing.getFirst().equals(instructionId)) return find(receivableId);
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "다른 출금에 사용된 거래 키입니다.");
        }
        if (!List.of("SUBMITTED", "CANCEL_REQUESTED", "UNKNOWN").contains(state)) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "처리 중인 출금 지시만 수납 확정할 수 있습니다.");
        }
        BigDecimal amount = jdbcTemplate.queryForObject("SELECT amount FROM pay_collection_instruction WHERE collection_instruction_id = ?", BigDecimal.class, instructionId);
        jdbcTemplate.update("UPDATE pay_collection_instruction SET status = 'CAPTURED', captured_at = NOW(), updated_at = NOW() WHERE collection_instruction_id = ?", instructionId);
        jdbcTemplate.update("INSERT INTO pay_premium_settlement (premium_receivable_id, collection_instruction_id, amount, provider_transaction_key, settled_at) VALUES (?, ?, ?, ?, NOW())", receivableId, instructionId, amount, providerTransactionKey);
        refreshBalance(receivableId);
        return find(receivableId);
    }

    public int createDuplicateDebitRefundCases() {
        return jdbcTemplate.update("""
                INSERT INTO pay_refund_case (premium_receivable_id, refund_no, amount, reason_code, status, idempotency_key)
                SELECT r.premium_receivable_id, 'REF-' || r.premium_receivable_id || '-' || r.amount_settled, r.amount_settled - r.amount_due,
                       'DUPLICATE_DEBIT', 'AUTO_REFUND_ELIGIBLE', 'duplicate-debit:' || r.premium_receivable_id || ':' || r.amount_settled
                FROM ins_premium_receivable r
                WHERE r.status = 'OVERPAID'
                ON CONFLICT (idempotency_key) DO NOTHING
                """);
    }

    public PremiumCollectionTimelineResponse loadTimeline(int limit) {
        long due = countByStatus("DUE");
        long grace = countByStatus("GRACE");
        long overpaid = countByStatus("OVERPAID");
        long refunds = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_refund_case WHERE status IN ('PENDING', 'AUTO_REFUND_ELIGIBLE', 'MANUAL_REVIEW')", Long.class);
        List<PremiumCollectionTimelineResponse.Item> items = jdbcTemplate.query("""
                SELECT r.premium_receivable_id, r.policy_id, r.billing_cycle, r.amount_due, r.amount_settled, r.status AS receivable_status, r.updated_at,
                       p.status AS policy_status, p.lapsed_at, r.due_date, n.effective_grace_ends_on, n.status AS notice_status,
                       (SELECT reason_code FROM ins_policy_delinquency_history h WHERE h.policy_id = p.policy_id ORDER BY h.history_id DESC LIMIT 1) AS change_reason,
                       (SELECT COUNT(*) FROM pay_late_settlement_review lr JOIN pay_premium_settlement ps USING(premium_settlement_id) WHERE ps.premium_receivable_id = r.premium_receivable_id) AS late_review_count,
                       (SELECT status FROM pay_collection_instruction i WHERE i.premium_receivable_id = r.premium_receivable_id AND i.collection_method = 'AUTO_DEBIT' ORDER BY i.collection_instruction_id DESC LIMIT 1) AS instruction_status,
                       (SELECT status FROM pay_refund_case f WHERE f.premium_receivable_id = r.premium_receivable_id ORDER BY f.refund_case_id DESC LIMIT 1) AS refund_status
                FROM ins_premium_receivable r JOIN ins_policy p USING(policy_id)
                LEFT JOIN ins_premium_notice n USING(premium_receivable_id)
                ORDER BY r.updated_at DESC, r.premium_receivable_id DESC LIMIT ?
                """, (rs, row) -> new PremiumCollectionTimelineResponse.Item(rs.getLong("premium_receivable_id"), rs.getLong("policy_id"), rs.getObject("billing_cycle", java.time.LocalDate.class), rs.getBigDecimal("amount_due"), rs.getBigDecimal("amount_settled"), rs.getString("receivable_status"), rs.getString("instruction_status"), rs.getString("refund_status"), rs.getTimestamp("updated_at").toInstant(),
                    rs.getString("policy_status"), rs.getObject("due_date", java.time.LocalDate.class), rs.getObject("effective_grace_ends_on", java.time.LocalDate.class),
                    rs.getString("notice_status"), rs.getString("change_reason"), rs.getTimestamp("lapsed_at") == null ? null : rs.getTimestamp("lapsed_at").toInstant(), rs.getLong("late_review_count")), limit);
        return new PremiumCollectionTimelineResponse(due, grace, overpaid, refunds,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ins_policy WHERE status = 'LAPSED'", Long.class),
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_late_settlement_review", Long.class), items);
    }

    private long countByStatus(String status) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ins_premium_receivable WHERE status = ?", Long.class, status);
        return count == null ? 0 : count;
    }

    private PremiumCollectionSnapshot lock(Long receivableId) {
        return jdbcTemplate.query("SELECT r.*, i.collection_instruction_id, i.status AS collection_status FROM ins_premium_receivable r LEFT JOIN pay_collection_instruction i ON i.premium_receivable_id = r.premium_receivable_id AND i.collection_method = 'AUTO_DEBIT' WHERE r.premium_receivable_id = ? FOR UPDATE OF r", this::map, receivableId).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "보험료 채권을 찾을 수 없습니다."));
    }
    private void lockPolicy(Long policyId) {
        if (jdbcTemplate.query("SELECT policy_id FROM ins_policy WHERE policy_id = ? FOR UPDATE",
                (rs, n) -> rs.getLong(1), policyId).isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "계약을 찾을 수 없습니다.");
        }
    }
    private void lockPolicyForReceivable(Long receivableId) {
        lockPolicy(policyIdForReceivable(receivableId));
    }
    public Long policyIdForReceivable(Long receivableId) {
        return jdbcTemplate.query("SELECT policy_id FROM ins_premium_receivable WHERE premium_receivable_id = ?",
                (rs, n) -> rs.getLong(1), receivableId).stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "보험료 채권을 찾을 수 없습니다."));
    }
    public Long policyIdForInstruction(Long instructionId) {
        return jdbcTemplate.query("""
                SELECT r.policy_id FROM pay_collection_instruction i JOIN ins_premium_receivable r USING(premium_receivable_id)
                WHERE i.collection_instruction_id = ?
                """, (rs, n) -> rs.getLong(1), instructionId).stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "출금 지시를 찾을 수 없습니다."));
    }
    private void refreshBalance(Long receivableId) {
        jdbcTemplate.update("""
                UPDATE ins_premium_receivable r SET amount_settled = s.total, updated_at = NOW(),
                    status = CASE WHEN s.total > r.amount_due THEN 'OVERPAID'
                                  WHEN s.total = r.amount_due THEN 'SETTLED'
                                  WHEN r.status IN ('GRACE', 'LAPSED') THEN r.status ELSE 'DUE' END
                FROM (SELECT COALESCE(SUM(amount), 0) AS total FROM pay_premium_settlement
                      WHERE premium_receivable_id = ?) s
                WHERE r.premium_receivable_id = ?
                """, receivableId, receivableId);
    }
    private PremiumCollectionSnapshot find(Long receivableId) {
        return jdbcTemplate.query("SELECT r.*, i.collection_instruction_id, i.status AS collection_status FROM ins_premium_receivable r LEFT JOIN pay_collection_instruction i ON i.premium_receivable_id = r.premium_receivable_id AND i.collection_method = 'AUTO_DEBIT' WHERE r.premium_receivable_id = ?", this::map, receivableId).stream().findFirst().orElseThrow();
    }
    private PremiumCollectionSnapshot map(ResultSet rs, int rowNum) throws SQLException {
        return new PremiumCollectionSnapshot(rs.getLong("premium_receivable_id"), rs.getLong("policy_id"), rs.getObject("billing_cycle", java.time.LocalDate.class), rs.getObject("due_date", java.time.LocalDate.class), rs.getObject("grace_ends_on", java.time.LocalDate.class), rs.getBigDecimal("amount_due"), rs.getBigDecimal("amount_settled"), rs.getString("status"), rs.getObject("collection_instruction_id", Long.class), rs.getString("collection_status"), rs.getTimestamp("updated_at").toInstant());
    }
}
