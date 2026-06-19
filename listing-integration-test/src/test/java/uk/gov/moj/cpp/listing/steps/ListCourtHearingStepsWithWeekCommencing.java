package uk.gov.moj.cpp.listing.steps;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataForWeekCommencing;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithWeekCommencingDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetCourtSchedulesByIdWithDraftStatus;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;

import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matcher;

public class ListCourtHearingStepsWithWeekCommencing {

    private final static LocalDate DEFAULT_START_DATE = ItClock.today();
    private final static LocalDate DEFAULT_END_DATE = ItClock.today().plusDays(1L);

    private final static String WEEK_COMMENCING_START_DATE = ItClock.today().toString();

    public static List<HearingsData> loadFixedHearingData() {
        final UUID firstFixedHearingId = randomUUID();
        final UUID secondFixedHearingId = randomUUID();
        final UUID thirdFixedHearingId = randomUUID();
        final UUID fourthFixedHearingId = randomUUID();
        final UUID fifthFixedHearingId = randomUUID();
        final UUID sixthFixedHearingId = randomUUID();
        final UUID seventhFixedHearingId = randomUUID();

        final UUID firstCourtRoomId = getRandomCourtRoomId();
        final UUID secondCourtRoomId = getRandomCourtRoomId(asList(firstCourtRoomId));
        final UUID thirdCourtRoomId = getRandomCourtRoomId(asList(firstCourtRoomId, secondCourtRoomId));

        final LocalDate firstFixedHearingStartDate = ItClock.today().plusDays(1);
        final LocalDate secondFixedHearingStartDate = ItClock.today();
        final LocalDate thirdFixedHearingStartDate = ItClock.today();
        final LocalDate fourthFixedHearingStartDate = ItClock.today().plusDays(4L);

        final LocalDate firstFixedHearingEndDate = ItClock.today().plusDays(2);
        final LocalDate secondFixedHearingEndDate = ItClock.today().plusDays(1);
        final LocalDate thirdFixedHearingEndDate = ItClock.today().plusDays(4L);
        final LocalDate fourthFixedHearingEndDate = ItClock.today().plusDays(5L);

        final HearingsData hearingsData1 = hearingsDataForWeekCommencing(firstFixedHearingId, firstFixedHearingEndDate, firstCourtRoomId, null, null, firstFixedHearingStartDate);
        final HearingsData hearingsData2 = hearingsDataForWeekCommencing(secondFixedHearingId, secondFixedHearingEndDate, secondCourtRoomId, null, null, secondFixedHearingStartDate);
        final HearingsData hearingsData3 = hearingsDataForWeekCommencing(thirdFixedHearingId, thirdFixedHearingEndDate, thirdCourtRoomId, null, null, thirdFixedHearingStartDate);
        final HearingsData hearingsData4 = hearingsDataForWeekCommencing(fourthFixedHearingId, fourthFixedHearingEndDate, firstCourtRoomId, null, null, fourthFixedHearingStartDate);
        final HearingsData hearingsData5 = hearingsDataForWeekCommencing(seventhFixedHearingId, ItClock.today(), null, null, null, ItClock.today());
        final HearingsData hearingsData6 = hearingsDataForWeekCommencing(fifthFixedHearingId, DEFAULT_END_DATE, secondCourtRoomId, null, null, DEFAULT_START_DATE);
        final HearingsData hearingsData7 = hearingsDataForWeekCommencing(sixthFixedHearingId, DEFAULT_END_DATE, thirdCourtRoomId, null, null, DEFAULT_START_DATE);

        final List<HearingsData> hearingsDataList = asList(hearingsData1, hearingsData2, hearingsData3, hearingsData4, hearingsData5, hearingsData6, hearingsData7);

        hearingsDataList.forEach(ListCourtHearingStepsWithWeekCommencing::createHearingListed);

        return hearingsDataList;
    }

    public static UpdatedHearingData updateLoadedFixedHearingToWeekCommencingHearing(final HearingsData hearingsData, final String weekCommencingEndDate, final int weekCommencingDuration) {
        //update selected fixed hearings to week commencing hearings
        final UpdatedHearingData updatedHearingDataWithWeekCommencingDate = updatedHearingDataWithWeekCommencingDate(hearingsData.getHearingData().get(0), WEEK_COMMENCING_START_DATE, weekCommencingEndDate, weekCommencingDuration);
        createWeekCommencingHearingForWeekCommencingSearch(singletonList(updatedHearingDataWithWeekCommencingDate));

        return updatedHearingDataWithWeekCommencingDate;
    }

    public static void verifyHearingListedForWeekCommencing(final String jurisdictionType, final String weekCommencingStartDate, final String weekCommencingEndDate, final boolean allocated, final Matcher... matchers) {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps();
        listCourtHearingSteps.verifyHearingForWeekCommencingRange(jurisdictionType, weekCommencingStartDate, weekCommencingEndDate, allocated, matchers);
    }

    public static UpdatedHearingData updatedHearingListedData(final HearingsData hearingsData) {
        UpdatedHearingData updatedHearingData = updatedHearingData(hearingsData.getHearingData().get(0));
        // CROWN updates carry a courtScheduleId on their nonDefaultDay; the single-day update
        // enrichment re-fetches it via search.court-schedules-by-id. Without these stubs the
        // catch-all answers with an alien shape -> parses empty -> WARN "CROWN single-day
        // update: failed to fetch court schedules ... Returning unchanged".
        updatedHearingData.getNonDefaultDays().get(0).getCourtScheduleId().ifPresent(courtScheduleId -> {
            final LocalDate updatedStartDate = LocalDate.parse(updatedHearingData.getStartDate());
            // Both stubs MUST carry the UPDATED start date/time: the enrichment rebuilds the
            // hearing day from these session responses, so stale (seed-time) values would
            // silently revert the very date this update is asserting on.
            final java.time.ZonedDateTime updatedStartTime = updatedStartDate.atTime(10, 0).atZone(ZoneOffset.UTC);
            stubGetCourtSchedulesByIdWithDraftStatus(singletonList(courtScheduleId), false,
                    updatedStartDate,
                    updatedHearingData.getCourtCentreId(),
                    updatedHearingData.getCourtRoomId(),
                    updatedStartTime);
            stubListHearingInCourtSessions(hearingsData.getHearingData().get(0).getId().toString(),
                    courtScheduleId,
                    updatedStartTime);
        });
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
        return updatedHearingData;
    }

    private static void createHearingListed(final HearingsData hearingsData) {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
    }

    private static void createWeekCommencingHearingForWeekCommencingSearch(final List<UpdatedHearingData> updatedHearingsDataList) {
        updatedHearingsDataList.forEach(ListCourtHearingStepsWithWeekCommencing::createUpdatedHearingListedWithWeekCommencing);
    }

    private static void createUpdatedHearingListedWithWeekCommencing(final UpdatedHearingData updatedHearingDataWithWeekCommencingDate) {

        final WeekCommencingHearingSteps weekCommencingHearingSteps = new WeekCommencingHearingSteps(updatedHearingDataWithWeekCommencingDate);
        weekCommencingHearingSteps.whenHearingIsUpdatedForListingForWeekCommencingDate();

        weekCommencingHearingSteps.verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI();
    }
}
