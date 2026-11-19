package uk.gov.moj.cpp.listing.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EventOutboxTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00.123Z");
    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EARLIER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID STREAM = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final EventOutboxJdbcRepository repository = mock(EventOutboxJdbcRepository.class);
    private final EnvelopeCodec codec = new EnvelopeCodec();
    private final EventOutbox outbox = new EventOutbox(repository, codec, Clock.fixed(NOW, ZoneOffset.UTC));

    private final Metadata command = Metadata.builder()
            .withId(COMMAND_ID)
            .withName("listing.command.create-listing-note")
            .withUserId("user-1")
            .withSessionId("session-1")
            .withClientCorrelationId("client-1")
            .withCausation(List.of(EARLIER))
            .build();

    @Test
    void eventMetadataFollowsTheFrameworkConventionForEventsCausedByACommand() {
        final Envelope event = outbox.envelopeFor(command, new DomainEvent("public.listing.x", STREAM,
                Json.createObjectBuilder().add("a", 1).build()));

        final Metadata metadata = event.metadata();
        assertThat(metadata.id()).isNotEqualTo(COMMAND_ID);
        assertThat(metadata.name()).isEqualTo("public.listing.x");
        assertThat(metadata.createdAt()).contains(ZonedDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(metadata.causation()).containsExactly(EARLIER, COMMAND_ID);
        assertThat(metadata.userId()).contains("user-1");
        assertThat(metadata.sessionId()).contains("session-1");
        assertThat(metadata.clientCorrelationId()).contains("client-1");
        assertThat(metadata.streamId()).contains(STREAM);
        assertThat(event.payload().getInt("a")).isEqualTo(1);
    }

    @Test
    void pojoPayloadsAreSerialisedWithTheFrameworkEquivalentMapper() {
        final Envelope event = outbox.envelopeFor(command, new DomainEvent("public.listing.x", null,
                new Payload("v", ZonedDateTime.ofInstant(NOW, ZoneOffset.UTC), null)));

        assertThat(event.metadata().streamId()).isEmpty();
        assertThat(event.payload().getString("value")).isEqualTo("v");
        assertThat(event.payload().getString("at")).isEqualTo("2026-11-19T12:00:00.123Z");
        assertThat(event.payload().containsKey("absent")).isFalse();
    }

    @Test
    void writeAppendsEachEventsEnvelopeToTheOutbox() {
        final List<Envelope> written = outbox.write(command, List.of(
                new DomainEvent("public.listing.one", STREAM, Json.createObjectBuilder().build()),
                new DomainEvent("listing.events.two", null, Json.createObjectBuilder().build())));

        assertThat(written).hasSize(2);
        final ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(repository).append(eq(written.get(0).metadata().id()), eq("public.listing.one"), eq(STREAM), json.capture(),
                eq(ZonedDateTime.ofInstant(NOW, ZoneOffset.UTC)));
        verify(repository).append(eq(written.get(1).metadata().id()), eq("listing.events.two"), isNull(), anyString(), any());
        assertThat(codec.parse(json.getValue())).isEqualTo(written.get(0));
    }

    @Test
    void appendIfAbsentPassesTheEnvelopeAndReportsWhetherItWasInserted() {
        final Envelope envelope = outbox.envelopeFor(command, new DomainEvent("public.listing.x", STREAM, Json.createObjectBuilder().build()));
        when(repository.appendIfAbsent(eq(envelope.metadata().id()), eq("public.listing.x"), eq(STREAM), anyString(), any()))
                .thenReturn(true, false);

        assertThat(outbox.appendIfAbsent(envelope)).isTrue();
        assertThat(outbox.appendIfAbsent(envelope)).isFalse();
    }

    public record Payload(String value, ZonedDateTime at, String absent) {
    }
}
