# CapSure 암보험 Ubiquitous Language v1

- 상태: 구현 전 합의 용어집
- 적용 범위: 가상 암보험 청약·수납·계약·지급심사 MVP
- 원칙: 코드, DB, API, 테스트, 화면에서 같은 개념에 같은 단어를 사용한다.

## 1. 혼동 금지 용어

| 용어 | 이 프로젝트의 뜻 | 혼동하면 안 되는 것 |
| --- | --- | --- |
| 수신 | 은행이 예금 등 고객 자금을 받는 업무 | 보험료 수납 |
| 수납 | 보험회사가 보험료를 받는 업무 | 보험금 지급 |
| 지급 | 승인된 보험금을 수익자에게 지급하는 업무 | PG 결제 승인 |
| 청약 | 계약자가 보험계약 체결을 신청하는 행위 | 계약 활성화 |
| 승낙 | 보험회사가 청약을 받아들이는 의사표시 | 결제 성공 |
| 인수심사 | 계약 전에 위험을 평가해 승인·수동심사·거절하는 과정 | 보험금 지급심사 |
| 지급심사 | 사고 후 약관과 증빙으로 보험금 지급 여부·금액을 판단하는 과정 | 인수심사 |
| 보험료 | 계약자가 보험회사에 납입하는 금액 | 보험금 |
| 보험금 | 보험사고 발생 시 약관에 따라 지급하는 금액 | 해지환급금 |

## 2. 보험 상품·계약

| 한글 | 코드 권장어 | 정의 |
| --- | --- | --- |
| 보험상품 | Product | 위험 보장 규칙과 대가인 보험료를 묶은 계약 설계 |
| 상품 버전 | ProductVersion | 특정 판매기간에 적용되는 변경 불가능한 상품 정의 |
| 주계약 | MainCoverage | 상품의 기본 계약을 이루는 보장 |
| 특약 | Rider | 주계약에 추가되는 선택적 계약 조항 |
| 담보 | Coverage | 특정 사고와 지급조건을 정의하는 보장 단위 |
| 보험계약자 | Policyholder | 보험계약을 체결하고 보험료 납입 의무를 지는 사람 |
| 피보험자 | Insured | 보험사고의 대상이 되는 사람 |
| 보험수익자 | Beneficiary | 보험금을 받을 권리가 있는 사람 |
| 청약 | Application | 계약 체결을 요청하고 고지·동의를 포함한 신청 |
| 고지사항 | Disclosure | 인수 판단을 위해 질문받은 위험 관련 사실과 답변 |
| 인수결정 | UnderwritingDecision | `APPROVED`, `MANUAL_REVIEW`, `DECLINED` 결과 |
| 보험계약 | Policy | 승낙된 청약과 보험료 수납에 따라 발행된 계약 원장 |
| 계약 버전 | PolicyVersion | 변경 시점별 계약 전체 snapshot |
| 보험증권 | PolicyDocument | 계약 성립과 주요 내용을 증명하는 문서 표현 |
| 보장개시일 | CoverageStartDate | 특정 담보의 책임이 시작되는 날짜 |
| 면책기간 | WaitingPeriod | 약관상 특정 보장을 하지 않는 기간 |
| 감액기간 | ReductionPeriod | 약관상 보험금을 줄여 지급할 수 있는 기간 |
| 실효 | Lapse | 보험료 미납 등 약관상 사유로 계약 효력이 상실된 상태 |
| 부활 | Reinstatement | 일정 조건으로 실효 계약의 효력을 회복하는 절차 |
| 해지 | Cancellation/Termination | 계약자의 요청 또는 약관상 사유로 계약을 종료하는 행위 |

`CoverageStartDate`, 면책기간, 감액기간의 관계는 상품별 약관에 따라 다르므로 하나의 보편 공식으로 하드코딩하지 않는다.

## 3. 암보험·지급심사

