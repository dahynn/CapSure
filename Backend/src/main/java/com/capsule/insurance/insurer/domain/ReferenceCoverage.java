// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReferenceCoverage {

    private String coverageCode;
    private CoverageCategory coverageCategory;
    private String coverageName;
    private String duplicateGroupCode;
    private String description;
    private String searchKeywordsJson;
    private boolean active;
    private CompensationType compensationType;
    private DuplicateRuleCode duplicateRuleCode;
    private RecommendationRuleCode recommendationRuleCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
