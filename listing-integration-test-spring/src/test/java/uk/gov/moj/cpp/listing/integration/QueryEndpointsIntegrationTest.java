package uk.gov.moj.cpp.listing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Every path of {@code listing-query-api.raml} (17 paths, 18 query names) against the compose app,
 * over the viewstore fixture ({@code fixtures/viewstore.sql}) and the WireMock stubs of the services
 * the queries call. Each test asserts the status and key fields and validates the body against the
 * legacy RAML response schema the path declares (8 schema files; {@code /courtlist} declares none and
 * returns a PDF, the CSV report returns {@code text/csv}).
 *
 * <p>The legacy query ITs of the Maven {@code listing-integration-test} module set their data up with
 * commands, so they move onto this stack with the commands (P2.3/P2.5).
 */
class QueryEndpointsIntegrationTest extends AbstractIntegrationTest {

    static final String COURT_CENTRE_ID = "b52f805c-2821-4904-a0e0-26f7fda6dd08";
    static final String COURT_ROOM_ID = "b4562684-9209-3ec4-a544-7f80dabd94d8";
    static final String UNALLOCATED_HEARING_ID = "7c1d9b2e-3a4f-4b5c-9d6e-000000000002";
    static final String UNSCHEDULED_HEARING_ID = "9e2f0c3b-4b5a-4c6d-8e7f-000000000003";
    static final String CASE_ID = "0a1b2c3d-1111-4a5b-8c9d-000000000001";
    static final String HEARING_DATE = "2030-09-24";

