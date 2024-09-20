package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesStepsWithCustodyTimeLimit;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceDataWithCustodyTimeLimit;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.UUID;

import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S2925") //TODO Remove once issuse of timeout identified in jenkins
public class DefendantOffencesChangedIT extends AbstractIT {

    private static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "public.listing.case-sent-for-listing";

    static {
        QueueUtil.publicEvents.createPublicConsumer(PUBLIC_EVENT_CASE_SENT_FOR_LISTING);
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgression() throws Exception {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UUID offenceIdToBeDeleted = defendantData.getOffences().get(1).getOffenceId();
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, offenceIdToBeDeleted);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublished();
        Thread.sleep(10000); // TODO Looks like this larger payload with both
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
    public void shouldUpdateDefendantOffencesWithCustodyTimeLimit() throws Exception {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UUID offenceIdToBeDeleted = defendantData.getOffences().get(1).getOffenceId();
        UpdatedOffenceDataWithCustodyTimeLimit updatedOffenceData = UpdatedOffenceDataWithCustodyTimeLimit.updateOffenceData(offenceData);

        final UpdateDefendantOffencesStepsWithCustodyTimeLimit steps = new UpdateDefendantOffencesStepsWithCustodyTimeLimit(caseId, hearingData, updatedOffenceData, offenceIdToBeDeleted);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublished();
        Thread.sleep(10000); // TODO Looks like this larger payload with both
        steps.verifyPublicEventDefendantOffencesUpdatedInActiveMQ();
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionUpdatedOnly() throws Exception {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedUpdatedOnly();
        Thread.sleep(10000);
        steps.verifyEventOffenceUpdatedInActiveMQ();

        steps.verifyDefendentOffenceUpdatedOnlyFromAPI(false);
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionAddedOnly() throws Exception {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null);
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly();
        Thread.sleep(10000);
        steps.verifyEventOffenceAddedInActiveMQ();

        steps.verifyDefendentOffenceAddedOnlyFromAPI(false);
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionDeletedOnly() throws Exception {
        HearingsData hearingsData = listCourtHearing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, updatedOffenceData.getOffenceId());
        steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedDeletedOnly();
        Thread.sleep(10000);
        steps.verifyEventOffenceDeletedInActiveMQ();

        steps.verifyDefendentOffenceDeletedOnlyFromAPI(false);
    }

    private HearingsData listCourtHearing() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedWithReportingRestrictionInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        return hearingsData;
    }

}
