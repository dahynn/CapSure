-- 화면 재확인을 위한 구조화 초안만 보관한다. 프롬프트·고객 원문·모델 원문은 넣지 않는다.
CREATE TABLE public.ops_claim_copilot_draft_snapshot (
    claim_id BIGINT NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    terms_to_check_json JSONB NOT NULL,
    missing_evidence_json JSONB NOT NULL,
    additional_questions_json JSONB NOT NULL,
    evidence_insufficient BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ops_claim_copilot_draft_snapshot PRIMARY KEY (claim_id, request_id),
    CONSTRAINT fk_ops_claim_copilot_draft_snapshot_review
        FOREIGN KEY (claim_id, request_id)
        REFERENCES public.ops_claim_copilot_review (claim_id, request_id)
        ON DELETE CASCADE
);
