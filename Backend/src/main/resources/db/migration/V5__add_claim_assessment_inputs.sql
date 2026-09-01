ALTER TABLE public.clm_claim
    ADD COLUMN diagnosis_category VARCHAR(80) NOT NULL,
    ADD COLUMN submission_idempotency_key VARCHAR(150);
