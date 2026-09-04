package com.capsule.insurance.premiumcollection.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.premiumcollection.application.PremiumCollectionService;
import com.capsule.insurance.premiumcollection.dto.CaptureCollectionRequest;
import com.capsule.insurance.premiumcollection.dto.CreatePremiumReceivableRequest;
import com.capsule.insurance.premiumcollection.dto.InstantSettlementRequest;
import com.capsule.insurance.premiumcollection.dto.PremiumCollectionResponse;
import com.capsule.insurance.premiumcollection.dto.PremiumCollectionTimelineResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ops/premium-collections")
public class PremiumCollectionOperationsController {
    private final PremiumCollectionService service;
    public PremiumCollectionOperationsController(PremiumCollectionService service) { this.service = service; }
    @PostMapping("/receivables") public ApiResponse<PremiumCollectionResponse> create(@Valid @RequestBody CreatePremiumReceivableRequest request) { return ApiResponse.success("보험료 채권과 자동출금 지시를 생성했습니다.", PremiumCollectionResponse.from(service.createDue(request))); }
    @PostMapping("/instant-settlements") public ApiResponse<PremiumCollectionResponse> settle(@Valid @RequestBody InstantSettlementRequest request) { return ApiResponse.success("선납 처리와 자동출금 취소 판단을 완료했습니다.", PremiumCollectionResponse.from(service.settleImmediately(request))); }
    @PostMapping("/instructions/{instructionId}/capture") public ApiResponse<PremiumCollectionResponse> capture(@PathVariable Long instructionId, @Valid @RequestBody CaptureCollectionRequest request) { return ApiResponse.success("자동출금 결과를 반영했습니다.", PremiumCollectionResponse.from(service.captureAutomaticDebit(instructionId, request.providerTransactionKey()))); }
    @PostMapping("/jobs/duplicate-debit-reconciliation") public ApiResponse<Integer> reconcile() { return ApiResponse.success("중복출금 환급 후보 대사를 완료했습니다.", service.createDuplicateDebitRefundCases()); }
    @GetMapping("/timeline") public ApiResponse<PremiumCollectionTimelineResponse> timeline(@RequestParam(defaultValue = "8") int limit) { return ApiResponse.success(service.getTimeline(limit)); }
}
