package uk.gov.moj.cpp.listing.domain.json;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_WITH_ZONE_ID;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static java.time.ZoneOffset.UTC;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

/**
 * Framework-free replacement for {@code uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer}.
 * Same configuration as the framework mapper (read from the 17.104.0 jar), so POJOs serialise to
 * the same JSON as on WildFly: ZonedDateTime as {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}, dates as ISO
 * strings, UTC (deserialised ZonedDateTimes in the region zone {@code UTC}, as jsr310 2.12), NON_ABSENT inclusion, enums via toString, unknown properties ignored.
 * Also as the framework: constructor parameter names (jackson-module-parameter-names'
 * behaviour, see {@link ParameterNamesModule}) with properties-based creators (modules compiled
 * with {@code -parameters}). Not ported: the framework's AdditionalProperties and IntegerEnum
 * modules (only used by schema-generated classes, which this port does not use).
 */
public class ObjectMapperProducer {

    private static final ObjectMapper SHARED = create();

    public ObjectMapper objectMapper() {
        return SHARED;
    }

    public static ObjectMapper create() {
        final JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(ZonedDateTime.class,
                new ZonedDateTimeSerializer(DateTimeFormatter.ofPattern(ZonedDateTimes.ISO_8601)));
        // The framework ran jackson-datatype-jsr310 2.12.7 (the WAR's WEB-INF/lib), which adjusts a
        // deserialised ZonedDateTime to the context time zone as TimeZone("UTC").toZoneId(), i.e. the
        // region ZoneId "UTC" (toString "...Z[UTC]"). From 2.16 the module normalises that to the
        // offset "Z" by default; disabled so values (equals, toString) match the JEE app (P2.3.3).
        javaTimeModule.disable(JavaTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID);
        return new ObjectMapper()
                .registerModule(javaTimeModule)
                .registerModule(new Jdk8Module())
                .registerModule(new ParameterNamesModule())
                .registerModule(new JakartaJsonModule())
                .configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(WRITE_DATES_WITH_ZONE_ID, false)
                .setTimeZone(TimeZone.getTimeZone(UTC))
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .enable(WRITE_ENUMS_USING_TO_STRING)
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(READ_ENUMS_USING_TO_STRING)
                .setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
    }
}
