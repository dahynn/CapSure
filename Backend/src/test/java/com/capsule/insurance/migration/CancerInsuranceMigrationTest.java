package com.capsule.insurance.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class CancerInsuranceMigrationTest {

    private static final String TERMS_RESOURCE = "terms/demo-cancer-terms-v1.md";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_migration_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static Flyway flyway;

    @BeforeAll
    static void migrateSchema() throws Exception {
        createLegacySchema();

        flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .cleanDisabled(true)
                .load();

        MigrateResult result = flyway.migrate();
        assertThat(result.migrationsExecuted).isEqualTo(11);
    }

    @Test
    @DisplayName("암보험 코어 migration은 필요한 원장 테이블을 생성한다")
    void createsCancerInsuranceLedgerTables() throws SQLException {
        assertThat(queryLong("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'ins_product_version', 'ins_terms_document', 'ins_application', 'ins_policy',
                    'pay_order', 'clm_claim', 'clm_decision', 'ops_job_execution', 'ops_outbox_event',
                    'ops_financial_event_audit', 'ops_recovery_action', 'ifc_financial_message',
                    'ins_premium_receivable', 'pay_collection_instruction',
                    'pay_premium_settlement', 'pay_refund_case'
                  )
                """)).isEqualTo(16);

        assertThat(queryLong("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'pay_order'
                  AND column_name IN (
                    'reconciliation_attempt_count',
                    'reconciliation_available_at',
                    'reconciliation_locked_at',
                    'reconciliation_locked_by'
                  )
                """)).isEqualTo(4);

        assertThat(queryLong("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'usr_user'
                  AND column_name = 'access_role'
                """)).isEqualTo(1);

        assertThat(queryLong("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'ops_job_execution'
                  AND column_name IN (
                    'processed_count', 'resolved_count', 'still_unknown_count', 'failed_count'
                  )
                """)).isEqualTo(4);
    }

    @Test
    @DisplayName("가상 암보험 한 상품은 세 담보를 가지며 보험료는 상품 버전에 한 번만 저장한다")
    void seedsOneProductWithThreeCoveragesAndOnePremium() throws SQLException {
        assertThat(queryLong("""
                SELECT COUNT(*)
                FROM ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = '1.0.0'
                """)).isEqualTo(1);

        assertThat(queryLong("""
                SELECT COUNT(*)
                FROM ins_product_coverage product_coverage
                JOIN ins_product_version product
                  ON product.product_version_id = product_coverage.product_version_id
                WHERE product.product_code = 'CAPSURE-DEMO-CANCER'
                  AND product.version = '1.0.0'
                """)).isEqualTo(3);

        assertThat(queryString("""
                SELECT base_monthly_premium::TEXT
                FROM ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = '1.0.0'
                """)).isEqualTo("29900.00");
    }

    @Test
    @DisplayName("DB에 고정한 약관 해시는 실제 fixture 파일과 일치한다")
    void termsHashMatchesFixtureBytes() throws Exception {
        assertThat(queryString("""
                SELECT source_hash
                FROM ins_terms_document
                WHERE document_code = 'CAPSURE-DEMO-CANCER-TERMS'
                  AND document_version = '1.0.0'
                """)).isEqualTo(resourceSha256(TERMS_RESOURCE));

        assertThat(queryLong("""
                SELECT COUNT(*)
                FROM ins_terms_clause clause
                JOIN ins_terms_document document
                  ON document.terms_document_id = clause.terms_document_id
                WHERE document.document_code = 'CAPSURE-DEMO-CANCER-TERMS'
                  AND document.document_version = '1.0.0'
                """)).isEqualTo(15);
    }

    @Test
    @DisplayName("완료된 migration을 다시 실행해도 데이터가 중복되지 않는다")
    void rerunIsIdempotent() throws SQLException {
        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isZero();
        assertThat(queryLong("SELECT COUNT(*) FROM ins_product_version")).isEqualTo(1);
        assertThat(queryLong("SELECT COUNT(*) FROM ins_product_coverage")).isEqualTo(3);
        assertThat(queryLong("SELECT COUNT(*) FROM ins_terms_clause")).isEqualTo(15);
    }

    @Test
    @DisplayName("환급 케이스의 멱등키는 중복 수납 환급을 한 건으로 제한한다")
    void enforcesRefundCaseIdempotency() throws SQLException {
        execute("INSERT INTO usr_user (email, name, phone, user_status) VALUES ('refund-test@example.com', '테스트', '010-0000-0000', 'ACTIVE')");
        execute("""
                INSERT INTO ins_quote (quote_no, user_id, product_version_id, status, monthly_premium, snapshot_json, terms_document_hash, expires_at)
                SELECT 'QUOTE-REFUND-TEST', u.user_id, p.product_version_id, 'ISSUED', 29900, '{}'::jsonb, t.source_hash, NOW() + INTERVAL '1 day'
                FROM usr_user u JOIN ins_product_version p ON p.product_code = 'CAPSURE-DEMO-CANCER'
                JOIN ins_terms_document t ON t.terms_document_id = p.terms_document_id WHERE u.email = 'refund-test@example.com'
                """);
        execute("""
                INSERT INTO ins_application (application_no, quote_id, applicant_user_id, insured_user_id, status)
                SELECT 'APP-REFUND-TEST', q.quote_id, u.user_id, u.user_id, 'APPROVED' FROM ins_quote q JOIN usr_user u ON u.email = 'refund-test@example.com' WHERE q.quote_no = 'QUOTE-REFUND-TEST'
                """);
        execute("""
                INSERT INTO ins_policy (policy_no, application_id, policyholder_user_id, insured_user_id, beneficiary_user_id, status, activated_at)
                SELECT 'POL-REFUND-TEST', a.application_id, u.user_id, u.user_id, u.user_id, 'ACTIVE', NOW() FROM ins_application a JOIN usr_user u ON u.email = 'refund-test@example.com' WHERE a.application_no = 'APP-REFUND-TEST'
                """);
        execute("""
                INSERT INTO ins_premium_receivable (policy_id, billing_cycle, due_date, grace_ends_on, amount_due, amount_settled, status)
                SELECT policy_id, DATE '2026-09-01', DATE '2026-09-01', DATE '2026-09-15', 29900, 59800, 'OVERPAID' FROM ins_policy WHERE policy_no = 'POL-REFUND-TEST'
                """);
        execute("""
                INSERT INTO pay_refund_case (premium_receivable_id, refund_no, amount, reason_code, status, idempotency_key)
                SELECT premium_receivable_id, 'REF-REFUND-TEST', 29900, 'DUPLICATE_DEBIT', 'AUTO_REFUND_ELIGIBLE', 'duplicate-debit:refund-test' FROM ins_premium_receivable WHERE billing_cycle = DATE '2026-09-01'
                ON CONFLICT (idempotency_key) DO NOTHING
                """);
        execute("""
                INSERT INTO pay_refund_case (premium_receivable_id, refund_no, amount, reason_code, status, idempotency_key)
                SELECT premium_receivable_id, 'REF-REFUND-TEST-RETRY', 29900, 'DUPLICATE_DEBIT', 'AUTO_REFUND_ELIGIBLE', 'duplicate-debit:refund-test' FROM ins_premium_receivable WHERE billing_cycle = DATE '2026-09-01'
                ON CONFLICT (idempotency_key) DO NOTHING
                """);
        assertThat(queryLong("SELECT COUNT(*) FROM pay_refund_case WHERE idempotency_key = 'duplicate-debit:refund-test'"))
                .isEqualTo(1);
    }

    private static void createLegacySchema() throws Exception {
        try (Connection connection = openConnection();
             InputStream input = new ClassPathResource("db/schema/schema.sql").getInputStream();
             Statement statement = connection.createStatement()) {
            statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static long queryLong(String sql) throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String queryString(String sql) throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
    }

    private static String resourceSha256(String resourcePath) throws Exception {
        try (InputStream input = CancerInsuranceMigrationTest.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            assertThat(input).as("fixture resource %s", resourcePath).isNotNull();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.readAllBytes());
            return HexFormat.of().formatHex(digest);
        }
    }
}
