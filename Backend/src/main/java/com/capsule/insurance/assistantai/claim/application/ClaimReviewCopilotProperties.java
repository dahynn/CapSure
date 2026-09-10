package com.capsule.insurance.assistantai.claim.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 외부 공급자 전환 전용 설정입니다. API 키 값은 응답·로그·감사 기록에 노출하지 않습니다. */
@Component
@ConfigurationProperties(prefix = "copilot.claim-review")
public class ClaimReviewCopilotProperties {

    private String provider = "blocked";
    private String baseUrl = "";
    private String model = "";
    private String apiKey = "";
    private Duration timeout = Duration.ofSeconds(10);
    private boolean allowExternalCalls;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public boolean isAllowExternalCalls() { return allowExternalCalls; }
    public void setAllowExternalCalls(boolean allowExternalCalls) { this.allowExternalCalls = allowExternalCalls; }
}
