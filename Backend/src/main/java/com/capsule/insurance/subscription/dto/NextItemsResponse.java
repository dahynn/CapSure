package com.capsule.insurance.subscription.dto;

import java.util.List;

public record NextItemsResponse(
        Long subscriptionId,
        Integer billingAnchorDay,
        String nextBillingAt,
        List<SubscriptionItemDto> currentItems,
        List<SubscriptionItemDto> nextItems
) {
    public record SubscriptionItemDto(
            Long subscriptionItemId,
            Long capsuleProductId,
            String productName,
            String companyName,
            Integer monthlyPrice,
            String itemStatus
    ) {}
}
