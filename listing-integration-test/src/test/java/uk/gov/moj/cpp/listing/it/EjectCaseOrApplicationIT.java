package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.EjectCaseApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.Test;

public class EjectCaseOrApplicationIT extends AbstractIT {

    @Test
    public void shouldEjectCaseFollowingPublicCaseEjectedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData)) {
            ejectCaseApplicationSteps.verifyListedCasesInHearings(false, 2);
            ejectCaseApplicationSteps.buildEjectCaseData();
            ejectCaseApplicationSteps.verifyEventCaseEjectedInActiveMQ();
            ejectCaseApplicationSteps.verifyNoHearingsReturned(false);
        }
    }


    @Test
    public void shouldEjectCaseFollowingPublicApplicationEjectedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsDataStandaloneApplication();
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedInActiveMQForStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);
        }

        try (final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData)) {
            ejectCaseApplicationSteps.verifyCourtApplicationInHearings(false, 1);
            ejectCaseApplicationSteps.buildEjectApplicationData();
            ejectCaseApplicationSteps.verifyEventApplicationEjectedInActiveMQ();
            ejectCaseApplicationSteps.verifyNoHearingsReturned(false);
        }
    }


    @Test
    public void shouldNotFailEjectCaseFollowingPublicApplicationEjectedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsDataStandaloneApplication();
        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedInActiveMQForStandaloneApplication();
            listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);
        }

        try (final EjectCaseApplicationSteps ejectCaseApplicationSteps = new EjectCaseApplicationSteps(hearingsData)) {
            ejectCaseApplicationSteps.verifyCourtApplicationInHearings(false, 1);
            UUID hearingID = ejectCaseApplicationSteps.buildEjectApplicationDataWithRandomHearingID();
            ejectCaseApplicationSteps.verifyEventApplicationEjectedInActiveMQ(hearingID);
            ejectCaseApplicationSteps.verifyNoHearingsReturned(false);
        }
    }
}
