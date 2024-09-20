package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;

import uk.gov.moj.cpp.listing.steps.AddDefendantSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DefendantsAddedIT extends AbstractIT {

    @Test
    public void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsUnallocated() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.whenCaseDefendantsAddedPublicEventIsPublished();
        addDefendantSteps.verifyEventDefendantAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantsToBeAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantDetailsAddedInActiveMQ();
        addDefendantSteps.verifyPublicEventDefendantAddedNotRaisedInActiveMQ();
        addDefendantSteps.verifyHearingListedFromAPI(false);
    }

    @Test
    public void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsUnallocatedHmiEnabled() {
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.whenCaseDefendantsAddedPublicEventIsPublished();
        addDefendantSteps.verifyEventDefendantAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantsToBeAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantDetailsAddedInActiveMQ();
        addDefendantSteps.verifyHearingListedFromAPI(true);
    }


    @Test
    public void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsAllocated() {
        HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.whenCaseDefendantsAddedPublicEventIsPublished();
        addDefendantSteps.verifyEventDefendantAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantsToBeAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantDetailsAddedInActiveMQ();
        addDefendantSteps.verifyPublicEventDefendantAddedInActiveMQ();
    }

    @Test
    public void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgressionAndHearingIsAllocatedHmiEnabled() {
        HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData);
        addDefendantSteps.whenCaseDefendantsAddedPublicEventIsPublished();
        addDefendantSteps.verifyEventDefendantAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantsToBeAddedInActiveMQ();
        addDefendantSteps.verifyEventDefendantDetailsAddedInActiveMQ();
        addDefendantSteps.verifyPublicEventDefendantAddedInActiveMQ();
    }
}
