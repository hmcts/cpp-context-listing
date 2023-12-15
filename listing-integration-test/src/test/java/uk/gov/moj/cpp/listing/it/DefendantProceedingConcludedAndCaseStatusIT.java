package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.listing.steps.CaseUpdatedAndDefendantProceedingsConcludedSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;

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
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventUpdatedHearingInStagingHmiNotInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPI(UNALLOCATED);
        }

    }

    @Test
    public void shouldUpdateDefendantProceedingConcludedAndCaseStatusEventFromProgressionWhenAllocated() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(true);
            listCourtHearingSteps.verifyHearingListedInForStagingHmi();
        }

        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);

        try (final CaseUpdatedAndDefendantProceedingsConcludedSteps caseUpdatedAndDefendantProceedingsConcludedSteps = new CaseUpdatedAndDefendantProceedingsConcludedSteps(caseId, hearingData)) {
            caseUpdatedAndDefendantProceedingsConcludedSteps.whenPublicEventCaseUpdatedAndHearingResultedIsPublished();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventCaseResultedDefendantProceedingsUpdatedInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventDefendantCourtProceedingsUpdatedInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventUpdatedHearingInStagingHmiInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyHearingForCaseStatusAndDefendantProceedingsConcludedFromAPI(true);
        }

    }

    @Test
    public void shouldNotUpdateDefendantProceedingConcludedAndCaseStatusEventFromProgressionWhenIncorrectDefandantIsSupplied() {
        HearingsData hearingsData = HearingsData.singleHearingDataMultipleCasesWithSingleOffence();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        final UUID caseId = listedCaseData.getCaseId();

        // Using a different defendant ID so that we land in the situation where defendant list is empty
        ReflectionUtil.setField(listedCaseData.getDefendants().get(0), "defendantId", randomUUID());

        try (final CaseUpdatedAndDefendantProceedingsConcludedSteps caseUpdatedAndDefendantProceedingsConcludedSteps = new CaseUpdatedAndDefendantProceedingsConcludedSteps(caseId, hearingData)) {
            caseUpdatedAndDefendantProceedingsConcludedSteps.whenPublicEventCaseUpdatedAndHearingResultedIsPublished();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventCaseResultedDefendantProceedingsUpdatedInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyPrivateEventDefendantCourtProceedingsUpdatedIsNotInActiveMQ();
            caseUpdatedAndDefendantProceedingsConcludedSteps.verifyHearingForCaseStatusAndDefendantProceedingsConcludedNotSetFromAPI(UNALLOCATED);
        }

    }
}