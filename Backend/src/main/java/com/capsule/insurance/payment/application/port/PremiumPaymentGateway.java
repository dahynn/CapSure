package com.capsule.insurance.payment.application.port;

import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import java.math.BigDecimal;

public interface PremiumPaymentGateway {

    GatewayPaymentResult confirm(ConfirmCommand command);

    GatewayPaymentResult inquire(String providerPaymentKey);

    record ConfirmCommand(
            String orderNo,
            String providerPaymentKey,
            BigDecimal amount,
            String currencyCode,
            String idempotencyKey
    ) {
    }
}
