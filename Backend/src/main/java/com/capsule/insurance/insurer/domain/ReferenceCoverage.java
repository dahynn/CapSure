// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReferenceCoverage {

    private final String coverageCode;
    private final CoverageDomain coverageDomain;
    private final CoverageCategory coverageCategory;
    private final String coverageName;
    private final String duplicateGroupCode;
    private final String description;
    private final String searchKeywordsJson;
    private final boolean active;
    private final CompensationType compensationType;
    private final DuplicateRuleCode duplicateRuleCode;
    private final RecommendationRuleCode recommendationRuleCode;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
