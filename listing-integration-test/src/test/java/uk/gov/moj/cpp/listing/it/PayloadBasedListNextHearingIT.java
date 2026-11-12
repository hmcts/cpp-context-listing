package uk.gov.moj.cpp.listing.it;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.moj.cpp.listing.steps.PayloadBasedListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadBasedListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadBasedUpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.PayloadGenerator;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.endpoint.UnscheduledHearingsEndpoint.pollForUnscheduledHearings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubDeleteAvailableHearingSlotsServiceForAnyHearing;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubJudiciarySearchCourtSchedulesById;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubJudiciarySearchCourtSchedulesByIdServerError;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsForCourtSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;

/**
 * Example integration test demonstrating the new payload-based approach
 * using JSON files from test-data folder instead of nested classes
 */
class PayloadBasedListNextHearingIT extends AbstractIT {

    // fixed date and judiciary in test-data/MAGS/update-hearing-for-listing/update-hearing-for-listing-unallocated-to-allocated.json
    private static final String JUDICIARY_SEARCH_DATE = "2025-05-27";
    private static final List<String> UNALLOCATED_TO_ALLOCATED_JUDICIAL_IDS = List.of(
            "96d362ac-5b2f-4aa3-a4de-9d2969fa3404", "5ad14972-4584-48b4-ab1b-cef786093c2b", "5818a055-35da-4c62-b0aa-a98c85698378");

    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadBasedListNextHearingIT.class);

    @Test
     void shouldListNextHearingsUsingAdhocHearingCreation() {
        // First, create an initial hearing using adhoc hearing creation scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues firstHearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithAdhocHearingCreation();

        // Verify the first hearing was listed
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);

        // Now create next hearings using adjournment crown fixed date scenario
        PayloadBasedListNextHearingSteps listNextHearingSteps = new PayloadBasedListNextHearingSteps(firstHearingValues.hearingId);
        PayloadGenerator.PayloadValues nextHearingValues = listNextHearingSteps.whenListNextHearingSubmittedWithAdjournmentCrownFixedDate();

        // Verify the next hearing was listed
        listNextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.ALLOCATED,2);

        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully with first hearing: {} and next hearing: {}",
                   firstHearingValues.hearingId, nextHearingValues.hearingId);
    }

    @Test
    void shouldUpdateHearingFromUnallocatedToAllocated() {
        // First, create an unallocated hearing using SPI unallocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiUnallocated();

        // Verify the hearing was listed as unallocated
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);

        // Now update the hearing from unallocated to allocated
        PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);
        PayloadGenerator.PayloadValues updatedValues = updateHearingSteps.whenUpdateHearingSubmittedWithUnallocatedToAllocated();

        // Verify the hearing was updated to allocated
        updateHearingSteps.verifyHearingUpdatedFromAPI(AbstractIT.ALLOCATED);
        updateHearingSteps.verifyHearingAllocationStatusChanged(true);

        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully - hearing {} updated from unallocated to allocated",
                   hearingValues.hearingId);
    }

    @Test
    void shouldAssignJudiciaryToHearing() {
        // First, create an allocated hearing using SPI allocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiAllocated();

        // Verify the hearing was listed as allocated
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);

        // Now assign judiciary to the hearing
        PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);

        PayloadGenerator.PayloadValues updatedValues = updateHearingSteps.whenUpdateHearingSubmittedWithAssignJudiciary();

        // Verify judiciary was assigned
        updateHearingSteps.verifyJudiciaryAssignedToHearing();
        updateHearingSteps.verifyHearingUpdatedFromAPI(AbstractIT.ALLOCATED);
        updateHearingSteps.verifyJudiciarySearch(updatedValues);

        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully - judiciary assigned to hearing {}",
                   hearingValues.hearingId);
    }

    @Test
    void shouldSearchJudiciaryInCourtSchedulerSessionOfHearingDay() {
        // The hearing day starts 10:00 UK time (AM by start time), so finding the judiciary in PM proves
        // the Court Scheduler session of its courtScheduleId wins over the start time
        final PayloadGenerator.PayloadValues hearingValues = givenMagistratesHearingAllocatedWithCourtScheduleAndJudiciary();
        stubJudiciarySearchCourtSchedulesById(hearingValues.courtScheduleId, "PM");

        new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues)
                .verifyJudiciarySearchInCourtSession(hearingValues.courtCentreId, hearingValues.courtRoomId, JUDICIARY_SEARCH_DATE,
                        "pmCourtSession", "amCourtSession", UNALLOCATED_TO_ALLOCATED_JUDICIAL_IDS);
    }

    @Test
    void shouldSearchJudiciaryByHearingStartTimeWhenCourtSchedulerFails() {
        final PayloadGenerator.PayloadValues hearingValues = givenMagistratesHearingAllocatedWithCourtScheduleAndJudiciary();
        stubJudiciarySearchCourtSchedulesByIdServerError(hearingValues.courtScheduleId);

        // Court Scheduler fails, so the 10:00 UK start time places the judiciary in AM
        new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues)
                .verifyJudiciarySearchInCourtSession(hearingValues.courtCentreId, hearingValues.courtRoomId, JUDICIARY_SEARCH_DATE,
                        "amCourtSession", "pmCourtSession", UNALLOCATED_TO_ALLOCATED_JUDICIAL_IDS);
    }

    @Test
    void shouldSearchJudiciaryWithoutVacatedTrial() {
        // TS-1a: an allocated hearing with judiciary contributes until its trial is vacated, then nothing
        final String searchDate = "2025-06-03";
        final PayloadGenerator.PayloadValues hearingValues = givenMagistratesHearingAllocated(
                "update-hearing-for-listing-unallocated-to-allocated", Map.of(JUDICIARY_SEARCH_DATE, searchDate));
        final PayloadBasedUpdateHearingSteps searchSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);
        searchSteps.verifyJudiciarySearchInCourtSession(hearingValues.courtCentreId, hearingValues.courtRoomId, searchDate,
                "amCourtSession", "pmCourtSession", UNALLOCATED_TO_ALLOCATED_JUDICIAL_IDS);

        stubDeleteAvailableHearingSlotsServiceForAnyHearing();
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingValues.hearingId);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();

        searchSteps.verifyJudiciarySearchReturnsNoCourtSessions(hearingValues.courtCentreId, hearingValues.courtRoomId, searchDate);
    }

    @Test
    void shouldSearchJudiciaryInCourtSchedulerSessionOfSearchedDayOfMultiDayHearing() {
        // TS-1b: both days start 10:00 UK time (AM by time); day 1's session is AM and day 2's is PM,
        // so finding the judiciary in PM on day 2 proves the searched day's session id is used
        final String day1 = "2025-06-10";
        final String day2 = "2025-06-11";
        final String courtScheduleIdDay1 = UUID.randomUUID().toString();
        final String courtScheduleIdDay2 = UUID.randomUUID().toString();
        final ZonedDateTime startDay1 = ZonedDateTime.parse(day1 + "T09:00:00Z");
        final ZonedDateTime startDay2 = ZonedDateTime.parse(day2 + "T09:00:00Z");

        final PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        final PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiUnallocated();
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);

        final Map<String, ZonedDateTime> startTimeByCourtScheduleId = new LinkedHashMap<>();
        startTimeByCourtScheduleId.put(courtScheduleIdDay1, startDay1);
        startTimeByCourtScheduleId.put(courtScheduleIdDay2, startDay2);
        stubListHearingInCourtSessionsForCourtSchedules(hearingValues.hearingId, startTimeByCourtScheduleId);

        final Map<String, String> customValues = new HashMap<>();
        customValues.put("%%DAY_1%%", day1);
        customValues.put("%%DAY_2%%", day2);
        customValues.put("%%START_TIME_DAY_1%%", startDay1.toInstant().toString());
        customValues.put("%%START_TIME_DAY_2%%", startDay2.toInstant().toString());
        customValues.put("%%COURT_SCHEDULE_ID_DAY_1%%", courtScheduleIdDay1);
        customValues.put("%%COURT_SCHEDULE_ID_DAY_2%%", courtScheduleIdDay2);
        allocate(hearingValues, "update-hearing-for-listing-multiday-court-schedules", customValues);

        stubJudiciarySearchCourtSchedulesById(Map.of(courtScheduleIdDay1, "AM", courtScheduleIdDay2, "PM"));

        final PayloadBasedUpdateHearingSteps searchSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);
        searchSteps.verifyJudiciarySearchInCourtSession(hearingValues.courtCentreId, hearingValues.courtRoomId, day2,
                "pmCourtSession", "amCourtSession", UNALLOCATED_TO_ALLOCATED_JUDICIAL_IDS);
        searchSteps.verifyJudiciarySearchInCourtSession(hearingValues.courtCentreId, hearingValues.courtRoomId, day1,
                "amCourtSession", "pmCourtSession", UNALLOCATED_TO_ALLOCATED_JUDICIAL_IDS);
    }

    @Test
    void shouldSearchJudiciaryForCrownCourtRoom() {
        // TS-2: a Crown court room returns its judiciary, placed by the 10:00 UK start time in AM
        final PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        final PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithScenario(
                "list-court-hearing", "spi_allocated", new HashMap<>(Map.of("%%JURISDICTION_TYPE%%", "CROWN")));
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);

        final PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);
        // keep the hearing in its listed room: the update template otherwise picks a different random room
        final PayloadGenerator.PayloadValues updatedValues = updateHearingSteps.whenUpdateHearingSubmittedWithScenario(
                "update-hearing-for-listing", "update-hearing-for-listing-assign-judiciary",
                new HashMap<>(Map.of("MAGISTRATES", "CROWN", "%%COURT_ROOM_ID%%", hearingValues.courtRoomId)));
        updateHearingSteps.verifyJudiciaryAssignedToHearing();
        updateHearingSteps.verifyJudiciarySearch(updatedValues);
    }

    private PayloadGenerator.PayloadValues givenMagistratesHearingAllocatedWithCourtScheduleAndJudiciary() {
        return givenMagistratesHearingAllocated("update-hearing-for-listing-unallocated-to-allocated", Map.of());
    }

    private PayloadGenerator.PayloadValues givenMagistratesHearingAllocated(final String updateTestCase, final Map<String, String> replacements) {
        final PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        final PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiUnallocated();
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);
        allocate(hearingValues, updateTestCase, new HashMap<>(replacements));
        return hearingValues;
    }

    private void allocate(final PayloadGenerator.PayloadValues hearingValues, final String updateTestCase, final Map<String, String> customValues) {
        // allocate into a known room with the template's three magistrates, keeping the courtScheduleId the
        // listing steps stubbed in Court Scheduler unless the template supplies its own; an unallocated
        // hearing has no room yet, so pick one the reference data stubs know
        hearingValues.courtRoomId = getRandomCourtRoomId().toString();
        final PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId, hearingValues);
        customValues.put("%%COURT_ROOM_ID%%", hearingValues.courtRoomId);
        updateHearingSteps.whenUpdateHearingSubmittedWithScenario("update-hearing-for-listing", updateTestCase, customValues);
        updateHearingSteps.verifyHearingUpdatedFromAPI(AbstractIT.ALLOCATED);
    }

    @Test
    void shouldCreateNextHearingForMagistratesJurisdiction() {
        // First, create an initial hearing using MCC without court schedule allocated scenario

        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues firstHearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithMccWithoutCourtScheduleAllocated();

        // Verify the first hearing was listed
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);

        // Now create next hearings for magistrates using adjournment mags scenario
        PayloadBasedListNextHearingSteps listNextHearingSteps = new PayloadBasedListNextHearingSteps(firstHearingValues.hearingId,firstHearingValues);

        stubListHearingInCourtSessions(firstHearingValues.hearingId,
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ItClock.nowUtc()
                        .withHour(9)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        final ZonedDateTime hearingStartTime = listNextHearingSteps.getPayloadValues().hearingStartTime;
        final LocalDate hearingDate = LocalDate.parse(listNextHearingSteps.getPayloadValues().hearingDate);
        final String courtCentreId = listNextHearingSteps.getPayloadValues().courtCentreId;
        final String courtroomId = listNextHearingSteps.getPayloadValues().courtRoomId;

        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final String bookingId = "20576fdd-4415-4bac-8948-07aa3b8d9b08";

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId);
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId);
        stubParams.put("BOOKING_ID", bookingId);
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        PayloadGenerator.PayloadValues nextHearingValues = listNextHearingSteps.whenListNextHearingSubmittedWithAdjournmentMagistrates();

        // Verify the next hearing was listed
        listNextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.ALLOCATED,2);

        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully with magistrates hearing: {} and next hearing: {}",
                   firstHearingValues.hearingId, nextHearingValues.hearingId);
    }

    @Test

    void shouldCreateUnscheduledNextHearing() {
        // First, create an initial hearing using SPI two defendants unallocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues firstHearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiTwoDefendantsUnallocated();

        // Verify the first hearing was listed
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.UNALLOCATED);

        // Now create unscheduled next hearings using adjournment crown unscheduled scenario
        PayloadBasedListNextHearingSteps listNextHearingSteps = new PayloadBasedListNextHearingSteps(firstHearingValues.hearingId,firstHearingValues);
        PayloadGenerator.PayloadValues nextHearingValues = listNextHearingSteps.whenListNextHearingSubmittedWithAdjournmentCrownUnscheduled();

        // Verify the unscheduled next hearing was listed
        listNextHearingSteps.verifyNextHearingListedFromAPI(AbstractIT.UNALLOCATED,1);
        final Matcher<? super ReadContext> oneHearingPresentMatcher = withJsonPath("hearings", hasSize(1));
        pollForUnscheduledHearings(getLoggedInUser(), UUID.fromString(nextHearingValues.courtCentreId), oneHearingPresentMatcher);

        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully with unscheduled next hearing for initial hearing: {}",
                   firstHearingValues.hearingId);
    }

    @Test
    void shouldUpdateHearingRoomAllocation() {
        // First, create an allocated hearing using SPI allocated scenario
        PayloadBasedListCourtHearingSteps listCourtHearingSteps = new PayloadBasedListCourtHearingSteps();
        PayloadGenerator.PayloadValues hearingValues = listCourtHearingSteps.whenListCourtHearingSubmittedWithSpiAllocated();

        // Verify the hearing was listed as allocated
        listCourtHearingSteps.verifyHearingListedFromAPI(AbstractIT.ALLOCATED);

        // Now update the room allocation
        PayloadBasedUpdateHearingSteps updateHearingSteps = new PayloadBasedUpdateHearingSteps(hearingValues.hearingId,hearingValues);
        PayloadGenerator.PayloadValues updatedValues = updateHearingSteps.whenUpdateHearingSubmittedWithAllocatedRoomUpdate();

        // Verify the hearing room was updated
        updateHearingSteps.verifyHearingUpdatedFromAPI(AbstractIT.ALLOCATED);
        updateHearingSteps.verifyCourtRoomUpdatedForHearing(hearingValues.courtRoomId);

        PayloadBasedListNextHearingIT.LOGGER.info("Test completed successfully - room allocation updated for hearing {}",
                   hearingValues.hearingId);
    }
}