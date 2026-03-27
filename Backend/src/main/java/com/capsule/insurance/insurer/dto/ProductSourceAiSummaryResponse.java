package com.capsule.insurance.insurer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductSourceAiSummaryResponse(
        Long productSourceId,
        String companyName,
        String productName,
        @JsonProperty("핵심보장")
        String coreCoverage,
        @JsonProperty("특징")
        String feature,
        @JsonProperty("보험료")
        String premium
) {
}
