package uk.gov.moj.cpp.listing.common.domain;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

/**
 * An event produced by a framework-free domain service (MBD refactor §5).
 *
 * <p>Copied unchanged from the Maven {@code listing-common} module (Phase 1, same package, so P2.3
 * moves the services without renames). The Spring {@code EventOutbox} writes it with metadata
 * taken from the originating command or inbound event.
 *
 * @param name     the event name, e.g. {@code public.listing.created-listing-note}
 * @param streamId the id the event belongs to (the former aggregate stream id); may be null
 * @param payload  the event payload: a POJO (serialised with the framework-equivalent mapper) or a
 *                 {@code jakarta.json.JsonObject}
 */
public record DomainEvent(String name, UUID streamId, Object payload) {

    public DomainEvent {
        requireNonNull(name, "name");
        requireNonNull(payload, "payload");
    }
}
