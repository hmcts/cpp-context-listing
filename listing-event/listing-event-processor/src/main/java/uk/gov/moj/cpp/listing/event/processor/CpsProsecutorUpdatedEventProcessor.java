package uk.gov.moj.cpp.listing.event.processor;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import org.slf4j.Logger;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class CpsProsecutorUpdatedEventProcessor {
    private static final Logger LOGGER = getLogger(CpsProsecutorUpdatedEventProcessor.class);

    private static final String HEARINGS_ID = "hearingIds";
    private static final String PROSECUTION_CASE_ID= "prosecutionCaseId";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private HearingQueryView hearingQueryView;

    @Handles("public.progression.events.cps-prosecutor-updated")
    public void cpsProsecutorUpdated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.progression.events.cps-prosecutor-updated event received {}", event.toObfuscatedDebugString());
        }

        final JsonObject inputPayload = event.payloadAsJsonObject();
        final JsonObject queryPayload = JsonObjects.createObjectBuilder().add("caseId", inputPayload.get(PROSECUTION_CASE_ID)).build();

        final JsonEnvelope queryResponse = hearingQueryView.searchAllocatedAndUnallocatedHearings(envelopeFrom(metadataFrom(event.metadata())
                .withName("listing.allocated.and.unallocated.hearings"), queryPayload));

        final List<String> hearingIds = queryResponse.payloadAsJsonObject().getJsonArray("hearings").stream()
                .map(h -> (JsonObject)h)
                .map(h-> h.getString("id")).collect(Collectors.toList());


        if(hearingIds.isEmpty()){
            if(LOGGER.isDebugEnabled()) {
                LOGGER.info("public.progression.events.cps-prosecutor-updated event received without hearings in listing {}", event.toObfuscatedDebugString());
            }
        }else{
            final JsonArrayBuilder builder = JsonObjects.createArrayBuilder();
            hearingIds.forEach(builder::add);
            final JsonObject outputPayload = JsonObjects.createObjectBuilder().add(PROSECUTION_CASE_ID, inputPayload.get(PROSECUTION_CASE_ID))
                    .add("prosecutionAuthorityId", inputPayload.get("prosecutionAuthorityId"))
                    .add("prosecutionAuthorityCode", inputPayload.get("prosecutionAuthorityCode"))
                    .add(HEARINGS_ID, builder.build())
                    .build();
            sender.send(envelopeFrom(metadataFrom(event.metadata())
                    .withName("listing.command.update-cps-prosecutor-with-associated-hearings"), outputPayload));
        }
    }
}
