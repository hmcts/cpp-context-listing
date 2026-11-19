package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Exercises cp-auth-rules-filter end-to-end with the DRL ported from the legacy
 * listing-command-api.drl / listing-query-api.drl (ADR 005):
 * <ul>
 *   <li>the action is resolved from the vendor media type (Accept for queries, Content-Type for commands);</li>
 *   <li>the user's groups come from the usersgroups permissions endpoint (WireMock);</li>
 *   <li>the legacy group rules grant or deny.</li>
 * </ul>
 * Ported from courtscheduler's {@code AuthorizationIntegrationTest} (which uses permissions;
 * listing's rules are group-based).
 */
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listingOfficer_isAllowedToSearchAHearing() {
        assertThat(get(HEARING_PATH, LISTING_OFFICER_USER_ID, SEARCH_HEARING).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void systemUser_isAllowedToSearchAHearing() {
        assertThat(get(HEARING_PATH, SYSTEM_USER_ID, SEARCH_HEARING).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userWithNoGroups_isDeniedTheQuery() {
        assertThat(get(HEARING_PATH, DENIED_USER_ID, SEARCH_HEARING).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void defenceLawyer_isDeniedTheQuery_becauseTheRuleListsOtherGroups() {
        assertThat(get(HEARING_PATH, DEFENCE_LAWYER_USER_ID, SEARCH_HEARING).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listingOfficer_isAllowedToCreateAListingNote() {
        assertThat(post(LISTING_NOTE_PATH, LISTING_OFFICER_USER_ID, CREATE_LISTING_NOTE, VALID_LISTING_NOTE).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void systemUser_isDeniedCreatingAListingNote_becauseTheRuleDoesNotListSystemUsers() {
        assertThat(post(LISTING_NOTE_PATH, SYSTEM_USER_ID, CREATE_LISTING_NOTE, VALID_LISTING_NOTE).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void userWithNoGroups_isDeniedTheCommand() {
        assertThat(post(LISTING_NOTE_PATH, DENIED_USER_ID, CREATE_LISTING_NOTE, VALID_LISTING_NOTE).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clientCannotChooseItsActionWithTheCppActionHeader() {
        // AuthzActionGuardFilter hides CPP-ACTION; the action stays the Accept vendor token.
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, SEARCH_HEARING);
        headers.set("CJSCPPUID", DEFENCE_LAWYER_USER_ID.toString());
        headers.set("CPP-ACTION", "listing.get.cases-by-person-defendant");
        final ResponseEntity<String> response = exchange(HEARING_PATH, HttpMethod.GET, new HttpEntity<>(headers));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void severalVendorTypesInAccept_areRejectedWith406() {
        // Otherwise the filter would authorise as the first type while Spring negotiated the second.
        final ResponseEntity<String> response = get(HEARING_PATH, DEFENCE_LAWYER_USER_ID,
                "application/vnd.listing.get.cases-by-person-defendant+json, " + SEARCH_HEARING);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    }
}
