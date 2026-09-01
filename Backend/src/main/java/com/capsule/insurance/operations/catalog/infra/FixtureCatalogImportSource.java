package com.capsule.insurance.operations.catalog.infra;

import com.capsule.insurance.operations.catalog.application.port.CatalogImportSource;
import com.capsule.insurance.operations.catalog.domain.CatalogImportBatch;
import com.capsule.insurance.operations.catalog.domain.CatalogImportRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class FixtureCatalogImportSource implements CatalogImportSource {

    private static final String RESOURCE_PATH = "catalog/demo-cancer-catalog-import-v1.json";

    private final ObjectMapper objectMapper;

    public FixtureCatalogImportSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CatalogImportBatch load() {
        try (InputStream input = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            byte[] sourceBytes = input.readAllBytes();
            JsonNode root = objectMapper.readTree(sourceBytes);
            List<CatalogImportRow> rows = objectMapper.convertValue(
                    root.path("rows"),
                    new TypeReference<>() {
                    }
            );
            return new CatalogImportBatch(
                    root.path("sourceName").asText(),
                    sha256(sourceBytes),
                    rows
            );
        } catch (IOException exception) {
            throw new IllegalStateException("암보험 catalog import fixture를 읽지 못했습니다.", exception);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
