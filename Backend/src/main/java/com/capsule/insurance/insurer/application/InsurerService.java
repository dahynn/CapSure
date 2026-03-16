// #Demo Setting
package com.capsule.insurance.insurer.application;

import com.capsule.insurance.insurer.dto.InsurerProductSummary;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InsurerService {

    public List<InsurerProductSummary> getProducts() {
        // TODO: 실제 보험사 상품 조회용 mapper 연동을 구현해야 합니다.
        return List.of(new InsurerProductSummary("INSURER-A", "PROD-001", "Starter Plan"));
    }
}
