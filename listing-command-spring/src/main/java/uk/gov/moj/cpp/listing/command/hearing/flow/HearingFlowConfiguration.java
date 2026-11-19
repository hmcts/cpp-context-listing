package uk.gov.moj.cpp.listing.command.hearing.flow;

import uk.gov.moj.cpp.listing.command.hearing.state.HearingStateStore;
import uk.gov.moj.cpp.listing.command.hearing.state.RulesStateStore;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.HearingRulesStateJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository;

import java.time.Clock;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The Hearing command flow (MBD refactor P2.3.2, ADR 015). */
@Configuration
public class HearingFlowConfiguration {

    @Bean
    public ProcessorQueue processorQueue() {
        return new ProcessorQueue();
    }

    @Bean
    public HearingStateStore hearingStateStore(final HearingRepository hearingRepository, final HearingRulesStateJdbcRepository stateRepository,
                                               final Clock clock) {
        return new HearingStateStore(hearingRepository, stateRepository, clock);
    }

    @Bean
    public RulesStateStore rulesStateStore(final RulesStateJdbcRepository repository, final Clock clock) {
        return new RulesStateStore(repository, clock);
    }

    @Bean
    public HearingFlow hearingFlow(final HearingStateStore hearingStateStore, final RulesStateStore rulesStateStore,
                                   final ObjectProvider<ListenerPort> listenerPorts, final ObjectProvider<ProcessorPort> processorPorts,
                                   final ProcessorQueue processorQueue, final Clock clock) {
        return new HearingFlow(hearingStateStore, rulesStateStore, listenerPorts, processorPorts, processorQueue, clock);
    }

    @Bean
    public InternalCommands internalCommands(final ObjectProvider<InternalCommandPort> ports) {
        return new InternalCommands(ports);
    }

    @Bean
    public ProcessorSender processorSender(final EventOutbox eventOutbox, final InternalCommands internalCommands) {
        return new ProcessorSender(eventOutbox, internalCommands);
    }
}
