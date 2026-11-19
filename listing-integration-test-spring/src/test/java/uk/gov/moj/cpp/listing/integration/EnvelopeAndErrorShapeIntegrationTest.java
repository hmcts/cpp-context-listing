package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Asserts the listing wire shapes (ported from courtscheduler's
 * {@code EnvelopeAndErrorShapeIntegrationTest}, adapted to listing's legacy contract):
 * <ul>
 *   <li>a successful query response is the payload only, with a {@code CPPID} header, as the
 *       framework's {@code OkStatusEnvelopePayloadEntityResponseStrategy} wrote it (no {@code _metadata});</li>
 *   <li>every error body is {@code {"error":"<msg>"}} (key {@code error}, not {@code errorMessage}),
 *       including the auth filter's 401/403 and the routing 404/406.</li>
 * </ul>
 */
class EnvelopeAndErrorShapeIntegrationTest extends AbstractIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void queryResponseIsPayloadOnlyWithCppidHeader() throws Exception {
        final ResponseEntity<String> response = get(HEARING_PATH, LISTING_OFFICER_USER_ID, SEARCH_HEARING);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.parseMediaType(SEARCH_HEARING))).isTrue();
        assertThat(response.getHeaders().getFirst("CPPID")).isNotBlank();
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.has("_metadata")).isFalse();
        assertThat(body.get("id").asText()).isEqualTo("2f6f2b0a-0b0c-4f43-9d1c-5f4e7f6d8a01");
    }

    @Test
    void commandResponseIs202WithNoBody() {
        final ResponseEntity<String> response =
                post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, VALID_LISTING_NOTE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNullOrEmpty();
    }

    @Test
    void forbiddenBodyIsTheLegacyErrorShape() throws Exception {
        assertOnlyErrorKey(get(HEARING_PATH, DENIED_USER_ID, SEARCH_HEARING), HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthorisedBodyIsTheLegacyErrorShape() throws Exception {
        assertOnlyErrorKey(get(HEARING_PATH, null, SEARCH_HEARING), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void notAcceptableAndNotFoundBodiesAreTheLegacyErrorShape() throws Exception {
        assertOnlyErrorKey(get(HEARING_PATH, LISTING_OFFICER_USER_ID, "application/json"), HttpStatus.NOT_ACCEPTABLE);
        assertOnlyErrorKey(get(QUERY_API + "/no-such-resource", LISTING_OFFICER_USER_ID, SEARCH_HEARING), HttpStatus.NOT_FOUND);
    }

    @Test
    void schemaFailureBodyHasErrorAndValidationErrors() throws Exception {
        final ResponseEntity<String> response = post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, "{}");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.get("error").asText()).startsWith("JSON schema validation has failed");
        assertThat(body.has("errorMessage")).isFalse();
        // As listing's WildFly BadRequestExceptionMapper: the everit detail under validationErrors.
        assertThat(body.get("validationErrors").get("causingExceptions").isArray()).isTrue();
    }

    private static void assertOnlyErrorKey(final ResponseEntity<String> response, final HttpStatus expected) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.isObject()).as("body %s", response.getBody()).isTrue();
        assertThat(body.size()).as("body %s", response.getBody()).isEqualTo(1);
        assertThat(body.get("error").isTextual()).as("body %s", response.getBody()).isTrue();
    }
}
