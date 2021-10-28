package uk.gov.moj.cpp.listing.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.courts.AddCasesToHearing;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import javax.inject.Inject;
import javax.json.Json;

@ServiceComponent(EVENT_PROCESSOR)
public class UpdateExistingHearingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateExistingHearingEventProcessor.class);
    private static final String PUBLIC_EVENT_CASES_ADDED_FOR_UPDATED_RELATED_HEARING = "public.events.listing.cases-added-for-updated-related-hearing";
    private static final String PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED = "listing.events.update-existing-hearing-requested";
    private static final String PRIVATE_EVENT_CASES_ADDED_TO_HEARING = "listing.event.cases-added-to-hearing";

    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    private static final String COMMAND_ADD_CASES_TO_HEARING = "listing.command.add-cases-to-hearing";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles(PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED)
    public void handleUpdateExistingHearingRequestedEvent(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED, envelope.toObfuscatedDebugString());
        }

        final UpdateExistingHearingRequested event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateExistingHearingRequested.class);
        final AddCasesToHearing addCasesForHearing = AddCasesToHearing.addCasesToHearing()
                 .withHearingId(event.getHearingId())
                 .withProsecutionCases(event.getProsecutionCases())
                 .withShadowListedOffences(event.getShadowListedOffences())
                 .withSeedingHearingId(event.getSeedingHearingId())
                 .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_CASES_TO_HEARING), objectToJsonObjectConverter.convert(addCasesForHearing)));

    }

    @SuppressWarnings("squid:S3655")
    @Handles(PRIVATE_EVENT_CASES_ADDED_TO_HEARING)
    public void handleCasesAddedToHearingEvent(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CASES_ADDED_TO_HEARING, envelope.toObfuscatedDebugString());
        }

        final CasesAddedToHearing casesAddedToHearing = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CasesAddedToHearing.class);
        if (casesAddedToHearing.getSeedingHearingId().isPresent()) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_CASES_ADDED_FOR_UPDATED_RELATED_HEARING),
                    Json.createObjectBuilder()
                            .add("hearingId", casesAddedToHearing.getHearingId().toString())
                            .add("seedingHearingId", casesAddedToHearing.getSeedingHearingId().get().toString())
                            .build()
            ));
        }
    }

}
