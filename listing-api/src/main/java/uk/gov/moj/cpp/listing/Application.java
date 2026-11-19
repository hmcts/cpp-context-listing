package uk.gov.moj.cpp.listing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the listing context; replaces the WildFly WAR
 * ({@code listing-service}). Serves both legacy context roots under one servlet context path
 * {@code /listing-service} (ADR 008).
 */
@SpringBootApplication
@SuppressWarnings("HideUtilityClassConstructor")
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
