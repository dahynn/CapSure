package com.capsule.insurance.assistantai.domain;

/**
 * AI 초안에 사용할 수 있는 근거의 출처 유형입니다.
 * 고객·계약·청구 원장처럼 개인정보가 포함될 수 있는 원천은 의도적으로 포함하지 않습니다.
 */
public enum InsuranceEvidenceType {
    TERMS_DOCUMENT,
    PRODUCT_SOURCE,
    CLAIM_RECORD,
    CUSTOMER_PROFILE
}
