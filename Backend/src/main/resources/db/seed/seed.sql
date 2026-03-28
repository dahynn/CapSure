-- Minimal seed data for local login and smoke tests.
-- product_source bulk data is intentionally excluded here.

-- ---------------------------------------------------------------------------
-- demo user reset (idempotent reseed)
-- ---------------------------------------------------------------------------
DELETE FROM public.audit_event_log
WHERE actor_user_id IN (
    SELECT u.user_id
    FROM public.usr_user u
    WHERE u.email = 'demo@example.com'
);

DELETE FROM public.usr_user
WHERE email = 'demo@example.com';

-- ---------------------------------------------------------------------------
-- login user
-- plain password: Passw0rd!
-- ---------------------------------------------------------------------------
INSERT INTO public.usr_user (
    email,
    password_encrypted,
    name,
    phone,
    birth_date,
    gender,
    user_status,
    onboarding_completed_at
)
VALUES (
    'demo@example.com',
    '$2a$10$QCibuURntLkkHh.yHFyMQeIVzl8.th7Kj2uvUMaOCmZIXLypM3ow.',
    '김싸피',
    '010-0000-0000',
    DATE '1990-01-01',
    'M',
    'ACTIVE',
    NOW()
)
ON CONFLICT (email) DO UPDATE
SET
    password_encrypted = EXCLUDED.password_encrypted,
    name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    birth_date = EXCLUDED.birth_date,
    gender = EXCLUDED.gender,
    user_status = EXCLUDED.user_status,
    onboarding_completed_at = EXCLUDED.onboarding_completed_at,
    updated_at = NOW();

