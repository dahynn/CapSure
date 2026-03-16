// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CapsuleProduct {

    private final Long capsuleProductId;
    private final String capsuleCode;
    private final String capsuleName;
    private final CoverageCategory coverageCategory;
    private final String coverageCode;
    private final BigDecimal coverageAmount;
    private final String coverageUnit;
    private final BigDecimal monthlyPrice;
    private final Integer minRetentionDays;
    private final ProductSaleStatus saleStatus;
    private final boolean duplicateCheckTarget;
    private final String termsUri;
    private final String termsVersion;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
