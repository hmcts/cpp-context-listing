package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.givenAUserHasLoggedInAsAListingOfficers;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenAllocatedHearingsForACourtCentreShouldContainAllocatedHearingData;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenAllocatedHearingsForACourtCentreShouldContainUpdatedHearingData;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenHearingConfirmedPublicEventShouldBePublished;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenHearingUpdatedPublicEventShouldBePublished;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenQueryValidationFailureOccursWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainTwoExpectedHearingsWhenQueried;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomId;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.whenCaseIsSubmittedForListing;
import static uk.gov.moj.cpp.listing.steps.ListingStepDefinitions.whenHearingIsUpdatedForListing;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithAllFieldsSet;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithoutCourtRoomId;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataWithoutJudgeId;
import static uk.gov.moj.cpp.listing.steps.data.factory.CaseDataFactory.caseData;
import static uk.gov.moj.cpp.listing.steps.data.factory.CaseDataFactory.caseDataExisting;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.data.CaseData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.steps.data.factory.CaseDataFactory;

import javax.jms.JMSException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class HearingIT extends AbstractIT {

    private static final String PUBLIC_HEARING_CONFIRMED = "public.hearing-confirmed";
    private static final String PUBLIC_HEARING_UPDATED = "public.hearing-updated";
    private static final String TOPIC_NAME = "public.event";

    private MessageConsumerClient hearingConfirmedMessageConsumer = new MessageConsumerClient();
    private MessageConsumerClient hearingUpdatedMessageConsumer = new MessageConsumerClient();

    @Before
    public void setup() {
        hearingConfirmedMessageConsumer.startConsumer(PUBLIC_HEARING_CONFIRMED, TOPIC_NAME);
        hearingUpdatedMessageConsumer.startConsumer(PUBLIC_HEARING_UPDATED, TOPIC_NAME);
    }

    @After
    public void tearDown() {
        hearingConfirmedMessageConsumer.close();
        hearingUpdatedMessageConsumer.close();
    }

    @Test
    public void shouldReturnMultipleHearingsListed() throws JMSException {
        final CaseData caseData = caseData();
        final CaseData caseDataNew = caseDataExisting(
                caseData.getCaseId().toString(),
                caseData.getHearingData().get(0).getCourtCentreId().toString()
        );

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        // Receive a caseForListing first time in to Listing
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        // Receive a caseForListing that was received before
        whenCaseIsSubmittedForListing(caseDataNew);
        thenUnallocatedHearingsForACourtCentreShouldContainTwoExpectedHearingsWhenQueried(caseData, caseDataNew);
    }

    @Test
    public void shouldFailQueryValidationForSearchForUnallocatedHearings() throws
            JMSException {

        final CaseData caseData = caseData();

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        thenQueryValidationFailureOccursWhenQueried(caseData);
    }

    @Test
    public void shouldUpdateAnExistingHearingWithoutCourtRoomId() throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData hearingDataWithoutJudgeData = updatedHearingDataWithoutJudgeId(caseData.getHearingData().get(0).getId());
        final UpdatedHearingData hearingDataWithoutCourtRoomData = updatedHearingDataWithoutCourtRoomId(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(hearingDataWithoutJudgeData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId(caseData, hearingDataWithoutJudgeData);

        whenHearingIsUpdatedForListing(hearingDataWithoutCourtRoomData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomId(caseData, hearingDataWithoutCourtRoomData);
    }

    @Test
    public void shouldUpdateAnExistingHearingWithoutJudgeId() throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData hearingDataWithoutJudgeData = updatedHearingDataWithoutJudgeId(caseData.getHearingData().get(0).getId());
        final UpdatedHearingData hearingDataWithoutCourtRoomData = updatedHearingDataWithoutCourtRoomId(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(hearingDataWithoutCourtRoomData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutCourtRoomId(caseData, hearingDataWithoutCourtRoomData);

        whenHearingIsUpdatedForListing(hearingDataWithoutJudgeData);
        thenUnallocatedHearingsForACourtCentreShouldContainUpdatedHearingDataWithoutJudgeId(caseData, hearingDataWithoutJudgeData);
    }

    @Test
    public void shouldUpdateAnExistingHearingToBeAllocatedAndRaiseHearingConfirmedPublicEvent()
            throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData updatedHearingData = updatedHearingDataWithAllFieldsSet(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(updatedHearingData);
        thenAllocatedHearingsForACourtCentreShouldContainUpdatedHearingData(caseData, updatedHearingData);

        thenHearingConfirmedPublicEventShouldBePublished(updatedHearingData.getHearingId(), hearingConfirmedMessageConsumer);
    }

    @Test
    public void shouldUpdateAnAllocatedHearingAndRaiseHearingConfirmedPublicEventFollowedByHearingUpdatedPublicEvent()
            throws JMSException {
        final CaseData caseData = caseData();
        final UpdatedHearingData updatedHearingData = updatedHearingDataWithAllFieldsSet(caseData.getHearingData().get(0).getId());

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        whenCaseIsSubmittedForListing(caseData);
        thenUnallocatedHearingsForACourtCentreShouldContainExpectedHearingWhenQueried(caseData);

        whenHearingIsUpdatedForListing(updatedHearingData);
        thenAllocatedHearingsForACourtCentreShouldContainUpdatedHearingData(caseData, updatedHearingData);

        thenHearingConfirmedPublicEventShouldBePublished(updatedHearingData.getHearingId(), hearingConfirmedMessageConsumer);

        whenHearingIsUpdatedForListing(updatedHearingData);
        thenHearingUpdatedPublicEventShouldBePublished(updatedHearingData.getHearingId(), hearingUpdatedMessageConsumer);

    }

    @Test
    public void shouldSendCaseForListingAndAllocateHearingAndRaiseHearingConfirmedPublicEvent()
            throws JMSException {
        final CaseData caseData = CaseDataFactory.caseWithAllocationHearingData();
        final HearingData hearingData = caseData.getHearingData().get(0);

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        whenCaseIsSubmittedForListing(caseData);

        thenAllocatedHearingsForACourtCentreShouldContainAllocatedHearingData(hearingData);

        thenHearingConfirmedPublicEventShouldBePublished(hearingData.getId(), hearingConfirmedMessageConsumer);
    }
}
