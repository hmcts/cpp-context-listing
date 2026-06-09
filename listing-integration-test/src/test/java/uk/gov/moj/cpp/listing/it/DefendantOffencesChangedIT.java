package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData.updateOffenceData;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class DefendantOffencesChangedIT extends AbstractIT {

    private static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "public.listing.case-sent-for-listing";
    private JmsMessageConsumerClient publicConsumer;

    @BeforeEach
    void setupConsumer() {
        publicConsumer = publicEvents.createPublicConsumer(PUBLIC_EVENT_CASE_SENT_FOR_LISTING);
    }

    @AfterEach
    void teardownConsumer() {
        if (publicConsumer != null) {
            publicConsumer.clearMessages();
        }
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgression() throws Exception {
        HearingsData hearingsData = listCourtHearing();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        DefendantData defendantData = hearingData.getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingData.getListedCases().get(0).getCaseId();

        OffenceData offenceData = defendantData.getOffences().get(0);
        UUID offenceIdToBeDeleted = defendantData.getOffences().get(1).getOffenceId();
        UpdatedOffenceData updatedOffenceData = updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, offenceIdToBeDeleted);
        // Re-publishes until the public event is consumed (gate). The Case aggregate silently drops
        // the update when the case<->hearing link has not yet formed (async add-hearing-to-case race);
        // a fresh metadata id per attempt avoids framework dedup. Remaining verify calls are outside
        // the loop — they only run once the gate consume has succeeded.
        steps.publishUntilOffencesConsumed();
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
        steps.publishUntilOffencesUpdatedOnlyReflected(false);
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
        steps.publishUntilOffencesAddedOnlyReflected(false);
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
        // Re-publish until reflected — closes the coverage gap left when the other offence-change
        // variants were wrapped: the case<->hearing link race silently drops a single publish.
        steps.publishUntilOffencesDeletedOnlyReflected(false);
    }

    private HearingsData listCourtHearing() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        // Use JMS-aware verification to handle timing issues
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        return hearingsData;
    }

}
