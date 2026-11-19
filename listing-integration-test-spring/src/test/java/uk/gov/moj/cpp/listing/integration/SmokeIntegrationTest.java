package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The dockerised app booted with authz and audit enabled, actuator health is reachable under the
 * servlet context path, and the routing statuses match WildFly/JAX-RS. Ported from courtscheduler's
 * {@code SmokeIntegrationTest}, plus the unknown-Accept → 406 check (§4 invariant 7).
 */
class SmokeIntegrationTest extends AbstractIntegrationTest {

    @Test
    void actuatorHealthIsUpUnderTheContextPath() {
        // /actuator is excluded from the authz filter, so no CJSCPPUID is needed.
        final ResponseEntity<String> response = rest.getForEntity(BASE_URL + "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void prometheusMetricsAreExposed() {
        get(HEARING_PATH, LISTING_OFFICER_USER_ID, SEARCH_HEARING);
        final ResponseEntity<String> response = rest.getForEntity(BASE_URL + "/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("cpp_action_individual").contains("action=\"listing.search.hearing\"");
    }

    @Test
    void noAuthHeaderReturns401() {
        final ResponseEntity<String> response = get(HEARING_PATH, null, SEARCH_HEARING);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unknownAcceptReturns406NotA500OrA403() {
        final ResponseEntity<String> plainJson = get(HEARING_PATH, LISTING_OFFICER_USER_ID, "application/json");
        assertThat(plainJson.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        final ResponseEntity<String> otherVendorType =
                get(HEARING_PATH, LISTING_OFFICER_USER_ID, "application/vnd.listing.search.hearings+json");
        assertThat(otherVendorType.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        // As on WildFly, routing is decided before access control: even a user who would be
        // denied gets 406.
        assertThat(get(HEARING_PATH, DENIED_USER_ID, "text/plain").getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    }

    @Test
    void unknownPathReturns404AndWrongMediaTypeReturns415() {
        assertThat(get(QUERY_API + "/no-such-resource", LISTING_OFFICER_USER_ID, SEARCH_HEARING).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID,
                "application/vnd.listing.command.edit-listing-note+json", "{\"noteDescription\":\"n\"}").getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
