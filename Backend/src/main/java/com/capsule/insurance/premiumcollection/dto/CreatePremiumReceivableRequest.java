package com.capsule.insurance.premiumcollection.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePremiumReceivableRequest(
        @NotNull Long policyId,
        @NotNull LocalDate billingCycle,
        @NotNull LocalDate dueDate,
        @NotNull LocalDate graceEndsOn,
        @NotNull @DecimalMin("1.00") BigDecimal amount
) {
}
