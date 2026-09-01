package com.capsule.insurance.application.dto;

import jakarta.validation.constraints.NotNull;

public record CreateApplicationRequest(
        @NotNull Long quoteId
) {
}
