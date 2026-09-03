package com.capsule.insurance.information.application;

import com.capsule.insurance.information.application.port.FinancialEventAuditRepository;
import com.capsule.insurance.operations.outbox.application.NonRetryableOutboxException;
import com.capsule.insurance.operations.outbox.application.port.OutboxEventHandler;
import com.capsule.insurance.operations.outbox.domain.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class FinancialEventAuditProjector implements OutboxEventHandler {

    private final FinancialEventAuditRepository repository;
    private final ObjectMapper objectMapper;

    public FinancialEventAuditProjector(
            FinancialEventAuditRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(OutboxEvent event) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(event.payloadJson());
        } catch (JsonProcessingException exception) {
            throw new NonRetryableOutboxException(
                    "금융 이벤트 payload가 유효한 JSON이 아닙니다.",
                    exception
            );
        }
        if (payload == null || !payload.isObject()) {
            throw new NonRetryableOutboxException(
                    "금융 이벤트 payload는 JSON object여야 합니다.",
                    new IllegalArgumentException("payload is not an object")
            );
        }

        Long policyId = payload.path("policyId").canConvertToLong()
                ? payload.path("policyId").longValue()
                : null;
        repository.save(event, policyId, sha256(event.payloadJson()));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
