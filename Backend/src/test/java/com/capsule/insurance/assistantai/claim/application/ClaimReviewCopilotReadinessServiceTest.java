package com.capsule.insurance.assistantai.claim.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClaimReviewCopilotReadinessServiceTest {

    @Test
    void externalProviderIsReadyOnlyWhenRequiredEnvironmentValuesAndExplicitAllowFlagArePresent() {
        ClaimReviewCopilotProperties properties = new ClaimReviewCopilotProperties();
        properties.setProvider("external");
        properties.setBaseUrl("https://provider.example.test");
        properties.setModel("review-model");
        properties.setApiKey("not-a-real-key");
        properties.setAllowExternalCalls(true);

        var readiness = new ClaimReviewCopilotReadinessService(properties).readiness();

        assertThat(readiness.ready()).isTrue();
        assertThat(readiness.toString()).doesNotContain("not-a-real-key");
    }
}
