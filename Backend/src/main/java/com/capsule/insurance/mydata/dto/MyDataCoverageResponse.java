package com.capsule.insurance.mydata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MyDataCoverageResponse(
        Long mydContractCoverageId,
        String coverageNum,
        String coverageName,
        BigDecimal coverageAmount,
        String coverageStatus,
        LocalDate startDate,
        LocalDate endDate,
        String coverageCode
) {
}
