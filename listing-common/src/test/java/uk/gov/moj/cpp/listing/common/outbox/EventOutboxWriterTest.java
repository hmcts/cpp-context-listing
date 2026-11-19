package uk.gov.moj.cpp.listing.common.outbox;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.listing.events.ListingNoteEdited;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventOutboxWriterTest {

    private static final ZonedDateTime NOW = new UtcClock().now();

    @Mock
    private EventOutboxJdbcRepository eventOutboxJdbcRepository;

    @Mock
    private UtcClock clock;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter = jsonObjectEnvelopeConverter();

    @InjectMocks
    private EventOutboxWriter writer;

    @Test
    void writesEachEventAsAnEnvelopeCausedByTheCommand() {
        when(clock.now()).thenReturn(NOW);
        final UUID earlierCause = randomUUID();
        final UUID commandId = randomUUID();
        final Metadata command = metadataBuilder()
                .withId(commandId)
                .withName("listing.command.edit-listing-note")
                .withCausation(earlierCause)
                .withUserId("user-1")
                .withClientCorrelationId("correlation-1")
                .build();
        final UUID noteId = randomUUID();

        writer.write(command, List.of(new DomainEvent("listing.events.listing-note-edited", noteId,
                new ListingNoteEdited("edited", noteId))));

        final ArgumentCaptor<UUID> eventId = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(eventOutboxJdbcRepository).append(eventId.capture(), eq("listing.events.listing-note-edited"),
                eq(noteId), json.capture(), eq(NOW));

        final JsonEnvelope envelope = jsonObjectEnvelopeConverter.asEnvelope(json.getValue());
        final Metadata metadata = envelope.metadata();
        assertThat(metadata.id(), is(eventId.getValue()));
        assertThat(metadata.id(), is(not(commandId)));
        assertThat(metadata.name(), is("listing.events.listing-note-edited"));
        assertThat(metadata.streamId().orElseThrow(), is(noteId));
        assertThat(metadata.causation(), contains(earlierCause, commandId));
        assertThat(metadata.userId().orElseThrow(), is("user-1"));
        assertThat(metadata.clientCorrelationId().orElseThrow(), is("correlation-1"));
        assertThat(envelope.payloadAsJsonObject().getString("noteId"), is(noteId.toString()));
        assertThat(envelope.payloadAsJsonObject().getString("noteDescription"), is("edited"));
    }

    private static JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter() {
        final DefaultJsonObjectEnvelopeConverter converter = new DefaultJsonObjectEnvelopeConverter();
        setField(converter, "objectMapper", new ObjectMapperProducer().objectMapper());
        return converter;
    }

    @Test
    void writesNothingWhenThereAreNoEvents() {
        writer.write(metadataBuilder().withId(randomUUID()).withName("any").build(), List.of());

        verify(eventOutboxJdbcRepository, never()).append(any(), any(), any(), any(), any());
    }
}
