package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataStandaloneApplication;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyDeleteAvailableHearingSlotsStubCommandInvoked;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyDeleteAvailableHearingSlotsStubCommandIsNeverInvoked;

import uk.gov.moj.cpp.listing.steps.EjectCaseApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.Test;

class EjectCaseOrApplicationIT extends AbstractIT {

    @Test
    void shouldEjectCaseFollowingPublicCaseEjectedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData);
        ejectCaseApplicationSteps.verifyListedCasesInHearings(false, 2);
        ejectCaseApplicationSteps.buildEjectCaseData();
        ejectCaseApplicationSteps.verifyNoHearingsReturned(false);

        verifyDeleteAvailableHearingSlotsStubCommandIsNeverInvoked(hearingsData.getHearingData().get(0).getId().toString());
    }


    @Test
    void shouldEjectCaseFollowingPublicApplicationEjectedEventFromProgression() {
        HearingsData hearingsData = hearingsDataStandaloneApplication();
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
        listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);

        final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData);
        ejectCaseApplicationSteps.verifyCourtApplicationInHearings(false, 1);
        ejectCaseApplicationSteps.buildEjectApplicationData();
        ejectCaseApplicationSteps.verifyNoHearingsReturned(false);

        verifyDeleteAvailableHearingSlotsStubCommandInvoked(hearingsData.getHearingData().get(0).getId().toString());
    }


    @Test
    void shouldNotFailEjectCaseFollowingPublicApplicationEjectedEventFromProgression() {
        HearingsData hearingsData = hearingsDataStandaloneApplication();
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
        listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);

        final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData);
        ejectCaseApplicationSteps.verifyCourtApplicationInHearings(false, 1);
        ejectCaseApplicationSteps.buildEjectApplicationDataWithRandomHearingID();
        ejectCaseApplicationSteps.verifyNoHearingsReturned(false);

        verifyDeleteAvailableHearingSlotsStubCommandInvoked(hearingsData.getHearingData().get(0).getId().toString());
    }
}
