package com.capsule.insurance.application.dto;

import com.capsule.insurance.application.domain.DisclosureAnswers;
import java.time.Instant;
import java.util.List;

public record ApplicationResponse(
        Long applicationId,
        String applicationNo,
        Long quoteId,
        String status,
        DisclosureAnswers disclosureAnswers,
        String termsDocumentHash,
        List<ConsentResponse> consents,
        UnderwritingDecisionResponse underwritingDecision,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public record ConsentResponse(
            Long consentId,
            String consentType,
            String documentHash,
            boolean required,
            boolean agreed,
            Instant agreedAt,
            String requestId
    ) {
    }

    public record UnderwritingDecisionResponse(
            Long underwritingDecisionId,
            int decisionVersion,
            String decision,
            String ruleVersion,
            List<String> reasonCodes,
            String inputHash,
            Instant decidedAt
    ) {
    }
}
