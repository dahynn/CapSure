package com.capsule.insurance.payment.application.port;

import com.capsule.insurance.payment.domain.GatewayPaymentResult;

/** Reserve outbound capacity before committing a payment attempt that could be dispatched. */
public interface PaymentConfirmationAdmission {
    Permit tryReserveConfirmation();

    interface Permit extends AutoCloseable {
        GatewayPaymentResult confirm(PremiumPaymentGateway.ConfirmCommand command);
        @Override void close();
    }
}
