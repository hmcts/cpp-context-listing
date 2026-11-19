package uk.gov.moj.cpp.listing.command.hearing.handler;

import uk.gov.moj.cpp.listing.command.hearing.flow.InternalCommandPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Hearing commands (MBD refactor P2.3.2, ADR 015): the JEE {@code ListingCommandHandler} (and
 * {@code UnscheduledListingCommandHandler}, {@code DeleteCourtApplicationHandler},
 * {@code HearingResultedCommandHandler}) methods for the Hearing aggregate, without an event
 * stream ({@link HearingCommandHandler}, the ported methods). Each is one transaction: {@code HearingFlow#update} (state → ported rules → listener
 * port) and then the processor steps ({@link ProcessorQueue}). The command API services call the
 * {@code -enriched} methods; the processor port reaches the internal ones by name
 * ({@link InternalCommandPort}), as do the P2.4 inbound handlers.
 */
@Service
public class HearingCommands implements InternalCommandPort {

    public static final String LIST_COURT_HEARING_ENRICHED = "listing.command.list-court-hearing-enriched";
    public static final String LIST_UNSCHEDULED_COURT_HEARING_ENRICHED = "listing.command.list-unscheduled-court-hearing-enriched";
    public static final String UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";
    public static final String UPDATE_HEARINGS_FOR_LISTING_ENRICHED = "listing.command.update-hearings-for-listing-enriched";
    public static final String VACATE_TRIAL_ENRICHED = "listing.command.vacate-trial-enriched";
    public static final String MOVE_HEARING_TO_PAST_DATE_ENRICHED = "listing.command.move-hearing-to-past-date-enriched";
    public static final String CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING_ENRICHED = "listing.command.change-court-room-for-multiday-hearing-enriched";
    public static final String EXTEND_HEARING_FOR_HEARING_ENRICHED = "listing.command.extend-hearing-for-hearing-enriched";
    public static final String CHANGE_JUDICIARY_FOR_HEARINGS = "listing.command.change-judiciary-for-hearings";
    public static final String SEQUENCE_HEARINGS = "listing.command.sequence-hearings";
    public static final String RESTRICT_COURT_LIST = "listing.command.restrict-court-list";
    public static final String UPDATE_HEARING_DAY_COURT_SCHEDULE = "listing.command.update-hearing-day-court-schedule";
    public static final String DELETE_HEARING = "listing.command.delete-hearing";
    public static final String UPDATE_HEARING_ADD_CASE_BDF = "listing.command.update-hearing-add-case-bdf";
    public static final String HEARING_VACATE_TRIAL = "listing.command.hearing-vacate-trial";
    public static final String REMOVE_PARTIALLY_MERGED_OFFENCES_FROM_ORIGINAL_HEARING = "listing.command.remove-partially-merged-offences-from-original-hearing";
    public static final String MARK_HEARING_AS_DELETED = "listing.command.mark-hearing-as-deleted";
    public static final String ADD_CASES_TO_HEARING = "listing.command.add-cases-to-hearing";
    public static final String UPDATE_COURT_APPLICATION_FOR_HEARINGS = "listing.command.update-court-application-for-hearings";
    public static final String ADD_COURT_APPLICATION_FOR_HEARING = "listing.command.add-court-application-for-hearing";
    public static final String HANDLER_MODIFY_HEARING_COUNSEL = "listing.command.handler.modify-hearing-counsel";
    public static final String CANCEL_HEARING_DAYS = "listing.command.cancel-hearing-days";
    public static final String UPDATE_CPS_PROSECUTOR_WITH_ASSOCIATED_HEARINGS = "listing.command.update-cps-prosecutor-with-associated-hearings";
    public static final String SET_HEARING_RESULTED_STATUS = "listing.command.set-hearing-resulted-status";
    public static final String DELETE_COURT_APPLICATION_HEARING = "listing.command.delete-court-application-hearing";
    public static final String CHANGE_NEXT_HEARING_DAY = "listing.command.change-next-hearing-day";
    public static final String REMOVE_SELECTED_OFFENCES_FROM_EXISTING_HEARING = "listing.command.remove-selected-offences-from-existing-hearing";
    public static final String REMOVE_OFFENCES_FROM_EXISTING_HEARING = "listing.command.remove-offences-from-existing-hearing";
    public static final String MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES = "listing.command.migrate-crown-hearings-to-courtschedules";
    public static final String MARK_UNALLOCATED_HEARING_AS_DUPLICATE = "listing.command.mark-unallocated-hearing-as-duplicate";
    public static final String MARK_HEARING_AS_DUPLICATE = "listing.command.mark-hearing-as-duplicate";
    public static final String LIST_NEXT_HEARING = "listing.command.list-next-hearing";
    public static final String LIST_UNSCHEDULED_NEXT_HEARING = "listing.command.list-unscheduled-next-hearing";
    public static final String DELETE_SEEDED_HEARING = "listing.command.delete-seeded-hearing";
    public static final String UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing";
    public static final String UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    public static final String DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    public static final String ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    public static final String UPDATE_CASE_MARKERS_TO_HEARING = "listing.command.update-case-markers-to-hearing";
    public static final String UPDATE_LINKED_CASE_IN_HEARING = "listing.command.update-linked-case-in-hearing";
    public static final String ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING = "listing.command.add-defendants-to-court-proceedings-for-hearing";
    public static final String EJECT_CASE = "listing.command.eject-case";
    public static final String EJECT_APPLICATION = "listing.command.eject-application";
    public static final String UPDATE_DEFENDANT_LEGALAID_STATUS_FOR_HEARING = "listing.command.update-defendant-legalaid-status-for-hearing";
    public static final String UPDATE_DEFENDANT_COURT_PROCEEDINGS = "listing.command.update-defendant-court-proceedings";

