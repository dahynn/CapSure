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
3. 초안은 확인할 약관 조항, 필요 서류·누락 가능성, 추가 확인 질문, 근거 부족 여부와 현재 담당자 검토 상태를 반환합니다. 같은 구조화 초안은 담당자 재확인용으로만 저장하며, 고객 원문·프롬프트·모델 원문은 저장하지 않습니다.
4. `manualReviewRequired=true`는 항상 유지됩니다. 지급 승인·거절·의학적 판단·청구 상태 변경은 수행하지 않습니다.
5. 담당자는 별도 검토 API에서 `CONFIRMED` 또는 `REJECTED` 상태만 기록할 수 있습니다. 어느 상태여도 청구 판단을 자동 확정할 수 없습니다.

관리자 역할만 접근 가능한 API는 `POST /api/v1/ops/claims/{claimId}/review-copilot/drafts`, `GET /api/v1/ops/claims/{claimId}/review-copilot/drafts/{requestId}/review`, `POST /api/v1/ops/claims/{claimId}/review-copilot/drafts/{requestId}/review`, `GET /api/v1/ops/claims/{claimId}/review-copilot/drafts/{requestId}/review/history`, `GET /api/v1/ops/claims/review-copilot/reviews?status=DRAFT&limit=20`입니다. 이 경로는 기존 `/ops/**` 보안 규칙을 따르며, 기본 gateway가 차단된 환경에서는 `GATEWAY_BLOCKED`만 반환합니다. 테스트의 demo/test-double은 외부 모델이 아닙니다.

현재 고객 청구 화면은 고객용 흐름이므로, 이 PoC의 담당자 검토 화면을 추가하지 않습니다. 내부 담당자 UI와 권한은 운영 범위를 확정한 뒤 별도 구현합니다.

## 검증 범위

단위 테스트로 다른 상품·약관 근거 혼입 차단, 근거 부족 시 자동 판단 금지, 민감 입력의 gateway 미전달, 기본 gateway 차단, 감사 기록의 원문 미포함, 사람 검토 전 최종 판단 불가를 확인합니다.

## 포트폴리오용 검증 서술

### 한 줄 요약

**"보험금 지급을 자동화하지 않고, 청구에 귀속된 약관 식별자와 증빙 유형만으로 AI 심사 보조 초안을 생성한 뒤 담당자 검토를 강제하는 흐름을 구현·검증했습니다."**

### 문제 → 설계 → 검증 결과

| 관점 | 포트폴리오에 쓸 수 있는 검증된 내용 |
| --- | --- |
| 문제 | 보험금 청구에 AI를 바로 연결하면 고객 원문·의료 정보가 과도하게 전송되거나, 다른 상품의 약관이 근거로 섞일 수 있습니다. |
| 설계 | 모델에는 약관 본문·고객 식별 정보 대신 `sourceId`, 약관 버전, 필수/확인된 증빙 **유형**만 전달했습니다. 전화번호·이메일·계좌번호·주민등록번호 형식은 호출 전 차단하고, 모델 응답의 약관 ID도 청구별 허용 목록과 다시 대조합니다. |
| 판단 경계 | 모델 결과는 항상 `REVIEW_REQUIRED` 초안이며, 지급 승인·거절·의학 판단·청구 상태 변경 경로를 열지 않았습니다. 담당자는 확인/반려 이력만 남깁니다. |
| 자동 검증 | 다른 상품/버전 약관 제거, 증빙 부족 시 모델 미호출, 민감 문구 미전달, 기본 외부 호출 차단, 원문 미감사기록, 최종 판단 불가를 서비스 테스트로 검증했습니다. |
| 외부 연동 검증 | 로컬 테스트 환경에서 OpenAI Responses API를 실제 1회 호출해 `DRAFT` 초안을 생성하고 저장된 검토 조회까지 확인했습니다. 원본 API의 `output[].content[].output_text` 구조를 파싱하는 테스트와, 허용되지 않은 약관 ID를 제거하는 테스트를 추가했습니다. |

### 면접·포트폴리오 본문 예시

