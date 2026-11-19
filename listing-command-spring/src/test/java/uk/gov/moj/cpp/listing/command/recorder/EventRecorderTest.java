package uk.gov.moj.cpp.listing.command.recorder;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.CreatedListingNote;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.EnvelopeCodec;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;
import uk.gov.moj.cpp.listing.messaging.outbox.InProcessEventRoute;
import uk.gov.moj.cpp.listing.messaging.outbox.PrivateEventHandler;
import uk.gov.moj.cpp.listing.persistence.repository.outbox.EventOutboxJdbcRepository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class EventRecorderTest {

    private static final String PROJECTED = "listing.events.projected";
    private static final String PROCESSED = "listing.events.processed";
    private static final String BOTH = "listing.events.both";
    private static final String NEITHER = "listing.events.neither";

    private final EventOutboxJdbcRepository outboxRepository = mock(EventOutboxJdbcRepository.class);
    private final EventOutbox outbox = new EventOutbox(outboxRepository, new EnvelopeCodec(), Clock.systemUTC());
    private final RecordingProjection projection = new RecordingProjection(Set.of(PROJECTED, BOTH));
    private final InProcessEventRoute processors = new InProcessEventRoute(List.of(new Processor(Set.of(PROCESSED, BOTH))),
            mock(PlatformTransactionManager.class));
    private final EventRecorder recorder = new EventRecorder(outbox, new EventProjections(List.of(projection)), processors);

    private final UUID commandId = randomUUID();
    private final Metadata command = Metadata.builder().withId(commandId).withName("listing.command.x").withUserId("user-1").build();

    @Test
    void runsTheProjectionInlineAndWritesOnlyProcessorEventsToTheOutbox() {
        final UUID stream = randomUUID();

        final List<Envelope> recorded = recorder.record(command, List.of(
                event(PROJECTED, stream), event(PROCESSED, stream), event(BOTH, stream), event(NEITHER, stream)));

        assertThat(recorded).extracting(Envelope::name).containsExactly(PROJECTED, PROCESSED, BOTH, NEITHER);
        assertThat(projection.projected).extracting(Envelope::name).containsExactly(PROJECTED, BOTH);
        verify(outboxRepository).append(recorded.get(1).metadata().id(), PROCESSED, stream, new EnvelopeCodec().toJson(recorded.get(1)),
                recorded.get(1).metadata().createdAt().orElseThrow());
        verify(outboxRepository).append(org.mockito.ArgumentMatchers.eq(recorded.get(2).metadata().id()),
                org.mockito.ArgumentMatchers.eq(BOTH), any(), any(), any());
        verify(outboxRepository, never()).append(org.mockito.ArgumentMatchers.eq(recorded.get(0).metadata().id()), any(), any(), any(), any());
        verify(outboxRepository, never()).append(org.mockito.ArgumentMatchers.eq(recorded.get(3).metadata().id()), any(), any(), any(), any());
    }

    @Test
    void eventMetadataComesFromTheCommand() {
        final UUID stream = randomUUID();

        final Envelope envelope = recorder.record(command, List.of(event(PROJECTED, stream))).get(0);

        assertThat(envelope.metadata().causation()).containsExactly(commandId);
        assertThat(envelope.metadata().userId()).contains("user-1");
        assertThat(envelope.metadata().streamId()).contains(stream);
        assertThat(envelope.metadata().id()).isNotEqualTo(commandId);
        assertThat(envelope.metadata().createdAt()).isPresent();
    }

    @Test
    void aggregateEventsAreNamedByTheirEventAnnotation() {
        final UUID noteId = randomUUID();
        final CreatedListingNote created = new CreatedListingNote(randomUUID(), "2026-11-20", noteId, "a note");

        final Envelope envelope = recorder.recordEvents(command, noteId, List.of(created)).get(0);

        assertThat(envelope.name()).isEqualTo("listing.events.created-listing-note");
        assertThat(envelope.metadata().streamId()).contains(noteId);
        assertThat(envelope.payload().getString("noteDescription")).isEqualTo("a note");
        assertThat(envelope.payload().getString("id")).isEqualTo(noteId.toString());
    }

    @Test
    void aPojoWithoutAnEventAnnotationIsRejected() {
        assertThatThrownBy(() -> EventRecorder.nameOf("not an event")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aProjectionFailureReachesTheCaller() {
        final EventProjection failing = new EventProjection() {
            @Override
            public Set<String> handledEvents() {
                return Set.of(PROJECTED);
            }

            @Override
            public void project(final Envelope event) {
                throw new IllegalStateException("projection failed");
            }
        };
        final EventRecorder failingRecorder = new EventRecorder(outbox, new EventProjections(List.of(failing)), processors);

        assertThatThrownBy(() -> failingRecorder.record(command, List.of(event(PROJECTED, null))))
                .hasMessage("projection failed");
    }

    @Test
    void twoProjectionsForOneEventFailStartUp() {
        assertThatThrownBy(() -> new EventProjections(List.of(new RecordingProjection(Set.of(PROJECTED)), new RecordingProjection(Set.of(PROJECTED)))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PROJECTED);
    }

    @Test
    void projectionsAreLookedUpByName() {
        final EventProjections projections = new EventProjections(List.of(projection));

        assertThat(projections.forEvent(BOTH)).containsSame(projection);
        assertThat(projections.forEvent(NEITHER)).isEmpty();
        assertThat(projections.eventNames()).containsExactlyInAnyOrder(PROJECTED, BOTH);
    }

    private static DomainEvent event(final String name, final UUID stream) {
        return new DomainEvent(name, stream, Json.createObjectBuilder().add("name", name).build());
    }

    private static final class RecordingProjection implements EventProjection {
        private final Set<String> names;
        private final List<Envelope> projected = new ArrayList<>();

        private RecordingProjection(final Set<String> names) {
            this.names = names;
        }

        @Override
        public Set<String> handledEvents() {
            return names;
        }

        @Override
        public void project(final Envelope event) {
            projected.add(event);
        }
    }

    private record Processor(Set<String> handledEvents) implements PrivateEventHandler {
        @Override
        public void handle(final Envelope event) {
        }
    }
}
