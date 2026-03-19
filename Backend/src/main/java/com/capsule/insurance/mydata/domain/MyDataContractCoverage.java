// #Demo Setting
package com.capsule.insurance.mydata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class MyDataContractCoverage {

    private Long myDataContractCoverageId;
    private Long myDataContractId;
    private String coverageNum;
    private String coverageName;
    private BigDecimal coverageAmount;
    private String currencyCode;
    private ContractCoverageStatus coverageStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverageCode;
    private String extraPayloadJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
