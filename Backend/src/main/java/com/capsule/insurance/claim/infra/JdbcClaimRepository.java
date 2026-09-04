package com.capsule.insurance.claim.infra;

import com.capsule.insurance.claim.application.port.ClaimRepository;
import com.capsule.insurance.claim.domain.ClaimAssessmentContext;
import com.capsule.insurance.claim.domain.ClaimDecision;
import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.claim.domain.ClaimPayment;
import com.capsule.insurance.claim.domain.InsuranceClaim;
import com.capsule.insurance.policy.domain.PolicySnapshot;
import com.capsule.insurance.quote.domain.QuoteSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClaimRepository implements ClaimRepository {

    private static final String CLAIM_SELECT = """
            SELECT claim_id,
                   claim_no,
                   policy_id,
                   policy_coverage_id,
                   claimant_user_id,
                   incident_at,
                   diagnosis_category,
                   event_fingerprint,
                   status,
                   submission_idempotency_key,
                   submitted_at,
                   created_at,
                   updated_at
            FROM public.clm_claim
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcClaimRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean ownsClaimablePolicyCoverage(Long policyId, Long policyCoverageId, Long userId, Instant incidentAt) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_policy policy
                JOIN public.ins_policy_version version
                  ON version.policy_id = policy.policy_id
                JOIN public.ins_policy_coverage coverage
                  ON coverage.policy_version_id = version.policy_version_id
                WHERE policy.policy_id = ?
                  AND coverage.policy_coverage_id = ?
                  AND policy.policyholder_user_id = ?
                  AND (policy.status IN ('ACTIVE', 'GRACE')
                       OR (policy.status = 'LAPSED' AND ? < policy.lapsed_at))
                """, Integer.class, policyId, policyCoverageId, userId, Timestamp.from(incidentAt));
        return count != null && count == 1;
    }

    @Override
    public Optional<InsuranceClaim> findByCoverageAndFingerprint(
            Long policyCoverageId,
            String fingerprint
    ) {
        return jdbcTemplate.query(
                CLAIM_SELECT + " WHERE policy_coverage_id = ? AND event_fingerprint = ?",
                this::mapClaim,
                policyCoverageId,
                fingerprint
        ).stream().findFirst();
    }

    @Override
    public InsuranceClaim createDraft(
            String claimNo,
            Long policyId,
            Long policyCoverageId,
            Long claimantUserId,
            Instant incidentAt,
            String diagnosisCategory,
            String fingerprint
    ) {
        Long claimId = jdbcTemplate.queryForObject("""
                INSERT INTO public.clm_claim (
                    claim_no,
                    policy_id,
                    policy_coverage_id,
                    claimant_user_id,
                    incident_at,
                    diagnosis_category,
                    event_fingerprint,
                    status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                ON CONFLICT (policy_coverage_id, event_fingerprint)
                DO UPDATE SET event_fingerprint = EXCLUDED.event_fingerprint
                RETURNING claim_id
                """,
                Long.class,
                claimNo,
                policyId,
                policyCoverageId,
                claimantUserId,
                Timestamp.from(incidentAt),
                diagnosisCategory,
                fingerprint
        );
        return findById(Objects.requireNonNull(claimId)).orElseThrow();
    }

    @Override
    public Optional<InsuranceClaim> findOwned(Long claimId, Long userId) {
        return jdbcTemplate.query(
                CLAIM_SELECT + " WHERE claim_id = ? AND claimant_user_id = ?",
                this::mapClaim,
                claimId,
                userId
        ).stream().findFirst();
    }

    @Override
    public Optional<InsuranceClaim> lockOwned(Long claimId, Long userId) {
        return jdbcTemplate.query(
                CLAIM_SELECT + " WHERE claim_id = ? AND claimant_user_id = ? FOR UPDATE",
                this::mapClaim,
                claimId,
                userId
        ).stream().findFirst();
    }

    @Override
    public ClaimEvidence saveEvidence(
            Long claimId,
            String evidenceType,
            String syntheticReference,
            String checksum,
            Map<String, Object> metadata,
            boolean verified
    ) {
        Long evidenceId = jdbcTemplate.queryForObject("""
                INSERT INTO public.clm_evidence (
                    claim_id,
                    evidence_type,
                    synthetic_reference,
                    checksum,
                    metadata_json,
                    verified
                ) VALUES (?, ?, ?, ?, CAST(? AS JSONB), ?)
                ON CONFLICT (claim_id, evidence_type, checksum)
                DO UPDATE SET synthetic_reference = EXCLUDED.synthetic_reference,
                              metadata_json = EXCLUDED.metadata_json,
                              verified = EXCLUDED.verified
                RETURNING claim_evidence_id
                """,
                Long.class,
                claimId,
                evidenceType,
                syntheticReference,
                checksum,
                toJson(metadata),
                verified
        );
        return findEvidenceById(Objects.requireNonNull(evidenceId)).orElseThrow();
    }

    @Override
    public List<ClaimEvidence> findEvidence(Long claimId) {
        return jdbcTemplate.query("""
                SELECT claim_evidence_id,
                       claim_id,
                       evidence_type,
                       synthetic_reference,
                       checksum,
                       metadata_json::TEXT AS metadata_json,
                       verified,
                       created_at
                FROM public.clm_evidence
                WHERE claim_id = ?
                ORDER BY evidence_type, claim_evidence_id
                """, this::mapEvidence, claimId);
    }

    @Override
    public ClaimAssessmentContext findAssessmentContext(Long claimId) {
        return jdbcTemplate.query("""
                SELECT claim.claim_id,
                       claim.claim_no,
                       claim.policy_id,
                       claim.policy_coverage_id,
                       claim.claimant_user_id,
                       claim.incident_at,
                       claim.diagnosis_category,
                       claim.event_fingerprint,
                       claim.status,
                       claim.submission_idempotency_key,
                       claim.submitted_at,
                       claim.created_at,
                       claim.updated_at,
                       version.policy_version_id,
                       policy.status AS policy_status,
                       coverage.product_coverage_id,
                       coverage.coverage_code_snapshot,
                       coverage.insured_amount,
                       coverage.currency_code,
                       coverage.coverage_start_at,
                       LEAST(coverage.coverage_end_at, policy.lapsed_at) AS coverage_end_at,
                       coverage.paid_benefit_count,
                       version.snapshot_json::TEXT AS snapshot_json
                FROM public.clm_claim claim
                JOIN public.ins_policy policy
                  ON policy.policy_id = claim.policy_id
                JOIN public.ins_policy_coverage coverage
                  ON coverage.policy_coverage_id = claim.policy_coverage_id
                JOIN public.ins_policy_version version
                  ON version.policy_version_id = coverage.policy_version_id
                WHERE claim.claim_id = ?
                FOR SHARE OF policy
                """, this::mapAssessmentContext, claimId).stream().findFirst().orElseThrow();
    }

    @Override
    public Optional<ClaimDecision> findDecision(Long claimId) {
        return jdbcTemplate.query("""
                SELECT claim_decision_id,
                       claim_id,
                       decision_version,
                       result,
                       benefit_amount,
                       currency_code,
                       reason_codes_json::TEXT AS reason_codes_json,
                       terms_clause_id,
                       rule_version,
                       input_hash,
                       actor_type,
                       decided_at
                FROM public.clm_decision
                WHERE claim_id = ?
                ORDER BY decision_version DESC
                LIMIT 1
                """, this::mapDecision, claimId).stream().findFirst();
    }

    @Override
    public InsuranceClaim saveDecision(
            Long claimId,
            String idempotencyKey,
            String result,
            BigDecimal benefitAmount,
            String currencyCode,
            List<String> reasonCodes,
            Long termsClauseId,
            String ruleVersion,
            String inputHash
    ) {
        jdbcTemplate.update("""
                INSERT INTO public.clm_decision (
                    claim_id,
                    decision_version,
                    result,
                    benefit_amount,
                    currency_code,
                    reason_codes_json,
                    terms_clause_id,
                    rule_version,
                    input_hash,
                    actor_type
                ) VALUES (?, 1, ?, ?, ?, CAST(? AS JSONB), ?, ?, ?, 'RULE_ENGINE')
                """,
                claimId,
                result,
                benefitAmount,
                currencyCode,
                toJson(reasonCodes),
                termsClauseId,
                ruleVersion,
                inputHash
        );
        jdbcTemplate.update("""
                UPDATE public.clm_claim
                SET status = ?,
                    submission_idempotency_key = ?,
                    submitted_at = NOW()
                WHERE claim_id = ?
                """, result, idempotencyKey, claimId);
        return findById(claimId).orElseThrow();
    }

    @Override
    public Optional<ClaimPayment> findPayment(Long claimDecisionId) {
        return jdbcTemplate.query("""
                SELECT claim_payment_id,
                       claim_decision_id,
                       payout_order_no,
                       amount,
                       currency_code,
                       status,
                       idempotency_key,
                       paid_at,
                       created_at,
                       updated_at
                FROM public.clm_payment
                WHERE claim_decision_id = ?
                """, this::mapPayment, claimDecisionId).stream().findFirst();
    }

    @Override
    public Optional<ClaimPayment> findPaymentByIdempotencyKey(String idempotencyKey) {
        return jdbcTemplate.query("""
                SELECT claim_payment_id,
                       claim_decision_id,
                       payout_order_no,
                       amount,
                       currency_code,
                       status,
                       idempotency_key,
                       paid_at,
                       created_at,
                       updated_at
                FROM public.clm_payment
                WHERE idempotency_key = ?
        """, this::mapPayment, idempotencyKey).stream().findFirst();
    }

    @Override
    public int lockPaidBenefitCount(Long policyCoverageId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT paid_benefit_count
                FROM public.ins_policy_coverage
                WHERE policy_coverage_id = ?
                FOR UPDATE
                """, Integer.class, policyCoverageId);
        return Objects.requireNonNull(count);
    }

    @Override
    public ClaimPayment payApprovedDecision(
            Long claimId,
            Long policyCoverageId,
            Long claimDecisionId,
            String payoutOrderNo,
            BigDecimal amount,
            String currencyCode,
            String idempotencyKey
    ) {
        Long claimPaymentId = jdbcTemplate.queryForObject("""
                INSERT INTO public.clm_payment (
                    claim_decision_id,
                    payout_order_no,
                    amount,
                    currency_code,
                    status,
                    idempotency_key,
                    paid_at
                ) VALUES (?, ?, ?, ?, 'PAID', ?, NOW())
                RETURNING claim_payment_id
                """,
                Long.class,
                claimDecisionId,
                payoutOrderNo,
                amount,
                currencyCode,
                idempotencyKey
        );
        jdbcTemplate.update("""
                UPDATE public.clm_claim
                SET status = 'PAID'
                WHERE claim_id = ?
                  AND status = 'APPROVED'
                """, claimId);
        jdbcTemplate.update("""
                UPDATE public.ins_policy_coverage
                SET paid_benefit_count = paid_benefit_count + 1
                WHERE policy_coverage_id = ?
                """, policyCoverageId);
        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_event (
                    event_id,
                    aggregate_type,
                    aggregate_id,
                    event_type,
                    payload_json
                ) VALUES (?, 'CLAIM', ?, 'CLAIM_BENEFIT_PAID', jsonb_build_object(
                    'claimId', ?,
                    'policyId', (
                        SELECT policy_id
                        FROM public.clm_claim
                        WHERE claim_id = ?
                    ),
                    'claimDecisionId', ?,
                    'amount', ?
                ))
                ON CONFLICT (event_id) DO NOTHING
                """,
                "CLAIM-PAID-" + claimId,
                claimId.toString(),
                claimId,
                claimId,
                claimDecisionId,
                amount
        );
        return findPaymentById(Objects.requireNonNull(claimPaymentId)).orElseThrow();
    }

    private Optional<InsuranceClaim> findById(Long claimId) {
        return jdbcTemplate.query(
                CLAIM_SELECT + " WHERE claim_id = ?",
                this::mapClaim,
                claimId
        ).stream().findFirst();
    }

    private Optional<ClaimEvidence> findEvidenceById(Long evidenceId) {
        return jdbcTemplate.query("""
                SELECT claim_evidence_id,
                       claim_id,
                       evidence_type,
                       synthetic_reference,
                       checksum,
                       metadata_json::TEXT AS metadata_json,
                       verified,
                       created_at
                FROM public.clm_evidence
                WHERE claim_evidence_id = ?
                """, this::mapEvidence, evidenceId).stream().findFirst();
    }

    private Optional<ClaimPayment> findPaymentById(Long paymentId) {
        return jdbcTemplate.query("""
                SELECT claim_payment_id,
                       claim_decision_id,
                       payout_order_no,
                       amount,
                       currency_code,
                       status,
                       idempotency_key,
                       paid_at,
                       created_at,
                       updated_at
                FROM public.clm_payment
                WHERE claim_payment_id = ?
                """, this::mapPayment, paymentId).stream().findFirst();
    }

    private InsuranceClaim mapClaim(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InsuranceClaim(
                resultSet.getLong("claim_id"),
                resultSet.getString("claim_no"),
                resultSet.getLong("policy_id"),
                resultSet.getLong("policy_coverage_id"),
                resultSet.getLong("claimant_user_id"),
                resultSet.getTimestamp("incident_at").toInstant(),
                resultSet.getString("diagnosis_category"),
                resultSet.getString("event_fingerprint"),
                resultSet.getString("status"),
                resultSet.getString("submission_idempotency_key"),
                toInstant(resultSet, "submitted_at"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private ClaimEvidence mapEvidence(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ClaimEvidence(
                resultSet.getLong("claim_evidence_id"),
                resultSet.getLong("claim_id"),
                resultSet.getString("evidence_type"),
                resultSet.getString("synthetic_reference"),
                resultSet.getString("checksum"),
                fromJson(resultSet.getString("metadata_json"), new TypeReference<>() {
                }),
                resultSet.getBoolean("verified"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private ClaimDecision mapDecision(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ClaimDecision(
                resultSet.getLong("claim_decision_id"),
                resultSet.getLong("claim_id"),
                resultSet.getInt("decision_version"),
                resultSet.getString("result"),
                resultSet.getBigDecimal("benefit_amount"),
                resultSet.getString("currency_code"),
                fromJson(resultSet.getString("reason_codes_json"), new TypeReference<>() {
                }),
                resultSet.getLong("terms_clause_id"),
                resultSet.getString("rule_version"),
                resultSet.getString("input_hash"),
                resultSet.getString("actor_type"),
                resultSet.getTimestamp("decided_at").toInstant()
        );
    }

    private ClaimPayment mapPayment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ClaimPayment(
                resultSet.getLong("claim_payment_id"),
                resultSet.getLong("claim_decision_id"),
                resultSet.getString("payout_order_no"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("currency_code"),
                resultSet.getString("status"),
                resultSet.getString("idempotency_key"),
                toInstant(resultSet, "paid_at"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private ClaimAssessmentContext mapAssessmentContext(ResultSet resultSet, int rowNumber)
            throws SQLException {
        InsuranceClaim claim = mapClaim(resultSet, rowNumber);
        PolicySnapshot snapshot = fromJson(
                resultSet.getString("snapshot_json"),
                PolicySnapshot.class
        );
        Long productCoverageId = resultSet.getLong("product_coverage_id");
        QuoteSnapshot.CoverageSnapshot coverageSnapshot = snapshot.quote().coverages().stream()
                .filter(coverage -> coverage.productCoverageId().equals(productCoverageId))
                .findFirst()
                .orElseThrow();
        PolicySnapshot.ClaimRuleSnapshot rule = snapshot.claimRules().stream()
                .filter(item -> item.productCoverageId().equals(productCoverageId))
                .findFirst()
                .orElseThrow();
        return new ClaimAssessmentContext(
                claim,
                resultSet.getLong("policy_version_id"),
                resultSet.getString("policy_status"),
                productCoverageId,
                resultSet.getString("coverage_code_snapshot"),
                resultSet.getBigDecimal("insured_amount"),
                resultSet.getString("currency_code"),
                resultSet.getTimestamp("coverage_start_at").toInstant(),
                toInstant(resultSet, "coverage_end_at"),
                resultSet.getInt("paid_benefit_count"),
                coverageSnapshot.reductionPeriodDays(),
                coverageSnapshot.reductionRate(),
                snapshot.quote().termsHash(),
                rule.ruleVersion(),
                rule.diagnosisCategories(),
                rule.requiredEvidence(),
                rule.firstDiagnosisOnly(),
                rule.eligibilityClauseId(),
                rule.missingEvidenceClauseId(),
                rule.denialClauseId()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("청구 원장 JSON을 직렬화하지 못했습니다.", exception);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("청구 원장 JSON을 역직렬화하지 못했습니다.", exception);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("청구 원장 JSON을 역직렬화하지 못했습니다.", exception);
        }
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

}
