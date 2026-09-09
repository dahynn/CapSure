package com.capsule.insurance.assistantai.claim.infra;

import com.capsule.insurance.assistantai.claim.application.port.ClaimCopilotReviewRepository;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClaimCopilotReviewRepository implements ClaimCopilotReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcClaimCopilotReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ClaimCopilotReview registerDraft(Long claimId, String requestId) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_claim_copilot_review (claim_id, request_id, review_status)
                VALUES (?, ?, 'DRAFT')
                ON CONFLICT (claim_id, request_id) DO NOTHING
                """, claimId, requestId);
        return find(claimId, requestId).orElseThrow();
    }

    @Override
    public Optional<ClaimCopilotReview> find(Long claimId, String requestId) {
        return jdbcTemplate.query("""
                SELECT claim_id, request_id, review_status, reviewer_user_id, updated_at
                FROM public.ops_claim_copilot_review
                WHERE claim_id = ? AND request_id = ?
                """, this::mapReview, claimId, requestId).stream().findFirst();
    }

    @Override
    public Optional<ClaimCopilotReview> updateReview(
            Long claimId,
            String requestId,
            ClaimCopilotReviewStatus status,
            Long reviewerUserId
    ) {
        return jdbcTemplate.query("""
                UPDATE public.ops_claim_copilot_review
                SET review_status = ?, reviewer_user_id = ?, updated_at = NOW()
                WHERE claim_id = ? AND request_id = ?
                RETURNING claim_id, request_id, review_status, reviewer_user_id, updated_at
                """, this::mapReview, status.name(), reviewerUserId, claimId, requestId).stream().findFirst();
    }

    private ClaimCopilotReview mapReview(ResultSet resultSet, int rowNumber) throws SQLException {
        long reviewerUserId = resultSet.getLong("reviewer_user_id");
        return new ClaimCopilotReview(
                resultSet.getLong("claim_id"),
                resultSet.getString("request_id"),
                ClaimCopilotReviewStatus.valueOf(resultSet.getString("review_status")),
                resultSet.wasNull() ? null : reviewerUserId,
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }
}
