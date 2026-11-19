package uk.gov.moj.cpp.listing.command.hearing.flow;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.command.hearing.state.HearingStateStore;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** The JEE handler → listener → processor order without an event stream (ADR 015, P2.3.2). */
class HearingFlowTest {

    private final HearingStateStore stateStore = mock(HearingStateStore.class);
    private final List<String> calls = new ArrayList<>();
    private final ProcessorQueue processorQueue = new ProcessorQueue();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);
    private final UUID hearingId = randomUUID();
    private final Metadata command = Metadata.builder().withId(randomUUID()).withName("listing.command.x").withUserId("user")
            .withCausation(List.of(randomUUID())).build();

    @Test
    void appliesEveryListenerChangeBeforeTheProcessorStepsInRaisedOrder() {
        final HearingFlow flow = flow();
        final HearingRules rules = new HearingRules();
        when(stateStore.load(hearingId)).thenReturn(rules);

        processorQueue.command(() -> {
            flow.update(command, hearingId, r -> Stream.of(trialVacated(), rescheduled()));
            calls.add("command done");
        });

        assertThat(calls).containsExactly(
                "listener TrialVacated", "listener HearingRescheduled", "command done",
                "processor TrialVacated", "processor HearingRescheduled");
        verify(stateStore).save(hearingId, rules);
    }

    @Test
    void aFollowUpCommandRunsInsideTheStepAndItsStepsQueueBehindTheOthers() {
        final HearingFlow[] flow = new HearingFlow[1];
        flow[0] = flow(processors -> processors
                .on(TrialVacated.class, e -> {
                    calls.add("processor TrialVacated");
                    processorQueue.command(() -> flow[0].raise(command, hearingId, List.of(rescheduled())));
                    calls.add("follow-up command done");
                })
                .on(HearingRescheduled.class, e -> calls.add("processor HearingRescheduled")));

        processorQueue.command(() -> flow[0].raise(command, hearingId, List.of(trialVacated(), rescheduled())));

        assertThat(calls).containsExactly(
                "listener TrialVacated", "listener HearingRescheduled",
                "processor TrialVacated", "listener HearingRescheduled", "follow-up command done",
                "processor HearingRescheduled", "processor HearingRescheduled");
    }

    @Test
    void aRejectedCommandRaisesNothing() {
        final HearingFlow flow = flow();
        when(stateStore.load(hearingId)).thenReturn(new HearingRules());

        processorQueue.command(() -> flow.update(command, hearingId, r -> Stream.empty()));

        assertThat(calls).isEmpty();
    }

    @Test
    void theRaisedEventCarriesTheCommandMetadataAsTheFrameworkDid() {
        final List<RaisedEvent<?>> seen = new ArrayList<>();
        final HearingFlow flow = new HearingFlow(stateStore, provider((ListenerPort) listeners -> listeners.on(TrialVacated.class, seen::add)),
                provider((ProcessorPort) processors -> { }), processorQueue, clock);

        flow.raise(command, hearingId, List.of(trialVacated()));

        final Metadata metadata = seen.get(0).metadata();
        assertThat(metadata.name()).isEqualTo("listing.events.trial-vacated");
        assertThat(metadata.id()).isNotEqualTo(command.id());
        assertThat(metadata.streamId()).contains(hearingId);
        assertThat(metadata.userId()).contains("user");
        assertThat(metadata.causation()).containsExactly(command.causation().get(0), command.id());
        assertThat(metadata.createdAt()).isPresent();
        verify(stateStore, never()).save(any());
        verify(stateStore, never()).save(any(), any());
    }

    private HearingFlow flow() {
        return flow(processors -> processors
                .on(TrialVacated.class, e -> calls.add("processor TrialVacated"))
                .on(HearingRescheduled.class, e -> calls.add("processor HearingRescheduled")));
    }

    private HearingFlow flow(final ProcessorPort processorPort) {
        final ListenerPort listenerPort = listeners -> listeners
                .on(TrialVacated.class, e -> calls.add("listener TrialVacated"))
                .on(HearingRescheduled.class, e -> calls.add("listener HearingRescheduled"));
        return new HearingFlow(stateStore, provider(listenerPort), provider(processorPort), processorQueue, clock);
    }

    @SuppressWarnings("unchecked")
    private static <T> org.springframework.beans.factory.ObjectProvider<T> provider(final T bean) {
        final StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("bean", bean);
        final Class<?> type = bean instanceof ListenerPort ? ListenerPort.class : ProcessorPort.class;
        return (org.springframework.beans.factory.ObjectProvider<T>) (Object) factory.getBeanProvider((Class<Object>) type);
    }

    private TrialVacated trialVacated() {
        return TrialVacated.trialVacated().withHearingId(hearingId).withVacatedTrialReasonId(randomUUID()).build();
    }

    private HearingRescheduled rescheduled() {
        return HearingRescheduled.hearingRescheduled().withHearingId(hearingId).build();
    }
}
