package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.HearingAsMarkedSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HearingAsMarkedIT extends AbstractIT {

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
    public void shouldHearingAsMarked() {
        final HearingsData hearingsData = hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        HearingData hearingData = hearingsData.getHearingData().get(0);
        try (final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData)) {
            hearingAsMarkedSteps.whenHearingMarkedAsDuplicatePublicEventIsPublished();
            hearingAsMarkedSteps.verifyHearingMarkedAsDuplicatePublicEventInActiveMQ();
            hearingAsMarkedSteps.verifyHearingMarkedAsDuplicateInActiveMQ();
            hearingAsMarkedSteps.verifyHearingMarkedAsDuplicateForCaseInActiveMQ();
            hearingAsMarkedSteps.verifyDeletedFromHearingViewStore();

        }
    }

}
