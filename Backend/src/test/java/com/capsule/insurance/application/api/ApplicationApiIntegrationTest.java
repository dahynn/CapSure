package com.capsule.insurance.application.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.application.application.ApplicationService;
import com.capsule.insurance.application.infra.JdbcApplicationRepository;
import com.capsule.insurance.catalog.infra.JdbcCancerProductQueryRepository;
import com.capsule.insurance.common.exception.GlobalExceptionHandler;
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
class ApplicationApiIntegrationTest {

    private static final String TERMS_HASH =
            "c4ac2f41311096ba768d0550eecef4bdf21937ac3afb8ef8dad2bf5a6c03f04a";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_application_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static JdbcTemplate jdbcTemplate;
    private static QuoteService quoteService;
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
        for (int index = 1; index <= 8; index++) {
            userIds.add(insertUser(
                    "application-user-" + index + "@capsure.test",
                    "청약" + index,
                    "010-3000-30" + String.format("%02d", index)
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
        ApplicationService applicationService = new ApplicationService(
                new JdbcApplicationRepository(jdbcTemplate, OBJECT_MAPPER),
                new DataSourceTransactionManager(dataSource)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ApplicationController(applicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("필수 동의가 하나라도 빠지면 제출을 거절하고 심사 원장을 만들지 않는다")
    void rejectsSubmissionWithoutAllRequiredConsents() throws Exception {
        Long userId = userIds.get(0);
        Long applicationId = createApplication(userId);
        replaceDisclosures(userId, applicationId, false, false, false);
        recordConsent(userId, applicationId, "PRODUCT_TERMS", TERMS_HASH, true);

        mockMvc.perform(post("/api/v1/applications/{applicationId}/submit", applicationId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "application-missing-consent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        assertThat(applicationStatus(applicationId)).isEqualTo("DISCLOSURE_COMPLETED");
        assertThat(decisionCount(applicationId)).isZero();
    }

    @Test
    @DisplayName("같은 견적은 청약 하나에만 연결되고 동일 제출 키 100회는 심사 한 건만 만든다")
    void makesQuoteAndSubmissionIdempotent() throws Exception {
        Long userId = userIds.get(1);
        Long quoteId = issueQuote(userId);
        Long firstApplicationId = createApplication(userId, quoteId);
        Long repeatedApplicationId = createApplication(userId, quoteId);

        assertThat(repeatedApplicationId).isEqualTo(firstApplicationId);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_application
                WHERE quote_id = ?
                """, Integer.class, quoteId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT status
                FROM public.ins_quote
                WHERE quote_id = ?
                """, String.class, quoteId)).isEqualTo("USED");

        replaceDisclosures(userId, firstApplicationId, false, false, false);
        recordRequiredConsents(userId, firstApplicationId);

        Long firstDecisionId = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            MvcResult result = submit(userId, firstApplicationId, "application-submit-100");
            JsonNode body = OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray());
            Long decisionId = body.path("data").path("underwritingDecision")
                    .path("underwritingDecisionId").asLong();
            if (firstDecisionId == null) {
                firstDecisionId = decisionId;
            }
            assertThat(decisionId).isEqualTo(firstDecisionId);
        }

        assertThat(applicationStatus(firstApplicationId)).isEqualTo("APPROVED");
        assertThat(decisionCount(firstApplicationId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT submission_idempotency_key
                FROM public.ins_application
                WHERE application_id = ?
                """, String.class, firstApplicationId)).isEqualTo("application-submit-100");

        mockMvc.perform(post("/api/v1/applications/{applicationId}/submit", firstApplicationId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "application-submit-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @DisplayName("고지 결과에 따라 승인·수동심사·인수거절을 버전과 입력 hash로 기록한다")
    void recordsDeterministicUnderwritingOutcomes() throws Exception {
        JsonNode approved = completeAndSubmit(userIds.get(2), false, false, false, "uw-shared-key");
        assertDecision(approved, "APPROVED", "STANDARD_ACCEPT");

        JsonNode manualReview = completeAndSubmit(userIds.get(3), false, true, false, "uw-shared-key");
        assertDecision(manualReview, "MANUAL_REVIEW", "PENDING_CANCER_EXAMINATION");

        JsonNode declined = completeAndSubmit(userIds.get(4), true, false, false, "uw-declined");
        assertDecision(declined, "DECLINED", "PREEXISTING_CANCER_DISCLOSED");
    }

    @Test
    @DisplayName("견적 약관 hash가 아닌 동의와 다른 사용자의 청약 조회를 거절한다")
    void rejectsTamperedConsentAndHidesOtherUsersApplication() throws Exception {
        Long ownerId = userIds.get(5);
        Long otherUserId = userIds.get(6);
        Long applicationId = createApplication(ownerId);
        replaceDisclosures(ownerId, applicationId, false, false, false);

        mockMvc.perform(post("/api/v1/applications/{applicationId}/consents", applicationId)
                        .principal(authentication(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "consentType", "PRODUCT_TERMS",
                                "documentHash", "0".repeat(64),
                                "agreed", true
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_consent
                WHERE application_id = ?
                """, Integer.class, applicationId)).isZero();

        mockMvc.perform(get("/api/v1/applications/{applicationId}", applicationId)
                        .principal(authentication(otherUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    private static JsonNode completeAndSubmit(
            Long userId,
            boolean diagnosedCancer,
            boolean underCancerExamination,
            boolean recentHospitalization,
            String idempotencyKey
    ) throws Exception {
        Long applicationId = createApplication(userId);
        replaceDisclosures(
                userId,
                applicationId,
                diagnosedCancer,
                underCancerExamination,
                recentHospitalization
        );
        recordRequiredConsents(userId, applicationId);
        return OBJECT_MAPPER.readTree(
                submit(userId, applicationId, idempotencyKey).getResponse().getContentAsByteArray()
        ).path("data");
    }

    private static void assertDecision(JsonNode application, String decision, String reasonCode) {
        JsonNode underwriting = application.path("underwritingDecision");
        assertThat(application.path("status").asText()).isEqualTo(decision);
        assertThat(underwriting.path("decision").asText()).isEqualTo(decision);
        assertThat(underwriting.path("decisionVersion").asInt()).isEqualTo(1);
        assertThat(underwriting.path("ruleVersion").asText()).isEqualTo("UW-DEMO-CANCER-1.0.0");
        assertThat(underwriting.path("reasonCodes").get(0).asText()).isEqualTo(reasonCode);
        assertThat(underwriting.path("inputHash").asText()).matches("[0-9a-f]{64}");
    }

    private static Long createApplication(Long userId) throws Exception {
        return createApplication(userId, issueQuote(userId));
    }

    private static Long createApplication(Long userId, Long quoteId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quoteId", quoteId))))
                .andExpect(status().isOk())
                .andReturn();
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray())
                .path("data").path("applicationId").asLong();
    }

    private static Long issueQuote(Long userId) {
        return quoteService.issue(
                userId,
                new CreateQuoteRequest(productVersionId, productCoverageIds)
        ).quoteId();
    }

    private static void replaceDisclosures(
            Long userId,
            Long applicationId,
            boolean diagnosedCancer,
            boolean underCancerExamination,
            boolean recentHospitalization
    ) throws Exception {
        mockMvc.perform(put("/api/v1/applications/{applicationId}/disclosures", applicationId)
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "diagnosedCancer", diagnosedCancer,
                                "underCancerExamination", underCancerExamination,
                                "recentHospitalization", recentHospitalization
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISCLOSURE_COMPLETED"));
    }

    private static void recordRequiredConsents(Long userId, Long applicationId) throws Exception {
        recordConsent(userId, applicationId, "PRODUCT_TERMS", TERMS_HASH, true);
        recordConsent(userId, applicationId, "PRODUCT_EXPLANATION", TERMS_HASH, true);
    }

    private static void recordConsent(
            Long userId,
            Long applicationId,
            String consentType,
            String documentHash,
            boolean agreed
    ) throws Exception {
        mockMvc.perform(post("/api/v1/applications/{applicationId}/consents", applicationId)
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "consentType", consentType,
                                "documentHash", documentHash,
                                "agreed", agreed
                        ))))
                .andExpect(status().isOk());
    }

    private static MvcResult submit(Long userId, Long applicationId, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{applicationId}/submit", applicationId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String applicationStatus(Long applicationId) {
        return jdbcTemplate.queryForObject("""
                SELECT status
                FROM public.ins_application
                WHERE application_id = ?
                """, String.class, applicationId);
    }

    private static int decisionCount(Long applicationId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_uw_decision
                WHERE application_id = ?
                """, Integer.class, applicationId);
    }

    private static String json(Object value) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(value);
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
}
