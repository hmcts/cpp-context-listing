package uk.gov.moj.cpp.listing.common.client;

import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ListToJsonArrayConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.StringToJsonObjectConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans for the JSON converters the ported code injects (the framework produced them with CDI
 * producers). They use the framework-equivalent object mapper (ObjectMapperProducer), not the
 * web layer's mapper, so POJOs serialise exactly as on WildFly.
 */
@Configuration
public class JsonConvertersConfig {

    @Bean
    public JsonObjectToObjectConverter jsonObjectToObjectConverter() {
        return new JsonObjectToObjectConverter();
    }

    @Bean
    public ObjectToJsonObjectConverter objectToJsonObjectConverter() {
        return new ObjectToJsonObjectConverter();
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public ListToJsonArrayConverter listToJsonArrayConverter() {
        return new ListToJsonArrayConverter<>();
    }

    @Bean
    public StringToJsonObjectConverter stringToJsonObjectConverter() {
        return new StringToJsonObjectConverter();
    }
}
