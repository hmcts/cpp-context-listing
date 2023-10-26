package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.List;

import org.junit.Test;


public class AddCourtApplicationIT extends AbstractIT {


    @Test
    public void shouldAddCourtApplicationForHearingId() {

        final HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(false);
        }

        try (final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData)) {
            courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
            courtApplicationSteps.verifyCourtApplicationAddedInActiveMQ();
            courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
            courtApplicationSteps.verifyCourtApplicationAddedFromAPI(false);
        }
    }

    @Test
    public void shouldAddCourtApplicationForHearingIdHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(true);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }

        try (final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData)) {
            courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
            courtApplicationSteps.verifyCourtApplicationAddedInActiveMQ();
            courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
            courtApplicationSteps.verifyCourtApplicationAddedFromAPI(true);
            courtApplicationSteps.verifyHmiPublicEventForUpdateHearing();
        }
    }

    @Test
    public void shouldAddCourtApplicationAndCaseForHearingIdHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(true);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }

        try (final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData)) {
            courtApplicationSteps.verifyCaseCountFromAPI(true, 2);
            courtApplicationSteps.whenCaseCourtApplicationAndLinkedCaseAreAddedToListingAndHearingIsExtended();
            courtApplicationSteps.verifyCourtApplicationAddedInActiveMQ();
            courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
            courtApplicationSteps.verifyCourtApplicationAddedFromAPI(true);
            courtApplicationSteps.verifyHmiPublicEventForUpdateHearing();
            courtApplicationSteps.verifyAddedCaseForHearingInActiveMQ();
            courtApplicationSteps.verifyCaseCountFromAPI(true, 3);
        }
    }

    @Test
    public void shouldAddCourtApplicationForUnAllocatedHearingIdHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsData();
        final List<CourtApplicationData> applications = hearingsData.getHearingData().get(0).getCourtApplications();
        hearingsData.getHearingData().get(0).setCourtApplications(null);

        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        hearingsData.getHearingData().get(0).setCourtApplications(applications);
        try (final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData)) {
            courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
            courtApplicationSteps.verifyCourtApplicationAddedInActiveMQ();
            courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
            courtApplicationSteps.verifyCourtApplicationAddedFromAPI(false);
            courtApplicationSteps.verifyTimeLine(hearingsData.getHearingData().get(0));
        }
    }
}
