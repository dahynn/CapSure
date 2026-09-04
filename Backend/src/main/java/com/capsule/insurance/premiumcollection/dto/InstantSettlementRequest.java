package com.capsule.insurance.premiumcollection.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InstantSettlementRequest(
        @NotNull Long premiumReceivableId,
        @NotNull @DecimalMin("1.00") BigDecimal amount,
        @NotBlank String providerTransactionKey,
        @NotBlank String idempotencyKey
) {
}
