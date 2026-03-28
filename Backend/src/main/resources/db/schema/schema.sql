-- Integrated bootstrap schema for capsule-insurance-api.
-- Apply order:
--   1) schema.sql
--   2) seed.sql
--   3) product_source.sql

-- ---------------------------------------------------------------------------
-- Reset public operational tables
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS public.audit_event_log CASCADE;
DROP TABLE IF EXISTS public.audit_outbox_dlq CASCADE;
DROP TABLE IF EXISTS public.audit_outbox CASCADE;
DROP TABLE IF EXISTS public.comp_user_legal_event CASCADE;
DROP TABLE IF EXISTS public.sec_digital_seal CASCADE;
DROP TABLE IF EXISTS public.subscription_capsule_snapshot_item CASCADE;
DROP TABLE IF EXISTS public.subscription_capsule_snapshot CASCADE;
DROP TABLE IF EXISTS public.subscription_item CASCADE;
DROP TABLE IF EXISTS public.subscription CASCADE;
DROP TABLE IF EXISTS public.usr_payment_method CASCADE;
DROP TABLE IF EXISTS public.usr_onboarding_category CASCADE;
DROP TABLE IF EXISTS public.capsule_product CASCADE;
DROP TABLE IF EXISTS public.ref_coverage CASCADE;
DROP TABLE IF EXISTS public.myd_contract_coverage CASCADE;
DROP TABLE IF EXISTS public.myd_contract CASCADE;
DROP TABLE IF EXISTS public.usr_user CASCADE;

DROP FUNCTION IF EXISTS public.set_updated_at() CASCADE;

DROP TYPE IF EXISTS public.audit_target_type_enum CASCADE;
DROP TYPE IF EXISTS public.audit_event_type_enum CASCADE;
DROP TYPE IF EXISTS public.legal_event_result_enum CASCADE;
DROP TYPE IF EXISTS public.legal_event_type_enum CASCADE;
DROP TYPE IF EXISTS public.digital_seal_status_enum CASCADE;
DROP TYPE IF EXISTS public.product_sale_status_enum CASCADE;
DROP TYPE IF EXISTS public.recommendation_rule_code_enum CASCADE;
DROP TYPE IF EXISTS public.duplicate_rule_code_enum CASCADE;
DROP TYPE IF EXISTS public.compensation_type_enum CASCADE;
DROP TYPE IF EXISTS public.coverage_category_code_enum CASCADE;
DROP TYPE IF EXISTS public.subscription_item_status_enum CASCADE;
DROP TYPE IF EXISTS public.subscription_status_enum CASCADE;
DROP TYPE IF EXISTS public.plan_version_enum CASCADE;
DROP TYPE IF EXISTS public.contract_coverage_status_enum CASCADE;
DROP TYPE IF EXISTS public.business_type_enum CASCADE;
DROP TYPE IF EXISTS public.user_status_enum CASCADE;
DROP TYPE IF EXISTS public.gender_enum CASCADE;

-- ---------------------------------------------------------------------------
-- Reset insurance staging schema and keep product_source definitions intact
-- ---------------------------------------------------------------------------
DROP SCHEMA IF EXISTS insurance CASCADE;
CREATE SCHEMA insurance;

