package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetHearingIds;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.FileUtil.payloadToObject;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class RangeSearchQueryForMagistratesIT extends AbstractIT {

    public static final String CASE_AND_MATCHED_DEFENDANTS = "CASE_IN_HEARING,MATCHED_DEFENDANTS";

    @Test
    void shouldReturnNotesAndHearingsForMagistratesRangeSearchIfHearingInCourtScheduler() throws IOException {
        final UUID hearingId1 = UUID.fromString("51e0e229-f22e-4ab6-87fa-2b1c07f97028");
        final UUID hearingId2 = UUID.fromString("ed4f666a-866a-4fcb-8c3b-e89f6ce1e7e5");
        String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID masterDefendantId1 = randomUUID();
        final String caseUrn1 = STRING.next();
        final String caseUrnForLinkedCases1 = STRING.next();

        final UUID masterDefendantId2 = randomUUID();
        final String caseUrn2 = STRING.next();
        final String caseUrnForLinkedCases2 = STRING.next();

        final String jurisdictionTypeMags = JurisdictionType.MAGISTRATES.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn1, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeMags,
                caseUrnForLinkedCases1, caseUrnForLinkedCases1);

        final CaseAndDefendantData caseAndDefendantData2 = new CaseAndDefendantData(hearingId2, null, caseUrn2, masterDefendantId2, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeMags,
                caseUrnForLinkedCases2, caseUrnForLinkedCases2);
        stubGetHearingIds(false);
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1, courtCentreId, courtRoomId));
        LocalDate hearingDate = LocalDate.of(2020, 5, 21);
        listCourtHearingSteps1.createListingNotes(hearingDate, "note 1");
        stubListHearingInCourtSessions(listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getId().toString(),
                courtScheduleId, listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getHearingStartTime());
        final ZonedDateTime hearingStartTime = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final UUID courtroomId = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();

        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2, courtCentreId, courtRoomId));
        listCourtHearingSteps2.createListingNotes(hearingDate.plusDays(2), "note 2");
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();

        final Map<String, String> params = getParams();
        params.remove("panel");
        final String queryString = getQueryString(params);
        final RequestParams requestParams = getCourtSchedulerRequestParams(queryString);
        final ResponseData res = pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(2)),
                        withJsonPath("$.pageCount", is(1)),
//                        withJsonPath("$.notes.size()", is(1)),
                        withJsonPath("$.hearings.size()", is(2)),
                        withJsonPath("$.hearings[0].id", is("51e0e229-f22e-4ab6-87fa-2b1c07f97028")),
                        withJsonPath("$.hearings[0].allocated", is(true)),
                        withJsonPath("$.hearings[0].jurisdictionType", is(JurisdictionType.MAGISTRATES.name())),
                        withJsonPath("$.hearings[1].id", is("ed4f666a-866a-4fcb-8c3b-e89f6ce1e7e5")),
                        withJsonPath("$.hearings[1].allocated", is(true)),
                        withJsonPath("$.hearings[1].jurisdictionType", is(JurisdictionType.MAGISTRATES.name()))
