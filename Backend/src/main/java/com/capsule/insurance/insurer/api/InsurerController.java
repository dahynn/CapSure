// #Demo Setting
package com.capsule.insurance.insurer.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.insurer.application.FixedTermsPdfSummaryService;
import com.capsule.insurance.insurer.application.InsurerService;
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
        Long userId = Long.parseLong(authentication.getName()); // 유저 성별로 보험료 조회
        return ApiResponse.success(insurerService.getProducts(category, budget, userId));
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