> 외부 AI를 단순히 연결하는 대신, 모델이 참고할 수 있는 근거를 청구별 약관 식별자와 증빙 유형으로 제한했습니다. 민감 형식의 운영자 입력은 모델 호출 전에 차단하고, 응답에 포함된 약관 ID도 서버에서 다시 허용 목록과 대조했습니다. 로컬 환경의 실제 Responses API 호출로 초안 생성·저장·조회까지 확인했으며, 반환값은 항상 담당자 검토가 필요한 보조 초안으로만 남도록 설계했습니다.

### 검증 시나리오와 근거

| 시나리오 | 기대 결과 | 확인 방법 |
| --- | --- | --- |
| 증빙이 부족한 청구 | 모델 호출 없이 `REVIEW_REQUIRED`, 누락 증빙과 질문만 생성 | `ClaimAssessmentAssistantServiceTest` |
| 전화번호 등 민감 문구 입력 | gateway 호출 전 `REJECTED_SENSITIVE_INPUT` | `ClaimAssessmentAssistantServiceTest` |
| 다른 상품 약관 ID가 모델 응답에 포함 | 청구의 허용 약관 ID만 초안에 유지 | `ClaimAssessmentAssistantServiceTest`, `OpenAiClaimAssessmentGatewayTest` |
| 외부 설정이 미완성 | 외부 HTTP 호출 없이 fail-closed | `ClaimReviewCopilotReadinessServiceTest`, 기본 gateway 테스트 |
| 실제 외부 연결 | `provider=external`, `ready=true` 확인 후 `DRAFT` 생성·검토 조회 | 2026-09-10 로컬 수동 연동 점검 |

### 검증 범위의 한계

- 실제 고객·의료정보, 실제 보험금 지급, 의료적 정확도, 지급/면책 판정 정확도를 검증한 것이 아닙니다.
- 로컬에서의 외부 호출 성공은 모델 품질·운영 가용성·규제 준수 완료를 의미하지 않습니다. 운영 전에는 평가 데이터셋, 담당자 승인 정책, 접근 통제, 보존 기간, 장애·비용 모니터링을 별도로 검증해야 합니다.
- 외부 호출 검증의 입력은 로컬 demo 청구에 연결된 최소 식별자와 증빙 유형이며, API 키·프롬프트 원문·응답 원문은 문서와 로그에 저장하지 않습니다.

### 화면 캡처 구성 제안

1. 운영 콘솔의 `DRAFT` 결과: 약관 식별자·누락 증빙·검토 필요 상태가 보이는 화면
2. 검토 이력 화면: 담당자 `CONFIRMED` 또는 `REJECTED` 기록이 남는 화면
3. readiness 응답: 키 값 없이 `provider=external`, `ready=true`만 표시되는 화면

캡처에는 API 키, 고객 이름·연락처, 실제 의료 문서 원문을 포함하지 않습니다.

## 한계와 운영 전제

이 구현은 규제 준수 완료나 실제 망분리 운영을 보장하지 않는 로컬 PoC입니다. 검토 상태와 상태 변경 이력은 DB에 영속화하지만, 상태·담당자 ID·시각만 남기는 최소 기록입니다. DB 권한 분리, 이력 위변조 방지, 보존 기간, 접근 이력, 개인정보 영향 평가와 담당자 승인 흐름은 운영 전 별도로 설계·검증해야 합니다.

## 로컬 시연

`ClaimAssessmentAssistantServiceTest`의 네 흐름으로 확인합니다.

- 같은 청구에 귀속된 조항만 선택하고, gateway 초안의 다른 상품 조항도 제거합니다.
- 필수 증빙 또는 약관 근거가 부족하면 `manualReviewRequired=true`와 확인 질문만 반환합니다.
- 전화번호 등 민감한 요청 문구는 gateway 호출 전에 차단합니다.
- 기본 gateway는 외부 egress를 차단합니다. 테스트의 `RecordingGateway`는 모델이 아닌 안전한 demo/test-double입니다.

## 외부 AI API 연결 전 설정

`Backend/.env.example`에 필요한 환경변수 이름만 정리해 두었습니다. 현재 기본값은 `CLAIM_REVIEW_COPILOT_PROVIDER=blocked`입니다. OpenAI Responses API 어댑터는 `CLAIM_REVIEW_COPILOT_ALLOW_EXTERNAL_CALLS=true`가 명시되기 전까지 fail-closed로 차단됩니다. API 키는 `.env`, 배포 시크릿 또는 운영 환경변수에만 넣고 채팅·코드·커밋에 넣지 않습니다.
