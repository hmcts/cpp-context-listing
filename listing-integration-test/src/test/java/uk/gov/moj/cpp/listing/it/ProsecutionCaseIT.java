package uk.gov.moj.cpp.listing.it;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class ProsecutionCaseIT extends AbstractIT{
    private static final String PUBLIC_EVENT_CPS_PROSECUTOR_UPDATED = "public.progression.events.cps-prosecutor-updated";

    private JmsMessageProducerClient publicEventProgression;

    @BeforeEach
    public void setup(){
        publicEventProgression = QueueUtil.publicEvents.createPublicProducer();
    }

    @Test
    public void shouldUpdateProsecutionCase()  {
        String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = getRandomCourtCenterId();
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", UUID.fromString(courtScheduleId).toString());
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubGetReferenceDataCourtCentreById(courtCentreId);
        stubProvisionalBookingWithCustomParams(stubParams);

        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                courtScheduleId, listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);
        List<String> idList = hearingsData.getHearingData().stream().map(h -> h.getId().toString()).toList();
        List<String> prosecutionIdList = hearingsData.getHearingData().stream().flatMap(h -> h.getListedCases().stream()).map(lc -> lc.getCaseId().toString()).toList();


        JsonObject payload = JsonObjects.createObjectBuilder()
                .add("prosecutionCaseId", prosecutionIdList.get(0))
                .add("hearingIds", JsonObjects.createArrayBuilder()
                        .add(idList.get(0))
                        .build())
                .add("caseURN", "test Case URN")
                .add("prosecutionAuthorityId", hearingsData.getHearingData().get(0).getListedCases().get(0).getAuthorityId().toString())
                .add("prosecutionAuthorityReference", "test prosecutionAuthorityReference")
                .add("prosecutionAuthorityCode", hearingsData.getHearingData().get(0).getListedCases().get(0).getAuthorityCode())
                .add("prosecutionAuthorityName", "test prosecutionAuthorityName")
                .add("address", JsonObjects.createObjectBuilder()
                        .add("address1", "41 Manhattan House")
                        .add("postcode", "MK9 2BQ")
                        .build())
                .build();
        QueueUtil.sendMessage(
                publicEventProgression,
                PUBLIC_EVENT_CPS_PROSECUTOR_UPDATED,
                payload,
                metadataOf(randomUUID(), PUBLIC_EVENT_CPS_PROSECUTOR_UPDATED).withUserId(randomUUID().toString()).build());

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, null);
         updateHearingSteps.verifyCaseIdentifierWhenQueryingFromAPIWithJmsDelay(idList.get(0), payload, hearingsData);
    }

}
