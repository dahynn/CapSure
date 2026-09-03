# 초회 보험료 외부 금융 인터페이스 계약

## 목적

CapSure의 채널계가 결제 승인 결과를 직접 해석하지 않도록 분리한다. 결제 코어는 주문 금액·상태·멱등성을 책임지고, 외부기관 어댑터는 기관별 전문 형식과 통신 실패를 흡수한다. 현재 기관은 `FAKE_PREMIUM_PAYMENT`이며, Toss Payments 또는 은행 sandbox는 같은 port를 구현하는 별도 adapter로 교체한다.

```text
채널계 PaymentController
        ↓ Idempotency-Key
결제 코어 PaymentService
        ↓ PremiumPaymentGateway port
전문·회로차단 JournaledPremiumPaymentGateway
        ↓
FakePremiumPaymentGateway (향후 Toss/은행 adapter)
        ↓
ifc_financial_message 요청·응답 전문 원장
```

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

## 조회·대사 계약

timeout 또는 통신 오류는 승인 실패로 단정하지 않고 `UNKNOWN`으로 남긴다. 기존 결제 대사 배치가 `PREMIUM_PAYMENT_INQUIRY` 요청·응답 전문을 남기며 외부 상태를 재조회한다. 따라서 승인 요청을 무작정 재시도해 이중 수납을 만들지 않는다.

## 회로 차단 규칙

- `FAKE_GATEWAY_TIMEOUT` 또는 adapter 예외가 연속 3회면 30초 동안 새 승인 전문을 외부기관으로 보내지 않는다.
- 차단 중 요청은 `PAYMENT_INTERFACE_CIRCUIT_OPEN`과 `CIRCUIT_OPEN` 전문 상태로 남기고 주문을 `UNKNOWN` 대사 흐름에 맡긴다.
- 정상 승인 또는 명시적 거절 응답은 연속 timeout 수를 초기화한다.

현재 회로 상태는 단일 애플리케이션 인스턴스 메모리에 있다. 다중 인스턴스 운영 시에는 Redis 등 공유 저장소와 기관별 장애 알림을 별도 도입해야 한다.

## 원장 보존 경계

`ifc_financial_message`는 전문 본문 JSON, SHA-256 해시, interface 이름, 방향, 상관관계 ID, 멱등 키, 업무 키, 상태, 오류 코드, 시각을 저장한다. 카드 번호, 계좌번호, 토큰, 실결제 키 같은 비밀 정보는 이 교육용 구현의 전문에 넣지 않는다.

## 범위

이 문서는 실제 Toss Payments·기업은행 API 명세가 아니다. 실제 기관 연결 전에는 해당 기관의 최신 공식 API·인증 방식·webhook 검증 규칙을 별도 계약 문서와 adapter 테스트로 확인한다.
