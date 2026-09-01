package com.capsule.insurance.payment.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.dto.ConfirmPaymentRequest;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/applications/{applicationId}/payment-orders")
    public ApiResponse<PaymentOrderResponse> createInitialPremiumOrder(
            @PathVariable Long applicationId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        return ApiResponse.success(paymentService.createInitialPremiumOrder(
                AuthenticatedUser.id(authentication),
                applicationId,
                idempotencyKey
        ));
    }

    @PostMapping("/payments/{paymentOrderId}/confirm")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> confirm(
            @PathVariable Long paymentOrderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConfirmPaymentRequest request,
            Authentication authentication
    ) {
        PaymentOrderResponse response = paymentService.confirm(
                AuthenticatedUser.id(authentication),
                paymentOrderId,
                idempotencyKey,
                request
        );
        ApiResponse<PaymentOrderResponse> body = ApiResponse.success(response);
        if ("UNKNOWN".equals(response.status()) || "APPROVING".equals(response.status())) {
            return ResponseEntity.accepted().body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/payments/{paymentOrderId}")
    public ApiResponse<PaymentOrderResponse> get(
            @PathVariable Long paymentOrderId,
            Authentication authentication
    ) {
        return ApiResponse.success(paymentService.get(
                AuthenticatedUser.id(authentication),
                paymentOrderId
        ));
    }
}
