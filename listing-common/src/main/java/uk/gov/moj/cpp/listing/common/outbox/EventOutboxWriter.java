package uk.gov.moj.cpp.listing.common.outbox;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JEE adapter (MBD refactor Phase 1, ADR 002): turns {@link DomainEvent}s into framework event
 * envelopes and appends them to {@code event_outbox}. Must be called inside the transaction that
 * made the matching viewstore changes; {@link EventOutboxRelay} delivers them after commit.
 *
 * <p>Metadata follows the framework's convention for events appended from a command: the
 * command's metadata (user, session, client correlation, ...) with a new id, the event name, the
 * stream id, a fresh {@code createdAt}, and the command id added to the causation chain.
 */
@ApplicationScoped
public class EventOutboxWriter {

    @Inject
    private EventOutboxJdbcRepository eventOutboxJdbcRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter;

    @Inject
    private UtcClock clock;

    /** Builds each event's envelope and appends it to the outbox. */
    public void write(final Metadata causedBy, final List<DomainEvent> events) {
        events.forEach(event -> append(envelopeFor(causedBy, event)));
    }

    /** The framework envelope for {@code event}, caused by the command with metadata {@code causedBy}. */
    public JsonEnvelope envelopeFor(final Metadata causedBy, final DomainEvent event) {
        return envelopeFrom(metadataFor(causedBy, event), objectToJsonObjectConverter.convert(event.payload()));
    }

    public void append(final JsonEnvelope event) {
        final Metadata metadata = event.metadata();
        eventOutboxJdbcRepository.append(metadata.id(), metadata.name(), metadata.streamId().orElse(null),
                jsonObjectEnvelopeConverter.asJsonString(event), metadata.createdAt().orElseGet(clock::now));
    }

    private MetadataBuilder metadataFor(final Metadata causedBy, final DomainEvent event) {
        final List<UUID> causation = new ArrayList<>(causedBy.causation());
        causation.add(causedBy.id());

        final MetadataBuilder builder = metadataFrom(causedBy)
                .withId(randomUUID())
                .withName(event.name())
                .createdAt(clock.now())
                .withCausation(causation.toArray(new UUID[0]));
        if (event.streamId() != null) {
            builder.withStreamId(event.streamId());
        }
        return builder;
    }
}
