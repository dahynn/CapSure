package com.capsule.insurance.premiumcollection.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "operations.premium-delinquency.scheduler-enabled", havingValue = "true")
public class PremiumDelinquencyScheduler {
    private final PremiumDelinquencyService service;
    public PremiumDelinquencyScheduler(PremiumDelinquencyService service) { this.service = service; }

    @Scheduled(cron = "${operations.premium-delinquency.cron:0 10 0 * * *}", zone = "Asia/Seoul")
    public void run() {
        try {
            service.run("AUTO-DELINQUENCY-" + service.today(), null, "가상 상품 일일 미납 점검");
        } catch (RuntimeException ex) {
            log.error("Premium delinquency batch failed; same daily key is resumable", ex);
        }
    }
}
