package com.capsule.insurance.operations.catalog.domain;

public record ProductVersionRelease(
        Long productVersionId,
        String productCode,
        String productVersion,
        Long termsDocumentId,
        String termsVersion,
        String releaseStatus
) {
}

