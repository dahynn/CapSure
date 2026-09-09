# 보험금 심사 보조 PoC — Claim Review Copilot

## 목적

가상 암보험 상품 1개에서 담당자가 보험금 청구를 검토할 때 쓰는 **보험금 심사 보조 PoC**입니다. 이는 보험금 심사 AI나 자동 지급 기능이 아니며, 모델 연결보다 먼저 근거·개인정보·감사·사람 검토 경계를 코드로 고정합니다.

## 현재 강제하는 경계

- `TERMS_DOCUMENT`, `PRODUCT_SOURCE`만 AI 입력 근거로 허용합니다.
- 주민등록번호, 계좌번호 형식, 휴대전화 번호, 이메일 형식이 요청 문구나 근거 본문에 있으면 모델 호출 전에 거절합니다.
- `InsuranceAssistantGateway`는 내부망 모델 또는 별도 승인 공급자로 교체할 포트입니다. 기본 구현은 항상 차단하며 외부 HTTP 호출을 하지 않습니다.
- 감사 기록은 요청 ID, 정책 판정, 근거 ID, 기록 시각만 남깁니다. 프롬프트·근거 본문·개인정보는 기록하지 않습니다.
- 생성물은 `REVIEW_REQUIRED` 초안일 뿐이며, 보험 인수·지급·면책 같은 최종 판단을 확정할 수 없습니다.

## Claim Review Copilot 흐름

1. 청구 ID에 귀속된 증권 버전, 약관 hash, 심사 규칙의 조항 ID만 조회합니다.
2. 해당 약관 조항 3개 이하에서 요청 문구를 단순 검색해 확인할 근거를 선택합니다. hash가 다른 상품·약관 조항은 제외합니다.
3. 초안은 확인할 약관 조항, 필요 서류·누락 가능성, 추가 확인 질문, 근거 부족 여부와 현재 담당자 검토 상태를 반환합니다.
4. `manualReviewRequired=true`는 항상 유지됩니다. 지급 승인·거절·의학적 판단·청구 상태 변경은 수행하지 않습니다.
5. 담당자는 별도 검토 API에서 `CONFIRMED` 또는 `REJECTED` 상태만 기록할 수 있습니다. 어느 상태여도 청구 판단을 자동 확정할 수 없습니다.

관리자 역할만 접근 가능한 API는 `POST /api/v1/ops/claims/{claimId}/review-copilot/drafts`, `GET /api/v1/ops/claims/{claimId}/review-copilot/drafts/{requestId}/review`, `POST /api/v1/ops/claims/{claimId}/review-copilot/drafts/{requestId}/review`입니다. 이 경로는 기존 `/ops/**` 보안 규칙을 따르며, 기본 gateway가 차단된 환경에서는 `GATEWAY_BLOCKED`만 반환합니다. 테스트의 demo/test-double은 외부 모델이 아닙니다.

현재 고객 청구 화면은 고객용 흐름이므로, 이 PoC의 담당자 검토 화면을 추가하지 않습니다. 내부 담당자 UI와 권한은 운영 범위를 확정한 뒤 별도 구현합니다.

## 검증 범위

단위 테스트로 다른 상품·약관 근거 혼입 차단, 근거 부족 시 자동 판단 금지, 민감 입력의 gateway 미전달, 기본 gateway 차단, 감사 기록의 원문 미포함, 사람 검토 전 최종 판단 불가를 확인합니다.

## 한계와 운영 전제

이 구현은 규제 준수 완료나 실제 망분리 운영을 보장하지 않는 로컬 PoC입니다. 검토 상태는 DB에 영속화하지만, 이는 상태·담당자 ID·시각만 남기는 최소 기록입니다. 별도 이력 테이블의 위변조 방지, 보존 기간, 접근 이력, 개인정보 영향 평가와 담당자 승인 흐름은 운영 전 별도로 설계·검증해야 합니다.

## 로컬 시연

`ClaimAssessmentAssistantServiceTest`의 네 흐름으로 확인합니다.

- 같은 청구에 귀속된 조항만 선택하고, gateway 초안의 다른 상품 조항도 제거합니다.
- 필수 증빙 또는 약관 근거가 부족하면 `manualReviewRequired=true`와 확인 질문만 반환합니다.
- 전화번호 등 민감한 요청 문구는 gateway 호출 전에 차단합니다.
- 기본 gateway는 외부 egress를 차단합니다. 테스트의 `RecordingGateway`는 모델이 아닌 안전한 demo/test-double입니다.
