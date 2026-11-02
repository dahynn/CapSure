# CapSure 잔여 개발 및 로컬 운영 리허설 — 2026-09-07

## 작업 경계

리뷰 브랜치 fix/capsure-code-review-hardening에서 회원가입·세션·대시보드·배포 설정의 미완성 연결을 보완한다. SMS 실제 발송은 사용자 요청으로 제외한다. 외부 문자·메일·결제·환불·송금·원격 배포는 이번 검증에서 실행하지 않는다.

## 구현

- V14: PostgreSQL 기반 refresh 세션, 만료 토큰 차단, 이메일 일회성 검증, 사용자별 감사 읽음 위치. 이전 메모리 세션은 이전할 수 없으므로 적용 후 재로그인이 필요하다.
- Refresh는 토큰 해시 조건부 UPDATE로 회전한다. 여러 인스턴스가 같은 refresh를 동시에 사용하면 1건만 성공한다. 만료 상태는 주기적으로 정리한다. 서명키도 인스턴스 간 같은 JWT_SECRET으로 설정해야 한다.
- 이메일 인증: BCrypt 코드 해시, 3분 만료, 수신 주소별 60초 재발송 제한, 최대 5회 오답, 인증 후 30분 내 가입. 인증 소비와 계정 생성은 같은 트랜잭션이며 삽입 실패 시 소비도 롤백된다.
- 인증 성공 시 해당 이메일에 묶인 30분 유효 서명 증명을 발급하고, 가입 시 그 증명과 DB의 일회성 완료 상태를 함께 확인한다. 이메일 주소만 알고 다른 사람의 인증 완료 상태를 소비할 수 없다. 인증 증명은 ACCESS/REFRESH 용도로 사용할 수 없고 브라우저 컴포넌트 메모리에만 보관한다. 기존 가입 API 클라이언트도 emailVerificationToken 필드를 전달해야 한다.
- 기존 기본 메일 빈이 SMTP 자동 설정을 가릴 수 있어 제거했다. 실제 SMTP 설정이 없으면 성공을 가장하지 않고 기능 사용 불가를 반환한다. SMS 발송 호출은 주석 상태이고 발송·검증 API는 503을 반환한다.
- 프론트는 이메일 전송·확인·가입 및 이름 입력을 연결했다. 전화번호 입력은 본인인증 완료를 의미하지 않는다. 비밀번호·생년월일·필드 길이를 검증한다.
- 수동 세션 연장은 존재하지 않는 API 대신 자동 갱신과 같은 refresh 경로를 사용한다. 화면 타이머는 JWT 만료를 따른다. 같은 탭의 요청을 합치고 Web Locks 지원 브라우저에서는 탭 간 회전도 직렬화한다. 일시적 503은 세션을 지우지 않으며 늦은 응답이 새 로그인을 덮어쓰지 않는다.
- 대시보드 summary는 로그인 사용자의 활성 구독, 향후 7일 내 갱신 예정, 아직 확인하지 않은 감사 이벤트를 조회한다. 기존 subscription 모델의 집계이며 가상 암보험 ins_policy 수와 혼합하지 않는다. 감사 확인 POST /dashboard/audits/read는 해당 사용자의 읽음 위치만 전진시킨다. 사용자 1번 fallback은 제거했다.
- V15: 결제 circuit 상태를 기관 인터페이스별 PostgreSQL 행으로 공유한다. 외부 호출 전후의 짧은 트랜잭션만 사용하고 30초 차단 후 1건의 복구 probe, 2분 lease와 세대 번호를 적용한다. 다른 서버의 늦은 성공 응답은 새 차단을 해제하지 못한다. 운영 상태 조회는 읽기 전용이며 상태 행을 생성하지 않는다.
- 비활성화하거나 존재하지 않는 API 경로를 일반 예외가 500으로 바꾸던 문제를 수정해 404로 응답한다.

## 배포 설정

- main은 8080, mock은 8081 및 정확한 Spring 프로필·JDBC 설정을 사용한다. 새 PostgreSQL 볼륨에서만 기존 스키마 초기화 스크립트가 실행되고 이후 Flyway가 추가 변경을 적용한다. 기존 DB에 schema.sql을 수동 실행하면 안 된다.
- Frontend 이미지의 Nginx는 동일 출처 API를 backend:8080으로 전달한다. /dashboard 및 하위 SPA 주소는 HTML로, /dashboard/home·summary·audits는 API로 구분한다. Docker DNS를 통해 재생성된 backend 주소도 다시 조회한다.
- 과거 도메인·인증서 경로는 이미지에서 제거했다. 기본 Compose는 프론트를 127.0.0.1:58080에만 공개한다. 실제 도메인에서 운영하려면 신뢰할 수 있는 HTTPS ingress를 앞에 연결해야 한다. HTTPS 없이 로그인 서비스를 인터넷에 공개하지 않는다.
- main Compose 필수 값: DOCKER_USER, POSTGRES_PASSWORD, JWT_SECRET(최소 32바이트), MYDATA_MOCK_BASE_URL. 이미지 버전은 IMAGE_TAG로 선택한다. 실제 SMTP 연결은 SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD를 주입한다. 값은 문서나 Git에 저장하지 않는다.
- Toss 기본값은 fake다. 테스트 위젯을 이미지에 포함하려면 공개 클라이언트 키 VITE_TOSS_CLIENT_KEY를 빌드 인자로, 서버 비밀키는 실행 환경에만 주입한다. Docker의 KEY 이름 경고는 이 공개 클라이언트 키에 대한 것이며 비밀키를 빌드 인자로 넘기면 안 된다.
- .env 파일은 Docker context 및 Gradle resources에서 제외했다. Nginx 접근 로그도 paymentKey가 포함될 수 있는 URL query를 기록하지 않는다. 운영 프로필은 Swagger를 비활성화하고 기본 인증용 임시 암호 생성도 제거했다.
- 선택적 호스트 모니터링 컨테이너는 monitoring 프로필로 분리했다. SMTP 건강 상태는 보험 API의 readiness를 막지 않으며 실제 발송 경로는 별도로 점검해야 한다.

