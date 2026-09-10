package com.capsule.insurance.audit.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditRetentionSchedulerTest {

    @Mock
    private AuditLogRetentionService retentionService;

    @Test
    @DisplayName("활성화된 정리 배치는 현재 시각을 기준으로 보존 서비스에 위임한다")
    void delegatesCleanupToRetentionService() {
        AuditRetentionProperties properties = new AuditRetentionProperties();
        properties.setDays(365);
        when(retentionService.cleanupExpiredAuditLogs(any())).thenReturn(2);
        AuditRetentionScheduler scheduler = new AuditRetentionScheduler(retentionService, properties);

        scheduler.cleanupExpiredAuditLogs();

        verify(retentionService).cleanupExpiredAuditLogs(any());
    }
}
