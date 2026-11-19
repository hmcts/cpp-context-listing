package uk.gov.moj.cpp.listing.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for listing-api {@code @SpringBootTest}s (P2.1): the application context now includes JPA
 * and Liquibase, so it needs a database. One Postgres 16 container per JVM; Spring Boot applies the
 * production Liquibase changelog to it.
 */
public abstract class PostgresContainerSupport {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("listingviewstore")
                .withUsername("listing")
                .withPassword("listing");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "3");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
    }
}