INSERT INTO public.usr_onboarding_category (
    user_id,
    coverage_category_code
)
SELECT u.user_id, v.coverage_category_code::coverage_category_code_enum
FROM public.usr_user u
cross join (
    values
        ('CANCER'),
        ('DEATH'),
        ('SURGERY')
) as v(coverage_category_code)
WHERE u.email = 'demo@example.com'
ON CONFLICT (user_id, coverage_category_code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- reference coverage catalog
-- ---------------------------------------------------------------------------
INSERT INTO public.ref_coverage (
    coverage_code,
    coverage_category_code,
    coverage_name,
    duplicate_group_code,
    description,
    search_keywords_json,
    is_active,
    compensation_type,
    duplicate_rule_code,
    recommendation_rule_code
)
VALUES
    (
        'CANCER_DIAGNOSIS',
        'CANCER',
        'Cancer Diagnosis Benefit',
        'CANCER_DIAGNOSIS',
        'Fixed benefit on initial cancer diagnosis.',
        '["cancer","diagnosis","oncology"]',
        TRUE,
        'FIXED_BENEFIT',
        'CHECK_MANUALLY',
        'ALLOW_TOPUP'
    ),
    (
        'ACCIDENT_INJURY',
        'ACCIDENT',
        'Accident Injury Benefit',
        'ACCIDENT_BASIC',
        'Accident injury coverage with fixed payout.',
        '["accident","injury","trauma"]',
        TRUE,
        'FIXED_BENEFIT',
        'MULTI_PAY_ALLOWED',
        'SUPPRESS_IF_EXISTS'
    ),
    (
        'DEATH_GENERAL',
        'DEATH',
        'Death Benefit',
        'DEATH_GENERAL',
        'General death coverage.',
        '["death","life","benefit"]',
        TRUE,
        'FIXED_BENEFIT',
        'CHECK_MANUALLY',
        'ALLOW_TOPUP'
    ),
    (
        'SURGERY_GENERAL',
        'SURGERY',
        'Surgery Benefit',
        'SURGERY_GENERAL',
        'General surgery coverage.',
        '["surgery","operation","procedure"]',
        TRUE,
        'FIXED_BENEFIT',
        'CHECK_MANUALLY',
        'ALLOW_TOPUP'
    ),
    (
        'BRAIN_HEART_GENERAL',
        'BRAIN_HEART',
        'Brain/Heart Benefit',
        'BRAIN_HEART_GENERAL',
        'Brain and heart major disease coverage.',
        '["brain","heart","stroke","cardio"]',
        TRUE,
        'FIXED_BENEFIT',
        'CHECK_MANUALLY',
        'ALLOW_TOPUP'
    ),
    (
        'ACTUAL_LOSS_GENERAL',
        'ACTUAL_LOSS',
        'Actual Loss Medical Benefit',
        'ACTUAL_LOSS_GENERAL',
        'Actual loss reimbursement coverage.',
        '["actual loss","medical","reimbursement"]',
        TRUE,
        'INDEMNITY',
        'MULTI_PAY_ALLOWED',
        'ALLOW_TOPUP'
    ),
    (
        'LIABILITY_GENERAL',
        'LIABILITY',
        'Liability Benefit',
        'LIABILITY_GENERAL',
        'Daily liability protection coverage.',
        '["liability","daily","damage"]',
        TRUE,
        'INDEMNITY',
        'CHECK_MANUALLY',
        'ALLOW_TOPUP'
    )
ON CONFLICT (coverage_code) DO UPDATE
SET
    coverage_category_code = EXCLUDED.coverage_category_code,
    coverage_name = EXCLUDED.coverage_name,
    duplicate_group_code = EXCLUDED.duplicate_group_code,
    description = EXCLUDED.description,
    search_keywords_json = EXCLUDED.search_keywords_json,
    is_active = EXCLUDED.is_active,
    compensation_type = EXCLUDED.compensation_type,
    duplicate_rule_code = EXCLUDED.duplicate_rule_code,
    recommendation_rule_code = EXCLUDED.recommendation_rule_code,
    updated_at = NOW();

-- ---------------------------------------------------------------------------
-- capsule products
-- 실제 insurance.product_source 에서 카테고리별 샘플을 가져와 시드한다.
-- ---------------------------------------------------------------------------
WITH ranked_source AS (
    SELECT
        ps.product_source_id,
        ps.product_name,
        ps.coverage_category_code::text AS coverage_category_code,
        ps.monthly_premium_male,
        ps.monthly_premium_female,
        ROW_NUMBER() OVER (
            PARTITION BY ps.coverage_category_code
            ORDER BY ps.sale_date DESC NULLS LAST, ps.product_source_id ASC
        ) AS rn
    FROM insurance.product_source ps
    WHERE ps.coverage_category_code::text IN (
        'CANCER',
        'DEATH',
        'SURGERY',
        'BRAIN_HEART',
        'ACTUAL_LOSS',
        'ACCIDENT',
        'LIABILITY'
    )
),
selected_source AS (
    SELECT
        rs.product_source_id AS capsule_product_id,
        CASE
            WHEN rs.coverage_category_code = 'CANCER' AND rs.rn = 1 THEN 'CAPSULE-CANCER-001'
            WHEN rs.coverage_category_code = 'DEATH' AND rs.rn = 1 THEN 'CAPSULE-DEATH-001'
            WHEN rs.coverage_category_code = 'SURGERY' AND rs.rn = 1 THEN 'CAPSULE-SURGERY-001'
            WHEN rs.coverage_category_code = 'BRAIN_HEART' AND rs.rn = 1 THEN 'CAPSULE-BRAINHEART-001'
            WHEN rs.coverage_category_code = 'ACTUAL_LOSS' AND rs.rn = 1 THEN 'CAPSULE-ACTUALLOSS-001'
            WHEN rs.coverage_category_code = 'ACCIDENT' AND rs.rn = 1 THEN 'CAPSULE-ACCIDENT-001'
            WHEN rs.coverage_category_code = 'LIABILITY' AND rs.rn = 1 THEN 'CAPSULE-LIABILITY-001'
        END AS capsule_code,
        rs.product_name,
        rs.coverage_category_code,
        CASE
            WHEN rs.coverage_category_code = 'CANCER' THEN 'CANCER_DIAGNOSIS'
            WHEN rs.coverage_category_code = 'DEATH' THEN 'DEATH_GENERAL'
            WHEN rs.coverage_category_code = 'SURGERY' THEN 'SURGERY_GENERAL'
            WHEN rs.coverage_category_code = 'BRAIN_HEART' THEN 'BRAIN_HEART_GENERAL'
            WHEN rs.coverage_category_code = 'ACTUAL_LOSS' THEN 'ACTUAL_LOSS_GENERAL'
            WHEN rs.coverage_category_code = 'ACCIDENT' THEN 'ACCIDENT_INJURY'
            ELSE 'LIABILITY_GENERAL'
        END AS coverage_code,
        CASE
            WHEN rs.coverage_category_code = 'CANCER' AND rs.rn = 1 THEN 10000000
            WHEN rs.coverage_category_code = 'DEATH' AND rs.rn = 1 THEN 30000000
            WHEN rs.coverage_category_code = 'SURGERY' AND rs.rn = 1 THEN 3000000
            WHEN rs.coverage_category_code = 'BRAIN_HEART' AND rs.rn = 1 THEN 20000000
            WHEN rs.coverage_category_code = 'ACTUAL_LOSS' AND rs.rn = 1 THEN 5000000
            WHEN rs.coverage_category_code = 'ACCIDENT' AND rs.rn = 1 THEN 3000000
            ELSE 10000000
        END AS coverage_amount,
        COALESCE(rs.monthly_premium_male, 0) AS monthly_price_male,
        COALESCE(rs.monthly_premium_female, rs.monthly_premium_male, 0) AS monthly_price_female,
        CASE
            WHEN rs.coverage_category_code = 'CANCER' AND rs.rn = 1 THEN 'Starter cancer protection capsule.'
            WHEN rs.coverage_category_code = 'DEATH' AND rs.rn = 1 THEN 'General death protection capsule.'
            WHEN rs.coverage_category_code = 'SURGERY' AND rs.rn = 1 THEN 'General surgery protection capsule.'
            WHEN rs.coverage_category_code = 'BRAIN_HEART' AND rs.rn = 1 THEN 'Brain/heart focused protection capsule.'
            WHEN rs.coverage_category_code = 'ACTUAL_LOSS' AND rs.rn = 1 THEN 'Actual loss medical protection capsule.'
            WHEN rs.coverage_category_code = 'ACCIDENT' AND rs.rn = 1 THEN 'Daily accident protection capsule.'
            ELSE 'Daily liability protection capsule.'
        END AS description,
        CASE
            WHEN rs.coverage_category_code = 'CANCER' AND rs.rn = 1 THEN 'https://example.com/terms/cancer-starter'
            WHEN rs.coverage_category_code = 'DEATH' AND rs.rn = 1 THEN 'https://example.com/terms/death-basic'
            WHEN rs.coverage_category_code = 'SURGERY' AND rs.rn = 1 THEN 'https://example.com/terms/surgery-basic'
            WHEN rs.coverage_category_code = 'BRAIN_HEART' AND rs.rn = 1 THEN 'https://example.com/terms/brain-heart-basic'
            WHEN rs.coverage_category_code = 'ACTUAL_LOSS' AND rs.rn = 1 THEN 'https://example.com/terms/actual-loss-basic'
            WHEN rs.coverage_category_code = 'ACCIDENT' AND rs.rn = 1 THEN 'https://example.com/terms/accident-daily'
            ELSE 'https://example.com/terms/liability-basic'
        END AS terms_uri
    FROM ranked_source rs
    WHERE rs.rn = 1
)
INSERT INTO public.capsule_product (
    capsule_product_id,
    capsule_code,
    product_name,
    coverage_category_code,
    coverage_code,
    coverage_amount,
    coverage_unit,
    monthly_price_male,
    monthly_price_female,
    min_retention_days,
    sale_status,
    is_duplicate_check_target,
    terms_uri,
    terms_version,
    description
)
SELECT
    ss.capsule_product_id,
    ss.capsule_code,
    ss.product_name,
    ss.coverage_category_code::coverage_category_code_enum,
    ss.coverage_code,
    ss.coverage_amount,
    'KRW',
    ss.monthly_price_male,
    ss.monthly_price_female,
    30,
    'AVAILABLE',
    TRUE,
    ss.terms_uri,
    'v1',
    ss.description
FROM selected_source ss
WHERE ss.capsule_code IS NOT NULL
ON CONFLICT (capsule_code) DO UPDATE
SET
    product_name = EXCLUDED.product_name,
    coverage_category_code = EXCLUDED.coverage_category_code,
    coverage_code = EXCLUDED.coverage_code,
    coverage_amount = EXCLUDED.coverage_amount,
    coverage_unit = EXCLUDED.coverage_unit,
    monthly_price_male = EXCLUDED.monthly_price_male,
    monthly_price_female = EXCLUDED.monthly_price_female,
    min_retention_days = EXCLUDED.min_retention_days,
    sale_status = EXCLUDED.sale_status,
    is_duplicate_check_target = EXCLUDED.is_duplicate_check_target,
    terms_uri = EXCLUDED.terms_uri,
    terms_version = EXCLUDED.terms_version,
    description = EXCLUDED.description,
    updated_at = NOW();

-- ---------------------------------------------------------------------------
-- optional sample aggregate rows for mapper smoke tests
-- ---------------------------------------------------------------------------
INSERT INTO public.subscription (
    user_id,
    capsule_name,
    subscription_status,
    billing_anchor_day,
    current_cycle_start_at,
    current_cycle_end_at,
    next_billing_at,
    expected_next_amount
)
SELECT
    u.user_id,
    '데모 건강 캡슐',
    'ACTIVE',
    5,
    TIMESTAMPTZ '2026-03-01 00:00:00+09',
    TIMESTAMPTZ '2026-03-31 23:59:59+09',
    TIMESTAMPTZ '2026-04-05 00:00:00+09',
    0
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
  AND NOT EXISTS (
    SELECT 1
    FROM public.subscription s
    WHERE s.user_id = u.user_id
      AND s.capsule_name = '데모 건강 캡슐'
  );

INSERT INTO public.subscription (
    user_id,
    capsule_name,
    subscription_status,
    billing_anchor_day,
    current_cycle_start_at,
    current_cycle_end_at,
    next_billing_at,
    expected_next_amount
)
SELECT
    u.user_id,
    '데모 상해 캡슐',
    'ACTIVE',
    12,
    TIMESTAMPTZ '2026-03-08 00:00:00+09',
    TIMESTAMPTZ '2026-04-07 23:59:59+09',
    TIMESTAMPTZ '2026-04-12 00:00:00+09',
    0
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
  AND NOT EXISTS (
    SELECT 1
    FROM public.subscription s
    WHERE s.user_id = u.user_id
      AND s.capsule_name = '데모 상해 캡슐'
  );

INSERT INTO public.subscription (
    user_id,
    capsule_name,
    subscription_status,
    billing_anchor_day,
    current_cycle_start_at,
    current_cycle_end_at,
    next_billing_at,
    expected_next_amount
)
SELECT
    u.user_id,
    '데모 혼합 캡슐',
    'ACTIVE',
    18,
    TIMESTAMPTZ '2026-03-15 00:00:00+09',
    TIMESTAMPTZ '2026-04-14 23:59:59+09',
    TIMESTAMPTZ '2026-04-18 00:00:00+09',
    0
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
  AND NOT EXISTS (
    SELECT 1
    FROM public.subscription s
    WHERE s.user_id = u.user_id
      AND s.capsule_name = '데모 혼합 캡슐'
  );

INSERT INTO public.subscription_item (
    subscription_id,
    capsule_product_id,
    plan_version,
    item_status,
    coverage_amount_snapshot,
    monthly_price_snapshot,
    effective_start_at,
    effective_end_at,
    editable_after_at
)
SELECT
    s.subscription_id,
    cp.capsule_product_id,
    'CURRENT',
    'ACTIVE',
    cp.coverage_amount,
    CASE WHEN u.gender = 'F' THEN cp.monthly_price_female ELSE cp.monthly_price_male END,
    TIMESTAMPTZ '2026-03-01 00:00:00+09',
    TIMESTAMPTZ '2026-04-01 00:00:00+09',
    TIMESTAMPTZ '2026-04-01 00:00:00+09'
FROM public.subscription s
JOIN public.usr_user u
    ON u.user_id = s.user_id
JOIN public.capsule_product cp
    ON cp.capsule_code IN (
        'CAPSULE-CANCER-001',
        'CAPSULE-BRAINHEART-001',
        'CAPSULE-ACTUALLOSS-001'
    )
WHERE u.email = 'demo@example.com'
  AND s.capsule_name = '데모 건강 캡슐'
ON CONFLICT (subscription_id, capsule_product_id, plan_version) DO UPDATE
SET
    item_status = EXCLUDED.item_status,
    coverage_amount_snapshot = EXCLUDED.coverage_amount_snapshot,
    monthly_price_snapshot = EXCLUDED.monthly_price_snapshot,
    effective_start_at = EXCLUDED.effective_start_at,
    effective_end_at = EXCLUDED.effective_end_at,
    editable_after_at = EXCLUDED.editable_after_at,
    updated_at = NOW();

INSERT INTO public.subscription_item (
    subscription_id,
    capsule_product_id,
    plan_version,
    item_status,
    coverage_amount_snapshot,
    monthly_price_snapshot,
    effective_start_at,
    effective_end_at,
    editable_after_at
)
SELECT
    s.subscription_id,
    cp.capsule_product_id,
    'CURRENT',
    'ACTIVE',
    cp.coverage_amount,
    CASE WHEN u.gender = 'F' THEN cp.monthly_price_female ELSE cp.monthly_price_male END,
    TIMESTAMPTZ '2026-03-08 00:00:00+09',
    TIMESTAMPTZ '2026-04-08 00:00:00+09',
    TIMESTAMPTZ '2026-04-08 00:00:00+09'
FROM public.subscription s
JOIN public.usr_user u
    ON u.user_id = s.user_id
JOIN public.capsule_product cp
    ON cp.capsule_code IN (
        'CAPSULE-ACCIDENT-001',
        'CAPSULE-LIABILITY-001',
        'CAPSULE-DEATH-001'
    )
WHERE u.email = 'demo@example.com'
  AND s.capsule_name = '데모 상해 캡슐'
ON CONFLICT (subscription_id, capsule_product_id, plan_version) DO UPDATE
SET
    item_status = EXCLUDED.item_status,
    coverage_amount_snapshot = EXCLUDED.coverage_amount_snapshot,
    monthly_price_snapshot = EXCLUDED.monthly_price_snapshot,
    effective_start_at = EXCLUDED.effective_start_at,
    effective_end_at = EXCLUDED.effective_end_at,
    editable_after_at = EXCLUDED.editable_after_at,
    updated_at = NOW();

INSERT INTO public.subscription_item (
    subscription_id,
    capsule_product_id,
    plan_version,
    item_status,
    coverage_amount_snapshot,
    monthly_price_snapshot,
    effective_start_at,
    effective_end_at,
    editable_after_at
)
SELECT
    s.subscription_id,
    cp.capsule_product_id,
    'CURRENT',
    'ACTIVE',
    cp.coverage_amount,
    CASE WHEN u.gender = 'F' THEN cp.monthly_price_female ELSE cp.monthly_price_male END,
    TIMESTAMPTZ '2026-03-15 00:00:00+09',
    TIMESTAMPTZ '2026-04-15 00:00:00+09',
    TIMESTAMPTZ '2026-04-15 00:00:00+09'
FROM public.subscription s
JOIN public.usr_user u
    ON u.user_id = s.user_id
JOIN public.capsule_product cp
    ON cp.capsule_code IN (
        'CAPSULE-CANCER-001',
        'CAPSULE-SURGERY-001',
        'CAPSULE-LIABILITY-001'
    )
WHERE u.email = 'demo@example.com'
  AND s.capsule_name = '데모 혼합 캡슐'
ON CONFLICT (subscription_id, capsule_product_id, plan_version) DO UPDATE
SET
    item_status = EXCLUDED.item_status,
    coverage_amount_snapshot = EXCLUDED.coverage_amount_snapshot,
    monthly_price_snapshot = EXCLUDED.monthly_price_snapshot,
    effective_start_at = EXCLUDED.effective_start_at,
    effective_end_at = EXCLUDED.effective_end_at,
    editable_after_at = EXCLUDED.editable_after_at,
    updated_at = NOW();

UPDATE public.subscription s
SET
    expected_next_amount = COALESCE((
        SELECT SUM(si.monthly_price_snapshot)
        FROM public.subscription_item si
        WHERE si.subscription_id = s.subscription_id
          AND si.plan_version = 'CURRENT'
          AND si.item_status = 'ACTIVE'
    ), 0),
    updated_at = NOW()
FROM public.usr_user u
WHERE s.user_id = u.user_id
  AND u.email = 'demo@example.com';

INSERT INTO public.usr_payment_method (
    user_id,
    provider,
    method_type,
    masked_number,
    is_active
)
SELECT
    u.user_id,
    'TOSS',
    'BANK_ACCOUNT',
    '신한 ****-****-1234',
    true
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO public.subscription_capsule_snapshot (
    subscription_id,
    user_id,
    capsule_name,
    total_premium,
    cycle_started_at,
    cycle_ended_at,
    created_at
)
SELECT
    s.subscription_id,
    s.user_id,
    s.capsule_name,
    s.expected_next_amount,
    s.current_cycle_start_at,
    s.current_cycle_end_at,
    s.created_at
FROM public.subscription s
JOIN public.usr_user u
    ON u.user_id = s.user_id
WHERE u.email = 'demo@example.com'
  AND NOT EXISTS (
    SELECT 1
    FROM public.subscription_capsule_snapshot scs
    WHERE scs.subscription_id = s.subscription_id
  );

INSERT INTO public.subscription_capsule_snapshot_item (
    capsule_snapshot_id,
    product_source_id,
    product_name,
    company_name,
    coverage_category_code,
    monthly_price_snapshot,
    created_at
)
SELECT
    scs.capsule_snapshot_id,
    ps.product_source_id,
    ps.product_name,
    ps.company_name,
    ps.coverage_category_code::text::coverage_category_code_enum,
    si.monthly_price_snapshot,
    NOW()
FROM public.subscription_capsule_snapshot scs
JOIN public.subscription_item si
    ON si.subscription_id = scs.subscription_id
   AND si.plan_version = 'CURRENT'
   AND si.item_status = 'ACTIVE'
JOIN insurance.product_source ps
    ON ps.product_source_id = si.capsule_product_id
WHERE NOT EXISTS (
    SELECT 1
    FROM public.subscription_capsule_snapshot_item scsi
    WHERE scsi.capsule_snapshot_id = scs.capsule_snapshot_id
);

INSERT INTO public.sec_digital_seal (
    user_id,
    provider_name,
    seal_status,
    encrypted_token,
    key_version,
    issued_at,
    last_verified_at,
    expires_at
)
SELECT
    u.user_id,
    'CAPSULE-SEAL-DEMO',
    'REGISTERED',
    'demo-encrypted-token',
    1,
    TIMESTAMPTZ '2026-03-01 09:00:00+09',
    TIMESTAMPTZ '2026-03-20 09:00:00+09',
    TIMESTAMPTZ '2026-12-31 23:59:59+09'
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM public.sec_digital_seal s
      WHERE s.user_id = u.user_id
        AND s.provider_name = 'CAPSULE-SEAL-DEMO'
  );

