// #Demo Setting
package com.capsule.insurance.compliance.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserLegalEvent {

    private Long legalEventId;
    private Long userId;
    private Long subscriptionId;
    private Long sealId;
    private LegalEventType eventType;
    private LegalEventResult eventResult;
    private Instant eventAt;
    private String termsVersion;
    private String evidenceJson;
    private String requestId;
    private String sessionId;
    private Instant createdAt;

    // TODO: 이 파트에서 컴플라이언스 이벤트 보관 기간과 삭제 정책이라는 논의점이 있는데, 우선 append-only 조회 모델로 사용한다고 보고 수정 로직 없이 조회 전용으로 구현했음.
}
