package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData.updateOffenceData;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DefendantOffencesChangedIT extends AbstractIT {

    private static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "public.listing.case-sent-for-listing";

    static {
        publicEvents.createPublicConsumer(PUBLIC_EVENT_CASE_SENT_FOR_LISTING);
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgression() throws Exception {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UUID offenceIdToBeDeleted = defendantData.getOffences().get(1).getOffenceId();
        UpdatedOffenceData updatedOffenceData = updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, offenceIdToBeDeleted);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublished();
        steps.verifyPublicEventDefendantOffencesUpdatedInActiveMQ();
        steps.verifyEventDefendantOffencesToBeUpdateInActiveMQ();
        steps.verifyEventDefendantOffencesToBeAddedInActiveMQ();
        steps.verifyEventDefendantOffencesToBeDeletedInActiveMQ();

        // add/update/delete message causes timing issues where messages are not ready to be pulled of the queue for verification
        steps.verifyEventOffenceUpdatedInActiveMQ();
        steps.verifyEventOffenceDeletedInActiveMQ();
        steps.verifyEventOffenceAddedInActiveMQ();

    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionUpdatedOnly() {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedUpdatedOnly();
        steps.verifyDefendentOffenceUpdatedOnlyFromAPI(false);
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionAddedOnly() {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly();
        steps.verifyDefendentOffenceAddedOnlyFromAPI(false);
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionDeletedOnly() {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, updatedOffenceData.getOffenceId());
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedDeletedOnly();
        steps.verifyDefendentOffenceDeletedOnlyFromAPI(false);
    }

    private HearingsData listCourtHearing() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        return hearingsData;
    }

}
