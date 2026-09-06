package com.capsule.insurance.premiumcollection.application;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "operations.premium-billing.scheduler-enabled", havingValue = "true")
public class PremiumBillingScheduler {
    private final PremiumBillingService service;

    public PremiumBillingScheduler(PremiumBillingService service) {
        this.service = service;
    }

    @Scheduled(cron = "${operations.premium-billing.cron:0 5 0 1 * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDate billingCycle = service.today().withDayOfMonth(1);
        try {
            service.run(
                    "AUTO-PREMIUM-BILLING-" + billingCycle,
                    billingCycle,
                    null,
                    "가상 상품 월 정기 보험료 채권 생성"
            );
        } catch (RuntimeException exception) {
            log.error("Recurring premium billing failed; same monthly key is resumable: cycle={}",
                    billingCycle, exception);
        }
    }
}
