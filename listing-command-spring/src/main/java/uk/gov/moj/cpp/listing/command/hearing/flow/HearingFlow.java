package uk.gov.moj.cpp.listing.command.hearing.flow;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.command.hearing.rules.ApplicationRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.CaseRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;
import uk.gov.moj.cpp.listing.command.hearing.state.HearingStateStore;
import uk.gov.moj.cpp.listing.command.hearing.state.RulesStateStore;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;

/**
 * The JEE handler's {@code updateHearingEventStream(command, hearingId, hearing -> events)} without
 * an event stream (MBD refactor P2.3.2, ADR 015), in the caller's transaction:
 * <ol>
 *     <li>load the hearing's state (viewstore + {@code hearing_rules_state});</li>
 *     <li>run the ported rules; a rejected command raises nothing (and changes nothing);</li>
 *     <li>keep the command-side state;</li>
 *     <li>for each raised event, in order, the viewstore change the JEE listener made;</li>
 *     <li>queue each event's processor step (public messages, follow-up commands), run after the
 *     command and in JEE relay order by {@link ProcessorQueue}.</li>
 * </ol>
 * The raised events are never stored or published.
 *
 * <p>P2.3.3: the same for the Case, Application and SeedHearing rules ({@code update<X>}), whose
 * state is only the changeset 034 document ({@link RulesStateStore}); it is kept only when the rules
 * raised something (every state change of those aggregates is an event).
 */
public class HearingFlow {

    private final HearingStateStore stateStore;
    private final RulesStateStore rulesStateStore;
    private final ObjectProvider<ListenerPort> listenerPorts;
    private final ObjectProvider<ProcessorPort> processorPorts;
    private final ProcessorQueue processorQueue;
    private final Clock clock;
    private volatile EventHandlers listeners;
    private volatile EventHandlers processors;

    public HearingFlow(final HearingStateStore stateStore, final ObjectProvider<ListenerPort> listenerPorts,
                       final ObjectProvider<ProcessorPort> processorPorts, final ProcessorQueue processorQueue, final Clock clock) {
        this(stateStore, null, listenerPorts, processorPorts, processorQueue, clock);
    }

    public HearingFlow(final HearingStateStore stateStore, final RulesStateStore rulesStateStore, final ObjectProvider<ListenerPort> listenerPorts,
                       final ObjectProvider<ProcessorPort> processorPorts, final ProcessorQueue processorQueue, final Clock clock) {
        this.stateStore = stateStore;
        this.rulesStateStore = rulesStateStore;
        this.listenerPorts = listenerPorts;
        this.processorPorts = processorPorts;
        this.processorQueue = processorQueue;
        this.clock = clock;
    }

    /** The hearing's rules in their current state, for a read-only check. */
    public HearingRules load(final UUID hearingId) {
        return stateStore.load(hearingId);
    }

    /** {@code updateHearingEventStream}: decide on the hearing's current state, then apply and process what was raised. */
    public List<Object> update(final Metadata command, final UUID hearingId, final Function<HearingRules, Stream<Object>> decision) {
        final HearingRules rules = stateStore.load(hearingId);
        final List<Object> events = decision.apply(rules).toList();
        stateStore.save(hearingId, rules);
        raise(command, hearingId, events);
        return events;
    }

    /** The case's rules in their current state, for a read-only check ({@code aggregateStore.load(caseId, Case.class)}). */
    public CaseRules loadCase(final UUID caseId) {
        return rulesStateStore.loadCase(caseId);
    }

    /** {@code updateCaseEventStream}: the Case rules (stream: the case id). */
    public List<Object> updateCase(final Metadata command, final UUID caseId, final Function<CaseRules, Stream<Object>> decision) {
        final CaseRules rules = rulesStateStore.loadCase(caseId);
        final List<Object> events = decision.apply(rules).toList();
        if (!events.isEmpty()) {
            rulesStateStore.saveCase(rules);
        }
        raise(command, caseId, events);
        return events;
    }

    /** {@code updateApplicationEventStream}: the Application rules (stream: the application id). */
    public List<Object> updateApplication(final Metadata command, final UUID applicationId, final Function<ApplicationRules, Stream<Object>> decision) {
        final ApplicationRules rules = rulesStateStore.loadApplication(applicationId);
        final List<Object> events = decision.apply(rules).toList();
        if (!events.isEmpty()) {
            rulesStateStore.saveApplication(rules);
        }
        raise(command, applicationId, events);
        return events;
    }

    /** {@code updateSeedAggregateEventStream}: the SeedHearing rules (stream: the seeding hearing id). */
    public List<Object> updateSeedHearing(final Metadata command, final UUID seedingHearingId, final Function<SeedHearingRules, Stream<Object>> decision) {
        final SeedHearingRules rules = rulesStateStore.loadSeedHearing(seedingHearingId);
        final List<Object> events = decision.apply(rules).toList();
        if (!events.isEmpty()) {
            rulesStateStore.saveSeedHearing(rules);
        }
        raise(command, seedingHearingId, events);
        return events;
    }

    /** Events built outside the rules (the JEE handler appended them to the hearing's stream itself). */
    public void raise(final Metadata command, final UUID streamId, final List<Object> events) {
        final List<RaisedEvent<?>> raised = new ArrayList<>(events.size());
        for (final Object event : events) {
            raised.add(new RaisedEvent<>(eventMetadata(command, PrivateEventNames.nameOf(event), streamId), event));
        }
        raised.forEach(listeners()::dispatch);
        raised.forEach(event -> processorQueue.enqueue(() -> processors().dispatch(event)));
    }

    /** What the framework gave an event appended while handling {@code command} ({@code toEnvelopeWithMetadataFrom}). */
    private Metadata eventMetadata(final Metadata command, final String name, final UUID streamId) {
        final List<UUID> causation = new ArrayList<>(command.causation());
        causation.add(command.id());
        final Metadata.Builder metadata = Metadata.from(command)
                .withId(randomUUID())
                .withName(name)
                .createdAt(ZonedDateTime.now(clock))
                .withCausation(causation);
        if (streamId != null) {
            metadata.withStreamId(streamId);
        }
        return metadata.build();
    }

    private EventHandlers listeners() {
        EventHandlers result = listeners;
        if (result == null) {
            result = new EventHandlers();
            final EventHandlers registry = result;
            listenerPorts.orderedStream().forEach(port -> port.register(registry));
            listeners = result;
        }
        return result;
    }

    private EventHandlers processors() {
        EventHandlers result = processors;
        if (result == null) {
            result = new EventHandlers();
            final EventHandlers registry = result;
            processorPorts.orderedStream().forEach(port -> port.register(registry));
            processors = result;
        }
        return result;
    }
}
