package com.capsule.insurance.assistantai.claim.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.capsule.insurance.assistantai.application.InsuranceAssistantAuditRecord;
import com.capsule.insurance.assistantai.application.InsuranceAssistantInputPolicy;
import com.capsule.insurance.assistantai.application.port.InsuranceAssistantAuditRecorder;
import com.capsule.insurance.assistantai.claim.application.port.ClaimAssessmentAssistantGateway;
import com.capsule.insurance.assistantai.claim.application.port.ClaimCopilotReviewRepository;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEvent;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEventType;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentSourceReference;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewStatus;
import com.capsule.insurance.assistantai.claim.infra.DefaultBlockedClaimAssessmentAssistantGateway;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantDecision;
import com.capsule.insurance.catalog.application.port.CancerProductQueryRepository;
import com.capsule.insurance.catalog.domain.TermsClause;
import com.capsule.insurance.claim.application.port.ClaimRepository;
import com.capsule.insurance.claim.domain.ClaimAssessmentContext;
import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.claim.domain.InsuranceClaim;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimAssessmentAssistantServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private CancerProductQueryRepository cancerProductQueryRepository;

    private RecordingGateway gateway;
    private RecordingAudit audit;
    private ClaimCopilotReviewService reviewService;
    private ClaimAssessmentAssistantService service;

    @BeforeEach
    void setUp() {
        gateway = new RecordingGateway();
        audit = new RecordingAudit();
        reviewService = new ClaimCopilotReviewService(new RecordingReviewRepository());
        service = new ClaimAssessmentAssistantService(
                claimRepository, cancerProductQueryRepository, new InsuranceAssistantInputPolicy(), gateway, audit, reviewService);
        when(claimRepository.findOwned(100L, 7L)).thenReturn(Optional.of(context("terms-a").claim()));
        when(cancerProductQueryRepository.findTermsClause(11L)).thenReturn(Optional.of(clause(11L, "ARTICLE-09", "일반암 진단비", "약관 기준", "terms-a")));
        when(cancerProductQueryRepository.findTermsClause(12L)).thenReturn(Optional.of(clause(12L, "ARTICLE-13", "진단확정 증빙", "필수 서류", "terms-a")));
        when(cancerProductQueryRepository.findTermsClause(13L)).thenReturn(Optional.of(clause(13L, "ARTICLE-14", "지급하지 않는 사유", "제외 기준", "terms-a")));
    }

    @Test
    void onlyTheClaimBoundPolicyAndTermsSourcesAreForwardedAndMixedSourcesAreRemoved() {
        ClaimAssessmentContext context = context("terms-a");
        when(claimRepository.findAssessmentContext(100L)).thenReturn(context);
        when(claimRepository.findEvidence(100L)).thenReturn(List.of(verified("DIAGNOSIS_CERTIFICATE")));
        gateway.reply = new ClaimAssessmentAssistantModelDraft(
                List.of(
                        new ClaimAssessmentSourceReference("TERMS_CLAUSE", "terms-clause:ARTICLE-09", "1.0.0", 11L),
                        new ClaimAssessmentSourceReference("TERMS_DOCUMENT", "terms-hash:other-product", "rule-v9", 99L)
                ),
                List.of(), List.of("약관 조항 적용 여부를 확인해 주세요."), false);

        var result = service.createDraft(7L, 100L, "request-1", "약관 기준을 정리해줘");

        assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.REVIEW_REQUIRED);
        assertThat(gateway.requests).singleElement().satisfies(request ->
                        assertThat(request.allowedSources()).extracting(ClaimAssessmentSourceReference::sourceId)
                        .containsExactly("terms-clause:ARTICLE-09", "policy-version:55"));
        assertThat(result.draft().termsToCheck()).extracting(ClaimAssessmentSourceReference::sourceId)
                .containsExactly("terms-clause:ARTICLE-09");
    }

    @Test
    void insufficientEvidenceCreatesQuestionsButNeverAnAutomaticDecision() {
        when(claimRepository.findAssessmentContext(100L)).thenReturn(context("terms-a"));
        when(claimRepository.findEvidence(100L)).thenReturn(List.of());

        var result = service.createDraft(7L, 100L, "request-2", "누락 서류를 확인해줘");

        assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.REVIEW_REQUIRED);
        assertThat(result.draft().evidenceInsufficient()).isTrue();
        assertThat(result.draft().possibleMissingEvidence()).containsExactly("DIAGNOSIS_CERTIFICATE");
        assertThat(result.draft().manualReviewRequired()).isTrue();
        assertThat(result.draft().canFinalizeClaimDecision()).isFalse();
        assertThat(result.review().status()).isEqualTo(ClaimCopilotReviewStatus.DRAFT);
        assertThat(reviewService.get(100L, "request-2").status()).isEqualTo(ClaimCopilotReviewStatus.DRAFT);
        assertThat(gateway.requests).isEmpty();
    }

    @Test
    void termsFromAnotherProductOrVersionAreNotUsedAsClaimEvidence() {
        when(claimRepository.findAssessmentContext(100L)).thenReturn(context("terms-a"));
        when(claimRepository.findEvidence(100L)).thenReturn(List.of(verified("DIAGNOSIS_CERTIFICATE")));
        when(cancerProductQueryRepository.findTermsClause(11L))
                .thenReturn(Optional.of(clause(11L, "ARTICLE-09", "일반암 진단비", "약관 기준", "other-product-hash")));
        when(cancerProductQueryRepository.findTermsClause(12L)).thenReturn(Optional.empty());
        when(cancerProductQueryRepository.findTermsClause(13L)).thenReturn(Optional.empty());

        var result = service.createDraft(7L, 100L, "request-isolated", "약관 기준을 정리해줘");

        assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.REVIEW_REQUIRED);
        assertThat(result.draft().evidenceInsufficient()).isTrue();
        assertThat(result.draft().possibleMissingEvidence()).containsExactly("TERMS_CLAUSE_REFERENCE");
        assertThat(gateway.requests).isEmpty();
    }

    @Test
    void sensitiveOperatorInstructionIsNeverForwardedToGateway() {
        when(claimRepository.findAssessmentContext(100L)).thenReturn(context("terms-a"));
        String sensitivePhone = "010-1234-5678";

        var result = service.createDraft(7L, 100L, "request-3", "고객 전화번호 " + sensitivePhone + "를 참고해줘");

        assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.REJECTED_SENSITIVE_INPUT);
        assertThat(gateway.requests).isEmpty();
        assertThat(audit.records).singleElement().satisfies(record ->
                assertThat(record.toString()).doesNotContain(sensitivePhone));
    }

    @Test
    void defaultGatewayBlocksEgressAndStillLeavesNoFinalDecisionPath() {
        ClaimAssessmentAssistantService blockedService = new ClaimAssessmentAssistantService(
                claimRepository, cancerProductQueryRepository, new InsuranceAssistantInputPolicy(), new DefaultBlockedClaimAssessmentAssistantGateway(), audit, reviewService);
        when(claimRepository.findAssessmentContext(100L)).thenReturn(context("terms-a"));
        when(claimRepository.findEvidence(100L)).thenReturn(List.of(verified("DIAGNOSIS_CERTIFICATE")));

        var result = blockedService.createDraft(7L, 100L, "request-4", "약관 기준을 정리해줘");

        assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.GATEWAY_BLOCKED);
        assertThat(result.draft()).isNull();
    }

    @Test
    void reviewerStatusCanBeRecordedWithoutUnlockingAnAutomaticClaimDecision() {
        when(claimRepository.findAssessmentContext(100L)).thenReturn(context("terms-a"));
        when(claimRepository.findEvidence(100L)).thenReturn(List.of());

        var result = service.createDraft(7L, 100L, "request-review", "누락 서류를 확인해줘");
        var review = reviewService.review(100L, "request-review", ClaimCopilotReviewStatus.CONFIRMED, 99L);

        assertThat(review.status()).isEqualTo(ClaimCopilotReviewStatus.CONFIRMED);
        assertThat(review.reviewerUserId()).isEqualTo(99L);
        assertThat(result.draft().canFinalizeClaimDecision()).isFalse();
        assertThat(reviewService.history(100L, "request-review"))
                .extracting(ClaimCopilotReviewEvent::eventType)
                .containsExactly(
                        ClaimCopilotReviewEventType.DRAFT_CREATED,
                        ClaimCopilotReviewEventType.REVIEW_CONFIRMED
                );
    }

    private ClaimAssessmentContext context(String termsHash) {
        Instant now = Instant.parse("2026-09-09T00:00:00Z");
        InsuranceClaim claim = new InsuranceClaim(100L, "C-100", 10L, 20L, 7L, now,
                "DEMO_GENERAL_CANCER", "fingerprint", "DOCUMENTS_PENDING", null, null, now, now);
        return new ClaimAssessmentContext(claim, 55L, "ACTIVE", 66L, "CANCER_DIAGNOSIS",
                BigDecimal.valueOf(10_000_000L), "KRW", now.minusSeconds(86_400), null,
                0, 0, BigDecimal.ONE, termsHash, "rule-v1", List.of("DEMO_GENERAL_CANCER"),
                List.of("DIAGNOSIS_CERTIFICATE"), true, 11L, 12L, 13L);
    }

    private ClaimEvidence verified(String type) {
        return new ClaimEvidence(1L, 100L, type, "synthetic://evidence", "a".repeat(64),
                java.util.Map.of(), true, Instant.parse("2026-09-09T00:00:00Z"));
    }

    private TermsClause clause(Long id, String code, String title, String body, String hash) {
        return new TermsClause(id, 1L, "CAPSURE-DEMO-CANCER-TERMS", "1.0.0", hash,
                true, code, title, body, 1, 1);
    }

    private static class RecordingGateway implements ClaimAssessmentAssistantGateway {
        private final List<ClaimAssessmentAssistantModelRequest> requests = new ArrayList<>();
        private ClaimAssessmentAssistantModelDraft reply = new ClaimAssessmentAssistantModelDraft(List.of(), List.of(), List.of(), false);

        @Override
        public ClaimAssessmentAssistantModelDraft generate(ClaimAssessmentAssistantModelRequest request) {
            requests.add(request);
            return reply;
        }
    }

    private static class RecordingAudit implements InsuranceAssistantAuditRecorder {
        private final List<InsuranceAssistantAuditRecord> records = new ArrayList<>();

        @Override
        public void record(InsuranceAssistantAuditRecord record) {
            records.add(record);
        }
    }

    private static class RecordingReviewRepository implements ClaimCopilotReviewRepository {
        private final Map<String, ClaimCopilotReview> reviews = new HashMap<>();
        private final Map<String, List<ClaimCopilotReviewEvent>> events = new HashMap<>();

        @Override
        public ClaimCopilotReview registerDraft(Long claimId, String requestId) {
            String key = key(claimId, requestId);
            if (reviews.containsKey(key)) {
                return reviews.get(key);
            }
            ClaimCopilotReview review = new ClaimCopilotReview(
                    claimId, requestId, ClaimCopilotReviewStatus.DRAFT, null, Instant.now());
            reviews.put(key, review);
            events.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new ClaimCopilotReviewEvent(
                    claimId, requestId, ClaimCopilotReviewEventType.DRAFT_CREATED,
                    ClaimCopilotReviewStatus.DRAFT, null, Instant.now()));
            return review;
        }

        @Override
        public Optional<ClaimCopilotReview> find(Long claimId, String requestId) {
            return Optional.ofNullable(reviews.get(key(claimId, requestId)));
        }

        @Override
        public List<ClaimCopilotReviewEvent> findHistory(Long claimId, String requestId) {
            return List.copyOf(events.getOrDefault(key(claimId, requestId), List.of()));
        }

        @Override
        public Optional<ClaimCopilotReview> updateReview(
                Long claimId,
                String requestId,
                ClaimCopilotReviewStatus status,
                Long reviewerUserId
        ) {
            String key = key(claimId, requestId);
            ClaimCopilotReview current = reviews.get(key);
            if (current == null) {
                return Optional.empty();
            }
            ClaimCopilotReview updated = new ClaimCopilotReview(
                    claimId, requestId, status, reviewerUserId, Instant.now());
            reviews.put(key, updated);
            events.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new ClaimCopilotReviewEvent(
                    claimId,
                    requestId,
                    status == ClaimCopilotReviewStatus.CONFIRMED
                            ? ClaimCopilotReviewEventType.REVIEW_CONFIRMED
                            : ClaimCopilotReviewEventType.REVIEW_REJECTED,
                    status,
                    reviewerUserId,
                    Instant.now()));
            return Optional.of(updated);
        }

        private String key(Long claimId, String requestId) {
            return claimId + ":" + requestId;
        }
    }
}
