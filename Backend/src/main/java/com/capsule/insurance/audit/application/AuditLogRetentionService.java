package com.capsule.insurance.audit.application;

import com.capsule.insurance.audit.infra.AuditEventLogMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 보존 기간을 지난 감사 이벤트만 삭제합니다. 현재 시각과 같은 경계의 이벤트는 보존합니다. */
@Service
@RequiredArgsConstructor
public class AuditLogRetentionService {

    private final AuditEventLogMapper auditEventLogMapper;
    private final AuditRetentionProperties properties;

    @Transactional
    public int cleanupExpiredAuditLogs(Instant now) {
        properties.validate();
        Instant cutoff = now.minus(properties.getDays(), ChronoUnit.DAYS);
        return auditEventLogMapper.deleteOccurredBefore(cutoff);
    }
}
