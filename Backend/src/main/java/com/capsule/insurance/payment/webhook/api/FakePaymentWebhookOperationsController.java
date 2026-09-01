package com.capsule.insurance.payment.webhook.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.payment.webhook.application.PaymentWebhookService;
import com.capsule.insurance.payment.webhook.dto.FakePaymentWebhookRequest;
import com.capsule.insurance.payment.webhook.dto.PaymentWebhookResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/webhooks/fake/payments")
public class FakePaymentWebhookOperationsController {

    private final PaymentWebhookService paymentWebhookService;

    public FakePaymentWebhookOperationsController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping
    public ApiResponse<PaymentWebhookResponse> receive(
            @Valid @RequestBody FakePaymentWebhookRequest request
    ) {
        return ApiResponse.success(paymentWebhookService.receiveFakeWebhook(request));
    }
}
