package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.SendCaseForListingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantOffencesSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedOffenceData;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
@SuppressWarnings("squid:S2925") //TODO Remove once issuse of timeout identified in jenkins
public class DefendantOffencesChangedIT extends AbstractIT {

    private static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "public.listing.case-sent-for-listing";
    private static final String TOPIC_NAME = "public.event";

    private MessageConsumerClient publicMessageConsumer = new MessageConsumerClient();

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @Before
    public void setup() {

        publicMessageConsumer.startConsumer(PUBLIC_EVENT_CASE_SENT_FOR_LISTING, TOPIC_NAME);
    }

    @After
    public void tearDown() { publicMessageConsumer.close(); }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgression() throws Exception {
        HearingsData hearingsData = sendCaseForListing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UUID offenceIdToBeDeleted = defendantData.getOffences().get(1).getOffenceId();
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        try (final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, offenceIdToBeDeleted)) {
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
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionUpdatedOnly() throws Exception {
        HearingsData hearingsData = sendCaseForListing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        try (final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null)) {
            steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedUpdatedOnly();
            Thread.sleep(10000);
            steps.verifyEventOffenceUpdatedInActiveMQ();

            steps.verifyDefendentOffenceUpdatedOnlyFromAPI(false);

        }
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionAddedOnly() throws Exception {
        HearingsData hearingsData = sendCaseForListing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        try (final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, null)) {
            steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedAddedOnly();
            Thread.sleep(10000);
            steps.verifyEventOffenceAddedInActiveMQ();

            steps.verifyDefendentOffenceAddedOnlyFromAPI(false);

        }
    }

    @Test
    public void shouldUpdateDefendantOffencesFollowingPublicDefendantOffencesChangedEventFromProgressionDeletedOnly() throws Exception {
        HearingsData hearingsData = sendCaseForListing();

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        OffenceData offenceData = defendantData.getOffences().get(0);
        UpdatedOffenceData updatedOffenceData = UpdatedOffenceData.updateOffenceData(offenceData);

        try (final UpdateDefendantOffencesSteps steps = new UpdateDefendantOffencesSteps(caseId, hearingData, updatedOffenceData, updatedOffenceData.getOffenceId())) {
            steps.whenCaseDefendantOffencesUpdatedPublicEventIsPublishedDeletedOnly();
            Thread.sleep(10000);
            steps.verifyEventOffenceDeletedInActiveMQ();

            steps.verifyDefendentOffenceDeletedOnlyFromAPI(false);

        }
    }

    private HearingsData sendCaseForListing() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final SendCaseForListingSteps sendCaseForListingSteps = new SendCaseForListingSteps(hearingsData)) {
            sendCaseForListingSteps.whenCaseIsSubmittedForListing();
            sendCaseForListingSteps.verifyHearingListedInActiveMQ();
            sendCaseForListingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        return hearingsData;
    }

}
