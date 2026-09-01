package com.capsule.insurance.application.application;

import com.capsule.insurance.application.application.port.ApplicationRepository;
import com.capsule.insurance.application.domain.ApplicationConsent;
import com.capsule.insurance.application.domain.ApplicationQuote;
import com.capsule.insurance.application.domain.DisclosureAnswers;
import com.capsule.insurance.application.domain.InsuranceApplication;
import com.capsule.insurance.application.domain.UnderwritingDecision;
import com.capsule.insurance.application.dto.ApplicationResponse;
import com.capsule.insurance.application.dto.CreateApplicationRequest;
import com.capsule.insurance.application.dto.CreateConsentRequest;
import com.capsule.insurance.application.dto.ReplaceDisclosuresRequest;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ApplicationService {

    private static final String UNDERWRITING_RULE_VERSION = "UW-DEMO-CANCER-1.0.0";
    private static final Set<String> REQUIRED_CONSENT_TYPES = Set.of(
            "PRODUCT_TERMS",
            "PRODUCT_EXPLANATION"
    );

    private final ApplicationRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ApplicationService(
            ApplicationRepository repository,
            PlatformTransactionManager transactionManager
    ) {
        this(repository, transactionManager, Clock.systemUTC());
    }

    ApplicationService(
            ApplicationRepository repository,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public ApplicationResponse create(Long userId, CreateApplicationRequest request) {
        InsuranceApplication application = Objects.requireNonNull(transactionTemplate.execute(status -> {
            ApplicationQuote quote = repository.lockOwnedQuote(request.quoteId(), userId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "청약에 사용할 견적을 찾을 수 없습니다."
                    ));

            InsuranceApplication existing = repository.findByQuote(quote.quoteId()).orElse(null);
            if (existing != null) {
                return existing;
            }
            if (quote.expiresAt().isBefore(Instant.now(clock))) {
                repository.expireQuote(quote.quoteId());
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "만료된 견적으로 청약할 수 없습니다."
                );
            }
            if (!"ISSUED".equals(quote.status())) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "ISSUED 견적만 청약에 사용할 수 있습니다."
                );
            }

            InsuranceApplication created = repository.createDraft(
                    "A-" + UUID.randomUUID(),
                    quote.quoteId(),
                    userId
            );
            repository.markQuoteUsed(quote.quoteId());
            return created;
        }));
        return toResponse(application);
    }

    public ApplicationResponse replaceDisclosures(
            Long userId,
            Long applicationId,
            ReplaceDisclosuresRequest request
    ) {
        InsuranceApplication application = Objects.requireNonNull(transactionTemplate.execute(status -> {
            InsuranceApplication locked = getLockedOwned(applicationId, userId);
            requireMutable(locked);
            DisclosureAnswers answers = new DisclosureAnswers(
                    request.diagnosedCancer(),
                    request.underCancerExamination(),
                    request.recentHospitalization()
            );
            if (!answers.complete()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "모든 고지 질문에 답해야 합니다.");
            }
            return repository.replaceDisclosure(applicationId, answers);
        }));
        return toResponse(application);
    }

    public ApplicationResponse recordConsent(
            Long userId,
            Long applicationId,
            CreateConsentRequest request
    ) {
        InsuranceApplication application = Objects.requireNonNull(transactionTemplate.execute(status -> {
            InsuranceApplication locked = getLockedOwned(applicationId, userId);
            requireMutable(locked);
            if (!REQUIRED_CONSENT_TYPES.contains(request.consentType())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "지원하지 않는 필수 동의 종류입니다."
                );
            }
            if (!locked.termsDocumentHash().equals(request.documentHash())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "견적에 고정된 약관 hash와 동의 문서 hash가 다릅니다."
                );
            }

            ApplicationConsent existing = repository
                    .findConsent(applicationId, request.consentType())
                    .orElse(null);
            if (existing != null) {
                if (existing.documentHash().equals(request.documentHash())
                        && existing.agreed() == request.agreed()) {
                    return locked;
                }
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "이미 다른 내용으로 기록된 동의가 있습니다."
                );
            }

            repository.saveConsent(
                    applicationId,
                    request.consentType(),
                    locked.termsDocumentId(),
                    request.documentHash(),
                    true,
                    request.agreed(),
                    userId,
                    "CONSENT-" + UUID.randomUUID()
            );
            return locked;
        }));
        return toResponse(application);
    }

    public ApplicationResponse submit(Long userId, Long applicationId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        InsuranceApplication application = Objects.requireNonNull(transactionTemplate.execute(status -> {
            InsuranceApplication locked = getLockedOwned(applicationId, userId);
            if (locked.submissionIdempotencyKey() != null) {
                if (locked.submissionIdempotencyKey().equals(idempotencyKey)) {
                    return locked;
                }
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "다른 idempotency key로 이미 제출된 청약입니다."
                );
            }
            if (!"DISCLOSURE_COMPLETED".equals(locked.status())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "필수 고지가 완료된 청약만 제출할 수 있습니다."
                );
            }

            List<ApplicationConsent> consents = repository.findConsents(applicationId);
            requireConsents(locked, consents);
            DecisionResult result = decide(locked.disclosureAnswers());
            String inputHash = underwritingInputHash(locked, consents);
            return repository.saveDecision(
                    applicationId,
                    idempotencyKey,
                    result.decision(),
                    UNDERWRITING_RULE_VERSION,
                    result.reasonCodes(),
                    inputHash
            );
        }));
        return toResponse(application);
    }

    public ApplicationResponse get(Long userId, Long applicationId) {
        return repository.findOwned(applicationId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "청약을 찾을 수 없습니다."
                ));
    }

    private InsuranceApplication getLockedOwned(Long applicationId, Long userId) {
        return repository.lockOwned(applicationId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "청약을 찾을 수 없습니다."
                ));
    }

    private void requireMutable(InsuranceApplication application) {
        if (!Set.of("DRAFT", "DISCLOSURE_COMPLETED").contains(application.status())) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "제출 전 청약만 수정할 수 있습니다."
            );
        }
    }

    private void requireConsents(
            InsuranceApplication application,
            List<ApplicationConsent> consents
    ) {
        boolean allRequiredAgreed = REQUIRED_CONSENT_TYPES.stream().allMatch(requiredType ->
                consents.stream().anyMatch(consent ->
                        consent.consentType().equals(requiredType)
                                && consent.required()
                                && consent.agreed()
                                && consent.documentHash().equals(application.termsDocumentHash())
                )
        );
        if (!allRequiredAgreed) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "필수 약관·상품설명 동의가 완료되지 않았습니다."
            );
        }
    }

    private DecisionResult decide(DisclosureAnswers answers) {
        if (Boolean.TRUE.equals(answers.diagnosedCancer())) {
            return new DecisionResult("DECLINED", List.of("PREEXISTING_CANCER_DISCLOSED"));
        }
        if (Boolean.TRUE.equals(answers.underCancerExamination())) {
            return new DecisionResult("MANUAL_REVIEW", List.of("PENDING_CANCER_EXAMINATION"));
        }
        if (Boolean.TRUE.equals(answers.recentHospitalization())) {
            return new DecisionResult("MANUAL_REVIEW", List.of("RECENT_HOSPITALIZATION_REVIEW"));
        }
        return new DecisionResult("APPROVED", List.of("STANDARD_ACCEPT"));
    }

    private String underwritingInputHash(
            InsuranceApplication application,
            List<ApplicationConsent> consents
    ) {
        String consentInput = consents.stream()
                .sorted(Comparator.comparing(ApplicationConsent::consentType))
                .map(consent -> consent.consentType() + ":" + consent.documentHash() + ":" + consent.agreed())
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        DisclosureAnswers answers = application.disclosureAnswers();
        String canonicalInput = application.applicationId()
                + "|" + answers.diagnosedCancer()
                + "|" + answers.underCancerExamination()
                + "|" + answers.recentHospitalization()
                + "|" + consentInput
                + "|" + UNDERWRITING_RULE_VERSION;
        return sha256(canonicalInput);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Idempotency-Key가 필요합니다.");
        }
        if (idempotencyKey.length() > 255) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Idempotency-Key는 255자 이하여야 합니다.");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private ApplicationResponse toResponse(InsuranceApplication application) {
        List<ApplicationResponse.ConsentResponse> consents = repository
                .findConsents(application.applicationId())
                .stream()
                .map(this::toConsentResponse)
                .toList();
        ApplicationResponse.UnderwritingDecisionResponse decision = repository
                .findLatestDecision(application.applicationId())
                .map(this::toDecisionResponse)
                .orElse(null);
        return new ApplicationResponse(
                application.applicationId(),
                application.applicationNo(),
                application.quoteId(),
                application.status(),
                application.disclosureAnswers(),
                application.termsDocumentHash(),
                consents,
                decision,
                application.submittedAt(),
                application.createdAt(),
                application.updatedAt()
        );
    }

    private ApplicationResponse.ConsentResponse toConsentResponse(ApplicationConsent consent) {
        return new ApplicationResponse.ConsentResponse(
                consent.consentId(),
                consent.consentType(),
                consent.documentHash(),
                consent.required(),
                consent.agreed(),
                consent.agreedAt(),
                consent.requestId()
        );
    }

    private ApplicationResponse.UnderwritingDecisionResponse toDecisionResponse(
            UnderwritingDecision decision
    ) {
        return new ApplicationResponse.UnderwritingDecisionResponse(
                decision.underwritingDecisionId(),
                decision.decisionVersion(),
                decision.decision(),
                decision.ruleVersion(),
                decision.reasonCodes(),
                decision.inputHash(),
                decision.decidedAt()
        );
    }

    private record DecisionResult(String decision, List<String> reasonCodes) {
    }
}
