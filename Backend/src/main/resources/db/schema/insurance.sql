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
-- Source manifest
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS insurance.source_file (
    source_file_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name             VARCHAR(255) NOT NULL UNIQUE,
    insurer_sector        insurance.insurer_sector NOT NULL,
    parse_format          VARCHAR(50) NOT NULL CHECK (parse_format IN ('HTML_TABLE', 'XLS')),
    header_row_from       INTEGER NOT NULL,
    header_row_to         INTEGER NOT NULL,
    data_row_from         INTEGER NOT NULL,
    row_count             INTEGER,
    column_count          INTEGER,
    notes                 TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE insurance.source_file IS '원천 Excel 파일 manifest';
COMMENT ON COLUMN insurance.source_file.file_name IS '원천 파일명';
COMMENT ON COLUMN insurance.source_file.insurer_sector IS '생명/손해 구분';
COMMENT ON COLUMN insurance.source_file.parse_format IS 'HTML로 저장된 xls인지, 실제 xls인지';
COMMENT ON COLUMN insurance.source_file.header_row_from IS '원천 파일 기준 헤더 시작 row (0-base)';
COMMENT ON COLUMN insurance.source_file.header_row_to IS '원천 파일 기준 헤더 종료 row (0-base)';
COMMENT ON COLUMN insurance.source_file.data_row_from IS '원천 파일 기준 데이터 시작 row (0-base)';
COMMENT ON COLUMN insurance.source_file.row_count IS '정규화 후 데이터 row 수';
COMMENT ON COLUMN insurance.source_file.column_count IS '정규화 후 컬럼 수';

INSERT INTO insurance.source_file (
    file_name, insurer_sector, parse_format, header_row_from, header_row_to, data_row_from, row_count, column_count
) VALUES
    ('생명-변액보장성보험-CI보험.xls', 'LIFE', 'HTML_TABLE', 0, 2, 3, 9, 23),
    ('생명-변액보장성보험-정기보험.xls', 'LIFE', 'HTML_TABLE', 0, 2, 3, 4, 23),
    ('생명-변액보장성보험-종신보험.xls', 'LIFE', 'HTML_TABLE', 0, 2, 3, 34, 23),
    ('생명-보장성보험-CI보험15.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 96, 29),
    ('생명-보장성보험-간병치매보험27.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 142, 27),
    ('생명-보장성보험-상해보험96.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 418, 26),
    ('생명-보장성보험-암보험98.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 623, 27),
    ('생명-보장성보험-정기보험185.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 741, 30),
    ('생명-보장성보험-종신보험167.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 697, 30),
    ('생명-보장성보험-질병보험126.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 682, 26),
    ('생명-실손의료보험-실손의료보험17.xls', 'LIFE', 'HTML_TABLE', 0, 3, 4, 81, 22),
    ('손해-실손의료보험-4세대실손의료보험21.xls', 'NONLIFE', 'XLS', 5, 6, 7, 107, 21),
    ('손해-장기보장성-갱신상해보험240.xls', 'NONLIFE', 'XLS', 5, 6, 7, 1493, 21),
    ('손해-장기보장성-기타55.xls', 'NONLIFE', 'XLS', 5, 6, 7, 390, 21),
    ('손해-장기보장성-비갱신상해보험353.xls', 'NONLIFE', 'XLS', 5, 6, 7, 2100, 21),
    ('손해-장기보장성-암보험117.xls', 'NONLIFE', 'XLS', 5, 6, 7, 1470, 23),
    ('손해-장기보장성-운전자보험44.xls', 'NONLIFE', 'XLS', 5, 6, 7, 289, 21),
    ('손해-장기보장성-종합보험23.xls', 'NONLIFE', 'XLS', 5, 6, 7, 144, 21),
    ('손해-장기보장성-질병보험59.xls', 'NONLIFE', 'XLS', 5, 6, 7, 367, 21),
    ('손해-장기보장성-화재보험13.xls', 'NONLIFE', 'XLS', 5, 6, 7, 77, 21)
ON CONFLICT (file_name) DO UPDATE
SET insurer_sector  = EXCLUDED.insurer_sector,
    parse_format    = EXCLUDED.parse_format,
    header_row_from = EXCLUDED.header_row_from,
    header_row_to   = EXCLUDED.header_row_to,
    data_row_from   = EXCLUDED.data_row_from,
    row_count       = EXCLUDED.row_count,
    column_count    = EXCLUDED.column_count;

-- ---------------------------------------------------------------------------
-- 생명보험 staging
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS insurance.stg_life_product (
    stg_life_id                                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_file_id                               BIGINT NOT NULL REFERENCES insurance.source_file(source_file_id),
    source_row_no                                INTEGER NOT NULL,
    company_name                                 TEXT NOT NULL,
    product_name                                 TEXT NOT NULL,
    contract_type_text                           TEXT,
    coverage_name                                TEXT,
    claim_reason_text                            TEXT,
    payout_amount_text                           TEXT,
    join_amount_text                             TEXT,
    premium_male_text                            TEXT,
    premium_female_text                          TEXT,
    price_index_male_text                        TEXT,
    price_index_female_text                      TEXT,
    extra_premium_index_male_text                TEXT,
    extra_premium_index_female_text              TEXT,
    contract_cost_index_male_text                TEXT,
    contract_cost_index_female_text              TEXT,
    product_feature_text                         TEXT,
    surrender_value_text                         TEXT,
    fixed_rate_text                              TEXT,
    current_announced_rate_text                  TEXT,
    minimum_guaranteed_rate_text                 TEXT,
    minimum_death_benefit_text                   TEXT,
    minimum_death_benefit_method_text            TEXT,
    minimum_surrender_value_text                 TEXT,
    minimum_surrender_value_method_text          TEXT,
    mild_dementia_covered_text                   TEXT,
    mild_dementia_benefit_amount_text            TEXT,
    coverage_scope_index_cancer_diagnosis_text   TEXT,
    coverage_scope_index_cancer_hospitalization_text TEXT,
    product_subtype_text                         TEXT,
    renewal_text                                 TEXT,
    universal_text                               TEXT,
    sale_channel                                 TEXT,
    sale_date                                    DATE,
    special_note                                 TEXT,
    contact_phone                                TEXT,
    coverage_category_code                       insurance.coverage_category_code,
    coverage_code                                TEXT,
    mapping_status                               insurance.mapping_status NOT NULL DEFAULT 'UNMAPPED',
    manual_note                                  TEXT,
    raw_row_jsonb                                JSONB,
    loaded_at                                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (source_file_id, source_row_no)
);

COMMENT ON TABLE insurance.stg_life_product IS '생명보험 Excel 정규화 staging';
COMMENT ON COLUMN insurance.stg_life_product.source_row_no IS '헤더를 제외한 데이터 row 순번(원천 추적용)';
COMMENT ON COLUMN insurance.stg_life_product.coverage_name IS '급부명칭을 통합 레이어에서 보장 항목명으로 사용';
COMMENT ON COLUMN insurance.stg_life_product.payout_amount_text IS '지급금액 원천 문자열';
COMMENT ON COLUMN insurance.stg_life_product.renewal_text IS '원천은 갱신주기였으나 통합 레이어에서는 갱신 속성으로 사용';
COMMENT ON COLUMN insurance.stg_life_product.raw_row_jsonb IS '원천 row 전체 JSON';

CREATE INDEX IF NOT EXISTS ix_stg_life_product_company_product ON insurance.stg_life_product (company_name, product_name);
CREATE INDEX IF NOT EXISTS ix_stg_life_product_coverage_name ON insurance.stg_life_product (coverage_name);
CREATE INDEX IF NOT EXISTS ix_stg_life_product_coverage_code ON insurance.stg_life_product (coverage_code);
CREATE INDEX IF NOT EXISTS ix_stg_life_product_raw_row_jsonb ON insurance.stg_life_product USING GIN (raw_row_jsonb);

CREATE TRIGGER trg_stg_life_product_set_updated_at
BEFORE UPDATE ON insurance.stg_life_product
FOR EACH ROW EXECUTE FUNCTION insurance.set_updated_at();

-- ---------------------------------------------------------------------------
-- 손해보험 staging
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS insurance.stg_nonlife_product (
    stg_nonlife_id                               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_file_id                               BIGINT NOT NULL REFERENCES insurance.source_file(source_file_id),
    source_row_no                                INTEGER NOT NULL,
    is_selected_text                             TEXT,
    company_name                                 TEXT NOT NULL,
    product_name                                 TEXT NOT NULL,
    sale_channel                                 TEXT,
    coverage_name                                TEXT,
    claim_reason_text                            TEXT,
    payout_amount_text                           TEXT,
    coverage_part_interest_rate_text             TEXT,
    reserve_part_interest_rate_text              TEXT,
    premium_male_text                            TEXT,
    premium_female_text                          TEXT,
    minimum_join_premium_text                    TEXT,
    price_index_male_text                        TEXT,
    price_index_female_text                      TEXT,
    coverage_scope_index_name_text               TEXT,
    coverage_scope_index_value_text              TEXT,
    contract_cost_index_text                     TEXT,
    extra_premium_index_text                     TEXT,
    expected_renewal_premium_text                TEXT,
    product_summary_text                         TEXT,
    renewal_text                                 TEXT,
    special_note                                 TEXT,
    contact_phone                                TEXT,
    coverage_category_code                       insurance.coverage_category_code,
    coverage_code                                TEXT,
    mapping_status                               insurance.mapping_status NOT NULL DEFAULT 'UNMAPPED',
    manual_note                                  TEXT,
    raw_row_jsonb                                JSONB,
    loaded_at                                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (source_file_id, source_row_no)
);

COMMENT ON TABLE insurance.stg_nonlife_product IS '손해보험 Excel 정규화 staging';
COMMENT ON COLUMN insurance.stg_nonlife_product.coverage_name IS '담보명';
COMMENT ON COLUMN insurance.stg_nonlife_product.payout_amount_text IS '지급액/지급액설명보기 원천 문자열';
COMMENT ON COLUMN insurance.stg_nonlife_product.coverage_scope_index_name_text IS '손보 암보험 파일의 첫 번째 보장범위지수 컬럼(예: 암진단비담보)';
COMMENT ON COLUMN insurance.stg_nonlife_product.coverage_scope_index_value_text IS '손보 암보험 파일의 두 번째 보장범위지수 컬럼(예: 99.4)';
COMMENT ON COLUMN insurance.stg_nonlife_product.raw_row_jsonb IS '원천 row 전체 JSON';

CREATE INDEX IF NOT EXISTS ix_stg_nonlife_product_company_product ON insurance.stg_nonlife_product (company_name, product_name);
CREATE INDEX IF NOT EXISTS ix_stg_nonlife_product_coverage_name ON insurance.stg_nonlife_product (coverage_name);
CREATE INDEX IF NOT EXISTS ix_stg_nonlife_product_coverage_code ON insurance.stg_nonlife_product (coverage_code);
CREATE INDEX IF NOT EXISTS ix_stg_nonlife_product_raw_row_jsonb ON insurance.stg_nonlife_product USING GIN (raw_row_jsonb);

CREATE TRIGGER trg_stg_nonlife_product_set_updated_at
BEFORE UPDATE ON insurance.stg_nonlife_product
FOR EACH ROW EXECUTE FUNCTION insurance.set_updated_at();

-- ---------------------------------------------------------------------------
-- 통합 조회 view
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW insurance.v_product_unified AS
SELECT
    'stg_life_product'::text                      AS source_table,
    l.stg_life_id                                 AS record_id,
    sf.file_name                                  AS source_file_name,
    sf.insurer_sector                             AS insurer_sector,
    l.source_file_id,
    l.source_row_no,
    l.company_name,
    l.product_name,
    l.sale_channel,
    l.contract_type_text,
    NULL::text                                    AS is_selected_text,
    l.coverage_name,
    l.claim_reason_text,
    l.payout_amount_text,
    l.join_amount_text,
    NULL::text                                    AS minimum_join_premium_text,
    l.premium_male_text,
    l.premium_female_text,
    l.fixed_rate_text,
    l.current_announced_rate_text,
    l.minimum_guaranteed_rate_text,
    NULL::text                                    AS coverage_part_interest_rate_text,
    NULL::text                                    AS reserve_part_interest_rate_text,
    l.price_index_male_text,
    l.price_index_female_text,
    l.extra_premium_index_male_text,
    l.extra_premium_index_female_text,
    NULL::text                                    AS extra_premium_index_text,
    l.contract_cost_index_male_text,
    l.contract_cost_index_female_text,
    NULL::text                                    AS contract_cost_index_text,
    NULL::text                                    AS coverage_scope_index_name_text,
    NULL::text                                    AS coverage_scope_index_value_text,
    l.coverage_scope_index_cancer_diagnosis_text,
    l.coverage_scope_index_cancer_hospitalization_text,
    NULL::text                                    AS expected_renewal_premium_text,
    NULL::text                                    AS product_summary_text,
    l.product_feature_text,
    l.surrender_value_text,
    l.minimum_death_benefit_text,
    l.minimum_death_benefit_method_text,
    l.minimum_surrender_value_text,
    l.minimum_surrender_value_method_text,
    l.mild_dementia_covered_text,
    l.mild_dementia_benefit_amount_text,
    l.product_subtype_text,
    l.renewal_text,
    l.universal_text,
    l.special_note,
    l.contact_phone,
    l.sale_date,
    l.coverage_category_code,
    l.coverage_code,
    l.mapping_status,
    l.manual_note,
    l.raw_row_jsonb,
    l.loaded_at,
    l.updated_at
FROM insurance.stg_life_product l
JOIN insurance.source_file sf ON sf.source_file_id = l.source_file_id

UNION ALL

SELECT
    'stg_nonlife_product'::text                   AS source_table,
    n.stg_nonlife_id                              AS record_id,
    sf.file_name                                  AS source_file_name,
    sf.insurer_sector                             AS insurer_sector,
    n.source_file_id,
    n.source_row_no,
    n.company_name,
    n.product_name,
    n.sale_channel,
    NULL::text                                    AS contract_type_text,
    n.is_selected_text,
    n.coverage_name,
    n.claim_reason_text,
    n.payout_amount_text,
    NULL::text                                    AS join_amount_text,
    n.minimum_join_premium_text,
    n.premium_male_text,
    n.premium_female_text,
    NULL::text                                    AS fixed_rate_text,
    NULL::text                                    AS current_announced_rate_text,
    NULL::text                                    AS minimum_guaranteed_rate_text,
    n.coverage_part_interest_rate_text,
    n.reserve_part_interest_rate_text,
    n.price_index_male_text,
    n.price_index_female_text,
    NULL::text                                    AS extra_premium_index_male_text,
    NULL::text                                    AS extra_premium_index_female_text,
    n.extra_premium_index_text,
    NULL::text                                    AS contract_cost_index_male_text,
    NULL::text                                    AS contract_cost_index_female_text,
    n.contract_cost_index_text,
    n.coverage_scope_index_name_text,
    n.coverage_scope_index_value_text,
    NULL::text                                    AS coverage_scope_index_cancer_diagnosis_text,
    NULL::text                                    AS coverage_scope_index_cancer_hospitalization_text,
    n.expected_renewal_premium_text,
    n.product_summary_text,
    NULL::text                                    AS product_feature_text,
    NULL::text                                    AS surrender_value_text,
    NULL::text                                    AS minimum_death_benefit_text,
    NULL::text                                    AS minimum_death_benefit_method_text,
    NULL::text                                    AS minimum_surrender_value_text,
    NULL::text                                    AS minimum_surrender_value_method_text,
    NULL::text                                    AS mild_dementia_covered_text,
    NULL::text                                    AS mild_dementia_benefit_amount_text,
    NULL::text                                    AS product_subtype_text,
    n.renewal_text,
    NULL::text                                    AS universal_text,
    n.special_note,
    n.contact_phone,
    NULL::date                                    AS sale_date,
    n.coverage_category_code,
    n.coverage_code,
    n.mapping_status,
    n.manual_note,
    n.raw_row_jsonb,
    n.loaded_at,
    n.updated_at
FROM insurance.stg_nonlife_product n
JOIN insurance.source_file sf ON sf.source_file_id = n.source_file_id;

COMMENT ON VIEW insurance.v_product_unified IS '생보/손보 Excel 정규화 결과를 하나의 조회 인터페이스로 통합한 view';
