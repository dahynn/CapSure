package com.capsule.insurance.operations.catalog.application.port;

import com.capsule.insurance.operations.catalog.domain.CatalogImportExecution;
import com.capsule.insurance.operations.catalog.domain.CatalogImportRow;
import java.util.Optional;

public interface CatalogImportExecutionRepository {

    void lockInstance(String jobName, String instanceKey);

    Optional<CatalogImportExecution> findLatest(String jobName, String instanceKey);

    Optional<CatalogImportExecution> findById(Long jobExecutionId);

    CatalogImportExecution create(
            String jobName,
            String instanceKey,
            String sourceChecksum,
            String mappingRuleVersion
    );

    CatalogImportExecution resume(Long jobExecutionId);

    boolean productExists(String productCode, String productVersion);

    boolean coverageDefinitionExists(String coverageCode);

    boolean productCoverageExists(CatalogImportRow row);

    boolean insertProductCoverage(CatalogImportRow row);

    void quarantine(
            Long jobExecutionId,
            String sourceKey,
            String reasonCode,
            String rawHash,
            String rawPayloadJson
    );

    CatalogImportExecution saveCheckpoint(
            Long jobExecutionId,
            int nextIndex,
            int processedChunks,
            long inputCount,
            long acceptedCount,
            long duplicateCount,
            long quarantinedCount,
            boolean controlTotalMatched
    );

    CatalogImportExecution complete(Long jobExecutionId);

    CatalogImportExecution fail(Long jobExecutionId, String errorReason);
}
