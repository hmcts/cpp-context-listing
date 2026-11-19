package uk.gov.moj.cpp.listing.command.hearing.processor;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.PrivateEventNames;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Every ported processor registers its JEE {@code @Handles} events, by class: inv-processor Table A rows 1–53 (MBD refactor P2.3.2). */
class ProcessorRegistrationTest {

    @Test
    void theProcessorPortHandlesThePrivateEventsTheJeeProcessorHandled() throws Exception {
        final EventHandlers handlers = new EventHandlers();
        final List<ProcessorPort> ports = List.of(new ListingEventProcessor(), new NextHearingProcessor(), new UpdateExistingHearingEventProcessor(),
                new CreateNextHearingRequestedEventProcessor(), new HearingSequencedEventProcessor(), new HearingUnallocatedCourtroomRemovedEventProcessor(),
                new JudiciaryChangedEventProcessor(), new CourtSchedulerListingEventProcessor());
        ports.forEach(port -> port.register(handlers));

        final Field field = EventHandlers.class.getDeclaredField("handlers");
        field.setAccessible(true);
        final Map<?, ?> registered = (Map<?, ?>) field.get(handlers);
        final List<String> names = registered.keySet().stream()
                .map(eventClass -> PrivateEventNames.nameOf(org.springframework.objenesis.ObjenesisHelper.newInstance((Class<?>) eventClass)))
                .toList();

        assertThat(names).hasSize(53).doesNotHaveDuplicates().allMatch(name -> name.startsWith("listing.event"));
        assertThat(names).contains("listing.events.hearing-listed", "listing.events.hearing-allocated-for-listing-v2",
                "listing.events.allocated-hearing-updated-for-listing-v2", "listing.events.hearing-unallocated-for-listing",
                "listing.events.court-list-restricted", "listing.events.allocated-hearing-extended-for-listing-v2",
                "listing.event.cases-added-to-hearing", "listing.events.hearings-update-completed", "listing.events.available-slots-for-hearing-freed",
                "listing.events.hearing-days-changed-for-hearing", "listing.events.next-hearing-day-changed", "listing.events.hearing-deleted");
    }
}
