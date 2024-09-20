package uk.gov.moj.cpp.listing.it;


import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DefendantsChangedIT extends AbstractIT {

    @Test
    public void shouldUpdateDefendantsFollowingPublicDefendantsChangedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedDefendantData updatedDefendantData = UpdatedDefendantData.updatedDefendantData(defendantData);

        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenCaseDefendantsUpdatedPublicEventIsPublished();
        updateDefendantSteps.verifyEventDefendantUpdatedInActiveMQ();
        updateDefendantSteps.verifyEventDefendantsToBeUpdateInActiveMQ();
        updateDefendantSteps.verifyEventDefendantDetailsUpdatedInActiveMQ();
        updateDefendantSteps.verifyHearingListedFromAPI(false);
    }

    @Test
    public void shouldUpdateDefendantsFollowingPublicDefendantsChangedEventFromProgressionHmiEnabled() {
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedDefendantData updatedDefendantData = UpdatedDefendantData.updatedDefendantData(defendantData);

        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenCaseDefendantsUpdatedPublicEventIsPublished();
        updateDefendantSteps.verifyEventDefendantUpdatedInActiveMQ();
        updateDefendantSteps.verifyEventDefendantsToBeUpdateInActiveMQ();
        updateDefendantSteps.verifyEventDefendantDetailsUpdatedInActiveMQ();
        updateDefendantSteps.verifyHearingListedFromAPI(true);
    }
}