-- ---------------------------------------------------------------------------
-- Public enum types aligned to Java domain enums
-- ---------------------------------------------------------------------------
CREATE TYPE public.gender_enum AS ENUM ('M', 'F', 'UNKNOWN');
CREATE TYPE public.user_status_enum AS ENUM ('PENDING_ONBOARDING', 'ACTIVE', 'LOCKED', 'WITHDRAWN');
CREATE TYPE public.business_type_enum AS ENUM ('LIFE', 'NONLIFE', 'AUTO', 'GENERAL');
CREATE TYPE public.contract_coverage_status_enum AS ENUM ('NORMAL', 'ENDED', 'SUSPENDED');
CREATE TYPE public.plan_version_enum AS ENUM ('CURRENT', 'NEXT');
CREATE TYPE public.subscription_status_enum AS ENUM ('ACTIVE', 'PAUSED', 'CANCELLED');
CREATE TYPE public.subscription_item_status_enum AS ENUM (
    'ACTIVE',
    'PAUSED',
    'RESERVED_ADD',
    'RESERVED_REMOVE',
    'CANCELLED'
);
CREATE TYPE public.coverage_category_code_enum AS ENUM (
    'DEATH',
    'CANCER',
    'BRAIN_HEART',
    'ACTUAL_LOSS',
    'SURGERY',
    'ACCIDENT',
    'LIABILITY'
);
CREATE TYPE public.compensation_type_enum AS ENUM ('INDEMNITY', 'FIXED_BENEFIT', 'MIXED');
CREATE TYPE public.duplicate_rule_code_enum AS ENUM (
    'PROPORTIONAL_ONLY',
    'MULTI_PAY_ALLOWED',
    'CHECK_MANUALLY'
);
CREATE TYPE public.recommendation_rule_code_enum AS ENUM (
    'SUPPRESS_IF_EXISTS',
    'ALLOW_TOPUP',
    'MANUAL_REVIEW'
);
CREATE TYPE public.product_sale_status_enum AS ENUM ('AVAILABLE', 'HIDDEN', 'DISCONTINUED');
CREATE TYPE public.digital_seal_status_enum AS ENUM ('REGISTERED', 'EXPIRED', 'REVOKED');
CREATE TYPE public.legal_event_type_enum AS ENUM (
    'SERVICE_TERMS_AGREE',
    'PRODUCT_TERMS_AGREE',
    'PRICE_NOTICE_ACK',
    'EXPLANATION_ACK',
    'DIGITAL_SEAL_REAUTH'
);
CREATE TYPE public.legal_event_result_enum AS ENUM ('SUCCESS', 'FAIL');
CREATE TYPE public.audit_event_type_enum AS ENUM (
    'SUBSCRIPTION_CHANGED',
    'CONTRACT_CHANGED',
    'DIGITAL_SEAL_USED',
    'CONSENT_CAPTURED'
);
CREATE TYPE public.audit_target_type_enum AS ENUM (
    'SUBSCRIPTION',
    'SUBSCRIPTION_ITEM',
    'MYD_CONTRACT',
    'DIGITAL_SEAL',
    'LEGAL_EVENT'
);

-- ---------------------------------------------------------------------------
-- Shared timestamp trigger for public schema
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- auth
-- ---------------------------------------------------------------------------
CREATE TABLE usr_user (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(50) NOT NULL,
    password_encrypted VARCHAR(255) NULL,
    name VARCHAR(10) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    birth_date DATE NULL,
    gender VARCHAR(10) DEFAULT 'UNKNOWN' NULL,
    user_status VARCHAR(20) DEFAULT 'PENDING_ONBOARDING' NOT NULL,
    onboarding_completed_at TIMESTAMP NULL,
    withdrawn_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_usr_user_email UNIQUE (email)
);

CREATE INDEX ix_usr_user_status ON public.usr_user (user_status);

CREATE TRIGGER trg_usr_user_set_updated_at
BEFORE UPDATE ON public.usr_user
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.usr_onboarding_category (
    onboarding_category_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coverage_category_code public.coverage_category_code_enum NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_onboarding_category_user
        FOREIGN KEY (user_id) REFERENCES public.usr_user (user_id) ON DELETE CASCADE,
    CONSTRAINT uk_onboarding_category_user_category
        UNIQUE (user_id, coverage_category_code)
);

CREATE INDEX ix_onboarding_category_user
    ON public.usr_onboarding_category (user_id);

CREATE TABLE public.usr_payment_method (
    payment_method_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'TOSS',
    method_type VARCHAR(20) NOT NULL DEFAULT 'BANK_ACCOUNT',
    masked_number VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_method_user
        FOREIGN KEY (user_id) REFERENCES public.usr_user (user_id) ON DELETE CASCADE
);

CREATE INDEX ix_payment_method_user_active
    ON public.usr_payment_method (user_id, is_active);

