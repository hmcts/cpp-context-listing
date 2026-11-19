package uk.gov.moj.cpp.listing.common.domain;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

/**
 * An event produced by a framework-free domain service (MBD refactor Phase 1, §5).
 *
 * <p>The service returns these; the JEE adapter that called it writes them to the event outbox
 * in the same transaction, with metadata taken from the originating command.
 *
 * @param name     the event name, e.g. {@code listing.events.created-listing-note}
 * @param streamId the id the event belongs to (the former aggregate stream id); may be null
 * @param payload  the event payload POJO, serialised as the event's JSON schema
 */
public record DomainEvent(String name, UUID streamId, Object payload) {

    public DomainEvent {
        requireNonNull(name, "name");
        requireNonNull(payload, "payload");
    }
}
