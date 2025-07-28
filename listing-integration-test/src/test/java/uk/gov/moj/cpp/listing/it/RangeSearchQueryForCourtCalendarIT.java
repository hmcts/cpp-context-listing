package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.it.SearchAvailableHearingIT.CASE_IN_HEARING;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.RefDataCourtRoomCacheStep;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class RangeSearchQueryForCourtCalendarIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";
    private static final Map<UUID, String> COURT_ROOMS = new LinkedHashMap<>() {{
        put(fromString("f8254db1-1683-483e-afb3-b87fde5a0a24"), "Courtroom 01");
        put(fromString("f8254db1-1683-483e-afb3-b87fde5a0a23"), "Courtroom 03");
        put(fromString("f8254db1-1683-483e-afb3-b87fde5a0a21"), "Courtroom 04");
        put(fromString("f8254db1-1683-483e-afb3-b87fde5a0a22"), "Courtroom 05");
    }};

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() throws JsonProcessingException {
        clearDB();
        new RefDataCourtRoomCacheStep().assertRefreshCache();
    }


    @Test
    public void hearingCanBeSearchedForUsingDifferentCombinationsOfParametersForMagsCourtcalendar() throws JsonProcessingException {
        final UUID magsCourtCenterId = randomUUID();
        final List<TestData> testDataList = new ArrayList<>();
        IntStream.range(0, 7).forEach(i ->
        {
            final UUID magestirateCourtRoomId = new ArrayList<>(COURT_ROOMS.keySet()).get(i % COURT_ROOMS.size());
            final int dayFromToday = i % 3;
            final LocalDate hearingEndDate = LocalDate.now().plusDays(dayFromToday);
            final ZonedDateTime hearingStartTime = ZonedDateTime.now().plusDays(dayFromToday - 1);

            final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate(magsCourtCenterId, magestirateCourtRoomId, hearingEndDate, hearingStartTime);
            final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

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
        final UUID crownCourtCenterId = randomUUID();
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
            final int dayFromToday = i % 3;
            final LocalDate hearingEndDate = LocalDate.now().plusDays(dayFromToday);
            final ZonedDateTime hearingStartTime = ZonedDateTime.now().plusDays(dayFromToday - 1);

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

    private static void checkPayload(final String payload, final int hearingCount, List<TestData> testDataList, final int pageSize, final int pageNumber) throws JsonProcessingException {
        final Map jObj = new ObjectMapper().readValue(payload, Map.class);
        assertThat(jObj, Matchers.is(notNullValue()));
        assertThat((List<Map>) jObj.get("hearings"), hasSize(hearingCount));
        assertThat(jObj.get("pageCount"), Matchers.is(2));

        List<Map> hearings = (List<Map>) jObj.get("hearings");
        assertThat(hearings, hasSize(hearingCount));

        assertThat(hearings.get(0).get("hearingDayCount"), Matchers.is(2));
        assertThat(hearings.get(0).get("hearingDayPosition"), Matchers.is(2));
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
}

record TestData(LocalDate hearingDate, UUID courtRoomId, String courtRoomName, ZonedDateTime hearingTime) {
}
