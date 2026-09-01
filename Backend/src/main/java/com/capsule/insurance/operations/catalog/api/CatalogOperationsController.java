package com.capsule.insurance.operations.catalog.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.operations.catalog.application.CatalogImportService;
import com.capsule.insurance.operations.catalog.dto.CatalogImportExecutionResponse;
import com.capsule.insurance.operations.catalog.dto.StartCatalogImportRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
public class CatalogOperationsController {

    private final CatalogImportService catalogImportService;

    public CatalogOperationsController(CatalogImportService catalogImportService) {
        this.catalogImportService = catalogImportService;
    }

    @PostMapping("/jobs/catalog-import")
    public ApiResponse<CatalogImportExecutionResponse> startCatalogImport(
            @Valid @RequestBody StartCatalogImportRequest request
    ) {
        return ApiResponse.success(
                "catalog import 실행이 완료되었습니다.",
                catalogImportService.runFixture(request.mappingRuleVersion(), request.chunkSize())
        );
    }

    @GetMapping("/job-executions/{jobExecutionId}")
    public ApiResponse<CatalogImportExecutionResponse> getJobExecution(
            @PathVariable Long jobExecutionId
    ) {
        return ApiResponse.success(catalogImportService.getExecution(jobExecutionId));
    }
}
