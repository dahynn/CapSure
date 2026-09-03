package com.capsule.insurance.operations.dashboard.infra;

import com.capsule.insurance.operations.dashboard.application.port.OperationsDashboardRepository;
import com.capsule.insurance.operations.dashboard.domain.OperationsDashboardSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperationsDashboardRepository implements OperationsDashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOperationsDashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OperationsDashboardSnapshot load(int recentLimit) {
        return new OperationsDashboardSnapshot(
                loadOutboxMetrics(),
                loadReconciliationMetrics(),
                loadRecentJobs(recentLimit),
                loadDeadLetters(recentLimit),
                loadRecentReconciliations(recentLimit)
        );
    }

    private OperationsDashboardSnapshot.OutboxMetrics loadOutboxMetrics() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_count,
                       COUNT(*) FILTER (WHERE status = 'PROCESSING') AS processing_count,
                       COUNT(*) FILTER (WHERE status = 'PUBLISHED') AS published_count,
                       COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_count,
                       (SELECT COUNT(*)
                        FROM public.ops_outbox_dead_letter
                        WHERE replay_status = 'PENDING') AS pending_dead_letter_count,
                       (SELECT COUNT(*)
                        FROM public.ops_financial_event_audit) AS projected_audit_count,
                       MIN(created_at) FILTER (
                           WHERE status IN ('PENDING', 'PROCESSING', 'FAILED')
                       ) AS oldest_unpublished_at
                FROM public.ops_outbox_event
                """, (resultSet, rowNum) -> new OperationsDashboardSnapshot.OutboxMetrics(
                resultSet.getLong("pending_count"),
                resultSet.getLong("processing_count"),
                resultSet.getLong("published_count"),
                resultSet.getLong("failed_count"),
                resultSet.getLong("pending_dead_letter_count"),
                resultSet.getLong("projected_audit_count"),
                instantOrNull(resultSet, "oldest_unpublished_at")
        ));
    }

    private OperationsDashboardSnapshot.ReconciliationMetrics loadReconciliationMetrics() {
        return jdbcTemplate.queryForObject("""
                WITH latest_execution AS (
                    SELECT status,
                           ROW_NUMBER() OVER (
                               PARTITION BY job_name, instance_key
                               ORDER BY execution_no DESC, job_execution_id DESC
                           ) AS row_number
                    FROM public.ops_job_execution
                    WHERE job_name = 'PAYMENT_RECONCILIATION'
                )
                SELECT (SELECT COUNT(*)
                        FROM public.pay_order
                        WHERE status IN ('APPROVING', 'UNKNOWN')) AS waiting_order_count,
                       (SELECT COUNT(*)
                        FROM public.pay_order
                        WHERE status IN ('APPROVING', 'UNKNOWN')
                          AND reconciliation_available_at <= NOW()) AS due_order_count,
                       (SELECT COUNT(*)
                        FROM public.pay_order
                        WHERE status IN ('APPROVING', 'UNKNOWN')
                          AND reconciliation_locked_at IS NOT NULL) AS locked_order_count,
                       COUNT(*) AS total_execution_count,
                       (SELECT COUNT(*)
                        FROM latest_execution
                        WHERE row_number = 1
                          AND status = 'RUNNING') AS running_execution_count,
                       (SELECT COUNT(*)
                        FROM latest_execution
                        WHERE row_number = 1
                          AND status = 'FAILED') AS failed_latest_execution_count,
                       COALESCE(SUM(processed_count), 0) AS processed_count,
                       COALESCE(SUM(resolved_count), 0) AS resolved_count,
                       COALESCE(SUM(still_unknown_count), 0) AS still_unknown_count,
                       COALESCE(SUM(failed_count), 0) AS failed_count
                FROM public.ops_job_execution
                WHERE job_name = 'PAYMENT_RECONCILIATION'
                """, (resultSet, rowNum) -> new OperationsDashboardSnapshot.ReconciliationMetrics(
                resultSet.getLong("waiting_order_count"),
                resultSet.getLong("due_order_count"),
                resultSet.getLong("locked_order_count"),
                resultSet.getLong("total_execution_count"),
                resultSet.getLong("running_execution_count"),
                resultSet.getLong("failed_latest_execution_count"),
                resultSet.getLong("processed_count"),
                resultSet.getLong("resolved_count"),
                resultSet.getLong("still_unknown_count"),
                resultSet.getLong("failed_count")
        ));
    }

    private List<OperationsDashboardSnapshot.JobExecutionItem> loadRecentJobs(int recentLimit) {
        return jdbcTemplate.query("""
                SELECT job_execution_id,
                       job_name,
                       instance_key,
                       execution_no,
                       status,
                       input_count,
                       accepted_count,
                       duplicate_count,
                       quarantined_count,
                       processed_count,
                       resolved_count,
                       still_unknown_count,
                       failed_count,
                       started_at,
                       finished_at,
                       error_reason
                FROM public.ops_job_execution
                ORDER BY started_at DESC, job_execution_id DESC
                LIMIT ?
                """, (resultSet, rowNum) -> new OperationsDashboardSnapshot.JobExecutionItem(
                resultSet.getLong("job_execution_id"),
                resultSet.getString("job_name"),
                resultSet.getString("instance_key"),
                resultSet.getInt("execution_no"),
                resultSet.getString("status"),
                resultSet.getLong("input_count"),
                resultSet.getLong("accepted_count"),
                resultSet.getLong("duplicate_count"),
                resultSet.getLong("quarantined_count"),
                resultSet.getLong("processed_count"),
                resultSet.getLong("resolved_count"),
                resultSet.getLong("still_unknown_count"),
                resultSet.getLong("failed_count"),
                instantOrNull(resultSet, "started_at"),
                instantOrNull(resultSet, "finished_at"),
                resultSet.getString("error_reason")
        ), recentLimit);
    }

    private List<OperationsDashboardSnapshot.DeadLetterItem> loadDeadLetters(int recentLimit) {
        return jdbcTemplate.query("""
                SELECT dead_letter_id,
                       event_id,
                       replay_status,
                       error_reason,
                       replay_reason,
                       created_at,
                       replayed_at
                FROM public.ops_outbox_dead_letter
                ORDER BY CASE replay_status WHEN 'PENDING' THEN 0 ELSE 1 END,
                         created_at DESC,
                         dead_letter_id DESC
                LIMIT ?
                """, (resultSet, rowNum) -> new OperationsDashboardSnapshot.DeadLetterItem(
                resultSet.getLong("dead_letter_id"),
                resultSet.getString("event_id"),
                resultSet.getString("replay_status"),
                resultSet.getString("error_reason"),
                resultSet.getString("replay_reason"),
                instantOrNull(resultSet, "created_at"),
                instantOrNull(resultSet, "replayed_at")
        ), recentLimit);
    }

    private List<OperationsDashboardSnapshot.ReconciliationItem> loadRecentReconciliations(int recentLimit) {
        return jdbcTemplate.query("""
                SELECT reconciliation_id,
                       target_type,
                       target_id,
                       provider,
                       local_status,
                       provider_status,
                       result,
                       executed_at
                FROM public.ops_reconciliation
                ORDER BY executed_at DESC, reconciliation_id DESC
                LIMIT ?
                """, (resultSet, rowNum) -> new OperationsDashboardSnapshot.ReconciliationItem(
                resultSet.getLong("reconciliation_id"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getString("provider"),
                resultSet.getString("local_status"),
                resultSet.getString("provider_status"),
                resultSet.getString("result"),
                instantOrNull(resultSet, "executed_at")
        ), recentLimit);
    }

    private static Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
