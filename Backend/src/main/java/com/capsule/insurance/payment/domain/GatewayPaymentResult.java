package com.capsule.insurance.payment.domain;

public record GatewayPaymentResult(
        String status,
        String providerPaymentKey,
        String providerTransactionId,
        String errorCode,
        String message
) {

    public static GatewayPaymentResult paid(String providerPaymentKey, String transactionId) {
        return new GatewayPaymentResult("PAID", providerPaymentKey, transactionId, null, "승인 완료");
    }

    public static GatewayPaymentResult failed(String providerPaymentKey, String errorCode) {
        return new GatewayPaymentResult("FAILED", providerPaymentKey, null, errorCode, "승인 거절");
    }

    public static GatewayPaymentResult unknown(String providerPaymentKey, String errorCode) {
        return new GatewayPaymentResult("UNKNOWN", providerPaymentKey, null, errorCode, "승인 결과 미확정");
    }
}
