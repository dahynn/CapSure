# 결제 정합성·정보 보호 반복 검증

## 목적과 실행 경계

2026-09-08 로컬 강화 작업입니다. 실제 PG 결제, 정기 카드 자동출금, 미납금 재결제, 실제 문자, 배포는 실행하지 않습니다. 기존 초회 결제의 승인 재전송 멱등성, 가상 수납·환급 후보, 미납·대사 복구를 검증합니다. `FAILED` 확정은 결제 성공이 아닙니다.

## 변경과 결정

| 관찰한 위험 | 변경 | 검증·트레이드오프 |
| --- | --- | --- |
| 다른 PG의 `APPROVING` 주문은 조회를 수행하지 않았어도 대사 완료로 집계 | `UNKNOWN`과 함께 미해결로 분류, 백오프 유지 | `otherProviderApprovingOrdersRemainUnresolved`; PG 자동 전환/재승인은 하지 않음 |
| 대사 예외 이력의 provider가 `FAKE`로 고정 | 해당 주문의 최근 시도 PG로 기록, 시도가 없으면 `UNKNOWN` | `inquiryFailureStoresOnlySafeTypeAndActualProvider` |
| 전역 예외의 stack/message와 운영 실패 원문에 SQL·PG 키·개인정보가 섞일 수 있음 | `SafeFailure`가 오류 코드와 원인 클래스만 기록. 순환 cause/과도한 깊이 제한 | `SafeFailureTest`, 복구·대사 통합 시험. 원문/stack 없이 request ID와 오류 유형으로 좁혀야 하므로 디버깅 상세도 감소 |
| 요청 URL·임의 요청 ID/Forwarded 값이 로그로 유입 | 쿼리 미기록, 종료 시 매칭 경로 패턴만 기록, UUID 요청 ID, 컨테이너 remote address 사용 | `RequestIdFilterTest`가 본문 로그·MDC·응답 헤더를 확인. START에는 경로가 없고 매칭 실패는 `[unmatched]` |
| 결제 응답에 화면이 사용하지 않는 PG 키 포함 | `attempts[].providerPaymentKey` JSON 필드 제외 | `PaymentPolicyIntegrationTest`; 승인 요청의 키와 서버 내부 저장은 유지. 다른 소비자는 이 응답 필드 제거를 확인해야 함 |
| 사용자/관리자 경계의 회귀 위험 | 계약·청구·결제 소유권, 익명/일반 사용자 운영 접근 및 인증 주체 우선 시험 추가 | DB 기반 소유권 시험과 Spring Security MockMvc 시험은 서로 다른 계층. 실배포 인증/침투시험을 대신하지 않음 |
| 감사 이벤트가 무기한 누적 | `audit.retention.days` 기준의 정리 서비스와 일일 배치 추가. 기본 비활성이며 명시적 환경변수 없이는 삭제하지 않음 | 보존 기간 경계·0일 설정 차단·배치 위임 테스트. 실제 법정 보존 기간과 보관소 분리는 운영 정책 확인 후 설정 |

## 반복 명령

필수: Python 3, JDK 21, Docker daemon, Gradle 의존성/`postgres:16-alpine` 이미지 접근. 기존 개발 DB가 아닌 Testcontainers의 임시 DB만 사용합니다. `.env`를 읽거나 환경 전체를 출력하지 않습니다.

저장소 루트에서 공통 잠금 스크립트의 실제 경로를 지정합니다.

```sh
python3 tools/verify-reliability.py all --lock /absolute/path/to/measurement_lock.py --timeout 0
```

