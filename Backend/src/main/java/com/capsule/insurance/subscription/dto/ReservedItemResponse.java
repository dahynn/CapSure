package com.capsule.insurance.subscription.dto;

public record ReservedItemResponse(
        Long subscriptionItemId,
        Long capsuleProductId,
        String productName,
        String companyName,
        Integer monthlyPrice,
        String itemStatus
) {}
