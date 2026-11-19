package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end proof that request bodies are validated against the legacy RAML JSON schemas in the
 * running app: {@code RequestSchemaValidationFilter} is in the filter chain and rejects
 * schema-invalid payloads with 400 before the controller, as the framework did. Ported from
 * courtscheduler's {@code RequestSchemaValidationEndToEndIT}.
 *
 * <p>Schema exercised: {@code listing.command.create-listing-note.json} — three required fields,
 * {@code additionalProperties:false}, and remote {@code $ref}s into the core-domain catalog
 * ({@code courtsDefinitions.json#/definitions/uuid} and {@code datePattern}).
 */
class RequestSchemaValidationEndToEndIT extends AbstractIntegrationTest {

    @Test
    void validBody_passesTheSchemaAndReachesTheController() {
        assertThat(post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, VALID_LISTING_NOTE)
                .getStatusCode().value()).isEqualTo(202);
    }

    @Test
    void emptyObject_isRejectedForMissingRequiredFields() {
        final ResponseEntity<String> response = post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, "{}");
        assertThat(response.getStatusCode().value())
                .as("got %s / %s", response.getStatusCode(), response.getBody()).isEqualTo(400);
        assertThat(response.getBody()).contains("required key [courtRoomId] not found");
    }

    @Test
    void wrongType_isRejected() {
        final ResponseEntity<String> response = post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE,
                VALID_LISTING_NOTE.replace("\"note\"", "42"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("noteDescription");
    }

    @Test
    void unknownField_isRejectedByAdditionalPropertiesFalse() {
        final ResponseEntity<String> response = post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE,
                VALID_LISTING_NOTE.replace("}", ",\"definitelyNotAKnownField\":true}"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("definitelyNotAKnownField");
    }

    @Test
    void remoteRefIsEnforced_courtRoomIdMustBeAUuid() {
        final ResponseEntity<String> response = post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE,
                VALID_LISTING_NOTE.replace("a299cf49-d14a-496f-895f-59c46859b4a9", "not-a-uuid"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("courtRoomId");
    }

    @Test
    void malformedJson_isRejected() {
        assertThat(post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, "{not json")
                .getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void schemaIsCheckedBeforeAccessControl_asOnWildFly() {
        // The JAX-RS schema interceptor ran when the body was read, before the access-control
        // interceptor: an invalid body is 400 even for a user who would be denied (403).
        assertThat(post(LISTING_NOTE_PATH, DENIED_USER_ID, CREATE_LISTING_NOTE, "{}").getStatusCode().value()).isEqualTo(400);
        assertThat(post(LISTING_NOTE_PATH, DENIED_USER_ID, CREATE_LISTING_NOTE, VALID_LISTING_NOTE).getStatusCode().value()).isEqualTo(403);
    }
}
