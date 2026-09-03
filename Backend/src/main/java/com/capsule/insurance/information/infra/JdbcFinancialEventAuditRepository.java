package com.capsule.insurance.information.infra;

import com.capsule.insurance.information.application.port.FinancialEventAuditRepository;
import com.capsule.insurance.information.domain.FinancialEventAudit;
import com.capsule.insurance.operations.outbox.domain.OutboxEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFinancialEventAuditRepository implements FinancialEventAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFinancialEventAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(OutboxEvent event, Long policyId, String payloadHash) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_financial_event_audit (
                    event_id,
                    aggregate_type,
                    aggregate_id,
                    event_type,
                    policy_id,
                    payload_json,
                    payload_hash,
                    occurred_at
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                policyId,
                event.payloadJson(),
                payloadHash,
                Timestamp.from(event.createdAt())
        );
    }

    @Override
    public List<FinancialEventAudit> findByPolicyId(Long policyId) {
        return jdbcTemplate.query("""
                SELECT financial_event_audit_id,
                       event_id,
                       aggregate_type,
                       aggregate_id,
                       event_type,
                       policy_id,
                       payload_json::TEXT AS payload_json,
                       payload_hash,
                       occurred_at,
                       projected_at
                FROM public.ops_financial_event_audit
                WHERE policy_id = ?
                ORDER BY occurred_at, financial_event_audit_id
                """, this::mapAudit, policyId);
    }

    private FinancialEventAudit mapAudit(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FinancialEventAudit(
                resultSet.getLong("financial_event_audit_id"),
                resultSet.getString("event_id"),
                resultSet.getString("aggregate_type"),
                resultSet.getString("aggregate_id"),
                resultSet.getString("event_type"),
                nullableLong(resultSet, "policy_id"),
                resultSet.getString("payload_json"),
                resultSet.getString("payload_hash"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getTimestamp("projected_at").toInstant()
        );
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
