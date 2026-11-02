package com.capsule.insurance.assistantai.infra;

import com.capsule.insurance.assistantai.application.InsuranceAssistantAuditRecord;
import com.capsule.insurance.assistantai.application.port.InsuranceAssistantAuditRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SafeInsuranceAssistantAuditLogger implements InsuranceAssistantAuditRecorder {
    @Override
    public void record(InsuranceAssistantAuditRecord record) {
        log.info("insurance_ai_audit requestId={} decision={} evidenceIds={}",
                record.requestId(), record.decision(), record.evidenceIds());
    }
}
