package com.capsule.insurance.operations.catalog.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.operations.catalog.application.ProductVersionApprovalService;
import com.capsule.insurance.operations.catalog.dto.DecideProductVersionApprovalRequest;
import com.capsule.insurance.operations.catalog.dto.ProductVersionApprovalEventResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/catalog/product-versions/{productVersionId}/approval-decisions")
public class ProductVersionApprovalOperationsController {

    private final ProductVersionApprovalService service;

    public ProductVersionApprovalOperationsController(ProductVersionApprovalService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<ProductVersionApprovalEventResponse> decide(
            @PathVariable Long productVersionId,
            @Valid @RequestBody DecideProductVersionApprovalRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "상품·약관 버전 승인 판단이 기록되었습니다.",
                service.decide(
                        productVersionId,
                        request.decision(),
                        AuthenticatedUser.id(authentication),
                        request.reason()
                )
        );
    }

    @GetMapping
    public ApiResponse<List<ProductVersionApprovalEventResponse>> getHistory(
            @PathVariable Long productVersionId
    ) {
        return ApiResponse.success(service.getHistory(productVersionId));
    }
}

