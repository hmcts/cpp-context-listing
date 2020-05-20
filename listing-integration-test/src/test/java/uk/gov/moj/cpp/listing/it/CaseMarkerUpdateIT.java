package uk.gov.moj.cpp.listing.it;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateCaseMarkersSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseMarkerData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S2925")
public class CaseMarkerUpdateIT extends AbstractIT {

    private static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "public.progression.case-markers-updated";
    private static final String TOPIC_NAME = "public.event";

    private final MessageConsumerClient publicMessageConsumer = new MessageConsumerClient();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @Before
    public void setup() {
        publicMessageConsumer.startConsumer(PUBLIC_EVENT_CASE_SENT_FOR_LISTING, TOPIC_NAME);
    }

    @After
    public void tearDown() {
        publicMessageConsumer.close();
    }

    @Test
    public void shouldUpdateCaseMarkersForListedCase() throws Exception {
        final HearingsData hearingsData = listCourtHearing();
        final CaseMarkerData caseMarkerData = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseMarkers().get(0);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        try (final UpdateCaseMarkersSteps steps = new UpdateCaseMarkersSteps(caseId, hearingData, caseMarkerData)) {
            steps.whenCaseMarkerUpdatedPublicEventIsPublished();
            Thread.sleep(10000); // TODO Looks like this larger payload with both
            steps.verifyPublicEventCaseMarkersUpdatedInActiveMQ();
            steps.verifyEventCaseMarkersToBeUpdateInActiveMQ();

            // add/update/delete message causes timing issues where messages are not ready to be pulled of the queue for verification
            steps.verifyEventCaseMarkersUpdatedInActiveMQ();
        }
    }

    private HearingsData listCourtHearing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        return hearingsData;
    }
}
