package com.capsule.insurance.operations.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideProductVersionApprovalRequest(
        @NotNull ProductVersionApprovalDecision decision,
        @NotBlank @Size(max = 500) String reason
) {
}

