package com.capsule.insurance.operations.catalog.infra;

import com.capsule.insurance.operations.catalog.application.port.CatalogImportExecutionRepository;
import com.capsule.insurance.operations.catalog.domain.CatalogImportExecution;
import com.capsule.insurance.operations.catalog.domain.CatalogImportRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCatalogImportExecutionRepository implements CatalogImportExecutionRepository {

    private static final String EXECUTION_SELECT = """
            SELECT job_execution_id,
                   job_name,
                   instance_key,
                   execution_no,
                   status,
                   COALESCE(checkpoint_json ->> 'sourceChecksum', '') AS source_checksum,
                   COALESCE(checkpoint_json ->> 'mappingRuleVersion', '') AS mapping_rule_version,
                   COALESCE((checkpoint_json ->> 'nextIndex')::INTEGER, 0) AS next_index,
                   COALESCE((checkpoint_json ->> 'processedChunks')::INTEGER, 0) AS processed_chunks,
                   input_count,
                   accepted_count,
                   duplicate_count,
                   quarantined_count,
                   COALESCE((checkpoint_json ->> 'controlTotalMatched')::BOOLEAN, FALSE)
                       AS control_total_matched,
                   started_at,
                   finished_at,
                   error_reason
            FROM public.ops_job_execution
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogImportExecutionRepository(JdbcTemplate jdbcTemplate) {
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
    public Optional<CatalogImportExecution> findLatest(String jobName, String instanceKey) {
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
    public Optional<CatalogImportExecution> findById(Long jobExecutionId) {
        return jdbcTemplate.query(
                EXECUTION_SELECT + " WHERE job_execution_id = ?",
                this::mapExecution,
                jobExecutionId
        ).stream().findFirst();
    }

    @Override
    public CatalogImportExecution create(
            String jobName,
            String instanceKey,
            String sourceChecksum,
            String mappingRuleVersion
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
                        'sourceChecksum', ?,
                        'mappingRuleVersion', ?,
                        'nextIndex', 0,
                        'processedChunks', 0,
                        'controlTotalMatched', FALSE
                    )
                )
                RETURNING job_execution_id
                """, Long.class, jobName, instanceKey, executionNo, sourceChecksum, mappingRuleVersion);

        return getRequired(jobExecutionId);
    }

    @Override
    public CatalogImportExecution resume(Long jobExecutionId) {
        jdbcTemplate.update("""
                UPDATE public.ops_job_execution
                SET status = 'RUNNING',
                    finished_at = NULL,
                    error_reason = NULL,
                    updated_at = NOW()
                WHERE job_execution_id = ?
                  AND status IN ('FAILED', 'STOPPED')
                """, jobExecutionId);
        return getRequired(jobExecutionId);
    }

    @Override
    public boolean productExists(String productCode, String productVersion) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_product_version
                WHERE product_code = ?
                  AND version = ?
                """, Long.class, productCode, productVersion);
        return count != null && count > 0;
    }

    @Override
    public boolean coverageDefinitionExists(String coverageCode) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_coverage
                WHERE coverage_code = ?
                """, Long.class, coverageCode);
        return count != null && count > 0;
    }

    @Override
    public boolean productCoverageExists(CatalogImportRow row) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_product_coverage product_coverage
                JOIN public.ins_product_version product
                  ON product.product_version_id = product_coverage.product_version_id
                JOIN public.ins_coverage coverage
                  ON coverage.coverage_id = product_coverage.coverage_id
                WHERE product.product_code = ?
                  AND product.version = ?
                  AND coverage.coverage_code = ?
                """, Long.class, row.productCode(), row.productVersion(), row.coverageCode());
        return count != null && count > 0;
    }

    @Override
    public boolean insertProductCoverage(CatalogImportRow row) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO public.ins_product_coverage (
                    product_version_id,
                    coverage_id,
                    insured_amount,
                    currency_code,
                    waiting_period_days,
                    reduction_period_days,
                    reduction_rate,
                    coverage_start_rule,
                    display_order
                )
                SELECT product.product_version_id,
                       coverage.coverage_id,
                       ?, ?, ?, ?, ?, ?, ?
                FROM public.ins_product_version product
                JOIN public.ins_coverage coverage
                  ON coverage.coverage_code = ?
                WHERE product.product_code = ?
                  AND product.version = ?
                ON CONFLICT (product_version_id, coverage_id) DO NOTHING
                """,
                row.insuredAmount(),
                row.currencyCode(),
                row.waitingPeriodDays(),
                row.reductionPeriodDays(),
                row.reductionRate(),
                row.coverageStartRule(),
                row.displayOrder(),
                row.coverageCode(),
                row.productCode(),
                row.productVersion()
        );
        return inserted == 1;
    }

    @Override
    public void quarantine(
            Long jobExecutionId,
            String sourceKey,
            String reasonCode,
            String rawHash,
            String rawPayloadJson
    ) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_quarantine (
                    job_execution_id,
                    source_key,
                    reason_code,
                    raw_hash,
                    raw_payload_json
                ) VALUES (?, ?, ?, ?, CAST(? AS JSONB))
                ON CONFLICT (job_execution_id, source_key) DO NOTHING
                """, jobExecutionId, sourceKey, reasonCode, rawHash, rawPayloadJson);
    }

    @Override
    public CatalogImportExecution saveCheckpoint(
            Long jobExecutionId,
            int nextIndex,
            int processedChunks,
            long inputCount,
            long acceptedCount,
            long duplicateCount,
            long quarantinedCount,
            boolean controlTotalMatched
    ) {
        jdbcTemplate.update("""
                UPDATE public.ops_job_execution
                SET checkpoint_json = jsonb_build_object(
                        'sourceChecksum', checkpoint_json ->> 'sourceChecksum',
                        'mappingRuleVersion', checkpoint_json ->> 'mappingRuleVersion',
                        'nextIndex', ?,
                        'processedChunks', ?,
                        'controlTotalMatched', ?
                    ),
                    input_count = ?,
                    accepted_count = ?,
                    duplicate_count = ?,
                    quarantined_count = ?,
                    updated_at = NOW()
                WHERE job_execution_id = ?
                  AND status = 'RUNNING'
                """,
                nextIndex,
                processedChunks,
                controlTotalMatched,
                inputCount,
                acceptedCount,
                duplicateCount,
                quarantinedCount,
                jobExecutionId
        );
        return getRequired(jobExecutionId);
    }

    @Override
    public CatalogImportExecution complete(Long jobExecutionId) {
        jdbcTemplate.update("""
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
                """, jobExecutionId);
        return getRequired(jobExecutionId);
    }

    @Override
    public CatalogImportExecution fail(Long jobExecutionId, String errorReason) {
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

    private CatalogImportExecution getRequired(Long jobExecutionId) {
        return findById(Objects.requireNonNull(jobExecutionId))
                .orElseThrow(() -> new IllegalStateException("catalog import 실행 원장을 찾을 수 없습니다."));
    }

    private CatalogImportExecution mapExecution(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CatalogImportExecution(
                resultSet.getLong("job_execution_id"),
                resultSet.getString("job_name"),
                resultSet.getString("instance_key"),
                resultSet.getInt("execution_no"),
                resultSet.getString("status"),
                resultSet.getString("source_checksum"),
                resultSet.getString("mapping_rule_version"),
                resultSet.getInt("next_index"),
                resultSet.getInt("processed_chunks"),
                resultSet.getLong("input_count"),
                resultSet.getLong("accepted_count"),
                resultSet.getLong("duplicate_count"),
                resultSet.getLong("quarantined_count"),
                resultSet.getBoolean("control_total_matched"),
                toInstant(resultSet, "started_at"),
                toInstant(resultSet, "finished_at"),
                resultSet.getString("error_reason")
        );
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
