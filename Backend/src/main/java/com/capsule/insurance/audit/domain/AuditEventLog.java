// #Demo Setting
package com.capsule.insurance.audit.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditEventLog {

    private final Long auditEventId;
    private final AuditEventType eventType;
    private final Long actorUserId;
    private final AuditTargetType targetType;
    private final Long targetId;
    private final LocalDateTime occurredAt;
    private final String requestId;
    private final String beforeJson;
    private final String afterJson;
    private final String integrityHash;
    private final String prevHash;
    private final LocalDateTime createdAt;
}
