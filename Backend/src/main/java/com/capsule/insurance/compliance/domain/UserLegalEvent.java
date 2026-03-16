// #Demo Setting
package com.capsule.insurance.compliance.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLegalEvent {

    private final Long legalEventId;
    private final Long userId;
    private final Long subscriptionId;
    private final Long quickOrderId;
    private final Long sealId;
    private final LegalEventType eventType;
    private final LegalEventResult eventResult;
    private final LocalDateTime eventAt;
    private final String termsVersion;
    private final String evidenceJson;
    private final String requestId;
    private final String sessionId;
    private final LocalDateTime createdAt;
}