## 검증 체크포인트

- 최종 18:17 KST: BE 전체 151 tests, failures 0, errors 0, skipped 0. FE 10 tests와 production build 통과. 마지막 보완의 초기 실행에서 AuthControllerTest의 새 mock 의존성 누락 1건을 확인·수정한 뒤 전체를 다시 통과시켰다.
- 17:43 KST: 백엔드 전체 145 tests, failures 0, errors 0, skipped 0. 프론트 10 tests와 production build 통과.
- backend/frontend Docker 이미지 빌드 성공. main/mock Compose 설정 검사 성공.
- 격리 Docker 프로젝트에서 새 DB 초기화와 Flyway V1~V14, main·mock·frontend healthy를 확인했다.
- Nginx 경유 health 200, SPA /dashboard 및 /dashboard/diagnosis-report 200 HTML, 비인증 summary/profile 401 JSON.
- 브라우저 이메일 발송·확인→회원가입 성공→로그인→온보딩 이동을 로컬 합성 SMTP와 실제 애플리케이션 API로 확인했다. 프로필과 summary는 200, 새 사용자의 집계는 0·0·0이었다.
- DB를 보존한 채 main/mock 두 컨테이너를 재생성했다. V14→V15 적용, 두 서버 healthy, 재시작 전 refresh의 갱신 200·토큰 회전, 이전 refresh 재사용 401, 갱신한 access의 프로필 200, 감사 읽음 200을 확인했다.
- 로그아웃 200 후 해당 access 및 refresh는 모두 401이었다. SMS 전송 API는 실제 발송 없이 503이었다.
- 모바일 가입 화면 390×844에서 가로 넘침 없음, agent-browser page errors 0. 연도 선택 및 하단 제출 버튼은 스크롤 후 정상 동작했다.
- 패키징된 JAR의 .env 파일 0건, Nginx SPA 요청 로그에서 합성 paymentKey query 문자열 0건을 확인했다. 최종 FE 10 tests·build 재통과. 외부 발송·실결제는 하지 않았다.
- 마지막 인증 증명 보완 후 BE/FE 이미지를 다시 빌드·기동하고 합성 SMTP+실제 API로 재검증했다. 발송 200, 인증·증명 발급 200, 증명 누락 가입 400, 다른 이메일에 증명 사용 401, 정상 가입 200, 로그인 200이었다. 비활성 OpenAPI 주소는 404다. 이 마지막 확인은 브라우저 내 API 호출이며 UI 전체 입력 흐름의 재반복과 구분한다.
- 종료 정리: 자체 Docker 프로젝트 capsure-review-20260907의 컨테이너 5개·네트워크·합성 DB 볼륨을 제거하고 capsure-followup 브라우저를 닫았다. 합성 DB 내용은 폐기했으며 같은 테스트 환경을 새로 생성해 재검증할 수 있다. 사용자 기존 capsule-postgres-local DB는 보존했다.

## 커밋 및 적용 주의

- aa36565: 공유 인증 저장소·이메일 일회성 검증·실제 대시보드 집계.
- 9b05d43: 이메일 가입 UI·수동/자동 세션 갱신 통합.
- 1e9c9d7: 공유 결제 circuit·동시 복구 시도 통제.
- c1bfe6e: Docker·Nginx·Compose·운영 프로필 및 환경파일 제외.
- cc460cb: 가입 이메일 인증 증명·용도 검증 및 잘못된 경로 404 처리.
- 51062ee: 프론트 가입 요청에 인증 증명 연결.
- 작업 브랜치는 fix/capsure-code-review-hardening이며 기존 리뷰 커밋 위의 로컬 변경이다. push·PR·외부 배포하지 않았다.
- V14/V15 적용은 정상 앱 기동 시 Flyway가 수행한다. 기존 사용자 DB에는 이번 테스트를 실행하지 않았다. 이전 메모리 refresh 세션은 옮길 수 없으므로 최초 적용 시 재로그인이 필요하다.

## 남는 외부 확인

공개 도메인·TLS·실제 SMTP 수신, 실환경 백업·복구·알림은 별도 환경 확인이 필요하다. 실결제·자동결제·실제 환불·송금은 기존 범위 밖이다. SMS는 사용자 요청에 따라 보류한다. 이번 작업은 코드 및 로컬 리허설이며 공개 운영 완료가 아니다.

근거: [Spring Boot 3.5 메일 자동 설정](https://docs.spring.io/spring-boot/3.5/reference/io/email.html), [Nginx proxy_pass URI 처리](https://nginx.org/en/docs/http/ngx_http_proxy_module.html).
