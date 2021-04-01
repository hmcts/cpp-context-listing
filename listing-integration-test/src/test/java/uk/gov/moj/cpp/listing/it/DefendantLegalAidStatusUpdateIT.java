package uk.gov.moj.cpp.listing.it;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.DefendantLegalAidStatusUpdateSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

public class DefendantLegalAidStatusUpdateIT extends AbstractIT {
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
    public void shouldUpdateDefendantLegalAidStatusFollowingPublicDefendantLegalAidStatusUpdatedEventFromProgression() throws Exception {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        try (final DefendantLegalAidStatusUpdateSteps defendantLegalAidStatusUpdateSteps = new DefendantLegalAidStatusUpdateSteps(caseId, hearingData)) {
            defendantLegalAidStatusUpdateSteps.whenCaseDefendantLegalAidStatusUpdatedPublicEventIsPublished();
            defendantLegalAidStatusUpdateSteps.verifyEventDefendantLegalAidStatusUpdatedInActiveMQ();
            defendantLegalAidStatusUpdateSteps.verifyHearingListedFromAPI(false);
        }

    }
}
