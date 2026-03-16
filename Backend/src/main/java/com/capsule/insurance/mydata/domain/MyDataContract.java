// #Demo Setting
package com.capsule.insurance.mydata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyDataContract {

    private final Long myDataContractId;
    private final Long userId;
    private final String insuNum;
    private final boolean consent;
    private final BusinessType businessType;
    private final String productName;
    private final String insuTypeCode;
    private final String contractStatusCode;
    private final LocalDate contractDate;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal premiumAmount;
    private final String currencyCode;
    private final String insuredListJson;
    private final String prizeListJson;
    private final String riderListJson;
    private final String policyUri;
    private final String extraPayloadJson;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
