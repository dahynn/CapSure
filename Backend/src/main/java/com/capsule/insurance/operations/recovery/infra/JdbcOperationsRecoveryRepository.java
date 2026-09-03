package com.capsule.insurance.operations.recovery.infra;

import com.capsule.insurance.operations.recovery.application.port.OperationsRecoveryRepository;
import com.capsule.insurance.operations.recovery.domain.OperationsRecoveryAction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperationsRecoveryRepository implements OperationsRecoveryRepository {

    private static final String ACTION_SELECT = """
            SELECT recovery_action_id,
                   action_type,
                   target_type,
                   target_id,
                   actor_user_id,
                   reason,
                   status,
                   detected_at,
                   started_at,
                   completed_at,
                   action_duration_ms,
                   recovery_time_ms,
                   error_reason
            FROM public.ops_recovery_action
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcOperationsRecoveryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<OperationsRecoveryAction> startDlqReplay(
            String eventId,
            Long actorUserId,
            String reason,
            Instant startedAt
    ) {
        return jdbcTemplate.query("""
                INSERT INTO public.ops_recovery_action (
                    action_type,
                    target_type,
                    target_id,
                    actor_user_id,
                    reason,
                    status,
                    detected_at,
                    started_at
                )
                SELECT 'DLQ_REPLAY',
                       'OUTBOX_EVENT',
                       dead_letter.event_id,
                       ?,
                       ?,
                       'RUNNING',
                       LEAST(dead_letter.created_at, ?),
                       ?
                FROM public.ops_outbox_dead_letter dead_letter
                WHERE dead_letter.event_id = ?
                  AND dead_letter.replay_status = 'PENDING'
                RETURNING recovery_action_id
                """, (resultSet, rowNumber) -> getRequired(resultSet.getLong("recovery_action_id")),
                actorUserId,
                reason,
                Timestamp.from(startedAt),
                Timestamp.from(startedAt),
                eventId
        ).stream().findFirst();
    }

    @Override
    public OperationsRecoveryAction startPaymentReconciliation(
            Long actorUserId,
            String reason,
            Instant startedAt
    ) {
        Long actionId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ops_recovery_action (
                    action_type,
                    target_type,
                    actor_user_id,
                    reason,
                    status,
                    detected_at,
                    started_at
                ) VALUES (
                    'PAYMENT_RECONCILIATION',
                    'PAYMENT_RECONCILIATION_JOB',
                    ?,
                    ?,
                    'RUNNING',
                    LEAST(
                        COALESCE(
                            (SELECT MIN(updated_at)
                             FROM public.pay_order
                             WHERE status IN ('APPROVING', 'UNKNOWN')
                               AND reconciliation_available_at <= ?),
                            ?
                        ),
                        ?
                    ),
                    ?
                )
                RETURNING recovery_action_id
                """,
                Long.class,
                actorUserId,
                reason,
                Timestamp.from(startedAt),
                Timestamp.from(startedAt),
                Timestamp.from(startedAt),
                Timestamp.from(startedAt)
        );
        return getRequired(actionId);
    }

    @Override
    public OperationsRecoveryAction succeed(
            long recoveryActionId,
            String targetId,
            String resultJson,
            Instant completedAt
    ) {
        updateTerminalState(
                recoveryActionId,
                targetId,
                "SUCCEEDED",
                resultJson,
                null,
                completedAt
        );
        return getRequired(recoveryActionId);
    }

    @Override
    public OperationsRecoveryAction fail(
            long recoveryActionId,
            String targetId,
            String errorReason,
            Instant completedAt
    ) {
        updateTerminalState(
                recoveryActionId,
                targetId,
                "FAILED",
                "{}",
                errorReason,
                completedAt
        );
        return getRequired(recoveryActionId);
    }

    private void updateTerminalState(
            long recoveryActionId,
            String targetId,
            String status,
            String resultJson,
            String errorReason,
            Instant completedAt
    ) {
        int updated = jdbcTemplate.update("""
                UPDATE public.ops_recovery_action
                SET target_id = COALESCE(?, target_id),
                    status = ?,
                    completed_at = ?,
                    action_duration_ms = GREATEST(
                        0,
                        FLOOR(EXTRACT(EPOCH FROM (? - started_at)) * 1000)::BIGINT
                    ),
                    recovery_time_ms = GREATEST(
                        0,
                        FLOOR(EXTRACT(EPOCH FROM (? - detected_at)) * 1000)::BIGINT
                    ),
                    result_json = CAST(? AS JSONB),
                    error_reason = ?
                WHERE recovery_action_id = ?
                  AND status = 'RUNNING'
                """,
                targetId,
                status,
                Timestamp.from(completedAt),
                Timestamp.from(completedAt),
                Timestamp.from(completedAt),
                resultJson,
                errorReason,
                recoveryActionId
        );
        if (updated != 1) {
            throw new IllegalStateException("실행 중인 운영 복구 원장을 완료할 수 없습니다.");
        }
    }

    private OperationsRecoveryAction getRequired(Long recoveryActionId) {
        if (recoveryActionId == null) {
            throw new IllegalStateException("운영 복구 원장 ID가 생성되지 않았습니다.");
        }
        return jdbcTemplate.query(
                ACTION_SELECT + " WHERE recovery_action_id = ?",
                this::mapAction,
                recoveryActionId
        ).stream().findFirst().orElseThrow(
                () -> new IllegalStateException("운영 복구 원장을 찾을 수 없습니다.")
        );
    }

    private OperationsRecoveryAction mapAction(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new OperationsRecoveryAction(
                resultSet.getLong("recovery_action_id"),
                resultSet.getString("action_type"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getLong("actor_user_id"),
                resultSet.getString("reason"),
                resultSet.getString("status"),
                resultSet.getTimestamp("detected_at").toInstant(),
                resultSet.getTimestamp("started_at").toInstant(),
                instantOrNull(resultSet, "completed_at"),
                longOrNull(resultSet, "action_duration_ms"),
                longOrNull(resultSet, "recovery_time_ms"),
                resultSet.getString("error_reason")
        );
    }

    private Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Long longOrNull(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
