package com.capsule.insurance.payment.adapter;

import com.capsule.insurance.payment.application.port.PaymentCircuitStateStore;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

// Standalone deterministic test adapter. Runtime Spring wiring always uses JDBC.
public class InMemoryPaymentCircuitStateStore implements PaymentCircuitStateStore {
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;
    public InMemoryPaymentCircuitStateStore(Clock clock) { this.clock = clock; }
    @Override
    public Snapshot read(String name) {
        return new Snapshot(states.getOrDefault(name, State.closed()), Instant.now(clock));
    }
    @Override
    public <T> T update(String name, BiFunction<State, Instant, Change<T>> transition) {
        AtomicReference<T> result = new AtomicReference<>();
        states.compute(name, (key, existing) -> {
            Change<T> change = transition.apply(existing == null ? State.closed() : existing, Instant.now(clock));
            result.set(change.result());
            return change.state();
        });
        return result.get();
    }
}
