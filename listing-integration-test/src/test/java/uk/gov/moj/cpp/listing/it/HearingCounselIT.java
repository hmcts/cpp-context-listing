package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.it.util.HearingHelper.getHearingById;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.FileUtil.payloadToObject;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.IOException;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Test;

public class HearingCounselIT extends AbstractIT {

    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED = "public.hearing.defence-counsel-added";
    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED = "public.hearing.defence-counsel-updated";
    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED = "public.hearing.defence-counsel-removed";
    private MessageProducer publicMessageProducer = publicEvents.createProducer();
    static final boolean ALLOCATED = true;

    @Test
    public void shouldAddAndUpdateDefenceCounsel() throws IOException {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary())) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

            //given
            UUID hearingId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId();

            //when
            sendPublicHearingDefenceCounselAdded(hearingId);
            //then
            getHearingById(USER_ID_VALUE, hearingId, withJsonPath("$.defenceCounsels[0].firstName", equalTo("Eric")));

            //when
            sendPublicHearingDefenceCounselUpdated(hearingId);
            //then
            getHearingById(USER_ID_VALUE, hearingId, withJsonPath("$.defenceCounsels[0].firstName", equalTo("EricUpdated")));

            //when
            sendPublicHearingDefenceCounselRemoved(hearingId);
            //then
            getHearingById(USER_ID_VALUE, hearingId, withJsonPath("$.defenceCounsels", hasSize(0)));
        }
    }

    private void sendPublicHearingDefenceCounselAdded(final UUID hearingId) throws IOException {
        JsonObject jsonObject = payloadToObject(
                getPayload("stub-data/public.hearing.defence-counsel-added.json")
                        .replace("HEARING_ID", hearingId.toString()));
        sendMessage(publicMessageProducer,
                PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED, jsonObject, metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED)
                        .withUserId(randomUUID().toString())
                        .build());
    }

    private void sendPublicHearingDefenceCounselUpdated(final UUID hearingId) throws IOException {
        JsonObject jsonObject = payloadToObject(
                getPayload("stub-data/public.hearing.defence-counsel-updated.json")
                        .replace("HEARING_ID", hearingId.toString()));
        sendMessage(publicMessageProducer,
                PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED, jsonObject, metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED)
                        .withUserId(randomUUID().toString())
                        .build());
    }

    private void sendPublicHearingDefenceCounselRemoved(final UUID hearingId) throws IOException {
        JsonObject jsonObject = payloadToObject(
                getPayload("stub-data/public.hearing.defence-counsel-removed.json")
                        .replace("HEARING_ID", hearingId.toString()));
        sendMessage(publicMessageProducer,
                PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED, jsonObject, metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED)
                        .withUserId(randomUUID().toString())
                        .build());
    }

    @After
    public void tearDown() throws JMSException {
        publicMessageProducer.close();
    }
}
