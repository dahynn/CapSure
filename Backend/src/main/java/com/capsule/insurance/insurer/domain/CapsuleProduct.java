// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.math.BigDecimal;
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
public class CapsuleProduct {

    private Long capsuleProductId;
    private String capsuleCode;
    private String capsuleName;
    private CoverageCategory coverageCategory;
    private String coverageCode;
    private BigDecimal coverageAmount;
    private String coverageUnit;
    private BigDecimal monthlyPrice;
    private Integer minRetentionDays;
    private ProductSaleStatus saleStatus;
    private boolean duplicateCheckTarget;
    private String termsUri;
    private String termsVersion;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
