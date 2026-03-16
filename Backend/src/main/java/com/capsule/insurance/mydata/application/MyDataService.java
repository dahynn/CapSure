// #Demo Setting
package com.capsule.insurance.mydata.application;

import com.capsule.insurance.mydata.dto.InsuranceSummary;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MyDataService {

    public List<InsuranceSummary> getInsuranceSummaries() {
        // TODO: 실제 마이데이터 수집 및 매퍼 연동 로직을 구현해야 합니다.
        return List.of(new InsuranceSummary("INS-001", "Capsule Basic Care", "ACTIVE"));
    }
}
