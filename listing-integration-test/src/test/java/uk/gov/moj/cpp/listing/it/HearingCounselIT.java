package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.it.util.HearingHelper.pollForHearingById;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.FileUtil.payloadToObject;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
class HearingCounselIT extends AbstractIT {

    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED = "public.hearing.defence-counsel-added";
    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED = "public.hearing.defence-counsel-updated";
    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED = "public.hearing.defence-counsel-removed";
    private final JmsMessageProducerClient publicMessageProducer = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    static final boolean ALLOCATED = true;

    @Test
    void shouldAddAndUpdateDefenceCounsel() throws IOException {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary());
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(), "8e837de0-743a-4a2c-9db3-b2e678c48729",
                listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        //given
        UUID hearingId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId();

        //when
        sendPublicHearingDefenceCounselAdded(hearingId);
        //then
        pollForHearingById(USER_ID_VALUE, hearingId, withJsonPath("$.defenceCounsels[0].firstName", equalTo("Eric")));

        //when
        sendPublicHearingDefenceCounselUpdated(hearingId);
        //then
        pollForHearingById(USER_ID_VALUE, hearingId, withJsonPath("$.defenceCounsels[0].firstName", equalTo("EricUpdated")));

        //when
        sendPublicHearingDefenceCounselRemoved(hearingId);
        //then
        pollForHearingById(USER_ID_VALUE, hearingId, withJsonPath("$.defenceCounsels", hasSize(0)));
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
}
