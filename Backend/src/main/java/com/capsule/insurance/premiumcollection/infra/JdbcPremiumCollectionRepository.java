package com.capsule.insurance.premiumcollection.infra;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import com.capsule.insurance.premiumcollection.dto.CreatePremiumReceivableRequest;
import com.capsule.insurance.premiumcollection.dto.InstantSettlementRequest;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPremiumCollectionRepository {
    private final JdbcTemplate jdbcTemplate;
    public JdbcPremiumCollectionRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public PremiumCollectionSnapshot createDueWithAutomaticInstruction(CreatePremiumReceivableRequest request, String instructionNo) {
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
        PremiumCollectionSnapshot before = lock(request.premiumReceivableId());
        jdbcTemplate.update("""
                INSERT INTO pay_collection_instruction (premium_receivable_id, instruction_no, collection_method, amount, provider, provider_request_key, idempotency_key, status, captured_at)
                VALUES (?, ?, 'INSTANT_PAYMENT', ?, 'FAKE_PREMIUM_PAYMENT', ?, ?, 'CAPTURED', NOW())
                ON CONFLICT (idempotency_key) DO NOTHING
                """, before.premiumReceivableId(), instructionNo, request.amount(), request.providerTransactionKey(), request.idempotencyKey());
        Long instructionId = jdbcTemplate.queryForObject("SELECT collection_instruction_id FROM pay_collection_instruction WHERE idempotency_key = ?", Long.class, request.idempotencyKey());
        jdbcTemplate.update("""
                INSERT INTO pay_premium_settlement (premium_receivable_id, collection_instruction_id, amount, provider_transaction_key, settled_at)
                VALUES (?, ?, ?, ?, NOW()) ON CONFLICT (provider_transaction_key) DO NOTHING
                """, before.premiumReceivableId(), instructionId, request.amount(), request.providerTransactionKey());
        jdbcTemplate.update("""
                UPDATE ins_premium_receivable SET amount_settled = (SELECT COALESCE(SUM(amount), 0) FROM pay_premium_settlement WHERE premium_receivable_id = ?),
                    status = CASE WHEN (SELECT COALESCE(SUM(amount), 0) FROM pay_premium_settlement WHERE premium_receivable_id = ?) > amount_due THEN 'OVERPAID' ELSE 'SETTLED' END
                WHERE premium_receivable_id = ?
                """, before.premiumReceivableId(), before.premiumReceivableId(), before.premiumReceivableId());
        jdbcTemplate.update("UPDATE pay_collection_instruction SET status = 'CANCELED', canceled_at = NOW() WHERE premium_receivable_id = ? AND collection_method = 'AUTO_DEBIT' AND status = 'SCHEDULED'", before.premiumReceivableId());
        jdbcTemplate.update("UPDATE pay_collection_instruction SET status = 'CANCEL_REQUESTED' WHERE premium_receivable_id = ? AND collection_method = 'AUTO_DEBIT' AND status = 'SUBMITTED'", before.premiumReceivableId());
        return find(before.premiumReceivableId());
    }

    public PremiumCollectionSnapshot captureAutomaticDebit(Long instructionId, String providerTransactionKey) {
        Long receivableId = jdbcTemplate.queryForObject("SELECT premium_receivable_id FROM pay_collection_instruction WHERE collection_instruction_id = ? FOR UPDATE", Long.class, instructionId);
        BigDecimal amount = jdbcTemplate.queryForObject("SELECT amount FROM pay_collection_instruction WHERE collection_instruction_id = ?", BigDecimal.class, instructionId);
        jdbcTemplate.update("UPDATE pay_collection_instruction SET status = 'CAPTURED', captured_at = NOW() WHERE collection_instruction_id = ? AND status IN ('SUBMITTED', 'CANCEL_REQUESTED')", instructionId);
        jdbcTemplate.update("INSERT INTO pay_premium_settlement (premium_receivable_id, collection_instruction_id, amount, provider_transaction_key, settled_at) VALUES (?, ?, ?, ?, NOW()) ON CONFLICT (provider_transaction_key) DO NOTHING", receivableId, instructionId, amount, providerTransactionKey);
        jdbcTemplate.update("UPDATE ins_premium_receivable SET amount_settled = (SELECT COALESCE(SUM(amount), 0) FROM pay_premium_settlement WHERE premium_receivable_id = ?), status = CASE WHEN (SELECT COALESCE(SUM(amount), 0) FROM pay_premium_settlement WHERE premium_receivable_id = ?) > amount_due THEN 'OVERPAID' ELSE 'SETTLED' END WHERE premium_receivable_id = ?", receivableId, receivableId, receivableId);
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

    private PremiumCollectionSnapshot lock(Long receivableId) {
        return jdbcTemplate.query("SELECT r.*, i.collection_instruction_id, i.status AS collection_status FROM ins_premium_receivable r LEFT JOIN pay_collection_instruction i ON i.premium_receivable_id = r.premium_receivable_id AND i.collection_method = 'AUTO_DEBIT' WHERE r.premium_receivable_id = ? FOR UPDATE OF r", this::map, receivableId).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "보험료 채권을 찾을 수 없습니다."));
    }
    private PremiumCollectionSnapshot find(Long receivableId) {
        return jdbcTemplate.query("SELECT r.*, i.collection_instruction_id, i.status AS collection_status FROM ins_premium_receivable r LEFT JOIN pay_collection_instruction i ON i.premium_receivable_id = r.premium_receivable_id AND i.collection_method = 'AUTO_DEBIT' WHERE r.premium_receivable_id = ?", this::map, receivableId).stream().findFirst().orElseThrow();
    }
    private PremiumCollectionSnapshot map(ResultSet rs, int rowNum) throws SQLException {
        return new PremiumCollectionSnapshot(rs.getLong("premium_receivable_id"), rs.getLong("policy_id"), rs.getObject("billing_cycle", java.time.LocalDate.class), rs.getObject("due_date", java.time.LocalDate.class), rs.getObject("grace_ends_on", java.time.LocalDate.class), rs.getBigDecimal("amount_due"), rs.getBigDecimal("amount_settled"), rs.getString("status"), rs.getObject("collection_instruction_id", Long.class), rs.getString("collection_status"), rs.getTimestamp("updated_at").toInstant());
    }
}
