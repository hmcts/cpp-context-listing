package uk.gov.moj.cpp.listing.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Explicit primary Jackson 2 {@link ObjectMapper} bean (ported from courtscheduler). The web
 * plumbing (schema-validation filter) and cp-auth-rules-filter inject {@code ObjectMapper} by
 * type; declaring it as
 * {@code @Primary} avoids ambiguity with any framework-internal mappers and
 * guarantees there is exactly one primary candidate.
 */
@Configuration
public class JacksonObjectMapperConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // findAndRegisterModules() picks up jackson-datatype-jsr310 (LocalDate, LocalDateTime, ...)
        // so java.time values serialise as ISO strings.
        return new ObjectMapper()
                .findAndRegisterModules()
                // JavaTimeModule defaults to WRITE_DATES_AS_TIMESTAMPS=true, which serializes
                // java.time.LocalDate as [2025,12,15] instead of "2025-12-15"; the legacy wire
                // format is the string form.
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JakartaJsonModule());
    }
}
