package com.capsule.insurance.payment.infra;

import com.capsule.insurance.payment.application.port.FinancialInterfaceJournalRepository;
import com.capsule.insurance.payment.domain.FinancialInterfaceMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFinancialInterfaceJournalRepository implements FinancialInterfaceJournalRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFinancialInterfaceJournalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(FinancialInterfaceMessage message) {
        jdbcTemplate.update("""
                INSERT INTO public.ifc_financial_message (
                    interface_name,
                    message_type,
                    direction,
                    correlation_id,
                    idempotency_key,
                    business_key,
                    status,
                    error_code,
                    payload_json,
                    payload_hash,
                    occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?)
                """,
                message.interfaceName(),
                message.messageType(),
                message.direction(),
                message.correlationId(),
                message.idempotencyKey(),
                message.businessKey(),
                message.status(),
                message.errorCode(),
                message.payloadJson(),
                sha256(message.payloadJson()),
                Timestamp.from(message.occurredAt())
        );
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