    static final String SEARCH_HEARINGS = "application/vnd.listing.search.hearings+json";
    static final String COURT_CALENDAR = "application/vnd.listing.search.hearings.court.calendar+json";
    static final String SEARCH_COURT_LIST = "application/vnd.listing.search.court.list+json";
    static final String COURT_LIST_PAYLOAD = "application/vnd.listing.search.court.list.payload+json";
    static final String DAILY_LIST_PAYLOAD = "application/vnd.listing.search.daily.list.payload+json";
    static final String PUBLISH_STATUS = "application/vnd.listing.court.list.publish.status+json";
    static final String HEARING_SLOTS = "application/vnd.listing.search.hearing.slots+json";
    static final String VALIDATE_SESSION = "application/vnd.listing.validate.session.availability+json";
    static final String DRAFT_STATUS = "application/vnd.listing.query.court.schedule.draft.status+json";
    static final String CASES_BY_PERSON = "application/vnd.listing.get.cases-by-person-defendant+json";
    static final String CASES_BY_ORGANISATION = "application/vnd.listing.get.cases-by-organisation-defendant+json";
    static final String CACHE_REFRESH = "application/vnd.listing.get.cache-refdata-courtrooms-refresh+json";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void searchHearings_returnsTheHearingOfTheDayWithItsCourtRoomNote() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings?allocated=true&courtCentreId=" + COURT_CENTRE_ID
                + "&searchDate=" + HEARING_DATE, LISTING_OFFICER_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(ids(body.get("hearings"))).containsExactly(HEARING_ID);
        assertThat(body.get("notes").get(0).get("note").asText()).isEqualTo("Court room note");
        // matchedWithQuery compares the hearing day with "now" when no startTime is given (legacy), so
        // only its presence is asserted.
        assertThat(body.get("hearings").get(0).get("hearingDays").get(0).get("matchedWithQuery").isBoolean()).isTrue();
        // Each hearing day carries matchedWithQuery, which hearing.json's hearing day does not allow:
        // legacy behaviour, kept. The rest of the body is validated.
        ResponseSchemas.assertValid("listing.search.hearings.json", withoutFields(body, "matchedWithQuery"));
    }

    @Test
    void availableSearch_findsTheFutureHearingsOfTheCase() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/available-search?caseUrn=TFL1001",
                LISTING_OFFICER_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        ResponseSchemas.assertValid("listing.search.hearings.json", response.getBody());
        assertThat(ids(JSON.readTree(response.getBody()).get("hearings"))).containsExactly(HEARING_ID);
    }

    @Test
    void allocatedAndUnallocated_findsTheHearingsOfTheCase() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/allocated-and-unallocated?caseId=" + CASE_ID,
                LISTING_OFFICER_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(ids(body.get("hearings"))).containsExactly(HEARING_ID);
        // No "notes" (required by the RAML schema) in this response: legacy behaviour, kept.
        ResponseSchemas.assertValid("listing.search.hearings.json", withNotes(body));
    }

    @Test
    void rangeSearch_findsTheAllocatedHearingsInTheDateRange() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/range-search?allocated=true&courtCentreId=" + COURT_CENTRE_ID
                + "&startDate=2030-09-23&endDate=2030-09-27", LISTING_OFFICER_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        ResponseSchemas.assertValid("listing.search.hearings.json", response.getBody());
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(ids(body.get("hearings"))).containsExactly(HEARING_ID);
        assertThat(body.get("results").asInt()).isEqualTo(1);
    }

    @Test
    void rangeSearch_findsTheUnallocatedHearingsInTheWeekCommencingRange() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/range-search?allocated=false&courtCentreId=" + COURT_CENTRE_ID
                + "&weekCommencingStartDate=2030-09-23&weekCommencingEndDate=2030-09-27", LISTING_OFFICER_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        ResponseSchemas.assertValid("listing.search.hearings.json", response.getBody());
        assertThat(ids(JSON.readTree(response.getBody()).get("hearings"))).containsExactly(UNALLOCATED_HEARING_ID);
    }

    @Test
    void courtCalendar_isTheRangeSearchNegotiatedOnAccept() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/range-search?allocated=true&courtCentreId=" + COURT_CENTRE_ID
                + "&startDate=2030-09-23&endDate=2030-09-27", LISTING_OFFICER_USER_ID, COURT_CALENDAR);

        assertOk(response, COURT_CALENDAR);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(ids(body.get("hearings"))).containsExactly(HEARING_ID);
        assertThat(body.get("hearings").get(0).get("hearingDayCount").asInt()).isEqualTo(1);
        assertThat(body.get("hearings").get(0).get("hearingDayPosition").asInt()).isEqualTo(1);
        // The court calendar adds hearingDayCount/hearingDayPosition to each hearing, which the RAML's
        // listing.search.hearings.json (hearing.json, additionalProperties false) does not allow:
        // legacy behaviour, kept. The rest of the body is validated.
        ResponseSchemas.assertValid("listing.search.hearings.json", without(body, "hearingDayCount", "hearingDayPosition"));
    }

    @Test
    void searchHearing_returnsTheHearingById() throws Exception {
        final ResponseEntity<String> response = get(HEARING_PATH, LISTING_OFFICER_USER_ID, SEARCH_HEARING);

        assertOk(response, SEARCH_HEARING);
        ResponseSchemas.assertValid("listing.search.hearing.json", response.getBody());
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.get("id").asText()).isEqualTo(HEARING_ID);
        assertThat(body.get("listedCases").get(0).get("caseIdentifier").get("caseReference").asText()).isEqualTo("TFL1001");
    }

    @Test
    void searchHearing_unknownIdIs404_invalidIdIs400() {
        assertThat(get(QUERY_API + "/hearings/" + UUID.randomUUID(), LISTING_OFFICER_USER_ID, SEARCH_HEARING).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(QUERY_API + "/hearings/not-a-uuid", LISTING_OFFICER_USER_ID, SEARCH_HEARING).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void courtList_isThePdfRenderedBySystemDocGenerator() {
        final ResponseEntity<String> response = get(QUERY_API + "/courtlist?courtCentreId=" + COURT_CENTRE_ID + "&listId=PUBLIC&startDate="
                + HEARING_DATE + "&endDate=" + HEARING_DATE, LISTING_OFFICER_USER_ID, SEARCH_COURT_LIST);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"CourtList.pdf\"");
        assertThat(response.getBody()).isEqualTo("%PDF-1.4 listing court list");
    }

    @Test
    void courtList_unknownListTypeIs400() {
        assertThat(get(QUERY_API + "/courtlist?courtCentreId=" + COURT_CENTRE_ID + "&listId=NO_SUCH_LIST&startDate="
                + HEARING_DATE + "&endDate=" + HEARING_DATE, LISTING_OFFICER_USER_ID, SEARCH_COURT_LIST).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void courtListPayload_isTheTemplatePayloadOfThePublicList() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/courtlistpayload?courtCentreId=" + COURT_CENTRE_ID + "&listId=PUBLIC&startDate="
                + HEARING_DATE + "&endDate=" + HEARING_DATE, SYSTEM_USER_ID, COURT_LIST_PAYLOAD);

        assertOk(response, COURT_LIST_PAYLOAD);
        ResponseSchemas.assertValid("listing.search.court.list.payload.json", response.getBody());
        assertThat(JSON.readTree(response.getBody()).get("templateName").asText()).isNotBlank();
    }

    @Test
    void dailyListPayload_isTheXhibitDailyListOfTheCourtCentre() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/dailylistpayload?courtCentreId=" + COURT_CENTRE_ID + "&startDate="
                + HEARING_DATE + "&publishCourtListType=DRAFT", SYSTEM_USER_ID, DAILY_LIST_PAYLOAD);

        assertOk(response, DAILY_LIST_PAYLOAD);
        ResponseSchemas.assertValid("listing.search.daily.list.payload.json", response.getBody());
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.get("documentType").asText()).isEqualTo("DRAFT");
        assertThat(body.get("sittingDate").asText()).isEqualTo(HEARING_DATE);
        assertThat(body.get("courtLists")).isNotEmpty();
    }

    @Test
    void courtListPublishStatus_returnsTheLatestStatusPerListType() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/courtListPublishStatus/" + COURT_CENTRE_ID
                + "?publishCourtListTypes=FINAL&publishDate=" + HEARING_DATE, LISTING_OFFICER_USER_ID, PUBLISH_STATUS);

        assertOk(response, PUBLISH_STATUS);
        // Not validated against listing.court.list.publish.status.json: the RAML names, as this
        // response's schema, a schema of the request parameters (courtCentreId, publishCourtListTypes,
        // weekCommencing; additionalProperties false), which the legacy response
        // {"publishCourtListStatuses": [...]} never matched. Legacy contract defect, reported, kept.
        final JsonNode status = JSON.readTree(response.getBody()).get("publishCourtListStatuses").get(0);
        assertThat(status.get("publishStatus").asText()).isEqualTo("EXPORT_SUCCESSFUL");
        assertThat(status.get("publishCourtListType").asText()).isEqualTo("FINAL");
    }

    @Test
    void hearingSlots_proxiesCourtSchedulerAndAddsTheListingNotes_withoutAccessControl() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearingSlots?panel=ADULT&sessionStartDate=2020-06-01&sessionEndDate=2020-06-05"
                + "&pageSize=10&pageNumber=1&jurisdiction=MAGISTRATES", DENIED_USER_ID, HEARING_SLOTS);

        assertOk(response, HEARING_SLOTS);
        final JsonNode body = JSON.readTree(response.getBody());
        // courtscheduler's slots (the legacy IT stub payload, passed through) carry sessionStartTime,
        // which listing-json's court-schedule.json does not allow; the rest is validated.
        ResponseSchemas.assertValid("listing.search.hearing.slots.json", withoutFields(body, "sessionStartTime"));
        assertThat(body.get("hearingSlots").get(0).get("courtScheduleId").asText()).isEqualTo("0205eb29-5d01-4779-a8c1-3038bc39dc09");
        assertThat(body.has("notes")).isTrue();
    }

    @Test
    void sessionAvailabilityValidation_proxiesCourtScheduler_withoutAccessControl() throws Exception {
        final ResponseEntity<String> response = post(QUERY_API + "/sessionAvailabilityValidation", DENIED_USER_ID, VALIDATE_SESSION,
                "{\"courtScheduleIdList\":[{\"courtScheduleId\":\"0205eb29-5d01-4779-a8c1-3038bc39dc09\"}],\"duration\":30}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseSchemas.assertValid("listing.validate.session.availability.response.json", response.getBody());
        assertThat(JSON.readTree(response.getBody()).get("validationResult").get("status").asText()).isEqualTo("SUCCESS");
    }

    @Test
    void courtScheduleDraftStatus_reportsADraftSession_withoutAccessControl() throws Exception {
        final ResponseEntity<String> response = post(QUERY_API + "/courtScheduleDraftStatus", DENIED_USER_ID, DRAFT_STATUS,
                "{\"courtScheduleIdList\":[\"0205eb29-5d01-4779-a8c1-3038bc39dc09\"]}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseSchemas.assertValid("listing.query.court.schedule.draft.status.response.json", response.getBody());
        assertThat(JSON.readTree(response.getBody()).get("anyDraft").asBoolean()).isTrue();
    }

    @Test
    void unscheduledSearch_findsTheUnscheduledHearingOfTheCase() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/unscheduled?caseUrn=TFL3001", LISTING_OFFICER_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(ids(body.get("hearings"))).containsExactly(UNSCHEDULED_HEARING_ID);
        assertThat(body.get("results").asInt()).isEqualTo(1);
        assertThat(body.get("pageCount").asInt()).isEqualTo(1);
        // The RAML declares listing.search.hearings.json, which requires "notes"; this query never
        // returned notes (legacy behaviour, kept), so only the hearings are checked against the schema.
        ResponseSchemas.assertValid("listing.search.hearings.json", withNotes(body));
    }

    @Test
    void anyAllocationSearch_findsTheHearingsOfTheCaseFromTheStartDate() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/any-allocation?caseUrn=tfl1001&startDate=2030-01-01",
                SYSTEM_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(ids(body.get("hearings"))).containsExactly(HEARING_ID);
        ResponseSchemas.assertValid("listing.search.hearings.json", withNotes(body));
    }

    @Test
    void cotrSearch_returnsTheTrialHearingsOfTheCourtCentre() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/cotr-search?courtCentreId=" + COURT_CENTRE_ID
                + "&startDate=2030-09-23&endDate=2030-09-27", LISTING_OFFICER_USER_ID, SEARCH_HEARINGS);

        assertOk(response, SEARCH_HEARINGS);
        final JsonNode body = JSON.readTree(response.getBody());
        assertThat(ids(body.get("hearings"))).isEmpty();
        // No "notes" (required by the RAML schema) in this response: legacy behaviour, kept.
        ResponseSchemas.assertValid("listing.search.hearings.json", withNotes(body));
    }

    @Test
    void casesByPersonDefendant_returnsTheCasesHeardOnTheDate() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/cases/by-person-defendant-and-hearingDate?firstName=John&lastName=Smith&hearingDate="
                + HEARING_DATE, DEFENCE_LAWYER_USER_ID, CASES_BY_PERSON);

        assertOk(response, CASES_BY_PERSON);
        ResponseSchemas.assertValid("listing.search.cases-by-defendant-and-hearingDate.json", response.getBody());
        final JsonNode prosecutionCase = JSON.readTree(response.getBody()).get("prosecutionCases").get(0);
        assertThat(prosecutionCase.get("caseId").asText()).isEqualTo(CASE_ID);
        assertThat(prosecutionCase.get("urn").asText()).isEqualTo("TFL1001");
    }

    @Test
    void casesByOrganisationDefendant_returnsTheCasesHeardOnTheDate() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/cases/by-organisation-defendant-and-hearingDate?organisationName=ACME&hearingDate="
                + HEARING_DATE, DEFENCE_LAWYER_USER_ID, CASES_BY_ORGANISATION);

        assertOk(response, CASES_BY_ORGANISATION);
        ResponseSchemas.assertValid("listing.search.cases-by-defendant-and-hearingDate.json", response.getBody());
        assertThat(JSON.readTree(response.getBody()).get("prosecutionCases").get(0).get("caseId").asText()).isEqualTo(CASE_ID);
    }

    @Test
    void cacheRefresh_reloadsTheCourtRoomsFromReferenceData() throws Exception {
        final ResponseEntity<String> response = get(QUERY_API + "/cache-refdata-courtrooms/refresh", SYSTEM_USER_ID, CACHE_REFRESH);

        assertOk(response, CACHE_REFRESH);
        ResponseSchemas.assertValid("listing.get.cache-refdata-courtrooms-refresh.json", response.getBody());
        assertThat(JSON.readTree(response.getBody()).get("count").asInt()).isEqualTo(1);
    }

    @Test
    void csvReport_isTextCsvAttachment_withoutAccessControl() {
        final ResponseEntity<String> response = get(QUERY_API + "/hearings/download-hearing-csv-report?courtCentreId=" + COURT_CENTRE_ID
                + "&startDate=2030-09-23", DENIED_USER_ID, "text/csv");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.parseMediaType("text/csv"))).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"hearing_report_2030-09-23.csv\"");
        assertThat(response.getBody()).startsWith("Date of hearing,");
        assertThat(response.getHeaders().getFirst("CPPID")).isNull();
    }

    private static void assertOk(final ResponseEntity<String> response, final String mediaType) {
        assertThat(response.getStatusCode()).as(response.getBody()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.parseMediaType(mediaType))).isTrue();
        assertThat(response.getHeaders().getFirst("CPPID")).isNotBlank();
    }

    private static java.util.List<String> ids(final JsonNode hearings) {
        final java.util.List<String> ids = new java.util.ArrayList<>();
        hearings.forEach(hearing -> ids.add(hearing.get("id").asText()));
        return ids;
    }

    private static String withoutFields(final JsonNode body, final String... fields) {
        final JsonNode copy = body.deepCopy();
        removeFields(copy, java.util.List.of(fields));
        return copy.toString();
    }

    private static void removeFields(final JsonNode node, final java.util.List<String> fields) {
        if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
            object.remove(fields);
        }
        node.forEach(child -> removeFields(child, fields));
    }

    private static String without(final JsonNode body, final String... hearingFields) {
        final com.fasterxml.jackson.databind.node.ObjectNode copy = ((com.fasterxml.jackson.databind.node.ObjectNode) body).deepCopy();
        copy.get("hearings").forEach(hearing -> ((com.fasterxml.jackson.databind.node.ObjectNode) hearing).remove(java.util.List.of(hearingFields)));
        return copy.toString();
    }

    private static String withNotes(final JsonNode body) {
        final com.fasterxml.jackson.databind.node.ObjectNode copy = ((com.fasterxml.jackson.databind.node.ObjectNode) body).deepCopy();
        if (!copy.has("notes")) {
            copy.putArray("notes");
        }
        return copy.toString();
    }
}
