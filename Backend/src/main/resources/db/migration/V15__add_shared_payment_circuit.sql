CREATE TABLE ifc_payment_circuit_state (
    interface_name VARCHAR(100) PRIMARY KEY,
    failure_count INTEGER NOT NULL DEFAULT 0 CHECK (failure_count >= 0),
    open_until TIMESTAMPTZ,
    probe_until TIMESTAMPTZ,
    generation BIGINT NOT NULL DEFAULT 0
);
