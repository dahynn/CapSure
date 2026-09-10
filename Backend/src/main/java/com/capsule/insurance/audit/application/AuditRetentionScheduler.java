package com.capsule.insurance.audit.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 명시적으로 활성화한 환경에서만 감사로그 정리 배치를 실행합니다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.retention", name = "enabled", havingValue = "true")
public class AuditRetentionScheduler {

    private final AuditLogRetentionService retentionService;
    private final AuditRetentionProperties properties;

    @Scheduled(cron = "${audit.retention.cron:0 30 3 * * *}", zone = "${audit.retention.zone:Asia/Seoul}")
    public void cleanupExpiredAuditLogs() {
        int deletedCount = retentionService.cleanupExpiredAuditLogs(Instant.now());
        log.info("Audit retention cleanup completed: deletedCount={}, retentionDays={}", deletedCount, properties.getDays());
    }
}
