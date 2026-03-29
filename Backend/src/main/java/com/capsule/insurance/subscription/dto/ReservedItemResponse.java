package com.capsule.insurance.subscription.dto;

public record ReservedItemResponse(
        Long subscriptionItemId,
        Long productSourceId,
        String productName,
        String companyName,
        Integer monthlyPrice,
        String itemStatus
) {}