    private final HearingCommandHandler handler;
    private final ProcessorQueue processorQueue;

    public HearingCommands(final HearingCommandHandler handler, final ProcessorQueue processorQueue) {
        this.handler = handler;
        this.processorQueue = processorQueue;
    }

    @Override
    public void register(final Registry commands) {
        commands
                .on(HEARING_VACATE_TRIAL, this::hearingVacateTrial)
                .on(REMOVE_PARTIALLY_MERGED_OFFENCES_FROM_ORIGINAL_HEARING, this::removePartiallyMergedOffencesFromOriginalHearing)
                .on(MARK_HEARING_AS_DELETED, this::markHearingAsDeleted)
                .on(ADD_CASES_TO_HEARING, this::addCasesToHearing)
                .on(UPDATE_COURT_APPLICATION_FOR_HEARINGS, this::updateCourtApplicationForHearings)
                .on(ADD_COURT_APPLICATION_FOR_HEARING, this::addCourtApplicationForHearing)
                .on(HANDLER_MODIFY_HEARING_COUNSEL, this::modifyHearingCounsel)
                .on(CANCEL_HEARING_DAYS, this::cancelHearingDays)
                .on(UPDATE_CPS_PROSECUTOR_WITH_ASSOCIATED_HEARINGS, this::updateCpsProsecutorWithAssociatedHearings)
                .on(SET_HEARING_RESULTED_STATUS, this::setHearingResultedStatus)
                .on(DELETE_COURT_APPLICATION_HEARING, this::deleteCourtApplicationHearing)
                .on(CHANGE_NEXT_HEARING_DAY, this::changeNextHearingDay)
                .on(REMOVE_SELECTED_OFFENCES_FROM_EXISTING_HEARING, this::removeSelectedOffencesFromExistingHearing)
                .on(REMOVE_OFFENCES_FROM_EXISTING_HEARING, this::removeOffencesFromExistingHearing)
                .on(MARK_HEARING_AS_DUPLICATE, this::markHearingAsDuplicate)
                .on(LIST_NEXT_HEARING, this::listNextHearing)
                .on(LIST_UNSCHEDULED_NEXT_HEARING, this::listUnscheduledNextHearing)
                .on(DELETE_SEEDED_HEARING, this::deleteSeededHearing)
                .on(UPDATE_DEFENDANTS_FOR_HEARING, this::updateDefendantsForHearing)
                .on(UPDATE_OFFENCES_FOR_HEARING, this::updateOffencesForHearing)
                .on(DELETE_OFFENCES_FOR_HEARING, this::deleteOffencesForHearing)
                .on(ADD_OFFENCES_FOR_HEARING, this::addOffencesForHearing)
                .on(UPDATE_CASE_MARKERS_TO_HEARING, this::updateCaseMarkersToHearing)
                .on(UPDATE_LINKED_CASE_IN_HEARING, this::updateLinkedCaseInHearing)
                .on(ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING, this::addDefendantsToCourtProceedingsForHearing)
                .on(EJECT_CASE, this::ejectCase)
                .on(EJECT_APPLICATION, this::ejectApplication)
                .on(UPDATE_DEFENDANT_LEGALAID_STATUS_FOR_HEARING, this::updateDefendantLegalaidStatusForHearing)
                .on(UPDATE_DEFENDANT_COURT_PROCEEDINGS, this::updateDefendantCourtProceedings);
    }

