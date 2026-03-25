package com.capsule.insurance.mydata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MyDataContractResponse(
        Long mydContractId,
        String insuNum,
        String providerCode,
        String productName,
        String businessType,
        String insuTypeCode,
        String contractStatusCode,
        LocalDate contractDate,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal premiumAmount,
        String contractListJson,
        List<MyDataCoverageResponse> coverages
) {
}
