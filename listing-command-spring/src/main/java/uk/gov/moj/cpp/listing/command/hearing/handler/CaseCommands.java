package uk.gov.moj.cpp.listing.command.hearing.handler;

import uk.gov.moj.cpp.listing.command.hearing.flow.InternalCommandPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Case and Application commands (MBD refactor P2.3.3, ADR 015): the JEE handler methods for the
 * Case and Application aggregates ({@link CaseCommandHandler}), each one transaction (the rules,
 * then the processor steps, {@link ProcessorQueue}). All of them are internal: the processor port
 * sends them by name ({@link InternalCommandPort}), and so will the P2.4 inbound handlers
 * (progression's case/defendant/application/eject messages).
 */
@Service
public class CaseCommands implements InternalCommandPort {

    public static final String UPDATE_CASE_DEFENDANT_DETAILS = "listing.command.update-case-defendant-details";
    public static final String UPDATE_CASE_DEFENDANT_OFFENCES = "listing.command.update-case-defendant-offences";
    public static final String ADD_COURT_APPLICATION_TO_HEARING = "listing.command.add-court-application-to-hearing";
    public static final String UPDATE_COURT_APPLICATION = "listing.command.update-court-application";
    public static final String UPDATE_LAA_REFERENCE_FOR_APPLICATION = "listing.command.update-laa-reference-for-application";
    public static final String UPDATE_CASE_MARKERS = "listing.command.update-case-markers";
    public static final String UPDATE_LINKED_CASES = "listing.command.update-linked-cases";
    public static final String ADD_DEFENDANTS_TO_COURT_PROCEEDINGS = "listing.command.add-defendants-to-court-proceedings";
    public static final String ADD_HEARING_TO_CASE = "listing.command.add-hearing-to-case";
    public static final String UPDATE_HEARING_TO_CASE = "listing.command.update-hearing-to-case";
    public static final String EJECT_CASE_OR_APPLICATION = "listing.command.eject-case-or-application";
    public static final String UPDATE_DEFENDANT_LEGALAID_STATUS = "listing.command.update-defendant-legalaid-status";
    public static final String UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED = "listing.command.update-case-resulted-defendant-proceedings-concluded";
    public static final String MARK_HEARING_AS_DUPLICATE_FOR_CASE = "listing.command.mark-hearing-as-duplicate-for-case";
    public static final String REMOVE_CASE_FROM_GROUP_CASES = "listing.command.remove-case-from-group-cases";

    private final CaseCommandHandler handler;
    private final ProcessorQueue processorQueue;

    public CaseCommands(final CaseCommandHandler handler, final ProcessorQueue processorQueue) {
        this.handler = handler;
        this.processorQueue = processorQueue;
    }

    @Override
    public void register(final Registry commands) {
        commands
                .on(UPDATE_CASE_DEFENDANT_DETAILS, this::updateCaseDefendantDetails)
                .on(UPDATE_CASE_DEFENDANT_OFFENCES, this::updateCaseDefendantOffences)
                .on(ADD_COURT_APPLICATION_TO_HEARING, this::addCourtApplicationToHearing)
                .on(UPDATE_COURT_APPLICATION, this::updateCourtApplication)
                .on(UPDATE_LAA_REFERENCE_FOR_APPLICATION, this::updateLaaReferenceForApplication)
                .on(UPDATE_CASE_MARKERS, this::updateCaseMarkers)
                .on(UPDATE_LINKED_CASES, this::updateLinkedCases)
                .on(ADD_DEFENDANTS_TO_COURT_PROCEEDINGS, this::addDefendantsToCourtProceedings)
                .on(ADD_HEARING_TO_CASE, this::addHearingToCase)
                .on(UPDATE_HEARING_TO_CASE, this::updateHearingToCase)
                .on(EJECT_CASE_OR_APPLICATION, this::ejectCaseOrApplication)
                .on(UPDATE_DEFENDANT_LEGALAID_STATUS, this::updateDefendantLegalaidStatus)
                .on(UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED, this::updateCaseResultedDefendantProceedingsConcluded)
                .on(MARK_HEARING_AS_DUPLICATE_FOR_CASE, this::markHearingAsDuplicateForCase)
                .on(REMOVE_CASE_FROM_GROUP_CASES, this::removeCaseFromGroupCases);
    }

    /** {@code listing.command.update-case-defendant-details}. */
    @Transactional
    public void updateCaseDefendantDetails(final Envelope command) {
        processorQueue.command(() -> handler.updateCaseDefendantDetails(command));
    }

    /** {@code listing.command.update-case-defendant-offences}. */
    @Transactional
    public void updateCaseDefendantOffences(final Envelope command) {
        processorQueue.command(() -> handler.updateCaseDefendantOffences(command));
    }

    /** {@code listing.command.add-court-application-to-hearing}. */
    @Transactional
    public void addCourtApplicationToHearing(final Envelope command) {
        processorQueue.command(() -> handler.addCourtApplicationToHearing(command));
    }

    /** {@code listing.command.update-court-application}. */
    @Transactional
    public void updateCourtApplication(final Envelope command) {
        processorQueue.command(() -> handler.updateCourtApplication(command));
    }

    /** {@code listing.command.update-laa-reference-for-application}. */
    @Transactional
    public void updateLaaReferenceForApplication(final Envelope command) {
        processorQueue.command(() -> handler.updateLaaReferenceForCourtApplication(command));
    }

    /** {@code listing.command.update-case-markers}. */
    @Transactional
    public void updateCaseMarkers(final Envelope command) {
        processorQueue.command(() -> handler.updateCaseMarker(command));
    }

    /** {@code listing.command.update-linked-cases}. */
    @Transactional
    public void updateLinkedCases(final Envelope command) {
        processorQueue.command(() -> handler.updateLinkedCases(command));
    }

    /** {@code listing.command.add-defendants-to-court-proceedings}. */
    @Transactional
    public void addDefendantsToCourtProceedings(final Envelope command) {
        processorQueue.command(() -> handler.addDefendantsToCourtProceedings(command));
    }

    /** {@code listing.command.add-hearing-to-case}. */
    @Transactional
    public void addHearingToCase(final Envelope command) {
        processorQueue.command(() -> handler.addHearingToCase(command));
    }

    /** {@code listing.command.update-hearing-to-case}. */
    @Transactional
    public void updateHearingToCase(final Envelope command) {
        processorQueue.command(() -> handler.updateHearingToCase(command));
    }

    /** {@code listing.command.eject-case-or-application}. */
    @Transactional
    public void ejectCaseOrApplication(final Envelope command) {
        processorQueue.command(() -> handler.ejectCaseOrApplication(command));
    }

    /** {@code listing.command.update-defendant-legalaid-status}. */
    @Transactional
    public void updateDefendantLegalaidStatus(final Envelope command) {
        processorQueue.command(() -> handler.updateDefendantLegalAidStatus(command));
    }

    /** {@code listing.command.update-case-resulted-defendant-proceedings-concluded}. */
    @Transactional
    public void updateCaseResultedDefendantProceedingsConcluded(final Envelope command) {
        processorQueue.command(() -> handler.updateDefendantHearingResultedAndCaseResulted(command));
    }

    /** {@code listing.command.mark-hearing-as-duplicate-for-case}. */
    @Transactional
    public void markHearingAsDuplicateForCase(final Envelope command) {
        processorQueue.command(() -> handler.handleMarkHearingAsDuplicateForCase(command));
    }

    /** {@code listing.command.remove-case-from-group-cases}. */
    @Transactional
    public void removeCaseFromGroupCases(final Envelope command) {
        processorQueue.command(() -> handler.removeCaseFromGroupCases(command));
    }
}
