package com.capsule.insurance.subscription.infra.projection;

public record SnapshotCategoryCodeProjection(
        Long capsuleSnapshotId,
        String coverageCategoryCode
) {
}
