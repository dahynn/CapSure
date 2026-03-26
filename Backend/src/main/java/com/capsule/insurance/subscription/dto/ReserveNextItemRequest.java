package com.capsule.insurance.subscription.dto;

import jakarta.validation.constraints.NotNull;

public record ReserveNextItemRequest(
        @NotNull(message = "capsuleProductId는 필수입니다.")
        Long capsuleProductId
) {}
