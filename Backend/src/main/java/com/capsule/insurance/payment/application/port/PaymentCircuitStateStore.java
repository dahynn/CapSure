package com.capsule.insurance.payment.application.port;

import java.time.Instant;
import java.util.function.BiFunction;

public interface PaymentCircuitStateStore {
    // The transition must be atomic; no provider/network calls are allowed inside it.
    <T> T update(String interfaceName, BiFunction<State, Instant, Change<T>> transition);
    Snapshot read(String interfaceName);
    record Snapshot(State state, Instant now) {}
    record State(int failures, Instant openUntil, Instant probeUntil, long generation) {
        public static State closed() { return new State(0, null, null, 0); }
    }
    record Change<T>(State state, T result) {}
}
