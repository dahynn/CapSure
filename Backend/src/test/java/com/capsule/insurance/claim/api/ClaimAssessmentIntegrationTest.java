package com.capsule.insurance.claim.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.application.application.ApplicationService;
import com.capsule.insurance.application.dto.CreateApplicationRequest;
import com.capsule.insurance.application.dto.CreateConsentRequest;
import com.capsule.insurance.application.dto.ReplaceDisclosuresRequest;
import com.capsule.insurance.application.infra.JdbcApplicationRepository;
import com.capsule.insurance.catalog.infra.JdbcCancerProductQueryRepository;
import com.capsule.insurance.claim.application.ClaimService;
import com.capsule.insurance.claim.infra.JdbcClaimRepository;
import com.capsule.insurance.common.exception.GlobalExceptionHandler;
import com.capsule.insurance.payment.adapter.FakePremiumPaymentGateway;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.dto.ConfirmPaymentRequest;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import com.capsule.insurance.payment.infra.JdbcPaymentRepository;
import com.capsule.insurance.policy.infra.JdbcPolicyRepository;
import com.capsule.insurance.quote.application.QuoteService;
import com.capsule.insurance.quote.dto.CreateQuoteRequest;
import com.capsule.insurance.quote.infra.JdbcQuoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ClaimAssessmentIntegrationTest {

    private static final String TERMS_HASH =
            "c4ac2f41311096ba768d0550eecef4bdf21937ac3afb8ef8dad2bf5a6c03f04a";
    private static final String DIAGNOSIS_CHECKSUM = "a".repeat(64);
    private static final String PATHOLOGY_CHECKSUM = "b".repeat(64);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_claim_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static JdbcTemplate jdbcTemplate;
    private static QuoteService quoteService;
    private static ApplicationService applicationService;
    private static PaymentService paymentService;
    private static MockMvc mockMvc;
    private static Long productVersionId;
    private static List<Long> productCoverageIds;
    private static List<Long> userIds;

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
        userIds = new ArrayList<>();
        for (int index = 1; index <= 6; index++) {
            userIds.add(insertUser(
                    "claim-user-" + index + "@capsure.test",
                    "청구" + index,
                    "010-5000-50" + String.format("%02d", index)
            ));
        }
        productVersionId = jdbcTemplate.queryForObject("""
                SELECT product_version_id
                FROM public.ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = '1.0.0'
                """, Long.class);
        productCoverageIds = jdbcTemplate.queryForList("""
                SELECT product_coverage_id
                FROM public.ins_product_coverage
                WHERE product_version_id = ?
                ORDER BY display_order
                """, Long.class, productVersionId);

        quoteService = new QuoteService(
                new JdbcCancerProductQueryRepository(jdbcTemplate),
                new JdbcQuoteRepository(jdbcTemplate, OBJECT_MAPPER)
        );
        applicationService = new ApplicationService(
                new JdbcApplicationRepository(jdbcTemplate, OBJECT_MAPPER),
                new DataSourceTransactionManager(dataSource)
        );
        JdbcPolicyRepository policyRepository = new JdbcPolicyRepository(jdbcTemplate, OBJECT_MAPPER);
        paymentService = new PaymentService(
                new JdbcPaymentRepository(jdbcTemplate),
                policyRepository,
                new FakePremiumPaymentGateway(),
                new DataSourceTransactionManager(dataSource),
                OBJECT_MAPPER
        );
        ClaimService claimService = new ClaimService(
                new JdbcClaimRepository(jdbcTemplate, OBJECT_MAPPER),
                new DataSourceTransactionManager(dataSource)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ClaimController(claimService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("보장개시·감액기간 후 일반암과 필수 합성 증빙은 전액 승인하고 지급 100회를 한 건으로 제한한다")
    void approvesFullBenefitAndPaysOnceForOneHundredRetries() throws Exception {
        Long userId = userIds.get(0);
        PolicyFixture policy = activatePolicy(userId, "full-benefit");
        Instant coverageStart = Instant.now().minus(450, ChronoUnit.DAYS);
        setCoverageStart(policy.policyCoverageId(), coverageStart);
        Long claimId = createClaim(
                userId,
                policy,
                Instant.now().minus(10, ChronoUnit.DAYS),
                "DEMO_GENERAL_CANCER"
        );
        recordRequiredEvidence(userId, claimId);

        String currentRuleJson = jdbcTemplate.queryForObject("""
                SELECT rule_json::TEXT
                FROM public.ins_coverage_rule
                WHERE product_coverage_id = ?
                  AND rule_type = 'CLAIM_ELIGIBILITY'
                  AND is_active = TRUE
                """, String.class, productCoverageIds.getFirst());
        JsonNode assessed;
        jdbcTemplate.update("""
                UPDATE public.ins_coverage_rule
                SET rule_json = '{"diagnosisCategories":["CHANGED_AFTER_ISSUE"],"requiredEvidence":[]}'::JSONB
                WHERE product_coverage_id = ?
                  AND rule_type = 'CLAIM_ELIGIBILITY'
                  AND is_active = TRUE
                """, productCoverageIds.getFirst());
        try {
            assessed = submit(userId, claimId, "claim-submit-full");
        } finally {
            jdbcTemplate.update("""
                    UPDATE public.ins_coverage_rule
                    SET rule_json = CAST(? AS JSONB)
                    WHERE product_coverage_id = ?
                      AND rule_type = 'CLAIM_ELIGIBILITY'
                      AND is_active = TRUE
                    """, currentRuleJson, productCoverageIds.getFirst());
        }
        assertThat(assessed.path("status").asText()).isEqualTo("APPROVED");
        assertThat(assessed.path("decision").path("benefitAmount").decimalValue())
                .isEqualByComparingTo("20000000.00");
        assertThat(assessed.path("decision").path("reasonCodes").get(0).asText())
                .isEqualTo("ELIGIBLE_FULL_BENEFIT");
        assertDecisionEvidence(assessed, "ARTICLE-09");

        Long competingClaimId = createClaim(
                userId,
                policy,
                Instant.now().minus(9, ChronoUnit.DAYS),
                "DEMO_GENERAL_CANCER"
        );
        recordRequiredEvidence(userId, competingClaimId);
        JsonNode competingAssessment = submit(
                userId,
                competingClaimId,
                "claim-submit-competing"
        );
        assertThat(competingAssessment.path("status").asText()).isEqualTo("APPROVED");

        Long firstPaymentId = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            MvcResult result = mockMvc.perform(post("/api/v1/claims/{claimId}/payments", claimId)
                            .principal(authentication(userId))
                            .header("Idempotency-Key", "claim-payment-100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PAID"))
                    .andReturn();
            Long paymentId = OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray())
                    .path("data").path("payment").path("claimPaymentId").asLong();
            if (firstPaymentId == null) {
                firstPaymentId = paymentId;
            }
            assertThat(paymentId).isEqualTo(firstPaymentId);
        }

        assertThat(count("clm_payment", "claim_decision_id", assessed.path("decision")
                .path("claimDecisionId").asLong())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT paid_benefit_count
                FROM public.ins_policy_coverage
                WHERE policy_coverage_id = ?
                """, Integer.class, policy.policyCoverageId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ops_outbox_event
                WHERE aggregate_type = 'CLAIM'
                  AND aggregate_id = ?
                  AND event_type = 'CLAIM_BENEFIT_PAID'
                """, Integer.class, claimId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT payload_json ->> 'policyId'
                FROM public.ops_outbox_event
                WHERE event_type = 'CLAIM_BENEFIT_PAID'
                  AND aggregate_id = ?
                """, String.class, claimId.toString())).isEqualTo(policy.policyId().toString());

        mockMvc.perform(post("/api/v1/claims/{claimId}/payments", competingClaimId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "claim-payment-competing"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        assertThat(count(
                "clm_payment",
                "claim_decision_id",
                competingAssessment.path("decision").path("claimDecisionId").asLong()
        )).isZero();
    }

    @Test
    @DisplayName("감액기간 안의 일반암 진단은 약관 snapshot의 50%인 천만원을 승인한다")
    void appliesReductionRateFromPolicySnapshot() throws Exception {
        Long userId = userIds.get(1);
        PolicyFixture policy = activatePolicy(userId, "reduced-benefit");
        setCoverageStart(policy.policyCoverageId(), Instant.now().minus(100, ChronoUnit.DAYS));
        Long claimId = createClaim(
                userId,
                policy,
                Instant.now().minus(1, ChronoUnit.DAYS),
                "DEMO_GENERAL_CANCER"
        );
        recordRequiredEvidence(userId, claimId);

        JsonNode assessed = submit(userId, claimId, "claim-submit-reduced");

        assertThat(assessed.path("status").asText()).isEqualTo("APPROVED");
        assertThat(assessed.path("decision").path("benefitAmount").decimalValue())
                .isEqualByComparingTo("10000000.00");
        assertThat(assessed.path("decision").path("reasonCodes").get(0).asText())
                .isEqualTo("ELIGIBLE_REDUCED_BENEFIT");
    }

    @Test
    @DisplayName("보장개시 전 진단은 증빙 여부와 무관하게 약관 조항 근거를 가진 부지급이다")
    void deniesIncidentBeforeCoverageStarts() throws Exception {
        Long userId = userIds.get(2);
        PolicyFixture policy = activatePolicy(userId, "before-coverage");
        Long claimId = createClaim(
                userId,
                policy,
                Instant.now().minus(1, ChronoUnit.DAYS),
                "DEMO_GENERAL_CANCER"
        );

        JsonNode assessed = submit(userId, claimId, "claim-submit-denied");

        assertThat(assessed.path("status").asText()).isEqualTo("DENIED");
        assertThat(assessed.path("decision").path("benefitAmount").isNull()).isTrue();
        assertThat(assessed.path("decision").path("reasonCodes").get(0).asText())
                .isEqualTo("COVERAGE_NOT_STARTED");
        assertDecisionEvidence(assessed, "ARTICLE-14");

        mockMvc.perform(post("/api/v1/claims/{claimId}/payments", claimId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "denied-payment"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("필수 증빙 부족은 자동 부지급하지 않고 수동심사로 보내며 타 사용자에게 숨긴다")
    void sendsMissingEvidenceToManualReviewAndHidesOwnership() throws Exception {
        Long userId = userIds.get(3);
        Long otherUserId = userIds.get(4);
        PolicyFixture policy = activatePolicy(userId, "manual-review");
        setCoverageStart(policy.policyCoverageId(), Instant.now().minus(450, ChronoUnit.DAYS));
        Long claimId = createClaim(
                userId,
                policy,
                Instant.now().minus(1, ChronoUnit.DAYS),
                "DEMO_GENERAL_CANCER"
        );
        recordEvidence(
                userId,
                claimId,
                "DEMO_DIAGNOSIS_CERTIFICATE",
                DIAGNOSIS_CHECKSUM,
                true
        );

        JsonNode assessed = submit(userId, claimId, "claim-submit-manual");

        assertThat(assessed.path("status").asText()).isEqualTo("MANUAL_REVIEW");
        assertThat(assessed.path("decision").path("reasonCodes").get(0).asText())
                .isEqualTo("REQUIRED_EVIDENCE_MISSING_OR_UNVERIFIED");
        assertDecisionEvidence(assessed, "ARTICLE-13");

        mockMvc.perform(get("/api/v1/claims/{claimId}", claimId)
                        .principal(authentication(otherUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void graceClaimsRemainEligibleAndHistoricalClaimsSurviveLapse() throws Exception {
        Long userId = userIds.get(5);
        PolicyFixture policy = activatePolicy(userId, "grace-history");
        setCoverageStart(policy.policyCoverageId(), Instant.now().minus(450, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE ins_policy SET status = 'GRACE' WHERE policy_id = ?", policy.policyId());
        Instant incident = Instant.now().minus(3, ChronoUnit.DAYS);
        Long claimId = createClaim(userId, policy, incident, "DEMO_GENERAL_CANCER");
        recordRequiredEvidence(userId, claimId);
        Instant lapse = Instant.now().minus(1, ChronoUnit.DAYS);
        jdbcTemplate.update("UPDATE ins_policy SET status = 'LAPSED', lapsed_at = ? WHERE policy_id = ?",
                java.sql.Timestamp.from(lapse), policy.policyId());
        assertThat(submit(userId, claimId, "grace-historical-submit").path("status").asText()).isEqualTo("APPROVED");
        createClaim(userId, policy, incident.minusSeconds(1), "DEMO_GENERAL_CANCER");
        var repository = new JdbcClaimRepository(jdbcTemplate, OBJECT_MAPPER);
        assertThat(repository.ownsClaimablePolicyCoverage(policy.policyId(), policy.policyCoverageId(), userId, lapse)).isFalse();
        assertThat(repository.ownsClaimablePolicyCoverage(policy.policyId(), policy.policyCoverageId(), userId, lapse.plusSeconds(1))).isFalse();
        assertThat(repository.ownsClaimablePolicyCoverage(policy.policyId(), policy.policyCoverageId(), userIds.get(0), incident)).isFalse();
    }

    @Test
    void draftAssessmentUsesEffectiveLapseBoundary() throws Exception {
        Long userId = userIds.get(5);
        PolicyFixture policy = activatePolicy(userId, "lapse-draft");
        setCoverageStart(policy.policyCoverageId(), Instant.now().minus(450, ChronoUnit.DAYS));
        Instant incident = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Long claimId = createClaim(userId, policy, incident, "DEMO_GENERAL_CANCER");
        jdbcTemplate.update("UPDATE ins_policy SET status = 'LAPSED', lapsed_at = ? WHERE policy_id = ?",
                java.sql.Timestamp.from(incident), policy.policyId());
        JsonNode decision = submit(userId, claimId, "lapse-draft-submit");
        assertThat(decision.path("status").asText()).isEqualTo("DENIED");
        assertThat(decision.path("decision").path("reasonCodes").get(0).asText()).isEqualTo("COVERAGE_ENDED");
    }

    @Test
    void paymentRechecksLapseBoundaryEvenForPreviouslyApprovedDecision() throws Exception {
        Long userId = userIds.get(5);
        PolicyFixture policy = activatePolicy(userId, "lapse-approved");
        setCoverageStart(policy.policyCoverageId(), Instant.now().minus(450, ChronoUnit.DAYS));
        Instant incident = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Long claimId = createClaim(userId, policy, incident, "DEMO_GENERAL_CANCER");
        recordRequiredEvidence(userId, claimId);
        assertThat(submit(userId, claimId, "lapse-approved-submit").path("status").asText()).isEqualTo("APPROVED");
        jdbcTemplate.update("UPDATE ins_policy SET status = 'LAPSED', lapsed_at = ? WHERE policy_id = ?",
                java.sql.Timestamp.from(incident), policy.policyId());
        mockMvc.perform(post("/api/v1/claims/{claimId}/payments", claimId)
                        .principal(authentication(userId)).header("Idempotency-Key", "lapse-approved-pay"))
                .andExpect(status().isUnprocessableEntity());
    }

    private static PolicyFixture activatePolicy(Long userId, String suffix) {
        Long quoteId = quoteService.issue(
                userId,
                new CreateQuoteRequest(productVersionId, productCoverageIds)
        ).quoteId();
        Long applicationId = applicationService.create(
                userId,
                new CreateApplicationRequest(quoteId)
        ).applicationId();
        applicationService.replaceDisclosures(
                userId,
                applicationId,
                new ReplaceDisclosuresRequest(false, false, false)
        );
        applicationService.recordConsent(
                userId,
                applicationId,
                new CreateConsentRequest("PRODUCT_TERMS", TERMS_HASH, true)
        );
        applicationService.recordConsent(
                userId,
                applicationId,
                new CreateConsentRequest("PRODUCT_EXPLANATION", TERMS_HASH, true)
        );
        applicationService.submit(userId, applicationId, "claim-app-submit-" + suffix);
        PaymentOrderResponse order = paymentService.createInitialPremiumOrder(
                userId,
                applicationId,
                "claim-order-" + suffix
        );
        PaymentOrderResponse paid = paymentService.confirm(
                userId,
                order.paymentOrderId(),
                "claim-confirm-" + suffix,
                new ConfirmPaymentRequest("fake-paid-claim-" + suffix, new java.math.BigDecimal("29900.00"))
        );
        Long generalCancerCoverageId = jdbcTemplate.queryForObject("""
                SELECT coverage.policy_coverage_id
                FROM public.ins_policy_coverage coverage
                WHERE coverage.policy_version_id = (
                    SELECT policy_version_id
                    FROM public.ins_policy_version
                    WHERE policy_id = ?
                      AND version = 1
                )
                  AND coverage.coverage_code_snapshot = 'DEMO_GENERAL_CANCER_DIAGNOSIS'
                """, Long.class, paid.policyId());
        return new PolicyFixture(paid.policyId(), generalCancerCoverageId);
    }

    private static Long createClaim(
            Long userId,
            PolicyFixture policy,
            Instant incidentAt,
            String diagnosisCategory
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/policies/{policyId}/claims", policy.policyId())
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(Map.of(
                                "policyCoverageId", policy.policyCoverageId(),
                                "incidentAt", incidentAt.toString(),
                                "diagnosisCategory", diagnosisCategory
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray())
                .path("data").path("claimId").asLong();
    }

    private static void recordRequiredEvidence(Long userId, Long claimId) throws Exception {
        recordEvidence(
                userId,
                claimId,
                "DEMO_DIAGNOSIS_CERTIFICATE",
                DIAGNOSIS_CHECKSUM,
                true
        );
        recordEvidence(
                userId,
                claimId,
                "DEMO_PATHOLOGY_REPORT",
                PATHOLOGY_CHECKSUM,
                true
        );
    }

    private static void recordEvidence(
            Long userId,
            Long claimId,
            String evidenceType,
            String checksum,
            boolean verified
    ) throws Exception {
        mockMvc.perform(put("/api/v1/claims/{claimId}/evidence", claimId)
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(Map.of(
                                "evidenceType", evidenceType,
                                "syntheticReference", "synthetic://claim/" + claimId + "/" + evidenceType,
                                "checksum", checksum,
                                "metadata", Map.of("fixture", true),
                                "verified", verified
                        ))))
                .andExpect(status().isOk());
    }

    private static JsonNode submit(Long userId, Long claimId, String idempotencyKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/claims/{claimId}/submit", claimId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andReturn();
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private static void assertDecisionEvidence(JsonNode assessed, String expectedClauseCode) {
        JsonNode decision = assessed.path("decision");
        assertThat(decision.path("ruleVersion").asText()).isEqualTo("1.0.0");
        assertThat(decision.path("inputHash").asText()).matches("[0-9a-f]{64}");
        assertThat(decision.path("actorType").asText()).isEqualTo("RULE_ENGINE");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT clause_code
                FROM public.ins_terms_clause
                WHERE terms_clause_id = ?
                """, String.class, decision.path("termsClauseId").asLong()))
                .isEqualTo(expectedClauseCode);
    }

    private static void setCoverageStart(Long policyCoverageId, Instant coverageStartAt) {
        jdbcTemplate.update("""
                UPDATE public.ins_policy_coverage
                SET coverage_start_at = ?
                WHERE policy_coverage_id = ?
                """, java.sql.Timestamp.from(coverageStartAt), policyCoverageId);
    }

    private static int count(String table, String column, Object value) {
        String sql = "SELECT COUNT(*) FROM public." + table + " WHERE " + column + " = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, value);
    }

    private static UsernamePasswordAuthenticationToken authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId.toString(), "", List.of());
    }

    private static Long insertUser(String email, String name, String phone) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.usr_user (email, name, phone, user_status)
                VALUES (?, ?, ?, 'ACTIVE')
                RETURNING user_id
                """, Long.class, email, name, phone);
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

    private record PolicyFixture(Long policyId, Long policyCoverageId) {
    }
}