- `regression`: 전체 백엔드 test(measurement 태그 제외)와 bootJar.
- `measure`: opt-in `reliabilityMeasurement`만 실행.
- `all`: 회귀·빌드·측정을 한 잠금 안에서 순차 수행. 작업자 1/2 비교를 다른 잠금으로 나누지 않습니다.
- 잠금 획득 실패는 exit 75. 임의로 우회하거나 잠금 파일을 지우지 않습니다. `--inside-lock`는 실행기 내부 인자이므로 직접 사용하지 않습니다.
- Docker 사전 확인 실패, Gradle 실패, 결과 XML 부재, skip, 측정 샘플 누락은 성공으로 인정하지 않습니다. 매번 새 `docs/evidence/reliability-*` 디렉터리에 raw log/XML/metadata/measurement/summary JSON을 만듭니다. 실패 결과도 보존합니다.
- 원본은 Git 제외입니다. metadata에는 기준 SHA, tracked diff 해시와 새 소스 해시를 기록합니다. 같은 HEAD여도 미커밋 코드가 다르면 다른 실험입니다.
- 집계기 자체 시험: `PYTHONDONTWRITEBYTECODE=1 python3 tools/test_verify_reliability.py`. XML 실패/skip 보존, 빈 결과 거부, 워밍업 제외를 확인합니다.

## 측정 설계

- PostgreSQL 16 Alpine, Hikari 풀 크기 8을 두 구성에서 동일하게 사용합니다. 풀 개선 전후 실험이 아닙니다.
- 각 표본은 합성 주문 40건, chunk 5, PG 조회마다 실제 `Thread.sleep(10)` 지연입니다. 외부 네트워크 호출이나 승인 호출은 없습니다.
- `mixed-provider-results`: 30건 `FAILED`, 10건 `UNKNOWN`. 미확정 건의 백오프를 유지합니다.
- `checkpoint-recovery`: 총 10건의 체크포인트 저장 후 예외로 중단, 실제 약 100ms 쉬고 서비스를 재생성해 같은 instance로 나머지 30건을 처리합니다. 작업자 1명은 2 chunk, 2명은 각 1 chunk 후 중단하여 중단 시점의 처리량도 같습니다.
- 작업자 1/2 구성의 순서를 반복마다 교차합니다. 구성×시나리오별 1회 워밍업 + 5회 측정, 총 24표본(워밍업 4, 집계 20)을 보관합니다.
- `System.nanoTime`으로 전체 처리 경과 시간, 실제 주입 대기, 재개 직후부터 완료까지, PG 호출별 실제 경과 시간을 기록합니다. DB 생성·합성 데이터 삽입·결과 assertion·완료 후 no-op 재실행 시간은 `elapsedMs`에 포함하지 않습니다.
- 40건 처리/상태/시도/대사 원장 수, 조회키당 한 번, 작업 집계 합계 일치, 완료된 같은 instance 재실행 시 추가 조회 0을 매 표본 확인합니다.
- 동일 코드의 실행 구성 비교입니다. 운영 코드의 속도 개선율, 초당 성공 결제 수, 금융사 RTO/SLA로 표현하지 않습니다. 5회 표본의 중앙값·범위만 기술 통계로 봅니다.

## 공식 기준과 좁은 통제 매핑

확인일: 2026-09-08. 「개인정보의 안전성 확보조치 기준」은 2026-07-01 시행, 개인정보보호위원회고시 제2026-9호를 확인했습니다. 아래는 기술적 참고 대응이며 개인정보처리자 해당 여부·처리 규모·민감정보·사업 성격에 따른 적용 판단과 법률 검토를 대신하지 않습니다.

