package uk.gov.moj.cpp.listing.command.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.common.aggregate.AggregateStore;
import uk.gov.moj.cpp.listing.common.outbox.EventRecorder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * MBD refactor Phase 1 test support (ADR 011). Handlers of migrated aggregates call
 * {@link AggregateStore} / {@link EventRecorder} instead of the event store. This bridge makes the
 * mocks of those two behave like the old event-store mocks, so existing handler tests keep their
 * stubs ({@code eventSource.getStreamById}, {@code aggregateService.get}) and assertions
 * ({@code verifyAppendAndGetArgumentFrom(eventStream)}, call counts) unchanged:
 * <ul>
 *     <li>{@code aggregateStore.load(id, type)} returns {@code aggregateService.get(eventSource.getStreamById(id), type)};</li>
 *     <li>{@code aggregateStore.update(cause, id, type, command)} does the same lookup, runs the
 *     command and appends the raised events to that stream mock;</li>
 *     <li>{@code aggregateStore.update(cause, id, aggregate, command)} runs the command on the
 *     aggregate returned by an earlier {@code load} and appends to that load's stream (one
 *     {@code getStreamById} call per aggregate, as before);</li>
 *     <li>{@code eventRecorder.record(cause, id, events)} appends the events to
 *     {@code eventSource.getStreamById(id)}.</li>
 * </ul>
 * As in the old handler code, the event stream is passed to {@code append} unconsumed, enveloped
 * with the framework's {@code toEnvelopeWithMetadataFrom(cause)}; the {@code update} mocks return
 * an empty list. The production metadata ({@code EventOutboxWriter}) is tested separately.
 */
final class MigratedAggregateTestBridge {

    private MigratedAggregateTestBridge() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void bridge(final AggregateStore aggregateStore, final EventRecorder eventRecorder,
                       final EventSource eventSource, final AggregateService aggregateService) {
        final Map<UUID, EventStream> loadedStreams = new HashMap<>();

        lenient().when(aggregateStore.load(any(), any())).thenAnswer(invocation -> {
            final UUID id = invocation.getArgument(0);
            final EventStream stream = eventSource.getStreamById(id);
            if (stream != null) {
                loadedStreams.put(id, stream);
            }
            return aggregateService.get(stream, (Class) invocation.getArgument(1));
        });

        lenient().when(aggregateStore.update(any(), any(), any(Class.class), any())).thenAnswer(invocation -> {
            final EventStream stream = eventSource.getStreamById(invocation.getArgument(1));
            final Aggregate aggregate = (Aggregate) aggregateService.get(stream, (Class) invocation.getArgument(2));
            final Function<Aggregate, Stream<Object>> command = invocation.getArgument(3);
            append(stream, invocation.getArgument(0), command.apply(aggregate));
            return List.of();
        });

        lenient().when(aggregateStore.update(any(), any(), any(Aggregate.class), any())).thenAnswer(invocation -> {
            final UUID id = invocation.getArgument(1);
            final EventStream stream = loadedStreams.containsKey(id) ? loadedStreams.get(id) : eventSource.getStreamById(id);
            final Function<Aggregate, Stream<Object>> command = invocation.getArgument(3);
            append(stream, invocation.getArgument(0), command.apply(invocation.getArgument(2)));
            return List.of();
        });

        if (eventRecorder != null) {
            lenient().doAnswer(invocation -> {
                final List<Object> events = invocation.getArgument(2);
                append(eventSource.getStreamById(invocation.getArgument(1)), invocation.getArgument(0), events.stream());
                return null;
            }).when(eventRecorder).record(any(), any(), any());
        }
    }

    private static void append(final EventStream stream, final Envelope<?> cause, final Stream<Object> events) throws EventStreamException {
        if (stream != null && events != null) {
            stream.append(events.map(toEnvelopeWithMetadataFrom(cause)));
        }
    }
}
