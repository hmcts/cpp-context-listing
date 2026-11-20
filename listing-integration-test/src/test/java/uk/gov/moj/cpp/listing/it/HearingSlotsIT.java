package uk.gov.moj.cpp.listing.it;

import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.POLL_INTERVAL;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.TIMEOUT_IN_MILLIS;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithOverbookedSlots;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetCrownHearingSlotsByWeekCommencing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubSessionEndDateEmptyRequest;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyCrownHearingSlotsSearchSentIsWeekCommencing;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.listing.steps.NotesSteps;
import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

class HearingSlotsIT extends AbstractIT {

    private static final String QUERY_API_PATH = "/listing-query-api/query/api/rest/listing/hearingSlots";
    private static final String QUERY_MEDIA_TYPE = "application/vnd.listing.search.hearing.slots+json";
    private static final String WEEK_COMMENCING_OU_CODE = "C05LV00";
    private static final String WEEK_COMMENCING_MONDAY = "2026-11-23";
    private final NotesSteps notesSteps = new NotesSteps();

    StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    @Test
    void shouldGetHearingSlots() {
        final String queryString = getQueryString(getParams());

        stubGetAvailableHearingSlots(false);
        listAllStubMappings();
        final RequestParams requestParams = getRequestParams(queryString);
        pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(446)),
                        withJsonPath("$.pageCount", is(23)),
                        withJsonPath("$.notes.size()", is(0)),
                        withJsonPath("$.hearingSlots.size()", is(10)),
                        withJsonPath("$.hearingSlots[0].courtScheduleId", is("0205eb29-5d01-4779-a8c1-3038bc39dc09")),
                        withJsonPath("$.hearingSlots[0].sessionDate", is("2020-06-01")),
                        withJsonPath("$.hearingSlots[0].ouCode", is("B01LY00")),
                        withJsonPath("$.hearingSlots[0].courtHouseName", is("Lavender Hill Magistrates' Court")),
                        withJsonPath("$.hearingSlots[0].courtRoomNumber", is(2331)),
                        withJsonPath("$.hearingSlots[0].courtRoomName", is("Courtroom 02")),
                        withJsonPath("$.hearingSlots[0].availableSlots", is(5)),
                        withJsonPath("$.hearingSlots[0].availableDuration", is(0)),
                        withJsonPath("$.hearingSlots[0].maxSlots", is(10)),
                        withJsonPath("$.hearingSlots[0].maxDuration", is(0)),
                        withJsonPath("$.hearingSlots[0].slotStartTimes.size()", is(1))
                ))
        );
    }

    @Test
    void shouldNotReturnHearingsAndNotesWhenThereIsNoSlots() {
        final String queryString = getQueryString(getParams());

        stubGetAvailableHearingSlots(true);
        listAllStubMappings();
        final RequestParams requestParams = getRequestParams(queryString);
        pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(0)),
                        withJsonPath("$.pageCount", is(0)),
                        withJsonPath("$.notes.size()", is(0)),
                        withJsonPath("$.hearingSlots.size()", is(0))
                ))
        );
    }

    @Test
    void shouldGetHearingSlotsAndNotes() {
        final String queryString = getQueryString(getParams());
        createListingNotes();
        stubGetAvailableHearingSlots(false);
        listAllStubMappings();
        final RequestParams requestParams = getRequestParams(queryString);
        pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(446)),
                        withJsonPath("$.pageCount", is(23)),
                        withJsonPath("$.notes.size()", is(10)),
                        withJsonPath("$.hearingSlots.size()", is(10)),
                        withJsonPath("$.hearingSlots[0].courtScheduleId", is("0205eb29-5d01-4779-a8c1-3038bc39dc09")),
                        withJsonPath("$.hearingSlots[0].sessionDate", is("2020-06-01")),
                        withJsonPath("$.hearingSlots[0].ouCode", is("B01LY00")),
                        withJsonPath("$.hearingSlots[0].courtHouseName", is("Lavender Hill Magistrates' Court")),
                        withJsonPath("$.hearingSlots[0].courtRoomNumber", is(2331)),
                        withJsonPath("$.hearingSlots[0].courtRoomName", is("Courtroom 02")),
                        withJsonPath("$.hearingSlots[0].availableSlots", is(5)),
                        withJsonPath("$.hearingSlots[0].availableDuration", is(0)),
                        withJsonPath("$.hearingSlots[0].maxSlots", is(10)),
                        withJsonPath("$.hearingSlots[0].maxDuration", is(0)),
                        withJsonPath("$.hearingSlots[0].slotStartTimes.size()", is(1)),
                        withJsonPath("$.notes[0].id", notNullValue()),
                        withJsonPath("$.notes[0].courtRoomId", notNullValue()),
                        withJsonPath("$.notes[0].date",notNullValue()),
                        withJsonPath("$.notes[0].note", notNullValue())
                ))
        );
    }


    @Test
    @ExpectedServerErrors("query without sessionEndDate -> ERROR 'hearingSlotsSearch from rota returned an error : Mandatory Search Criteria sessionEndDate cannot be null with status 400'")
    void shouldReturnErrorWhenSessionEndDateIsEmpty() {
        final Map<String, String> params = getParams();
        params.remove("sessionEndDate");

        final String queryString = getQueryString(params);
        stubSessionEndDateEmptyRequest();

        final ResponseData responseData = queryService(getRequestParams(queryString));

        assertThat(responseData.getStatus().getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        assertThat(responseData.getPayload(), is("Mandatory Search Criteria sessionEndDate cannot be null"));
    }

    @Test
    public void shouldGetHearingSlotsWithOverbookedSlotsWhenShowOverbookedSlotsIsTrue() {
        final Map<String, String> params = getParams();
        params.put("showOverbookedSlots", "true");
        final String queryString = getQueryString(params);

        stubGetAvailableHearingSlotsWithOverbookedSlots(true);
        listAllStubMappings();
        final RequestParams requestParams = getRequestParams(queryString);
        final ResponseData responseData = pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(446)),
                        withJsonPath("$.pageCount", is(23)),
                        withJsonPath("$.notes.size()", is(0)),
                        withJsonPath("$.hearingSlots.size()", is(10)),
                        withJsonPath("$.hearingSlots[0].courtScheduleId", is("0205eb29-5d01-4779-a8c1-3038bc39dc09")),
                        withJsonPath("$.hearingSlots[0].sessionDate", is("2020-06-01")),
                        withJsonPath("$.hearingSlots[0].ouCode", is("B01LY00")),
                        withJsonPath("$.hearingSlots[0].courtHouseName", is("Lavender Hill Magistrates' Court")),
                        withJsonPath("$.hearingSlots[0].courtRoomNumber", is(2331)),
                        withJsonPath("$.hearingSlots[0].courtRoomName", is("Courtroom 02")),
                        withJsonPath("$.hearingSlots[0].availableSlots", is(5)),
                        withJsonPath("$.hearingSlots[0].availableDuration", is(0)),
                        withJsonPath("$.hearingSlots[0].maxSlots", is(10)),
                        withJsonPath("$.hearingSlots[0].maxDuration", is(0)),
                        withJsonPath("$.hearingSlots[0].slotStartTimes.size()", is(1))
                ))
        );

        // Additional assertions to verify the showOverbookedSlots parameter behavior
        assertThat("Response should contain hearing slots when showOverbookedSlots is true", 
                responseData.getPayload(), notNullValue());
        assertThat("Query string should contain showOverbookedSlots parameter", 
                queryString, is(notNullValue()));
        assertThat("showOverbookedSlots parameter should be set to true", 
                queryString.contains("showOverbookedSlots=true"), is(true));
    }

    @Test
    public void shouldNotReturnOverbookedSlotsWhenShowOverbookedSlotsIsFalse() {
        final Map<String, String> params = getParams();
        params.put("showOverbookedSlots", "false");
        final String queryString = getQueryString(params);

        stubGetAvailableHearingSlotsWithOverbookedSlots(false);
        listAllStubMappings();
        final RequestParams requestParams = getRequestParams(queryString);
        final ResponseData responseData = pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(0)),
                        withJsonPath("$.pageCount", is(0)),
                        withJsonPath("$.notes.size()", is(0)),
                        withJsonPath("$.hearingSlots.size()", is(0))
                ))
        );

        // Additional assertions to verify the showOverbookedSlots parameter behavior
        assertThat("Response should be empty when showOverbookedSlots is false", 
                responseData.getPayload(), notNullValue());
        assertThat("Query string should contain showOverbookedSlots parameter", 
                queryString, is(notNullValue()));
        assertThat("showOverbookedSlots parameter should be set to false", 
                queryString.contains("showOverbookedSlots=false"), is(true));
        assertThat("Response should indicate no results when overbooked slots are filtered out", 
                responseData.getPayload().contains("\"results\":0"), is(true));
    }


    @Test
    void shouldSendIsWeekCommencingTrueToCourtSchedulerWhenItIsTrueInTheQuery() {
        stubGetCrownHearingSlotsByWeekCommencing(WEEK_COMMENCING_OU_CODE, WEEK_COMMENCING_MONDAY);

        pollWithDefaults(getRequestParams(getQueryString(weekCommencingSearch("true")))).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(1)),
                        withJsonPath("$.pageCount", is(1)),
                        withJsonPath("$.notes.size()", is(0)),
                        withJsonPath("$.hearingSlots.size()", is(1)),
                        withJsonPath("$.hearingSlots[0].courtScheduleId", is("7c1d5f0e-3b0a-4c58-9a61-2f4f5d6a9b10")),
                        withJsonPath("$.hearingSlots[0].sessionDate", is(WEEK_COMMENCING_MONDAY)),
                        withJsonPath("$.hearingSlots[0].ouCode", is(WEEK_COMMENCING_OU_CODE)),
                        withJsonPath("$.hearingSlots[0].isWeekCommencing", is(true)),
                        withJsonPath("$.hearingSlots[0].draft", is(true)),
                        withJsonPath("$.hearingSlots[0].maxDuration", is(3600)),
                        withJsonPath("$.hearingSlots[0].availableDuration", is(2520))
                ))
        );
        verifyCrownHearingSlotsSearchSentIsWeekCommencing(WEEK_COMMENCING_OU_CODE, WEEK_COMMENCING_MONDAY, "true");
    }

    @Test
    void shouldNotSendIsWeekCommencingToCourtSchedulerWhenItIsNotInTheQuery() {
        stubGetCrownHearingSlotsByWeekCommencing(WEEK_COMMENCING_OU_CODE, WEEK_COMMENCING_MONDAY);

        pollForOrdinaryCrownSessions(weekCommencingSearch(null));
        verifyCrownHearingSlotsSearchSentIsWeekCommencing(WEEK_COMMENCING_OU_CODE, WEEK_COMMENCING_MONDAY, null);
    }

    @Test
    void shouldSendIsWeekCommencingFalseToCourtSchedulerWhenItIsFalseInTheQuery() {
        stubGetCrownHearingSlotsByWeekCommencing(WEEK_COMMENCING_OU_CODE, WEEK_COMMENCING_MONDAY);

        pollForOrdinaryCrownSessions(weekCommencingSearch("false"));
        verifyCrownHearingSlotsSearchSentIsWeekCommencing(WEEK_COMMENCING_OU_CODE, WEEK_COMMENCING_MONDAY, "false");
    }

    private void pollForOrdinaryCrownSessions(final Map<String, String> search) {
        pollWithDefaults(getRequestParams(getQueryString(search))).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(2)),
                        withJsonPath("$.pageCount", is(1)),
                        withJsonPath("$.hearingSlots.size()", is(2)),
                        withJsonPath("$.hearingSlots[0].courtScheduleId", is("4a8e2c71-6d3f-4b0e-8f15-9c2d7e5a1b36")),
                        withJsonPath("$.hearingSlots[0].ouCode", is(WEEK_COMMENCING_OU_CODE)),
                        withJsonPath("$.hearingSlots[0].isWeekCommencing", is(false)),
                        withJsonPath("$.hearingSlots[1].courtScheduleId", is("b3f7d924-1c5a-4e86-a0d2-6e8b4f3c7a59")),
                        withJsonPath("$.hearingSlots[1].isWeekCommencing", is(false))
                ))
        );
    }

    private static Map<String, String> weekCommencingSearch(final String isWeekCommencing) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("panel", "ADULT");
        params.put("sessionStartDate", WEEK_COMMENCING_MONDAY);
        params.put("sessionEndDate", "2026-12-14");
        params.put("ouCode", WEEK_COMMENCING_OU_CODE);
        params.put("businessType", "GENC");
        params.put("pageSize", "20");
        params.put("pageNumber", "1");
        params.put("status", "DRAFT");
        params.put("jurisdiction", "CROWN");
        if (isWeekCommencing != null) {
            params.put("isWeekCommencing", isWeekCommencing);
        }
        return params;
    }

    private RequestParams getRequestParams(final String queryString) {
        final String url = format("%s%s%s%s", getBaseUri(), QUERY_API_PATH, "?", queryString);
        return requestParams(url, QUERY_MEDIA_TYPE)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
    }

    private ResponseData queryService(final RequestParams requestParams) {
        final Response response = new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
        return new ResponseData(Response.Status.fromStatusCode(response.getStatus()), response.readEntity(String.class), response.getHeaders());
    }

    private void createListingNotes(){
        final JsonObject payload = stringToJsonObjectConverter.convert(getPayload(CourtSchedulerServiceStub.LISTING_SEARCH_HEARING_SLOTS_JSON));
        payload.getJsonArray("hearingSlots").stream().map(h -> (JsonObject) h).forEach(hearing -> {
            notesSteps.createNoteForListing(UUID.fromString(hearing.getString("courtRoomId")), hearing.getString("sessionDate"), "Note 1");
        });
    }
}
