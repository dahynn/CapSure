// #Demo Setting
package com.capsule.insurance.mydata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyDataContractCoverage {

    private final Long myDataContractCoverageId;
    private final Long myDataContractId;
    private final String coverageNum;
    private final String coverageName;
    private final BigDecimal coverageAmount;
    private final String currencyCode;
    private final ContractCoverageStatus coverageStatus;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String coverageCode;
    private final String extraPayloadJson;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