INSERT INTO public.comp_user_legal_event (
    user_id,
    subscription_id,
    seal_id,
    event_type,
    event_result,
    event_at,
    terms_version,
    evidence_json,
    request_id,
    session_id
)
SELECT
    u.user_id,
    s.subscription_id,
    ds.seal_id,
    'SERVICE_TERMS_AGREE',
    'SUCCESS',
    TIMESTAMPTZ '2026-03-20 09:01:00+09',
    'v1',
    '{"channel":"seed","note":"demo legal event"}',
    'seed-request-001',
    'seed-session-001'
FROM public.usr_user u
JOIN public.subscription s
    ON s.user_id = u.user_id
JOIN public.sec_digital_seal ds
    ON ds.user_id = u.user_id
WHERE u.email = 'demo@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM public.comp_user_legal_event e
      WHERE e.user_id = u.user_id
        AND e.request_id = 'seed-request-001'
  );

INSERT INTO public.audit_event_log (
    event_type,
    actor_user_id,
    target_type,
    target_id,
    occurred_at,
    request_id,
    before_json,
    after_json,
    integrity_hash,
    prev_hash
)
SELECT
    'SUBSCRIPTION_CHANGED',
    u.user_id,
    'SUBSCRIPTION',
    s.subscription_id,
    TIMESTAMPTZ '2026-03-20 09:02:00+09',
    'seed-request-001',
    '{"status":"NONE"}',
    '{"status":"ACTIVE"}',
    'seed-integrity-hash-001',
    'seed-prev-hash-000'
