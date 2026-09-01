package com.capsule.insurance.catalog.api;

import com.capsule.insurance.catalog.application.CancerProductService;
import com.capsule.insurance.catalog.dto.CancerProductDetailResponse;
import com.capsule.insurance.catalog.dto.CancerProductSummaryResponse;
import com.capsule.insurance.catalog.dto.CancerProductTermsSummaryResponse;
import com.capsule.insurance.catalog.dto.TermsClauseResponse;
import com.capsule.insurance.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CancerProductController {

    private final CancerProductService cancerProductService;

    public CancerProductController(CancerProductService cancerProductService) {
        this.cancerProductService = cancerProductService;
    }

    @GetMapping("/cancer-products")
    public ApiResponse<List<CancerProductSummaryResponse>> getCancerProducts() {
        return ApiResponse.success(cancerProductService.getOnSaleProducts());
    }

    @GetMapping("/cancer-products/{productVersionId}")
    public ApiResponse<CancerProductDetailResponse> getCancerProduct(
            @PathVariable Long productVersionId
    ) {
        return ApiResponse.success(cancerProductService.getProduct(productVersionId));
    }

    @GetMapping("/cancer-products/{productVersionId}/terms/summary")
    public ApiResponse<CancerProductTermsSummaryResponse> getTermsSummary(
            @PathVariable Long productVersionId
    ) {
        return ApiResponse.success(cancerProductService.getTermsSummary(productVersionId));
    }

    @GetMapping("/terms/clauses/{termsClauseId}")
    public ApiResponse<TermsClauseResponse> getTermsClause(
            @PathVariable Long termsClauseId
    ) {
        return ApiResponse.success(cancerProductService.getTermsClause(termsClauseId));
    }
}
