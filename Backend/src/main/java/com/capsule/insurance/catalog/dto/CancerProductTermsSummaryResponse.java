package com.capsule.insurance.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CancerProductTermsSummaryResponse(
        Long productVersionId,
        String productCode,
        String productVersion,
        String productName,
        boolean simulation,
        TermsDocumentResponse termsDocument,
        List<CoverageConditionResponse> coverageConditions,
        List<TermsHighlightResponse> highlights,
        String disclaimer
) {

    public record TermsDocumentResponse(
            Long termsDocumentId,
            String documentCode,
            String documentVersion,
            String title,
            String sourceType,
            String sourceUri,
            String sourceHash,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String status,
            boolean simulation
    ) {
    }

    public record CoverageConditionResponse(
            String coverageCode,
            String coverageName,
            BigDecimal insuredAmount,
            String currencyCode,
            int waitingPeriodDays,
            int reductionPeriodDays,
            BigDecimal reductionRate,
            String coverageStartRule,
            Long termsClauseId,
            String termsClauseCode
    ) {
    }

    public record TermsHighlightResponse(
            String category,
            Long termsClauseId,
            String clauseCode,
            String title,
            String content
    ) {
    }
}
