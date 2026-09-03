package com.capsule.insurance.operations.reconciliation.infra;

import com.capsule.insurance.operations.reconciliation.application.port.PaymentReconciliationJobRepository;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationExecution;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationOutcome;
import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationTarget;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentReconciliationJobRepository implements PaymentReconciliationJobRepository {

    private static final String EXECUTION_SELECT = """
            SELECT job_execution_id,
                   job_name,
                   instance_key,
                   execution_no,
                   status,
                   (checkpoint_json ->> 'cutoffAt')::TIMESTAMPTZ AS cutoff_at,
                   COALESCE((checkpoint_json ->> 'lastPaymentOrderId')::BIGINT, 0)
                       AS last_payment_order_id,
                   COALESCE((checkpoint_json ->> 'processedChunks')::INTEGER, 0)
                       AS processed_chunks,
                   COALESCE(checkpoint_json ->> 'workerId', '') AS worker_id,
                   processed_count,
                   resolved_count,
                   still_unknown_count,
                   failed_count,
                   COALESCE((checkpoint_json ->> 'controlTotalMatched')::BOOLEAN, FALSE)
                       AS control_total_matched,
                   started_at,
                   finished_at,
                   error_reason
            FROM public.ops_job_execution
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentReconciliationJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void lockInstance(String jobName, String instanceKey) {
        jdbcTemplate.queryForList(
                "SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS TEXT), 0))",
                jobName + ":" + instanceKey
        );
    }

    @Override
    public Optional<PaymentReconciliationExecution> findLatest(String jobName, String instanceKey) {
        return jdbcTemplate.query(
                EXECUTION_SELECT + """
                        WHERE job_name = ?
                          AND instance_key = ?
                        ORDER BY execution_no DESC
                        LIMIT 1
                        """,
                this::mapExecution,
                jobName,
                instanceKey
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentReconciliationExecution> findById(Long jobExecutionId) {
        return jdbcTemplate.query(
                EXECUTION_SELECT + " WHERE job_execution_id = ?",
                this::mapExecution,
                jobExecutionId
        ).stream().findFirst();
    }

    @Override
    public PaymentReconciliationExecution create(
            String jobName,
            String instanceKey,
            Instant cutoffAt,
            String workerId
    ) {
        Integer executionNo = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(execution_no), 0) + 1
                FROM public.ops_job_execution
                WHERE job_name = ?
                  AND instance_key = ?
                """, Integer.class, jobName, instanceKey);

        Long jobExecutionId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ops_job_execution (
                    job_name,
                    instance_key,
                    execution_no,
                    status,
                    checkpoint_json
                ) VALUES (
                    ?,
                    ?,
                    ?,
                    'RUNNING',
                    jsonb_build_object(
                        'cutoffAt', CAST(? AS TIMESTAMPTZ),
                        'lastPaymentOrderId', 0,
                        'processedChunks', 0,
                        'workerId', ?,
                        'controlTotalMatched', FALSE
                    )
                )
                RETURNING job_execution_id
                """,
                Long.class,
                jobName,
                instanceKey,
                executionNo,
                Timestamp.from(cutoffAt),
                workerId
        );
        return getRequired(jobExecutionId);
    }

    @Override
    public PaymentReconciliationExecution resume(Long jobExecutionId, String workerId) {
        int updated = jdbcTemplate.update("""
                UPDATE public.ops_job_execution
                SET status = 'RUNNING',
                    checkpoint_json = jsonb_set(
                        checkpoint_json,
                        '{workerId}',
                        to_jsonb(CAST(? AS TEXT)),
                        TRUE
                    ),
                    finished_at = NULL,
                    error_reason = NULL,
                    updated_at = NOW()
                WHERE job_execution_id = ?
                  AND status IN ('FAILED', 'STOPPED')
                """, workerId, jobExecutionId);
        if (updated != 1) {
            throw new IllegalStateException("재시작할 결제 대사 실행 원장의 상태가 올바르지 않습니다.");
        }
        return getRequired(jobExecutionId);
    }

    @Override
    public List<PaymentReconciliationTarget> claimChunk(
            String workerId,
            long afterPaymentOrderId,
            int chunkSize,
            Instant cutoffAt,
            Instant lockExpiredBefore,
            Instant claimedAt
    ) {
        return jdbcTemplate.query("""
                WITH candidates AS (
                    SELECT payment_order_id
                    FROM public.pay_order
                    WHERE status IN ('APPROVING', 'UNKNOWN')
                      AND payment_order_id > ?
                      AND reconciliation_available_at <= ?
                      AND (
                        reconciliation_locked_at IS NULL
                        OR reconciliation_locked_at < ?
                      )
                    ORDER BY payment_order_id
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                ), claimed AS (
                    UPDATE public.pay_order payment_order
                    SET reconciliation_locked_by = ?,
                        reconciliation_locked_at = ?,
                        reconciliation_attempt_count = reconciliation_attempt_count + 1
                    FROM candidates
                    WHERE payment_order.payment_order_id = candidates.payment_order_id
                    RETURNING payment_order.payment_order_id,
                              payment_order.status,
                              payment_order.reconciliation_attempt_count
                )
                SELECT payment_order_id,
                       status,
                       reconciliation_attempt_count
                FROM claimed
                ORDER BY payment_order_id
                """,
                this::mapTarget,
                afterPaymentOrderId,
                Timestamp.from(cutoffAt),
                Timestamp.from(lockExpiredBefore),
                chunkSize,
                workerId,
                Timestamp.from(claimedAt)
        );
    }

    @Override
    public void recordFailure(PaymentReconciliationTarget target, String errorReason) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_reconciliation (
                    target_type,
                    target_id,
                    provider,
                    local_status,
                    provider_status,
                    result,
                    details_json
                ) VALUES (
                    'PAYMENT_ORDER',
                    ?,
                    'FAKE',
                    ?,
                    'INQUIRY_ERROR',
                    'FAILED',
                    jsonb_build_object('errorReason', CAST(? AS TEXT))
                )
                """, target.paymentOrderId().toString(), target.localStatus(), errorReason);
    }

    @Override
    public PaymentReconciliationExecution saveTargetResult(
            Long jobExecutionId,
            PaymentReconciliationTarget target,
            String workerId,
            PaymentReconciliationOutcome outcome,
            Instant nextAvailableAt,
            int processedChunks,
            long processedCount,
            long resolvedCount,
            long stillUnknownCount,
            long failedCount
    ) {
        int released = jdbcTemplate.update("""
                UPDATE public.pay_order
                SET reconciliation_locked_at = NULL,
                    reconciliation_locked_by = NULL,
                    reconciliation_available_at = ?
                WHERE payment_order_id = ?
                  AND reconciliation_locked_by = ?
                """, Timestamp.from(nextAvailableAt), target.paymentOrderId(), workerId);
        if (released != 1) {
            throw new IllegalStateException(
                    "결제 대사 대상의 작업자 소유권을 확인할 수 없습니다: " + target.paymentOrderId()
            );
        }

        boolean controlTotalMatched = processedCount
                == resolvedCount + stillUnknownCount + failedCount;
        int updated = jdbcTemplate.update("""
                UPDATE public.ops_job_execution
                SET checkpoint_json = jsonb_build_object(
                        'cutoffAt', checkpoint_json ->> 'cutoffAt',
                        'lastPaymentOrderId', ?,
                        'processedChunks', ?,
                        'workerId', ?,
                        'lastOutcome', ?,
                        'controlTotalMatched', ?
                    ),
                    processed_count = ?,
                    resolved_count = ?,
                    still_unknown_count = ?,
                    failed_count = ?,
                    updated_at = NOW()
                WHERE job_execution_id = ?
                  AND status = 'RUNNING'
                """,
                target.paymentOrderId(),
                processedChunks,
                workerId,
                outcome.name(),
                controlTotalMatched,
                processedCount,
                resolvedCount,
                stillUnknownCount,
                failedCount,
                jobExecutionId
        );
        if (updated != 1) {
            throw new IllegalStateException("실행 중인 결제 대사 원장을 갱신할 수 없습니다.");
        }
        return getRequired(jobExecutionId);
    }

    @Override
    public PaymentReconciliationExecution complete(Long jobExecutionId) {
        int updated = jdbcTemplate.update("""
                UPDATE public.ops_job_execution
                SET status = 'COMPLETED',
                    checkpoint_json = jsonb_set(
                        checkpoint_json,
                        '{controlTotalMatched}',
                        'true'::JSONB,
                        TRUE
                    ),
                    finished_at = NOW(),
                    error_reason = NULL,
                    updated_at = NOW()
                WHERE job_execution_id = ?
                  AND status = 'RUNNING'
                  AND processed_count = resolved_count + still_unknown_count + failed_count
                """, jobExecutionId);
        if (updated != 1) {
            throw new IllegalStateException("결제 대사 control total이 일치하지 않습니다.");
        }
        return getRequired(jobExecutionId);
    }

    @Override
    public PaymentReconciliationExecution fail(Long jobExecutionId, String errorReason) {
        jdbcTemplate.update("""
                UPDATE public.ops_job_execution
                SET status = 'FAILED',
                    finished_at = NOW(),
                    error_reason = ?,
                    updated_at = NOW()
                WHERE job_execution_id = ?
                  AND status = 'RUNNING'
                """, errorReason, jobExecutionId);
        return getRequired(jobExecutionId);
    }

    private PaymentReconciliationExecution getRequired(Long jobExecutionId) {
        return findById(Objects.requireNonNull(jobExecutionId))
                .orElseThrow(() -> new IllegalStateException("결제 대사 실행 원장을 찾을 수 없습니다."));
    }

    private PaymentReconciliationExecution mapExecution(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new PaymentReconciliationExecution(
                resultSet.getLong("job_execution_id"),
                resultSet.getString("job_name"),
                resultSet.getString("instance_key"),
                resultSet.getInt("execution_no"),
                resultSet.getString("status"),
                toInstant(resultSet, "cutoff_at"),
                resultSet.getLong("last_payment_order_id"),
                resultSet.getInt("processed_chunks"),
                resultSet.getString("worker_id"),
                resultSet.getLong("processed_count"),
                resultSet.getLong("resolved_count"),
                resultSet.getLong("still_unknown_count"),
                resultSet.getLong("failed_count"),
                resultSet.getBoolean("control_total_matched"),
                toInstant(resultSet, "started_at"),
                toInstant(resultSet, "finished_at"),
                resultSet.getString("error_reason")
        );
    }

    private PaymentReconciliationTarget mapTarget(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new PaymentReconciliationTarget(
                resultSet.getLong("payment_order_id"),
                resultSet.getString("status"),
                resultSet.getInt("reconciliation_attempt_count")
        );
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