    /** {@code listing.command.list-court-hearing-enriched}. */
    @Transactional
    public void listCourtHearingEnriched(final Envelope command) {
        processorQueue.command(() -> handler.listCourtHearing(command));
    }

    /** {@code listing.command.list-unscheduled-court-hearing-enriched}. */
    @Transactional
    public void listUnscheduledCourtHearingEnriched(final Envelope command) {
        processorQueue.command(() -> handler.handleListUnscheduledCourtHearing(command));
    }

    /** {@code listing.command.update-hearing-for-listing-enriched}. */
    @Transactional
    public void updateHearingForListingEnriched(final Envelope command) {
        processorQueue.command(() -> handler.updateHearingForListing(command));
    }

    /** {@code listing.command.update-hearings-for-listing-enriched}. */
    @Transactional
    public void updateHearingsForListingEnriched(final Envelope command) {
        processorQueue.command(() -> handler.updateHearingsForListing(command));
    }

    /** {@code listing.command.vacate-trial-enriched}. */
    @Transactional
    public void vacateTrialEnriched(final Envelope command) {
        processorQueue.command(() -> handler.vacateTrial(command));
    }

    /** {@code listing.command.move-hearing-to-past-date-enriched}. */
    @Transactional
    public void moveHearingToPastDateEnriched(final Envelope command) {
        processorQueue.command(() -> handler.moveHearingToPastDate(command));
    }

    /** {@code listing.command.change-court-room-for-multiday-hearing-enriched}. */
    @Transactional
    public void changeCourtRoomForMultidayHearingEnriched(final Envelope command) {
        processorQueue.command(() -> handler.changeCourtRoomForMultidayHearing(command));
    }

    /** {@code listing.command.extend-hearing-for-hearing-enriched}. */
    @Transactional
    public void extendHearingForHearingEnriched(final Envelope command) {
        processorQueue.command(() -> handler.extendHearingForHearing(command));
    }

    /** {@code listing.command.change-judiciary-for-hearings}. */
    @Transactional
    public void changeJudiciaryForHearings(final Envelope command) {
        processorQueue.command(() -> handler.changeJudiciaryForHearings(command));
    }

    /** {@code listing.command.sequence-hearings}. */
    @Transactional
    public void sequenceHearings(final Envelope command) {
        processorQueue.command(() -> handler.sequenceHearings(command));
    }

    /** {@code listing.command.restrict-court-list}. */
    @Transactional
    public void restrictCourtList(final Envelope command) {
        processorQueue.command(() -> handler.restrictFromCourtList(command));
    }

