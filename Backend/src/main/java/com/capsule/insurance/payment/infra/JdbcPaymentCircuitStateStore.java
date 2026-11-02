package com.capsule.insurance.payment.infra;

import com.capsule.insurance.payment.application.port.PaymentCircuitStateStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcPaymentCircuitStateStore implements PaymentCircuitStateStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public JdbcPaymentCircuitStateStore(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
    }
    @Override
    public Snapshot read(String name) {
        return jdbc.queryForObject("""
                SELECT COALESCE(s.failure_count, 0) AS failures, s.open_until, s.probe_until,
                    COALESCE(s.generation, 0) AS generation, clock_timestamp() AS db_now
                FROM (SELECT 1) seed LEFT JOIN ifc_payment_circuit_state s ON s.interface_name = ?
                """, (rs, index) -> new Snapshot(new State(rs.getInt("failures"),
                        instant(rs.getTimestamp("open_until")), instant(rs.getTimestamp("probe_until")),
                        rs.getLong("generation")), rs.getTimestamp("db_now").toInstant()), name);
    }
    @Override
    public <T> T update(String name, BiFunction<State, Instant, Change<T>> transition) {
        return tx.execute(transaction -> {
            jdbc.update("INSERT INTO ifc_payment_circuit_state(interface_name) VALUES (?) ON CONFLICT DO NOTHING", name);
            var row = Objects.requireNonNull(jdbc.queryForObject("""
                    SELECT *, clock_timestamp() AS db_now FROM ifc_payment_circuit_state
                    WHERE interface_name = ? FOR UPDATE
                    """, (rs, index) -> new Row(new State(rs.getInt("failure_count"),
                            instant(rs.getTimestamp("open_until")), instant(rs.getTimestamp("probe_until")),
                            rs.getLong("generation")), rs.getTimestamp("db_now").toInstant()), name));
            Change<T> change = transition.apply(row.state(), row.now());
            if (!change.state().equals(row.state())) {
                State next = change.state();
                jdbc.update("""
                        UPDATE ifc_payment_circuit_state SET failure_count = ?, open_until = ?, probe_until = ?, generation = ?
                        WHERE interface_name = ?
                        """, next.failures(), timestamp(next.openUntil()), timestamp(next.probeUntil()), next.generation(), name);
            }
            return change.result();
        });
    }
    private record Row(State state, Instant now) {}
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
}
