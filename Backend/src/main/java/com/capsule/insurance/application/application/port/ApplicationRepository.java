package com.capsule.insurance.application.application.port;

import com.capsule.insurance.application.domain.ApplicationConsent;
import com.capsule.insurance.application.domain.ApplicationQuote;
import com.capsule.insurance.application.domain.DisclosureAnswers;
import com.capsule.insurance.application.domain.InsuranceApplication;
import com.capsule.insurance.application.domain.UnderwritingDecision;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {

    Optional<ApplicationQuote> lockOwnedQuote(Long quoteId, Long userId);

    Optional<InsuranceApplication> findByQuote(Long quoteId);

    InsuranceApplication createDraft(String applicationNo, Long quoteId, Long userId);

    void markQuoteUsed(Long quoteId);

    void expireQuote(Long quoteId);

    Optional<InsuranceApplication> findOwned(Long applicationId, Long userId);

    Optional<InsuranceApplication> lockOwned(Long applicationId, Long userId);

    InsuranceApplication replaceDisclosure(Long applicationId, DisclosureAnswers answers);

    Optional<ApplicationConsent> findConsent(Long applicationId, String consentType);

    List<ApplicationConsent> findConsents(Long applicationId);

    ApplicationConsent saveConsent(
            Long applicationId,
            String consentType,
            Long termsDocumentId,
            String documentHash,
            boolean required,
            boolean agreed,
            Long actorUserId,
            String requestId
    );

    Optional<UnderwritingDecision> findLatestDecision(Long applicationId);

    InsuranceApplication saveDecision(
            Long applicationId,
            String idempotencyKey,
            String decision,
            String ruleVersion,
            List<String> reasonCodes,
            String inputHash
    );
}
