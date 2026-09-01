package com.capsule.insurance.payment.application.port;

import com.capsule.insurance.payment.domain.ApprovedApplication;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.capsule.insurance.payment.domain.PaymentAttempt;
import com.capsule.insurance.payment.domain.PaymentOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Optional<ApprovedApplication> lockOwnedApplication(Long applicationId, Long userId);

    Optional<PaymentOrder> findByBusinessKey(String businessKey);

    Optional<PaymentOrder> findByCreationIdempotencyKey(String idempotencyKey);

    PaymentOrder createOrder(
            String orderNo,
            String businessKey,
            Long applicationId,
            Long policyId,
            BigDecimal amount,
            String currencyCode,
            String idempotencyKey,
            Instant expiresAt
    );

    Optional<PaymentOrder> findOwned(Long paymentOrderId, Long userId);

    Optional<PaymentOrder> lockOwned(Long paymentOrderId, Long userId);

    Optional<PaymentOrder> lockById(Long paymentOrderId);

    Optional<PaymentAttempt> findAttemptByIdempotencyKey(String idempotencyKey);

    Optional<PaymentAttempt> findLatestAttempt(Long paymentOrderId);

    List<PaymentAttempt> findAttempts(Long paymentOrderId);

    PaymentAttempt createProcessingAttempt(
            Long paymentOrderId,
            String provider,
            String providerPaymentKey,
            String idempotencyKey,
            String requestJson
    );

    void markApproving(Long paymentOrderId);

    void completeAttempt(Long paymentAttemptId, GatewayPaymentResult result, String responseJson);

    PaymentOrder completeOrder(Long paymentOrderId, GatewayPaymentResult result);

    void recordReconciliation(
            Long paymentOrderId,
            String provider,
            String localStatus,
            String providerStatus,
            String result,
            String detailsJson
    );
}
