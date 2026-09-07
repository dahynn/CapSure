package com.capsule.insurance.payment.application;

import com.capsule.insurance.payment.application.port.PaymentCircuitStateStore;
import com.capsule.insurance.payment.application.port.PaymentCircuitStateStore.Change;
import com.capsule.insurance.payment.application.port.PaymentCircuitStateStore.State;
import com.capsule.insurance.payment.application.port.PaymentInterfaceCircuitStatusProvider.CircuitStatus;
import java.time.Duration;

public class PaymentCircuitBreaker {
    private final PaymentCircuitStateStore store;
    private final int threshold;
    private final Duration openDuration;
    private final Duration probeLease;

    public PaymentCircuitBreaker(PaymentCircuitStateStore store, int threshold, Duration openDuration, Duration probeLease) {
        this.store = store;
        this.threshold = threshold;
        this.openDuration = openDuration;
        this.probeLease = probeLease;
    }

    public record Permit(String interfaceName, long generation) {}

    public Permit acquire(String interfaceName) {
        return store.update(interfaceName, (state, now) -> {
            if (state.openUntil() == null) return new Change<>(state, new Permit(interfaceName, state.generation()));
            if (now.isBefore(state.openUntil()) || (state.probeUntil() != null && now.isBefore(state.probeUntil()))) {
                return new Change<>(state, null);
            }
            State probing = new State(state.failures(), state.openUntil(), now.plus(probeLease), state.generation() + 1);
            return new Change<>(probing, new Permit(interfaceName, probing.generation()));
        });
    }

    public void complete(Permit permit, boolean uncertain) {
        store.update(permit.interfaceName(), (state, now) -> {
            // Ignore an outcome from before the circuit opened or from an expired probe lease.
            if (state.generation() != permit.generation()) return new Change<>(state, null);
            if (!uncertain) {
                return new Change<>(new State(0, null, null,
                        state.generation() + (state.openUntil() == null ? 0 : 1)), null);
            }
            int failures = state.failures() + 1;
            State next = failures >= threshold
                    ? new State(failures, now.plus(openDuration), null, state.generation() + 1)
                    : new State(failures, null, null, state.generation());
            return new Change<>(next, null);
        });
    }

    public CircuitStatus status(String interfaceName) {
        var snapshot = store.read(interfaceName);
        var state = snapshot.state();
        var until = state.probeUntil() != null ? state.probeUntil() : state.openUntil();
        boolean open = until != null && snapshot.now().isBefore(until);
        return new CircuitStatus(interfaceName, open, state.failures(), threshold, open ? until : null);
    }
}
