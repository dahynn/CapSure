package com.capsule.insurance.claim.application.port;

import com.capsule.insurance.claim.domain.ClaimAssessmentContext;
import com.capsule.insurance.claim.domain.ClaimDecision;
import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.claim.domain.ClaimPayment;
import com.capsule.insurance.claim.domain.InsuranceClaim;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ClaimRepository {

    boolean ownsActivePolicyCoverage(Long policyId, Long policyCoverageId, Long userId);

    Optional<InsuranceClaim> findByCoverageAndFingerprint(Long policyCoverageId, String fingerprint);

    InsuranceClaim createDraft(
            String claimNo,
            Long policyId,
            Long policyCoverageId,
            Long claimantUserId,
            Instant incidentAt,
            String diagnosisCategory,
            String fingerprint
    );

    Optional<InsuranceClaim> findOwned(Long claimId, Long userId);

    Optional<InsuranceClaim> lockOwned(Long claimId, Long userId);

    ClaimEvidence saveEvidence(
            Long claimId,
            String evidenceType,
            String syntheticReference,
            String checksum,
            Map<String, Object> metadata,
            boolean verified
    );

    List<ClaimEvidence> findEvidence(Long claimId);

    ClaimAssessmentContext findAssessmentContext(Long claimId);

    Optional<ClaimDecision> findDecision(Long claimId);

    InsuranceClaim saveDecision(
            Long claimId,
            String idempotencyKey,
            String result,
            BigDecimal benefitAmount,
            String currencyCode,
            List<String> reasonCodes,
            Long termsClauseId,
            String ruleVersion,
            String inputHash
    );

    Optional<ClaimPayment> findPayment(Long claimDecisionId);

    Optional<ClaimPayment> findPaymentByIdempotencyKey(String idempotencyKey);

    int lockPaidBenefitCount(Long policyCoverageId);

    ClaimPayment payApprovedDecision(
            Long claimId,
            Long policyCoverageId,
            Long claimDecisionId,
            String payoutOrderNo,
            BigDecimal amount,
            String currencyCode,
            String idempotencyKey
    );
}
