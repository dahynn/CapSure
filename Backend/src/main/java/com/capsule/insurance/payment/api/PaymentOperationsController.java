package com.capsule.insurance.payment.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/payments")
public class PaymentOperationsController {

    private final PaymentService paymentService;

    public PaymentOperationsController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{paymentOrderId}/reconcile")
    public ApiResponse<PaymentOrderResponse> reconcile(@PathVariable Long paymentOrderId) {
        return ApiResponse.success(paymentService.reconcile(paymentOrderId));
    }
}
