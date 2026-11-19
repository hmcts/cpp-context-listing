package uk.gov.moj.cpp.listing.event.processor;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.outbox.PrivateEventHandler;

import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Port of the JEE {@code ListingNoteEventProcessor} (MBD refactor P2.3.0, slice: listing notes).
 * Republishes the three private note events as their public events, with the same payloads:
 * <ul>
 *     <li>{@code listing.events.created-listing-note} → {@code public.listing.created-listing-note}
 *     with {@code id}, {@code date}, {@code courtRoomId}, {@code note} (renamed from the private
 *     payload, as before);</li>
 *     <li>{@code listing.events.listing-note-edited} → {@code public.listing.note-edited}, payload
 *     unchanged;</li>
 *     <li>{@code listing.events.deleted-listing-note} → {@code public.listing.deleted-listing-note},
 *     payload unchanged.</li>
 * </ul>
 * Runs after the command commits, from the outbox relay ({@link PrivateEventHandler}).
 */
@Component
public class ListingNoteEventProcessor implements PrivateEventHandler {

    static final String CREATED_LISTING_NOTE = "listing.events.created-listing-note";
    static final String PUBLIC_CREATED_LISTING_NOTE = "public.listing.created-listing-note";
    static final String LISTING_NOTE_EDITED = "listing.events.listing-note-edited";
    static final String PUBLIC_NOTE_EDITED = "public.listing.note-edited";
    static final String DELETED_LISTING_NOTE = "listing.events.deleted-listing-note";
    static final String PUBLIC_DELETED_LISTING_NOTE = "public.listing.deleted-listing-note";

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingNoteEventProcessor.class);

    private final ProcessorSender sender;

    public ListingNoteEventProcessor(final ProcessorSender sender) {
        this.sender = sender;
    }

    @Override
    public Set<String> handledEvents() {
        return Set.of(CREATED_LISTING_NOTE, LISTING_NOTE_EDITED, DELETED_LISTING_NOTE);
    }

    @Override
    public void handle(final Envelope event) {
        LOGGER.info("Received '{}' event {}", event.name(), event.metadata().id());
        switch (event.name()) {
            case CREATED_LISTING_NOTE -> sender.sendPublic(event, PUBLIC_CREATED_LISTING_NOTE, createdPayload(event.payload()));
            case LISTING_NOTE_EDITED -> sender.sendPublic(event, PUBLIC_NOTE_EDITED, event.payload());
            case DELETED_LISTING_NOTE -> sender.sendPublic(event, PUBLIC_DELETED_LISTING_NOTE, event.payload());
            default -> throw new IllegalArgumentException("Unexpected event " + event.name());
        }
    }

    /** As the legacy handler: read through the {@code CreatedListingNote} fields, renamed. */
    private static JsonObject createdPayload(final JsonObject created) {
        return Json.createObjectBuilder()
                .add("id", created.getString("id"))
                .add("date", created.getString("hearingDate"))
                .add("courtRoomId", created.getString("courtRoomId"))
                .add("note", created.getString("noteDescription"))
                .build();
    }
}
