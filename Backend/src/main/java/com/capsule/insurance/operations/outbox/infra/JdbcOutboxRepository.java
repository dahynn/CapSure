package com.capsule.insurance.operations.outbox.infra;

import com.capsule.insurance.operations.outbox.application.port.OutboxRepository;
import com.capsule.insurance.operations.outbox.domain.OutboxEvent;
import com.capsule.insurance.operations.outbox.domain.OutboxReplayTarget;
import com.capsule.insurance.operations.outbox.dto.OutboxReplayResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxRepository implements OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OutboxEvent> claimAvailable(
            String workerId,
            int batchSize,
            Instant now,
            Instant staleLockBefore
    ) {
        return jdbcTemplate.query("""
                WITH candidates AS (
                    SELECT outbox_event_id
                    FROM public.ops_outbox_event
                    WHERE available_at <= ?
                      AND (
                            status IN ('PENDING', 'FAILED')
                            OR (status = 'PROCESSING' AND locked_at < ?)
                      )
                    ORDER BY available_at, outbox_event_id
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE public.ops_outbox_event event
                SET status = 'PROCESSING',
                    attempt_count = event.attempt_count + 1,
                    locked_at = ?,
                    locked_by = ?,
                    last_error = NULL
                FROM candidates
                WHERE event.outbox_event_id = candidates.outbox_event_id
                RETURNING event.outbox_event_id,
                          event.event_id,
                          event.aggregate_type,
                          event.aggregate_id,
                          event.event_type,
                          event.payload_json::TEXT AS payload_json,
                          event.status,
                          event.attempt_count,
                          event.available_at,
                          event.locked_at,
                          event.locked_by,
                          event.created_at
                """,
                this::mapEvent,
                Timestamp.from(now),
                Timestamp.from(staleLockBefore),
                batchSize,
                Timestamp.from(now),
                workerId
        );
    }

    @Override
    public List<OutboxEvent> claimAvailableByEventId(
            String workerId,
            String eventId,
            Instant now,
            Instant staleLockBefore
    ) {
        return jdbcTemplate.query("""
                WITH candidate AS (
                    SELECT outbox_event_id
                    FROM public.ops_outbox_event
                    WHERE event_id = ?
                      AND available_at <= ?
                      AND (
                            status IN ('PENDING', 'FAILED')
                            OR (status = 'PROCESSING' AND locked_at < ?)
                      )
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE public.ops_outbox_event event
                SET status = 'PROCESSING',
                    attempt_count = event.attempt_count + 1,
                    locked_at = ?,
                    locked_by = ?,
                    last_error = NULL
                FROM candidate
                WHERE event.outbox_event_id = candidate.outbox_event_id
                RETURNING event.outbox_event_id,
                          event.event_id,
                          event.aggregate_type,
                          event.aggregate_id,
                          event.event_type,
                          event.payload_json::TEXT AS payload_json,
                          event.status,
                          event.attempt_count,
                          event.available_at,
                          event.locked_at,
                          event.locked_by,
                          event.created_at
                """,
                this::mapEvent,
                eventId,
                Timestamp.from(now),
                Timestamp.from(staleLockBefore),
                Timestamp.from(now),
                workerId
        );
    }

    @Override
    public void markPublished(Long outboxEventId, String workerId) {
        int updated = jdbcTemplate.update("""
                UPDATE public.ops_outbox_event
                SET status = 'PUBLISHED',
                    locked_at = NULL,
                    locked_by = NULL,
                    last_error = NULL
                WHERE outbox_event_id = ?
                  AND status = 'PROCESSING'
                  AND locked_by = ?
                """, outboxEventId, workerId);
        requireSingleUpdate(updated, outboxEventId, workerId);
    }

    @Override
    public void scheduleRetry(
            Long outboxEventId,
            String workerId,
            Instant availableAt,
            String errorReason
    ) {
        int updated = jdbcTemplate.update("""
                UPDATE public.ops_outbox_event
                SET status = 'FAILED',
                    available_at = ?,
                    locked_at = NULL,
                    locked_by = NULL,
                    last_error = ?
                WHERE outbox_event_id = ?
                  AND status = 'PROCESSING'
                  AND locked_by = ?
                """,
                Timestamp.from(availableAt),
                errorReason,
                outboxEventId,
                workerId
        );
        requireSingleUpdate(updated, outboxEventId, workerId);
    }

    @Override
    public void moveToDeadLetter(OutboxEvent event, String workerId, String errorReason) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_dead_letter (
                    event_id,
                    payload_json,
                    error_reason
                ) VALUES (?, CAST(? AS JSONB), ?)
                ON CONFLICT (event_id) DO UPDATE
                SET payload_json = EXCLUDED.payload_json,
                    error_reason = EXCLUDED.error_reason,
                    replay_status = 'PENDING',
                    replay_actor_user_id = NULL,
                    replay_reason = NULL,
                    replayed_at = NULL
                """, event.eventId(), event.payloadJson(), errorReason);

        int updated = jdbcTemplate.update("""
                UPDATE public.ops_outbox_event
                SET status = 'DEAD_LETTER',
                    locked_at = NULL,
                    locked_by = NULL,
                    last_error = ?
                WHERE outbox_event_id = ?
                  AND status = 'PROCESSING'
                  AND locked_by = ?
                """, errorReason, event.outboxEventId(), workerId);
        requireSingleUpdate(updated, event.outboxEventId(), workerId);
    }

    @Override
    public Optional<OutboxReplayTarget> lockReplayTarget(String eventId) {
        return jdbcTemplate.query("""
                SELECT event.outbox_event_id,
                       event.event_id,
                       event.status AS outbox_status,
                       event.attempt_count,
                       dead_letter.dead_letter_id,
                       dead_letter.replay_status
                FROM public.ops_outbox_event event
                LEFT JOIN public.ops_outbox_dead_letter dead_letter
                  ON dead_letter.event_id = event.event_id
                WHERE event.event_id = ?
                FOR UPDATE OF event
                """, (resultSet, rowNumber) -> new OutboxReplayTarget(
                resultSet.getLong("outbox_event_id"),
                resultSet.getString("event_id"),
                resultSet.getString("outbox_status"),
                resultSet.getInt("attempt_count"),
                nullableLong(resultSet, "dead_letter_id"),
                resultSet.getString("replay_status")
        ), eventId).stream().findFirst();
    }

    @Override
    public OutboxReplayResponse replay(
            OutboxReplayTarget target,
            Long actorUserId,
            String reason,
            Instant replayedAt
    ) {
        int dlqUpdated = jdbcTemplate.update("""
                UPDATE public.ops_outbox_dead_letter
                SET replay_status = 'REPLAYED',
                    replay_actor_user_id = ?,
                    replay_reason = ?,
                    replayed_at = ?
                WHERE dead_letter_id = ?
                  AND replay_status = 'PENDING'
                """,
                actorUserId,
                reason,
                Timestamp.from(replayedAt),
                target.deadLetterId()
        );
        if (dlqUpdated != 1) {
            throw new IllegalStateException("DLQ replay 상태가 변경되어 재처리할 수 없습니다.");
        }

        int eventUpdated = jdbcTemplate.update("""
                UPDATE public.ops_outbox_event
                SET status = 'PENDING',
                    attempt_count = 0,
                    available_at = ?,
                    locked_at = NULL,
                    locked_by = NULL,
                    last_error = NULL
                WHERE outbox_event_id = ?
                  AND status = 'DEAD_LETTER'
                """, Timestamp.from(replayedAt), target.outboxEventId());
        if (eventUpdated != 1) {
            throw new IllegalStateException("Outbox dead letter 상태가 변경되어 재처리할 수 없습니다.");
        }

        return new OutboxReplayResponse(
                target.eventId(),
                "PENDING",
                "REPLAYED",
                0,
                actorUserId,
                reason,
                replayedAt,
                replayedAt
        );
    }

    @Override
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT status, COUNT(*) AS event_count
                FROM public.ops_outbox_event
                GROUP BY status
                """);
        for (Map<String, Object> row : rows) {
            counts.put(
                    (String) row.get("status"),
                    ((Number) row.get("event_count")).longValue()
            );
        }
        return counts;
    }

    @Override
    public long countProjectedAuditEvents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.ops_financial_event_audit",
                Long.class
        );
        return count == null ? 0L : count;
    }

    private OutboxEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxEvent(
                resultSet.getLong("outbox_event_id"),
                resultSet.getString("event_id"),
                resultSet.getString("aggregate_type"),
                resultSet.getString("aggregate_id"),
                resultSet.getString("event_type"),
                resultSet.getString("payload_json"),
                resultSet.getString("status"),
                resultSet.getInt("attempt_count"),
                resultSet.getTimestamp("available_at").toInstant(),
                nullableInstant(resultSet, "locked_at"),
                resultSet.getString("locked_by"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private void requireSingleUpdate(int updated, Long outboxEventId, String workerId) {
        if (updated != 1) {
            throw new IllegalStateException(
                    "Outbox 처리권을 잃었습니다. event=" + outboxEventId + ", worker=" + workerId
            );
        }
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
