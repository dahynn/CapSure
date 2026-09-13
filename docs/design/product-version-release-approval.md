# 상품·약관 버전 출시 승인 통제

## 목적

상품의 판매 상태가 `ON_SALE`이고 판매 기간에 들어왔다는 사실만으로 신규 견적에 노출하지 않습니다. 상품·약관 변경안은 별도의 출시 승인을 통과해야 하며, 누가 어떤 근거로 승인하거나 반려했는지 변경 불가능한 이벤트 원장으로 설명할 수 있어야 합니다.

## 불변식

1. 새 상품 버전은 기본적으로 `PENDING_APPROVAL`입니다.
2. 신규 상품 조회와 견적 발행은 `ON_SALE`이면서 `APPROVED`인 버전만 허용합니다.
3. `PENDING_APPROVAL` 버전만 한 번 승인 또는 반려할 수 있습니다. 같은 버전의 판단은 덮어쓰지 않고, 수정이 필요하면 새 버전을 만듭니다.
4. 승인·반려 시점의 상품 코드/버전과 약관 문서 ID/버전, 행위자, 사유, 시각을 이벤트에 고정합니다.
5. 과거 계약은 `ins_policy_version.snapshot_json`과 당시 `product_version_id`를 계속 사용하며, 새 버전의 승인·반려로 변경되지 않습니다.

## 운영 API

- `POST /api/v1/ops/catalog/product-versions/{id}/approval-decisions`
  - body: `decision`(`APPROVE` 또는 `REJECT`), `reason`(1~500자)
- `GET /api/v1/ops/catalog/product-versions/{id}/approval-decisions`
  - 해당 버전의 승인 판단 이력을 반환합니다.

두 API는 기존 `/api/v1/ops/**` 보안 경계를 그대로 사용하므로 `ROLE_ADMIN`만 접근할 수 있고, 요청 body가 아닌 로그인 주체 ID를 행위자로 기록합니다. 이번 범위에서는 실제 조직의 상품 작성자·검토자·승인자를 별도 역할로 나누는 이중 승인까지 확장하지 않았습니다.

## 검증

- `ProductVersionApprovalIntegrationTest`
  - 승인 대기와 반려 버전의 신규 견적 차단
  - 승인 후 신규 견적 허용
  - 승인·반려 이벤트의 대상 버전, 행위자, 사유, 시각 검증
  - 이미 결정된 버전의 판단 덮어쓰기 차단
  - 기존 계약 버전과 snapshot 불변 검증
- `ProductVersionApprovalSecurityTest`
  - 익명 401, 일반 사용자 403, 관리자 행위자 ID 전달 검증
- `CancerInsuranceMigrationTest`
  - 마이그레이션 수, 승인 원장 테이블, 출시 상태 컬럼, 기존 판매 버전의 호환 이관 검증
- `QuoteApiIntegrationTest`
  - 기존 견적 발행·스냅샷 회귀 검증

검증 환경은 Java 21과 PostgreSQL 16 Testcontainers이며 실제 보험사 상품·약관 승인 규정이나 전자결재 시스템을 재현한 것은 아닙니다.

