package com.capsule.insurance.application.api;

import com.capsule.insurance.application.application.ApplicationService;
import com.capsule.insurance.application.dto.ApplicationResponse;
import com.capsule.insurance.application.dto.CreateApplicationRequest;
import com.capsule.insurance.application.dto.CreateConsentRequest;
import com.capsule.insurance.application.dto.ReplaceDisclosuresRequest;
import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ApiResponse<ApplicationResponse> create(
            @Valid @RequestBody CreateApplicationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(applicationService.create(AuthenticatedUser.id(authentication), request));
    }

    @PutMapping("/{applicationId}/disclosures")
    public ApiResponse<ApplicationResponse> replaceDisclosures(
            @PathVariable Long applicationId,
            @Valid @RequestBody ReplaceDisclosuresRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(applicationService.replaceDisclosures(
                AuthenticatedUser.id(authentication),
                applicationId,
                request
        ));
    }

    @PostMapping("/{applicationId}/consents")
    public ApiResponse<ApplicationResponse> recordConsent(
            @PathVariable Long applicationId,
            @Valid @RequestBody CreateConsentRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(applicationService.recordConsent(
                AuthenticatedUser.id(authentication),
                applicationId,
                request
        ));
    }

    @PostMapping("/{applicationId}/submit")
    public ApiResponse<ApplicationResponse> submit(
            @PathVariable Long applicationId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        return ApiResponse.success(applicationService.submit(
                AuthenticatedUser.id(authentication),
                applicationId,
                idempotencyKey
        ));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<ApplicationResponse> get(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        return ApiResponse.success(applicationService.get(
                AuthenticatedUser.id(authentication),
                applicationId
        ));
    }
}
