package com.capsule.insurance.assistantai.claim.application;

import com.capsule.insurance.assistantai.claim.application.port.ClaimCopilotReviewRepository;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEvent;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewStatus;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Claim Review Copilot의 담당자 검토 상태입니다.
 * 고객 정보·프롬프트·모델 원문은 저장하지 않고 검토 상태와 담당자 식별자만 남깁니다.
 */
@Service
public class ClaimCopilotReviewService {

    private final ClaimCopilotReviewRepository repository;

    public ClaimCopilotReviewService(ClaimCopilotReviewRepository repository) {
        this.repository = repository;
    }

    public ClaimCopilotReview registerDraft(Long claimId, String requestId) {
        return repository.registerDraft(claimId, requestId);
    }

    public ClaimCopilotReview get(Long claimId, String requestId) {
        return repository.find(claimId, requestId).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "심사 보조 초안 검토 상태를 찾을 수 없습니다."));
    }

    public List<ClaimCopilotReviewEvent> history(Long claimId, String requestId) {
        get(claimId, requestId);
        return repository.findHistory(claimId, requestId);
    }

    public ClaimCopilotReview review(
            Long claimId,
            String requestId,
            ClaimCopilotReviewStatus status,
            Long reviewerUserId
    ) {
        if (status == ClaimCopilotReviewStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "담당자 검토 상태는 CONFIRMED 또는 REJECTED여야 합니다.");
        }
        return repository.updateReview(claimId, requestId, status, reviewerUserId).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "심사 보조 초안 검토 상태를 찾을 수 없습니다."));
    }
}
