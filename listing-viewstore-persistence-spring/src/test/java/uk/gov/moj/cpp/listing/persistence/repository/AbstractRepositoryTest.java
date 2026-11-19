package uk.gov.moj.cpp.listing.persistence.repository;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for the {@code @DataJpaTest} repository tests (copied from courtscheduler's
 * {@code AbstractRepositoryTest}): one Postgres 16 container per JVM, the production Liquibase
 * changelog ({@code classpath:liquibase/db-changelog.xml} from :listing-viewstore-liquibase)
 * applied by Spring Boot, and this module's Spring Data and JDBC repositories in the context.
 *
 * <p>Each test runs in a transaction that is rolled back ({@code @DataJpaTest} default), so tests
 * do not see each other's rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("listingviewstore")
                .withUsername("listing")
                .withPassword("listing")
                .withCommand("postgres", "-c", "max_connections=200");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "3");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "uk.gov.moj.cpp.listing.persistence.repository")
    @EntityScan(basePackages = "uk.gov.moj.cpp.listing.persistence")
    @EnableJpaRepositories(basePackages = "uk.gov.moj.cpp.listing.persistence.repository")
    static class TestRepositoriesConfig {
    }
}
