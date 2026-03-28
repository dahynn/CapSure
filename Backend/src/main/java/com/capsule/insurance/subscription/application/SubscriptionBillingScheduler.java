// #Demo Setting
package com.capsule.insurance.subscription.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBillingScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleBillingPreparation() {
        int renewedCount = subscriptionService.processDueRenewals(Instant.now());
        log.info("subscription billing scheduler executed: renewedCount={}", renewedCount);
    }
}
