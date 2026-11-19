package uk.gov.moj.cpp.listing.command.hearing.processor;

import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

/**
 * Port of the JEE {@code HearingUnallocatedCourtroomRemovedEventProcessor} (MBD refactor P2.3.2, ADR 015), logic unchanged.
 */
@Component
public class HearingUnallocatedCourtroomRemovedEventProcessor implements ProcessorPort {

    @Override
    public void register(final EventHandlers processors) {
        processors
                .onJson(uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved.class, this::handleHearingUnallocatedCourtroomRemoved);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUnallocatedCourtroomRemovedEventProcessor.class);
    private static final String EVENT_PAYLOAD_INFO_STRING = "Received '{}' event with payload {}";
    private static final String LISTING_EVENTS_HEARING_UNALLOCATED_COURTROOM_REMOVED = "listing.events.hearing-unallocated-courtroom-removed";
    private static final String PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED = "public.listing.hearing-unallocated-courtroom-removed";

    @Autowired
    private ProcessorSender sender;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    public void handleHearingUnallocatedCourtroomRemoved(Envelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_HEARING_UNALLOCATED_COURTROOM_REMOVED, jsonEnvelope.metadata());
        }

        final HearingUnallocatedCourtroomRemoved hearingUnallocatedCourtroomRemoved =
            jsonObjectConverter.convert(jsonEnvelope.payload(), HearingUnallocatedCourtroomRemoved.class);

        final JsonObject hearingUnallocatedCourtroomRemovedPublicEventPayload = Json.createObjectBuilder()
                .add("hearingId", hearingUnallocatedCourtroomRemoved.getHearingId().toString())
                .add("estimatedMinutes", hearingUnallocatedCourtroomRemoved.getEstimatedMinutes())
                .build();

        this.sender.send(envelopeFrom(
            metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_UNALLOCATED_COURTROOM_REMOVED),
            hearingUnallocatedCourtroomRemovedPublicEventPayload));
    }
}
