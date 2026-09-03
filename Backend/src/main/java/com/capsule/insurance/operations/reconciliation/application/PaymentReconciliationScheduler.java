package com.capsule.insurance.operations.reconciliation.application;

import com.capsule.insurance.operations.reconciliation.domain.PaymentReconciliationRunOptions;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "operations.payment-reconciliation.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationBatchService service;

    public PaymentReconciliationScheduler(PaymentReconciliationBatchService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${operations.payment-reconciliation.fixed-delay-ms:60000}")
    public void reconcile() {
        String instanceKey = "AUTO-" + Instant.now().truncatedTo(ChronoUnit.MINUTES);
        try {
            var result = service.run(
                    instanceKey,
                    PaymentReconciliationRunOptions.production(100, Duration.ofMinutes(1))
            );
            if (result.processedCount() > 0) {
                log.info(
                        "Payment reconciliation completed: instance={}, processed={}, resolved={}, unknown={}, failed={}",
                        result.instanceKey(),
                        result.processedCount(),
                        result.resolvedCount(),
                        result.stillUnknownCount(),
                        result.failedCount()
                );
            }
        } catch (RuntimeException exception) {
            log.error("Payment reconciliation cycle failed: instance={}", instanceKey, exception);
        }
    }
}
