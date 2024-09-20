package uk.gov.moj.cpp.listing.steps;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataForWeekCommencing;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithWeekCommencingDate;

import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matcher;

public class ListCourtHearingStepsWithWeekCommencing {

    private final static LocalDate DEFAULT_START_DATE = now();
    private final static LocalDate DEFAULT_END_DATE = now().plusDays(1L);

    private final static String WEEK_COMMENCING_START_DATE = now().toString();

    public static List<HearingsData> loadFixedHearingData() {
        final UUID firstFixedHearingId = randomUUID();
        final UUID secondFixedHearingId = randomUUID();
        final UUID thirdFixedHearingId = randomUUID();
        final UUID fourthFixedHearingId = randomUUID();
        final UUID fifthFixedHearingId = randomUUID();
        final UUID sixthFixedHearingId = randomUUID();
        final UUID seventhFixedHearingId = randomUUID();

        final UUID firstCourtRoomId = randomUUID();
        final UUID secondCourtRoomId = randomUUID();
        final UUID thirdCourtRoomId = randomUUID();

        final LocalDate firstFixedHearingStartDate = now().plusDays(1);
        final LocalDate secondFixedHearingStartDate = now();
        final LocalDate thirdFixedHearingStartDate = now();
        final LocalDate fourthFixedHearingStartDate = now().plusDays(4L);

        final LocalDate firstFixedHearingEndDate = now().plusDays(2);
        final LocalDate secondFixedHearingEndDate = now().plusDays(1);
        final LocalDate thirdFixedHearingEndDate = now().plusDays(4L);
        final LocalDate fourthFixedHearingEndDate = now().plusDays(5L);

        final HearingsData hearingsData1 = hearingsDataForWeekCommencing(firstFixedHearingId, firstFixedHearingEndDate, firstCourtRoomId, null, null, firstFixedHearingStartDate);
        final HearingsData hearingsData2 = hearingsDataForWeekCommencing(secondFixedHearingId, secondFixedHearingEndDate, secondCourtRoomId, null, null, secondFixedHearingStartDate);
        final HearingsData hearingsData3 = hearingsDataForWeekCommencing(thirdFixedHearingId, thirdFixedHearingEndDate, thirdCourtRoomId, null, null, thirdFixedHearingStartDate);
        final HearingsData hearingsData4 = hearingsDataForWeekCommencing(fourthFixedHearingId, fourthFixedHearingEndDate, firstCourtRoomId, null, null, fourthFixedHearingStartDate);
        final HearingsData hearingsData5 = hearingsDataForWeekCommencing(seventhFixedHearingId, now(), null, null, null, now());
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
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingData);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingUpdatedWhenQueryingFromAPI();
        updateHearingSteps.verifyPublicHearingChangesSaved();
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

        weekCommencingHearingSteps.verifyHearingUpdatedResultsForWeekCommencingInMQ();
        weekCommencingHearingSteps.verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI();
    }
}