FROM public.usr_user u
JOIN public.subscription s
    ON s.user_id = u.user_id
WHERE u.email = 'demo@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM public.audit_event_log a
      WHERE a.request_id = 'seed-request-001'
        AND a.target_type = 'SUBSCRIPTION'
        AND a.target_id = s.subscription_id
  );

-- ---------------------------------------------------------------------------
-- demo mydata contracts
-- 런타임 조회 source of truth: myd_contract / myd_contract_coverage
-- ---------------------------------------------------------------------------
INSERT INTO public.myd_contract (
    user_id,
    provider_code,
    insu_num,
    is_consent,
    business_type,
    product_name,
    insu_type_code,
    contract_status_code,
    contract_date,
    start_date,
    end_date,
    premium_amount,
    currency_code,
    insured_list_json,
    prize_list_json,
    contract_list_json,
    policy_uri,
    extra_payload_json
)
SELECT
    u.user_id,
    'CAPSULE-MOCK',
    'DEMO-0001',
    TRUE,
    'LIFE',
    'KDB든든한 건강보험(암보장)(무)',
    'DEMO',
    'NORMAL',
    DATE '2024-01-15',
    DATE '2024-01-15',
    DATE '2034-01-14',
    3165900,
    'KRW',
    '["김싸피"]',
    '["만기환급"]',
    '{"contractId":"DEMO-CONTRACT-0001","source":"seed","note":"demo contract payload"}',
    'https://example.com/policies/DEMO-0001',
    '{"source":"seed","productSourceHint":"KDB든든한 건강보험(암보장)(무)"}'
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
ON CONFLICT (user_id, provider_code, insu_num) DO UPDATE
SET
    is_consent = EXCLUDED.is_consent,
    business_type = EXCLUDED.business_type,
    product_name = EXCLUDED.product_name,
    insu_type_code = EXCLUDED.insu_type_code,
    contract_status_code = EXCLUDED.contract_status_code,
    contract_date = EXCLUDED.contract_date,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    premium_amount = EXCLUDED.premium_amount,
    currency_code = EXCLUDED.currency_code,
    insured_list_json = EXCLUDED.insured_list_json,
    prize_list_json = EXCLUDED.prize_list_json,
    contract_list_json = EXCLUDED.contract_list_json,
    policy_uri = EXCLUDED.policy_uri,
    extra_payload_json = EXCLUDED.extra_payload_json,
    synced_at = NOW(),
    updated_at = NOW();

