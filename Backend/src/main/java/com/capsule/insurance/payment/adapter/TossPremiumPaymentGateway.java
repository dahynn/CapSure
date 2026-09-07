package com.capsule.insurance.payment.adapter;

import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class TossPremiumPaymentGateway implements PremiumPaymentGateway {
    private static final String PROVIDER = "TOSS";

    private final URI baseUri;
    private final String authorization;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final TossHttpTransport transport;

    public TossPremiumPaymentGateway(
            String baseUrl,
            String secretKey,
            Duration timeout,
            boolean allowLiveKey,
            ObjectMapper objectMapper
    ) {
        this(
                URI.create(stripTrailingSlash(baseUrl)),
                secretKey,
                timeout,
                allowLiveKey,
                objectMapper,
                defaultTransport(timeout)
        );
    }

    TossPremiumPaymentGateway(
            URI baseUri,
            String secretKey,
            Duration timeout,
            boolean allowLiveKey,
            ObjectMapper objectMapper,
            TossHttpTransport transport
    ) {
        validateConfiguration(baseUri, secretKey, timeout, allowLiveKey);
        this.baseUri = baseUri;
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(
                (secretKey + ":").getBytes(StandardCharsets.UTF_8)
        );
        this.timeout = timeout;
        this.objectMapper = objectMapper;
        this.transport = transport;
    }

    @Override
    public String providerCode() {
        return PROVIDER;
    }

    @Override
    public GatewayPaymentResult confirm(ConfirmCommand command) {
        if (!"KRW".equals(command.currencyCode())) {
            return GatewayPaymentResult.failed(command.providerPaymentKey(), "TOSS_UNSUPPORTED_CURRENCY");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentKey", command.providerPaymentKey());
        payload.put("orderId", command.orderNo());
        payload.put("amount", integerAmount(command.amount()));
        HttpRequest request = requestBuilder("/v1/payments/confirm")
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", command.idempotencyKey())
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();
        return exchange(request, new InquiryCommand(
                command.orderNo(), command.providerPaymentKey(), command.amount(), command.currencyCode()
        ));
    }

    @Override
    public GatewayPaymentResult inquire(InquiryCommand command) {
        String encodedKey = URLEncoder.encode(command.providerPaymentKey(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpRequest request = requestBuilder("/v1/payments/" + encodedKey)
                .GET()
                .build();
        return exchange(request, command);
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(timeout)
                .header("Authorization", authorization)
                .header("Accept", "application/json");
    }

    private GatewayPaymentResult exchange(HttpRequest request, InquiryCommand expected) {
        String providerPaymentKey = expected.providerPaymentKey();
        try {
            TossHttpResponse response = transport.send(request);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return mapPayment(expected, response.body());
            }
            String errorCode = errorCode(response.body(), "TOSS_HTTP_" + response.statusCode());
            // An unsuccessful lookup is not evidence that the payment failed.
            if ("GET".equals(request.method())
                    || response.statusCode() >= 500
                    || response.statusCode() == 408
                    || response.statusCode() == 429
                    || response.statusCode() == 401
                    || response.statusCode() == 403
                    || response.statusCode() == 409
                    || errorCode.contains("DUPLICATED")
                    || errorCode.contains("ALREADY_PROCESSED")) {
                return GatewayPaymentResult.unknown(providerPaymentKey, errorCode);
            }
            return GatewayPaymentResult.failed(providerPaymentKey, errorCode);
        } catch (HttpTimeoutException exception) {
            return GatewayPaymentResult.unknown(providerPaymentKey, "TOSS_GATEWAY_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return GatewayPaymentResult.unknown(providerPaymentKey, "TOSS_GATEWAY_INTERRUPTED");
        } catch (IOException | RuntimeException exception) {
            return GatewayPaymentResult.unknown(providerPaymentKey, "TOSS_GATEWAY_ERROR");
        }
    }

    private GatewayPaymentResult mapPayment(InquiryCommand expected, String body) {
        String providerPaymentKey = expected.providerPaymentKey();
        try {
            JsonNode payment = objectMapper.readTree(body);
            if (payment == null || !payment.isObject()) {
                return GatewayPaymentResult.unknown(providerPaymentKey, "TOSS_INVALID_RESPONSE");
            }
            if (!providerPaymentKey.equals(payment.path("paymentKey").asText())
                    || !expected.orderNo().equals(payment.path("orderId").asText())
                    || !expected.currencyCode().equals(payment.path("currency").asText())
                    || !payment.path("totalAmount").isNumber()
                    || expected.amount().compareTo(payment.path("totalAmount").decimalValue()) != 0) {
                return GatewayPaymentResult.unknown(providerPaymentKey, "TOSS_PAYMENT_MISMATCH");
            }
            String status = payment.path("status").asText();
            String transactionId = payment.path("lastTransactionKey").asText(providerPaymentKey);
            return switch (status) {
                case "DONE" -> GatewayPaymentResult.paid(providerPaymentKey, transactionId);
                case "ABORTED", "EXPIRED", "CANCELED", "PARTIAL_CANCELED" ->
                        GatewayPaymentResult.failed(providerPaymentKey, "TOSS_" + status);
                default -> GatewayPaymentResult.unknown(
                        providerPaymentKey,
                        status.isBlank() ? "TOSS_INVALID_RESPONSE" : "TOSS_" + status
                );
            };
        } catch (JsonProcessingException exception) {
            return GatewayPaymentResult.unknown(providerPaymentKey, "TOSS_INVALID_RESPONSE");
        }
    }

    private String errorCode(String body, String fallback) {
        try {
            String code = objectMapper.readTree(body).path("code").asText();
            return code.isBlank() ? fallback : code;
        } catch (JsonProcessingException exception) {
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Toss Payments 요청을 직렬화하지 못했습니다.", exception);
        }
    }

    private long integerAmount(BigDecimal amount) {
        try {
            return amount.longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("KRW 결제 금액은 정수여야 합니다.", exception);
        }
    }

    private static TossHttpTransport defaultTransport(Duration timeout) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return request -> {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new TossHttpResponse(response.statusCode(), response.body());
        };
    }

    private static void validateConfiguration(
            URI baseUri,
            String secretKey,
            Duration timeout,
            boolean allowLiveKey
    ) {
        if (!"https".equalsIgnoreCase(baseUri.getScheme())) {
            throw new IllegalArgumentException("Toss Payments API는 HTTPS 주소만 사용할 수 있습니다.");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("TOSS_PAYMENTS_SECRET_KEY가 필요합니다.");
        }
        if (!allowLiveKey && !secretKey.startsWith("test_")) {
            throw new IllegalArgumentException("테스트 실행에서는 test_ 키만 사용할 수 있습니다.");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Toss Payments timeout은 양수여야 합니다.");
        }
    }

    private static String stripTrailingSlash(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @FunctionalInterface
    interface TossHttpTransport {
        TossHttpResponse send(HttpRequest request) throws IOException, InterruptedException;
    }

    record TossHttpResponse(int statusCode, String body) {
    }
}
