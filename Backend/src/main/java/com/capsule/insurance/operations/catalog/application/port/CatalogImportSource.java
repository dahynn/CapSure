package com.capsule.insurance.operations.catalog.application.port;

import com.capsule.insurance.operations.catalog.domain.CatalogImportBatch;

public interface CatalogImportSource {

    CatalogImportBatch load();
}
