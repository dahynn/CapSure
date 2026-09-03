package com.capsule.insurance.operations.outbox.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "operations.outbox.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxRelayScheduler {

    private final OutboxRelayService relayService;

    public OutboxRelayScheduler(OutboxRelayService relayService) {
        this.relayService = relayService;
    }

    @Scheduled(fixedDelayString = "${operations.outbox.fixed-delay-ms:5000}")
    public void relay() {
        try {
            var result = relayService.relay(100);
            if (result.claimedCount() > 0) {
                log.info(
                        "Financial outbox relay completed: claimed={}, published={}, retry={}, dlq={}",
                        result.claimedCount(),
                        result.publishedCount(),
                        result.retryScheduledCount(),
                        result.deadLetterCount()
                );
            }
        } catch (RuntimeException exception) {
            log.error("Financial outbox relay cycle failed", exception);
        }
    }
}
