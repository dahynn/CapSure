package com.capsule.insurance.assistantai.claim.application;

import com.capsule.insurance.assistantai.application.ExternalModelCallBlockedException;
import com.capsule.insurance.assistantai.application.InsuranceAssistantAuditRecord;
import com.capsule.insurance.assistantai.application.InsuranceAssistantInputPolicy;
import com.capsule.insurance.assistantai.application.port.InsuranceAssistantAuditRecorder;
import com.capsule.insurance.assistantai.claim.application.port.ClaimAssessmentAssistantGateway;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentSourceReference;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantDecision;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantEvidence;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantRequest;
import com.capsule.insurance.assistantai.domain.InsuranceEvidenceType;
import com.capsule.insurance.catalog.application.port.CancerProductQueryRepository;
import com.capsule.insurance.catalog.domain.TermsClause;
import com.capsule.insurance.claim.application.port.ClaimRepository;
import com.capsule.insurance.claim.domain.ClaimAssessmentContext;
import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 보험금 심사 보조 PoC입니다. 이 서비스는 지급 승인·거절·의학적 판단을 내리거나 청구 상태를 바꾸지 않습니다.
 */
@Service
public class ClaimAssessmentAssistantService {

    private final ClaimRepository claimRepository;
    private final CancerProductQueryRepository cancerProductQueryRepository;
    private final InsuranceAssistantInputPolicy inputPolicy;
    private final ClaimAssessmentAssistantGateway gateway;
    private final InsuranceAssistantAuditRecorder auditRecorder;
    private final ClaimCopilotReviewService reviewService;

    public ClaimAssessmentAssistantService(
            ClaimRepository claimRepository,
            CancerProductQueryRepository cancerProductQueryRepository,
            InsuranceAssistantInputPolicy inputPolicy,
            ClaimAssessmentAssistantGateway gateway,
            InsuranceAssistantAuditRecorder auditRecorder,
            ClaimCopilotReviewService reviewService
    ) {
        this.claimRepository = claimRepository;
        this.cancerProductQueryRepository = cancerProductQueryRepository;
        this.inputPolicy = inputPolicy;
        this.gateway = gateway;
        this.auditRecorder = auditRecorder;
        this.reviewService = reviewService;
    }

