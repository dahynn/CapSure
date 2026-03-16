// #Demo Setting
package com.capsule.insurance.subscription.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubscriptionBillingScheduler {

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleBillingPreparation() {
        // TODO: 실제 정기 결제 대상 조회와 청구 연동 로직을 구현해야 합니다.
        log.info("subscription billing scheduler placeholder executed");
    }
}
