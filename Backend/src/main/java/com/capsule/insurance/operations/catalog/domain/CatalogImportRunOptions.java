package com.capsule.insurance.operations.catalog.domain;

public record CatalogImportRunOptions(
        int chunkSize,
        Integer failAfterChunks
) {

    public CatalogImportRunOptions {
        if (chunkSize <= 0 || chunkSize > 1000) {
            throw new IllegalArgumentException("chunkSize는 1 이상 1000 이하여야 합니다.");
        }
        if (failAfterChunks != null && failAfterChunks <= 0) {
            throw new IllegalArgumentException("failAfterChunks는 양수여야 합니다.");
        }
    }

    public static CatalogImportRunOptions production(int chunkSize) {
        return new CatalogImportRunOptions(chunkSize, null);
    }
}
