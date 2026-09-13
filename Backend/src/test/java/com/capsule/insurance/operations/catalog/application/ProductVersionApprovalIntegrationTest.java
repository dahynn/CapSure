package com.capsule.insurance.operations.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capsule.insurance.application.application.ApplicationService;
import com.capsule.insurance.application.dto.CreateApplicationRequest;
import com.capsule.insurance.application.infra.JdbcApplicationRepository;
import com.capsule.insurance.catalog.infra.JdbcCancerProductQueryRepository;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.operations.catalog.dto.ProductVersionApprovalDecision;
import com.capsule.insurance.operations.catalog.dto.ProductVersionApprovalEventResponse;
import com.capsule.insurance.operations.catalog.infra.JdbcProductVersionApprovalRepository;
import com.capsule.insurance.quote.application.QuoteService;
import com.capsule.insurance.quote.dto.CreateQuoteRequest;
import com.capsule.insurance.quote.dto.QuoteResponse;
import com.capsule.insurance.quote.infra.JdbcQuoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ProductVersionApprovalIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_product_approval_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static JdbcTemplate jdbcTemplate;
    private static TransactionTemplate transactionTemplate;
    private static ProductVersionApprovalService approvalService;
    private static QuoteService quoteService;
    private static ApplicationService applicationService;
    private static Long adminUserId;
    private static Long customerUserId;

    @BeforeAll
    static void setUp() throws Exception {
        createLegacySchema();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .cleanDisabled(true)
                .load()
                .migrate();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        approvalService = new ProductVersionApprovalService(
                new JdbcProductVersionApprovalRepository(jdbcTemplate)
        );
        quoteService = new QuoteService(
                new JdbcCancerProductQueryRepository(jdbcTemplate),
                new JdbcQuoteRepository(jdbcTemplate, new ObjectMapper())
        );
        applicationService = new ApplicationService(
                new JdbcApplicationRepository(jdbcTemplate, new ObjectMapper()),
                new DataSourceTransactionManager(dataSource)
        );
        adminUserId = insertUser("product-approval-admin@capsure.test", "상품승인자", "ROLE_ADMIN");
        customerUserId = insertUser("product-approval-customer@capsure.test", "계약고객", "ROLE_USER");
    }

    @Test
    @DisplayName("미승인 버전은 신규 견적에서 차단하고 승인·반려 원장을 남기며 기존 계약 snapshot은 보존한다")
    void gatesNewBusinessAuditsDecisionsAndPreservesExistingPolicySnapshot() {
        Long approvedV1 = productVersionId("1.0.0");
        QuoteResponse oldQuote = quoteService.issue(
                customerUserId,
                new CreateQuoteRequest(approvedV1, productCoverageIds(approvedV1))
        );
        Long existingPolicyVersionId = createExistingPolicyVersion(oldQuote.quoteId());
        String existingSnapshot = policySnapshot(existingPolicyVersionId);

        Long pendingV2 = createPendingProductVersion("2.0.0", "승인 대기 보험료 개정안");
        assertQuoteBlocked(pendingV2);
        Long forgedPendingQuote = createIssuedQuoteForPendingProduct(pendingV2, oldQuote.quoteId());
        assertThatThrownBy(() -> applicationService.create(
                customerUserId,
                new CreateApplicationRequest(forgedPendingQuote)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        ProductVersionApprovalEventResponse approved = transactionTemplate.execute(status ->
                approvalService.decide(
                        pendingV2,
                        ProductVersionApprovalDecision.APPROVE,
                        adminUserId,
                        "보험료와 약관 연결 검증 완료"
                ));

        assertThat(approved).isNotNull();
        assertThat(approved.productVersionId()).isEqualTo(pendingV2);
        assertThat(approved.productVersion()).isEqualTo("2.0.0");
        assertThat(approved.termsDocumentId()).isNotNull();
        assertThat(approved.termsVersion()).isEqualTo("1.0.0");
        assertThat(approved.decision()).isEqualTo("APPROVE");
        assertThat(approved.previousStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(approved.resultingStatus()).isEqualTo("APPROVED");
        assertThat(approved.actorUserId()).isEqualTo(adminUserId);
        assertThat(approved.reason()).isEqualTo("보험료와 약관 연결 검증 완료");
        assertThat(approved.decidedAt()).isNotNull();

        QuoteResponse newQuote = quoteService.issue(
                customerUserId,
                new CreateQuoteRequest(pendingV2, productCoverageIds(pendingV2))
        );
        assertThat(newQuote.status()).isEqualTo("ISSUED");
        assertThat(newQuote.snapshot().productVersion()).isEqualTo("2.0.0");
        assertThat(newQuote.snapshot().productName()).isEqualTo("승인 대기 보험료 개정안");

        assertThat(approvalService.getHistory(pendingV2))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.approvalEventId()).isEqualTo(approved.approvalEventId());
                    assertThat(event.actorUserId()).isEqualTo(adminUserId);
                    assertThat(event.reason()).isEqualTo("보험료와 약관 연결 검증 완료");
                });
        assertThatThrownBy(() -> transactionTemplate.execute(status -> approvalService.decide(
                pendingV2,
                ProductVersionApprovalDecision.REJECT,
                adminUserId,
                "이미 승인된 버전의 결정을 덮어쓰지 않는다"
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));

        Long rejectedV3 = createPendingProductVersion("3.0.0", "검증 반려 보험료 개정안");
        ProductVersionApprovalEventResponse rejected = transactionTemplate.execute(status ->
                approvalService.decide(
                        rejectedV3,
                        ProductVersionApprovalDecision.REJECT,
                        adminUserId,
                        "필수 약관 검토 근거 부족"
                ));
        assertThat(rejected).isNotNull();
        assertThat(rejected.decision()).isEqualTo("REJECT");
        assertThat(rejected.resultingStatus()).isEqualTo("REJECTED");
        assertQuoteBlocked(rejectedV3);

        assertThat(policySnapshot(existingPolicyVersionId)).isEqualTo(existingSnapshot);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT product_version_id
                FROM public.ins_policy_version
                WHERE policy_version_id = ?
                """, Long.class, existingPolicyVersionId)).isEqualTo(approvedV1);
    }

    private static void assertQuoteBlocked(Long productVersionId) {
        assertThatThrownBy(() -> quoteService.issue(
                customerUserId,
                new CreateQuoteRequest(productVersionId, List.of(1L))
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static Long createPendingProductVersion(String version, String productName) {
        Long productVersionId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_product_version (
                    product_code,
                    version,
                    product_name,
                    insurer_name,
                    insurer_sector,
                    sale_from,
                    status,
                    base_monthly_premium,
                    currency_code,
                    terms_document_id,
                    is_simulation
                )
                SELECT product_code,
                       ?,
                       ?,
                       insurer_name,
                       insurer_sector,
                       CURRENT_DATE,
                       'ON_SALE',
                       base_monthly_premium + 1000,
                       currency_code,
                       terms_document_id,
                       is_simulation
                FROM public.ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = '1.0.0'
                RETURNING product_version_id
                """, Long.class, version, productName);
        jdbcTemplate.update("""
                INSERT INTO public.ins_product_coverage (
                    product_version_id,
                    coverage_id,
                    insured_amount,
                    currency_code,
                    waiting_period_days,
                    reduction_period_days,
                    reduction_rate,
                    coverage_start_rule,
                    display_order
                )
                SELECT ?,
                       coverage_id,
                       insured_amount,
                       currency_code,
                       waiting_period_days,
                       reduction_period_days,
                       reduction_rate,
                       coverage_start_rule,
                       display_order
                FROM public.ins_product_coverage
                WHERE product_version_id = ?
                """, productVersionId, productVersionId("1.0.0"));
        return productVersionId;
    }

    private static Long createExistingPolicyVersion(Long quoteId) {
        Long applicationId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_application (
                    application_no,
                    quote_id,
                    applicant_user_id,
                    insured_user_id,
                    status
                ) VALUES (?, ?, ?, ?, 'APPROVED')
                RETURNING application_id
                """, Long.class, "APP-EXISTING-APPROVAL", quoteId, customerUserId, customerUserId);
        Long policyId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_policy (
                    policy_no,
                    application_id,
                    policyholder_user_id,
                    insured_user_id,
                    beneficiary_user_id,
                    status,
                    activated_at
                ) VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW())
                RETURNING policy_id
                """, Long.class, "POL-EXISTING-APPROVAL", applicationId,
                customerUserId, customerUserId, customerUserId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_policy_version (
                    policy_id,
                    version,
                    product_version_id,
                    terms_document_id,
                    valid_from,
                    snapshot_json
                )
                SELECT ?,
                       1,
                       quote.product_version_id,
                       product.terms_document_id,
                       NOW(),
                       jsonb_build_object('quote', quote.snapshot_json, 'claimRules', '[]'::JSONB)
                FROM public.ins_quote quote
                JOIN public.ins_product_version product
                  ON product.product_version_id = quote.product_version_id
                WHERE quote.quote_id = ?
                RETURNING policy_version_id
                """, Long.class, policyId, quoteId);
    }

    private static Long createIssuedQuoteForPendingProduct(Long productVersionId, Long snapshotSourceQuoteId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_quote (
                    quote_no,
                    user_id,
                    product_version_id,
                    status,
                    monthly_premium,
                    currency_code,
                    snapshot_json,
                    terms_document_hash,
                    expires_at
                )
                SELECT 'Q-FORGED-PENDING-APPROVAL',
                       ?,
                       ?,
                       'ISSUED',
                       monthly_premium,
                       currency_code,
                       snapshot_json,
                       terms_document_hash,
                       NOW() + INTERVAL '30 minutes'
                FROM public.ins_quote
                WHERE quote_id = ?
                RETURNING quote_id
                """, Long.class, customerUserId, productVersionId, snapshotSourceQuoteId);
    }

    private static Long productVersionId(String version) {
        return jdbcTemplate.queryForObject("""
                SELECT product_version_id
                FROM public.ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = ?
                """, Long.class, version);
    }

    private static List<Long> productCoverageIds(Long productVersionId) {
        return jdbcTemplate.queryForList("""
                SELECT product_coverage_id
                FROM public.ins_product_coverage
                WHERE product_version_id = ?
                ORDER BY display_order
                """, Long.class, productVersionId);
    }

    private static String policySnapshot(Long policyVersionId) {
        return jdbcTemplate.queryForObject("""
                SELECT snapshot_json::TEXT
                FROM public.ins_policy_version
                WHERE policy_version_id = ?
                """, String.class, policyVersionId);
    }

    private static Long insertUser(String email, String name, String accessRole) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.usr_user (email, name, phone, user_status, access_role)
                VALUES (?, ?, '010-0000-0000', 'ACTIVE', ?)
                RETURNING user_id
                """, Long.class, email, name, accessRole);
    }

    private static void createLegacySchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
             InputStream input = new ClassPathResource("db/schema/schema.sql").getInputStream();
             Statement statement = connection.createStatement()) {
            statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
