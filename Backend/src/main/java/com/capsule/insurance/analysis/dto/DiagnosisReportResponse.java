package com.capsule.insurance.analysis.dto;

import java.math.BigDecimal;
import java.util.List;

public record DiagnosisReportResponse(
        String description,
        List<CategoryDiagnosis> diagnoses
) {
    public record CategoryDiagnosis(
            String categoryCode,
            String categoryName,
            boolean insured,
            String status,
            List<String> coverageNames,
            RecommendedProduct recommendedProduct
    ) {
    }

    public record RecommendedProduct(
            Long productSourceId,
            String companyName,
            String productName,
            BigDecimal monthlyPrice
    ) {
    }
}
