package uk.gov.moj.cpp.listing.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class ListingNoteEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListingNoteEventProcessor.class);
    private static final String EVENT_PAYLOAD_INFO_STRING = "Received '{}' event with payload {}";
    private static final String LISTING_EVENTS_NOTE_CREATED_LISTING_NOTE = "listing.events.created-listing-note";
    private static final String PUBLIC_LISTING_NOTE_CREATED_LISTING_NOTE = "public.listing.created-listing-note";
    private static final String LISTING_EVENTS_NOTE_EDITED = "listing.events.listing-note-edited";
    public static final String PUBLIC_LISTING_NOTE_EDITED = "public.listing.note-edited";

    private static final String LISTING_EVENTS_NOTE_DELETED_LISTING_NOTE = "listing.events.deleted-listing-note";
    private static final String PUBLIC_LISTING_NOTE_DELETED_LISTING_NOTE = "public.listing.deleted-listing-note";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles(LISTING_EVENTS_NOTE_CREATED_LISTING_NOTE)
    public void handlesNoteCreatedForListing(JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_NOTE_CREATED_LISTING_NOTE, jsonEnvelope.toObfuscatedDebugString());
        }

        final CreatedListingNote createdListingNote = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), CreatedListingNote.class);

        final JsonObject listingNoteCreatedPublicEventPayload = JsonObjects.createObjectBuilder()
                .add("id", createdListingNote.getId().toString())
                .add("date", createdListingNote.getHearingDate())
                .add("courtRoomId", createdListingNote.getCourtRoomId().toString())
                .add("note", createdListingNote.getNoteDescription())
                .build();

        this.sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_NOTE_CREATED_LISTING_NOTE),
                listingNoteCreatedPublicEventPayload));
    }

    @Handles(LISTING_EVENTS_NOTE_EDITED)
    public void handleListingNoteEditedEvent(JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_NOTE_EDITED, jsonEnvelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom
                (metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_NOTE_EDITED),
                        jsonEnvelope.payloadAsJsonObject())
        );
    }

    @Handles(LISTING_EVENTS_NOTE_DELETED_LISTING_NOTE)
    public void handlesNoteDeletedForListing(JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_INFO_STRING, LISTING_EVENTS_NOTE_DELETED_LISTING_NOTE, jsonEnvelope.toObfuscatedDebugString());
        }
        this.sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_NOTE_DELETED_LISTING_NOTE),
                jsonEnvelope.payloadAsJsonObject()));
    }
}
