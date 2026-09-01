-- 실제 보험상품이 아닌 CapSure 교육용 가상 암보험 fixture.
-- 약관 원문: classpath:terms/demo-cancer-terms-v1.md

INSERT INTO public.ins_terms_document (
    document_code,
    document_version,
    title,
    source_type,
    source_uri,
    source_hash,
    effective_from,
    status,
    is_simulation
) VALUES (
    'CAPSURE-DEMO-CANCER-TERMS',
    '1.0.0',
    'CapSure 교육용 암보험 약관 v1',
    'SYNTHETIC_FIXTURE',
    'classpath:terms/demo-cancer-terms-v1.md',
    'c4ac2f41311096ba768d0550eecef4bdf21937ac3afb8ef8dad2bf5a6c03f04a',
    DATE '2026-09-01',
    'PUBLISHED',
    TRUE
);

INSERT INTO public.ins_terms_clause (
    terms_document_id,
    clause_code,
    title,
    body,
    sort_order
)
SELECT document.terms_document_id, clause.clause_code, clause.title, clause.body, clause.sort_order
FROM public.ins_terms_document document
CROSS JOIN (
    VALUES
        ('ARTICLE-01', '목적', '교육용 암보험 시뮬레이션의 가입·보장·청구 규칙을 정합니다.', 1),
        ('ARTICLE-02', '용어', '계약자, 피보험자, 수익자와 보장개시일의 의미를 구분합니다.', 2),
        ('ARTICLE-03', '상품 구성', '일반암 진단비, 유사암·소액암 진단비, 암 수술비 담보로 구성합니다.', 3),
        ('ARTICLE-04', '청약과 동의', '유효한 견적과 약관 문서 버전·해시 동의가 있어야 청약할 수 있습니다.', 4),
        ('ARTICLE-05', '계약 전 알릴 의무', '고지 답변 누락 또는 검토 조건은 자동 승인하지 않고 수동심사합니다.', 5),
        ('ARTICLE-06', '인수심사와 계약', '인수 승인과 초회 보험료 수납 후에만 가상 보험계약을 활성화합니다.', 6),
        ('ARTICLE-07', '일반 보장개시', '암 수술비 담보는 가상 보험계약 활성일부터 보장을 시작합니다.', 7),
        ('ARTICLE-08', '암 진단 담보의 보장개시', '암 진단 담보는 계약 활성일을 포함해 90일이 지난 다음 날부터 보장합니다.', 8),
        ('ARTICLE-09', '일반암 진단비', '보장개시 후 일반암 최초 진단 확정과 필수 증빙 충족 시 가입금액을 지급합니다.', 9),
        ('ARTICLE-10', '유사암·소액암 진단비', '보장개시 후 유사암·소액암 최초 진단 확정과 필수 증빙 충족 시 가입금액을 지급합니다.', 10),
        ('ARTICLE-11', '암 수술비', '보장개시 후 암의 직접 치료 목적 수술과 필수 증빙 충족 시 가입금액을 지급합니다.', 11),
        ('ARTICLE-12', '감액기간', '암 진단 담보는 보장개시일부터 365일 동안 가입금액의 50%를 지급합니다.', 12),
        ('ARTICLE-13', '진단확정 증빙', '합성 진단서와 합성 병리검사 결과가 부족하면 자동 부지급하지 않고 수동심사합니다.', 13),
        ('ARTICLE-14', '지급하지 않는 사유', '보장개시 전 사고, 동일 최초 진단비 기지급, 입력 위변조는 규칙에 따라 부지급할 수 있습니다.', 14),
        ('ARTICLE-15', '지급심사와 설명', '사고일 당시 계약·약관·규칙 버전으로 심사하고 결과·금액·사유·조항을 기록합니다.', 15)
) AS clause(clause_code, title, body, sort_order)
WHERE document.document_code = 'CAPSURE-DEMO-CANCER-TERMS'
  AND document.document_version = '1.0.0';

INSERT INTO public.ins_product_version (
    product_code,
    version,
    product_name,
    insurer_name,
    insurer_sector,
    sale_from,
    status,
    base_monthly_premium,
    currency_code,
    terms_document_id,
    is_simulation
)
SELECT
    'CAPSURE-DEMO-CANCER',
    '1.0.0',
    'CapSure 암케어 시뮬레이션',
    'CapSure Demo Insurance',
    'THIRD_INSURANCE',
    DATE '2026-09-01',
    'ON_SALE',
    29900.00,
    'KRW',
    document.terms_document_id,
    TRUE
FROM public.ins_terms_document document
WHERE document.document_code = 'CAPSURE-DEMO-CANCER-TERMS'
  AND document.document_version = '1.0.0';

