// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSource {

    private final Long productSourceId;
    private final String sourceFileName;
    private final Integer sourceRowNo;
    private final InsurerSector insurerSector;

    // 공통 속성
    private final String companyName;
    private final String productName;
    private final String saleChannel;
    private final String contractTypeText;
    private final String coverageName;
    private final String claimReasonText;
    private final String payoutAmountText;
    private final String joinAmountText;
    private final String minimumJoinPremiumText;

    // 보험료 및 이율
    private final String premiumMaleText;
    private final String premiumFemaleText;
    private final String fixedRateText;
    private final String currentAnnouncedRateText;
    private final String minimumGuaranteedRateText;
    private final String coveragePartInterestRateText;
    private final String reservePartInterestRateText;

    // 지수 관련
    private final String priceIndexMaleText;
    private final String priceIndexFemaleText;
    private final String extraPremiumIndexMaleText;
    private final String extraPremiumIndexFemaleText;
    private final String extraPremiumIndexText;
    private final String contractCostIndexMaleText;
    private final String contractCostIndexFemaleText;
    private final String contractCostIndexText;
    private final String coverageScopeIndexNameText;
    private final String coverageScopeIndexValueText;
    private final String coverageScopeIndexCancerDiagnosisText;
    private final String coverageScopeIndexCancerHospitalizationText;

    // 특징 및 텍스트 데이터
    private final String expectedRenewalPremiumText;
    private final String productSummaryText;
    private final String productFeatureText;
    private final String surrenderValueText;
    private final String minimumDeathBenefitText;
    private final String minimumDeathBenefitMethodText;
    private final String minimumSurrenderValueText;
    private final String minimumSurrenderValueMethodText;
    private final String mildDementiaCoveredText;
    private final String mildDementiaBenefitAmountText;

    // 분류 및 기타 메타데이터
    private final String productSubtypeText;
    private final String renewalText;
    private final String universalText;
    private final String specialNote;
    private final String contactPhone;
    private final LocalDate saleDate;

    // 매핑 상태 관리
    private final CoverageCategory coverageCategory;
    private final String coverageCode;
    private final ProductMappingStatus mappingStatus;
    private final String manualNote;

    private final LocalDateTime loadedAt;
    private final LocalDateTime updatedAt;
}
