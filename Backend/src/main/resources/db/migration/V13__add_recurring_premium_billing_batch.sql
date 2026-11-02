-- Monthly premium receivable generation with resumable execution and immutable targets.
CREATE TABLE public.ops_premium_billing_run (
    run_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instance_key VARCHAR(150) NOT NULL UNIQUE,
    billing_cycle DATE NOT NULL,
    business_date DATE NOT NULL,
    actor_user_id BIGINT REFERENCES public.usr_user(user_id),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'FAILED', 'COMPLETED')),
    error_reason VARCHAR(200),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    CHECK (billing_cycle = DATE_TRUNC('month', billing_cycle)::DATE)
);

CREATE TABLE public.ops_premium_billing_target (
    run_id BIGINT NOT NULL REFERENCES public.ops_premium_billing_run(run_id),
    policy_id BIGINT NOT NULL REFERENCES public.ins_policy(policy_id),
    amount_due NUMERIC(18, 2) NOT NULL CHECK (amount_due > 0),
    currency_code CHAR(3) NOT NULL,
    due_date DATE NOT NULL,
    grace_ends_on DATE NOT NULL,
    outcome VARCHAR(30) CHECK (outcome IN ('CREATED', 'EXISTING', 'INELIGIBLE')),
    premium_receivable_id BIGINT REFERENCES public.ins_premium_receivable(premium_receivable_id),
    processed_at TIMESTAMPTZ,
    PRIMARY KEY (run_id, policy_id),
    CHECK (grace_ends_on >= due_date),
    CHECK ((outcome IS NULL AND processed_at IS NULL)
        OR (outcome IS NOT NULL AND processed_at IS NOT NULL))
);

CREATE TABLE public.ops_premium_billing_attempt (
    attempt_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES public.ops_premium_billing_run(run_id),
    actor_user_id BIGINT REFERENCES public.usr_user(user_id),
    reason VARCHAR(500) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_ops_premium_billing_target_pending
    ON public.ops_premium_billing_target (run_id, policy_id)
    WHERE outcome IS NULL;
