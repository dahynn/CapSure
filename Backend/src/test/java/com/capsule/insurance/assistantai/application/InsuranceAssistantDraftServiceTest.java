package com.capsule.insurance.assistantai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.capsule.insurance.assistantai.application.port.InsuranceAssistantAuditRecorder;
import com.capsule.insurance.assistantai.application.port.InsuranceAssistantGateway;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantDecision;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantEvidence;
import com.capsule.insurance.assistantai.domain.InsuranceAssistantRequest;
import com.capsule.insurance.assistantai.domain.InsuranceEvidenceType;
import com.capsule.insurance.assistantai.infra.DefaultBlockedInsuranceAssistantGateway;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsuranceAssistantDraftServiceTest {

    @Test
    void sensitiveInputIsRejectedBeforeItReachesGatewayOrAuditPayload() {
        RecordingGateway gateway = new RecordingGateway();
        RecordingAudit audit = new RecordingAudit();
        InsuranceAssistantDraftService service = service(gateway, audit);
        List<String> sensitiveValues = List.of("900101-1234567", "110-123-456789", "010-1234-5678", "customer@example.com");

        for (String sensitiveValue : sensitiveValues) {
            var result = service.createDraft(request("req-1", "고객 정보: " + sensitiveValue,
                    InsuranceEvidenceType.TERMS_DOCUMENT, "암 진단 시 약정 금액을 지급합니다."));
            assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.REJECTED_SENSITIVE_INPUT);
        }

        assertThat(gateway.requests).isEmpty();
        assertThat(audit.records).hasSize(4).allSatisfy(record -> {
            assertThat(record.requestId()).isEqualTo("req-1");
            assertThat(record.evidenceIds()).containsExactly("terms-2026-v1");
            sensitiveValues.forEach(sensitiveValue -> assertThat(record.toString()).doesNotContain(sensitiveValue));
        });
    }

    @Test
    void unapprovedEvidenceIsRejected() {
        RecordingGateway gateway = new RecordingGateway();
        RecordingAudit audit = new RecordingAudit();
        InsuranceAssistantDraftService service = service(gateway, audit);

        var result = service.createDraft(request("req-2", "요약해줘",
                InsuranceEvidenceType.CUSTOMER_PROFILE, "고객의 계약 정보"));

        assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.REJECTED_UNAPPROVED_EVIDENCE);
        assertThat(gateway.requests).isEmpty();
        assertThat(audit.records).singleElement().extracting(InsuranceAssistantAuditRecord::decision)
                .isEqualTo(InsuranceAssistantDecision.REJECTED_UNAPPROVED_EVIDENCE);
    }

    @Test
    void defaultGatewayBlocksExternalModelCall() {
        RecordingAudit audit = new RecordingAudit();
        InsuranceAssistantDraftService service = service(new DefaultBlockedInsuranceAssistantGateway(), audit);

        var result = service.createDraft(request("req-3", "약관 핵심을 요약해줘",
                InsuranceEvidenceType.TERMS_DOCUMENT, "암 진단 시 약정 금액을 지급합니다."));

        assertThat(result.decision()).isEqualTo(InsuranceAssistantDecision.GATEWAY_BLOCKED);
        assertThat(result.draft()).isNull();
        assertThat(audit.records).singleElement().extracting(InsuranceAssistantAuditRecord::decision)
                .isEqualTo(InsuranceAssistantDecision.GATEWAY_BLOCKED);
    }

    @Test
    void generatedDraftAlwaysRequiresHumanReviewAndCannotFinalizeInsuranceDecision() {
        RecordingGateway gateway = new RecordingGateway();
        RecordingAudit audit = new RecordingAudit();
        InsuranceAssistantDraftService service = service(gateway, audit);

        var result = service.createDraft(request("req-4", "약관 핵심을 요약해줘",
                InsuranceEvidenceType.PRODUCT_SOURCE, "보장 개시일과 면책 기간은 약관 원문을 확인합니다."));

        assertThat(result.requiresHumanReview()).isTrue();
        assertThat(result.draft().canFinalizeInsuranceDecision()).isFalse();
        assertThat(result.draft().decision()).isEqualTo(InsuranceAssistantDecision.REVIEW_REQUIRED);
        assertThat(gateway.requests).singleElement().satisfies(forwarded ->
                assertThat(forwarded.evidence().get(0).evidenceId()).isEqualTo("terms-2026-v1"));
    }

    private InsuranceAssistantDraftService service(InsuranceAssistantGateway gateway, InsuranceAssistantAuditRecorder audit) {
        return new InsuranceAssistantDraftService(new InsuranceAssistantInputPolicy(), gateway, audit);
    }

    private InsuranceAssistantRequest request(String requestId, String instruction, InsuranceEvidenceType type, String content) {
        return new InsuranceAssistantRequest(requestId, instruction,
                List.of(new InsuranceAssistantEvidence("terms-2026-v1", type, content)));
    }

    private static class RecordingGateway implements InsuranceAssistantGateway {
        private final List<InsuranceAssistantRequest> requests = new ArrayList<>();

        @Override
        public String generateDraft(InsuranceAssistantRequest request) {
            requests.add(request);
            return "보험 업무 보조 초안";
        }
    }

    private static class RecordingAudit implements InsuranceAssistantAuditRecorder {
        private final List<InsuranceAssistantAuditRecord> records = new ArrayList<>();

        @Override
        public void record(InsuranceAssistantAuditRecord record) {
            records.add(record);
        }
    }
}
