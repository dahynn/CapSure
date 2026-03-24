-- ===========================================================================
-- 1. 보험 데이터 전용 스키마 및 공통 타입 (insurance)
-- ===========================================================================

DROP SCHEMA IF EXISTS insurance CASCADE;
CREATE SCHEMA insurance;

-- Enum types
DO $$ BEGIN
    CREATE TYPE insurance.insurer_sector AS ENUM ('LIFE', 'NONLIFE');
EXCEPTION WHEN DUPLICATE_OBJECT THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE insurance.coverage_category_code AS ENUM (
        'DEATH', 'CANCER', 'BRAIN_HEART', 'ACTUAL_LOSS', 'SURGERY', 'ACCIDENT', 'LIABILITY'
    );
EXCEPTION WHEN DUPLICATE_OBJECT THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE insurance.mapping_status AS ENUM ('UNMAPPED', 'AUTO_MAPPED', 'REVIEWED');
EXCEPTION WHEN DUPLICATE_OBJECT THEN NULL;
END $$;

-- Common helper
CREATE OR REPLACE FUNCTION insurance.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- 통합 상품 Data Staging (생명보함 + 손해보험 공통)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS insurance.product_source (
    product_source_id                            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_file_name                             TEXT NOT NULL,
    source_row_no                                INTEGER NOT NULL,
    insurer_sector                               insurance.insurer_sector NOT NULL,
    company_name                                 TEXT NOT NULL,
    product_name                                 TEXT NOT NULL,
    sale_channel                                 TEXT,
    
    contract_type                           TEXT,
    coverage_name                                TEXT,
    claim_reason                            TEXT,
    payout_amount                           TEXT,
    join_amount                             TEXT,
    minimum_join_premium                    TEXT,
    
    premium_male                            TEXT,
    premium_female                          TEXT,
    
    fixed_rate                              TEXT,
    current_announced_rate                  TEXT,
    minimum_guaranteed_rate                 TEXT,
    coverage_part_interest_rate             TEXT,
    reserve_part_interest_rate              TEXT,
    
    price_index_male                        TEXT,
    price_index_female                      TEXT,
    
    extra_premium_index_male                TEXT,
    extra_premium_index_female              TEXT,
    extra_premium_index                     TEXT,
    
    contract_cost_index_male                TEXT,
    contract_cost_index_female              TEXT,
    contract_cost_index                     TEXT,
    
    coverage_scope_index_name               TEXT,
    coverage_scope_index_value              TEXT,
    coverage_scope_index_cancer_diagnosis   TEXT,
    coverage_scope_index_cancer_hospitalization TEXT,
    
    expected_renewal_premium                TEXT,
    product_summary                         TEXT,
    product_feature                         TEXT,
    
    surrender_value                         TEXT,
    minimum_death_benefit                   TEXT,
    minimum_death_benefit_method            TEXT,
    minimum_surrender_value                 TEXT,
    minimum_surrender_value_method          TEXT,
    mild_dementia_covered                   TEXT,
    mild_dementia_benefit_amount            TEXT,
    
    product_subtype                         TEXT,
    renewal                                 TEXT,
    universal                               TEXT,
    special_note                                 TEXT,
    contact_phone                                TEXT,
    sale_date                                    DATE,
    
    coverage_category_code                       insurance.coverage_category_code,
    coverage_code                                TEXT,
    mapping_status                               insurance.mapping_status NOT NULL DEFAULT 'UNMAPPED',
    manual_note                                  TEXT,
    raw_row_jsonb                                JSONB,
    loaded_at                                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
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

COMMENT ON TABLE insurance.product_source IS '생명보함 및 손해보험 Excel 정규화 통합 staging';
COMMENT ON COLUMN insurance.product_source.source_file_name IS '원천 파일명';
COMMENT ON COLUMN insurance.product_source.source_row_no IS '헤더를 제외한 데이터 row 순번';
COMMENT ON COLUMN insurance.product_source.insurer_sector IS '구분 (LIFE, NONLIFE)';
COMMENT ON COLUMN insurance.product_source.coverage_name IS '급부명칭/담보명';
COMMENT ON COLUMN insurance.product_source.raw_row_jsonb IS '원천 row 전체 JSON';

CREATE INDEX IF NOT EXISTS ix_product_source_company_product ON insurance.product_source (company_name, product_name);
CREATE INDEX IF NOT EXISTS ix_product_source_sector ON insurance.product_source (insurer_sector);
CREATE INDEX IF NOT EXISTS ix_product_source_source_file ON insurance.product_source (source_file_name);
CREATE INDEX IF NOT EXISTS ix_product_source_raw_row_jsonb ON insurance.product_source USING GIN (raw_row_jsonb);

DROP TRIGGER IF EXISTS trg_product_source_set_updated_at ON insurance.product_source;
CREATE TRIGGER trg_product_source_set_updated_at
BEFORE UPDATE ON insurance.product_source
FOR EACH ROW EXECUTE FUNCTION insurance.set_updated_at();

-- ---------------------------------------------------------------------------
-- 보험료 조회용 테이블
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS insurance.product_premium_rate (
    premium_rate_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    product_source_id         BIGINT REFERENCES insurance.product_source(product_source_id) ON DELETE CASCADE,
    source_file_name          TEXT NOT NULL,
    source_row_no             INTEGER,

    insurer_sector            insurance.insurer_sector NOT NULL,
    company_name              TEXT NOT NULL,
    product_name              TEXT NOT NULL,

    product_variant_name      TEXT,
    plan_name                 TEXT,
    coverage_name             TEXT,
    sale_channel              TEXT,

    gender                    VARCHAR(1) NOT NULL
                                  CHECK (gender IN ('M', 'F', 'U')),
    age                       INTEGER NOT NULL
                                  CHECK (age >= 0 AND age <= 120),
    age_band                  TEXT,

    premium_amount            NUMERIC(14,2) NOT NULL,
    premium_currency          VARCHAR(3) NOT NULL DEFAULT 'KRW',

    payment_cycle             TEXT,
    payment_term              TEXT,
    coverage_term             TEXT,
    renewal              TEXT,

    join_amount          TEXT,
    conditions_jsonb          JSONB,
    raw_row_jsonb             JSONB,

    loaded_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_product_premium_company_name_not_blank
        CHECK (
            btrim(company_name) <> ''
            AND lower(btrim(company_name)) NOT IN ('none', 'null', 'nan')
        ),

    CONSTRAINT chk_product_premium_product_name_not_blank
        CHECK (
            btrim(product_name) <> ''
            AND lower(btrim(product_name)) NOT IN ('none', 'null', 'nan')
        ),

    CONSTRAINT chk_product_premium_source_file_name_not_blank
        CHECK (
            btrim(source_file_name) <> ''
            AND lower(btrim(source_file_name)) NOT IN ('none', 'null', 'nan')
        )

);

COMMENT ON TABLE insurance.product_premium_rate IS '보험사/상품/조건별 보험료 조회용 테이블';
COMMENT ON COLUMN insurance.product_premium_rate.source_file_name IS '원천 파일명';
COMMENT ON COLUMN insurance.product_premium_rate.source_row_no IS '원천 row 번호';
COMMENT ON COLUMN insurance.product_premium_rate.insurer_sector IS '생보/손보 구분';
COMMENT ON COLUMN insurance.product_premium_rate.company_name IS '보험사명';
COMMENT ON COLUMN insurance.product_premium_rate.product_name IS '상품명';
COMMENT ON COLUMN insurance.product_premium_rate.product_variant_name IS '상품 변형명(예: 기본형, 체증형)';
COMMENT ON COLUMN insurance.product_premium_rate.plan_name IS '플랜/종 구분(예: 1종, 2종)';
COMMENT ON COLUMN insurance.product_premium_rate.coverage_name IS '특정 담보 기준 보험료일 경우 담보명';
COMMENT ON COLUMN insurance.product_premium_rate.sale_channel IS '판매채널';
COMMENT ON COLUMN insurance.product_premium_rate.gender IS 'M/F/U';
COMMENT ON COLUMN insurance.product_premium_rate.age IS '만 나이';
COMMENT ON COLUMN insurance.product_premium_rate.age_band IS '연령대(예: 20대, 30대)';
COMMENT ON COLUMN insurance.product_premium_rate.premium_amount IS '보험료 금액';
COMMENT ON COLUMN insurance.product_premium_rate.premium_currency IS '통화코드';
COMMENT ON COLUMN insurance.product_premium_rate.payment_cycle IS '납입주기(월납, 연납)';
COMMENT ON COLUMN insurance.product_premium_rate.payment_term IS '납입기간(10년납, 20년납)';
COMMENT ON COLUMN insurance.product_premium_rate.coverage_term IS '보장기간(80세만기, 종신)';
COMMENT ON COLUMN insurance.product_premium_rate.renewal IS '갱신 관련 원천 문자열';
COMMENT ON COLUMN insurance.product_premium_rate.join_amount IS '가입금액 원문';
COMMENT ON COLUMN insurance.product_premium_rate.conditions_jsonb IS '흡연 여부, 특약 여부 등 추가 조건 JSON';
COMMENT ON COLUMN insurance.product_premium_rate.raw_row_jsonb IS '원천 row 전체 JSON';

CREATE INDEX IF NOT EXISTS ix_product_premium_rate_company_product
    ON insurance.product_premium_rate (company_name, product_name);

CREATE UNIQUE INDEX IF NOT EXISTS uix_product_premium_rate_unique
    ON insurance.product_premium_rate (
        insurer_sector,
        company_name,
        product_name,
        COALESCE(product_variant_name, ''),
        COALESCE(plan_name, ''),
        COALESCE(coverage_name, ''),
        gender,
        age,
        COALESCE(payment_cycle, ''),
        COALESCE(payment_term, ''),
        COALESCE(coverage_term, '')
    );

CREATE INDEX IF NOT EXISTS ix_product_premium_rate_lookup
    ON insurance.product_premium_rate (company_name, product_name, gender, age);

CREATE INDEX IF NOT EXISTS ix_product_premium_rate_age_band
    ON insurance.product_premium_rate (age_band);

CREATE INDEX IF NOT EXISTS ix_product_premium_rate_source_file
    ON insurance.product_premium_rate (source_file_name);

CREATE INDEX IF NOT EXISTS ix_product_premium_rate_conditions_jsonb
    ON insurance.product_premium_rate USING GIN (conditions_jsonb);

CREATE INDEX IF NOT EXISTS ix_product_premium_rate_raw_row_jsonb
    ON insurance.product_premium_rate USING GIN (raw_row_jsonb);

DROP TRIGGER IF EXISTS trg_product_premium_rate_set_updated_at
    ON insurance.product_premium_rate;

CREATE TRIGGER trg_product_premium_rate_set_updated_at
BEFORE UPDATE ON insurance.product_premium_rate
FOR EACH ROW EXECUTE FUNCTION insurance.set_updated_at();

