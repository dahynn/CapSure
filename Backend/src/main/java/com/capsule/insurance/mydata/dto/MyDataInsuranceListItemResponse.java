package com.capsule.insurance.mydata.dto;

public record MyDataInsuranceListItemResponse(
        String insuNum,
        String providerCode,
        String productName,
        String contractStatusCode
) {
}