| 한글 | 코드 권장어 | 정의 |
| --- | --- | --- |
| 정액형 | FixedBenefit | 실제 치료비와 무관하게 약정된 금액을 지급하는 방식 |
| 실손형 | Indemnity | 실제 발생한 손해 범위에서 보상하는 방식 |
| 일반암 | GeneralCancer | 선택한 약관 버전에서 일반암으로 정의된 분류 |
| 유사암·소액암 | MinorCancer | 선택한 약관 버전에서 별도 금액으로 정의된 암 분류 |
| 진단확정 | ConfirmedDiagnosis | 약관이 요구하는 방법과 증빙으로 진단이 확정된 상태 |
| 보험사고 | InsuredEvent | 약관상 보험금 지급 가능성을 발생시키는 사건 |
| 사고일 | IncidentAt | 지급심사에서 계약·약관 버전을 선택하는 기준 사건 시각 |
| 보험금 청구 | Claim | 계약자·피보험자·수익자가 보험금 지급을 요청하는 업무 객체 |
| 청구 증빙 | ClaimEvidence | 지급조건 확인에 필요한 합성 진단·검사 metadata |
| 지급결정 | ClaimDecision | `APPROVED`, `MANUAL_REVIEW`, `DENIED`와 금액·근거 |
| 수동심사 | ManualReview | 규칙이나 증빙만으로 자동 확정할 수 없어 담당자 판단이 필요한 상태 |
| 부지급 | Denied | 약관상 지급조건을 충족하지 않는다는 확정 결정 |
| 지급 사유 코드 | ReasonCode | 결정 이유를 기계적으로 재현하는 안정된 코드 |
| 조항 근거 | ClauseCitation | 지급결정이 참조한 약관 문서 버전과 조항 ID |

`MANUAL_REVIEW`와 `DENIED`를 같은 의미로 사용하지 않는다. 증빙 부족과 규칙 불확실성은 원칙적으로 자동 부지급 사유가 아니다.

## 4. 결제·수납

| 한글 | 코드 권장어 | 정의 |
| --- | --- | --- |
| 결제 주문 | PaymentOrder | 업무상 받아야 할 금액·목적·대상을 나타내는 내부 원장 |
| 결제 시도 | PaymentAttempt | PG에 한 번 요청한 외부 호출 단위 |
| 결제 인증 | PaymentAuthentication | 고객이 결제수단 사용을 인증한 단계 |
| 결제 승인 | PaymentConfirmation | 서버가 주문과 금액을 검증한 뒤 PG에 승인 요청하는 단계 |
| 멱등성 키 | IdempotencyKey | 같은 요청의 반복 처리를 한 번의 결과로 수렴시키는 키 |
| 웹훅 | WebhookEvent | 외부 PG가 결제 상태 변경을 비동기로 알리는 이벤트 |
| 미확정 | UNKNOWN | 외부 성공 여부를 로컬에서 확정하지 못한 상태 |
| 대사 | Reconciliation | 내부 원장과 외부 PG 상태를 비교해 불일치를 확정·기록하는 작업 |
| 수납 회차 | BillingCycle | 계약이 보험료를 납입해야 하는 주기 식별자 |
| 가상 지급 원장 | ClaimPaymentLedger | 실제 송금 없이 지급 결과를 시뮬레이션하는 내부 기록 |

`PaymentOrder`와 `PaymentAttempt`를 합치지 않는다. 주문은 업무 의도이며 시도는 네트워크 호출이다.

## 5. 시스템 계층·인터페이스

| 용어 | 정의 | CapSure 적용 |
| --- | --- | --- |
| 채널계 | 고객·설계사·관리자 접점 | React Web, admin 화면 |
| 기간계/Core | 계약과 돈의 기준 상태를 소유 | Application, Policy, Payment, Claim |
| 인터페이스계 | 내부·외부 시스템의 형식·프로토콜 연결 | Toss adapter, 향후 bank adapter |
| 정보계 | 통계·조회·분석을 위한 파생 데이터 | dashboard, timeline read model |
| 운영·통제계 | 배치·재시도·대사·감사 | job, outbox, DLQ, audit |
| MCI | 여러 채널 요청을 공통 형식으로 통합 | API facade 역할 |
| EAI/ESB | 내부 시스템 메시지와 데이터 연계 | 모듈 간 이벤트 역할 |
| FEP | 외부기관 전문·프로토콜 연계 | PG/bank adapter 역할 |
| ACL | 외부 모델이 코어 도메인을 오염시키지 않게 번역하는 계층 | Toss DTO ↔ Payment domain 변환 |

보험사마다 실제 명칭과 경계가 다르므로 CapSure 문서에서는 `대표 역할`이라고 표현한다.

## 6. DDD

