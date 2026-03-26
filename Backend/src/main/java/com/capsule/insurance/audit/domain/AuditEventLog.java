// #Demo Setting
package com.capsule.insurance.audit.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Instant occurredAt;
    private String requestId;
    private String beforeJson;
    private String afterJson;
    private String integrityHash;
    private String prevHash;
    private Instant createdAt;

    // TODO: 이 파트에서 감사 로그 보관 및 삭제 정책이라는 논의점이 있는데, 우선 최신순 단순 조회 모델로 구현했음.
    public boolean isIntegrityChained() {
        return prevHash != null && !prevHash.isBlank();
    }

    /**
     * 커스텀 빌더: 마스킹 로직 및 MDC 자동 매핑을 도메인 내부에 캡슐화
     */
    public static class AuditEventLogBuilder {
        private static final ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 보안 검열이 필요한 필드명 정규식
        private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
                "\"(password|ssn|token|accountNumber)\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

        // 발생 시간 및 RequestID 자동 주입을 위한 오버라이딩 (선택적 편의 기능)
        public AuditEventLogBuilder autoFillContext() {
            this.occurredAt = Instant.now();
            if (MDC.get("requestId") != null && !MDC.get("requestId").isBlank()) {
                this.requestId = MDC.get("requestId");
            } else {
                // MDC가 없는 백그라운드 스케줄러 영역 등에서 진입 시 로깅 강행을 위해 임의 식별자 부여
                this.requestId = "SYS-" + java.util.UUID.randomUUID().toString();
            }
            return this;
        }

        public AuditEventLogBuilder beforeData(Object data) {
            this.beforeJson = serializeAndMask(data);
            return this;
        }

        public AuditEventLogBuilder afterData(Object data) {
            this.afterJson = serializeAndMask(data);
            return this;
        }

        private String serializeAndMask(Object data) {
            if (data == null) return null;
            try {
                String rawJson = mapper.writeValueAsString(data);
                Matcher matcher = SENSITIVE_PATTERN.matcher(rawJson);
                // 해당되는 민감 키워드의 값을 모두 "***" 로 치환
                return matcher.replaceAll("\"$1\":\"***\"");
            } catch (JsonProcessingException e) {
                return "{\"error\":\"json_parse_failed\"}";
            }
        }
    }
}