CREATE TRIGGER trg_payment_method_set_updated_at
BEFORE UPDATE ON public.usr_payment_method
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ---------------------------------------------------------------------------
-- mydata
-- ---------------------------------------------------------------------------
CREATE TABLE public.myd_contract (
    myd_contract_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_code VARCHAR(100) NOT NULL DEFAULT 'CAPSULE-MOCK',
    insu_num VARCHAR(100) NOT NULL,
    is_consent BOOLEAN NOT NULL,
    business_type public.business_type_enum NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    insu_type_code VARCHAR(100),
    contract_status_code VARCHAR(100) NOT NULL,
    contract_date DATE,
    start_date DATE,
    end_date DATE,
    premium_amount NUMERIC(18, 2),
    currency_code VARCHAR(10),
    insured_list_json TEXT,
    prize_list_json TEXT,
    contract_list_json TEXT,
    policy_uri VARCHAR(512),
    extra_payload_json TEXT,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_myd_contract_user
        FOREIGN KEY (user_id) REFERENCES public.usr_user (user_id) ON DELETE CASCADE,
    CONSTRAINT uk_myd_contract_user_provider_insu
        UNIQUE (user_id, provider_code, insu_num),
    CONSTRAINT chk_myd_contract_premium_non_negative
        CHECK (premium_amount IS NULL OR premium_amount >= 0)
);

CREATE INDEX ix_myd_contract_user_id ON public.myd_contract (user_id);

CREATE TRIGGER trg_myd_contract_set_updated_at
BEFORE UPDATE ON public.myd_contract
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.myd_contract_coverage (
    myd_contract_coverage_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    myd_contract_id BIGINT NOT NULL,
    coverage_num VARCHAR(100) NOT NULL,
    coverage_name VARCHAR(255) NOT NULL,
    coverage_amount NUMERIC(18, 2),
    currency_code VARCHAR(10),
    coverage_status public.contract_coverage_status_enum NOT NULL DEFAULT 'NORMAL',
    start_date DATE,
    end_date DATE,
    coverage_code VARCHAR(100),
    extra_payload_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_myd_contract_coverage_contract
        FOREIGN KEY (myd_contract_id) REFERENCES public.myd_contract (myd_contract_id) ON DELETE CASCADE,
    CONSTRAINT uk_myd_contract_coverage_num
        UNIQUE (myd_contract_id, coverage_num),
    CONSTRAINT chk_myd_contract_coverage_amount_non_negative
        CHECK (coverage_amount IS NULL OR coverage_amount >= 0)
);

CREATE INDEX ix_myd_contract_coverage_contract_id ON public.myd_contract_coverage (myd_contract_id);

