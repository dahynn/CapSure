package com.capsule.insurance.operations.catalog.domain;

import java.util.List;

public record CatalogImportBatch(
        String sourceName,
        String sourceChecksum,
        List<CatalogImportRow> rows
) {

    public CatalogImportBatch {
        rows = List.copyOf(rows);
    }
}
