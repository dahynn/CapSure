
package com.capsule.insurance.mydata.application;

import com.capsule.insurance.mydata.domain.MyDataContract;
import com.capsule.insurance.mydata.domain.MyDataContractCoverage;
import com.capsule.insurance.mydata.dto.InsuranceSummary;
import com.capsule.insurance.mydata.dto.MyDataContractResponse;
import com.capsule.insurance.mydata.dto.MyDataCoverageResponse;
import com.capsule.insurance.mydata.dto.MyDataUserInsurancesResponse;
import com.capsule.insurance.mydata.infra.MyDataContractMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MyDataService {

    private final MyDataContractMapper myDataContractMapper;

    public MyDataService(MyDataContractMapper myDataContractMapper) {
        this.myDataContractMapper = myDataContractMapper;
    }

    public List<InsuranceSummary> getInsuranceSummaries() {
        return getUserInsurances(1L).contracts().stream()
                .map(contract -> new InsuranceSummary(
                        contract.insuNum(),
                        contract.productName(),
                        contract.contractStatusCode()
                ))
                .toList();
    }

    public MyDataUserInsurancesResponse getUserInsurances(Long userId) {
        List<MyDataContractResponse> contracts = myDataContractMapper.findContractsByUserId(userId).stream()
                .map(this::toContractResponse)
                .toList();
        return new MyDataUserInsurancesResponse(userId, contracts);
    }

    private MyDataContractResponse toContractResponse(MyDataContract contract) {
        List<MyDataCoverageResponse> coverages = contract.getCoverages().stream()
                .map(this::toCoverageResponse)
                .toList();

        return new MyDataContractResponse(
                contract.getMyDataContractId(),
                contract.getInsuNum(),
                contract.getProviderCode(),
                contract.getProductName(),
                contract.getBusinessType() != null ? contract.getBusinessType().name() : null,
                contract.getInsuTypeCode(),
                contract.getContractStatusCode(),
                contract.getContractDate(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getPremiumAmount(),
                contract.getContractListJson(),
                coverages
        );
    }

    private MyDataCoverageResponse toCoverageResponse(MyDataContractCoverage coverage) {
        return new MyDataCoverageResponse(
                coverage.getMyDataContractCoverageId(),
                coverage.getCoverageNum(),
                coverage.getCoverageName(),
                coverage.getCoverageAmount(),
                coverage.getCoverageStatus() != null ? coverage.getCoverageStatus().name() : null,
                coverage.getStartDate(),
                coverage.getEndDate(),
                coverage.getCoverageCode()
        );
    }
}
