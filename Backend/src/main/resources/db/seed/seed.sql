-- Minimal seed data for local login and smoke tests.
-- product_source bulk data is intentionally excluded here.

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
    'Demo User',
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
-- ---------------------------------------------------------------------------
INSERT INTO public.capsule_product (
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
VALUES
    (
        'CAPSULE-CANCER-001',
        'Capsule Cancer Starter',
        'CANCER',
        'CANCER_DIAGNOSIS',
        10000000,
        'KRW',
        9900,
        8900,
        30,
        'AVAILABLE',
        TRUE,
        'https://example.com/terms/cancer-starter',
        'v1',
        'Starter cancer protection capsule.'
    ),
    (
        'CAPSULE-ACCIDENT-001',
        'Capsule Accident Daily',
        'ACCIDENT',
        'ACCIDENT_INJURY',
        3000000,
        'KRW',
        4900,
        4500,
        30,
        'AVAILABLE',
        TRUE,
        'https://example.com/terms/accident-daily',
        'v1',
        'Daily accident protection capsule.'
    )
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
    subscription_status,
    billing_anchor_day,
    current_cycle_start_at,
    current_cycle_end_at,
    next_billing_at,
    expected_next_amount
)
SELECT
    u.user_id,
    'ACTIVE',
    5,
    TIMESTAMPTZ '2026-03-01 00:00:00+09',
    TIMESTAMPTZ '2026-03-31 23:59:59+09',
    TIMESTAMPTZ '2026-04-05 00:00:00+09',
    14800
FROM public.usr_user u
WHERE u.email = 'demo@example.com'
ON CONFLICT (user_id) DO UPDATE
SET
    subscription_status = EXCLUDED.subscription_status,
    billing_anchor_day = EXCLUDED.billing_anchor_day,
    current_cycle_start_at = EXCLUDED.current_cycle_start_at,
    current_cycle_end_at = EXCLUDED.current_cycle_end_at,
    next_billing_at = EXCLUDED.next_billing_at,
    expected_next_amount = EXCLUDED.expected_next_amount,
    updated_at = NOW();

INSERT INTO public.subscription_item (
    subscription_id,
    capsule_product_id,
    plan_version,
    item_status,
    coverage_amount_snapshot,
    monthly_price_snapshot,
    effective_start_at,
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
    TIMESTAMPTZ '2026-04-01 00:00:00+09'
FROM public.subscription s
JOIN public.usr_user u
    ON u.user_id = s.user_id
JOIN public.capsule_product cp
    ON cp.capsule_code = 'CAPSULE-CANCER-001'
WHERE u.email = 'demo@example.com'
ON CONFLICT (subscription_id, capsule_product_id, plan_version) DO UPDATE
SET
    item_status = EXCLUDED.item_status,
    coverage_amount_snapshot = EXCLUDED.coverage_amount_snapshot,
    monthly_price_snapshot = EXCLUDED.monthly_price_snapshot,
    effective_start_at = EXCLUDED.effective_start_at,
    editable_after_at = EXCLUDED.editable_after_at,
    updated_at = NOW();

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
    '["Demo User"]',
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
    '["Demo User"]',
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