INSERT INTO public.myd_contract (
    user_id,
    provider_code,
    insu_num,
    is_consent,
    business_type,
    product_name,
    insu_type_code,
    contract_status_code,
    contract_date,
    start_date,
    end_date,
    premium_amount,
    currency_code,
    insured_list_json,
    prize_list_json,
    contract_list_json,
    policy_uri,
    extra_payload_json
)
SELECT
    u.user_id,
    'CAPSULE-MOCK',
    'DEMO-0002',
    TRUE,
    'NONLIFE',
    '생활밀착 상해보험 데모플랜',
    'DEMO',
    'NORMAL',
    DATE '2023-09-03',
    DATE '2023-09-10',
    DATE '2028-09-09',
    15700,
    'KRW',
    '["김싸피"]',
    '["무사고할인"]',
    '{"contractId":"DEMO-CONTRACT-0002","source":"seed","note":"demo contract payload"}',
    'https://example.com/policies/DEMO-0002',
    '{"source":"seed","productSourceHint":"accident demo"}'
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
ON CONFLICT (user_id, provider_code, insu_num) DO UPDATE
SET
    is_consent = EXCLUDED.is_consent,
    business_type = EXCLUDED.business_type,
    product_name = EXCLUDED.product_name,
    insu_type_code = EXCLUDED.insu_type_code,
    contract_status_code = EXCLUDED.contract_status_code,
    contract_date = EXCLUDED.contract_date,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    premium_amount = EXCLUDED.premium_amount,
    currency_code = EXCLUDED.currency_code,
    insured_list_json = EXCLUDED.insured_list_json,
    prize_list_json = EXCLUDED.prize_list_json,
    contract_list_json = EXCLUDED.contract_list_json,
    policy_uri = EXCLUDED.policy_uri,
    extra_payload_json = EXCLUDED.extra_payload_json,
    synced_at = NOW(),
    updated_at = NOW();

