// #Demo Setting
package com.capsule.insurance.insurer.domain;

import java.math.BigDecimal;
import java.time.Instant;
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
    private String productName;
    private String companyName;
    private CoverageCategory coverageCategory;
    private String coverageName;
    private BigDecimal coverageAmount;
    private BigDecimal monthlyPriceMale;
    private BigDecimal monthlyPriceFemale;
    private ProductSaleStatus saleStatus;
    private boolean duplicateCheckTarget;
    private String termsUri;
    private String termsVersion;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
