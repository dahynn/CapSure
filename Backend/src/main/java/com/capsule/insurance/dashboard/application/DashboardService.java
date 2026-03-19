// #Demo Setting
package com.capsule.insurance.dashboard.application;

import com.capsule.insurance.dashboard.dto.DashboardSummary;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    public DashboardSummary getSummary() {
        // TODO: 실제 대시보드 집계용 조회 로직을 구현해야 합니다.
        return new DashboardSummary(2, 1, 0);
    }
}
