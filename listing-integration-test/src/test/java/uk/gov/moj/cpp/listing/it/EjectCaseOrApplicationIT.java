package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataStandaloneApplication;

import uk.gov.moj.cpp.listing.steps.EjectCaseApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.jupiter.api.Test;

public class EjectCaseOrApplicationIT extends AbstractIT {

    @Test
    public void shouldEjectCaseFollowingPublicCaseEjectedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData);
        ejectCaseApplicationSteps.verifyListedCasesInHearings(false, 2);
        ejectCaseApplicationSteps.buildEjectCaseData();
        ejectCaseApplicationSteps.verifyNoHearingsReturned(false);
    }


    @Test
    public void shouldEjectCaseFollowingPublicApplicationEjectedEventFromProgression() {
        HearingsData hearingsData = hearingsDataStandaloneApplication();
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
        listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);

        final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData);
        ejectCaseApplicationSteps.verifyCourtApplicationInHearings(false, 1);
        ejectCaseApplicationSteps.buildEjectApplicationData();
        ejectCaseApplicationSteps.verifyNoHearingsReturned(false);
    }


    @Test
    public void shouldNotFailEjectCaseFollowingPublicApplicationEjectedEventFromProgression() {
        HearingsData hearingsData = hearingsDataStandaloneApplication();
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
        listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);

        final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData);
        ejectCaseApplicationSteps.verifyCourtApplicationInHearings(false, 1);
        ejectCaseApplicationSteps.buildEjectApplicationDataWithRandomHearingID();
        ejectCaseApplicationSteps.verifyNoHearingsReturned(false);
    }
}
