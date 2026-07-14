package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.it.SearchAvailableHearingIT.CASE_IN_HEARING;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetHearingIdsWithBody;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.RefDataCourtRoomCacheStep;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class RangeSearchQueryForCourtCalendarIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";
    private static final Map<UUID, String> COURT_ROOMS = new LinkedHashMap<>() {{
        put(fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19"), "Courtroom 01");
        put(fromString("18982e9c-2475-36a4-a852-09ab720acfc9"), "Courtroom 03");
        put(fromString("28b922c3-0396-3c68-970f-5b805c7ab1bb"), "Courtroom 04");
        put(fromString("02d9847e-00e9-3c6c-b25c-1adbf5355a52"), "Courtroom 05");
    }};

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() throws JsonProcessingException {
        new RefDataCourtRoomCacheStep().assertRefreshCache();
    }


    @Test
    public void hearingCanBeSearchedForUsingDifferentCombinationsOfParametersForMagsCourtCalendar() throws JsonProcessingException {
        final UUID magsCourtCenterId = getRandomCourtCenterId();
        final List<TestData> testDataList = new ArrayList<>();
        IntStream.range(0, 7).forEach(i ->
        {
            final UUID magestirateCourtRoomId = new ArrayList<>(COURT_ROOMS.keySet()).get(i % COURT_ROOMS.size());
            final int dayFromToday = i % 3;
            final LocalDate hearingEndDate = ItClock.today().plusDays(dayFromToday);
            //This was failing due to startDate/enDate new adjustment/shrinking
            final ZonedDateTime hearingStartTime = ItClock.nowUtc().plusDays(dayFromToday);

            final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate(magsCourtCenterId, magestirateCourtRoomId, hearingEndDate, hearingStartTime);
            final ListCourtHearingSteps listCourtHearingSteps = getListCourtHearingStepsWithStubbedBookingRef(hearingsData, hearingStartTime);
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

            final UpdatedHearingData updatedHearingDataWithUpdatedJudiciary = UpdatedHearingData.updatedHearingDataDifferentJudiciary(hearingsData.getHearingData().get(0));
            final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUpdatedJudiciary);
            updateHearingSteps.whenJudiciaryIsChangedForHearings();
            updateHearingSteps.verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI();
            testDataList.add(new TestData(hearingStartTime.toLocalDate(), magestirateCourtRoomId, COURT_ROOMS.get(magestirateCourtRoomId), hearingStartTime));

        });

        final String payload = new UpdateHearingSteps().verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDateCourtCalendarWithPagination(magsCourtCenterId, 5, 1, 5);
        checkPayload(payload, 5, testDataList, 5, 1);

        final String payload2 = new UpdateHearingSteps().verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDateCourtCalendarWithPagination(magsCourtCenterId, 5, 2, 2);
        checkPayload(payload2, 2, testDataList, 5, 2);
    }


    @Test
    public void shouldRangeSearchCourtCalendarForCrown() throws JsonProcessingException {

        final String jurisdictionType = JurisdictionType.CROWN.name();
        final UUID crownCourtCenterId = getRandomCourtCenterId();
        final List<TestData> testDataList = new ArrayList<>();

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps();

        IntStream.range(0, 7).forEach(i ->
        {
            final UUID hearingId = randomUUID();
            final UUID masterDefendantId = randomUUID();
            final UUID crownCourtRoomId = new ArrayList<>(COURT_ROOMS.keySet()).get(i % COURT_ROOMS.size());

            final String caseUrn = STRING.next();
            final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, caseUrn, caseUrn, masterDefendantId, CASE_IN_HEARING, jurisdictionType, jurisdictionType,
                    null, null);
            final int dayFromToday = i % 3 ;
            final LocalDate hearingEndDate = ItClock.today().plusDays(dayFromToday + 1);
            final ZonedDateTime hearingStartTime = ItClock.nowUtc().plusDays(dayFromToday);

            ListCourtHearingSteps listCourtHearingSteps1 = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData, crownCourtCenterId, crownCourtRoomId, hearingEndDate, hearingStartTime));
            testDataList.add(new TestData(hearingStartTime.toLocalDate(), crownCourtRoomId, COURT_ROOMS.get(crownCourtRoomId), hearingStartTime));
            listCourtHearingSteps1.whenCaseIsSubmittedForListing();
            updateHearingSteps.pollUntilHearingIsPresentWithHearingId(crownCourtCenterId.toString(), ALLOCATED, getLoggedInUser().toString(), hearingId.toString());
        });

        final String payload = updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDateCourtCalendarWithPagination(crownCourtCenterId, 5, 1, 5);
        checkPayload(payload, 5, testDataList, 5, 1);

        final String payload2 = updateHearingSteps.verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDateCourtCalendarWithPagination(crownCourtCenterId, 5, 2, 2);
        checkPayload(payload2, 2, testDataList, 5, 2);
    }

    /**
     * PATH A (courtscheduler-backed) regression for SPRDT-1164. When the courtscheduler returns the same
     * allocated CROWN hearing id for more than one hearing-day, each day must render as its own row with its
     * own hearing-day data. Before the fix, the shared Hearing/properties instance was mutated and re-added,
     * so the day-of-week the enrichment finished on won: every row collapsed onto the last date and the real
     * date {@code dA} disappeared. The assertion below (dA present in the rendered hearing-days) fails on the
     * old code and passes on the fixed code.
     */
    @Test
    public void crownCourtSchedulerPathKeepsEachHearingDayDistinct() {
        final String jurisdictionType = JurisdictionType.CROWN.name();
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomId = new ArrayList<>(COURT_ROOMS.keySet()).get(0);
        final UUID hearingId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String caseUrn = STRING.next();

        final ZonedDateTime hearingStartTime = ItClock.nowUtc();
        final LocalDate hearingEndDate = ItClock.today().plusDays(1);
        final LocalDate dA = hearingStartTime.toLocalDate();     // the hearing's real (seeded) date
        final LocalDate dB = dA.plusDays(1);                     // a second courtscheduler day for the same id

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, caseUrn, caseUrn,
                masterDefendantId, CASE_IN_HEARING, jurisdictionType, jurisdictionType, null, null);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(
                HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData, courtCentreId, courtRoomId, hearingEndDate, hearingStartTime));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        new UpdateHearingSteps().pollUntilHearingIsPresentWithHearingId(courtCentreId.toString(), ALLOCATED, getLoggedInUser().toString(), hearingId.toString());

        // Courtscheduler returns the SAME allocated hearing id for two hearing-days.
        final String body = "{ \"results\": 2, \"pageCount\": 1, \"hearingIds\": ["
                + "{\"hearingId\":\"" + hearingId + "\",\"courtScheduleId\":\"" + randomUUID() + "\",\"hearingDate\":\"" + dA + "\",\"hearingDayCount\":2,\"hearingDayPosition\":1},"
                + "{\"hearingId\":\"" + hearingId + "\",\"courtScheduleId\":\"" + randomUUID() + "\",\"hearingDate\":\"" + dB + "\",\"hearingDayCount\":2,\"hearingDayPosition\":2}"
                + "] }";
        stubGetHearingIdsWithBody(body);

        final String url = String.format("%s/listing-query-api/query/api/rest/listing/hearings/range-search"
                        + "?allocated=true&jurisdictionType=CROWN&courtSession=AD&ouCode=C01CY00&courtCentreId=%s"
                        + "&startDate=%s&endDate=%s&pageSize=40&pageNumber=1",
                getBaseUri(), courtCentreId, ItClock.today(), ItClock.today().plusDays(3));
        final RequestParams requestParams = requestParams(url, "application/vnd.listing.search.hearings.court.calendar+json")
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();

        pollWithDefaults(requestParams).until(status().is(OK),
                payload().isJson(allOf(
                        withJsonPath("$.hearings.size()", is(2)),
                        withJsonPath("$.hearings[0].id", is(hearingId.toString())),
                        withJsonPath("$.hearings[0].allocated", is(true)),
                        withJsonPath("$.hearings[0].jurisdictionType", is("CROWN")),
                        // the real date survives per-row de-duplication (collapses to a single wrong date pre-fix)
                        withJsonPath("$.hearings[*].hearingDays[*].hearingDate", hasItem(dA.toString()))
                ))
        );
    }

    private static void checkPayload(final String payload, final int hearingCount, List<TestData> testDataList, final int pageSize, final int pageNumber) throws JsonProcessingException {
        final Map jObj = new ObjectMapper().readValue(payload, Map.class);
        assertThat(jObj, Matchers.is(notNullValue()));
        assertThat((List<Map>) jObj.get("hearings"), hasSize(hearingCount));
        assertThat(jObj.get("pageCount"), Matchers.is(2));

        List<Map> hearings = (List<Map>) jObj.get("hearings");
        assertThat(hearings, hasSize(hearingCount));

        assertThat(hearings.get(0).get("hearingDayCount"), Matchers.is(1));
        assertThat(hearings.get(0).get("hearingDayPosition"), Matchers.is(1));
        assertThat((List<Map>) hearings.get(0).get("hearingDays"), hasSize(1));

        final List<TestData> sortedTestData = testDataList.stream()
                .sorted(
                        Comparator.comparing(TestData::hearingDate)
                                .thenComparing(TestData::courtRoomName)
                                .thenComparing(TestData::hearingTime)
                ).toList();

        final List<TestData> pagedTestData = paginate(sortedTestData, pageNumber, pageSize);
        int i = 0;
        for (var testData : pagedTestData) {
            assertThat(hearings.get(i).get("startDate"), Matchers.is(testData.hearingDate().toString()));
            assertThat(hearings.get(i).get("courtRoomId"), Matchers.is(testData.courtRoomId().toString()));
            i++;
        }
    }

    private static <T> List<T> paginate(List<T> sortedList, int pageNumber, int pageSize) {
        if (pageSize <= 0 || pageNumber < 1) {
            throw new IllegalArgumentException("Invalid page size or page number");
        }

        int fromIndex = (pageNumber - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, sortedList.size());

        if (fromIndex >= sortedList.size()) {
            return Collections.emptyList(); // Page beyond data
        }

        return sortedList.subList(fromIndex, toIndex);
    }


    private static ListCourtHearingSteps getListCourtHearingStepsWithStubbedBookingRef(final HearingsData hearingsData, final ZonedDateTime hearingStartTime) {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);

        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                courtScheduleId,
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());

        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId().toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        return listCourtHearingSteps;
    }
}

record TestData(LocalDate hearingDate, UUID courtRoomId, String courtRoomName, ZonedDateTime hearingTime) {
}
