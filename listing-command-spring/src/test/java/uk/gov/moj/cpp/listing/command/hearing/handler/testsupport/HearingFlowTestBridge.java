package uk.gov.moj.cpp.listing.command.hearing.handler.testsupport;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.command.hearing.flow.PrivateEventNames;
import uk.gov.moj.cpp.listing.command.hearing.rules.ApplicationRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.CaseRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The ported JEE handler tests stubbed the event store ({@code eventSource.getStreamById},
 * {@code aggregateService.get}) and asserted on what was appended. This bridge makes a mocked
 * {@link HearingFlow} behave the same way (as the Phase 1 {@code MigratedAggregateTestBridge} did
 * for {@code AggregateStore}):
 * <ul>
 *     <li>{@code load(id)} → {@code aggregateService.get(eventSource.getStreamById(id), HearingRules.class)};</li>
 *     <li>{@code update(metadata, id, rules -> events)} → the same lookup, runs the decision and
 *     appends the raised events (as JSON envelopes named as the JEE events) to that stream mock;</li>
 *     <li>{@code raise(metadata, id, events)} → appends to {@code eventSource.getStreamById(id)};</li>
 *     <li>P2.3.3: {@code loadCase} / {@code updateCase}, {@code updateApplication} and
 *     {@code updateSeedHearing} likewise, with {@code aggregateService.get(stream, CaseRules.class)}
 *     (resp. {@code ApplicationRules}, {@code SeedHearingRules}).</li>
 * </ul>
 * The events are appended unconsumed (lazily mapped), as the JEE handler passed its stream.
 */
public final class HearingFlowTestBridge {

    private static final ObjectToJsonObjectConverter TO_JSON = new ObjectToJsonObjectConverter();

    private HearingFlowTestBridge() {
    }

    @SuppressWarnings("unchecked")
    public static void bridge(final HearingFlow hearingFlow, final EventSource eventSource, final AggregateService aggregateService) {
        lenient().when(hearingFlow.load(any())).thenAnswer(invocation ->
                aggregateService.get(eventSource.getStreamById(invocation.getArgument(0)), HearingRules.class));

        lenient().when(hearingFlow.update(any(), any(), any())).thenAnswer(invocation -> {
            final Metadata command = invocation.getArgument(0);
            final EventStream stream = eventSource.getStreamById(invocation.getArgument(1));
            final HearingRules rules = aggregateService.get(stream, HearingRules.class);
            final Function<HearingRules, Stream<Object>> decision = invocation.getArgument(2);
            final Stream<Object> events = decision.apply(rules);
            if (stream != null && events != null) {
                stream.append(events.map(event -> toEnvelope(command, invocation.getArgument(1), event)));
            }
            return List.of();
        });

        lenient().when(hearingFlow.loadCase(any())).thenAnswer(invocation ->
                aggregateService.get(eventSource.getStreamById(invocation.getArgument(0)), CaseRules.class));
        lenient().when(hearingFlow.updateCase(any(), any(), any())).thenAnswer(invocation -> decide(invocation.getArgument(0), invocation.getArgument(1),
                invocation.getArgument(2), CaseRules.class, eventSource, aggregateService));
        lenient().when(hearingFlow.updateApplication(any(), any(), any())).thenAnswer(invocation -> decide(invocation.getArgument(0), invocation.getArgument(1),
                invocation.getArgument(2), ApplicationRules.class, eventSource, aggregateService));
        lenient().when(hearingFlow.updateSeedHearing(any(), any(), any())).thenAnswer(invocation -> decide(invocation.getArgument(0), invocation.getArgument(1),
                invocation.getArgument(2), SeedHearingRules.class, eventSource, aggregateService));

        lenient().doAnswer(invocation -> {
            final Metadata command = invocation.getArgument(0);
            final UUID streamId = invocation.getArgument(1);
            final List<Object> events = invocation.getArgument(2);
            final EventStream stream = eventSource.getStreamById(streamId);
            if (stream != null) {
                stream.append(new ArrayList<>(events).stream().map(event -> toEnvelope(command, streamId, event)));
            }
            return null;
        }).when(hearingFlow).raise(any(), any(), any());
    }

    private static <R> List<Object> decide(final Metadata command, final UUID id, final Function<R, Stream<Object>> decision, final Class<R> type,
                                           final EventSource eventSource, final AggregateService aggregateService) {
        final EventStream stream = eventSource.getStreamById(id);
        final R rules = aggregateService.get(stream, type);
        final Stream<Object> events = decision.apply(rules);
        if (stream != null && events != null) {
            stream.append(events.map(event -> toEnvelope(command, id, event)));
        }
        return List.of();
    }

    /** What the framework's {@code toEnvelopeWithMetadataFrom(command)} gave an event. */
    public static Envelope toEnvelope(final Metadata command, final UUID streamId, final Object event) {
        final List<UUID> causation = new ArrayList<>(command.causation());
        causation.add(command.id());
        final Metadata.Builder metadata = Metadata.from(command).withId(randomUUID()).withName(nameOf(event)).withCausation(causation);
        if (streamId != null) {
            metadata.withStreamId(streamId);
        }
        return new Envelope(metadata.build(), TO_JSON.convert(event));
    }

    private static String nameOf(final Object event) {
        try {
            return PrivateEventNames.nameOf(event);
        } catch (final IllegalArgumentException e) {
            return event.getClass().getName();
        }
    }
}
