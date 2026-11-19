package uk.gov.moj.cpp.listing.messaging.outbox;

import static java.util.UUID.randomUUID;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.json.JsonObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional outbox writer for Spring services (MBD refactor P2.2; ADR 002, ADR 013).
 * Replaces the Phase 1 JEE {@code EventOutboxWriter}.
 *
 * <p>Command services call {@link #write} (or {@link #append}) at the end of their
 * {@code @Transactional} method: the rows commit atomically with the service's viewstore changes
 * ({@code MANDATORY}: there must be a caller transaction), and {@link OutboxRelay} publishes them
 * after commit. The insert is a plain {@code INSERT}; nothing is caught and retried inside the
 * caller's transaction (§11 pitfall 2).
 *
 * <p>ADR 015: the outbox carries <em>only</em> public messages ({@code public.*}), exactly the ones
 * the JEE event processor emitted. There are no private events in the Spring application; any
 * other name is rejected here, before it can be stored.
 *
 * <p>Metadata follows the framework convention for events raised while handling a command or an
 * event (as Phase 1): the cause's metadata (user, session, client correlation, …) with a new
 * {@code id}, the event {@code name}, a fresh {@code createdAt}, the cause's id appended to the
 * {@code causation} chain, and {@code stream.id} when the event has one.
 */
@Component
@Transactional(propagation = MANDATORY)
public class EventOutbox {

    static final String PUBLIC_PREFIX = "public.";

    private final EventOutboxJdbcRepository repository;
    private final EnvelopeCodec codec;
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    private final Clock clock;

    public EventOutbox(final EventOutboxJdbcRepository repository, final EnvelopeCodec codec, final Clock clock) {
        this.repository = repository;
        this.codec = codec;
        this.clock = clock;
    }

    /** Builds each event's envelope (caused by {@code causedBy}) and appends it; returns the envelopes. */
    public List<Envelope> write(final Metadata causedBy, final List<DomainEvent> events) {
        final List<Envelope> written = new ArrayList<>(events.size());
        for (final DomainEvent event : events) {
            final Envelope envelope = envelopeFor(causedBy, event);
            append(envelope);
            written.add(envelope);
        }
        return written;
    }

    /** Appends an envelope that is already complete (id, name, createdAt set). */
    public void append(final Envelope envelope) {
        final Metadata metadata = envelope.metadata();
        requirePublic(metadata.name());
        repository.append(metadata.id(), metadata.name(), metadata.streamId().orElse(null), codec.toJson(envelope),
                metadata.createdAt().orElseGet(this::now));
    }


    /** The envelope for {@code event}, caused by the command or event with metadata {@code causedBy}. */
    @Transactional(propagation = SUPPORTS)
    public Envelope envelopeFor(final Metadata causedBy, final DomainEvent event) {
        final List<UUID> causation = new ArrayList<>(causedBy.causation());
        causation.add(causedBy.id());

        final Metadata.Builder metadata = Metadata.from(causedBy)
                .withId(randomUUID())
                .withName(event.name())
                .createdAt(now())
                .withCausation(causation);
        if (event.streamId() != null) {
            metadata.withStreamId(event.streamId());
        }
        return new Envelope(metadata.build(), payloadOf(event.payload()));
    }

    /** ADR 015: only public messages ({@code public.*}) are ever stored in the outbox. */
    static void requirePublic(final String name) {
        if (!name.startsWith(PUBLIC_PREFIX)) {
            throw new IllegalArgumentException("Only public messages (public.*) go to the outbox (ADR 015), not " + name);
        }
    }

    private JsonObject payloadOf(final Object payload) {
        return payload instanceof JsonObject json ? json : objectToJsonObjectConverter.convert(payload);
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock);
    }
}
