package uk.gov.moj.cpp.listing.command.inbound;

import static jakarta.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Port of the JEE {@code HearingCounselEventProcessor} (MBD refactor P2.4): the six hearing-context
 * counsel events → {@code listing.command.handler.modify-hearing-counsel} with the action, the counsel
 * type and the counsel (add/update: the counsel object as a JSON string; remove: the counsel id as a
 * quoted JSON string, {@code JsonString.toString()}, as JEE sent it).
 */
@SuppressWarnings("squid:S2629")
@Component
public class HearingCounselEventProcessor extends InboundProcessor {

    static final String PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED = "public.hearing.defence-counsel-added";
    static final String PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED = "public.hearing.defence-counsel-updated";
    static final String PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED = "public.hearing.defence-counsel-removed";
    static final String PUBLIC_HEARING_PROSECUTION_COUNSEL_ADDED = "public.hearing.prosecution-counsel-added";
    static final String PUBLIC_HEARING_PROSECUTION_COUNSEL_UPDATED = "public.hearing.prosecution-counsel-updated";
    static final String PUBLIC_HEARING_PROSECUTION_COUNSEL_REMOVED = "public.hearing.prosecution-counsel-removed";
    static final String LISTING_COMMAND_HANDLER_MODIFY_HEARING_COUNSEL = "listing.command.handler.modify-hearing-counsel";

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingCounselEventProcessor.class);
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String COUNSEL_TYPE_DEFENCE = "DEFENCE";
    private static final String COUNSEL_TYPE_PROSECUTION = "PROSECUTION";
    private static final String ACTION_REMOVE = "REMOVE";
    private static final String ACTION_ADD = "ADD";
    private static final String ACTION_UPDATE = "UPDATE";

    private final ProcessorSender sender;

    public HearingCounselEventProcessor(final ProcessorQueue processorQueue, final ProcessorSender sender) {
        super(processorQueue);
        this.sender = sender;
        on(PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED, this::hearingDefenceCounselAdded);
        on(PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED, this::hearingDefenceCounselUpdated);
        on(PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED, this::hearingDefenceCounselRemoved);
        on(PUBLIC_HEARING_PROSECUTION_COUNSEL_ADDED, this::hearingProsecutionCounselAdded);
        on(PUBLIC_HEARING_PROSECUTION_COUNSEL_UPDATED, this::hearingProsecutionCounselUpdated);
        on(PUBLIC_HEARING_PROSECUTION_COUNSEL_REMOVED, this::hearingProsecutionCounselRemoved);
    }

    public void hearingDefenceCounselAdded(final Envelope envelope) {
        LOGGER.info("{} {}", PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED, envelope.metadata());
        handleCounselModification(envelope, COUNSEL_TYPE_DEFENCE, ACTION_ADD);
    }

    public void hearingProsecutionCounselAdded(final Envelope envelope) {
        LOGGER.info("{} {}", PUBLIC_HEARING_PROSECUTION_COUNSEL_ADDED, envelope.metadata());
        handleCounselModification(envelope, COUNSEL_TYPE_PROSECUTION, ACTION_ADD);
    }

    public void hearingDefenceCounselRemoved(final Envelope envelope) {
        LOGGER.info("{} {}", PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED, envelope.metadata());
        handleCounselModification(envelope, COUNSEL_TYPE_DEFENCE, ACTION_REMOVE);
    }

    public void hearingProsecutionCounselRemoved(final Envelope envelope) {
        LOGGER.info("{} {}", PUBLIC_HEARING_PROSECUTION_COUNSEL_REMOVED, envelope.metadata());
        handleCounselModification(envelope, COUNSEL_TYPE_PROSECUTION, ACTION_REMOVE);
    }

    public void hearingDefenceCounselUpdated(final Envelope envelope) {
        LOGGER.info("{} {}", PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED, envelope.metadata());
        handleCounselModification(envelope, COUNSEL_TYPE_DEFENCE, ACTION_UPDATE);
    }

    public void hearingProsecutionCounselUpdated(final Envelope envelope) {
        LOGGER.info("{} {}", PUBLIC_HEARING_PROSECUTION_COUNSEL_UPDATED, envelope.metadata());
        handleCounselModification(envelope, COUNSEL_TYPE_PROSECUTION, ACTION_UPDATE);
    }

    private void handleCounselModification(final Envelope envelope, final String counselType, final String action) {
        final JsonObject eventPayload = envelope.payload();
        final boolean isAddOrUpdate = ACTION_ADD.equals(action) || ACTION_UPDATE.equals(action);
        final String nodeName = isAddOrUpdate ? counselType.toLowerCase() + "Counsel" : "id";
        final String payload = isAddOrUpdate ? eventPayload.getJsonObject(nodeName).toString()
                : eventPayload.getJsonString(nodeName).toString();
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_HANDLER_MODIFY_HEARING_COUNSEL),
                createObjectBuilder()
                        .add("action", action)
                        .add("counselType", counselType)
                        .add(FIELD_HEARING_ID, eventPayload.getString(FIELD_HEARING_ID))
                        .add("payload", payload)
                        .build()));
    }
}
