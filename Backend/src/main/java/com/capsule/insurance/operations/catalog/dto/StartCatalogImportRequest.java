package com.capsule.insurance.operations.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartCatalogImportRequest(
        @NotBlank @Size(max = 100) String mappingRuleVersion,
        @NotNull @Min(1) @Max(1000) Integer chunkSize
) {
}
