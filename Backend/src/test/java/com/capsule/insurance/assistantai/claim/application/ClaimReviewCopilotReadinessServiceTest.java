package com.capsule.insurance.assistantai.claim.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClaimReviewCopilotReadinessServiceTest {

    @Test
    void externalProviderStaysNotReadyUntilAdapterAndExplicitAllowFlagAreApproved() {
        ClaimReviewCopilotProperties properties = new ClaimReviewCopilotProperties();
        properties.setProvider("external");
        properties.setBaseUrl("https://provider.example.test");
        properties.setModel("review-model");
        properties.setApiKey("not-a-real-key");
        properties.setAllowExternalCalls(true);

        var readiness = new ClaimReviewCopilotReadinessService(properties).readiness();

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.blockers()).contains("공급자 HTTP 어댑터는 API 확정 후에만 활성화합니다.");
        assertThat(readiness.toString()).doesNotContain("not-a-real-key");
    }
}
