package uk.gov.moj.cpp.listing.command.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import uk.gov.moj.cpp.listing.command.hearing.flow.InternalCommandPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.command.hearing.handler.CaseCommandHandler;
import uk.gov.moj.cpp.listing.command.hearing.handler.CaseCommands;
import uk.gov.moj.cpp.listing.command.hearing.handler.HearingCommandHandler;
import uk.gov.moj.cpp.listing.command.hearing.handler.HearingCommands;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.inbound.InboundEventDispatcher;
import uk.gov.moj.cpp.listing.messaging.inbound.InboundEventHandler;
import uk.gov.moj.cpp.listing.messaging.inbound.PublicEventSubscription;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * P2.4 exit check (§4.3, ADR 013): the inbound handlers cover the public.event subscription exactly —
 * every subscribed name has exactly one handler, no handler takes a name the selector does not
 * select, and every internal command they send has a P2.3 command service.
 */
class InboundHandlerRegistrationTest {

    private final PublicEventSubscription subscription = new PublicEventSubscription();
    private final List<InboundEventHandler> handlers = InboundHandlers.all(new ProcessorQueue(), mock(ProcessorSender.class),
            mock(HearingSlotsService.class), mock(HearingQueryView.class), mock(EntityManager.class));

    @Test
    void everySubscribedNameHasExactlyOneHandlerAndTheSelectorListEqualsTheHandlerSet() {
        final List<String> handled = handlers.stream().flatMap(h -> h.handledEvents().stream()).toList();

        assertThat(handled).doesNotHaveDuplicates();
        assertThat(handled).containsExactlyInAnyOrderElementsOf(PublicEventSubscription.EVENT_NAMES);
        assertThat(PublicEventSubscription.EVENT_NAMES).hasSize(28).doesNotHaveDuplicates();
        assertThat(subscription.selector()).isEqualTo(PublicEventSubscription.EVENT_NAMES.stream()
                .map(name -> "'" + name + "'").collect(Collectors.joining(",", "CPPNAME in(", ")")));
    }

    @Test
    void theDispatcherAcceptsTheHandlersAndHasOneForEverySubscribedName() {
        final InboundEventDispatcher dispatcher = new InboundEventDispatcher(handlers, subscription);

        assertThat(PublicEventSubscription.EVENT_NAMES).allSatisfy(name -> assertThat(dispatcher.hasHandlerFor(name)).as(name).isTrue());
    }

    @Test
    void theTestListHoldsEveryInboundHandlerComponentOfTheApplication() {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(InboundEventHandler.class));
        final Set<String> components = scanner.findCandidateComponents("uk.gov.moj.cpp.listing").stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(name -> !name.contains("Test"))
                .collect(Collectors.toSet());

        assertThat(components).containsExactlyInAnyOrderElementsOf(handlers.stream().map(h -> h.getClass().getName()).toList());
    }

    @Test
    void everyCommandTheInboundHandlersSendHasACommandService() {
        final Set<String> services = new HashSet<>();
        final InternalCommandPort.Registry registry = new InternalCommandPort.Registry() {
            @Override
            public InternalCommandPort.Registry on(final String commandName, final Consumer<Envelope> handler) {
                services.add(commandName);
                return this;
            }
        };
        new HearingCommands(mock(HearingCommandHandler.class), new ProcessorQueue()).register(registry);
        new CaseCommands(mock(CaseCommandHandler.class), new ProcessorQueue()).register(registry);

        final List<String> sent = new ArrayList<>(List.of(
                ListingPublicEventProcessor.COMMAND_UPDATE_CASE_DEFENDANT_DETAILS,
                ListingPublicEventProcessor.COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES,
                ListingPublicEventProcessor.COMMAND_UPDATE_COURT_APPLICATION,
                ListingPublicEventProcessor.COMMAND_UPDATE_CASE_MARKERS,
                ListingPublicEventProcessor.LISTING_COMMAND_ADD_CASES_TO_HEARING,
                ListingPublicEventProcessor.COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING,
                ListingPublicEventProcessor.COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS,
                ListingPublicEventProcessor.COMMAND_CASE_OR_APPLICATION_EJECTED,
                ListingPublicEventProcessor.COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS,
                ListingPublicEventProcessor.COMMAND_UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED,
                ListingPublicEventProcessor.PRIVATE_COMMAND_HEARING_VACATE_TRIAL,
                ListingPublicEventProcessor.COMMAND_UPDATE_LAA_REFERENCE_FOR_APPLICATION,
                ListingPublicEventProcessor.COMMAND_UPDATE_LINKED_CASES,
                ListingPublicEventProcessor.COMMAND_CANCEL_HEARING_DAYS,
                ListingPublicEventProcessor.COMMAND_MARK_HEARING_AS_DUPLICATE,
                ListingPublicEventProcessor.COMMAND_REMOVE_SELECTED_OFFENCES_FROM_EXISTING_HEARING,
                CpsProsecutorUpdatedEventProcessor.COMMAND_UPDATE_CPS_PROSECUTOR_WITH_ASSOCIATED_HEARINGS,
                HearingResultedEventProcessor.COMMAND_SET_HEARING_RESULTED_STATUS,
                CaseRemovedFromGroupCasesEventProcessor.COMMAND_REMOVE_CASE_FROM_GROUP_CASES,
                CourtApplicationDeletedProcessor.COMMAND_DELETE_COURT_APPLICATION_HEARING,
                HearingCounselEventProcessor.LISTING_COMMAND_HANDLER_MODIFY_HEARING_COUNSEL,
                RelatedHearingUpdatedForAdhocHearingProcessor.COMMAND_ADD_CASES_TO_HEARING));

        assertThat(services).containsAll(sent);
    }
}
