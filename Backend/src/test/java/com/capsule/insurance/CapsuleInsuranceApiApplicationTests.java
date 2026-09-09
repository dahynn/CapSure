// #Demo Setting
package com.capsule.insurance;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.flyway.locations=classpath:db/migration",
        "coolsms.api-key=test-key",
        "coolsms.api-secret=test-secret",
        "coolsms.from-number=01000000000",
        "operations.outbox.scheduler-enabled=false",
        "operations.payment-reconciliation.scheduler-enabled=false",
        "operations.premium-delinquency.scheduler-enabled=false"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CapsuleInsuranceApiApplicationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_context_test")
            .withUsername("capsure")
            .withPassword("capsure");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        initializeLegacySchema();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    private static void initializeLegacySchema() {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             InputStream input = new ClassPathResource("db/schema/schema.sql").getInputStream();
             Statement statement = connection.createStatement()) {
            statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize the legacy schema for context test", exception);
        }
    }

    @Test
    void contextLoads() {
    }
}
