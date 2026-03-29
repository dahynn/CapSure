// #Demo Setting
package com.capsule.insurance.insurer.api;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.insurer.application.FixedTermsPdfSummaryService;
import com.capsule.insurance.insurer.application.InsurerService;
import com.capsule.insurance.insurer.application.ProductSourceAiSummaryService;
import com.capsule.insurance.insurer.dto.ProductDetailResponse;
import com.capsule.insurance.insurer.dto.FixedTermsPdfSummaryResponse;
import com.capsule.insurance.insurer.dto.ProductSourceAiSummaryResponse;
import com.capsule.insurance.insurer.dto.ProductSourceLightSummaryResponse;
import com.capsule.insurance.insurer.dto.ProductSourceTermsSummaryResponse;
import com.capsule.insurance.insurer.dto.ProductSummaryPageResponse;
import com.capsule.insurance.insurer.dto.ProductSummaryResponse;
import com.capsule.insurance.insurer.dto.CategoryRecommendResponse;
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
    private final ProductSourceAiSummaryService productSourceAiSummaryService;

    public InsurerController(
            InsurerService insurerService,
            FixedTermsPdfSummaryService fixedTermsPdfSummaryService,
            ProductSourceAiSummaryService productSourceAiSummaryService
    ) {
        this.insurerService = insurerService;
        this.fixedTermsPdfSummaryService = fixedTermsPdfSummaryService;
        this.productSourceAiSummaryService = productSourceAiSummaryService;
    }

    @GetMapping("/products")
    public ApiResponse<ProductSummaryPageResponse> getProducts(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String category,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer budget,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "popular") String sortBy,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "0") Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "12") Integer size,
            org.springframework.security.core.Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(insurerService.getProducts(category, budget, sortBy, page, size, userId));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductDetailResponse> getProductDetail(
            @PathVariable("id") Long productSourceId,
            org.springframework.security.core.Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(insurerService.getProductDetail(productSourceId, userId));
    }

    @GetMapping("/products/category-recommendations")
    public ApiResponse<List<CategoryRecommendResponse>> getCategoryRecommendations(
            org.springframework.security.core.Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(insurerService.getCategoryRecommendations(userId));
    }

    private Long getUserId(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효한 사용자 정보가 아닙니다.");
        }
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

    @GetMapping("/product-sources/{productSourceId}/ai-summary")
    public ApiResponse<ProductSourceAiSummaryResponse> getProductSourceAiSummary(
            @PathVariable("productSourceId") Long productSourceId
    ) {
        return ApiResponse.success(productSourceAiSummaryService.getProductSourceAiSummary(productSourceId));
    }

    @GetMapping("/fixed-terms-pdf/summary")
    public ApiResponse<FixedTermsPdfSummaryResponse> getFixedTermsPdfSummary() {
        return ApiResponse.success(fixedTermsPdfSummaryService.summarizeFixedPdf());
    }
}