INSERT INTO public.myd_contract_coverage (
    myd_contract_id,
    coverage_num,
    coverage_name,
    coverage_amount,
    currency_code,
    coverage_status,
    start_date,
    end_date,
    coverage_code,
    extra_payload_json
)
SELECT
    c.myd_contract_id,
    'COV-0001',
    '건강관리자금',
    NULL,
    'KRW',
    'NORMAL',
    DATE '2024-01-15',
    DATE '2034-01-14',
    'CANCER_GENERAL',
    '{"source":"seed","contract":"DEMO-0001"}'
FROM public.myd_contract c
JOIN public.usr_user u
    ON u.user_id = c.user_id
WHERE u.email = 'demo@example.com'
  AND c.insu_num = 'DEMO-0001'
ON CONFLICT (myd_contract_id, coverage_num) DO UPDATE
SET
    coverage_name = EXCLUDED.coverage_name,
    coverage_amount = EXCLUDED.coverage_amount,
    currency_code = EXCLUDED.currency_code,
    coverage_status = EXCLUDED.coverage_status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    coverage_code = EXCLUDED.coverage_code,
    extra_payload_json = EXCLUDED.extra_payload_json,
    updated_at = NOW();

-- ---------------------------------------------------------------------------
-- 104dashdemo cohort for dashboard / analysis demos
-- ---------------------------------------------------------------------------
INSERT INTO public.usr_user (
    email,
    password_encrypted,
    name,
    phone,
    birth_date,
    gender,
    user_status,
    onboarding_completed_at
)
VALUES
    (
        '104dashdemo01@example.com',
        '$2a$10$QCibuURntLkkHh.yHFyMQeIVzl8.th7Kj2uvUMaOCmZIXLypM3ow.',
        'D104U01',
        '010-1040-0001',
        DATE '1988-01-11',
        'M',
        'ACTIVE',
        NOW()
    ),
    (
        '104dashdemo02@example.com',
        '$2a$10$QCibuURntLkkHh.yHFyMQeIVzl8.th7Kj2uvUMaOCmZIXLypM3ow.',
        'D104U02',
        '010-1040-0002',
        DATE '1991-03-21',
        'F',
        'ACTIVE',
        NOW()
    ),
    (
        '104dashdemo03@example.com',
        '$2a$10$QCibuURntLkkHh.yHFyMQeIVzl8.th7Kj2uvUMaOCmZIXLypM3ow.',
        'D104U03',
        '010-1040-0003',
        DATE '1986-07-09',
        'M',
        'ACTIVE',
        NOW()
    ),
    (
        '104dashdemo04@example.com',
        '$2a$10$QCibuURntLkkHh.yHFyMQeIVzl8.th7Kj2uvUMaOCmZIXLypM3ow.',
        'D104U04',
        '010-1040-0004',
        DATE '1994-11-02',
        'F',
        'ACTIVE',
        NOW()
    ),
    (
        '104dashdemo05@example.com',
        '$2a$10$QCibuURntLkkHh.yHFyMQeIVzl8.th7Kj2uvUMaOCmZIXLypM3ow.',
        'D104U05',
        '010-1040-0005',
        DATE '1983-05-18',
        'M',
        'ACTIVE',
        NOW()
    ),
    (
        '104dashdemo06@example.com',
        '$2a$10$QCibuURntLkkHh.yHFyMQeIVzl8.th7Kj2uvUMaOCmZIXLypM3ow.',
        'D104U06',
        '010-1040-0006',
        DATE '1996-09-27',
        'F',
        'ACTIVE',
        NOW()
    )
