package com.capsule.insurance.assistantai.claim.infra;

import com.capsule.insurance.assistantai.claim.application.port.ClaimCopilotReviewRepository;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEvent;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEventType;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewStatus;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewDraftSnapshot;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentSourceReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcClaimCopilotReviewRepository implements ClaimCopilotReviewRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcClaimCopilotReviewRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ClaimCopilotReview registerDraft(Long claimId, ClaimCopilotReviewDraftSnapshot draft) {
        String requestId = draft.requestId();
        int created = jdbcTemplate.update("""
                INSERT INTO public.ops_claim_copilot_review (claim_id, request_id, review_status)
                VALUES (?, ?, 'DRAFT')
                ON CONFLICT (claim_id, request_id) DO NOTHING
                """, claimId, requestId);
        if (created == 1) {
            insertEvent(claimId, requestId, ClaimCopilotReviewEventType.DRAFT_CREATED,
                    ClaimCopilotReviewStatus.DRAFT, null);
        }
        jdbcTemplate.update("""
                INSERT INTO public.ops_claim_copilot_draft_snapshot (
                    claim_id, request_id, terms_to_check_json, missing_evidence_json,
                    additional_questions_json, evidence_insufficient
                ) VALUES (?, ?, CAST(? AS JSONB), CAST(? AS JSONB), CAST(? AS JSONB), ?)
                ON CONFLICT (claim_id, request_id) DO UPDATE
                SET terms_to_check_json = EXCLUDED.terms_to_check_json,
                    missing_evidence_json = EXCLUDED.missing_evidence_json,
                    additional_questions_json = EXCLUDED.additional_questions_json,
                    evidence_insufficient = EXCLUDED.evidence_insufficient,
                    updated_at = NOW()
                """, claimId, requestId, toJson(draft.termsToCheck()), toJson(draft.possibleMissingEvidence()),
                toJson(draft.additionalQuestions()), draft.evidenceInsufficient());
        return find(claimId, requestId).orElseThrow();
    }

    @Override
    public Optional<ClaimCopilotReviewDraftSnapshot> findDraft(Long claimId, String requestId) {
        return jdbcTemplate.query("""
                SELECT request_id, terms_to_check_json::TEXT AS terms_to_check_json,
                       missing_evidence_json::TEXT AS missing_evidence_json,
                       additional_questions_json::TEXT AS additional_questions_json,
                       evidence_insufficient
                FROM public.ops_claim_copilot_draft_snapshot
                WHERE claim_id = ? AND request_id = ?
                """, this::mapDraft, claimId, requestId).stream().findFirst();
    }

    @Override
    public List<ClaimCopilotReviewEvent> findHistory(Long claimId, String requestId) {
        return jdbcTemplate.query("""
                SELECT claim_id, request_id, event_type, review_status, reviewer_user_id, occurred_at
                FROM public.ops_claim_copilot_review_event
                WHERE claim_id = ? AND request_id = ?
                ORDER BY claim_copilot_review_event_id
                """, this::mapEvent, claimId, requestId);
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
    public List<ClaimCopilotReview> findRecent(ClaimCopilotReviewStatus status, int limit) {
        if (status == null) {
            return jdbcTemplate.query("""
                    SELECT claim_id, request_id, review_status, reviewer_user_id, updated_at
                    FROM public.ops_claim_copilot_review
                    ORDER BY updated_at DESC, claim_copilot_review_id DESC
                    LIMIT ?
                    """, this::mapReview, limit);
        }
        return jdbcTemplate.query("""
                SELECT claim_id, request_id, review_status, reviewer_user_id, updated_at
                FROM public.ops_claim_copilot_review
                WHERE review_status = ?
                ORDER BY updated_at DESC, claim_copilot_review_id DESC
                LIMIT ?
                """, this::mapReview, status.name(), limit);
    }

    @Override
    @Transactional
    public Optional<ClaimCopilotReview> updateReview(
            Long claimId,
            String requestId,
            ClaimCopilotReviewStatus status,
            Long reviewerUserId
    ) {
        Optional<ClaimCopilotReview> updated = jdbcTemplate.query("""
                UPDATE public.ops_claim_copilot_review
                SET review_status = ?, reviewer_user_id = ?, updated_at = NOW()
                WHERE claim_id = ? AND request_id = ?
                RETURNING claim_id, request_id, review_status, reviewer_user_id, updated_at
                """, this::mapReview, status.name(), reviewerUserId, claimId, requestId).stream().findFirst();
        updated.ifPresent(ignored -> insertEvent(
                claimId,
                requestId,
                status == ClaimCopilotReviewStatus.CONFIRMED
                        ? ClaimCopilotReviewEventType.REVIEW_CONFIRMED
                        : ClaimCopilotReviewEventType.REVIEW_REJECTED,
                status,
                reviewerUserId
        ));
        return updated;
    }

    private void insertEvent(
            Long claimId,
            String requestId,
            ClaimCopilotReviewEventType eventType,
            ClaimCopilotReviewStatus status,
            Long reviewerUserId
    ) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_claim_copilot_review_event (
                    claim_id, request_id, event_type, review_status, reviewer_user_id
                ) VALUES (?, ?, ?, ?, ?)
                """, claimId, requestId, eventType.name(), status.name(), reviewerUserId);
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

    private ClaimCopilotReviewEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        long reviewerUserId = resultSet.getLong("reviewer_user_id");
        return new ClaimCopilotReviewEvent(
                resultSet.getLong("claim_id"),
                resultSet.getString("request_id"),
                ClaimCopilotReviewEventType.valueOf(resultSet.getString("event_type")),
                ClaimCopilotReviewStatus.valueOf(resultSet.getString("review_status")),
                resultSet.wasNull() ? null : reviewerUserId,
                resultSet.getTimestamp("occurred_at").toInstant()
        );
    }

    private ClaimCopilotReviewDraftSnapshot mapDraft(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ClaimCopilotReviewDraftSnapshot(
                resultSet.getString("request_id"),
                fromJson(resultSet.getString("terms_to_check_json"), new TypeReference<List<ClaimAssessmentSourceReference>>() { }),
                fromJson(resultSet.getString("missing_evidence_json"), new TypeReference<List<String>>() { }),
                fromJson(resultSet.getString("additional_questions_json"), new TypeReference<List<String>>() { }),
                resultSet.getBoolean("evidence_insufficient")
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("심사 보조 초안을 저장할 수 없습니다.", exception);
        }
    }

    private <T> T fromJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("심사 보조 초안을 읽을 수 없습니다.", exception);
        }
    }
}
