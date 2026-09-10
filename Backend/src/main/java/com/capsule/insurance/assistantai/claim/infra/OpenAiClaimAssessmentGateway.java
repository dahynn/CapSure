package com.capsule.insurance.assistantai.claim.infra;

import com.capsule.insurance.assistantai.application.ExternalModelCallBlockedException;
import com.capsule.insurance.assistantai.claim.application.ClaimReviewCopilotProperties;
import com.capsule.insurance.assistantai.claim.application.port.ClaimAssessmentAssistantGateway;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelDraft;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentAssistantModelRequest;
import com.capsule.insurance.assistantai.claim.domain.ClaimAssessmentSourceReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OpenAI Responses API 어댑터. 허용된 식별자만 전송하고, 호출 허용 전에는 fail-closed로 차단합니다. */
@Component
@ConditionalOnProperty(name = "copilot.claim-review.provider", havingValue = "external")
public class OpenAiClaimAssessmentGateway implements ClaimAssessmentAssistantGateway {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClaimAssessmentGateway.class);

    private final ClaimReviewCopilotProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiClaimAssessmentGateway(ClaimReviewCopilotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.getTimeout()).build();
    }

    @Override
    public ClaimAssessmentAssistantModelDraft generate(ClaimAssessmentAssistantModelRequest request) {
        if (!properties.isAllowExternalCalls() || !configured()) throw new ExternalModelCallBlockedException();
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint()))
                    .timeout(properties.getTimeout())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload(request))))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI claim-review request rejected: status={}", response.statusCode());
                throw new ExternalModelCallBlockedException();
            }
            return parse(response.body(), request);
        } catch (ExternalModelCallBlockedException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("OpenAI claim-review request failed: failureType={}", exception.getClass().getSimpleName());
            throw new ExternalModelCallBlockedException();
        }
    }

    private boolean configured() {
        return StringUtils.hasText(properties.getBaseUrl()) && StringUtils.hasText(properties.getModel())
                && StringUtils.hasText(properties.getApiKey());
    }

    private String endpoint() {
        return properties.getBaseUrl().replaceAll("/+$", "") + "/responses";
    }

    private java.util.Map<String, Object> payload(ClaimAssessmentAssistantModelRequest request) {
        List<java.util.Map<String, String>> sources = request.allowedSources().stream().map(source -> java.util.Map.of(
                "sourceType", source.sourceType(), "sourceId", source.sourceId(), "version", source.version())).toList();
        return java.util.Map.of(
                "model", properties.getModel(),
                "store", false,
                "instructions", "보험금 지급 승인·거절·의학 판단을 하지 마세요. 제공된 sourceId만 인용하고 JSON만 반환하세요.",
                "input", "allowedSources=" + sources
                        + "; requiredEvidenceTypes=" + request.requiredEvidenceTypes()
                        + "; verifiedEvidenceTypes=" + request.verifiedEvidenceTypes(),
                "text", java.util.Map.of("format", java.util.Map.of(
                        "type", "json_schema", "name", "claim_review_draft", "strict", true,
                        "schema", java.util.Map.of("type", "object", "additionalProperties", false,
                                "properties", java.util.Map.of(
                                        "termsSourceIds", java.util.Map.of("type", "array", "items", java.util.Map.of("type", "string")),
                                        "possibleMissingEvidence", java.util.Map.of("type", "array", "items", java.util.Map.of("type", "string")),
                                        "additionalQuestions", java.util.Map.of("type", "array", "items", java.util.Map.of("type", "string")),
                                        "evidenceInsufficient", java.util.Map.of("type", "boolean")),
                                "required", List.of("termsSourceIds", "possibleMissingEvidence", "additionalQuestions", "evidenceInsufficient")))));
    }

    private ClaimAssessmentAssistantModelDraft parse(String body, ClaimAssessmentAssistantModelRequest request) throws Exception {
        JsonNode json = objectMapper.readTree(outputText(body));
        List<String> ids = objectMapper.convertValue(json.path("termsSourceIds"), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() { });
        List<ClaimAssessmentSourceReference> terms = request.allowedSources().stream().filter(source -> ids.contains(source.sourceId())).toList();
        return new ClaimAssessmentAssistantModelDraft(
                terms,
                objectMapper.convertValue(json.path("possibleMissingEvidence"), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() { }),
                objectMapper.convertValue(json.path("additionalQuestions"), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() { }),
                json.path("evidenceInsufficient").asBoolean(true));
    }

    /**
     * {@code output_text} is an SDK convenience property. The raw Responses API payload carries
     * text in {@code output[].content[]} instead, so this adapter extracts only output_text parts.
     */
    private String outputText(String body) throws Exception {
        JsonNode output = objectMapper.readTree(body).path("output");
        if (!output.isArray()) throw new ExternalModelCallBlockedException();
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    text.append(content.path("text").asText());
                }
            }
        }
        if (text.isEmpty()) throw new ExternalModelCallBlockedException();
        return text.toString();
    }
}
