package com.capsule.insurance.payment.infra;

import com.capsule.insurance.payment.application.port.PaymentRepository;
import com.capsule.insurance.payment.domain.ApprovedApplication;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.capsule.insurance.payment.domain.PaymentAttempt;
import com.capsule.insurance.payment.domain.PaymentOrder;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {

    private static final String ORDER_SELECT = """
            SELECT payment_order.payment_order_id,
                   payment_order.order_no,
                   payment_order.business_key,
                   payment_order.application_id,
                   payment_order.policy_id,
                   application.applicant_user_id AS owner_user_id,
                   payment_order.purpose,
                   payment_order.amount,
                   payment_order.currency_code,
                   payment_order.status,
                   payment_order.idempotency_key,
                   payment_order.expires_at,
                   payment_order.paid_at,
                   payment_order.created_at,
                   payment_order.updated_at
            FROM public.pay_order payment_order
            JOIN public.ins_application application
              ON application.application_id = payment_order.application_id
            """;

    private static final String ATTEMPT_SELECT = """
            SELECT payment_attempt_id,
                   payment_order_id,
                   attempt_no,
                   provider,
                   provider_payment_key,
                   idempotency_key,
                   status,
                   error_code,
                   requested_at,
                   completed_at
            FROM public.pay_attempt
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ApprovedApplication> lockOwnedApplication(Long applicationId, Long userId) {
        return jdbcTemplate.query("""
                SELECT application.application_id,
                       application.applicant_user_id,
                       application.insured_user_id,
                       application.status AS application_status,
                       quote.monthly_premium,
                       quote.currency_code
                FROM public.ins_application application
                JOIN public.ins_quote quote
                  ON quote.quote_id = application.quote_id
                WHERE application.application_id = ?
                  AND application.applicant_user_id = ?
                FOR UPDATE OF application
                """, this::mapApprovedApplication, applicationId, userId).stream().findFirst();
    }

    @Override
    public Optional<PaymentOrder> findByBusinessKey(String businessKey) {
        return jdbcTemplate.query(
                ORDER_SELECT + " WHERE payment_order.business_key = ?",
                this::mapOrder,
                businessKey
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentOrder> findByCreationIdempotencyKey(String idempotencyKey) {
        return jdbcTemplate.query(
                ORDER_SELECT + " WHERE payment_order.idempotency_key = ?",
                this::mapOrder,
                idempotencyKey
        ).stream().findFirst();
    }

    @Override
    public PaymentOrder createOrder(
            String orderNo,
            String businessKey,
            Long applicationId,
            Long policyId,
            BigDecimal amount,
            String currencyCode,
            String idempotencyKey,
            Instant expiresAt
    ) {
        Long paymentOrderId = jdbcTemplate.queryForObject("""
                INSERT INTO public.pay_order (
                    order_no,
                    business_key,
                    application_id,
                    policy_id,
                    purpose,
                    amount,
                    currency_code,
                    status,
                    idempotency_key,
                    expires_at
                ) VALUES (?, ?, ?, ?, 'INITIAL_PREMIUM', ?, ?, 'CREATED', ?, ?)
                RETURNING payment_order_id
                """,
                Long.class,
                orderNo,
                businessKey,
                applicationId,
                policyId,
                amount,
                currencyCode,
                idempotencyKey,
                java.sql.Timestamp.from(expiresAt)
        );
        return findById(Objects.requireNonNull(paymentOrderId)).orElseThrow();
    }

    @Override
    public Optional<PaymentOrder> findOwned(Long paymentOrderId, Long userId) {
        return jdbcTemplate.query(
                ORDER_SELECT + """
                        WHERE payment_order.payment_order_id = ?
                          AND application.applicant_user_id = ?
                        """,
                this::mapOrder,
                paymentOrderId,
                userId
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentOrder> lockOwned(Long paymentOrderId, Long userId) {
        return jdbcTemplate.query(
                ORDER_SELECT + """
                        WHERE payment_order.payment_order_id = ?
                          AND application.applicant_user_id = ?
                        FOR UPDATE OF payment_order
                        """,
                this::mapOrder,
                paymentOrderId,
                userId
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentOrder> lockById(Long paymentOrderId) {
        return jdbcTemplate.query(
                ORDER_SELECT + """
                        WHERE payment_order.payment_order_id = ?
                        FOR UPDATE OF payment_order
                        """,
                this::mapOrder,
                paymentOrderId
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentAttempt> findAttemptByIdempotencyKey(String idempotencyKey) {
        return jdbcTemplate.query(
                ATTEMPT_SELECT + " WHERE idempotency_key = ?",
                this::mapAttempt,
                idempotencyKey
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentAttempt> findAttemptByProviderPaymentKey(
            String provider,
            String providerPaymentKey
    ) {
        return jdbcTemplate.query(
                ATTEMPT_SELECT + " WHERE provider = ? AND provider_payment_key = ?",
                this::mapAttempt,
                provider,
                providerPaymentKey
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentAttempt> findLatestAttempt(Long paymentOrderId) {
        return jdbcTemplate.query(
                ATTEMPT_SELECT + """
                        WHERE payment_order_id = ?
                        ORDER BY attempt_no DESC
                        LIMIT 1
                        """,
                this::mapAttempt,
                paymentOrderId
        ).stream().findFirst();
    }

    @Override
    public List<PaymentAttempt> findAttempts(Long paymentOrderId) {
        return jdbcTemplate.query(
                ATTEMPT_SELECT + " WHERE payment_order_id = ? ORDER BY attempt_no",
                this::mapAttempt,
                paymentOrderId
        );
    }

    @Override
    public PaymentAttempt createProcessingAttempt(
            Long paymentOrderId,
            String provider,
            String providerPaymentKey,
            String idempotencyKey,
            String requestJson
    ) {
        Integer attemptNo = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(attempt_no), 0) + 1
                FROM public.pay_attempt
                WHERE payment_order_id = ?
                """, Integer.class, paymentOrderId);
        Long attemptId = jdbcTemplate.queryForObject("""
                INSERT INTO public.pay_attempt (
                    payment_order_id,
                    attempt_no,
                    provider,
                    provider_payment_key,
                    idempotency_key,
                    status,
                    request_json
                ) VALUES (?, ?, ?, ?, ?, 'PROCESSING', CAST(? AS JSONB))
                RETURNING payment_attempt_id
                """,
                Long.class,
                paymentOrderId,
                attemptNo,
                provider,
                providerPaymentKey,
                idempotencyKey,
                requestJson
        );
        return findAttemptById(Objects.requireNonNull(attemptId)).orElseThrow();
    }

    @Override
    public void markApproving(Long paymentOrderId) {
        jdbcTemplate.update("""
                UPDATE public.pay_order
                SET status = 'APPROVING'
                WHERE payment_order_id = ?
                  AND status = 'CREATED'
                """, paymentOrderId);
    }

    @Override
    public void completeAttempt(
            Long paymentAttemptId,
            GatewayPaymentResult result,
            String responseJson
    ) {
        jdbcTemplate.update("""
                UPDATE public.pay_attempt
                SET status = ?,
                    completed_at = NOW(),
                    response_json = CAST(? AS JSONB),
                    error_code = ?
                WHERE payment_attempt_id = ?
                  AND status IN ('PROCESSING', 'UNKNOWN')
                """, result.status(), responseJson, result.errorCode(), paymentAttemptId);
    }

    @Override
    public PaymentOrder completeOrder(Long paymentOrderId, GatewayPaymentResult result) {
        jdbcTemplate.update("""
                UPDATE public.pay_order
                SET status = ?,
                    paid_at = CASE WHEN ? = 'PAID' THEN NOW() ELSE NULL END
                WHERE payment_order_id = ?
                  AND status IN ('APPROVING', 'UNKNOWN')
                """, result.status(), result.status(), paymentOrderId);
        if ("PAID".equals(result.status())) {
            jdbcTemplate.update("""
                    INSERT INTO public.ops_outbox_event (
                        event_id,
                        aggregate_type,
                        aggregate_id,
                        event_type,
                        payload_json
                    ) VALUES (?, 'PAYMENT_ORDER', ?, 'PAYMENT_PAID', jsonb_build_object(
                        'paymentOrderId', ?,
                        'policyId', (
                            SELECT policy_id
                            FROM public.pay_order
                            WHERE payment_order_id = ?
                        ),
                        'status', 'PAID'
                    ))
                    ON CONFLICT (event_id) DO NOTHING
                    """,
                    "PAYMENT-PAID-" + paymentOrderId,
                    paymentOrderId.toString(),
                    paymentOrderId,
                    paymentOrderId
            );
        }
        return findById(paymentOrderId).orElseThrow();
    }

    @Override
    public void recordReconciliation(
            Long paymentOrderId,
            String provider,
            String localStatus,
            String providerStatus,
            String result,
            String detailsJson
    ) {
        jdbcTemplate.update("""
                INSERT INTO public.ops_reconciliation (
                    target_type,
                    target_id,
                    provider,
                    local_status,
                    provider_status,
                    result,
                    details_json
                ) VALUES ('PAYMENT_ORDER', ?, ?, ?, ?, ?, CAST(? AS JSONB))
                """,
                paymentOrderId.toString(),
                provider,
                localStatus,
                providerStatus,
                result,
                detailsJson
        );
    }

    private Optional<PaymentOrder> findById(Long paymentOrderId) {
        return jdbcTemplate.query(
                ORDER_SELECT + " WHERE payment_order.payment_order_id = ?",
                this::mapOrder,
                paymentOrderId
        ).stream().findFirst();
    }

    private Optional<PaymentAttempt> findAttemptById(Long paymentAttemptId) {
        return jdbcTemplate.query(
                ATTEMPT_SELECT + " WHERE payment_attempt_id = ?",
                this::mapAttempt,
                paymentAttemptId
        ).stream().findFirst();
    }

    private ApprovedApplication mapApprovedApplication(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ApprovedApplication(
                resultSet.getLong("application_id"),
                resultSet.getLong("applicant_user_id"),
                resultSet.getLong("insured_user_id"),
                resultSet.getString("application_status"),
                resultSet.getBigDecimal("monthly_premium"),
                resultSet.getString("currency_code")
        );
    }

    private PaymentOrder mapOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PaymentOrder(
                resultSet.getLong("payment_order_id"),
                resultSet.getString("order_no"),
                resultSet.getString("business_key"),
                resultSet.getLong("application_id"),
                resultSet.getLong("policy_id"),
                resultSet.getLong("owner_user_id"),
                resultSet.getString("purpose"),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("currency_code"),
                resultSet.getString("status"),
                resultSet.getString("idempotency_key"),
                toInstant(resultSet, "expires_at"),
                toInstant(resultSet, "paid_at"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private PaymentAttempt mapAttempt(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PaymentAttempt(
                resultSet.getLong("payment_attempt_id"),
                resultSet.getLong("payment_order_id"),
                resultSet.getInt("attempt_no"),
                resultSet.getString("provider"),
                resultSet.getString("provider_payment_key"),
                resultSet.getString("idempotency_key"),
                resultSet.getString("status"),
                resultSet.getString("error_code"),
                resultSet.getTimestamp("requested_at").toInstant(),
                toInstant(resultSet, "completed_at")
        );
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
