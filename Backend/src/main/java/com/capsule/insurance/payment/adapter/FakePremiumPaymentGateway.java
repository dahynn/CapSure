package com.capsule.insurance.payment.adapter;

import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class FakePremiumPaymentGateway implements PremiumPaymentGateway {

    private final Map<String, GatewayPaymentResult> providerLedger = new ConcurrentHashMap<>();
    private final AtomicInteger confirmationInvocations = new AtomicInteger();

    @Override
    public GatewayPaymentResult confirm(ConfirmCommand command) {
        confirmationInvocations.incrementAndGet();
        return providerLedger.computeIfAbsent(command.providerPaymentKey(), providerPaymentKey -> {
            if (providerPaymentKey.startsWith("fake-paid-")) {
                return GatewayPaymentResult.paid(
                        providerPaymentKey,
                        "FAKE-TX-" + providerPaymentKey.substring("fake-paid-".length())
                );
            }
            if (providerPaymentKey.startsWith("fake-failed-")) {
                return GatewayPaymentResult.failed(providerPaymentKey, "FAKE_CARD_DECLINED");
            }
            if (providerPaymentKey.startsWith("fake-timeout-")) {
                return GatewayPaymentResult.unknown(providerPaymentKey, "FAKE_GATEWAY_TIMEOUT");
            }
            return GatewayPaymentResult.failed(providerPaymentKey, "FAKE_UNSUPPORTED_SCENARIO");
        });
    }

    @Override
    public GatewayPaymentResult inquire(String providerPaymentKey) {
        return providerLedger.getOrDefault(
                providerPaymentKey,
                GatewayPaymentResult.unknown(providerPaymentKey, "FAKE_PAYMENT_NOT_FOUND")
        );
    }

    public void settleAsPaid(String providerPaymentKey) {
        providerLedger.put(
                providerPaymentKey,
                GatewayPaymentResult.paid(providerPaymentKey, "FAKE-TX-RECONCILED")
        );
    }

    public void settleAsFailed(String providerPaymentKey, String errorCode) {
        providerLedger.put(
                providerPaymentKey,
                GatewayPaymentResult.failed(providerPaymentKey, errorCode)
        );
    }

    public int confirmationInvocationCount() {
        return confirmationInvocations.get();
    }
}
