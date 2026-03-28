package com.capsule.insurance.subscription.api;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.subscription.application.SubscriptionService;
import com.capsule.insurance.subscription.dto.ConfirmNextResponse;
import com.capsule.insurance.subscription.dto.CreateSubscriptionRequest;
import com.capsule.insurance.subscription.dto.CurrentPaymentMethodResponse;
import com.capsule.insurance.subscription.dto.MonthlyBillingResponse;
import com.capsule.insurance.subscription.dto.NextItemsResponse;
import com.capsule.insurance.subscription.dto.QuoteRequest;
import com.capsule.insurance.subscription.dto.QuoteResponse;
import com.capsule.insurance.subscription.dto.RegisterPaymentMethodRequest;
import com.capsule.insurance.subscription.dto.ReserveNextItemRequest;
import com.capsule.insurance.subscription.dto.ReservedItemResponse;
import com.capsule.insurance.subscription.dto.ScheduleBillingResponse;
import com.capsule.insurance.subscription.dto.SubscriptionDetailResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("")
    public ApiResponse<Long> createInitialSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.createInitialSubscription(userId, request));
    }

    @PostMapping("/quote")
    public ApiResponse<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.success(subscriptionService.createQuote(request));
    }

    @PostMapping("/payment-methods")
    public ApiResponse<Void> registerPaymentMethod(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegisterPaymentMethodRequest request
    ) {
        Long userId = resolveUserId(userDetails);
        subscriptionService.registerPaymentMethod(userId, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/payment-methods/current")
    public ApiResponse<CurrentPaymentMethodResponse> getCurrentPaymentMethod(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.getCurrentPaymentMethod(userId));
    }

    @GetMapping("/me/monthly-billing")
    public ApiResponse<MonthlyBillingResponse> getMonthlyBilling(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.getMonthlyBilling(userId));
    }

    @GetMapping("/me/schedule-billing")
    public ApiResponse<ScheduleBillingResponse> getScheduleBilling(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.getScheduleBilling(userId));
    }

    @GetMapping("/{subscriptionId}/detail")
    public ApiResponse<SubscriptionDetailResponse> getSubscriptionDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.getSubscriptionDetail(userId, subscriptionId));
    }

    @GetMapping("/{subscriptionId}/next-items")
    public ApiResponse<NextItemsResponse> getNextItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.getNextItems(userId, subscriptionId));
    }

    @PostMapping("/{subscriptionId}/next-items")
    public ApiResponse<ReservedItemResponse> reserveNextItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId,
            @Valid @RequestBody ReserveNextItemRequest request
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.reserveNextItem(userId, subscriptionId, request.capsuleProductId()));
    }

    @DeleteMapping("/{subscriptionId}/next-items/{subscriptionItemId}")
    public ApiResponse<Void> cancelNextItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId,
            @PathVariable("subscriptionItemId") Long subscriptionItemId
    ) {
        Long userId = resolveUserId(userDetails);
        subscriptionService.cancelNextItem(userId, subscriptionId, subscriptionItemId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{subscriptionId}/confirm-next")
    public ApiResponse<ConfirmNextResponse> confirmNext(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.success(subscriptionService.confirmNext(userId, subscriptionId));
    }

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
