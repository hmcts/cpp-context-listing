package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.List;

import org.junit.jupiter.api.Test;


public class AddCourtApplicationIT extends AbstractIT {


    @Test
    public void shouldAddCourtApplicationForHearingId() {

        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(false);
        listCourtHearingSteps.verifyPublicCourtApplicationAdded();

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(false);
    }

    @Test
    public void shouldAddCourtApplicationForHearingIdHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(true);
        listCourtHearingSteps.verifyPublicCourtApplicationAdded();



        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyCourtApplicationAddedInActiveMQ();
        courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(true);
    }

    @Test
    public void shouldAddCourtApplicationAndCaseForHearingIdHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(true);
        listCourtHearingSteps.verifyPublicCourtApplicationAdded();

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.verifyCaseCountFromAPI(true, 2);
        courtApplicationSteps.whenCaseCourtApplicationAndLinkedCaseAreAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyCourtApplicationAddedInActiveMQ();
        courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(true);
        courtApplicationSteps.verifyAddedCaseForHearingInActiveMQ();
        courtApplicationSteps.verifyCaseCountFromAPI(true, 3);
    }

    @Test
    public void shouldAddCourtApplicationForUnAllocatedHearingIdHmiEnabled() {

        final HearingsData hearingsData = HearingsData.hearingsData();
        final List<CourtApplicationData> applications = hearingsData.getHearingData().get(0).getCourtApplications();
        hearingsData.getHearingData().get(0).setCourtApplications(null);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyPublicCourtApplicationAdded();

        hearingsData.getHearingData().get(0).setCourtApplications(applications);
        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyCourtApplicationAddedInPrivateMessage();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(false);
        courtApplicationSteps.verifyTimeLine(hearingsData.getHearingData().get(0));
    }
}
