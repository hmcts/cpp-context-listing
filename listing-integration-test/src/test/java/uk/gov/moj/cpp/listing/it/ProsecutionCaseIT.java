package uk.gov.moj.cpp.listing.it;


import java.util.List;
import java.util.stream.Collectors;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

public class ProsecutionCaseIT extends AbstractIT{
    private static final String PUBLIC_EVENT_CPS_PROSECUTOR_UPDATED = "public.progression.events.cps-prosecutor-updated";

    private MessageProducer publicEventProgression;

    @Before
    public void setup(){
        publicEventProgression = QueueUtil.publicEvents.createProducer();
    }

    @After
    public void tearDown() throws Exception {
        publicEventProgression.close();
    }

    @Test
    public void shouldUpdateProsecutionCase()  {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingAllocatedForListingInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }
        List<String> idList = hearingsData.getHearingData().stream().map(h -> h.getId().toString()).collect(Collectors.toList());
        List<String> prosecutionIdList = hearingsData.getHearingData().stream().flatMap(h -> h.getListedCases().stream()).map(lc -> lc.getCaseId().toString()).collect(Collectors.toList());


        JsonObject payload = Json.createObjectBuilder()
                .add("prosecutionCaseId", prosecutionIdList.get(0))
                .add("hearingIds", Json.createArrayBuilder()
                        .add(idList.get(0))
                        .build())
                .add("caseURN", "test Case URN")
                .add("prosecutionAuthorityId", randomUUID().toString())
                .add("prosecutionAuthorityReference", "test prosecutionAuthorityReference")
                .add("prosecutionAuthorityCode", "test prosecutionAuthorityCode")
                .add("prosecutionAuthorityName", "test prosecutionAuthorityName")
                .add("address", Json.createObjectBuilder()
                        .add("address1", "41 Manhattan House")
                        .add("postcode", "MK9 2BQ")
                        .build())
                .build();
        QueueUtil.sendMessage(
                publicEventProgression,
                PUBLIC_EVENT_CPS_PROSECUTOR_UPDATED,
                payload,
                metadataOf(randomUUID(), PUBLIC_EVENT_CPS_PROSECUTOR_UPDATED).withUserId(randomUUID().toString()).build());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, null)) {
             updateHearingSteps.verifyCaseIdentifierWhenQueryingFromAPI(idList.get(0), payload, hearingsData);
        }
    }

}