    /** {@code listing.command.update-hearing-day-court-schedule}. */
    @Transactional
    public void updateHearingDayCourtSchedule(final Envelope command) {
        processorQueue.command(() -> handler.updateHearingDayCourtSchedule(command));
    }

    /** {@code listing.command.delete-hearing}. */
    @Transactional
    public void deleteHearing(final Envelope command) {
        processorQueue.command(() -> handler.deleteHearing(command));
    }

    /** {@code listing.command.update-hearing-add-case-bdf}. */
    @Transactional
    public void updateHearingAddCaseBdf(final Envelope command) {
        processorQueue.command(() -> handler.updateHearingAddCaseBdf(command));
    }

    /** {@code listing.command.hearing-vacate-trial}. */
    @Transactional
    public void hearingVacateTrial(final Envelope command) {
        processorQueue.command(() -> handler.hearingVacateTrial(command));
    }

    /** {@code listing.command.remove-partially-merged-offences-from-original-hearing}. */
    @Transactional
    public void removePartiallyMergedOffencesFromOriginalHearing(final Envelope command) {
        processorQueue.command(() -> handler.handleRemovePartiallyMergedOffencesFromOriginalHearing(command));
    }

    /** {@code listing.command.mark-hearing-as-deleted}. */
    @Transactional
    public void markHearingAsDeleted(final Envelope command) {
        processorQueue.command(() -> handler.handleMarkHearingAsDeleted(command));
    }

    /** {@code listing.command.add-cases-to-hearing}. */
    @Transactional
    public void addCasesToHearing(final Envelope command) {
        processorQueue.command(() -> handler.handleAddCasesToHearing(command));
    }

    /** {@code listing.command.update-court-application-for-hearings}. */
    @Transactional
    public void updateCourtApplicationForHearings(final Envelope command) {
        processorQueue.command(() -> handler.updateCourtApplicationForHearings(command));
    }

    /** {@code listing.command.add-court-application-for-hearing}. */
    @Transactional
    public void addCourtApplicationForHearing(final Envelope command) {
        processorQueue.command(() -> handler.addCourtApplicationForHearing(command));
    }

    /** {@code listing.command.handler.modify-hearing-counsel}. */
    @Transactional
    public void modifyHearingCounsel(final Envelope command) {
        processorQueue.command(() -> handler.modifyHearingCounsels(command));
    }

    /** {@code listing.command.cancel-hearing-days}. */
    @Transactional
    public void cancelHearingDays(final Envelope command) {
        processorQueue.command(() -> handler.cancelHearingDays(command));
    }

    /** {@code listing.command.update-cps-prosecutor-with-associated-hearings}. */
    @Transactional
    public void updateCpsProsecutorWithAssociatedHearings(final Envelope command) {
        processorQueue.command(() -> handler.updateProsecutorForAssociatedHearings(command));
    }

    /** {@code listing.command.set-hearing-resulted-status}. */
    @Transactional
    public void setHearingResultedStatus(final Envelope command) {
        processorQueue.command(() -> handler.handleSetHearingResultStatus(command));
    }

    /** {@code listing.command.delete-court-application-hearing}. */
    @Transactional
    public void deleteCourtApplicationHearing(final Envelope command) {
        processorQueue.command(() -> handler.handleDeleteCourtApplicationHearing(command));
    }

    /** {@code listing.command.change-next-hearing-day}. */
    @Transactional
    public void changeNextHearingDay(final Envelope command) {
        processorQueue.command(() -> handler.changeNextHearingDay(command));
    }

    /** {@code listing.command.remove-selected-offences-from-existing-hearing}. */
    @Transactional
    public void removeSelectedOffencesFromExistingHearing(final Envelope command) {
        processorQueue.command(() -> handler.removeSelectedOffencesFromExistingHearing(command));
    }

    /** {@code listing.command.remove-offences-from-existing-hearing}. */
    @Transactional
    public void removeOffencesFromExistingHearing(final Envelope command) {
        processorQueue.command(() -> handler.removeOffencesFromExistingHearing(command));
    }

