package com.capsule.insurance.payment.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.capsule.insurance.payment.application.port.FinancialInterfaceJournalRepository;
import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.FinancialInterfaceMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournaledPremiumPaymentGatewayTest {

    @Test
    void opensCircuitAfterThreeTransportTimeoutsAndKeepsRequestResponsePairs() {
        FakePremiumPaymentGateway delegate = new FakePremiumPaymentGateway();
        RecordingJournal journal = new RecordingJournal();
        JournaledPremiumPaymentGateway gateway = new JournaledPremiumPaymentGateway(
                delegate,
                journal,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-09-03T08:00:00Z"), ZoneOffset.UTC),
                3,
                Duration.ofSeconds(30)
        );

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(gateway.confirm(command("fake-timeout-circuit-" + attempt, attempt)).errorCode())
                    .isEqualTo("FAKE_GATEWAY_TIMEOUT");
        }

        assertThat(gateway.confirm(command("fake-paid-circuit-blocked", 4)).errorCode())
                .isEqualTo("PAYMENT_INTERFACE_CIRCUIT_OPEN");
        assertThat(delegate.confirmationInvocationCount()).isEqualTo(3);
        assertThat(journal.messages).hasSize(8);
        assertThat(journal.messages.get(7).direction()).isEqualTo("INBOUND_RESPONSE");
        assertThat(journal.messages.get(7).status()).isEqualTo("CIRCUIT_OPEN");
        assertThat(journal.messages.stream()
                .filter(message -> "OUTBOUND_REQUEST".equals(message.direction()))
                .map(FinancialInterfaceMessage::correlationId)
                .distinct()
                .count()).isEqualTo(4);
    }

    private PremiumPaymentGateway.ConfirmCommand command(String providerPaymentKey, int sequence) {
        return new PremiumPaymentGateway.ConfirmCommand(
                "PAY-CIRCUIT-" + sequence,
                providerPaymentKey,
                BigDecimal.valueOf(29_900),
                "KRW",
                "confirm-circuit-" + sequence
        );
    }

    private static final class RecordingJournal implements FinancialInterfaceJournalRepository {

        private final List<FinancialInterfaceMessage> messages = new ArrayList<>();

        @Override
        public void append(FinancialInterfaceMessage message) {
            messages.add(message);
        }
    }
}