| 용어 | 정의 | 예시 |
| --- | --- | --- |
| Bounded Context | 한 모델과 용어가 일관되게 유효한 경계 | Policy, Payment, Claim |
| Aggregate | 하나의 트랜잭션에서 불변식을 지키는 변경 경계 | PaymentOrder와 그 상태 |
| Aggregate Root | 외부 변경이 통과해야 하는 aggregate 진입점 | InsuranceApplication |
| Invariant | 어떤 요청 순서에도 반드시 유지할 규칙 | `PAID` 없이 Policy ACTIVE 금지 |
| Value Object | 식별자보다 값과 제약이 중요한 객체 | Money, DocumentHash |
| Domain Event | 이미 발생한 도메인 사실 | `PolicyActivated` |
| Port | 코어가 요구하는 외부 기능의 인터페이스 | PremiumPaymentGateway |
| Adapter | Port를 특정 기술·사업자에 연결한 구현 | TossPaymentsAdapter |
| Read Model | 화면·통계를 위해 최적화한 파생 조회 | CustomerTimelineProjection |

## 7. 배치·장애 처리

| 용어 | 정의 | 완료 판단 |
| --- | --- | --- |
| Job | 하나의 완결된 배치 실행 정의 | CancerCatalogPublishJob |
| Job Instance | 식별 가능한 논리 실행 | source checksum + rule version |
| Job Execution | 한 instance의 실제 실행 시도 | 시작·종료·결과 기록 |
| Step | Job 안의 독립 처리 단계 | validate, normalize, publish |
| Chunk | 읽기·처리·쓰기와 commit의 묶음 | 설정값 100부터 시작 |
| Checkpoint | 재시작 위치를 저장한 상태 | 실패 지점 이후 재개 |
| Restart | 실패한 instance를 중복 없이 이어서 실행 | canonical 중복 0 |
| Retry | 일시적 기술 오류를 같은 입력으로 다시 시도 | 최대 횟수와 backoff 기록 |
| Skip | 정책상 특정 오류 행을 제외하고 계속 처리 | reason code 필수 |
| Quarantine | 자동 반영하지 못한 행의 격리 저장소 | 원본 hash와 재처리 상태 |
| Control Total | 입력·출력·오류의 건수와 금액 대사 | input = accepted + duplicate + quarantine |
| DLQ | 자동 재시도 한도를 넘긴 이벤트 보관 | 수동 replay 가능 |
| Transactional Outbox | 코어 변경과 발행 대상을 같은 DB 트랜잭션에 저장 | 상태와 이벤트 유실 0 |
| Poison Message | 반복 실패하는 특정 이벤트 | 자동 무한 재시도 금지 |

## 8. 상태 표현 규칙

- DB와 API enum은 영어 대문자 snake case를 사용한다.
- 화면은 의미가 드러나는 한국어를 사용한다.
- 상태명과 이벤트명을 섞지 않는다.
  - 상태: `ACTIVE`
  - 이벤트: `PolicyActivated`
- `FAILED`는 기술 실패, `DENIED`는 지급심사 부지급처럼 문맥을 구분한다.
- `UNKNOWN`은 실패의 동의어가 아니다.
- 상태 변경에는 `occurredAt`, `actor`, `reasonCode`, `requestId`를 남긴다.

## 9. 금지 표현

구현과 법적 근거가 없는 상태에서 다음 문구를 사용하지 않는다.

- 보험 가입이 완료되었습니다.
- 지금부터 모든 암이 보장됩니다.
- 보험금 지급이 보장됩니다.
- AI가 지급 가능 여부를 판정했습니다.
- 은행급 또는 금융권 수준을 달성했습니다.
- 장애가 절대 발생하지 않습니다.

대신 다음처럼 표현한다.

- 가상 암보험 청약 시뮬레이션이 완료되었습니다.
- 담보별 보장개시일과 지급조건을 확인해 주세요.
- 입력한 합성 사례는 지급심사 규칙상 다음 결과로 분류되었습니다.
- 중복·타임아웃 시나리오를 자동화 테스트로 검증했습니다.

## 10. 구현 중 용어 변경 절차

1. 새 용어가 필요한 이유와 기존 용어 충돌을 ADR에 기록한다.
2. 도메인 코드, DB, API, 테스트, 화면 문구의 영향을 찾는다.
3. 용어집을 먼저 수정한다.
4. 한 기능 단위로 변경하고 테스트한다.
5. 과거 데이터와 API 호환이 필요하면 translation layer를 둔다.
