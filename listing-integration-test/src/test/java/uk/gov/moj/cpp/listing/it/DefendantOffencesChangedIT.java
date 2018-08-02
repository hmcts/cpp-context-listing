package uk.gov.moj.cpp.listing.it;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.data.CaseData;

import javax.jms.JMSException;

import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.*;
import static uk.gov.moj.cpp.listing.steps.data.factory.CaseDataFactory.caseData;

public class DefendantOffencesChangedIT extends AbstractIT {

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
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgression() throws JMSException {
        final CaseData caseData = caseData();

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        whenCaseIsSubmittedForListing(caseData);
        thenCaseSentForListingPublicEventShouldBePublished(caseData, publicMessageConsumer);

        whenCaseDefendantOffencesChangedPublicEventIsPublished(caseData);

        thenDefendantOffencesShouldHaveUpdatedWhenQueried(caseData);
        thenDefendantOffencesShouldHaveAddedWhenQueried(caseData);
        thenDefendantOffencesShouldHaveDeletedWhenQueried(caseData);
    }
}
