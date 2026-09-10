# 초회 보험료 외부 금융 인터페이스 계약

## 목적

CapSure의 채널계가 결제 승인 결과를 직접 해석하지 않도록 분리한다. 결제 코어는 주문 금액·상태·멱등성을 책임지고, 외부기관 어댑터는 기관별 전문 형식과 통신 실패를 흡수한다. 기본값은 `FAKE_PREMIUM_PAYMENT`이고, 로컬 환경변수로 `TOSS_PREMIUM_PAYMENT` 테스트 어댑터를 선택할 수 있다.

```text
채널계 PaymentController
        ↓ Idempotency-Key
결제 코어 PaymentService
        ↓ PremiumPaymentGateway port
전문·회로차단 JournaledPremiumPaymentGateway
        ↓
FakePremiumPaymentGateway 또는 TossPremiumPaymentGateway
        ↓
ifc_financial_message 요청·응답 전문 원장
```

## 어댑터 선택과 비밀정보

| 환경변수 | 의미 | 기본값 |
| --- | --- | --- |
| `PAYMENT_GATEWAY` | `fake` 또는 `toss` 선택 | `fake` |
| `TOSS_PAYMENTS_SECRET_KEY` | Toss 테스트 시크릿 키 | 없음 |
| `TOSS_PAYMENTS_BASE_URL` | Toss API 기준 URL | `https://api.tosspayments.com` |
| `TOSS_PAYMENTS_TIMEOUT` | 승인·조회 제한시간 | `10s` |
| `TOSS_PAYMENTS_ALLOW_LIVE_KEY` | 실키 사용 방어 해제 | `false` |
| `VITE_TOSS_CLIENT_KEY` | 브라우저 결제위젯 테스트 클라이언트 키 | 없음 |

시크릿 키가 없거나 HTTPS가 아닌 API 주소를 사용하면 Toss 어댑터를 시작하지 않는다. 기본 설정에서는 `test_` 접두 키만 허용하며, 키 값은 코드·Git·외부 전문 원장에 저장하지 않는다.

## 승인 전문 계약

| 구분 | 값 |
| --- | --- |
| 요청 종류 | `PREMIUM_PAYMENT_CONFIRM` / `OUTBOUND_REQUEST` |
| 응답 종류 | `PREMIUM_PAYMENT_CONFIRM` / `INBOUND_RESPONSE` |
| 상관관계 ID | `PAYMENT-CONFIRM:{orderNo}:{idempotencyKey}` |
| 업무 키 | 보험료 주문 번호 `orderNo` |
| 멱등 키 | 채널이 전달한 `Idempotency-Key` |
| 요청 필드 | 주문번호, 외부 결제 키, 금액, 통화, 멱등 키 |
| 응답 필드 | 승인 상태, 외부 거래 ID, 오류 코드, 메시지 |

동일 멱등 키는 `pay_attempt`에서 먼저 확인한다. 이미 생성된 시도가 있으면 외부기관에 전문을 다시 보내지 않고 기존 주문 상태를 반환한다.

Toss 승인에서는 브라우저가 돌려준 `orderId`와 `amount`를 서버의 `PaymentOrder.orderNo` 및 금액과 먼저 비교한다. 일치할 때만 `POST /v1/payments/confirm`에 서버 주문번호·금액·결제 키를 전송한다. 승인 성공 URL은 결제 인증 결과일 뿐 계약 활성화 근거로 사용하지 않는다.

## 조회·대사 계약

timeout 또는 통신 오류는 승인 실패로 단정하지 않고 `UNKNOWN`으로 남긴다. 기존 결제 대사 배치가 `PREMIUM_PAYMENT_INQUIRY` 요청·응답 전문을 남기며 외부 상태를 재조회한다. 따라서 승인 요청을 무작정 재시도해 이중 수납을 만들지 않는다.

## 회로 차단 규칙

- `UNKNOWN` 응답 또는 adapter 예외가 연속 3회면 30초 동안 새 승인 전문을 외부기관으로 보내지 않는다.
- 차단 중 요청은 `PAYMENT_INTERFACE_CIRCUIT_OPEN`과 `CIRCUIT_OPEN` 전문 상태로 남기고 주문을 `UNKNOWN` 대사 흐름에 맡긴다.
- 정상 승인 또는 명시적 거절 응답은 연속 timeout 수를 초기화한다.

V15부터 회로 상태는 PostgreSQL `ifc_payment_circuit_state`에 기관 인터페이스별로 저장한다. 짧은 행 잠금 트랜잭션으로 상태 전이를 직렬화하고, 외부기관 호출 중에는 DB 잠금을 잡지 않는다. 30초 뒤에는 한 요청만 복구 probe를 획득한다. probe 서버가 중단되면 2분 lease 후 다른 서버가 재시도할 수 있으며, 세대 번호로 이전 요청의 늦은 결과가 새 차단 상태를 덮어쓰지 못하게 한다. 여러 서버가 같은 DB를 사용하는 구성이 전제다. 기관별 외부 장애 알림은 아직 별도다.

## 원장 보존 경계

`ifc_financial_message`는 전문 본문 JSON, SHA-256 해시, interface 이름, 방향, 상관관계 ID, 멱등 키, 업무 키, 상태, 오류 코드, 시각을 저장한다. Toss 결제 키와 거래 ID는 전문 본문에 원문 대신 SHA-256 해시만 기록한다. 카드 번호, 계좌번호, 토큰, 시크릿 키 같은 비밀 정보는 저장하지 않는다.

## 범위

현재 범위는 Toss Payments 테스트 일반결제를 이용한 초회 보험료 승인·조회와 `PAYMENT_STATUS_CHANGED` webhook inbox다. 일반결제 webhook에는 서명 헤더가 없으므로 webhook 본문을 신뢰하지 않고 결제 조회 API로 재검증한다. 실결제, 자동결제 빌링키, 취소·환불은 포함하지 않는다.

공식 근거:

- [Toss Payments 결제 흐름](https://docs.tosspayments.com/guides/v2/get-started/payment-flow)
- [Toss Payments API 키](https://docs.tosspayments.com/reference/using-api/api-keys)
- [Toss Payments 결제 승인 API](https://docs.tosspayments.com/reference)
