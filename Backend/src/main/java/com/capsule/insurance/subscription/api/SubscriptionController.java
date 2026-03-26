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
import com.capsule.insurance.subscription.dto.NextItemsResponse;
import com.capsule.insurance.subscription.dto.ReserveNextItemRequest;
import com.capsule.insurance.subscription.dto.ReservedItemResponse;
import com.capsule.insurance.subscription.dto.ConfirmNextResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.success(subscriptionService.getSubscriptionDetail(userId, subscriptionId));
    }

    @GetMapping("/{subscriptionId}/next-items")
    public ApiResponse<NextItemsResponse> getNextItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.success(subscriptionService.getNextItems(userId, subscriptionId));
    }

    @PostMapping("/{subscriptionId}/next-items")
    public ApiResponse<ReservedItemResponse> reserveNextItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId,
            @Valid @RequestBody ReserveNextItemRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.success(subscriptionService.reserveNextItem(userId, subscriptionId, request.capsuleProductId()));
    }

    @DeleteMapping("/{subscriptionId}/next-items/{subscriptionItemId}")
    public ApiResponse<Void> cancelNextItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId,
            @PathVariable("subscriptionItemId") Long subscriptionItemId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        subscriptionService.cancelNextItem(userId, subscriptionId, subscriptionItemId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{subscriptionId}/confirm-next")
    public ApiResponse<ConfirmNextResponse> confirmNext(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("subscriptionId") Long subscriptionId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.success(subscriptionService.confirmNext(userId, subscriptionId));
    }
}
