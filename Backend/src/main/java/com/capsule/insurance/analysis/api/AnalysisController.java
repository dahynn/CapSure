package com.capsule.insurance.analysis.api;

import com.capsule.insurance.analysis.application.AnalysisService;
import com.capsule.insurance.analysis.dto.CoveragePercentileResponse;
import com.capsule.insurance.analysis.dto.DiagnosisReportResponse;
import com.capsule.insurance.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/diagnosis-report")
    public ApiResponse<DiagnosisReportResponse> getDiagnosisReport(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.success(analysisService.getDiagnosisReport(userId));
    }

    @GetMapping("/coverage-percentile")
    public ApiResponse<CoveragePercentileResponse> getCoveragePercentile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.success(analysisService.getCoveragePercentile(userId));
    }
}