| 공식 조항·가이드 | 현재 대응 근거 | 미충족·미확인 |
| --- | --- | --- |
| [제5조 접근 권한 관리](https://www.law.go.kr/LSW/admRulSideInfoP.do?admRulSeq=2100000281400&chrClsCd=010201&docCls=jo&joBrNo=00&joNo=0005&urlMode=admRulScJoRltInfoR): 업무상 최소 권한 등 | 사용자 소유권 조회, 관리자 RBAC; `InsuranceBoundarySecurityTest`, `MyDataSecurityTest`, 계약/청구 DB 시험 | 직원별 권한 부여·변경·말소 이력/보존, 계정 수명 관리, 인증 실패 접근 제한의 운영 적용은 이번 시험의 보장 범위 밖 |
| [제8조 접속기록 보관·점검](https://www.law.go.kr/LSW/admRulSideInfoP.do?admRulSeq=2100000281400&chrClsCd=010201&docCls=jo&joBrNo=00&joNo=0008&urlMode=admRulScJoRltInfoR): 보관·점검 계획·안전한 보관 | 요청 ID/경로 패턴/상태/실제 경과 시간, 운영 복구 원장의 행위자·사유·결과. 오류 원문 노출 방지 | 일반 HTTP 로그는 완전한 개인정보 접속 감사기록이 아님. 보존기간, 위변조 방지, 정기 검토·다운로드 점검, 접근 이력의 완결성 미검증 |
| [제11조 재해·재난 대비](https://www.law.go.kr/LSW/admRulSideInfoP.do?admRulSeq=2100000281400&chrClsCd=010201&docCls=jo&joBrNo=00&joNo=0011&urlMode=admRulScJoRltInfoR): 해당 규모·유형에서 대응절차와 백업/복구 계획 | 제어된 애플리케이션 중단·체크포인트 재개 시험 및 반복 명령 | 이 조항의 규모·유형 적용 여부 미판정. DB 백업 복원, 단전/재난, 프로세스 강제 종료, 운영 RPO/RTO 시험은 아님 |
| [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html): 민감 값 직접 기록 억제와 로그 입력 정제·검증 | SafeFailure, URL 값 제외, UUID 제한, 합성 SQL/키/이메일 미노출 테스트 | 다른 라이브러리·프록시·DB 로그, 과거 저장된 오류 원문, 원장 원본 데이터와 운영자 자유입력은 전수 정제한 것이 아님 |

## 알려진 한계와 후속 설계

1. **강제 종료 복구**: 현재 배치는 `RUNNING`인 같은 instance를 자동 인수하지 않습니다. 미확정 주문의 lease, high-water mark와 결과 저장 사이에서 프로세스가 죽는 경우의 안전한 재선점/총계 재구성이 필요합니다. chunk 완료 직후 예외 시험을 이 보장으로 확대 해석하지 않습니다.
2. **진단과 감사 분리**: 예외 원문을 제거한 대신 오류 코드·유형과 request ID를 남깁니다. 개인정보 접근 주체/대상/행위를 남기는 별도 감사 채널, 접근통제·보존 정책은 별도입니다. 현재 필터의 사용자 식별 시점과 프록시 신뢰 설정도 실배포에서 확인해야 합니다.
3. **실제 PG/수납**: Toss sandbox 승인·조회 어댑터는 있지만 이번 검증은 Fake/합성 연동입니다. 확정 실패 주문의 다른 카드 재결제, live 자동출금/환불은 추가 개발입니다.
4. **기여 구분**: 이번 코드는 사용자 승인 아래 AI가 작성·검증했습니다. 사용자 경험에서 나온 미납 문제 정의와 제품 결정은 별도로 설명하며, AI 작성분을 사용자 수동 구현 실적으로 자동 전환하지 않습니다.

## 감사로그 보존 정책의 실행 경계

- 기본값은 `AUDIT_RETENTION_ENABLED=false`, `AUDIT_RETENTION_DAYS=365`입니다. 기본 365일은 포트폴리오용 안전한 예시일 뿐, 법정 보존 기간이라고 주장하지 않습니다.
- 활성화할 환경에서만 `AUDIT_RETENTION_ENABLED=true`를 설정합니다. 배치는 `AUDIT_RETENTION_CRON`(기본 매일 03:30, `AUDIT_RETENTION_ZONE` 기본 `Asia/Seoul`)에 실행됩니다.
- 정리 대상은 `occurred_at`이 계산된 기준 시각보다 **엄격히 이전**인 이벤트뿐입니다. 기준 시각과 같은 이벤트는 다음 실행까지 남습니다.
- 현재 구현은 로컬·가상 서비스의 물리 삭제 정책입니다. 실제 운영에서는 규제상 보존기간, 법적 보존·분리 보관, 삭제 승인, 위변조 방지 체인 검증, 백업 보존 정책을 별도로 확정해야 합니다.

실측 결과와 실행별 원본 위치는 중앙 CapSure 개별 인계 문서에 기록합니다. 측정이 끝나기 전에는 수치를 채우지 않습니다.
