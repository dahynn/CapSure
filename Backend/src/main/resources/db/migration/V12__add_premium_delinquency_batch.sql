-- Simulation only: no real notice delivery or legal grace-period guarantee.
CREATE TABLE public.ops_premium_delinquency_run (
    run_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instance_key VARCHAR(150) NOT NULL UNIQUE,
    business_date DATE NOT NULL,
    actor_user_id BIGINT REFERENCES public.usr_user(user_id),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'FAILED', 'COMPLETED')),
    error_reason VARCHAR(200),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ
);

CREATE TABLE public.ops_premium_delinquency_target (
    run_id BIGINT NOT NULL REFERENCES public.ops_premium_delinquency_run(run_id),
    policy_id BIGINT NOT NULL REFERENCES public.ins_policy(policy_id),
    outcome VARCHAR(30) CHECK (outcome IN ('CHANGED', 'UNCHANGED', 'NOTICE_FAILED')),
    processed_at TIMESTAMPTZ,
    PRIMARY KEY (run_id, policy_id)
);

CREATE TABLE public.ops_premium_delinquency_attempt (
    attempt_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES public.ops_premium_delinquency_run(run_id),
    actor_user_id BIGINT REFERENCES public.usr_user(user_id),
    reason VARCHAR(500) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE public.ins_premium_notice (
    premium_receivable_id BIGINT PRIMARY KEY REFERENCES public.ins_premium_receivable(premium_receivable_id),
    status VARCHAR(30) NOT NULL CHECK (status IN ('SIMULATED_DELIVERED', 'FAILED')),
    policy_version VARCHAR(50) NOT NULL,
    grace_days INTEGER NOT NULL CHECK (grace_days > 0),
    notified_on DATE,
    effective_grace_ends_on DATE,
    attempts INTEGER NOT NULL CHECK (attempts > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (status <> 'SIMULATED_DELIVERED' OR
           (notified_on IS NOT NULL AND effective_grace_ends_on >= notified_on + grace_days))
);

CREATE TABLE public.ins_policy_delinquency_history (
    history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES public.ins_policy(policy_id),
    run_id BIGINT REFERENCES public.ops_premium_delinquency_run(run_id),
    from_status VARCHAR(40) NOT NULL,
    to_status VARCHAR(40) NOT NULL,
    reason_code VARCHAR(60) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (from_status <> to_status)
);
CREATE INDEX ix_policy_delinquency_history ON public.ins_policy_delinquency_history(policy_id, history_id DESC);

CREATE TABLE public.pay_late_settlement_review (
    premium_settlement_id BIGINT PRIMARY KEY REFERENCES public.pay_premium_settlement(premium_settlement_id),
    policy_id BIGINT NOT NULL REFERENCES public.ins_policy(policy_id),
    reason_code VARCHAR(60) NOT NULL DEFAULT 'PAYMENT_AFTER_LAPSE',
    status VARCHAR(30) NOT NULL DEFAULT 'MANUAL_REVIEW' CHECK (status = 'MANUAL_REVIEW'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
