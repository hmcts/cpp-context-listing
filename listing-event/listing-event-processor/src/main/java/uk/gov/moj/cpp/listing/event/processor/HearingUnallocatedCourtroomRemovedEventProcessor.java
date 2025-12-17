package uk.gov.moj.cpp.listing.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingUnallocatedCourtroomRemovedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUnallocatedCourtroomRemovedEventProcessor.class);
    private static final String EVENT_PAYLOAD_INFO_STRING = "Received '{}' event with payload {}";
    private static final String LISTING_EVENTS_HEARING_UNALLOCATED_COURTROOM_REMOVED = "listing.events.hearing-unallocated-courtroom-removed";
    private static final String PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED = "public.listing.hearing-unallocated-courtroom-removed";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles(LISTING_EVENTS_HEARING_UNALLOCATED_COURTROOM_REMOVED)
    public void handleHearingUnallocatedCourtroomRemoved(JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_HEARING_UNALLOCATED_COURTROOM_REMOVED, jsonEnvelope.toObfuscatedDebugString());
        }

        final HearingUnallocatedCourtroomRemoved hearingUnallocatedCourtroomRemoved =
            jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingUnallocatedCourtroomRemoved.class);

        final JsonObject hearingUnallocatedCourtroomRemovedPublicEventPayload = Json.createObjectBuilder()
                .add("hearingId", hearingUnallocatedCourtroomRemoved.getHearingId().toString())
                .add("estimatedMinutes", hearingUnallocatedCourtroomRemoved.getEstimatedMinutes())
                .build();

        this.sender.send(envelopeFrom(
            metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED),
            hearingUnallocatedCourtroomRemovedPublicEventPayload));
    }
}
