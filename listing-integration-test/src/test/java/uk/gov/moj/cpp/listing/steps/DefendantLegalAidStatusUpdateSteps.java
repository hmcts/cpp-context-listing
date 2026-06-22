package uk.gov.moj.cpp.listing.steps;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.util.UUID;

import javax.json.JsonObject;

public class DefendantLegalAidStatusUpdateSteps extends AbstractIT {

    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";

    private JmsMessageProducerClient publicEventDefendantLegalAidStatusUpdated;


    private final HearingData hearingData;
    private final ListedCaseData listedCaseData;
    private final UUID caseId;

    public DefendantLegalAidStatusUpdateSteps(UUID caseId, HearingData hearingData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);

        publicEventDefendantLegalAidStatusUpdated = QueueUtil.publicEvents.createPublicProducer();

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseDefendantLegalAidStatusUpdatedPublicEventIsPublished() {

        final JsonObject payload = getPayloadForPublicEventFromHearingData();
        sendMessage(
                publicEventDefendantLegalAidStatusUpdated,
                PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED,
                payload,
                metadataOf(randomUUID(), PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED).withUserId(randomUUID().toString()).build());

    }

    private JsonObject getPayloadForPublicEventFromHearingData() {
        return createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", listedCaseData.getDefendants().get(0).getDefendantId().toString())
                .add("legalAidStatus", "Granted")
                .build();

    }
}
