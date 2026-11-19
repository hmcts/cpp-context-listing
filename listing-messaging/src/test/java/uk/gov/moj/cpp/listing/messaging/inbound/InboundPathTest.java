package uk.gov.moj.cpp.listing.messaging.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeFormatException;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.inbound.InboundEventDedupJdbcRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.jms.BytesMessage;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;

class InboundPathTest {

    private static final String CASE_LINKED = "public.progression.case-linked";
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private final EnvelopeCodec codec = new EnvelopeCodec();
    private final PublicEventSubscription subscription = new PublicEventSubscription();
    private final InboundEventDedupJdbcRepository dedup = mock(InboundEventDedupJdbcRepository.class);

    @Test
    void firstDeliveryIsRecordedThenDispatchedByMetadataName() {
        final RecordingHandler handler = new RecordingHandler(Set.of(CASE_LINKED));
        final InboundEventProcessor processor = processor(handler);
        final Envelope envelope = envelope(CASE_LINKED);
        when(dedup.recordIfAbsent(any(), anyString(), any())).thenReturn(true);

        processor.process(codec.toJson(envelope), CASE_LINKED);

        verify(dedup).recordIfAbsent(envelope.metadata().id(), CASE_LINKED, ZonedDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(handler.received).containsExactly(envelope);
    }

    @Test
    void aDuplicateIsAcknowledgedWithoutDispatch() {
        final RecordingHandler handler = new RecordingHandler(Set.of(CASE_LINKED));
        when(dedup.recordIfAbsent(any(), anyString(), any())).thenReturn(false);

        processor(handler).process(codec.toJson(envelope(CASE_LINKED)), CASE_LINKED);

        assertThat(handler.received).isEmpty();
    }

    @Test
    void aSubscribedEventWithoutAHandlerIsRecordedAndConsumed() {
        when(dedup.recordIfAbsent(any(), anyString(), any())).thenReturn(true);

        processor().process(codec.toJson(envelope("public.hearing.trial-vacated")), "public.hearing.trial-vacated");

        verify(dedup).recordIfAbsent(any(), eq("public.hearing.trial-vacated"), any());
    }

    @Test
    void aBodyThatIsNotAnEnvelopeThrowsBeforeAnythingIsRecorded() {
        assertThatThrownBy(() -> processor().process("{\"no\":\"metadata\"}", CASE_LINKED))
                .isInstanceOf(EnvelopeFormatException.class);
        verify(dedup, never()).recordIfAbsent(any(), anyString(), any());
    }

    @Test
    void handlerFailurePropagatesSoTheTransactionAndSessionRollBack() {
        final InboundEventHandler failing = new InboundEventHandler() {
            @Override
            public Set<String> handledEvents() {
                return Set.of(CASE_LINKED);
            }

            @Override
            public void handle(final Envelope event) {
                throw new IllegalStateException("boom");
            }
        };
        when(dedup.recordIfAbsent(any(), anyString(), any())).thenReturn(true);

        assertThatThrownBy(() -> processor(failing).process(codec.toJson(envelope(CASE_LINKED)), CASE_LINKED))
                .isInstanceOf(IllegalStateException.class).hasMessage("boom");
    }

    @Test
    void handlersForUnsubscribedOrDuplicateNamesFailAtStartUp() {
        assertThatThrownBy(() -> new InboundEventDispatcher(List.of(new RecordingHandler(Set.of("public.listing.not-subscribed"))), subscription))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("does not select");
        assertThatThrownBy(() -> new InboundEventDispatcher(
                List.of(new RecordingHandler(Set.of(CASE_LINKED)), new RecordingHandler(Set.of(CASE_LINKED))), subscription))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Two inbound event handlers");
    }

    @Test
    void listenerAcceptsOnlyTextMessages() throws Exception {
        final InboundEventProcessor processor = mock(InboundEventProcessor.class);
        final PublicEventListener listener = new PublicEventListener(processor);
        final TextMessage text = mock(TextMessage.class);
        when(text.getText()).thenReturn("{}");
        when(text.getStringProperty("CPPNAME")).thenReturn(CASE_LINKED);

        listener.onMessage(text);
        verify(processor).process("{}", CASE_LINKED);

        assertThatThrownBy(() -> listener.onMessage(mock(BytesMessage.class)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not an instance of TextMessage");
    }

    private InboundEventProcessor processor(final InboundEventHandler... handlers) {
        return new InboundEventProcessor(codec, dedup, new InboundEventDispatcher(List.of(handlers), subscription),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Envelope envelope(final String name) {
        return new Envelope(Metadata.builder().withId(UUID.randomUUID()).withName(name).withUserId("u").build(),
                Json.createObjectBuilder().add("caseId", "c").build());
    }

    private static final class RecordingHandler implements InboundEventHandler {
        private final Set<String> names;
        private final List<Envelope> received = new ArrayList<>();

        private RecordingHandler(final Set<String> names) {
            this.names = names;
        }

        @Override
        public Set<String> handledEvents() {
            return names;
        }

        @Override
        public void handle(final Envelope event) {
            received.add(event);
        }
    }
}