    /** {@code listing.command.migrate-crown-hearings-to-courtschedules}. */
    @Transactional
    public void migrateCrownHearingsToCourtschedules(final Envelope command) {
        processorQueue.command(() -> handler.migrateCrownHearingsToCourtSchedules(command));
    }

    /** {@code listing.command.mark-unallocated-hearing-as-duplicate}. */
    @Transactional
    public void markUnallocatedHearingAsDuplicate(final Envelope command) {
        processorQueue.command(() -> handler.markUnallocatedHearingAsDuplicate(command));
    }

    /** {@code listing.command.mark-hearing-as-duplicate}. */
    @Transactional
    public void markHearingAsDuplicate(final Envelope command) {
        processorQueue.command(() -> handler.markHearingAsDuplicate(command));
    }

    /** {@code listing.command.list-next-hearing}. */
    @Transactional
    public void listNextHearing(final Envelope command) {
        processorQueue.command(() -> handler.listNextCourtHearing(command));
    }

    /** {@code listing.command.list-unscheduled-next-hearing}. */
    @Transactional
    public void listUnscheduledNextHearing(final Envelope command) {
        processorQueue.command(() -> handler.handleListUnscheduledNextHearing(command));
    }

    /** {@code listing.command.delete-seeded-hearing}. */
    @Transactional
    public void deleteSeededHearing(final Envelope command) {
        processorQueue.command(() -> handler.deleteSeededHearing(command));
    }

    /** {@code listing.command.update-defendants-for-hearing}. */
    @Transactional
    public void updateDefendantsForHearing(final Envelope command) {
        processorQueue.command(() -> handler.updateDefendantsForHearing(command));
    }

    /** {@code listing.command.update-offences-for-hearing}. */
    @Transactional
    public void updateOffencesForHearing(final Envelope command) {
        processorQueue.command(() -> handler.updateOffencesForHearing(command));
    }

    /** {@code listing.command.delete-offences-for-hearing}. */
    @Transactional
    public void deleteOffencesForHearing(final Envelope command) {
        processorQueue.command(() -> handler.deleteOffencesForHearing(command));
    }

    /** {@code listing.command.add-offences-for-hearing}. */
    @Transactional
    public void addOffencesForHearing(final Envelope command) {
        processorQueue.command(() -> handler.addOffencesForHearing(command));
    }

    /** {@code listing.command.update-case-markers-to-hearing}. */
    @Transactional
    public void updateCaseMarkersToHearing(final Envelope command) {
        processorQueue.command(() -> handler.updateCaseMarkersToHearing(command));
    }

    /** {@code listing.command.update-linked-case-in-hearing}. */
    @Transactional
    public void updateLinkedCaseInHearing(final Envelope command) {
        processorQueue.command(() -> handler.updateLinkedCaseInHearing(command));
    }

    /** {@code listing.command.add-defendants-to-court-proceedings-for-hearing}. */
    @Transactional
    public void addDefendantsToCourtProceedingsForHearing(final Envelope command) {
        processorQueue.command(() -> handler.addDefendantsForCourtProceedingsForHearing(command));
    }

    /** {@code listing.command.eject-case}. */
    @Transactional
    public void ejectCase(final Envelope command) {
        processorQueue.command(() -> handler.ejectCase(command));
    }

    /** {@code listing.command.eject-application}. */
    @Transactional
    public void ejectApplication(final Envelope command) {
        processorQueue.command(() -> handler.ejectApplication(command));
    }

    /** {@code listing.command.update-defendant-legalaid-status-for-hearing}. */
    @Transactional
    public void updateDefendantLegalaidStatusForHearing(final Envelope command) {
        processorQueue.command(() -> handler.updateDefendantLegalAidStatusForHearing(command));
    }

    /** {@code listing.command.update-defendant-court-proceedings}. */
    @Transactional
    public void updateDefendantCourtProceedings(final Envelope command) {
        processorQueue.command(() -> handler.updateDefendantCourtProceedings(command));
    }
}
