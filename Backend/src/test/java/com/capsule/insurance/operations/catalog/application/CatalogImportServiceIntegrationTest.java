package com.capsule.insurance.operations.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.common.exception.GlobalExceptionHandler;
import com.capsule.insurance.operations.catalog.api.CatalogOperationsController;
import com.capsule.insurance.operations.catalog.domain.CatalogImportBatch;
import com.capsule.insurance.operations.catalog.domain.CatalogImportExecution;
import com.capsule.insurance.operations.catalog.domain.CatalogImportInterruptedException;
import com.capsule.insurance.operations.catalog.domain.CatalogImportRow;
import com.capsule.insurance.operations.catalog.domain.CatalogImportRunOptions;
import com.capsule.insurance.operations.catalog.dto.CatalogImportExecutionResponse;
import com.capsule.insurance.operations.catalog.infra.FixtureCatalogImportSource;
import com.capsule.insurance.operations.catalog.infra.JdbcCatalogImportExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class CatalogImportServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_catalog_import_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static JdbcTemplate jdbcTemplate;
    private static JdbcCatalogImportExecutionRepository repository;
    private static FixtureCatalogImportSource source;
    private static CatalogImportService service;
    private static MockMvc mockMvc;

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
        repository = new JdbcCatalogImportExecutionRepository(jdbcTemplate);
        ObjectMapper objectMapper = new ObjectMapper();
        source = new FixtureCatalogImportSource(objectMapper);
        service = new CatalogImportService(
                repository,
                source,
                objectMapper,
                new DataSourceTransactionManager(dataSource)
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CatalogOperationsController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("chunk 실패 후 같은 instance를 재실행하면 checkpoint 이후부터 완료하고 canonical은 증가하지 않는다")
    void resumesFromCheckpointAndKeepsCanonicalIdempotent() throws Exception {
        CatalogImportBatch batch = source.load();
        long productCountBefore = count("public.ins_product_version");
        long productCoverageCountBefore = count("public.ins_product_coverage");
        long termsClauseCountBefore = count("public.ins_terms_clause");

        assertThatThrownBy(() -> service.run(
                batch,
                "mapping-v1",
                new CatalogImportRunOptions(2, 1)
        )).isInstanceOf(CatalogImportInterruptedException.class)
                .hasMessageContaining("chunk 1");

        String instanceKey = batch.sourceChecksum() + ":mapping-v1";
        CatalogImportExecution failed = repository
                .findLatest("CANCER_CATALOG_IMPORT", instanceKey)
                .orElseThrow();
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.nextIndex()).isEqualTo(2);
        assertThat(failed.processedChunks()).isEqualTo(1);
        assertThat(failed.inputCount()).isEqualTo(2);
        assertThat(failed.duplicateCount()).isEqualTo(2);

        CatalogImportExecutionResponse completed = service.run(
                batch,
                "mapping-v1",
                CatalogImportRunOptions.production(2)
        );

        assertThat(completed.jobExecutionId()).isEqualTo(failed.jobExecutionId());
        assertThat(completed.executionNo()).isEqualTo(1);
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.nextIndex()).isEqualTo(4);
        assertThat(completed.processedChunks()).isEqualTo(2);
        assertThat(completed.inputCount()).isEqualTo(4);
        assertThat(completed.acceptedCount()).isZero();
        assertThat(completed.duplicateCount()).isEqualTo(3);
        assertThat(completed.quarantinedCount()).isEqualTo(1);
        assertThat(completed.controlTotalMatched()).isTrue();
        assertThat(completed.inputCount()).isEqualTo(
                completed.acceptedCount()
                        + completed.duplicateCount()
                        + completed.quarantinedCount()
        );

        assertThat(jdbcTemplate.queryForObject("""
                SELECT reason_code
                FROM public.ops_quarantine
                WHERE job_execution_id = ?
                """, String.class, completed.jobExecutionId())).isEqualTo("UNKNOWN_COVERAGE");

        assertThat(count("public.ins_product_version")).isEqualTo(productCountBefore);
        assertThat(count("public.ins_product_coverage")).isEqualTo(productCoverageCountBefore);
        assertThat(count("public.ins_terms_clause")).isEqualTo(termsClauseCountBefore);

        CatalogImportExecutionResponse rerun = service.run(
                batch,
                "mapping-v1",
                CatalogImportRunOptions.production(2)
        );
        assertThat(rerun.jobExecutionId()).isEqualTo(completed.jobExecutionId());
        assertThat(rerun.inputCount()).isEqualTo(4);
        assertThat(count("public.ins_product_coverage")).isEqualTo(productCoverageCountBefore);

        mockMvc.perform(post("/api/v1/ops/jobs/catalog-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mappingRuleVersion": "mapping-v1",
                                  "chunkSize": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobExecutionId").value(completed.jobExecutionId()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.controlTotalMatched").value(true));

        mockMvc.perform(get(
                        "/api/v1/ops/job-executions/{jobExecutionId}",
                        completed.jobExecutionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextIndex").value(4))
                .andExpect(jsonPath("$.data.duplicateCount").value(3))
                .andExpect(jsonPath("$.data.quarantinedCount").value(1));
    }

    @Test
    @DisplayName("존재하지 않는 batch 실행 원장은 404를 반환한다")
    void returnsNotFoundForMissingExecution() throws Exception {
        mockMvc.perform(get("/api/v1/ops/job-executions/{jobExecutionId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("신규 담보 행은 accepted로 반영되고 같은 instance 재실행에는 다시 적재되지 않는다")
    void acceptsNewCoverageOnlyOnce() {
        jdbcTemplate.update("""
                INSERT INTO public.ins_coverage (
                    coverage_code,
                    coverage_name,
                    coverage_category,
                    benefit_type,
                    description
                ) VALUES (
                    'DEMO_CANCER_RECOVERY',
                    '암 회복지원금',
                    'CANCER',
                    'FIXED_BENEFIT',
                    'batch accepted 경로를 검증하기 위한 합성 담보'
                )
                ON CONFLICT (coverage_code) DO NOTHING
                """);

        CatalogImportBatch batch = new CatalogImportBatch(
                "accepted-path-fixture",
                "a".repeat(64),
                List.of(new CatalogImportRow(
                        "DEMO-CANCER-1.0.0-RECOVERY",
                        "CAPSURE-DEMO-CANCER",
                        "1.0.0",
                        "DEMO_CANCER_RECOVERY",
                        new BigDecimal("300000"),
                        "KRW",
                        0,
                        0,
                        BigDecimal.ONE,
                        "POLICY_ACTIVATED_AT",
                        4
                ))
        );

        CatalogImportExecutionResponse first = service.run(
                batch,
                "accepted-mapping-v1",
                CatalogImportRunOptions.production(1)
        );
        CatalogImportExecutionResponse second = service.run(
                batch,
                "accepted-mapping-v1",
                CatalogImportRunOptions.production(1)
        );

        assertThat(first.status()).isEqualTo("COMPLETED");
        assertThat(first.inputCount()).isEqualTo(1);
        assertThat(first.acceptedCount()).isEqualTo(1);
        assertThat(first.duplicateCount()).isZero();
        assertThat(first.quarantinedCount()).isZero();
        assertThat(first.controlTotalMatched()).isTrue();
        assertThat(second.jobExecutionId()).isEqualTo(first.jobExecutionId());
        assertThat(second.acceptedCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_product_coverage product_coverage
                JOIN public.ins_coverage coverage
                  ON coverage.coverage_id = product_coverage.coverage_id
                WHERE coverage.coverage_code = 'DEMO_CANCER_RECOVERY'
                """, Long.class)).isEqualTo(1L);
    }

    private static long count(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count == null ? 0 : count;
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
