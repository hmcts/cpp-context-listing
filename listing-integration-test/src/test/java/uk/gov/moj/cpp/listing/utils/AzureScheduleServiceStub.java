package uk.gov.moj.cpp.listing.utils;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

public class AzureScheduleServiceStub {

    private static final String ROTA_SL_ENDPOINT_URL = "/fa-ste-ccm-scsl";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";
    private static final String HEARING_SLOTS = "/hearingSlots";

    public static final String LISTING_SEARCH_HEARING_SLOTS_JSON = "stub-data/listing.search.hearing.slots.json";
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
        stubFor(put(urlPathMatching(ROTA_SL_ENDPOINT_URL))
                .willReturn(aResponse().withStatus(NO_CONTENT.getStatusCode())));
    }

    public static void stubGetAvailableHearingSlots(boolean isEmpty) {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .withQueryParam("sessionEndDate", matching("2020-10-11"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(isEmpty ? LISTING_SEARCH_HEARING_EMPTY_SLOTS_JSON : LISTING_SEARCH_HEARING_SLOTS_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate sessionDate) {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_WITH_SESSION_DATE_JSON)
                                .replace("%SESSION_DATE%",sessionDate.toString()))
                        .withHeader("Content-Type", "application/json")
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased() {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased() {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_COUNT_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsSingleCourtScheduleDurationBased() {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_DURATION_BASED_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(String[] courtSchedules) {
        String payload = getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_MULTIPLE_COURT_SCHEDULES_DURATION_BASED_JSON);

        for (int i = 0; i < courtSchedules.length; i++) {
            payload = payload.replace("SESSION_DATE_" + (i + 1), courtSchedules[i]);
        }

        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(payload)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubSessionEndDateEmptyRequest() {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + HEARING_SLOTS)))
                .withQueryParam("sessionStartDate", matching("2017-10-11"))
                .withQueryParam("pageNumber", matching("1"))
                .withQueryParam("pageSize", matching("20"))
                .withQueryParam("panel", matching("ADULT"))
                .withQueryParam("oucodeL2Code", matching("Z01KR05"))
                .willReturn(aResponse().withStatus(BAD_REQUEST.getStatusCode())
                        .withBody("Mandatory Search Criteria sessionEndDate cannot be null")
                ));
    }

}