ON CONFLICT (email) DO UPDATE
SET
    password_encrypted = EXCLUDED.password_encrypted,
    name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    birth_date = EXCLUDED.birth_date,
    gender = EXCLUDED.gender,
    user_status = EXCLUDED.user_status,
    onboarding_completed_at = EXCLUDED.onboarding_completed_at,
    updated_at = NOW();

INSERT INTO public.myd_contract (
    user_id,
    provider_code,
    insu_num,
    is_consent,
    business_type,
    product_name,
    insu_type_code,
    contract_status_code,
    contract_date,
    start_date,
    end_date,
    premium_amount,
    currency_code,
    insured_list_json,
    prize_list_json,
    contract_list_json,
    policy_uri,
    extra_payload_json
)
SELECT
    u.user_id,
    '104DASHDEMO',
    v.insu_num,
    TRUE,
    v.business_type::public.business_type_enum,
    v.product_name,
    '104DASHDEMO',
    'NORMAL',
    DATE '2024-01-01',
    DATE '2024-01-01',
    DATE '2034-12-31',
    v.premium_amount,
    'KRW',
    '["104dashdemo insured"]',
    '["104dashdemo prize"]',
    '{"source":"seed","cohort":"104dashdemo"}',
    'https://example.com/policies/' || v.insu_num,
    '{"source":"seed","cohort":"104dashdemo"}'
FROM (
    VALUES
        ('104dashdemo01@example.com', '104DASHDEMO-0001', 'LIFE', '104dashdemo Death Plan', 41000),
        ('104dashdemo02@example.com', '104DASHDEMO-0002', 'NONLIFE', '104dashdemo Cancer Loss Plan', 53000),
        ('104dashdemo03@example.com', '104DASHDEMO-0003', 'LIFE', '104dashdemo Brain Surgery Plan', 62000),
        ('104dashdemo04@example.com', '104DASHDEMO-0004', 'NONLIFE', '104dashdemo Mixed Safety Plan', 47000),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'LIFE', '104dashdemo Full Coverage Plan', 89000),
        ('104dashdemo06@example.com', '104DASHDEMO-0006', 'NONLIFE', '104dashdemo Actual Loss Plan', 29000)
) AS v(email, insu_num, business_type, product_name, premium_amount)
JOIN public.usr_user u
    ON u.email = v.email
ON CONFLICT (user_id, provider_code, insu_num) DO UPDATE
SET
    is_consent = EXCLUDED.is_consent,
    business_type = EXCLUDED.business_type,
    product_name = EXCLUDED.product_name,
    insu_type_code = EXCLUDED.insu_type_code,
    contract_status_code = EXCLUDED.contract_status_code,
    contract_date = EXCLUDED.contract_date,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    premium_amount = EXCLUDED.premium_amount,
    currency_code = EXCLUDED.currency_code,
    insured_list_json = EXCLUDED.insured_list_json,
    prize_list_json = EXCLUDED.prize_list_json,
    contract_list_json = EXCLUDED.contract_list_json,
    policy_uri = EXCLUDED.policy_uri,
    extra_payload_json = EXCLUDED.extra_payload_json,
    synced_at = NOW(),
    updated_at = NOW();

INSERT INTO public.myd_contract_coverage (
    myd_contract_id,
    coverage_num,
    coverage_name,
    coverage_amount,
    currency_code,
    coverage_status,
    start_date,
    end_date,
    coverage_code,
    extra_payload_json
)
SELECT
    c.myd_contract_id,
    v.coverage_num,
    v.coverage_name,
    v.coverage_amount,
    'KRW',
    'NORMAL',
    DATE '2024-01-01',
    DATE '2034-12-31',
    v.coverage_code,
    '{"source":"seed","cohort":"104dashdemo"}'
FROM (
    VALUES
        ('104dashdemo01@example.com', '104DASHDEMO-0001', 'COV-104-0001', 'Death Benefit', 100000000, 'DEATH_GENERAL'),
        ('104dashdemo02@example.com', '104DASHDEMO-0002', 'COV-104-0001', 'Cancer Diagnosis', 30000000, 'CANCER_GENERAL'),
        ('104dashdemo02@example.com', '104DASHDEMO-0002', 'COV-104-0002', 'Actual Loss Medical', 5000000, 'ACTUAL_LOSS_MEDICAL'),
        ('104dashdemo03@example.com', '104DASHDEMO-0003', 'COV-104-0001', 'Brain Stroke', 20000000, 'BRAIN_STROKE'),
        ('104dashdemo03@example.com', '104DASHDEMO-0003', 'COV-104-0002', 'Surgery Benefit', 7000000, 'SURGERY_GENERAL'),
        ('104dashdemo03@example.com', '104DASHDEMO-0003', 'COV-104-0003', 'Accident Injury', 10000000, 'ACCIDENT_INJURY'),
        ('104dashdemo04@example.com', '104DASHDEMO-0004', 'COV-104-0001', 'Liability Shield', 100000000, 'LIABILITY_GENERAL'),
        ('104dashdemo04@example.com', '104DASHDEMO-0004', 'COV-104-0002', 'Cancer Diagnosis', 20000000, 'CANCER_GENERAL'),
        ('104dashdemo04@example.com', '104DASHDEMO-0004', 'COV-104-0003', 'Death Benefit', 50000000, 'DEATH_GENERAL'),
        ('104dashdemo04@example.com', '104DASHDEMO-0004', 'COV-104-0004', 'Accident Injury', 8000000, 'ACCIDENT_INJURY'),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'COV-104-0001', 'Death Benefit', 120000000, 'DEATH_GENERAL'),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'COV-104-0002', 'Cancer Diagnosis', 40000000, 'CANCER_GENERAL'),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'COV-104-0003', 'Brain Heart Care', 30000000, 'BRAIN_HEART_GENERAL'),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'COV-104-0004', 'Actual Loss Medical', 7000000, 'ACTUAL_LOSS_MEDICAL'),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'COV-104-0005', 'Surgery Benefit', 10000000, 'SURGERY_GENERAL'),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'COV-104-0006', 'Accident Injury', 12000000, 'ACCIDENT_INJURY'),
        ('104dashdemo05@example.com', '104DASHDEMO-0005', 'COV-104-0007', 'Liability Shield', 150000000, 'LIABILITY_GENERAL'),
        ('104dashdemo06@example.com', '104DASHDEMO-0006', 'COV-104-0001', 'Actual Loss Medical', 3000000, 'ACTUAL_LOSS_MEDICAL')
) AS v(email, insu_num, coverage_num, coverage_name, coverage_amount, coverage_code)
JOIN public.usr_user u
    ON u.email = v.email
