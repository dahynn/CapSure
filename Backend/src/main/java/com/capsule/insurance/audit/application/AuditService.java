package com.capsule.insurance.audit.application;

import com.capsule.insurance.audit.domain.AuditEventLog;
import com.capsule.insurance.audit.infra.OutboxMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    /**
     * 메인 비즈니스 계층에서 호출 필수.
     * 자체 @Transactional 없음 - 호출한 비즈니스 트랜잭션과 생명주기를 강제로 묶어
     * 도메인 상태 변경 성공 여부와 로깅의 완벽한 상태 정합성을 보장.
     */
    public void saveOutbox(AuditEventLog eventLog) {
        try {
            // Outbox 고유 식별자: Timestamp(밀리초) + UUID 조합 (B-Tree 인덱스 정렬 최적화 및 충돌 방지)
            String eventId = System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
            String payload = objectMapper.writeValueAsString(eventLog);
            outboxMapper.saveOutbox(eventId, payload);
        } catch (JsonProcessingException e) {
            log.error("Outbox Json Serialization failed", e);
            throw new RuntimeException("Audit event outbox save failed", e);
        }
    }
}
