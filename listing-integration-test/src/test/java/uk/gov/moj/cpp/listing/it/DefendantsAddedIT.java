package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.AddDefendantSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefendantsAddedIT extends AbstractIT {

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
    public void shouldAddDefendantsFollowingPublicDefendantsAddedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

      UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
      HearingData hearingData = hearingsData.getHearingData().get(0);
       try (final AddDefendantSteps addDefendantSteps = new AddDefendantSteps(caseId, hearingData)) {
            addDefendantSteps.whenCaseDefendantsAddedPublicEventIsPublished();
            addDefendantSteps.verifyEventDefendantAddedInActiveMQ();
            addDefendantSteps.verifyEventDefendantsToBeAddedInActiveMQ();
            addDefendantSteps.verifyEventDefendantDetailsAddedInActiveMQ();
            addDefendantSteps.verifyHearingListedFromAPI(false);
        }
    }

}
