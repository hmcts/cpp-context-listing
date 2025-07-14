package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class CourtSchedulerServiceStub {

    private static final String ROTA_SL_ENDPOINT_URL = "/fa-ste-ccm-scsl";
    private static final String COURT_SCHEDULER_ENDPOINT = "/listingcourtscheduler-api/rest/courtscheduler";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";
    private static final String HEARING_SLOTS = "/hearingslots";
    private static final String COURTSCHEDULER_GET_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.get.hearing.slots+json";
    private static final String COURTSCHEDULER_PUT_HEARING_SLOTS_TYPE = "application/vnd.courtscheduler.update.hearing.slots+json";
    public static final String COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE = "application/vnd.courtscheduler.get.provisional.booking+json";
    public static final String ROTASL_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES = "stub-data/rotasl.get.hearing.slots.with-judiciaries.json";
    public static final String LISTING_SEARCH_HEARING_SLOTS_JSON = "stub-data/listing.search.hearing.slots.json";
    public static final String LISTING_UPDATE_HEARING_SLOTS_JSON = "stub-data/listing.update.slots.json";
    public static final String LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON = "stub-data/listing.search.hearing.slots.empty.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON = "stub-data/provisionalBookingSampleDataSingleCourtScheduleCountBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_JSON = "stub-data/provisionalBookingSampleDataMultipleCourtSchedulesCountBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_DURATION_BASED_JSON = "stub-data/provisionalBookingSampleDataSingleCourtScheduleDurationBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_DURATION_BASED_JSON = "stub-data/provisionalBookingSampleDataMultipleCourtSchedulesDurationBased.json";
    public static final String STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_WITH_SESSION_DATE_JSON = "stub-data/provisionalBookingSampleDataSingleCourtScheduleCountBasedWithSessionDate.json";

    static {
        configureFor(HOST, 8080);
    }

    public static void stubUpdateAvailableHearingSlotsService() {
        stubFor(post(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .willReturn(aResponse().withStatus(NO_CONTENT.getStatusCode())));
    }

    public static void stubDeleteAvailableHearingSlotsService(final String hearingId) {
        stubFor(delete(urlPathMatching(COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS + "/" + hearingId))
                .willReturn(aResponse().withStatus(OK.getStatusCode())));
    }

    public static void verifyDeleteAvailableHearingSlotsStubCommandInvoked(final String hearingId) {
        await().atMost(30, SECONDS).pollInterval(1, SECONDS).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = deleteRequestedFor(urlPathMatching(COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS + "/" + hearingId));
            try {
                verify(exactly(1), requestPatternBuilder);
            } catch (VerificationException e) {
                return false;
            }
            return true;
        });
    }

    public static void stubGetAvailableHearingSlots(boolean isEmpty) {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .withQueryParam("sessionEndDate", matching("2020-10-11"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(isEmpty ? LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON : LISTING_SEARCH_HEARING_SLOTS_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetAvailableHearingSlots() {
        stubFor(get(urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(LISTING_SEARCH_HEARING_SLOTS_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubUpdateHearingSlots(final String hearingDate, final UUID courtScheduleId1) {

        String payload = getPayload(LISTING_UPDATE_HEARING_SLOTS_JSON).
                replace("HEARING_DATE_1", hearingDate).
                replace("COURT_SCHEDULE_ID_1", courtScheduleId1.toString());

        stubFor(put(urlPathEqualTo(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withHeader("content-type", containing(COURTSCHEDULER_PUT_HEARING_SLOTS_TYPE))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetHearingIds(boolean isEmpty) {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("1900-01-01"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT,YOUTH"))
                .withQueryParam("ouCode", matching("B01LY00"))
                .withQueryParam("sessionEndDate", matching("9999-01-01"))
                .withHeader("Accept", containing("application/vnd.courtscheduler.get.hearing.ids+json"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(isEmpty ? "stub-data/listing.search.hearing.ids.empty.json" : "stub-data/listing.search.hearing.ids.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate sessionDate, final Map<String, String> values) {
        final String courtRoomId = ofNullable(values.get("courtRoomId")).orElse("fce80cd4-0c00-3c30-9471-2c2ee7a52453");
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_WITH_SESSION_DATE_JSON)
                                .replace("%SESSION_DATE%", sessionDate.toString())
                                .replace("%COURT_ROOM_ID%", courtRoomId))
                        .withHeader("Content-Type", "application/json")
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_DURATION_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(final Map<String, String> courtRoomScedules, final String courtCentreId) {
        String payload = getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_DURATION_BASED_JSON);

        int idx = 0;
        for (Map.Entry<String, String> schedule : courtRoomScedules.entrySet()) {
            ++idx;
            payload = payload.replace("SESSION_DATE_" + (idx), schedule.getKey());
            payload = payload.replace("COURT_ROOM_ID_" + (idx), schedule.getValue());
            payload = payload.replace("COURT_CENTRE_ID", courtCentreId);
        }

        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static JsonObject stubGetAvailableHearingSlotsWithQueryParams(boolean isEmpty,
                                                                         final String courtRoomId,
                                                                         final String ouCode,
                                                                         final String sessionStartDate,
                                                                         final String sessionEndDate) throws IOException {
        final String getHearingSlotsPayload = getPayload(isEmpty ? LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON :
                ROTASL_GET_HEARING_SLOTS_RESPONSE_JSON_WITH_JUDICIARIES)
                .replaceAll("COURT_ROOM_ID", courtRoomId)
                .replaceAll("OU_CODE", ouCode)
                .replaceAll("SESSION_DATE", sessionStartDate);

        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching(sessionStartDate))
                .withQueryParam("sessionEndDate", matching(sessionEndDate))
                .withQueryParam("ouCode", matching(ouCode))
                .withQueryParam("pageSize", matching("1"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("courtRoomId", matching(courtRoomId))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getHearingSlotsPayload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));

        return FileUtil.payloadToObject(getHearingSlotsPayload);
    }

    public static void stubSessionEndDateEmptyRequest() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_HEARING_SLOTS_TYPE))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria sessionEndDate cannot be null")
                ));
    }

}
