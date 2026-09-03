package com.capsule.insurance.information.application.port;

import com.capsule.insurance.information.domain.FinancialEventAudit;
import com.capsule.insurance.operations.outbox.domain.OutboxEvent;
import java.util.List;

public interface FinancialEventAuditRepository {

    void save(OutboxEvent event, Long policyId, String payloadHash);

    List<FinancialEventAudit> findByPolicyId(Long policyId);
}
