package com.capsule.insurance.policy.domain;

import java.time.Instant;
import java.util.List;

public record InsurancePolicy(
        Long policyId,
        String policyNo,
        Long applicationId,
        Long policyholderUserId,
        Long insuredUserId,
        Long beneficiaryUserId,
        String status,
        Instant activatedAt,
        Instant createdAt,
        Instant updatedAt,
        PolicyVersion policyVersion
) {

    public record PolicyVersion(
            Long policyVersionId,
            int version,
            Long productVersionId,
            Long termsDocumentId,
            Instant validFrom,
            Instant validTo,
            PolicySnapshot snapshot,
            List<PolicyCoverage> coverages
    ) {
        public PolicyVersion {
            coverages = List.copyOf(coverages);
        }
    }

    public record PolicyCoverage(
            Long policyCoverageId,
            Long productCoverageId,
            String coverageCode,
            java.math.BigDecimal insuredAmount,
            String currencyCode,
            Instant coverageStartAt,
            Instant coverageEndAt
    ) {
    }
}
