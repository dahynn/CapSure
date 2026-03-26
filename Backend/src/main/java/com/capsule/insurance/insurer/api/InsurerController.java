// #Demo Setting
package com.capsule.insurance.insurer.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.insurer.application.FixedTermsPdfSummaryService;
import com.capsule.insurance.insurer.application.InsurerService;
import com.capsule.insurance.insurer.dto.ProductDetailResponse;
import com.capsule.insurance.insurer.dto.FixedTermsPdfSummaryResponse;
import com.capsule.insurance.insurer.dto.InsurerProductSummary;
import com.capsule.insurance.insurer.dto.ProductSourceLightSummaryResponse;
import com.capsule.insurance.insurer.dto.ProductSourceTermsSummaryResponse;
import com.capsule.insurance.insurer.dto.ProductSummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/insurers")
public class InsurerController {

    private final InsurerService insurerService;
    private final FixedTermsPdfSummaryService fixedTermsPdfSummaryService;

    public InsurerController(
            InsurerService insurerService,
            FixedTermsPdfSummaryService fixedTermsPdfSummaryService
    ) {
        this.insurerService = insurerService;
        this.fixedTermsPdfSummaryService = fixedTermsPdfSummaryService;
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductSummaryResponse>> getProducts(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String category,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer budget,
            org.springframework.security.core.Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(insurerService.getProducts(category, budget, userId));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductDetailResponse> getProductDetail(
            @PathVariable("id") Long productSourceId,
            org.springframework.security.core.Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(insurerService.getProductDetail(productSourceId, userId));
    }

    private Long getUserId(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return 1L; // 테스트를 위한 기본 유저 ID
        }
        return Long.parseLong(authentication.getName());
    }

    @GetMapping("/product-sources/{productSourceId}/terms-summary")
    public ApiResponse<ProductSourceTermsSummaryResponse> getProductSourceTermsSummary(
            @PathVariable("productSourceId") Long productSourceId
    ) {
        return ApiResponse.success(insurerService.getProductSourceTermsSummary(productSourceId));
    }

    @GetMapping("/product-sources/{productSourceId}/terms-summary/light")
    public ApiResponse<ProductSourceLightSummaryResponse> getProductSourceLightSummary(
            @PathVariable("productSourceId") Long productSourceId
    ) {
        return ApiResponse.success(insurerService.getProductSourceLightSummary(productSourceId));
    }

    @GetMapping("/fixed-terms-pdf/summary")
    public ApiResponse<FixedTermsPdfSummaryResponse> getFixedTermsPdfSummary() {
        return ApiResponse.success(fixedTermsPdfSummaryService.summarizeFixedPdf());
    }
}
