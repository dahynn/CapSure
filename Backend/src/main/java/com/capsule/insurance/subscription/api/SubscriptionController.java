// #Demo Setting
package com.capsule.insurance.subscription.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.subscription.application.SubscriptionService;
import com.capsule.insurance.subscription.dto.QuoteRequest;
import com.capsule.insurance.subscription.dto.QuoteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capsule.insurance.subscription.dto.SubscriptionDetailResponse;
import com.capsule.insurance.auth.domain.UserAccount;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/quote")
    public ApiResponse<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.success(subscriptionService.createQuote(request));
    }

    @GetMapping("/{subscriptionId}/detail")
    public ApiResponse<SubscriptionDetailResponse> getSubscriptionDetail(
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable("subscriptionId") Long subscriptionId) {
        
        Long userId = userAccount.getUserId();
        return ApiResponse.success(subscriptionService.getSubscriptionDetail(userId, subscriptionId));
    }
}
