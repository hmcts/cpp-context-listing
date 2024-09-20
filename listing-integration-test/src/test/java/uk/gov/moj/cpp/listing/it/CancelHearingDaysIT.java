package uk.gov.moj.cpp.listing.it;

import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ALPHABETICAL;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.STANDARD;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingDayFactory.buildHearingDaysWithCancelledFlag;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased;

import uk.gov.moj.cpp.listing.steps.CancelHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingDay;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;


public class CancelHearingDaysIT extends AbstractIT {

    private static final boolean ALLOCATED = true;

    @Test
    public void shouldCancelHearingDaysAndFreeMagistrateCourtSlotsForCancelledDays() {
        final List<HearingDay> hearingDays = buildHearingDaysWithCancelledFlag(null, null, true);
        final HearingsData hearingsData = givenMultidayAllocatedHearingExists(hearingDays, MAGISTRATES_JURISDICTION);

        final CancelHearingSteps cancelHearingSteps = new CancelHearingSteps(hearingsData, hearingDays);
        cancelHearingSteps.whenPublicEventHearingDaysCancelledIsPublished();

        cancelHearingSteps.verifyAllocatedHearingFoundOnNonCancelledHearingDay(hearingDays.get(0).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingFoundOnNonCancelledHearingDay(hearingDays.get(1).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingNotFoundOnCancelledHearingDay(hearingDays.get(2).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyHearingSlotsUpdatedToRetainNonCancelledDays();
    }

    @Test
    public void shouldCancelHearingDaysAndNotFreeAnyCourtSlotsForCrownHearing() {
        final List<HearingDay> hearingDays = buildHearingDaysWithCancelledFlag(null, true, true);
        final HearingsData hearingsData = givenMultidayAllocatedHearingExists(hearingDays, CROWN_JURISDICTION);

        final CancelHearingSteps cancelHearingSteps = new CancelHearingSteps(hearingsData, hearingDays);
        cancelHearingSteps.whenPublicEventHearingDaysCancelledIsPublished();

        cancelHearingSteps.verifyAllocatedHearingFoundOnNonCancelledHearingDay(hearingDays.get(0).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingNotFoundOnCancelledHearingDay(hearingDays.get(1).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingNotFoundOnCancelledHearingDay(hearingDays.get(2).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyHearingSlotsWereNotUpdated();
    }

    @Test
    public void shouldNotRetrieveCancelledDaysWhenCourtListSearchInvokedForAlphabeticListingType() {
        final List<HearingDay> hearingDays = buildHearingDaysWithCancelledFlag(null, false, true);
        final HearingsData hearingsData = givenMultidayAllocatedHearingExists(hearingDays, CROWN_JURISDICTION);

        final CancelHearingSteps cancelHearingSteps = new CancelHearingSteps(hearingsData, hearingDays);
        cancelHearingSteps.whenPublicEventHearingDaysCancelledIsPublished();

        cancelHearingSteps.verifyCourtListHearingFoundWithoutCancelledHearingDay(ALPHABETICAL, hearingDays.get(0).getSittingDay().toLocalDate(), hearingDays.get(0).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyCourtListHearingFoundWithCancelledFalseHearingDay(ALPHABETICAL, hearingDays.get(1).getSittingDay().toLocalDate(), hearingDays.get(1).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyCourtListHearingNotFound(ALPHABETICAL, hearingDays.get(2).getSittingDay().toLocalDate(), hearingDays.get(2).getSittingDay().toLocalDate());
    }

    @Test
    public void shouldNotRetrieveCancelledDaysWhenCourtListSearchInvokedForPublicOrStandardListingType() {
        final List<HearingDay> hearingDays = buildHearingDaysWithCancelledFlag(null, false, true);
        final HearingsData hearingsData = givenMultidayAllocatedHearingExists(hearingDays, CROWN_JURISDICTION);

        final CancelHearingSteps cancelHearingSteps = new CancelHearingSteps(hearingsData, hearingDays);
        cancelHearingSteps.whenPublicEventHearingDaysCancelledIsPublished();

        cancelHearingSteps.verifyCourtListHearingFoundWithoutCancelledHearingDay(PUBLIC, hearingDays.get(0).getSittingDay().toLocalDate(), hearingDays.get(0).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyCourtListHearingFoundWithCancelledFalseHearingDay(STANDARD, hearingDays.get(1).getSittingDay().toLocalDate(), hearingDays.get(1).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyCourtListHearingNotFound(STANDARD, hearingDays.get(2).getSittingDay().toLocalDate(), hearingDays.get(2).getSittingDay().toLocalDate());
    }

    @Test
    public void shouldCancelHearingDaysThenFreeMagistrateCourtSlotsAndNotRetrieveCancelledDaysOnCourtLists() {
        final List<HearingDay> hearingDays = buildHearingDaysWithCancelledFlag(false, true, true);
        final HearingsData hearingsData = givenMultidayAllocatedHearingExists(hearingDays, MAGISTRATES_JURISDICTION);

        final CancelHearingSteps cancelHearingSteps = new CancelHearingSteps(hearingsData, hearingDays);
        cancelHearingSteps.whenPublicEventHearingDaysCancelledIsPublished();

        cancelHearingSteps.verifyAllocatedHearingFoundWithCancelledDaysRemovedOnCourtLists();
        cancelHearingSteps.verifyAllocatedHearingFoundWhenSearchDateWithinStartAndEndDateRangeForCourtLists(hearingDays.get(0).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingFoundWhenSearchDateWithinStartAndEndDateRangeForCourtLists(hearingDays.get(1).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingFoundWhenSearchDateWithinStartAndEndDateRangeForCourtLists(hearingDays.get(2).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyHearingSlotsUpdatedToRetainNonCancelledDays();
    }

    @Test
    public void shouldCancelHearingDaysThenFreeMagistrateCourtSlotsAndNotRetrieveCancelledDaysOnCourtListsOnWeekCommencingRange() {
        final List<HearingDay> hearingDays = buildHearingDaysWithCancelledFlag(false, true, true);
        final HearingsData hearingsData = givenMultidayAllocatedHearingExists(hearingDays, MAGISTRATES_JURISDICTION);

        final CancelHearingSteps cancelHearingSteps = new CancelHearingSteps(hearingsData, hearingDays);
        cancelHearingSteps.whenPublicEventHearingDaysCancelledIsPublished();

        cancelHearingSteps.verifyAllocatedHearingFoundWithCancelledDaysRemovedOnCourtListsOnWeekCommencingRange();
        cancelHearingSteps.verifyAllocatedHearingFoundWhenSearchDateWithinWeekCommencingRangeForCourtLists(hearingDays.get(0).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingFoundWhenSearchDateWithinWeekCommencingRangeForCourtLists(hearingDays.get(1).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyAllocatedHearingFoundWhenSearchDateWithinWeekCommencingRangeForCourtLists(hearingDays.get(2).getSittingDay().toLocalDate());
        cancelHearingSteps.verifyHearingSlotsUpdatedToRetainNonCancelledDays();
    }

    @Test
    public void shouldNotImpactCancelledHearingDaysWhenHearingDaysAreSequenced() {
        final List<HearingDay> hearingDays = buildHearingDaysWithCancelledFlag(false, false, true);
        final HearingsData hearingsData = givenMultidayAllocatedHearingExists(hearingDays, MAGISTRATES_JURISDICTION);

        final CancelHearingSteps cancelHearingSteps = new CancelHearingSteps(hearingsData, hearingDays);
        cancelHearingSteps.whenPublicEventHearingDaysCancelledIsPublished();

        cancelHearingSteps.verifyAllocatedHearingFoundWithCancelledDaysRemovedOnCourtLists();
        cancelHearingSteps.whenHearingDaysAreSequenced();
        cancelHearingSteps.verifyAllocatedHearingFoundWithCancelledDaysRemovedOnCourtListsWithUpdatedSequence();
        cancelHearingSteps.verifyHearingSlotsUpdatedToRetainNonCancelledDays();
    }

    private HearingsData givenMultidayAllocatedHearingExists(final List<HearingDay> hearingDays, final String jurisdiction) {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(jurisdiction);
        final List<HearingData> hearingData = hearingsData.getHearingData();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>(){{
            put(to(hearingDays.get(0).getSittingDay().toLocalDate()), hearingData.get(0).getCourtRoomId().toString());
            put(to(hearingDays.get(1).getSittingDay().toLocalDate()), hearingData.get(0).getCourtRoomId().toString());
            put(to(hearingDays.get(2).getSittingDay().toLocalDate()), hearingData.get(0).getCourtRoomId().toString());
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, hearingData.get(0).getCourtCentreId().toString());
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().stream().toArray(String[]::new), courtRoomSchedules.values().stream().toArray(String[]::new));
        return hearingsData;
    }

}
