package uk.gov.moj.cpp.listing.command.hearing.handler;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The SeedHearingAggregate commands (MBD refactor P2.3.3, ADR 015): the JEE
 * {@code ListNextHearingCommandHandler}, {@code UnscheduledListingCommandHandler},
 * {@code DeletePreviousHearingsAndCreateNextHearingHandler} and
 * {@code UpdateExistingHearingCommandHandler} methods for the seeding hearing's next hearings
 * ({@link SeedHearingCommandHandler}, the ported methods). Each is one transaction: the seed
 * rules ({@code HearingFlow#updateSeedHearing}) and then the processor steps
 * ({@link ProcessorQueue}). Only the command API sends these.
 */
@Service
public class NextHearingCommands {

    public static final String LIST_NEXT_HEARINGS_ENRICHED_V2 = "listing.command.list-next-hearings-enriched-v2";
    public static final String LIST_UNSCHEDULED_NEXT_HEARINGS_ENRICHED = "listing.command.list-unscheduled-next-hearings-enriched";
    public static final String DELETE_NEXT_HEARINGS = "listing.command.delete-next-hearings";
    public static final String DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING = "listing.command.delete-previous-hearings-and-create-next-hearing";
    public static final String UPDATE_EXISTING_HEARING = "listing.command.update-existing-hearing";

    private final SeedHearingCommandHandler handler;
    private final ProcessorQueue processorQueue;

    public NextHearingCommands(final SeedHearingCommandHandler handler, final ProcessorQueue processorQueue) {
        this.handler = handler;
        this.processorQueue = processorQueue;
    }

    /** {@code listing.command.list-next-hearings-enriched-v2}. */
    @Transactional
    public void listNextHearingsEnrichedV2(final Envelope command) {
        processorQueue.command(() -> handler.listNextHearings(command));
    }

    /** {@code listing.command.list-unscheduled-next-hearings-enriched}. */
    @Transactional
    public void listUnscheduledNextHearingsEnriched(final Envelope command) {
        processorQueue.command(() -> handler.handleListUnscheduledNextHearings(command));
    }

    /** {@code listing.command.delete-next-hearings}. */
    @Transactional
    public void deleteNextHearings(final Envelope command) {
        processorQueue.command(() -> handler.deleteNextHearings(command));
    }

    /** {@code listing.command.delete-previous-hearings-and-create-next-hearing}. */
    @Transactional
    public void deletePreviousHearingsAndCreateNextHearing(final Envelope command) {
        processorQueue.command(() -> handler.deletePreviousHearingsAndCreateNextHearing(command));
    }

    /** {@code listing.command.update-existing-hearing}. */
    @Transactional
    public void updateExistingHearing(final Envelope command) {
        processorQueue.command(() -> handler.updateExistingHearing(command));
    }
}
