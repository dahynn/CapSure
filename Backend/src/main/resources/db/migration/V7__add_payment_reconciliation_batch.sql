-- 결제 미확정 건을 다중 작업자가 안전하게 선점하고 재시작할 수 있도록 운영 메타데이터를 확장한다.

ALTER TABLE public.pay_order
    ADD COLUMN reconciliation_attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN reconciliation_available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN reconciliation_locked_at TIMESTAMPTZ,
    ADD COLUMN reconciliation_locked_by VARCHAR(100),
    ADD CONSTRAINT chk_pay_order_reconciliation_attempt_count
        CHECK (reconciliation_attempt_count >= 0),
    ADD CONSTRAINT chk_pay_order_reconciliation_lock_pair
        CHECK (
            (reconciliation_locked_at IS NULL AND reconciliation_locked_by IS NULL)
            OR
            (reconciliation_locked_at IS NOT NULL AND reconciliation_locked_by IS NOT NULL)
        );

UPDATE public.pay_order
SET reconciliation_available_at = updated_at;

ALTER TABLE public.ops_job_execution
    ADD COLUMN processed_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN resolved_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN still_unknown_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN failed_count BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_ops_job_reconciliation_counts CHECK (
        processed_count >= 0
        AND resolved_count >= 0
        AND still_unknown_count >= 0
        AND failed_count >= 0
    );

DROP INDEX IF EXISTS public.ix_pay_order_reconciliation;

CREATE INDEX ix_pay_order_reconciliation
    ON public.pay_order (reconciliation_available_at, payment_order_id)
    WHERE status IN ('APPROVING', 'UNKNOWN');

CREATE INDEX ix_pay_order_reconciliation_lock
    ON public.pay_order (reconciliation_locked_at, payment_order_id)
    WHERE reconciliation_locked_at IS NOT NULL;
