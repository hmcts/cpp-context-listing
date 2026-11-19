package uk.gov.moj.cpp.listing.command.api.outbox;

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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JEE adapter (MBD refactor Phase 1, ADR 2): turns {@link DomainEvent}s into framework event
 * envelopes and appends them to {@code event_outbox}. Must be called inside the transaction that
 * made the matching viewstore changes; {@link EventOutboxRelay} publishes them after commit.
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

    public void write(final Metadata causedBy, final List<DomainEvent> events) {
        for (final DomainEvent event : events) {
            final UUID eventId = randomUUID();
            final ZonedDateTime createdAt = clock.now();
            final JsonEnvelope envelope = envelopeFrom(
                    metadataFor(causedBy, event, eventId, createdAt),
                    objectToJsonObjectConverter.convert(event.payload()));

            eventOutboxJdbcRepository.append(eventId, event.name(), event.streamId(),
                    jsonObjectEnvelopeConverter.asJsonString(envelope), createdAt);
        }
    }

    private static MetadataBuilder metadataFor(final Metadata causedBy, final DomainEvent event,
                                               final UUID eventId, final ZonedDateTime createdAt) {
        final List<UUID> causation = new ArrayList<>(causedBy.causation());
        causation.add(causedBy.id());

        final MetadataBuilder builder = metadataFrom(causedBy)
                .withId(eventId)
                .withName(event.name())
                .createdAt(createdAt)
                .withCausation(causation.toArray(new UUID[0]));
        if (event.streamId() != null) {
            builder.withStreamId(event.streamId());
        }
        return builder;
    }
}
