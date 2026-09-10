package com.capsule.insurance.payment.webhook.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.payment.webhook.application.TossPaymentWebhookService;
import com.capsule.insurance.payment.webhook.dto.PaymentWebhookResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Toss Developer Center에 등록할 일반결제 상태 변경 webhook endpoint입니다. */
@RestController
@RequestMapping("/webhooks/toss/payments")
@ConditionalOnProperty(name = "payment.gateway", havingValue = "toss")
public class TossPaymentWebhookController {

    private final TossPaymentWebhookService webhookService;

    public TossPaymentWebhookController(TossPaymentWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PaymentWebhookResponse> receive(
            @RequestHeader("tosspayments-webhook-transmission-id") String transmissionId,
            @RequestBody String payload
    ) {
        return ApiResponse.success(webhookService.receive(transmissionId, payload));
    }
}
