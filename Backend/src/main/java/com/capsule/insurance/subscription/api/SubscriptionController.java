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

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/quote")
    public ApiResponse<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.success(subscriptionService.createQuote(request));
    }
}
