# CapSure 실행·운영 점검표

이 문서는 **가상 암보험 서비스의 로컬·테스트 환경 점검표**다. 실결제, 실제 보험 판매, 실제 고객·의료정보 처리는 범위 밖이다.

## 1. 환경변수와 시크릿 경계

| 영역 | 기본값 | 테스트 시 전환 | 운영 경계 |
| --- | --- | --- | --- |
| 심사 보조 AI | `CLAIM_REVIEW_COPILOT_PROVIDER=blocked` | `external`과 모델·키·URL·명시적 허용 플래그를 모두 설정 | API 키는 코드·Git·대화에 남기지 않는다. 원문/개인정보는 전송하지 않는다. |
| 초회 보험료 | `PAYMENT_GATEWAY=fake` | Toss `test_` 시크릿 키와 브라우저 테스트 키를 별도 주입 | `live_` 키는 `TOSS_PAYMENTS_ALLOW_LIVE_KEY=true` 없이는 기동하지 않는다. |
| DB·인증 | 로컬 PostgreSQL·개발 JWT | 배포 시 `DB_*`, `JWT_SECRET` 주입 | 기본값·로컬 키를 운영 환경에 사용하지 않는다. |

`.env`는 로컬 전용이며 커밋하지 않는다. CI·배포 환경에는 저장소 변수 대신 플랫폼 시크릿을 사용한다.

## 2. 배포 전 확인

1. Flyway가 적용되는 빈 DB에서 앱을 기동한다. `flyway.clean-disabled=true`를 유지한다.
2. `GET /actuator/health`가 `UP`인지 확인한다.
3. 비인증 사용자는 `/api/v1/ops/**`에 접근할 수 없어야 하며, 관리자만 운영 대시보드와 심사 보조 대기열에 접근할 수 있어야 한다.
4. AI는 `readiness`가 준비되지 않으면 외부 호출을 하지 않아야 한다. 준비 상태 응답에는 키 값이 포함되면 안 된다.
5. Toss 테스트 결제는 서버가 주문번호·금액을 다시 대조한 뒤 승인 API를 호출해야 한다. 브라우저의 성공 복귀만으로 계약을 활성화하지 않는다.

## 3. 결제 실패·대사·webhook 복구

```text
승인 timeout/통신오류 → UNKNOWN 원장 → 대사 배치의 조회 API 재확인 → PAID/FAILED/UNKNOWN 확정
                                                           ↓
                                            반복 UNKNOWN은 운영 콘솔에서 사유와 함께 대사 실행
```

- 승인 요청은 동일 `Idempotency-Key`로 외부기관에 중복 전송하지 않는다.
- timeout은 실패로 단정하지 않는다. 승인 재시도 대신 조회·대사 흐름으로 보낸다.
- 3회 연속 timeout 뒤에는 circuit breaker가 새 승인 요청을 잠시 차단한다.
- Toss 일반결제 `PAYMENT_STATUS_CHANGED` webhook은 전송 ID를 inbox 멱등 키로 사용한다. 일반결제에는 webhook 서명이 없으므로, 본문의 상태를 확정값으로 쓰지 않고 같은 `paymentKey`로 Toss 조회 API를 다시 호출한다.
- Toss가 2xx를 받지 못하면 재전송하므로, 처리 실패 이벤트는 `FAILED`로 보관하고 같은 전송 ID의 재수신을 허용한다. 이미 `PROCESSED`인 같은 이벤트는 상태 전이를 반복하지 않는다.

## 4. 운영자 심사 보조 점검

1. 운영 콘솔에서 청구 ID와 약관·증빙 확인 목적만 입력해 초안을 만든다.
2. 약관 식별자·누락 증빙·추가 질문을 확인한다. 초안은 지급·거절 결정을 하지 않는다.
3. 담당자가 `검토 확인` 또는 `초안 반려`를 기록하고 이력을 확인한다.
4. 외부 AI가 실패하면 고객 정보나 진단 상세를 추가 입력하지 않고, 설정 상태를 확인한 뒤 같은 요청만 재시도한다.

## 5. 포트폴리오 캡처 후보

- 운영 콘솔의 결제 전문 타임라인: correlation ID·멱등 키·timeout·circuit 상태
- 결제 대사 카드: 처리/해결/미확정/실패와 관리자 복구 이력
- 심사 보조 대기열: 약관 근거·증빙 확인·담당자 검토 이력
- `/actuator/health`와 비인증 운영 API 거절 화면

공식 근거: [Toss 일반결제 webhook](https://docs.tosspayments.com/reference/using-api/webhook-events), [Toss webhook 재전송 정책](https://docs.tosspayments.com/en/webhooks), [Toss 결제 API와 멱등성](https://docs.tosspayments.com/en/api-guide).
