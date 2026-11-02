package com.capsule.insurance.operations.catalog.application.port;

import com.capsule.insurance.operations.catalog.domain.ProductVersionApprovalEvent;
import com.capsule.insurance.operations.catalog.domain.ProductVersionRelease;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProductVersionApprovalRepository {

    Optional<ProductVersionRelease> findForUpdate(Long productVersionId);

    boolean exists(Long productVersionId);

    ProductVersionApprovalEvent saveDecision(
            ProductVersionRelease release,
            String decision,
            String resultingStatus,
            Long actorUserId,
            String reason,
            Instant decidedAt
    );

    List<ProductVersionApprovalEvent> findHistory(Long productVersionId);
}

