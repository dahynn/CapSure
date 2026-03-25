package com.capsule.insurance.mydata.dto;

import java.util.List;

public record MyDataInsuranceListResponse(
        List<MyDataInsuranceListItemResponse> contracts
) {
}