//                        withJsonPath("$.notes[0].id", notNullValue()),
//                        withJsonPath("$.notes[0].courtRoomId", notNullValue()),
//                        withJsonPath("$.notes[0].date", notNullValue()),
//                        withJsonPath("$.notes[0].note", notNullValue())
                ))
        );
        JsonObject hearingsRespJsonObj = payloadToObject(res.getPayload());
        JsonArray hearingsJsonArr = hearingsRespJsonObj.getJsonArray("hearings");
        JsonObject bookedSlotsJsonObj = payloadToObject(getPayload(STUB_DATA_PROVISIONAL_BOOKING_SAMPLE_DATA_SINGLE_COURT_SCHEDULE_COUNT_BASED_JSON));
        JsonArray bookedSlotsJsonArr = bookedSlotsJsonObj.getJsonArray("provisionalSlots");
        JsonArray hearingDaysJsonArr = hearingsJsonArr.getJsonObject(0).getJsonArray("hearingDays");
        assertThat(hearingDaysJsonArr.getJsonObject(0).getString("courtScheduleId"), is(bookedSlotsJsonArr.getJsonObject(0).getString("courtScheduleId")));
    }

    @Test
    void shouldReturnNothingForMagistrateRangeSearchIfNoHearingInCourtScheduler() {
        final UUID hearingId1 = UUID.fromString("2329ea2b-b7dd-4aa7-97ba-951ac32aa635");
        String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID masterDefendantId1 = randomUUID();
        final String caseUrn = STRING.next();
        final String caseUrnForLinkedCases = STRING.next();
        final String jurisdictionTypeCrown = JurisdictionType.MAGISTRATES.name();

        final CaseAndDefendantData caseAndDefendantData1 = new CaseAndDefendantData(hearingId1, null, caseUrn, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeCrown,
                caseUrnForLinkedCases, caseUrnForLinkedCases);

        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1));
        listCourtHearingSteps1.createListingNotes(LocalDate.now().plusDays(1), "note 1");
        final ZonedDateTime hearingStartTime = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final UUID courtCentreId = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        stubListHearingInCourtSessions(listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getId().toString(),
                courtScheduleId, listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();

        final Map<String, String> params = getParams();
        params.remove("panel");
        params.put("courtRoomNumber", "2331");
        params.put("courtRoomId", "0205eb29-5d01-4779-a8c1-3038bc39dc09");
        final String queryString = getQueryString(params);

        stubGetHearingIds(true);
        final RequestParams requestParams = getCourtSchedulerRequestParams(queryString);
        final ResponseData res = pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(0)),
                        withJsonPath("$.pageCount", is(0)),
                        withJsonPath("$.notes.size()", is(0)),
                        withJsonPath("$.hearings.size()", is(0))
                ))
        );
        assertThat(res.getPayload(), is(notNullValue()));
    }

    @Test
    void shouldReturnTotalCountAsResultsFromRangeSearch() {
        final UUID masterDefendantId1 = randomUUID();
        final String caseUrn1 = STRING.next();
        final String caseUrnForLinkedCases1 = STRING.next();
        String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID masterDefendantId2 = randomUUID();
        final String caseUrn2 = STRING.next();
        final String caseUrnForLinkedCases2 = STRING.next();

        final String jurisdictionTypeMags = JurisdictionType.MAGISTRATES.name();

        final CaseAndDefendantData caseAndDefendantData1 =
                new CaseAndDefendantData(randomUUID(), null, caseUrn1, masterDefendantId1, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeMags, caseUrnForLinkedCases1, caseUrnForLinkedCases1);

        final CaseAndDefendantData caseAndDefendantData2 =
                new CaseAndDefendantData(randomUUID(), null, caseUrn2, masterDefendantId2, CASE_AND_MATCHED_DEFENDANTS, null, jurisdictionTypeMags, caseUrnForLinkedCases2, caseUrnForLinkedCases2);

        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData1, courtCentreId, courtRoomId));
        stubListHearingInCourtSessions(listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getId().toString(),
                courtScheduleId, listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getHearingStartTime());
        final ZonedDateTime hearingStartTime = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps1.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        listCourtHearingSteps1.whenCaseIsSubmittedForListing();

        ListCourtHearingSteps listCourtHearingSteps2 = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData2, courtCentreId, courtRoomId));
        listCourtHearingSteps2.whenCaseIsSubmittedForListing();


        final Map<String, String> params = getParams();
        params.remove("panel");
        params.remove("oucodeL2Code");
        final String queryString = getQueryString(params);
        stubGetHearingIds(false);
        final RequestParams requestParams = getRangeSearchRequestParams(queryString);
        final ResponseData res = pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.results", is(2)),
                        withJsonPath("$.pageCount", is(1)),
                        withJsonPath("$.hearings.size()", is(2))
                ))
        );
        assertThat(res.getPayload(), is(notNullValue()));
    }

    private RequestParams getCourtSchedulerRequestParams(final String queryString) {
        final String url = format("%s%s%s%s&allocated=true&jurisdictionType=MAGISTRATES&businessType=TRIAL&ouCode=B01LY00&sessionStartDate=2020-06-01&sessionEndDate=2020-06-01&pageSize=10&pageNumber=1", getBaseUri(), "/listing-query-api/query/api/rest/listing/hearings/range-search", "?", queryString);
        return requestParams(url, "application/vnd.listing.search.hearings+json")
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
    }

    private RequestParams getRangeSearchRequestParams(final String queryString) {
        final String url = format("%s%s%s%s&allocated=true&jurisdictionType=MAGISTRATES&sessionStartDate=2020-06-01&sessionEndDate=2020-06-01&pageSize=10&pageNumber=1", getBaseUri(), "/listing-query-api/query/api/rest/listing/hearings/range-search", "?", queryString);
        return requestParams(url, "application/vnd.listing.search.hearings+json")
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
    }
}
