
package com.capsule.insurance.mydata.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MyDataContract {

    private Long myDataContractId;
    private Long userId;
    private String providerCode;
    private String insuNum;
    private boolean consent;
    private BusinessType businessType;
    private String productName;
    private String insuTypeCode;
    private String contractStatusCode;
    private LocalDate contractDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal premiumAmount;
    private String currencyCode;
    private String insuredListJson;
    private String prizeListJson;
    private String contractListJson;
    private String policyUri;
    private String extraPayloadJson;
    private Instant createdAt;
    private Instant updatedAt;

    // TODO: 지금은 계약 조회 후 담보를 nested select로 조립한다. 업서트 키와 raw payload 보관 범위가 확정되면 적재 전략과 함께 다시 정리한다.
    @Builder.Default
    private List<MyDataContractCoverage> coverages = new ArrayList<>();
}
