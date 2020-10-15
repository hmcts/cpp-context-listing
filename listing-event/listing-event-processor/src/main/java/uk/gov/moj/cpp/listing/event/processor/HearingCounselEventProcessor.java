package uk.gov.moj.cpp.listing.event.processor;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2629")
@ServiceComponent(EVENT_PROCESSOR)
public class HearingCounselEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingCounselEventProcessor.class);
    private static final String LISTING_COMMAND_HANDLER_MODIFY_HEARING_COUNSEL = "listing.command.handler.modify-hearing-counsel";
    private static final String FIELD_HEARING_ID = "hearingId";
    private static final String COUNSEL_TYPE_DEFENCE = "DEFENCE";
    private static final String COUNSEL_TYPE_PROSECUTION = "PROSECUTION";
    private static final String ACTION_REMOVE = "REMOVE";
    private static final String ACTION_ADD = "ADD";
    private static final String ACTION_UPDATE = "UPDATE";

    @Inject
    private Sender sender;

    @Handles("public.hearing.defence-counsel-added")
    public void hearingDefenceCounselAdded(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.defence-counsel-added {}", envelope.toObfuscatedDebugString());
        }

        handleCounselModification(envelope, COUNSEL_TYPE_DEFENCE, ACTION_ADD);
    }

    @Handles("public.hearing.prosecution-counsel-added")
    public void hearingProsecutionCounselAdded(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.prosecution-counsel-added {}", envelope.toObfuscatedDebugString());
        }

        handleCounselModification(envelope, COUNSEL_TYPE_PROSECUTION, ACTION_ADD);

    }

    @Handles("public.hearing.defence-counsel-removed")
    public void hearingDefenceCounselRemoved(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.defence-counsel-removed {}", envelope.toObfuscatedDebugString());
        }

        handleCounselModification(envelope, COUNSEL_TYPE_DEFENCE, ACTION_REMOVE);
    }

    @Handles("public.hearing.prosecution-counsel-removed")
    public void hearingProsecutionCounselRemoved(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.prosecution-counsel-removed {}", envelope.toObfuscatedDebugString());
        }

        handleCounselModification(envelope, COUNSEL_TYPE_PROSECUTION, ACTION_REMOVE);
    }

    @Handles("public.hearing.defence-counsel-updated")
    public void hearingDefenceCounselUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.defence-counsel-updated {}", envelope.toObfuscatedDebugString());
        }

        handleCounselModification(envelope, COUNSEL_TYPE_DEFENCE, ACTION_UPDATE);
    }

    @Handles("public.hearing.prosecution-counsel-updated")
    public void hearingProsecutionCounselUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.prosecution-counsel-updated {}", envelope.toObfuscatedDebugString());
        }

        handleCounselModification(envelope, COUNSEL_TYPE_PROSECUTION, ACTION_UPDATE);
    }

    private void handleCounselModification(final JsonEnvelope envelope, final String counselType,
                                           final String action) {
        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final boolean isAddOrUpdate = ACTION_ADD.equals(action) || ACTION_UPDATE.equals(action);
        final String nodeName = isAddOrUpdate ? counselType.toLowerCase() + "Counsel" : "id";
        final String payload = isAddOrUpdate ? eventPayload.getJsonObject(nodeName).toString() :
                eventPayload.getJsonString(nodeName).toString();
        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(LISTING_COMMAND_HANDLER_MODIFY_HEARING_COUNSEL),
                createObjectBuilder()
                        .add("action", action)
                        .add("counselType", counselType)
                        .add(FIELD_HEARING_ID, eventPayload.getString(FIELD_HEARING_ID))
                        .add("payload", payload)));
    }
}
