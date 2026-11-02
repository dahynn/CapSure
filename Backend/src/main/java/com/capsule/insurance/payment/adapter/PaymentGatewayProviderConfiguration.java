package com.capsule.insurance.payment.adapter;

import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentGatewayProviderConfiguration {

    @Bean("selectedPremiumPaymentGateway")
    @ConditionalOnProperty(name = "payment.gateway", havingValue = "fake", matchIfMissing = true)
    PremiumPaymentGateway fakeGateway(FakePremiumPaymentGateway fakeGateway) {
        return fakeGateway;
    }

    @Bean("selectedPremiumPaymentGateway")
    @ConditionalOnProperty(name = "payment.gateway", havingValue = "toss")
    PremiumPaymentGateway tossGateway(
            @Value("${payment.toss.base-url:https://api.tosspayments.com}") String baseUrl,
            @Value("${payment.toss.secret-key:}") String secretKey,
            @Value("${payment.toss.timeout:10s}") Duration timeout,
            @Value("${payment.toss.allow-live-key:false}") boolean allowLiveKey,
            ObjectMapper objectMapper
    ) {
        return new TossPremiumPaymentGateway(baseUrl, secretKey, timeout, allowLiveKey, objectMapper);
    }
}
