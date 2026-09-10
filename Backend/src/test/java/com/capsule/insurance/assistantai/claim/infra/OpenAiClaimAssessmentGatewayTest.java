package com.capsule.insurance.assistantai.claim.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capsule.insurance.assistantai.application.ExternalModelCallBlockedException;
import com.capsule.insurance.assistantai.claim.application.ClaimReviewCopilotProperties;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentSourceReference;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenAiClaimAssessmentGatewayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Responses API 원본 output.content의 output_text를 추출한다")
    void extractsTextFromRawResponsesApiPayload() throws Exception {
        String modelJson = "{\"termsSourceIds\":[],\"possibleMissingEvidence\":[],\"additionalQuestions\":[],\"evidenceInsufficient\":false}";

        String text = gateway().outputText(responseBody(modelJson));

        assertThat(text).isEqualTo(modelJson);
    }

    @Test
    @DisplayName("모델이 반환한 허용되지 않은 약관 식별자는 초안에서 제거한다")
    void retainsOnlySourcesAllowedForTheClaim() throws Exception {
        String modelJson = objectMapper.writeValueAsString(Map.of(
                "termsSourceIds", List.of("terms-clause:ARTICLE-11", "terms-clause:OTHER_PRODUCT"),
                "possibleMissingEvidence", List.of("DIAGNOSIS_CERTIFICATE"),
                "additionalQuestions", List.of("진단서 발급일을 확인해 주세요."),
                "evidenceInsufficient", false));
        ClaimAssessmentAssistantModelRequest request = new ClaimAssessmentAssistantModelRequest(
                "portfolio-verification-request",
                List.of(
                        new ClaimAssessmentSourceReference("TERMS_CLAUSE", "terms-clause:ARTICLE-11", "1.0.0", 11L),
                        new ClaimAssessmentSourceReference("POLICY_VERSION", "policy-version:55", "rule-v1", null)),
                List.of("DIAGNOSIS_CERTIFICATE"),
                List.of("DIAGNOSIS_CERTIFICATE"));

        var draft = gateway().parse(responseBody(modelJson), request);

        assertThat(draft.termsToCheck())
                .extracting(ClaimAssessmentSourceReference::sourceId)
                .containsExactly("terms-clause:ARTICLE-11");
        assertThat(draft.possibleMissingEvidence()).containsExactly("DIAGNOSIS_CERTIFICATE");
        assertThat(draft.evidenceInsufficient()).isFalse();
    }

    @Test
    @DisplayName("SDK 편의 필드만 있는 응답은 원본 API 응답으로 오인하지 않고 차단한다")
    void rejectsSdkConvenienceFieldWithoutRawOutputItems() {
        assertThatThrownBy(() -> gateway().outputText("{\"output_text\":\"{}\"}"))
                .isInstanceOf(ExternalModelCallBlockedException.class);
    }

    private OpenAiClaimAssessmentGateway gateway() {
        return new OpenAiClaimAssessmentGateway(new ClaimReviewCopilotProperties(), objectMapper);
    }

    private String responseBody(String modelJson) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "output", List.of(Map.of(
                        "type", "message",
                        "content", List.of(Map.of("type", "output_text", "text", modelJson))))));
    }
}
