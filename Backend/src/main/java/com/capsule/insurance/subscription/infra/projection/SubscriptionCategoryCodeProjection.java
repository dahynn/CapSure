package com.capsule.insurance.subscription.infra.projection;

public record SubscriptionCategoryCodeProjection(
        Long subscriptionId,
        String coverageCategoryCode
) {
}