    public AssistantResult createDraft(Long claimantUserId, Long claimId, String requestId, String operatorInstruction) {
        claimRepository.findOwned(claimId, claimantUserId).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "보험금 청구를 찾을 수 없습니다."));
        return createDraftForClaim(claimId, requestId, operatorInstruction);
    }

    /** /ops 경로에서 관리자 역할이 인증된 뒤 호출하는 담당자용 진입점입니다. */
    public AssistantResult createDraftForOperator(Long claimId, String requestId, String operatorInstruction) {
        claimRepository.findById(claimId).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "보험금 청구를 찾을 수 없습니다."));
        return createDraftForClaim(claimId, requestId, operatorInstruction);
    }

    private AssistantResult createDraftForClaim(Long claimId, String requestId, String operatorInstruction) {
        ClaimAssessmentContext context = claimRepository.findAssessmentContext(claimId);
        if (!context.claim().claimId().equals(claimId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "청구 근거가 일치하지 않습니다.");
        }

        List<ClaimAssessmentSourceReference> baseSources = baseSources(context);
        List<String> evidenceIds = baseSources.stream().map(ClaimAssessmentSourceReference::sourceId).toList();
        InsuranceAssistantInputPolicy.PolicyEvaluation policyEvaluation = inputPolicy.evaluate(new InsuranceAssistantRequest(
                requestId,
                operatorInstruction,
                List.of(
                        new InsuranceAssistantEvidence(baseSources.getFirst().sourceId(), InsuranceEvidenceType.TERMS_DOCUMENT,
                                baseSources.getFirst().version()),
                        new InsuranceAssistantEvidence(baseSources.get(1).sourceId(), InsuranceEvidenceType.PRODUCT_SOURCE,
                                baseSources.get(1).version())
                )
        ));
        if (policyEvaluation != InsuranceAssistantInputPolicy.PolicyEvaluation.ALLOWED) {
            InsuranceAssistantDecision decision = policyEvaluation == InsuranceAssistantInputPolicy.PolicyEvaluation.SENSITIVE_INPUT
                    ? InsuranceAssistantDecision.REJECTED_SENSITIVE_INPUT
                    : InsuranceAssistantDecision.REJECTED_UNAPPROVED_EVIDENCE;
            audit(requestId, decision, evidenceIds);
            return AssistantResult.blocked(decision);
        }

        List<ClaimAssessmentSourceReference> selectedTerms = selectAllowedTerms(context, operatorInstruction);
        if (selectedTerms.isEmpty()) {
            ClaimAssessmentAssistantDraft draft = new ClaimAssessmentAssistantDraft(
                    requestId,
                    List.of(),
                    List.of("TERMS_CLAUSE_REFERENCE"),
                    List.of("해당 청구의 증권 버전에 연결된 약관 조항을 확인해 주세요."),
                    true,
                    true
            );
            audit(requestId, InsuranceAssistantDecision.REVIEW_REQUIRED, evidenceIds);
            return reviewRequired(claimId, draft);
        }
        List<ClaimAssessmentSourceReference> allowedSources = new java.util.ArrayList<>(selectedTerms);
        allowedSources.add(baseSources.get(1));

        List<String> verifiedEvidenceTypes = claimRepository.findEvidence(claimId).stream()
                .filter(ClaimEvidence::verified)
                .map(ClaimEvidence::evidenceType)
                .distinct()
                .toList();
        List<String> missingEvidence = context.requiredEvidence().stream()
                .filter(required -> !verifiedEvidenceTypes.contains(required))
                .toList();
        if (!missingEvidence.isEmpty()) {
            ClaimAssessmentAssistantDraft draft = new ClaimAssessmentAssistantDraft(
                    requestId,
                    allowedSources,
                    missingEvidence,
                    missingEvidence.stream().map(type -> type + " 서류의 진위와 누락 여부를 확인해 주세요.").toList(),
                    true,
                    true
            );
            audit(requestId, InsuranceAssistantDecision.REVIEW_REQUIRED, evidenceIds);
            return reviewRequired(claimId, draft);
        }

        ClaimAssessmentAssistantModelRequest modelRequest = new ClaimAssessmentAssistantModelRequest(
                requestId, allowedSources, context.requiredEvidence(), verifiedEvidenceTypes);
        try {
            ClaimAssessmentAssistantModelDraft modelDraft = gateway.generate(modelRequest);
            ClaimAssessmentAssistantDraft draft = new ClaimAssessmentAssistantDraft(
                    requestId,
                    modelDraft.termsToCheck().stream().filter(allowedSources::contains).toList(),
                    modelDraft.possibleMissingEvidence(),
                    modelDraft.additionalQuestions(),
                    modelDraft.evidenceInsufficient(),
                    true
            );
            audit(requestId, InsuranceAssistantDecision.REVIEW_REQUIRED, evidenceIds);
            return reviewRequired(claimId, draft);
        } catch (ExternalModelCallBlockedException exception) {
            audit(requestId, InsuranceAssistantDecision.GATEWAY_BLOCKED, evidenceIds);
            return AssistantResult.blocked(InsuranceAssistantDecision.GATEWAY_BLOCKED);
        }
    }

    private List<ClaimAssessmentSourceReference> baseSources(ClaimAssessmentContext context) {
        return List.of(
                new ClaimAssessmentSourceReference(
                        "TERMS_DOCUMENT", "terms-hash:" + context.termsHash(), context.ruleVersion(), null),
                new ClaimAssessmentSourceReference(
                        "POLICY_PRODUCT_VERSION", "policy-version:" + context.policyVersionId(), context.coverageCode(), null)
        );
    }

    /**
     * 현재 청구 snapshot이 가리키는 세 조항 안에서만 단순 키워드 선택을 수행합니다.
     * 다른 상품·약관 문서의 조항은 hash가 일치하지 않아 제외합니다.
     */
    private List<ClaimAssessmentSourceReference> selectAllowedTerms(
            ClaimAssessmentContext context,
            String operatorInstruction
    ) {
        List<Long> clauseIds = java.util.stream.Stream.of(
                        context.eligibilityClauseId(), context.missingEvidenceClauseId(), context.denialClauseId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<TermsClause> candidates = clauseIds.stream()
                .map(cancerProductQueryRepository::findTermsClause)
                .flatMap(java.util.Optional::stream)
                .filter(clause -> context.termsHash().equals(clause.documentHash()))
                .toList();
        List<TermsClause> matched = candidates.stream()
                .filter(clause -> matches(operatorInstruction, clause))
                .toList();
        List<TermsClause> selected = matched.isEmpty() ? candidates : matched;
        return selected.stream()
                .map(clause -> new ClaimAssessmentSourceReference(
                        "TERMS_CLAUSE", "terms-clause:" + clause.clauseCode(),
                        clause.documentVersion(), clause.termsClauseId()))
                .toList();
    }

    private boolean matches(String operatorInstruction, TermsClause clause) {
        String normalizedInstruction = operatorInstruction.toLowerCase(java.util.Locale.ROOT);
        return java.util.Arrays.stream(normalizedInstruction.split("\\s+"))
                .filter(token -> token.length() >= 2)
                .anyMatch(token -> clause.title().toLowerCase(java.util.Locale.ROOT).contains(token)
                        || clause.body().toLowerCase(java.util.Locale.ROOT).contains(token));
    }

    private void audit(String requestId, InsuranceAssistantDecision decision, List<String> evidenceIds) {
        auditRecorder.record(new InsuranceAssistantAuditRecord(requestId, decision, evidenceIds, Instant.now()));
    }

    private AssistantResult reviewRequired(Long claimId, ClaimAssessmentAssistantDraft draft) {
        ClaimCopilotReview review = reviewService.registerDraft(claimId, draft.requestId());
        return AssistantResult.reviewRequired(draft, review);
    }

    public record AssistantResult(
            ClaimAssessmentAssistantDraft draft,
            InsuranceAssistantDecision decision,
            ClaimCopilotReview review
    ) {
        static AssistantResult reviewRequired(ClaimAssessmentAssistantDraft draft, ClaimCopilotReview review) {
            return new AssistantResult(draft, InsuranceAssistantDecision.REVIEW_REQUIRED, review);
        }

        static AssistantResult blocked(InsuranceAssistantDecision decision) {
            return new AssistantResult(null, decision, null);
        }
    }
}
