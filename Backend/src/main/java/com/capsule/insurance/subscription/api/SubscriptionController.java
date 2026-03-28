// #Demo Setting
package com.capsule.insurance.subscription.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.subscription.application.SubscriptionService;
import com.capsule.insurance.subscription.dto.QuoteRequest;
import com.capsule.insurance.subscription.dto.QuoteResponse;
import com.capsule.insurance.subscription.dto.RegisterPaymentMethodRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capsule.insurance.subscription.dto.CurrentPaymentMethodResponse;
import com.capsule.insurance.subscription.dto.SubscriptionDetailResponse;
import com.capsule.insurance.subscription.dto.NextItemsResponse;
import com.capsule.insurance.subscription.dto.ReserveNextItemRequest;
import com.capsule.insurance.subscription.dto.ReservedItemResponse;
import com.capsule.insurance.subscription.dto.ConfirmNextResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.capsule.insurance.subscription.dto.CreateSubscriptionRequest;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /* ── 최초 캡슐 가입 ── */
    @PostMapping("")
    public ApiResponse<Long> createInitialSubscription(
            Authentication authentication,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(subscriptionService.createInitialSubscription(userId, request));
    }

    @PostMapping("/quote")
    public ApiResponse<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.success(subscriptionService.createQuote(request));
    }

    @PostMapping("/payment-methods")
    public ApiResponse<Void> registerPaymentMethod(
            Authentication authentication,
            @Valid @RequestBody RegisterPaymentMethodRequest request
    ) {
        Long userId = resolveUserId(authentication);
        subscriptionService.registerPaymentMethod(userId, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/payment-methods/current")
    public ApiResponse<CurrentPaymentMethodResponse> getCurrentPaymentMethod(
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(subscriptionService.getCurrentPaymentMethod(userId));
    }

    @GetMapping("/{subscriptionId}/detail")
    public ApiResponse<SubscriptionDetailResponse> getSubscriptionDetail(
            Authentication authentication,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(subscriptionService.getSubscriptionDetail(userId, subscriptionId));
    }

    @GetMapping("/{subscriptionId}/next-items")
    public ApiResponse<NextItemsResponse> getNextItems(
            Authentication authentication,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(subscriptionService.getNextItems(userId, subscriptionId));
    }

    @PostMapping("/{subscriptionId}/next-items")
    public ApiResponse<ReservedItemResponse> reserveNextItem(
            Authentication authentication,
            @PathVariable("subscriptionId") Long subscriptionId,
            @Valid @RequestBody ReserveNextItemRequest request) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(subscriptionService.reserveNextItem(userId, subscriptionId, request.capsuleProductId()));
    }

    @DeleteMapping("/{subscriptionId}/next-items/{subscriptionItemId}")
    public ApiResponse<Void> cancelNextItem(
            Authentication authentication,
            @PathVariable("subscriptionId") Long subscriptionId,
            @PathVariable("subscriptionItemId") Long subscriptionItemId) {
        Long userId = resolveUserId(authentication);
        subscriptionService.cancelNextItem(userId, subscriptionId, subscriptionItemId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{subscriptionId}/confirm-next")
    public ApiResponse<ConfirmNextResponse> confirmNext(
            Authentication authentication,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.success(subscriptionService.confirmNext(userId, subscriptionId));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Long.parseLong(authentication.getName());
    }
}
