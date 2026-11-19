package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.CACHE_REFRESH;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.CASES_BY_ORGANISATION;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.CASES_BY_PERSON;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.COURT_CALENDAR;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.COURT_CENTRE_ID;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.COURT_LIST_PAYLOAD;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.DAILY_LIST_PAYLOAD;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.HEARING_DATE;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.PUBLISH_STATUS;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.SEARCH_COURT_LIST;
import static uk.gov.moj.cpp.listing.integration.QueryEndpointsIntegrationTest.SEARCH_HEARINGS;

import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * One denied case per access-control rule of the legacy {@code listing-query-api.drl} that an
 * endpoint evaluates (cp-auth-rules-filter, ADR 005): a user outside the rule's groups gets 403 with
 * the legacy {@code {"error": ...}} body. The users come from the WireMock identity stubs: "Listing
 * Officers", "System Users", "Defence Lawyers" and a user with no groups. The four bypassed resources
 * are covered by QueryEndpointsIntegrationTest (called as the user with no groups).
 */
class QueryAccessControlIntegrationTest extends AbstractIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RANGE = "/hearings/range-search?allocated=true&courtCentreId=" + COURT_CENTRE_ID;

    static Stream<Arguments> deniedRequests() {
        return Stream.of(
                // rule, path, Accept, a user outside the rule's groups
                Arguments.of("listing.search.hearings", "/hearings?allocated=true&searchDate=" + HEARING_DATE, SEARCH_HEARINGS, DEFENCE_LAWYER_USER_ID),
                Arguments.of("listing.available.search.hearings", "/hearings/available-search?caseUrn=TFL1001", SEARCH_HEARINGS, SYSTEM_USER_ID),
                Arguments.of("listing.allocated.and.unallocated.hearings", "/hearings/allocated-and-unallocated?caseId=" + UUID.randomUUID(), SEARCH_HEARINGS, DEFENCE_LAWYER_USER_ID),
                Arguments.of("listing.range.search.hearings", RANGE, SEARCH_HEARINGS, SYSTEM_USER_ID),
                Arguments.of("listing.range.search.hearings.court.calendar", RANGE, COURT_CALENDAR, SYSTEM_USER_ID),
                Arguments.of("listing.search.hearing", "/hearings/" + HEARING_ID, SEARCH_HEARING, DEFENCE_LAWYER_USER_ID),
                Arguments.of("listing.search.court.list", "/courtlist?courtCentreId=" + COURT_CENTRE_ID + "&listId=PUBLIC&startDate=" + HEARING_DATE + "&endDate=" + HEARING_DATE, SEARCH_COURT_LIST, SYSTEM_USER_ID),
                Arguments.of("listing.search.court.list.payload", "/courtlistpayload?courtCentreId=" + COURT_CENTRE_ID + "&listId=PUBLIC&startDate=" + HEARING_DATE + "&endDate=" + HEARING_DATE, COURT_LIST_PAYLOAD, DEFENCE_LAWYER_USER_ID),
                Arguments.of("listing.search.daily.list.payload", "/dailylistpayload?courtCentreId=" + COURT_CENTRE_ID + "&startDate=" + HEARING_DATE + "&publishCourtListType=DRAFT", DAILY_LIST_PAYLOAD, DEFENCE_LAWYER_USER_ID),
                Arguments.of("listing.court.list.publish.status", "/courtListPublishStatus/" + COURT_CENTRE_ID + "?publishCourtListTypes=FINAL&publishDate=" + HEARING_DATE, PUBLISH_STATUS, SYSTEM_USER_ID),
                Arguments.of("listing.unscheduled.search.hearings", "/hearings/unscheduled?caseUrn=TFL3001", SEARCH_HEARINGS, SYSTEM_USER_ID),
                Arguments.of("listing.any-allocation.search.hearings", "/hearings/any-allocation?caseUrn=TFL1001&startDate=2030-01-01", SEARCH_HEARINGS, LISTING_OFFICER_USER_ID),
                Arguments.of("listing.cotr.search.hearings", "/hearings/cotr-search?courtCentreId=" + COURT_CENTRE_ID + "&startDate=2030-09-23&endDate=2030-09-27", SEARCH_HEARINGS, DEFENCE_LAWYER_USER_ID),
                Arguments.of("listing.get.cases-by-person-defendant", "/cases/by-person-defendant-and-hearingDate?firstName=J&lastName=S&hearingDate=" + HEARING_DATE, CASES_BY_PERSON, LISTING_OFFICER_USER_ID),
                Arguments.of("listing.get.cases-by-organisation-defendant", "/cases/by-organisation-defendant-and-hearingDate?organisationName=A&hearingDate=" + HEARING_DATE, CASES_BY_ORGANISATION, LISTING_OFFICER_USER_ID),
                Arguments.of("listing.get.cache-refdata-courtrooms-refresh", "/cache-refdata-courtrooms/refresh", CACHE_REFRESH, LISTING_OFFICER_USER_ID)
        );
    }

    @ParameterizedTest(name = "{0}: {3} denied")
    @MethodSource("deniedRequests")
    void userOutsideTheRuleGroupsIsDenied(final String rule, final String path, final String accept, final UUID user) throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + path, user, accept);

        assertThat(response.getStatusCode()).as(rule).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(JSON.readTree(response.getBody()).has("error")).isTrue();
    }

    @ParameterizedTest(name = "{0}: no groups denied")
    @MethodSource("deniedRequests")
    void userWithNoGroupsIsDenied(final String rule, final String path, final String accept, final UUID ignored) {
        assertThat(get(QUERY_API + path, DENIED_USER_ID, accept).getStatusCode()).as(rule).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
