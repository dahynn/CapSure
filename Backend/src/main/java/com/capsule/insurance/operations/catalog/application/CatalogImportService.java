package com.capsule.insurance.operations.catalog.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.operations.catalog.application.port.CatalogImportExecutionRepository;
import com.capsule.insurance.operations.catalog.application.port.CatalogImportSource;
import com.capsule.insurance.operations.catalog.domain.CatalogImportBatch;
import com.capsule.insurance.operations.catalog.domain.CatalogImportExecution;
import com.capsule.insurance.operations.catalog.domain.CatalogImportInterruptedException;
import com.capsule.insurance.operations.catalog.domain.CatalogImportRow;
import com.capsule.insurance.operations.catalog.domain.CatalogImportRunOptions;
import com.capsule.insurance.operations.catalog.dto.CatalogImportExecutionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class CatalogImportService {

    private static final String JOB_NAME = "CANCER_CATALOG_IMPORT";

    private final CatalogImportExecutionRepository repository;
    private final CatalogImportSource source;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public CatalogImportService(
            CatalogImportExecutionRepository repository,
            CatalogImportSource source,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.source = source;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public CatalogImportExecutionResponse runFixture(String mappingRuleVersion, int chunkSize) {
        return run(
                source.load(),
                mappingRuleVersion,
                CatalogImportRunOptions.production(chunkSize)
        );
    }

    public CatalogImportExecutionResponse run(
            CatalogImportBatch batch,
            String mappingRuleVersion,
            CatalogImportRunOptions options
    ) {
        validateRunRequest(batch, mappingRuleVersion);
        String instanceKey = batch.sourceChecksum() + ":" + mappingRuleVersion;

        CatalogImportExecution execution = Objects.requireNonNull(transactionTemplate.execute(status -> {
            repository.lockInstance(JOB_NAME, instanceKey);
            return acquireExecution(batch, mappingRuleVersion, instanceKey);
        }));

        if ("COMPLETED".equals(execution.status())) {
            return toResponse(execution);
        }

        int chunksProcessedThisRun = 0;
        try {
            while (execution.nextIndex() < batch.rows().size()) {
                CatalogImportExecution current = execution;
                execution = Objects.requireNonNull(transactionTemplate.execute(status ->
                        processChunk(current, batch, options.chunkSize())));
                chunksProcessedThisRun++;

                if (options.failAfterChunks() != null
                        && chunksProcessedThisRun >= options.failAfterChunks()) {
                    CatalogImportExecution interrupted = execution;
                    transactionTemplate.execute(status -> repository.fail(
                            interrupted.jobExecutionId(),
                            "INJECTED_FAILURE_AFTER_CHUNK_" + interrupted.processedChunks()
                    ));
                    throw new CatalogImportInterruptedException(
                            "검증용 강제 실패가 chunk " + interrupted.processedChunks() + " 이후 발생했습니다."
                    );
                }
            }

            if (!controlTotalMatches(execution)) {
                CatalogImportExecution mismatched = execution;
                transactionTemplate.execute(status -> repository.fail(
                        mismatched.jobExecutionId(),
                        "CONTROL_TOTAL_MISMATCH"
                ));
                throw new IllegalStateException("catalog import control total이 일치하지 않습니다.");
            }

            CatalogImportExecution completed = execution;
            return toResponse(Objects.requireNonNull(transactionTemplate.execute(status ->
                    repository.complete(completed.jobExecutionId()))));
        } catch (CatalogImportInterruptedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            CatalogImportExecution failedExecution = execution;
            transactionTemplate.execute(status -> repository.fail(
                    failedExecution.jobExecutionId(),
                    abbreviate(exception.getClass().getSimpleName() + ": " + exception.getMessage())
            ));
            throw exception;
        }
    }

    public CatalogImportExecutionResponse getExecution(Long jobExecutionId) {
        return repository.findById(jobExecutionId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "catalog import 실행 원장을 찾을 수 없습니다."
                ));
    }

    private CatalogImportExecution acquireExecution(
            CatalogImportBatch batch,
            String mappingRuleVersion,
            String instanceKey
    ) {
        return repository.findLatest(JOB_NAME, instanceKey)
                .map(existing -> switch (existing.status()) {
                    case "COMPLETED" -> existing;
                    case "FAILED", "STOPPED" -> repository.resume(existing.jobExecutionId());
                    case "STARTING", "RUNNING" -> throw new BusinessException(
                            ErrorCode.DUPLICATED_RESOURCE,
                            "동일한 catalog import instance가 이미 실행 중입니다."
                    );
                    default -> throw new IllegalStateException(
                            "지원하지 않는 catalog import 상태입니다: " + existing.status()
                    );
                })
                .orElseGet(() -> repository.create(
                        JOB_NAME,
                        instanceKey,
                        batch.sourceChecksum(),
                        mappingRuleVersion
                ));
    }

    private CatalogImportExecution processChunk(
            CatalogImportExecution execution,
            CatalogImportBatch batch,
            int chunkSize
    ) {
        int fromIndex = execution.nextIndex();
        int toIndex = Math.min(fromIndex + chunkSize, batch.rows().size());
        long inputCount = execution.inputCount();
        long acceptedCount = execution.acceptedCount();
        long duplicateCount = execution.duplicateCount();
        long quarantinedCount = execution.quarantinedCount();

        for (CatalogImportRow row : batch.rows().subList(fromIndex, toIndex)) {
            inputCount++;
            String invalidReason = validateRow(row);
            if (invalidReason == null && !repository.productExists(row.productCode(), row.productVersion())) {
                invalidReason = "UNKNOWN_PRODUCT_VERSION";
            }
            if (invalidReason == null && !repository.coverageDefinitionExists(row.coverageCode())) {
                invalidReason = "UNKNOWN_COVERAGE";
            }

            if (invalidReason != null) {
                quarantine(execution.jobExecutionId(), row, invalidReason);
                quarantinedCount++;
            } else if (repository.productCoverageExists(row)) {
                duplicateCount++;
            } else if (repository.insertProductCoverage(row)) {
                acceptedCount++;
            } else {
                duplicateCount++;
            }
        }

        return repository.saveCheckpoint(
                execution.jobExecutionId(),
                toIndex,
                execution.processedChunks() + 1,
                inputCount,
                acceptedCount,
                duplicateCount,
                quarantinedCount,
                false
        );
    }

    private String validateRow(CatalogImportRow row) {
        if (!StringUtils.hasText(row.sourceKey())) {
            return "MISSING_SOURCE_KEY";
        }
        if (row.sourceKey().length() > 255) {
            return "SOURCE_KEY_TOO_LONG";
        }
        if (!StringUtils.hasText(row.productCode()) || !StringUtils.hasText(row.productVersion())) {
            return "MISSING_PRODUCT_VERSION";
        }
        if (row.productCode().length() > 100 || row.productVersion().length() > 50) {
            return "INVALID_PRODUCT_VERSION_LENGTH";
        }
        if (!StringUtils.hasText(row.coverageCode())) {
            return "MISSING_COVERAGE_CODE";
        }
        if (row.coverageCode().length() > 100) {
            return "INVALID_COVERAGE_CODE_LENGTH";
        }
        if (row.insuredAmount() == null || row.insuredAmount().signum() < 0) {
            return "INVALID_INSURED_AMOUNT";
        }
        if (row.waitingPeriodDays() == null || row.waitingPeriodDays() < 0
                || row.reductionPeriodDays() == null || row.reductionPeriodDays() < 0) {
            return "INVALID_PERIOD";
        }
        if (row.reductionRate() == null
                || row.reductionRate().signum() <= 0
                || row.reductionRate().compareTo(java.math.BigDecimal.ONE) > 0) {
            return "INVALID_REDUCTION_RATE";
        }
        if (!StringUtils.hasText(row.currencyCode())
                || row.currencyCode().length() != 3
                || !StringUtils.hasText(row.coverageStartRule())) {
            return "MISSING_COVERAGE_CONDITION";
        }
        if (row.coverageStartRule().length() > 50) {
            return "INVALID_COVERAGE_START_RULE_LENGTH";
        }
        if (row.displayOrder() == null || row.displayOrder() <= 0) {
            return "INVALID_DISPLAY_ORDER";
        }
        return null;
    }

    private void quarantine(Long jobExecutionId, CatalogImportRow row, String reasonCode) {
        try {
            String payload = objectMapper.writeValueAsString(row);
            String rawHash = sha256(payload);
            repository.quarantine(
                    jobExecutionId,
                    safeSourceKey(row.sourceKey(), rawHash),
                    reasonCode,
                    rawHash,
                    payload
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("격리 대상 catalog 행을 직렬화하지 못했습니다.", exception);
        }
    }

    private boolean controlTotalMatches(CatalogImportExecution execution) {
        return execution.inputCount()
                == execution.acceptedCount() + execution.duplicateCount() + execution.quarantinedCount();
    }

    private void validateRunRequest(CatalogImportBatch batch, String mappingRuleVersion) {
        if (batch == null || !StringUtils.hasText(batch.sourceChecksum()) || batch.rows() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효한 catalog import source가 필요합니다.");
        }
        if (!StringUtils.hasText(mappingRuleVersion)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "mappingRuleVersion이 필요합니다.");
        }
        if (mappingRuleVersion.length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "mappingRuleVersion은 100자 이하여야 합니다.");
        }
    }

    private String safeSourceKey(String sourceKey, String rawHash) {
        if (!StringUtils.hasText(sourceKey)) {
            return "ROW-" + rawHash.substring(0, 32);
        }
        if (sourceKey.length() <= 255) {
            return sourceKey;
        }
        return sourceKey.substring(0, 220) + "-" + rawHash.substring(0, 32);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private String abbreviate(String reason) {
        if (reason == null) {
            return "UNKNOWN_ERROR";
        }
        return reason.length() <= 2000 ? reason : reason.substring(0, 2000);
    }

    private CatalogImportExecutionResponse toResponse(CatalogImportExecution execution) {
        return new CatalogImportExecutionResponse(
                execution.jobExecutionId(),
                execution.jobName(),
                execution.instanceKey(),
                execution.executionNo(),
                execution.status(),
                execution.sourceChecksum(),
                execution.mappingRuleVersion(),
                execution.nextIndex(),
                execution.processedChunks(),
                execution.inputCount(),
                execution.acceptedCount(),
                execution.duplicateCount(),
                execution.quarantinedCount(),
                execution.controlTotalMatched(),
                execution.startedAt(),
                execution.finishedAt(),
                execution.errorReason()
        );
    }
}
