package uk.gov.moj.cpp.listing.command.hearing.listener;

import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The listener port's JSON mapper (MBD refactor P2.3.2, ADR 015): the JEE listeners were given the
 * framework's {@code ObjectMapper} (CDI {@code ObjectMapperProducer}), which writes the viewstore
 * {@code properties} JSON (non-absent fields, framework date formats, enums by {@code toString}).
 * The application's primary {@code ObjectMapper} (REST) has different settings, so the ported
 * listeners take this one by qualifier: the listing-domain-spring {@link ObjectMapperProducer},
 * the framework-equivalent configuration the query side also reads with.
 */
@Configuration
public class ListenerConfiguration {

    public static final String VIEWSTORE_OBJECT_MAPPER = "viewstoreObjectMapper";

    @Bean(name = VIEWSTORE_OBJECT_MAPPER)
    public ObjectMapper viewstoreObjectMapper() {
        return ObjectMapperProducer.create();
    }
}
