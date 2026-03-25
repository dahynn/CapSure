package com.capsule.insurance.audit.application;

import com.capsule.insurance.audit.domain.AuditEventLog;
import com.capsule.insurance.audit.infra.AuditEventLogMapper;
import com.capsule.insurance.audit.infra.OutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditOutboxWorker {

    private final ObjectMapper objectMapper;
    private final OutboxMapper outboxMapper;
    private final AuditEventLogMapper auditEventLogMapper;

    @Scheduled(fixedDelay = 5000) // 5초 간격 실행
    public void processOutboxEvents() {
        List<Map<String, Object>> rows = outboxMapper.findPendingOutboxRecords();

        for (Map<String, Object> row : rows) {
            Long outboxId = ((Number) row.get("id")).longValue();
            String eventId = (String) row.get("eventId");
            String payloadStr = (String) row.get("payload");

            try {
                AuditEventLog eventLog = objectMapper.readValue(payloadStr, AuditEventLog.class);
                saveToAuditLog(eventLog);
                
                outboxMapper.deleteOutbox(outboxId);

                log.info("Outbox processing success for eventId: {}", eventId);
            } catch (Exception e) {
                log.error("Outbox processing failed for eventId: {}", eventId, e);
                handleFailureToDlq(outboxId, eventId, payloadStr, e.getMessage());
            }
        }
    }

    private void saveToAuditLog(AuditEventLog eventLog) {
        // Idempotency 보장 검증 (이미 들어간 요청인지 확인)
        int count = auditEventLogMapper.countByOutboxTrackingKey(
                eventLog.getRequestId(),
                eventLog.getEventType().name(),
                eventLog.getTargetId()
        );
        if (count > 0) return;

        auditEventLogMapper.saveAuditLog(eventLog);
    }

    private void handleFailureToDlq(Long outboxId, String eventId, String payload, String errorReason) {
        try {
            outboxMapper.saveDlq(eventId, payload, errorReason);
            outboxMapper.updateStatus(outboxId, "FAILED");
        } catch(Exception e) {
            log.error("Failed to move event {} to DLQ", eventId, e);
        }
    }
}
