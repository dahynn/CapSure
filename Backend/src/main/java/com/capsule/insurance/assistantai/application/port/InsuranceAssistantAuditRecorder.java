package com.capsule.insurance.assistantai.application.port;

import com.capsule.insurance.assistantai.application.InsuranceAssistantAuditRecord;

public interface InsuranceAssistantAuditRecorder {
    void record(InsuranceAssistantAuditRecord record);
}
