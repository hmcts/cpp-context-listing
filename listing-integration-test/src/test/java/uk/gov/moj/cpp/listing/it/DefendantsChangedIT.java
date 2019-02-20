package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.SendCaseForListingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefendantsChangedIT extends AbstractIT {

    private static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "public.listing.case-sent-for-listing";
    private static final String TOPIC_NAME = "public.event";

    private MessageConsumerClient publicMessageConsumer = new MessageConsumerClient();

    @Before
    public void setup() {
        publicMessageConsumer.startConsumer(PUBLIC_EVENT_CASE_SENT_FOR_LISTING, TOPIC_NAME);
    }

    @After
    public void tearDown() {
        publicMessageConsumer.close();
    }

    @Test
    public void shouldUpdateDefendantsFollowingPublicDefendantsChangedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedInActiveMQ();
            sendCaseForListingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedDefendantData updatedDefendantData = UpdatedDefendantData.updatedDefendantData(defendantData);

        try (final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData)) {
            updateDefendantSteps.whenCaseDefendantsUpdatedPublicEventIsPublished();
            updateDefendantSteps.verifyEventDefendantUpdatedInActiveMQ();
            updateDefendantSteps.verifyEventDefendantsToBeUpdateInActiveMQ();
            updateDefendantSteps.verifyEventDefendantDetailsUpdatedInActiveMQ();
            updateDefendantSteps.verifyHearingListedFromAPI(false);
        }
    }

}
