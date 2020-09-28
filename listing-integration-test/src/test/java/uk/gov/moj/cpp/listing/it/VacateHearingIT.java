package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListUnscheduledCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class VacateHearingIT extends AbstractIT {

    @DataProvider
    public static Object[][] provideJurisdictionTypes() {
        return new Object[][]{
                {CROWN_JURISDICTION},
                {MAGISTRATES_JURISDICTION}
        };
    }

    @Test
    public void shouldVacateMagistrateCourtHearingAndHearingSlotsFreed_WhenInitiatedFromHearing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);

        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();

        vacatingTrialSteps.verifyHearingTrialVacatedEvent();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        vacatingTrialSteps.verifyAvailableSlotsForHearingFreedEvent();
    }

    @Test
    public void shouldVacateMagistrateCourtHearingAndHearingSlotsFreed_WhenInitiatedFromListing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);

        vacatingTrialSteps.whenHearingIsVacated();

        vacatingTrialSteps.verifyListingTrialVacatedEvent(true);
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        vacatingTrialSteps.verifyAvailableSlotsForHearingFreedEvent();
    }

    @Test
    public void shouldNotVacateMagistrateCourtHearingAndNotAttemptToFreeHearingSlotsWhenReasonIdIsEmpty() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);

        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublishedWithEmptyVacatedTrialReasonId();

        vacatingTrialSteps.verifyHearingVacatingTrialEventForEmptyReasonId();
        vacatingTrialSteps.verifyVacatedTrialWithEmptyReasonIdWhenQueryingFromAPI();
        vacatingTrialSteps.verifyAvailableSlotsForHearingFreedEventNotRaised();
    }

    @Test
    public void shouldVacateCrownCourtHearingAndNotAttemptToFreeHearingSlots() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);

        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();

        vacatingTrialSteps.verifyHearingTrialVacatedEvent();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        vacatingTrialSteps.verifyAvailableSlotsForHearingFreedEventNotRaised();
    }

    @Test
    @UseDataProvider("provideJurisdictionTypes")
    public void shouldVacateUnallocatedHearingAndNotAttemptToFreeHearingSlots(final String jurisdictionType) {
        final HearingsData hearingsData = hearingsData(jurisdictionType);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);

        vacatingTrialSteps.whenHearingIsVacated();

        vacatingTrialSteps.verifyListingTrialVacatedEvent(false);
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        vacatingTrialSteps.verifyAvailableSlotsForHearingFreedEventNotRaised();
    }

    @Test
    @UseDataProvider("provideJurisdictionTypes")
    public void shouldVacateUnscheduledHearingAndNotAttemptToFreeHearingSlots(final String jurisdictionType) {
        final HearingsData hearingsData = hearingsData(jurisdictionType);
        try (final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();
        }

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacated();

        vacatingTrialSteps.verifyListingTrialVacatedEvent(false);
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        vacatingTrialSteps.verifyAvailableSlotsForHearingFreedEventNotRaised();
    }

}
