package com.capsule.insurance.insurer.dto;

public record FixedTermsPdfSummaryResponse(
        String sourcePath,
        int pageCount,
        String headline,
        String coverageScope,
        String coverageAmount,
        String exclusions,
        String keyLimitations,
        String specialNotes,
        String extractedPreview,
        String disclaimer
) {
}
