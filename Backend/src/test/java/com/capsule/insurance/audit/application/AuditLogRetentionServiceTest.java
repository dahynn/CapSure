package com.capsule.insurance.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capsule.insurance.audit.infra.AuditEventLogMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionServiceTest {

    @Mock
    private AuditEventLogMapper auditEventLogMapper;

    private AuditRetentionProperties properties;
    private AuditLogRetentionService service;

    @BeforeEach
    void setUp() {
        properties = new AuditRetentionProperties();
        properties.setDays(365);
        service = new AuditLogRetentionService(auditEventLogMapper, properties);
    }

    @Test
    @DisplayName("보존 기간보다 오래된 감사 이벤트만 정리 대상으로 전달한다")
    void removesOnlyEventsOlderThanRetentionPeriod() {
        Instant now = Instant.parse("2026-09-11T03:30:00Z");
        Instant expectedCutoff = Instant.parse("2025-09-11T03:30:00Z");
        when(auditEventLogMapper.deleteOccurredBefore(expectedCutoff)).thenReturn(4);

        int deletedCount = service.cleanupExpiredAuditLogs(now);

        assertThat(deletedCount).isEqualTo(4);
        verify(auditEventLogMapper).deleteOccurredBefore(expectedCutoff);
    }

    @Test
    @DisplayName("0일 이하 보존 기간은 삭제 쿼리를 실행하지 않는다")
    void rejectsNonPositiveRetentionPeriodBeforeDeletingAnything() {
        properties.setDays(0);

        assertThatThrownBy(() -> service.cleanupExpiredAuditLogs(Instant.parse("2026-09-11T03:30:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("감사로그 보존 기간은 하루 이상이어야 합니다.");
        verify(auditEventLogMapper, org.mockito.Mockito.never()).deleteOccurredBefore(any());
    }
}