INSERT INTO public.ins_coverage (
    coverage_code,
    coverage_name,
    coverage_category,
    benefit_type,
    description
) VALUES
    ('DEMO_GENERAL_CANCER_DIAGNOSIS', '일반암 진단비', 'CANCER', 'FIXED_BENEFIT', '합성 분류상 일반암 최초 진단을 보장하는 교육용 담보'),
    ('DEMO_MINOR_CANCER_DIAGNOSIS', '유사암·소액암 진단비', 'CANCER', 'FIXED_BENEFIT', '합성 분류상 유사암·소액암 최초 진단을 보장하는 교육용 담보'),
    ('DEMO_CANCER_SURGERY', '암 수술비', 'SURGERY', 'FIXED_BENEFIT', '합성 분류상 암의 직접 치료 목적 수술을 보장하는 교육용 담보');

INSERT INTO public.ins_product_coverage (
    product_version_id,
    coverage_id,
    insured_amount,
    waiting_period_days,
    reduction_period_days,
    reduction_rate,
    coverage_start_rule,
    display_order
)
SELECT product.product_version_id, coverage.coverage_id, fixture.insured_amount,
       fixture.waiting_period_days, fixture.reduction_period_days, fixture.reduction_rate,
       fixture.coverage_start_rule, fixture.display_order
FROM public.ins_product_version product
JOIN (
    VALUES
        ('DEMO_GENERAL_CANCER_DIAGNOSIS', 20000000.00::NUMERIC, 90, 365, 0.5000::NUMERIC, 'AFTER_WAITING_PERIOD', 1),
        ('DEMO_MINOR_CANCER_DIAGNOSIS', 4000000.00::NUMERIC, 90, 365, 0.5000::NUMERIC, 'AFTER_WAITING_PERIOD', 2),
        ('DEMO_CANCER_SURGERY', 1000000.00::NUMERIC, 0, 0, 1.0000::NUMERIC, 'POLICY_ACTIVATED_AT', 3)
) AS fixture(coverage_code, insured_amount, waiting_period_days, reduction_period_days, reduction_rate, coverage_start_rule, display_order)
    ON TRUE
JOIN public.ins_coverage coverage ON coverage.coverage_code = fixture.coverage_code
WHERE product.product_code = 'CAPSURE-DEMO-CANCER'
  AND product.version = '1.0.0';

INSERT INTO public.ins_coverage_rule (
    product_coverage_id,
    rule_version,
    rule_type,
    priority,
    rule_json,
    terms_clause_id,
    is_active
)
SELECT product_coverage.product_coverage_id,
       '1.0.0',
       'CLAIM_ELIGIBILITY',
       100,
       fixture.rule_json,
       clause.terms_clause_id,
       TRUE
FROM public.ins_product_coverage product_coverage
JOIN public.ins_coverage coverage ON coverage.coverage_id = product_coverage.coverage_id
JOIN (
    VALUES
        (
            'DEMO_GENERAL_CANCER_DIAGNOSIS',
            'ARTICLE-09',
            '{"diagnosisCategories":["DEMO_GENERAL_CANCER"],"requiredEvidence":["DEMO_DIAGNOSIS_CERTIFICATE","DEMO_PATHOLOGY_REPORT"],"firstDiagnosisOnly":true,"missingEvidenceResult":"MANUAL_REVIEW"}'::JSONB
        ),
        (
            'DEMO_MINOR_CANCER_DIAGNOSIS',
            'ARTICLE-10',
            '{"diagnosisCategories":["DEMO_MINOR_CANCER"],"requiredEvidence":["DEMO_DIAGNOSIS_CERTIFICATE","DEMO_PATHOLOGY_REPORT"],"firstDiagnosisOnly":true,"missingEvidenceResult":"MANUAL_REVIEW"}'::JSONB
        ),
        (
            'DEMO_CANCER_SURGERY',
            'ARTICLE-11',
            '{"diagnosisCategories":["DEMO_GENERAL_CANCER","DEMO_MINOR_CANCER"],"requiredEvidence":["DEMO_DIAGNOSIS_CERTIFICATE","DEMO_SURGERY_CERTIFICATE"],"directTreatmentRequired":true,"missingEvidenceResult":"MANUAL_REVIEW"}'::JSONB
        )
) AS fixture(coverage_code, clause_code, rule_json)
    ON fixture.coverage_code = coverage.coverage_code
JOIN public.ins_product_version product
    ON product.product_version_id = product_coverage.product_version_id
JOIN public.ins_terms_document document
    ON document.terms_document_id = product.terms_document_id
JOIN public.ins_terms_clause clause
    ON clause.terms_document_id = document.terms_document_id
   AND clause.clause_code = fixture.clause_code;
