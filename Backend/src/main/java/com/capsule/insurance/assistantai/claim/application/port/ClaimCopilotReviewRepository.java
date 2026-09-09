package com.capsule.insurance.assistantai.claim.application.port;

import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEvent;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewStatus;
import java.util.List;
import java.util.Optional;

/** 원문 없이 보관하는 청구 심사 보조 초안의 최소 검토·감사 상태 저장소입니다. */
public interface ClaimCopilotReviewRepository {

    ClaimCopilotReview registerDraft(Long claimId, String requestId);

    Optional<ClaimCopilotReview> find(Long claimId, String requestId);

    List<ClaimCopilotReview> findRecent(ClaimCopilotReviewStatus status, int limit);

    List<ClaimCopilotReviewEvent> findHistory(Long claimId, String requestId);

    Optional<ClaimCopilotReview> updateReview(
            Long claimId,
            String requestId,
            ClaimCopilotReviewStatus status,
            Long reviewerUserId
    );
}