CREATE TRIGGER trg_myd_contract_coverage_set_updated_at
BEFORE UPDATE ON public.myd_contract_coverage
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ---------------------------------------------------------------------------
-- insurer catalog
-- ---------------------------------------------------------------------------
CREATE TABLE public.ref_coverage (
    coverage_code VARCHAR(100) PRIMARY KEY,
    coverage_category_code public.coverage_category_code_enum NOT NULL,
    coverage_name VARCHAR(255) NOT NULL,
    duplicate_group_code VARCHAR(100),
    description TEXT,
    search_keywords_json TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    compensation_type public.compensation_type_enum NOT NULL,
    duplicate_rule_code public.duplicate_rule_code_enum NOT NULL,
    recommendation_rule_code public.recommendation_rule_code_enum NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_ref_coverage_active ON public.ref_coverage (is_active);

CREATE TRIGGER trg_ref_coverage_set_updated_at
BEFORE UPDATE ON public.ref_coverage
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.capsule_product (
    capsule_product_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    capsule_code VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    coverage_category_code public.coverage_category_code_enum NOT NULL,
    coverage_code VARCHAR(100) NOT NULL,
    coverage_amount NUMERIC(18, 2) NOT NULL,
    coverage_unit VARCHAR(50) NOT NULL,
    monthly_price_male NUMERIC(18, 2) NOT NULL,
    monthly_price_female NUMERIC(18, 2) NOT NULL,
    min_retention_days INTEGER NOT NULL,
    sale_status public.product_sale_status_enum NOT NULL,
    is_duplicate_check_target BOOLEAN NOT NULL DEFAULT FALSE,
    terms_uri VARCHAR(512),
    terms_version VARCHAR(100),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_capsule_product_code UNIQUE (capsule_code),
    CONSTRAINT fk_capsule_product_coverage
        FOREIGN KEY (coverage_code) REFERENCES public.ref_coverage (coverage_code),
    CONSTRAINT chk_capsule_product_amount_non_negative
        CHECK (coverage_amount >= 0 AND monthly_price_male >= 0 AND monthly_price_female >= 0 AND min_retention_days >= 0)
);

CREATE INDEX ix_capsule_product_sale_status ON public.capsule_product (sale_status);
CREATE INDEX ix_capsule_product_coverage_code ON public.capsule_product (coverage_code);

CREATE TRIGGER trg_capsule_product_set_updated_at
BEFORE UPDATE ON public.capsule_product
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ---------------------------------------------------------------------------
-- subscription
-- ---------------------------------------------------------------------------
CREATE TABLE public.subscription (
    subscription_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    capsule_name VARCHAR(50) NOT NULL DEFAULT '나만의 캡슐',
    subscription_status public.subscription_status_enum NOT NULL,
    billing_anchor_day INTEGER,
    current_cycle_start_at TIMESTAMPTZ,
    current_cycle_end_at TIMESTAMPTZ,
    next_billing_at TIMESTAMPTZ,
    expected_next_amount NUMERIC(18, 2),
    paused_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_subscription_user
        FOREIGN KEY (user_id) REFERENCES public.usr_user (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_subscription_anchor_day
        CHECK (billing_anchor_day IS NULL OR billing_anchor_day BETWEEN 1 AND 31),
    CONSTRAINT chk_subscription_expected_amount_non_negative
        CHECK (expected_next_amount IS NULL OR expected_next_amount >= 0)
);

CREATE INDEX ix_subscription_status ON public.subscription (subscription_status);

CREATE TRIGGER trg_subscription_set_updated_at
BEFORE UPDATE ON public.subscription
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.subscription_item (
    subscription_item_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    capsule_product_id BIGINT NOT NULL,
    plan_version public.plan_version_enum NOT NULL,
    item_status public.subscription_item_status_enum NOT NULL,
    coverage_amount_snapshot NUMERIC(18, 2),
    monthly_price_snapshot NUMERIC(18, 2),
    effective_start_at TIMESTAMPTZ,
    effective_end_at TIMESTAMPTZ,
    editable_after_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_subscription_item_subscription
        FOREIGN KEY (subscription_id) REFERENCES public.subscription (subscription_id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_item_product
        FOREIGN KEY (capsule_product_id) REFERENCES public.capsule_product (capsule_product_id),
    CONSTRAINT uk_subscription_item_version
        UNIQUE (subscription_id, capsule_product_id, plan_version),
    CONSTRAINT chk_subscription_item_amount_non_negative
        CHECK (
            (coverage_amount_snapshot IS NULL OR coverage_amount_snapshot >= 0)
            AND (monthly_price_snapshot IS NULL OR monthly_price_snapshot >= 0)
        )
);

CREATE INDEX ix_subscription_item_subscription_plan
    ON public.subscription_item (subscription_id, plan_version);

CREATE TRIGGER trg_subscription_item_set_updated_at
BEFORE UPDATE ON public.subscription_item
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.subscription_capsule_snapshot (
    capsule_snapshot_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    capsule_name VARCHAR(50) NOT NULL,
    total_premium NUMERIC(18, 2) NOT NULL DEFAULT 0,
    cycle_started_at TIMESTAMPTZ,
    cycle_ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_snapshot_subscription
        FOREIGN KEY (subscription_id) REFERENCES public.subscription (subscription_id) ON DELETE CASCADE,
    CONSTRAINT fk_snapshot_user
        FOREIGN KEY (user_id) REFERENCES public.usr_user (user_id) ON DELETE CASCADE
);

CREATE INDEX ix_snapshot_user_created_at
    ON public.subscription_capsule_snapshot (user_id, created_at DESC);

CREATE TABLE public.subscription_capsule_snapshot_item (
    capsule_snapshot_item_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    capsule_snapshot_id BIGINT NOT NULL,
    product_source_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    coverage_category_code public.coverage_category_code_enum,
    monthly_price_snapshot NUMERIC(18, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_snapshot_item_snapshot
        FOREIGN KEY (capsule_snapshot_id) REFERENCES public.subscription_capsule_snapshot (capsule_snapshot_id) ON DELETE CASCADE
);

CREATE INDEX ix_snapshot_item_snapshot_id
    ON public.subscription_capsule_snapshot_item (capsule_snapshot_id);

-- ---------------------------------------------------------------------------
-- compliance
-- ---------------------------------------------------------------------------
CREATE TABLE public.sec_digital_seal (
    seal_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    seal_status public.digital_seal_status_enum NOT NULL,
    encrypted_token TEXT NOT NULL,
    key_version INTEGER NOT NULL DEFAULT 1,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_verified_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sec_digital_seal_user
        FOREIGN KEY (user_id) REFERENCES public.usr_user (user_id) ON DELETE CASCADE
);

CREATE INDEX ix_sec_digital_seal_user_status
    ON public.sec_digital_seal (user_id, seal_status, issued_at DESC);

CREATE TRIGGER trg_sec_digital_seal_set_updated_at
BEFORE UPDATE ON public.sec_digital_seal
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.comp_user_legal_event (
    legal_event_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT,
    seal_id BIGINT,
    event_type public.legal_event_type_enum NOT NULL,
    event_result public.legal_event_result_enum NOT NULL,
    event_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    terms_version VARCHAR(100),
    evidence_json TEXT,
    request_id VARCHAR(100),
    session_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_comp_user_legal_event_user
        FOREIGN KEY (user_id) REFERENCES public.usr_user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_comp_user_legal_event_subscription
        FOREIGN KEY (subscription_id) REFERENCES public.subscription (subscription_id) ON DELETE SET NULL,
    CONSTRAINT fk_comp_user_legal_event_seal
        FOREIGN KEY (seal_id) REFERENCES public.sec_digital_seal (seal_id) ON DELETE SET NULL
);

CREATE INDEX ix_comp_user_legal_event_user_id
    ON public.comp_user_legal_event (user_id, legal_event_id DESC);

-- ---------------------------------------------------------------------------
-- audit
-- ---------------------------------------------------------------------------
CREATE TABLE public.audit_event_log (
    audit_event_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    event_type public.audit_event_type_enum NOT NULL,
    actor_user_id BIGINT,
    target_type public.audit_target_type_enum NOT NULL,
    target_id BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_id VARCHAR(100),
    before_json TEXT,
    after_json TEXT,
    integrity_hash VARCHAR(255),
    prev_hash VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_event_log_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES public.usr_user (user_id) ON DELETE SET NULL
);

CREATE INDEX ix_audit_event_log_actor_user_id
    ON public.audit_event_log (actor_user_id, audit_event_id DESC);
CREATE INDEX ix_audit_event_log_target
    ON public.audit_event_log (target_type, target_id, audit_event_id DESC);

CREATE TABLE public.audit_outbox (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_audit_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX ix_audit_outbox_status_id
    ON public.audit_outbox (status, id);

CREATE TRIGGER trg_audit_outbox_set_updated_at
BEFORE UPDATE ON public.audit_outbox
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.audit_outbox_dlq (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    error_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_audit_outbox_dlq_event_id
    ON public.audit_outbox_dlq (event_id, id DESC);

-- ---------------------------------------------------------------------------
-- insurance schema kept for product_source staging
-- ---------------------------------------------------------------------------
CREATE TYPE insurance.insurer_sector AS ENUM ('LIFE', 'NONLIFE');
CREATE TYPE insurance.coverage_category_code AS ENUM (
    'DEATH', 'CANCER', 'BRAIN_HEART', 'ACTUAL_LOSS', 'SURGERY', 'ACCIDENT', 'LIABILITY', 'ETC'
);
CREATE TYPE insurance.mapping_status AS ENUM ('UNMAPPED', 'AUTO_MAPPED', 'REVIEWED');

CREATE OR REPLACE FUNCTION insurance.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION insurance.append_variant_parts(
    base_parts TEXT[],
    raw_content TEXT
)
RETURNS TEXT[]
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    result TEXT[] := COALESCE(base_parts, ARRAY[]::TEXT[]);
    candidate TEXT;
BEGIN
    IF raw_content IS NULL THEN
        RETURN result;
    END IF;

    FOR candidate IN
        SELECT btrim(regexp_replace(value, '\s+', ' ', 'g'))
        FROM regexp_split_to_table(raw_content, '\s*,\s*') AS value
    LOOP
        IF candidate IS NOT NULL
           AND candidate <> ''
           AND NOT candidate = ANY(result) THEN
            result := array_append(result, candidate);
        END IF;
    END LOOP;

    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION insurance.is_product_option_content(content TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    normalized TEXT := regexp_replace(COALESCE(content, ''), '\s+', '', 'g');
BEGIN
    RETURN normalized ~ '(?:\d+종|\d+형|\d+년갱신형|갱신형|비갱신형|세만기|연만기|간편심사형|표준형|일반가입형|일반고지|해약환급금|납입면제|기본형|순수보장형|건강관리형|생활약속|시니어보장형|3\.5\.5|3\.N\.5)';
END;
$$;

CREATE OR REPLACE FUNCTION insurance.extract_product_name_parts(original_name TEXT)
RETURNS TABLE (
    display_name TEXT,
    variant_summary TEXT
)
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    working_name TEXT := COALESCE(original_name, '');
    variant_parts TEXT[] := ARRAY[]::TEXT[];
    trailing_code_match TEXT[];
    nested_option_match TEXT[];
    chunk TEXT;
    inner_text TEXT;
    standalone_option TEXT;
    did_replace BOOLEAN;
BEGIN
    working_name := replace(working_name, '_', ' ');
    working_name := regexp_replace(working_name, '\s+', ' ', 'g');
    working_name := btrim(working_name);

    SELECT regexp_match(
        working_name,
        '((?:(?:Hi)?\d{4}(?:\.\d+)?|\d{2}\.\d{2}))\s*$'
    )
    INTO trailing_code_match;

    IF trailing_code_match IS NOT NULL THEN
        variant_parts := insurance.append_variant_parts(variant_parts, trailing_code_match[1]);
        working_name := regexp_replace(
            working_name,
            '\s*(?:(?:Hi)?\d{4}(?:\.\d+)?|\d{2}\.\d{2})\s*$',
            ' '
        );
    END IF;

    LOOP
        nested_option_match := NULL;
        SELECT regexp_match(working_name, '\(([^()]+)\(([^()]+)\)\)')
        INTO nested_option_match;

        EXIT WHEN nested_option_match IS NULL;

        IF insurance.is_product_option_content(nested_option_match[1])
           OR insurance.is_product_option_content(
               btrim(nested_option_match[1]) || '(' || btrim(nested_option_match[2]) || ')'
           ) THEN
            variant_parts := insurance.append_variant_parts(
                variant_parts,
                btrim(nested_option_match[1]) || '(' || btrim(nested_option_match[2]) || ')'
            );
            working_name := replace(
                working_name,
                '(' || nested_option_match[1] || '(' || nested_option_match[2] || '))',
                ' '
            );
        ELSE
            EXIT;
        END IF;
    END LOOP;

    LOOP
        did_replace := FALSE;

        FOR chunk IN
            SELECT (regexp_matches(working_name, '\([^()]*\)|\[[^\[\]]*\]', 'g'))[1]
        LOOP
            inner_text := btrim(substr(chunk, 2, length(chunk) - 2));

            IF insurance.is_product_option_content(inner_text) THEN
                variant_parts := insurance.append_variant_parts(variant_parts, inner_text);
                working_name := replace(working_name, chunk, ' ');
                did_replace := TRUE;
            END IF;
        END LOOP;

        EXIT WHEN NOT did_replace;
    END LOOP;

    FOR standalone_option IN
        SELECT (regexp_matches(
            working_name,
            '(^|[[:space:]])((?:\d+종|\d+형|갱신형|비갱신형|세만기|연만기|표준형|기본형|순수보장형|일반가입형|일반고지|건강관리형|생활약속|시니어보장형|납입면제형))(?:[[:space:]]|$)',
            'g'
        ))[2]
    LOOP
        variant_parts := insurance.append_variant_parts(variant_parts, standalone_option);
    END LOOP;

    working_name := regexp_replace(
        working_name,
        '(^|[[:space:]])(?:\d+종|\d+형|갱신형|비갱신형|세만기|연만기|표준형|기본형|순수보장형|일반가입형|일반고지|건강관리형|생활약속|시니어보장형|납입면제형)(?:[[:space:]]|$)',
        ' ',
        'g'
    );
    working_name := regexp_replace(working_name, '\(\s*\)|\[\s*\]', ' ', 'g');
    working_name := regexp_replace(working_name, '\s+', ' ', 'g');
    working_name := regexp_replace(working_name, '\s+\)', ')', 'g');
    working_name := regexp_replace(working_name, '\(\s+', '(', 'g');
    working_name := btrim(working_name);

    IF working_name = '' THEN
        working_name := COALESCE(original_name, '');
    END IF;

    display_name := working_name;
    variant_summary := CASE
        WHEN array_length(variant_parts, 1) IS NULL THEN NULL
        ELSE array_to_string(variant_parts, ' / ')
    END;

    RETURN NEXT;
END;
$$;

CREATE TABLE insurance.product_source (
    product_source_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_file_name TEXT NOT NULL,
    source_row_no INTEGER NOT NULL,
    insurer_sector insurance.insurer_sector NOT NULL,
    company_name TEXT NOT NULL,
    product_name TEXT NOT NULL,
    display_name TEXT,
    variant_summary TEXT,
    sale_channel TEXT,
    contract_type TEXT,
    coverage_name TEXT,
    claim_reason TEXT,
    payout_amount TEXT,
    join_amount TEXT,
    minimum_join_premium TEXT,
    premium_male TEXT,
    premium_female TEXT,
    payment_cycle TEXT,
    payment_term TEXT,
    coverage_term TEXT,
    monthly_premium_male NUMERIC(14, 2),
    monthly_premium_female NUMERIC(14, 2),
    fixed_rate TEXT,
    current_announced_rate TEXT,
    minimum_guaranteed_rate TEXT,
    coverage_part_interest_rate TEXT,
    reserve_part_interest_rate TEXT,
    price_index_male TEXT,
    price_index_female TEXT,
    extra_premium_index_male TEXT,
    extra_premium_index_female TEXT,
    extra_premium_index TEXT,
    contract_cost_index_male TEXT,
    contract_cost_index_female TEXT,
    contract_cost_index TEXT,
    coverage_scope_index_name TEXT,
    coverage_scope_index_value TEXT,
    coverage_scope_index_cancer_diagnosis TEXT,
    coverage_scope_index_cancer_hospitalization TEXT,
    expected_renewal_premium TEXT,
    product_summary TEXT,
    product_feature TEXT,
    ai_summary_json JSON,
    surrender_value TEXT,
    minimum_death_benefit TEXT,
    minimum_death_benefit_method TEXT,
    minimum_surrender_value TEXT,
    minimum_surrender_value_method TEXT,
    mild_dementia_covered TEXT,
    mild_dementia_benefit_amount TEXT,
    product_subtype TEXT,
    renewal TEXT,
    universal TEXT,
    special_note TEXT,
    contact_phone TEXT,
    sale_date DATE,
    coverage_category_code insurance.coverage_category_code,
    coverage_code TEXT,
    avg_price NUMERIC(14, 2),
    normalized_avg_price INTEGER,
    mapping_status insurance.mapping_status NOT NULL DEFAULT 'UNMAPPED',
    manual_note TEXT,
    raw_row_jsonb JSONB,
    loaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (source_file_name, source_row_no),
    CONSTRAINT chk_product_source_company_name_not_blank
        CHECK (
            btrim(company_name) <> ''
            AND lower(btrim(company_name)) NOT IN ('none', 'null', 'nan')
        ),
    CONSTRAINT chk_product_source_product_name_not_blank
        CHECK (
            btrim(product_name) <> ''
            AND lower(btrim(product_name)) NOT IN ('none', 'null', 'nan')
        )
);

CREATE INDEX ix_product_source_company_product
    ON insurance.product_source (company_name, product_name);
CREATE INDEX ix_product_source_sector
    ON insurance.product_source (insurer_sector);
CREATE INDEX ix_product_source_source_file
    ON insurance.product_source (source_file_name);
CREATE INDEX ix_product_source_raw_row_jsonb
    ON insurance.product_source USING GIN (raw_row_jsonb);

CREATE TRIGGER trg_product_source_set_updated_at
BEFORE UPDATE ON insurance.product_source
FOR EACH ROW EXECUTE FUNCTION insurance.set_updated_at();
