package com.capsule.insurance.payment.webhook.infra;

import com.capsule.insurance.payment.webhook.application.port.PaymentWebhookRepository;
import com.capsule.insurance.payment.webhook.domain.PaymentWebhookEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentWebhookRepository implements PaymentWebhookRepository {

    private static final String WEBHOOK_SELECT = """
            SELECT payment_webhook_event_id,
                   provider,
                   provider_event_id,
                   provider_payment_key,
                   event_type,
                   payload_hash,
                   processing_status,
                   received_at,
                   processed_at,
                   error_reason
            FROM public.pay_webhook_event
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentWebhookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PaymentWebhookEvent> findByProviderEventId(
            String provider,
            String providerEventId
    ) {
        return jdbcTemplate.query(
                WEBHOOK_SELECT + " WHERE provider = ? AND provider_event_id = ?",
                this::mapEvent,
                provider,
                providerEventId
        ).stream().findFirst();
    }

    @Override
    public PaymentWebhookEvent saveReceived(
            String provider,
            String providerEventId,
            String providerPaymentKey,
            String eventType,
            String payloadJson,
            String payloadHash
    ) {
        Long eventId = jdbcTemplate.query("""
                INSERT INTO public.pay_webhook_event (
                    provider,
                    provider_event_id,
                    provider_payment_key,
                    event_type,
                    payload_json,
                    payload_hash,
                    processing_status
                ) VALUES (?, ?, ?, ?, CAST(? AS JSONB), ?, 'RECEIVED')
                ON CONFLICT (provider, provider_event_id) DO NOTHING
                RETURNING payment_webhook_event_id
                """,
                (resultSet, rowNumber) -> resultSet.getLong("payment_webhook_event_id"),
                provider,
                providerEventId,
                providerPaymentKey,
                eventType,
                payloadJson,
                payloadHash
        ).stream().findFirst().orElse(null);
        if (eventId == null) {
            return findByProviderEventId(provider, providerEventId).orElseThrow();
        }
        return findById(Objects.requireNonNull(eventId)).orElseThrow();
    }

    @Override
    public PaymentWebhookEvent markProcessed(Long paymentWebhookEventId) {
        jdbcTemplate.update("""
                UPDATE public.pay_webhook_event
                SET processing_status = 'PROCESSED',
                    processed_at = NOW(),
                    error_reason = NULL
                WHERE payment_webhook_event_id = ?
                """, paymentWebhookEventId);
        return findById(paymentWebhookEventId).orElseThrow();
    }

    @Override
    public PaymentWebhookEvent markFailed(Long paymentWebhookEventId, String errorReason) {
        jdbcTemplate.update("""
                UPDATE public.pay_webhook_event
                SET processing_status = 'FAILED',
                    error_reason = ?
                WHERE payment_webhook_event_id = ?
                """, errorReason, paymentWebhookEventId);
        return findById(paymentWebhookEventId).orElseThrow();
    }

    private Optional<PaymentWebhookEvent> findById(Long paymentWebhookEventId) {
        return jdbcTemplate.query(
                WEBHOOK_SELECT + " WHERE payment_webhook_event_id = ?",
                this::mapEvent,
                paymentWebhookEventId
        ).stream().findFirst();
    }

    private PaymentWebhookEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PaymentWebhookEvent(
                resultSet.getLong("payment_webhook_event_id"),
                resultSet.getString("provider"),
                resultSet.getString("provider_event_id"),
                resultSet.getString("provider_payment_key"),
                resultSet.getString("event_type"),
                resultSet.getString("payload_hash"),
                resultSet.getString("processing_status"),
                resultSet.getTimestamp("received_at").toInstant(),
                toInstant(resultSet, "processed_at"),
                resultSet.getString("error_reason")
        );
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
