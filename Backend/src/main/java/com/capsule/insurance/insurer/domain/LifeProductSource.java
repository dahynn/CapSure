// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LifeProductSource {

    private final Long stagedLifeId;
    private final String companyName;
    private final String productName;
    private final String contractType;
    private final String benefitName;
    private final String claimReasonText;
    private final String joinAmountText;
    private final String premiumMaleText;
    private final String premiumFemaleText;
    private final String saleChannel;
    private final LocalDate saleDate;
    private final String specialNote;
    private final String contactPhone;
    private final String productFeature;
    private final String productSubtype;
    private final CoverageCategory coverageCategory;
    private final String coverageCode;
    private final ProductMappingStatus mappingStatus;
    private final String manualNote;
    private final LocalDateTime loadedAt;
    private final LocalDateTime updatedAt;
}
