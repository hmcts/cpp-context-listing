package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the viewstore's {@code jsonb} columns to Jackson 2 {@link JsonNode}, replacing
 * hibernate-types' {@code JsonNodeBinaryType} ({@code @TypeDef(name = "jsonb-node")}) used by the
 * DeltaSpike entities. The column is read as text (works for {@code jsonb} and for the {@code json}
 * that the court-list native queries build with {@code row_to_json}); writes are cast back with
 * {@code @ColumnTransformer(write = "?::jsonb")} on the mapped attribute.
 *
 * <p>A converter is used instead of {@code @JdbcTypeCode(SqlTypes.JSON)} because Spring Boot 4
 * puts Jackson 3 on the classpath and Hibernate's JSON format mapper would not produce the
 * Jackson 2 {@link JsonNode} the ported code works with.
 */
@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final JsonNode attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public JsonNode convertToEntityAttribute(final String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return MAPPER.readTree(dbData);
        } catch (final IOException e) {
            throw new IllegalStateException("Unreadable jsonb column value", e);
        }
    }
}
