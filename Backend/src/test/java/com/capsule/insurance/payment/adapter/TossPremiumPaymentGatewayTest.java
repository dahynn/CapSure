package com.capsule.insurance.payment.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TossPremiumPaymentGatewayTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SECRET_KEY = "test_sk_unit_secret";

    @Test
    void sendsBasicAuthIdempotencyAndServerOrderAmountToConfirmApi() throws Exception {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        TossPremiumPaymentGateway gateway = gateway(request -> {
            captured.set(request);
            return new TossPremiumPaymentGateway.TossHttpResponse(
                    200,
                    "{\"status\":\"DONE\",\"lastTransactionKey\":\"tx-1\"}"
            );
        });

        GatewayPaymentResult result = gateway.confirm(command("payment-key-1"));

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.providerTransactionId()).isEqualTo("tx-1");
        assertThat(gateway.providerCode()).isEqualTo("TOSS");
        HttpRequest request = captured.get();
        assertThat(request.uri()).isEqualTo(URI.create("https://api.tosspayments.com/v1/payments/confirm"));
        assertThat(request.headers().firstValue("Authorization")).contains(
                "Basic " + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8))
        );
        assertThat(request.headers().firstValue("Idempotency-Key")).contains("confirm-key-1");
        JsonNode body = OBJECT_MAPPER.readTree(BodySubscriber.read(request.bodyPublisher().orElseThrow()));
        assertThat(body.path("paymentKey").asText()).isEqualTo("payment-key-1");
        assertThat(body.path("orderId").asText()).isEqualTo("PAY-ORDER-1");
        assertThat(body.path("amount").asLong()).isEqualTo(29_900L);
    }

    @Test
    void mapsDeclineServerFailureAndTimeoutWithoutGuessingPaymentSuccess() {
        TossPremiumPaymentGateway declined = gateway(request ->
                new TossPremiumPaymentGateway.TossHttpResponse(400, "{\"code\":\"REJECT_CARD_PAYMENT\"}"));
        TossPremiumPaymentGateway serverFailure = gateway(request ->
                new TossPremiumPaymentGateway.TossHttpResponse(503, "{\"code\":\"PROVIDER_ERROR\"}"));
        TossPremiumPaymentGateway timeout = gateway(request -> {
            throw new HttpTimeoutException("timeout");
        });

        assertThat(declined.confirm(command("declined")).status()).isEqualTo("FAILED");
        assertThat(serverFailure.confirm(command("server-error")).status()).isEqualTo("UNKNOWN");
        assertThat(timeout.confirm(command("timeout")).errorCode()).isEqualTo("TOSS_GATEWAY_TIMEOUT");
    }

    @Test
    void inquiryUsesPaymentKeyAndMapsDoneState() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        TossPremiumPaymentGateway gateway = gateway(request -> {
            captured.set(request);
            return new TossPremiumPaymentGateway.TossHttpResponse(200, "{\"status\":\"DONE\"}");
        });

        assertThat(gateway.inquire("pay key").status()).isEqualTo("PAID");
        assertThat(captured.get().uri().toString()).endsWith("/v1/payments/pay%20key");
        assertThat(captured.get().method()).isEqualTo("GET");
    }

    @Test
    void rejectsMissingOrLiveSecretAndNonHttpsEndpointByDefault() {
        assertThatThrownBy(() -> new TossPremiumPaymentGateway(
                "https://api.tosspayments.com", "", Duration.ofSeconds(10), false, OBJECT_MAPPER
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TossPremiumPaymentGateway(
                "https://api.tosspayments.com", "live_sk_not_allowed", Duration.ofSeconds(10), false, OBJECT_MAPPER
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TossPremiumPaymentGateway(
                "http://api.tosspayments.com", SECRET_KEY, Duration.ofSeconds(10), false, OBJECT_MAPPER
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private TossPremiumPaymentGateway gateway(TossPremiumPaymentGateway.TossHttpTransport transport) {
        return new TossPremiumPaymentGateway(
                URI.create("https://api.tosspayments.com"),
                SECRET_KEY,
                Duration.ofSeconds(10),
                false,
                OBJECT_MAPPER,
                transport
        );
    }

    private PremiumPaymentGateway.ConfirmCommand command(String paymentKey) {
        return new PremiumPaymentGateway.ConfirmCommand(
                "PAY-ORDER-1",
                paymentKey,
                new BigDecimal("29900"),
                "KRW",
                "confirm-key-1"
        );
    }

    private static final class BodySubscriber implements java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {
        private final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        @Override
        public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(java.nio.ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            try {
                output.write(bytes);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            throw new IllegalStateException(throwable);
        }

        @Override
        public void onComplete() {
        }

        String body() {
            return output.toString(StandardCharsets.UTF_8);
        }

        static String read(HttpRequest.BodyPublisher publisher) {
            BodySubscriber subscriber = new BodySubscriber();
            publisher.subscribe(subscriber);
            return subscriber.body();
        }
    }
}
