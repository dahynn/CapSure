# 대규모 합성 보험료 미납·결제 대사 벤치마크

`largeScaleDelinquencyBenchmark`은 기본 `test`에서 제외된 opt-in Testcontainers 검증이다. 보험료 미납 처리와 결제 불확실성 대사를 각각 1,000건으로 재현한 뒤, 10,000건을 1개 또는 2개 worker로 처리한다. 100건 테스트는 중복 방지와 동시성 정합성을 빠르게 확인하는 보조 검증이며 대규모 처리 근거로 사용하지 않는다.

이 결과는 통제된 개발 장비에서 합성 데이터로 실행한 처리량·정합성 측정이다. 실제 사용자 트래픽, 운영 용량, SLA를 입증하지 않으며 실제 문자, 자동출금, PG, 운영 DB를 호출하지 않는다.

```sh
python3 /absolute/path/measurement_lock.py --timeout 0 -- \
  env JAVA_HOME=/path/to/jdk-21 GRADLE_USER_HOME=/tmp/codex-gradle \
  bash ./gradlew largeScaleDelinquencyBenchmark --no-daemon --rerun-tasks \
  -PlargeBenchmarkOutput=../docs/evidence/large-scale-delinquency-result.json \
  -PlargeReconciliationBenchmarkOutput=../docs/evidence/large-scale-payment-reconciliation-result.json
```

## 보험료 미납 처리

각 scale/worker 구성은 1회 워밍업과 3회 측정으로 구성된다. 시간은 `System.nanoTime`으로 측정하며 Testcontainers 기동과 fixture 생성은 제외하고 배치의 DB 작업과 모의 독촉 호출은 포함한다. 결과 JSON에는 개별 표본, 실행·대상·독촉·상태 이력 수와 중복 차이를 보관한다.

처리 대상은 `ops_premium_delinquency_target` 행을 `FOR UPDATE SKIP LOCKED`로 선점한다. 같은 실행 키의 caller가 서로 다른 20개 단위 target을 처리할 수 있으며, 계약→채권 잠금 순서와 상태 이력 기록은 유지한다. target이 모두 완료될 때만 조건부로 실행을 `COMPLETED`로 전환한다.

10,000건 복구 시나리오는 2,021번째 모의 독촉에서 예외를 주입한다. 20건 chunk 기준 2,020건이 커밋된 상태를 확인하고 동일 run ID를 재개하여 target·독촉·상태 이력이 10,000건이고 중복 차이가 0인지 확인한다. 이는 제어된 트랜잭션 예외 검증이지 프로세스 강제 종료·DB 장애·재난 복구 검증이 아니다.

## 결제 불확실성 대사

실제 `JdbcPaymentRepository`, `JdbcPaymentReconciliationJobRepository`, `PaymentService`, `PaymentReconciliationBatchService` 경로를 사용한다. PostgreSQL에 `UNKNOWN` 상태의 주문과 결제 시도를 set-based SQL로 적재하고, 합성 PG 조회 결과를 `FAILED`로 돌려 불확실성을 해소한다. 여기서 `resolved`는 상태를 확정했다는 뜻이며 결제 성공을 뜻하지 않는다.

측정은 데이터 적재, 대사 workload, 결과 검증을 분리한다. 처리량은 `10,000 / workload 초`, p95는 개별 `PaymentService.reconcile` 호출의 wall-clock 지연시간으로 계산한다. 1개·2개 worker를 각각 1회 워밍업 후 3회 측정하고, 2개 worker 실행에서는 두 worker가 모두 실제 target을 처리했는지도 확인한다.

복구 시나리오는 100건 chunk를 사용하고 20개 chunk, 즉 2,000건이 커밋된 뒤 제어된 예외를 발생시킨다. 같은 DB·실행 키를 유지하고 새 배치 서비스 객체로 재개한 뒤 다음을 검증한다.

- 최종 주문 10,000건이 모두 `FAILED`로 확정됐는지
- 대사 작업 행과 지연시간 표본이 각각 10,000건인지
- 처리 누락과 결제 시도 중복이 0건인지
- 전체 건수에 대한 control total이 일치하는지

검증 중 예외가 발생하면 결과 JSON에 `status=FAIL`, 실패 예외 유형과 메시지를 남기고 테스트 자체도 실패시킨다. 성공 결과만 남도록 실패를 숨기지 않는다.

## 해석 한계

모든 수치는 실행 장비의 CPU·메모리, JVM 상태, Docker/Testcontainers, PostgreSQL 설정, HikariCP 크기의 영향을 받는다. 합성 PG는 네트워크 지연과 rate limit을 재현하지 않는다. 따라서 결과는 실제 저장소·서비스 경로가 10,000건에서 중복·누락 없이 동작하고 같은 DB 체크포인트에서 재개됨을 확인하는 개발 검증으로만 해석한다.