JOIN public.myd_contract c
    ON c.user_id = u.user_id
   AND c.provider_code = '104DASHDEMO'
   AND c.insu_num = v.insu_num
ON CONFLICT (myd_contract_id, coverage_num) DO UPDATE
SET
    coverage_name = EXCLUDED.coverage_name,
    coverage_amount = EXCLUDED.coverage_amount,
    currency_code = EXCLUDED.currency_code,
    coverage_status = EXCLUDED.coverage_status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    coverage_code = EXCLUDED.coverage_code,
    extra_payload_json = EXCLUDED.extra_payload_json,
    updated_at = NOW();

INSERT INTO public.myd_contract_coverage (
    myd_contract_id,
    coverage_num,
    coverage_name,
    coverage_amount,
    currency_code,
    coverage_status,
    start_date,
    end_date,
    coverage_code,
    extra_payload_json
)
SELECT
    c.myd_contract_id,
    'COV-0002',
    '암수술비',
    5000000,
    'KRW',
    'NORMAL',
    DATE '2024-01-15',
    DATE '2034-01-14',
    'CANCER_SURGERY',
    '{"source":"seed","contract":"DEMO-0001"}'
FROM public.myd_contract c
JOIN public.usr_user u
    ON u.user_id = c.user_id
WHERE u.email = 'demo@example.com'
  AND c.insu_num = 'DEMO-0001'
ON CONFLICT (myd_contract_id, coverage_num) DO UPDATE
SET
    coverage_name = EXCLUDED.coverage_name,
    coverage_amount = EXCLUDED.coverage_amount,
    currency_code = EXCLUDED.currency_code,
    coverage_status = EXCLUDED.coverage_status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    coverage_code = EXCLUDED.coverage_code,
    extra_payload_json = EXCLUDED.extra_payload_json,
    updated_at = NOW();

INSERT INTO public.myd_contract_coverage (
    myd_contract_id,
    coverage_num,
    coverage_name,
    coverage_amount,
    currency_code,
    coverage_status,
    start_date,
    end_date,
    coverage_code,
    extra_payload_json
)
SELECT
    c.myd_contract_id,
    'COV-0003',
    '상해사망',
    50000000,
    'KRW',
    'NORMAL',
    DATE '2023-09-10',
    DATE '2028-09-09',
    'ACCIDENT_DEATH',
    '{"source":"seed","contract":"DEMO-0002"}'
FROM public.myd_contract c
JOIN public.usr_user u
    ON u.user_id = c.user_id
WHERE u.email = 'demo@example.com'
  AND c.insu_num = 'DEMO-0002'
ON CONFLICT (myd_contract_id, coverage_num) DO UPDATE
SET
    coverage_name = EXCLUDED.coverage_name,
    coverage_amount = EXCLUDED.coverage_amount,
    currency_code = EXCLUDED.currency_code,
    coverage_status = EXCLUDED.coverage_status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    coverage_code = EXCLUDED.coverage_code,
    extra_payload_json = EXCLUDED.extra_payload_json,
    updated_at = NOW();

INSERT INTO public.myd_contract_coverage (
    myd_contract_id,
    coverage_num,
    coverage_name,
    coverage_amount,
    currency_code,
    coverage_status,
    start_date,
    end_date,
    coverage_code,
    extra_payload_json
)
SELECT
    c.myd_contract_id,
    'COV-0004',
    '상해입원일당',
    30000,
    'KRW',
    'NORMAL',
    DATE '2023-09-10',
    DATE '2028-09-09',
    'ACCIDENT_HOSPITAL',
    '{"source":"seed","contract":"DEMO-0002"}'
FROM public.myd_contract c
JOIN public.usr_user u
    ON u.user_id = c.user_id
WHERE u.email = 'demo@example.com'
  AND c.insu_num = 'DEMO-0002'
ON CONFLICT (myd_contract_id, coverage_num) DO UPDATE
SET
    coverage_name = EXCLUDED.coverage_name,
    coverage_amount = EXCLUDED.coverage_amount,
    currency_code = EXCLUDED.currency_code,
    coverage_status = EXCLUDED.coverage_status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    coverage_code = EXCLUDED.coverage_code,
    extra_payload_json = EXCLUDED.extra_payload_json,
    updated_at = NOW();
