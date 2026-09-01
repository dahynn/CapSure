package com.capsule.insurance.claim.application;

import com.capsule.insurance.claim.application.port.ClaimRepository;
import com.capsule.insurance.claim.domain.ClaimAssessmentContext;
import com.capsule.insurance.claim.domain.ClaimDecision;
import com.capsule.insurance.claim.domain.ClaimEvidence;
import com.capsule.insurance.claim.domain.ClaimPayment;
import com.capsule.insurance.claim.domain.InsuranceClaim;
import com.capsule.insurance.claim.dto.ClaimResponse;
import com.capsule.insurance.claim.dto.CreateClaimRequest;
import com.capsule.insurance.claim.dto.UpsertClaimEvidenceRequest;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Autowired
    public ClaimService(
            ClaimRepository claimRepository,
            PlatformTransactionManager transactionManager
    ) {
        this(claimRepository, transactionManager, Clock.systemUTC());
    }

    ClaimService(
            ClaimRepository claimRepository,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.claimRepository = claimRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public ClaimResponse create(
            Long userId,
            Long policyId,
            CreateClaimRequest request
    ) {
        if (request.incidentAt().isAfter(Instant.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사고일은 현재보다 미래일 수 없습니다.");
        }
        String fingerprint = sha256(
                request.policyCoverageId()
                        + "|" + request.incidentAt()
                        + "|" + request.diagnosisCategory()
        );
        InsuranceClaim claim = Objects.requireNonNull(transactionTemplate.execute(status -> {
            if (!claimRepository.ownsActivePolicyCoverage(
                    policyId,
                    request.policyCoverageId(),
                    userId
            )) {
                throw notFound("청구할 활성 계약 담보를 찾을 수 없습니다.");
            }
            InsuranceClaim existing = claimRepository
                    .findByCoverageAndFingerprint(request.policyCoverageId(), fingerprint)
                    .orElse(null);
            if (existing != null) {
                return existing;
            }
            return claimRepository.createDraft(
                    "C-" + UUID.randomUUID(),
                    policyId,
                    request.policyCoverageId(),
                    userId,
                    request.incidentAt(),
                    request.diagnosisCategory(),
                    fingerprint
            );
        }));
        return toResponse(claim);
    }

    public ClaimResponse recordEvidence(
            Long userId,
            Long claimId,
            UpsertClaimEvidenceRequest request
    ) {
        InsuranceClaim claim = Objects.requireNonNull(transactionTemplate.execute(status -> {
            InsuranceClaim locked = lockOwned(claimId, userId);
            if (!Set.of("DRAFT", "DOCUMENTS_PENDING").contains(locked.status())) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "지급심사 전 청구만 증빙을 변경할 수 있습니다."
                );
            }
            if (!request.syntheticReference().startsWith("synthetic://")) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "실제 의료파일 대신 synthetic:// 참조만 사용할 수 있습니다."
                );
            }
            claimRepository.saveEvidence(
                    claimId,
                    request.evidenceType(),
                    request.syntheticReference(),
                    request.checksum(),
                    request.metadata(),
                    request.verified()
            );
            return locked;
        }));
        return toResponse(claim);
    }

    public ClaimResponse submit(Long userId, Long claimId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        InsuranceClaim claim = Objects.requireNonNull(transactionTemplate.execute(status -> {
            InsuranceClaim locked = lockOwned(claimId, userId);
            if (locked.submissionIdempotencyKey() != null) {
                if (locked.submissionIdempotencyKey().equals(idempotencyKey)) {
                    return locked;
                }
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "다른 Idempotency-Key로 이미 지급심사된 청구입니다."
                );
            }
            if (!Set.of("DRAFT", "DOCUMENTS_PENDING").contains(locked.status())) {
                throw new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION,
                        "지급심사 전 청구만 제출할 수 있습니다."
                );
            }

            ClaimAssessmentContext context = claimRepository.findAssessmentContext(claimId);
            List<ClaimEvidence> evidence = claimRepository.findEvidence(claimId);
            AssessmentResult result = assess(context, evidence);
            return claimRepository.saveDecision(
                    claimId,
                    idempotencyKey,
                    result.result(),
                    result.benefitAmount(),
                    context.currencyCode(),
                    result.reasonCodes(),
                    result.termsClauseId(),
                    context.ruleVersion(),
                    inputHash(context, evidence)
            );
        }));
        return toResponse(claim);
    }

    public ClaimResponse pay(Long userId, Long claimId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        InsuranceClaim claim = Objects.requireNonNull(transactionTemplate.execute(status -> {
            InsuranceClaim locked = lockOwned(claimId, userId);
            ClaimDecision decision = claimRepository.findDecision(claimId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.BUSINESS_RULE_VIOLATION,
                            "지급심사 결정이 없습니다."
                    ));
            ClaimPayment sameRequest = claimRepository
                    .findPaymentByIdempotencyKey(idempotencyKey)
                    .orElse(null);
            if (sameRequest != null) {
                if (sameRequest.claimDecisionId().equals(decision.claimDecisionId())) {
                    return locked;
                }
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "다른 보험금 지급에 사용된 Idempotency-Key입니다."
                );
            }
            ClaimPayment existing = claimRepository.findPayment(decision.claimDecisionId()).orElse(null);
            if (existing != null) {
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "이미 다른 키로 실행된 보험금 지급이 있습니다."
                );
            }
            if (!"APPROVED".equals(decision.result()) || !"APPROVED".equals(locked.status())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "승인 상태의 청구만 가상 보험금을 지급할 수 있습니다."
                );
            }
            ClaimAssessmentContext context = claimRepository.findAssessmentContext(claimId);
            int paidBenefitCount = claimRepository.lockPaidBenefitCount(locked.policyCoverageId());
            if (context.firstDiagnosisOnly() && paidBenefitCount > 0) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "최초 1회 진단비가 이미 지급된 담보입니다."
                );
            }
            claimRepository.payApprovedDecision(
                    claimId,
                    locked.policyCoverageId(),
                    decision.claimDecisionId(),
                    "BENEFIT-" + UUID.randomUUID(),
                    decision.benefitAmount(),
                    decision.currencyCode(),
                    idempotencyKey
            );
            return claimRepository.findOwned(claimId, userId).orElseThrow();
        }));
        return toResponse(claim);
    }

    public ClaimResponse get(Long userId, Long claimId) {
        return claimRepository.findOwned(claimId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> notFound("보험금 청구를 찾을 수 없습니다."));
    }

    private AssessmentResult assess(
            ClaimAssessmentContext context,
            List<ClaimEvidence> evidence
    ) {
        Instant incidentAt = context.claim().incidentAt();
        if (incidentAt.isBefore(context.coverageStartAt())) {
            return new AssessmentResult(
                    "DENIED",
                    null,
                    List.of("COVERAGE_NOT_STARTED"),
                    context.denialClauseId()
            );
        }
        if (context.coverageEndAt() != null && !incidentAt.isBefore(context.coverageEndAt())) {
            return new AssessmentResult(
                    "DENIED",
                    null,
                    List.of("COVERAGE_ENDED"),
                    context.denialClauseId()
            );
        }
        if (!context.diagnosisCategories().contains(context.claim().diagnosisCategory())) {
            return new AssessmentResult(
                    "DENIED",
                    null,
                    List.of("DIAGNOSIS_CATEGORY_NOT_COVERED"),
                    context.denialClauseId()
            );
        }
        if (context.firstDiagnosisOnly() && context.paidBenefitCount() > 0) {
            return new AssessmentResult(
                    "DENIED",
                    null,
                    List.of("FIRST_DIAGNOSIS_BENEFIT_ALREADY_PAID"),
                    context.denialClauseId()
            );
        }

        boolean evidenceComplete = context.requiredEvidence().stream().allMatch(requiredType ->
                evidence.stream().anyMatch(item ->
                        item.evidenceType().equals(requiredType) && item.verified()
                )
        );
        if (!evidenceComplete) {
            return new AssessmentResult(
                    "MANUAL_REVIEW",
                    null,
                    List.of("REQUIRED_EVIDENCE_MISSING_OR_UNVERIFIED"),
                    context.missingEvidenceClauseId()
            );
        }

        Instant reductionEndAt = context.coverageStartAt()
                .plus(context.reductionPeriodDays(), ChronoUnit.DAYS);
        boolean inReductionPeriod = context.reductionPeriodDays() > 0
                && incidentAt.isBefore(reductionEndAt);
        BigDecimal benefitAmount = inReductionPeriod
                ? context.insuredAmount().multiply(context.reductionRate())
                : context.insuredAmount();
        benefitAmount = benefitAmount.setScale(2, RoundingMode.HALF_UP);
        return new AssessmentResult(
                "APPROVED",
                benefitAmount,
                List.of(inReductionPeriod ? "ELIGIBLE_REDUCED_BENEFIT" : "ELIGIBLE_FULL_BENEFIT"),
                context.eligibilityClauseId()
        );
    }

    private String inputHash(
            ClaimAssessmentContext context,
            List<ClaimEvidence> evidence
    ) {
        String evidenceInput = evidence.stream()
                .sorted(Comparator.comparing(ClaimEvidence::evidenceType)
                        .thenComparing(ClaimEvidence::checksum))
                .map(item -> item.evidenceType() + ":" + item.checksum() + ":" + item.verified())
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        return sha256(
                context.claim().claimId()
                        + "|" + context.policyVersionId()
                        + "|" + context.claim().incidentAt()
                        + "|" + context.claim().diagnosisCategory()
                        + "|" + context.coverageStartAt()
                        + "|" + context.insuredAmount()
                        + "|" + context.termsHash()
                        + "|" + context.ruleVersion()
                        + "|" + evidenceInput
        );
    }

    private ClaimResponse toResponse(InsuranceClaim claim) {
        ClaimDecision decision = claimRepository.findDecision(claim.claimId()).orElse(null);
        ClaimPayment payment = decision == null
                ? null
                : claimRepository.findPayment(decision.claimDecisionId()).orElse(null);
        return new ClaimResponse(
                claim.claimId(),
                claim.claimNo(),
                claim.policyId(),
                claim.policyCoverageId(),
                claim.incidentAt(),
                claim.diagnosisCategory(),
                claim.status(),
                claimRepository.findEvidence(claim.claimId()),
                decision,
                payment,
                claim.submittedAt(),
                claim.createdAt(),
                claim.updatedAt()
        );
    }

    private InsuranceClaim lockOwned(Long claimId, Long userId) {
        return claimRepository.lockOwned(claimId, userId)
                .orElseThrow(() -> notFound("보험금 청구를 찾을 수 없습니다."));
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Idempotency-Key가 필요합니다.");
        }
        if (idempotencyKey.length() > 150) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Idempotency-Key는 150자 이하여야 합니다.");
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

    private BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    private record AssessmentResult(
            String result,
            BigDecimal benefitAmount,
            List<String> reasonCodes,
            Long termsClauseId
    ) {
    }
}
