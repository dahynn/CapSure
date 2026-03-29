package com.capsule.insurance.analysis.dto;

public record CoveragePercentileResponse(
        int coveragePercentile,
        int coveredCategoryCount,
        int totalCategoryCount,
        String message
) {
}
