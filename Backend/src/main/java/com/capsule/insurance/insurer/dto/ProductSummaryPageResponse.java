package com.capsule.insurance.insurer.dto;

import java.util.List;

public record ProductSummaryPageResponse(
        List<ProductSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
