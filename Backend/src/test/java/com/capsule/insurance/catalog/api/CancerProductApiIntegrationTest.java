package com.capsule.insurance.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.catalog.application.CancerProductService;
import com.capsule.insurance.catalog.infra.JdbcCancerProductQueryRepository;
import com.capsule.insurance.common.exception.GlobalExceptionHandler;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class CancerProductApiIntegrationTest {

    private static final String TERMS_HASH =
            "c4ac2f41311096ba768d0550eecef4bdf21937ac3afb8ef8dad2bf5a6c03f04a";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_catalog_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static MockMvc mockMvc;
    private static JdbcTemplate jdbcTemplate;

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

        CancerProductService service = new CancerProductService(
                new JdbcCancerProductQueryRepository(jdbcTemplate)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CancerProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("판매 중인 암보험은 상품 한 건과 담보 세 건으로 집계된다")
    void listsOneProductWithThreeCoverages() throws Exception {
        mockMvc.perform(get("/api/v1/cancer-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productCode").value("CAPSURE-DEMO-CANCER"))
                .andExpect(jsonPath("$.data[0].baseMonthlyPremium").value(29900.00))
                .andExpect(jsonPath("$.data[0].coverageCount").value(3))
                .andExpect(jsonPath("$.data[0].simulation").value(true))
                .andExpect(jsonPath("$.data[0].termsVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data[0].termsSourceHash").value(TERMS_HASH));
    }

    @Test
    @DisplayName("상품 상세는 보험료를 한 번만 두고 담보별 금액과 대기기간을 구분한다")
    void returnsProductDetailWithoutDuplicatingPremium() throws Exception {
        Long productVersionId = productVersionId();

        mockMvc.perform(get("/api/v1/cancer-products/{productVersionId}", productVersionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.baseMonthlyPremium").value(29900.00))
                .andExpect(jsonPath("$.data.product.coverageCount").value(3))
                .andExpect(jsonPath("$.data.coverages.length()").value(3))
                .andExpect(jsonPath("$.data.coverages[0].coverageCode")
                        .value("DEMO_GENERAL_CANCER_DIAGNOSIS"))
                .andExpect(jsonPath("$.data.coverages[0].insuredAmount").value(20000000.00))
                .andExpect(jsonPath("$.data.coverages[0].waitingPeriodDays").value(90))
                .andExpect(jsonPath("$.data.coverages[0].reductionPeriodDays").value(365))
                .andExpect(jsonPath("$.data.coverages[0].reductionRate").value(0.5000))
                .andExpect(jsonPath("$.data.coverages[0].termsClauseCode").value("ARTICLE-09"))
                .andExpect(jsonPath("$.data.coverages[2].waitingPeriodDays").value(0));
    }

    @Test
    @DisplayName("30초 약관 요약은 버전·해시·대기기간과 조항 근거를 함께 반환한다")
    void returnsTraceableTermsSummary() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/cancer-products/{productVersionId}/terms/summary",
                        productVersionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.simulation").value(true))
                .andExpect(jsonPath("$.data.termsDocument.documentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.termsDocument.sourceHash").value(TERMS_HASH))
                .andExpect(jsonPath("$.data.coverageConditions.length()").value(3))
                .andExpect(jsonPath("$.data.coverageConditions[0].waitingPeriodDays").value(90))
                .andExpect(jsonPath("$.data.highlights.length()").value(9))
                .andExpect(jsonPath("$.data.highlights[?(@.category == 'EXCLUSION')].clauseCode")
                        .value("ARTICLE-14"))
                .andExpect(jsonPath("$.data.disclaimer").value(org.hamcrest.Matchers.containsString("합성 암보험")));
    }

    @Test
    @DisplayName("약관 원문 조항은 문서 버전과 해시를 잃지 않는다")
    void returnsClauseWithDocumentEvidence() throws Exception {
        Long termsClauseId = jdbcTemplate.queryForObject("""
                SELECT terms_clause_id
                FROM public.ins_terms_clause
                WHERE clause_code = 'ARTICLE-14'
                """, Long.class);

        mockMvc.perform(get("/api/v1/terms/clauses/{termsClauseId}", termsClauseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clauseCode").value("ARTICLE-14"))
                .andExpect(jsonPath("$.data.documentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.documentHash").value(TERMS_HASH))
                .andExpect(jsonPath("$.data.simulation").value(true))
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("부지급")));
    }

    @Test
    @DisplayName("판매 중이지 않거나 없는 상품 버전은 404를 반환한다")
    void returnsNotFoundForMissingProduct() throws Exception {
        mockMvc.perform(get("/api/v1/cancer-products/{productVersionId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    private static Long productVersionId() {
        return jdbcTemplate.queryForObject("""
                SELECT product_version_id
                FROM public.ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = '1.0.0'
                """, Long.class);
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
