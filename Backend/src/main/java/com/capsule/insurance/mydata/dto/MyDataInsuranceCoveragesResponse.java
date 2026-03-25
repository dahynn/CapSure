package com.capsule.insurance.mydata.dto;

import java.util.List;

public record MyDataInsuranceCoveragesResponse(
        String insuNum,
        List<MyDataCoverageResponse> coverages
) {
}
