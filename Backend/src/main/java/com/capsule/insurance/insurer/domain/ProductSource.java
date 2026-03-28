package com.capsule.insurance.insurer.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductSource {

    private Long productSourceId;
    private String sourceFileName;
    private Integer sourceRowNo;
    private InsurerSector insurerSector;

    private String companyName;
    private String productName;
    private String saleChannel;
    private String contractTypeText;
    private String coverageName;
    private String claimReasonText;
    private String payoutAmountText;
    private String joinAmountText;
    private String minimumJoinPremiumText;

    private String premiumMaleText;
    private String premiumFemaleText;
    private String paymentCycle;
    private String paymentTerm;
    private String coverageTerm;
    private BigDecimal monthlyPremiumMale;
    private BigDecimal monthlyPremiumFemale;
    private String fixedRateText;
    private String currentAnnouncedRateText;
    private String minimumGuaranteedRateText;
    private String coveragePartInterestRateText;
    private String reservePartInterestRateText;

    private String priceIndexMaleText;
    private String priceIndexFemaleText;
    private String extraPremiumIndexMaleText;
    private String extraPremiumIndexFemaleText;
    private String extraPremiumIndexText;
    private String contractCostIndexMaleText;
    private String contractCostIndexFemaleText;
    private String contractCostIndexText;
    private String coverageScopeIndexNameText;
    private String coverageScopeIndexValueText;
    private String coverageScopeIndexCancerDiagnosisText;
    private String coverageScopeIndexCancerHospitalizationText;

    private String expectedRenewalPremiumText;
    private String productSummaryText;
    private String productFeatureText;
    private String aiSummaryJson;
    private String surrenderValueText;
    private String minimumDeathBenefitText;
    private String minimumDeathBenefitMethodText;
    private String minimumSurrenderValueText;
    private String minimumSurrenderValueMethodText;
    private String mildDementiaCoveredText;
    private String mildDementiaBenefitAmountText;

    private String productSubtypeText;
    private String renewalText;
    private String universalText;
    private String specialNote;
    private String contactPhone;
    private LocalDate saleDate;

    private CoverageCategory coverageCategory;
    private String coverageCode;
    private ProductMappingStatus mappingStatus;
    private String manualNote;

    private Instant loadedAt;
    private Instant updatedAt;
}
