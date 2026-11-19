package uk.gov.moj.cpp.listing.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

/**
 * Renders an everit {@link ValidationException} in the shape of the framework's
 * {@code DefaultJsonValidationLoggerHelper#toJsonObject}, which listing's WildFly
 * {@code BadRequestExceptionMapper} put under {@code validationErrors} in schema-failure 400s:
 * {@code {"message":…, "violatedSchema":"<title - id - description>", "violation":"<pointer>",
 * "causingExceptions":[…]}}.
 */
final class ValidationErrorsRenderer {

    private ValidationErrorsRenderer() {
    }

    static Map<String, Object> render(final ValidationException ve) {
        final Map<String, Object> out = new LinkedHashMap<>();
        if (ve.getMessage() != null) {
            out.put("message", ve.getMessage());
        }
        if (ve.getViolatedSchema() != null) {
            out.put("violatedSchema", schemaName(ve.getViolatedSchema()));
        }
        if (ve.getPointerToViolation() != null) {
            out.put("violation", ve.getPointerToViolation());
        }
        final List<Object> causes = new ArrayList<>();
        ve.getCausingExceptions().forEach(cause -> causes.add(render(cause)));
        out.put("causingExceptions", causes);
        return out;
    }

    private static String schemaName(final Schema schema) {
        final List<String> parts = new ArrayList<>();
        if (schema.getTitle() != null) {
            parts.add(schema.getTitle());
        }
        if (schema.getId() != null) {
            parts.add(schema.getId());
        }
        if (schema.getDescription() != null) {
            parts.add(schema.getDescription());
        }
        return String.join(" - ", parts);
    }
}
