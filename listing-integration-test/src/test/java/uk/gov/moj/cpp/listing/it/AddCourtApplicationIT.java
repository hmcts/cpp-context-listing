package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;

import uk.gov.moj.cpp.listing.steps.CourtApplicationSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.List;

import org.junit.jupiter.api.Test;

public class AddCourtApplicationIT extends AbstractIT {

    @Test
    public void shouldAddCourtApplicationForHearingId() {

        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(false);
        listCourtHearingSteps.verifyPublicEventCourtApplicationAdded();

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyPublicEventCourtApplicationAdded();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(false);
    }

    @Test
    public void shouldAddCourtApplicationForHearingIdHmiEnabled() {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedFromAPI(true);
        listCourtHearingSteps.verifyPublicEventCourtApplicationAdded();

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyPublicEventCourtApplicationAdded();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(true);
    }

    @Test
    public void shouldAddCourtApplicationAndCaseForHearingIdHmiEnabled() {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedFromAPI(true);
        listCourtHearingSteps.verifyPublicEventCourtApplicationAdded();

        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.verifyCaseCountFromAPI(true, 2);
        courtApplicationSteps.whenCaseCourtApplicationAndLinkedCaseAreAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyPublicEventCourtApplicationAdded();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(true);
        courtApplicationSteps.verifyCaseCountFromAPI(true, 3);
    }

    @Test
    public void shouldAddCourtApplicationForUnAllocatedHearingIdHmiEnabled() {

        final HearingsData hearingsData = hearingsData();
        final List<CourtApplicationData> applications = hearingsData.getHearingData().get(0).getCourtApplications();
        hearingsData.getHearingData().get(0).setCourtApplications(null);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyPublicEventCourtApplicationAdded();

        hearingsData.getHearingData().get(0).setCourtApplications(applications);
        final CourtApplicationSteps courtApplicationSteps = new CourtApplicationSteps(hearingsData);
        courtApplicationSteps.whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended();
        courtApplicationSteps.verifyPublicEventCourtApplicationAdded();
        courtApplicationSteps.verifyCourtApplicationAddedFromAPI(false);
        courtApplicationSteps.verifyTimeLine(hearingsData.getHearingData().get(0));
    }
}
