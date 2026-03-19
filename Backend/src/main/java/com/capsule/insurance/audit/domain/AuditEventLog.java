// #Demo Setting
package com.capsule.insurance.audit.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AuditEventLog {

    private Long auditEventId;
    private AuditEventType eventType;
    private Long actorUserId;
    private AuditTargetType targetType;
    private Long targetId;
    private LocalDateTime occurredAt;
    private String requestId;
    private String beforeJson;
    private String afterJson;
    private String integrityHash;
    private String prevHash;
    private LocalDateTime createdAt;

    // TODO: 이 파트에서 감사 로그 보관 및 삭제 정책이라는 논의점이 있는데, 우선 최신순 단순 조회 모델로 구현했음.
    public boolean isIntegrityChained() {
        return prevHash != null && !prevHash.isBlank();
    }
}
