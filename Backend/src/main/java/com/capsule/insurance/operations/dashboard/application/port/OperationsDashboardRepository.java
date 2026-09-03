package com.capsule.insurance.operations.dashboard.application.port;

import com.capsule.insurance.operations.dashboard.domain.OperationsDashboardSnapshot;

public interface OperationsDashboardRepository {

    OperationsDashboardSnapshot load(int recentLimit);
}
