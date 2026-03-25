package com.capsule.insurance.mydata.dto;

import java.util.List;

public record MyDataUserInsurancesResponse(
        Long userId,
        List<MyDataContractResponse> contracts
) {
}
