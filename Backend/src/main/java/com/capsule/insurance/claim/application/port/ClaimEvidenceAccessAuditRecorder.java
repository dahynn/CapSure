package com.capsule.insurance.claim.application.port;

import com.capsule.insurance.claim.domain.ClaimEvidenceAccessAttempt;

public interface ClaimEvidenceAccessAuditRecorder {

    void record(ClaimEvidenceAccessAttempt attempt);
}
