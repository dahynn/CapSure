package com.capsule.insurance.assistantai.application;

import com.capsule.insurance.assistantai.application.port.InsuranceAssistantAuditRecorder;
import com.capsule.insurance.assistantai.application.port.InsuranceAssistantGateway;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantDecision;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantDraft;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InsuranceAssistantDraftService {

    private final InsuranceAssistantInputPolicy inputPolicy;
    private final InsuranceAssistantGateway gateway;
    private final InsuranceAssistantAuditRecorder auditRecorder;

    public InsuranceAssistantDraftService(
            InsuranceAssistantInputPolicy inputPolicy,
            InsuranceAssistantGateway gateway,
            InsuranceAssistantAuditRecorder auditRecorder
    ) {
        this.inputPolicy = inputPolicy;
        this.gateway = gateway;
        this.auditRecorder = auditRecorder;
    }

    public DraftResult createDraft(InsuranceAssistantRequest request) {
        List<String> evidenceIds = request.evidence().stream().map(evidence -> evidence.evidenceId()).toList();
        InsuranceAssistantInputPolicy.PolicyEvaluation evaluation = inputPolicy.evaluate(request);
        if (evaluation == InsuranceAssistantInputPolicy.PolicyEvaluation.UNAPPROVED_EVIDENCE) {
            return rejected(request.requestId(), evidenceIds, InsuranceAssistantDecision.REJECTED_UNAPPROVED_EVIDENCE);
        }
        if (evaluation == InsuranceAssistantInputPolicy.PolicyEvaluation.SENSITIVE_INPUT) {
            return rejected(request.requestId(), evidenceIds, InsuranceAssistantDecision.REJECTED_SENSITIVE_INPUT);
        }

        try {
            String generated = gateway.generateDraft(request);
            InsuranceAssistantDraft draft = new InsuranceAssistantDraft(
                    request.requestId(), generated, evidenceIds, InsuranceAssistantDecision.REVIEW_REQUIRED);
            audit(draft.decision(), request.requestId(), evidenceIds);
            return DraftResult.reviewRequired(draft);
        } catch (ExternalModelCallBlockedException exception) {
            return rejected(request.requestId(), evidenceIds, InsuranceAssistantDecision.GATEWAY_BLOCKED);
        }
    }

    private DraftResult rejected(String requestId, List<String> evidenceIds, InsuranceAssistantDecision decision) {
        audit(decision, requestId, evidenceIds);
        return DraftResult.rejected(decision);
    }

    private void audit(InsuranceAssistantDecision decision, String requestId, List<String> evidenceIds) {
        auditRecorder.record(new InsuranceAssistantAuditRecord(requestId, decision, evidenceIds, Instant.now()));
    }

    public record DraftResult(InsuranceAssistantDraft draft, InsuranceAssistantDecision decision) {
        static DraftResult reviewRequired(InsuranceAssistantDraft draft) {
            return new DraftResult(draft, InsuranceAssistantDecision.REVIEW_REQUIRED);
        }

        static DraftResult rejected(InsuranceAssistantDecision decision) {
            return new DraftResult(null, decision);
        }

        public boolean requiresHumanReview() {
            return decision == InsuranceAssistantDecision.REVIEW_REQUIRED;
        }
    }
}
