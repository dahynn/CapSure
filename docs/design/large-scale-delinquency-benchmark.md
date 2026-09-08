# 대용량 보험료 미납 배치 벤치마크

`largeScaleDelinquencyBenchmark`은 기본 `test`에서 제외된 opt-in Testcontainers 검증이다. 1,000·10,000개의 합성 계약과 미납 채권을 생성하고, 같은 실행 키로 1개 또는 2개의 caller가 처리한 시간을 기록한다. 실제 문자, 자동출금, PG, 운영 DB를 호출하지 않는다.

```sh
python3 /absolute/path/measurement_lock.py --timeout 0 -- \
  env JAVA_HOME=/path/to/jdk-21 GRADLE_USER_HOME=/tmp/codex-gradle \
  bash ./gradlew largeScaleDelinquencyBenchmark --no-daemon --rerun-tasks \
  -PlargeBenchmarkOutput=../docs/evidence/large-scale-result.json
```

각 scale/worker 구성은 1회 워밍업과 3회 측정으로 구성된다. 시간은 `System.nanoTime`으로 측정하며 Testcontainers 기동과 fixture 생성은 제외하고 배치의 DB 작업과 모의 독촉 호출은 포함한다. 결과 JSON에는 개별 표본, 실행·대상·독촉·상태 이력 수와 중복 차이를 보관한다.

처리 대상은 `ops_premium_delinquency_target` 행을 `FOR UPDATE SKIP LOCKED`로 선점한다. 같은 실행 키의 caller가 서로 다른 20개 단위 target을 처리할 수 있으며, 계약→채권 잠금 순서와 상태 이력 기록은 유지한다. target이 모두 완료될 때만 조건부로 실행을 `COMPLETED`로 전환한다.

10,000건 복구 시나리오는 2,021번째 모의 독촉에서 예외를 주입한다. 20건 chunk 기준 2,020건이 커밋된 상태를 확인하고 동일 run ID를 재개하여 target·독촉·상태 이력이 10,000건이고 중복 차이가 0인지 확인한다. 이는 제어된 트랜잭션 예외 검증이지 프로세스 강제 종료·DB 장애·재난 복구 검증이 아니다.
