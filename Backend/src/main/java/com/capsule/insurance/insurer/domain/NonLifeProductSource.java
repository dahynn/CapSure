// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NonLifeProductSource {

    private final Long stagedNonLifeId;
    private final String companyName;
    private final String productName;
    private final String saleChannel;
    private final String coverageName;
    private final String claimReasonText;
    private final String claimAmountText;
    private final String premiumMaleText;
    private final String premiumFemaleText;
    private final String minimumJoinPremiumText;
    private final String productSummaryText;
    private final String renewableText;
    private final String specialNote;
    private final String contactPhone;
    private final CoverageCategory coverageCategory;
    private final String coverageCode;
    private final ProductMappingStatus mappingStatus;
    private final String manualNote;
    private final LocalDateTime loadedAt;
    private final LocalDateTime updatedAt;
}
