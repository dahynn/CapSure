package com.capsule.insurance.claim.infra;

import com.capsule.insurance.claim.application.port.ClaimEvidenceAccessAuditRecorder;
import com.capsule.insurance.claim.domain.ClaimEvidenceAccessAttempt;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcClaimEvidenceAccessAuditRecorder implements ClaimEvidenceAccessAuditRecorder {

    private static final String INSERT_EVENT = """
            INSERT INTO public.ops_claim_evidence_access_event (
                access_request_id,
                actor_user_id,
                claim_id,
                target_type,
                target_id,
                access_action,
                access_result,
                occurred_at
            ) VALUES (?, ?, ?, ?, ?, 'CLAIM_DETAIL_READ', ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate requiresNewTransaction;

    public JdbcClaimEvidenceAccessAuditRecorder(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void record(ClaimEvidenceAccessAttempt attempt) {
        requiresNewTransaction.executeWithoutResult(status -> {
            List<Object[]> events = new ArrayList<>();
            events.add(arguments(attempt, "CLAIM", attempt.claimId()));
            attempt.evidenceIds().forEach(evidenceId ->
                    events.add(arguments(attempt, "EVIDENCE", evidenceId))
            );
            jdbcTemplate.batchUpdate(INSERT_EVENT, events);
        });
    }

    private Object[] arguments(
            ClaimEvidenceAccessAttempt attempt,
            String targetType,
            Long targetId
    ) {
        return new Object[] {
                attempt.accessRequestId(),
                attempt.actorUserId(),
                attempt.claimId(),
                targetType,
                targetId,
                attempt.result().name(),
                Timestamp.from(attempt.occurredAt())
        };
    }
}
