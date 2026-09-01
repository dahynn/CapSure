package com.capsule.insurance.quote.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.catalog.infra.JdbcCancerProductQueryRepository;
import com.capsule.insurance.common.exception.GlobalExceptionHandler;
import com.capsule.insurance.quote.application.QuoteService;
import com.capsule.insurance.quote.infra.JdbcQuoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class QuoteApiIntegrationTest {

    private static final String TERMS_HASH =
            "c4ac2f41311096ba768d0550eecef4bdf21937ac3afb8ef8dad2bf5a6c03f04a";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_quote_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static JdbcTemplate jdbcTemplate;
    private static MockMvc mockMvc;
    private static Long firstUserId;
    private static Long secondUserId;
    private static Long productVersionId;
    private static List<Long> productCoverageIds;

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
        firstUserId = insertUser("quote-user-1@capsure.test", "견적일", "010-1000-1000");
        secondUserId = insertUser("quote-user-2@capsure.test", "견적이", "010-2000-2000");
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

        QuoteService quoteService = new QuoteService(
                new JdbcCancerProductQueryRepository(jdbcTemplate),
                new JdbcQuoteRepository(jdbcTemplate, OBJECT_MAPPER)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuoteController(quoteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("서버 견적은 상품·담보·보험료·약관 hash를 발행 시점 snapshot으로 고정한다")
    void issuesImmutableServerSnapshot() throws Exception {
        Long quoteId = issueQuote(firstUserId, productCoverageIds);

        String snapshotBefore = jdbcTemplate.queryForObject("""
                SELECT snapshot_json::TEXT
                FROM public.ins_quote
                WHERE quote_id = ?
                """, String.class, quoteId);
        assertThat(snapshotBefore).contains("CAPSURE-DEMO-CANCER", TERMS_HASH);

        jdbcTemplate.update("""
                UPDATE public.ins_product_version
                SET base_monthly_premium = 99999.00
                WHERE product_version_id = ?
                """, productVersionId);
        try {
            mockMvc.perform(get("/api/v1/quotes/{quoteId}", quoteId)
                            .principal(authentication(firstUserId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ISSUED"))
                    .andExpect(jsonPath("$.data.monthlyPremium").value(29900.00))
                    .andExpect(jsonPath("$.data.snapshot.monthlyPremium").value(29900.00))
                    .andExpect(jsonPath("$.data.snapshot.coverages.length()").value(3))
                    .andExpect(jsonPath("$.data.snapshot.termsHash").value(TERMS_HASH))
                    .andExpect(jsonPath("$.data.snapshot.coverages[0].waitingPeriodDays").value(90));
        } finally {
            jdbcTemplate.update("""
                    UPDATE public.ins_product_version
                    SET base_monthly_premium = 29900.00
                    WHERE product_version_id = ?
                    """, productVersionId);
        }

        assertThat(jdbcTemplate.queryForObject("""
                SELECT snapshot_json::TEXT
                FROM public.ins_quote
                WHERE quote_id = ?
                """, String.class, quoteId)).isEqualTo(snapshotBefore);
    }

    @Test
    @DisplayName("상품 버전에 속하지 않거나 중복된 담보 선택은 422를 반환한다")
    void rejectsInvalidCoverageSelection() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                        .principal(authentication(firstUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteRequest(List.of(productCoverageIds.getFirst(), 999999L))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(post("/api/v1/quotes")
                        .principal(authentication(firstUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteRequest(List.of(
                                productCoverageIds.getFirst(),
                                productCoverageIds.getFirst()
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("다른 사용자의 견적은 존재 여부를 노출하지 않고 404를 반환한다")
    void hidesQuoteFromOtherUser() throws Exception {
        Long quoteId = issueQuote(firstUserId, productCoverageIds);

        mockMvc.perform(get("/api/v1/quotes/{quoteId}", quoteId)
                        .principal(authentication(secondUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("만료 시각이 지난 견적 조회는 원장 상태를 EXPIRED로 전환한다")
    void expiresStaleQuote() throws Exception {
        Long quoteId = issueQuote(firstUserId, productCoverageIds);
        jdbcTemplate.update("""
                UPDATE public.ins_quote
                SET expires_at = NOW() - INTERVAL '1 second'
                WHERE quote_id = ?
                """, quoteId);

        mockMvc.perform(get("/api/v1/quotes/{quoteId}", quoteId)
                        .principal(authentication(firstUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));

        assertThat(jdbcTemplate.queryForObject("""
                SELECT status
                FROM public.ins_quote
                WHERE quote_id = ?
                """, String.class, quoteId)).isEqualTo("EXPIRED");
    }

    private static Long issueQuote(Long userId, List<Long> selectedCoverageIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/quotes")
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteRequest(selectedCoverageIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.monthlyPremium").value(29900.00))
                .andExpect(jsonPath("$.data.snapshot.coverages.length()").value(selectedCoverageIds.size()))
                .andReturn();
        JsonNode body = OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray());
        return body.path("data").path("quoteId").asLong();
    }

    private static String quoteRequest(List<Long> selectedCoverageIds) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "productVersionId", productVersionId,
                "selectedProductCoverageIds", selectedCoverageIds
        ));
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
