package com.capsule.insurance.quote.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateQuoteRequest(
        @NotNull Long productVersionId,
        @NotEmpty List<@NotNull Long> selectedProductCoverageIds
) {
}
