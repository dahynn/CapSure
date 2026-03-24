DROP SCHEMA IF EXISTS insurance CASCADE;
CREATE SCHEMA insurance;

-- Enum types
DO $$ BEGIN
    CREATE TYPE insurance.insurer_sector AS ENUM ('LIFE', 'NONLIFE');
EXCEPTION WHEN DUPLICATE_OBJECT THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE insurance.coverage_category_code AS ENUM (
        'DEATH', 'CANCER', 'BRAIN_HEART', 'ACTUAL_LOSS', 'SURGERY', 'ACCIDENT', 'LIABILITY', 'ETC'
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

    payment_cycle                           TEXT,
    payment_term                            TEXT,
    coverage_term                           TEXT,
    monthly_premium_male                    NUMERIC(14,2),
    monthly_premium_female                  NUMERIC(14,2),
    
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



