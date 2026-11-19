package uk.gov.moj.cpp.listing.event.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;

import java.util.UUID;

import jakarta.json.JsonObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces the framework {@code Sender} for the ported event-processor handlers (MBD refactor P2.3,
 * ADR 014). Runs inside the handler's transaction ({@code MANDATORY}; the outbox relay opens it).
 *
 * <p>{@link #sendPublic} is the port of
 * {@code sender.send(envelopeFrom(metadataFrom(event.metadata()).withName("public.…"), payload))}:
 * the public event keeps the private event's metadata (user, session, client correlation,
 * causation, {@code stream.id}, {@code createdAt}) with the public name, and goes to the outbox,
 * from where the relay publishes it to {@code public.event} after this transaction commits
 * (ADR 013). One difference, invisible on the wire: the framework kept the private event's
 * {@code _metadata.id}; here the id is derived from it ({@link #derivedId}), because the private
 * event's own outbox row still holds that id ({@code event_outbox.event_id} is unique) until the
 * relay deletes it. The derived id is stable, so a redelivered private event cannot append its
 * public event twice ({@link EventOutbox#appendIfAbsent}).
 *
 * <p>Internal commands the processor sent ({@code listing.command.*}) are not sent through here:
 * the ported handlers call the command services directly.
 */
@Component
@Transactional(propagation = MANDATORY)
public class ProcessorSender {

    private final EventOutbox eventOutbox;

    public ProcessorSender(final EventOutbox eventOutbox) {
        this.eventOutbox = eventOutbox;
    }

    /**
     * Writes the public event {@code publicName} with {@code payload}, caused by {@code event}.
     *
     * @return the public envelope (appended, or already in the outbox from an earlier delivery)
     */
    public Envelope sendPublic(final Envelope event, final String publicName, final JsonObject payload) {
        if (!publicName.startsWith("public.")) {
            throw new IllegalArgumentException("Not a public event name: " + publicName);
        }
        final Metadata metadata = Metadata.from(event.metadata())
                .withId(derivedId(event.metadata().id(), publicName))
                .withName(publicName)
                .build();
        final Envelope publicEvent = new Envelope(metadata, payload);
        eventOutbox.appendIfAbsent(publicEvent);
        return publicEvent;
    }

    /** A name-based (type 3) UUID from the causing event's id and the public event's name. */
    static UUID derivedId(final UUID causeId, final String publicName) {
        return UUID.nameUUIDFromBytes((causeId + "/" + publicName).getBytes(UTF_8));
    }
}
