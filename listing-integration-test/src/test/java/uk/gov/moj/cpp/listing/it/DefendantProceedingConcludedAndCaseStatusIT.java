package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.CaseUpdatedAndDefendantProceedingsConcludedSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.Test;

public class DefendantProceedingConcludedAndCaseStatusIT extends AbstractIT {
    @Test
    public void shouldUpdateDefendantProceedingConcludedAndCaseStatusEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);

        try (final CaseUpdatedAndDefendantProceedingsConcludedSteps caseUpdatedAndDefendantProceedingsConcludedSteps = new CaseUpdatedAndDefendantProceedingsConcludedSteps(caseId, hearingData)) {
            caseUpdatedAndDefendantProceedingsConcludedSteps.whenPublicEventCaseUpdatedAndHearingResultedIsPublished();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventCaseResultedDefendantProceedingsUpdatedInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventDefendantCourtProceedingsUpdatedInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPI(UNALLOCATED);
        }

    }
}