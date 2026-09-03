package com.capsule.insurance.operations.outbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplayOutboxRequest(
        @NotBlank
        @Size(max = 500)
        String reason
) {
}
