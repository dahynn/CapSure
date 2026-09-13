package com.capsule.insurance.operations.catalog.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.operations.catalog.application.port.ProductVersionApprovalRepository;
import com.capsule.insurance.operations.catalog.domain.ProductVersionApprovalEvent;
import com.capsule.insurance.operations.catalog.domain.ProductVersionRelease;
import com.capsule.insurance.operations.catalog.dto.ProductVersionApprovalDecision;
import com.capsule.insurance.operations.catalog.dto.ProductVersionApprovalEventResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductVersionApprovalService {

    private final ProductVersionApprovalRepository repository;
    private final Clock clock;

    @Autowired
    public ProductVersionApprovalService(ProductVersionApprovalRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ProductVersionApprovalService(ProductVersionApprovalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ProductVersionApprovalEventResponse decide(
            Long productVersionId,
            ProductVersionApprovalDecision decision,
            Long actorUserId,
            String reason
    ) {
        if (!StringUtils.hasText(reason) || reason.trim().length() > 500) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "승인 판단 사유는 1자 이상 500자 이하여야 합니다."
            );
        }
        ProductVersionRelease release = repository.findForUpdate(productVersionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "상품 버전을 찾을 수 없습니다."
                ));
        if (!"PENDING_APPROVAL".equals(release.releaseStatus())) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "승인 대기 중인 상품 버전만 승인하거나 반려할 수 있습니다."
            );
        }

        String resultingStatus = decision == ProductVersionApprovalDecision.APPROVE
                ? "APPROVED"
                : "REJECTED";
        ProductVersionApprovalEvent event = repository.saveDecision(
                release,
                decision.name(),
                resultingStatus,
                actorUserId,
                reason.trim(),
                Instant.now(clock)
        );
        return ProductVersionApprovalEventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<ProductVersionApprovalEventResponse> getHistory(Long productVersionId) {
        if (!repository.exists(productVersionId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "상품 버전을 찾을 수 없습니다.");
        }
        return repository.findHistory(productVersionId).stream()
                .map(ProductVersionApprovalEventResponse::from)
                .toList();
    }
}
