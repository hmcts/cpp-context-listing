package uk.gov.moj.cpp.listing.command.hearing.handler;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;

import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateCaseMarkersCommand;
import uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand;
import uk.gov.justice.listing.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.listing.courts.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.AddedOffences;
import uk.gov.justice.listing.courts.CaseOrApplicationEjected;
import uk.gov.justice.listing.courts.Cases;
import uk.gov.justice.listing.courts.DeletedOffences;
import uk.gov.justice.listing.courts.MarkHearingAsDuplicateForCase;
import uk.gov.justice.listing.courts.UpdateCaseDefendantDetails;
import uk.gov.justice.listing.courts.UpdateCaseDefendantOffences;
import uk.gov.justice.listing.courts.UpdateCourtApplicationCommand;
import uk.gov.justice.listing.courts.UpdateHearingToCaseCommand;
import uk.gov.justice.listing.courts.UpdateLaaReferenceForApplication;
import uk.gov.justice.listing.courts.UpdateLinkedCases;
import uk.gov.justice.listing.courts.UpdatedOffences;
import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CaseMarkersToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CasesToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtApplicationToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsAddedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsDeletedOffenceToDomainCaseSimpleOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsUpdatedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.hearing.rules.ApplicationRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.CaseRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The JEE command-handler methods for the Case and Application aggregates (MBD refactor P2.3.3,
 * ADR 015), ported as verbatim as possible from {@code ListingCommandHandler},
 * {@code HearingMarkedAsDuplicateCommandHandler} (the case part) and
 * {@code RemoveCaseFromGroupCasesCommandHandler}: {@code updateCaseEventStream} /
 * {@code updateApplicationEventStream} run the ported rules ({@link CaseRules},
 * {@link ApplicationRules}) on their kept state through {@link HearingFlow}. Called by
 * {@link CaseCommands}, which owns the transaction and the processor queue.
 */
@Component
@SuppressWarnings({"squid:S2629"})
public class CaseCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseCommandHandler.class);

    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    private static final String LEGAL_AID_STATUS = "legalAidStatus";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String NUMBER_OF_GROUP_CASES = "numberOfGroupCases";

    @Autowired
    private HearingFlow hearingFlow;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Autowired
    private CommandToDomainConverter commandToDomainConverter;

    @Autowired
    private CourtsDefendantToDomainConverter defendantUpdatedToDomainConverter;

    @Autowired
    private CaseMarkersToDomainConverter caseMarkersToDomainConverter;

    @Autowired
    private CourtsUpdatedOffenceToDomainOffence courtsUpdatedOffenceToDomainOffence;

    @Autowired
    private CourtsAddedOffenceToDomainOffence courtsAddedOffenceToDomainOffence;

    @Autowired
    private CourtsDeletedOffenceToDomainCaseSimpleOffence courtsDeletedOffenceToDomainCaseSimpleOffence;

    @Autowired
    private CourtApplicationToDomainConverter courtApplicationToDomainConverter;

    @Autowired
    private CasesToDomainConverter casesToDomainConverter;

    /** {@code listing.command.update-case-defendant-details}. */
    public void updateCaseDefendantDetails(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-case-defendant-details' received with payload {}", command.payload());
        }
        final UpdateCaseDefendantDetails updateCaseDefendantDetails = jsonObjectConverter.convert(payload, UpdateCaseDefendantDetails.class);

        final uk.gov.justice.listing.courts.Defendant courtsDefendant = updateCaseDefendantDetails.getDefendant();
        final UUID caseId = courtsDefendant.getProsecutionCaseId();
        final uk.gov.moj.cpp.listing.domain.Defendant defendant = defendantUpdatedToDomainConverter.convert(courtsDefendant);

        updateCaseEventStream(command, caseId, (CaseRules listingCase) ->
                listingCase.updateDefendant(caseId, defendant));
    }

    /** {@code listing.command.update-case-defendant-offences}. */
    public void updateCaseDefendantOffences(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-case-defendant-offences' received with payload {}", command.payload());
        }

        final UpdateCaseDefendantOffences updateCaseDefendantOffences = jsonObjectConverter.convert(payload, UpdateCaseDefendantOffences.class);

        // Handle the updated offences
        final List<UpdatedOffences> updatedOffence = updateCaseDefendantOffences.getUpdatedOffences();
        final List<CaseOffences> multipleCaseOffences = courtsUpdatedOffenceToDomainOffence.convert(updatedOffence);
        for (final CaseOffences caseOffences : multipleCaseOffences) {
            final UUID caseId = caseOffences.getCaseId();
            updateCaseEventStream(command, caseId, (CaseRules listingCase) ->
                    listingCase.updateDefendantOffences(caseOffences)
            );
        }

        // Handle the deleted offences
        final List<DeletedOffences> deletedOffence = updateCaseDefendantOffences.getDeletedOffences();
        final List<CaseSimpleOffences> offencesToBeDeleted = courtsDeletedOffenceToDomainCaseSimpleOffence.convert(deletedOffence);
        for (final CaseSimpleOffences offenceToBeDeleted : offencesToBeDeleted) {
            final UUID caseId = offenceToBeDeleted.getCaseId();
            updateCaseEventStream(command, caseId, (CaseRules listingCase) ->
                    listingCase.deleteDefendantOffences(offenceToBeDeleted)
            );
        }

        // Handle the added offences
        final List<AddedOffences> addedOffence = updateCaseDefendantOffences.getAddedOffences();
        final List<CaseOffences> addedOffences = courtsAddedOffenceToDomainOffence.convert(addedOffence);
        for (final CaseOffences caseBaseOffences : addedOffences) {
            final UUID caseId = caseBaseOffences.getCaseId();
            updateCaseEventStream(command, caseId, (CaseRules listingCase) ->
                    listingCase.addedDefendantOffences(caseBaseOffences)
            );
        }
    }

    /** {@code listing.command.add-court-application-to-hearing}. */
    public void addCourtApplicationToHearing(final Envelope commandEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.add-court-application-to-hearing' received with payload {}", commandEnvelope.payload());
        }
        final AddCourtApplicationToHearingCommand command = jsonObjectConverter.convert(commandEnvelope.payload(), AddCourtApplicationToHearingCommand.class);

        final UUID applicationId = command.getApplicationId();
        final UUID hearingId = command.getHearingId();
        updateApplicationEventStream(commandEnvelope, applicationId, (ApplicationRules application) ->
                application.addToHearing(applicationId, hearingId));
    }

    /** {@code listing.command.update-court-application}. */
    public void updateCourtApplication(final Envelope commandEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-court-application' received with payload {}", commandEnvelope.payload());
        }
        final UpdateCourtApplicationCommand command = jsonObjectConverter.convert(commandEnvelope.payload(), UpdateCourtApplicationCommand.class);
        final uk.gov.moj.cpp.listing.domain.CourtApplication courtApplicationDomain = courtApplicationToDomainConverter.convert(command.getCourtApplication());

        final UUID applicationId = courtApplicationDomain.getId();
        updateApplicationEventStream(commandEnvelope, applicationId, (ApplicationRules application) ->
                application.update(courtApplicationDomain));
    }

    /** {@code listing.command.update-laa-reference-for-application}. */
    public void updateLaaReferenceForCourtApplication(final Envelope commandEnvelope) {
        final UpdateLaaReferenceForApplication command = jsonObjectConverter.convert(commandEnvelope.payload(), UpdateLaaReferenceForApplication.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("listing.command.update-laa-reference-for-application received with payload {}", objectToJsonObjectConverter.convert(command));
        }

        updateApplicationEventStream(commandEnvelope, command.getApplicationId(),
                application -> application.updateLaaReference(command.getApplicationId(), command.getSubjectId(), command.getOffenceId(), command.getLaaReference()));
    }

    /** {@code listing.command.update-case-markers}. */
    public void updateCaseMarker(final Envelope commandEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-case-markers' received with payload {}", commandEnvelope.payload());
        }
        final UpdateCaseMarkersCommand command = jsonObjectConverter.convert(commandEnvelope.payload(), UpdateCaseMarkersCommand.class);
        final List<CaseMarker> caseMarkers = caseMarkersToDomainConverter.convert(command.getCaseMarkers());
        final UUID prosecutionCaseId = command.getProsecutionCaseId();
        updateCaseEventStream(commandEnvelope, prosecutionCaseId, (CaseRules listingCase) ->
                listingCase.addedCaseMarkers(prosecutionCaseId, caseMarkers));
    }

    /** {@code listing.command.update-linked-cases}. */
    public void updateLinkedCases(final Envelope commandEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-linked-cases' received with payload {}", commandEnvelope.payload());
        }
        final UpdateLinkedCases command = jsonObjectConverter.convert(commandEnvelope.payload(), UpdateLinkedCases.class);
        for (final Cases cases : command.getCases()) {
            final UUID caseId = cases.getCaseId();
            final uk.gov.moj.cpp.listing.domain.Cases casesDomain = casesToDomainConverter.convert(cases);
            updateCaseEventStream(commandEnvelope, caseId, (CaseRules listingCase) -> listingCase.linkCases(command.getLinkActionType().toString(), cases.getCaseId(), cases.getCaseUrn(), casesDomain));
        }
    }

    /** {@code listing.command.add-defendants-to-court-proceedings}. */
    public void addDefendantsToCourtProceedings(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-defendants-to-court-proceedings' received with payload {}", command.payload());
        }

        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = jsonObjectConverter.convert
                (command.payload(), AddDefendantsToCourtProceedings.class);
        final List<uk.gov.justice.core.courts.Defendant> defendantList = addDefendantsToCourtProceedings.getDefendants();
        final List<ListHearingRequest> listHearingRequestsList = addDefendantsToCourtProceedings.getListHearingRequests();
        final List<uk.gov.moj.cpp.listing.domain.Defendant> defendantDomainList = commandToDomainConverter.convertDefendant(defendantList, listHearingRequestsList);

        for (final uk.gov.moj.cpp.listing.domain.Defendant defendantDomain : defendantDomainList) {
            final UUID caseId = defendantDomain.getProsecutionCaseId();
            updateCaseEventStream(command, caseId, (CaseRules listingCase) ->
                    listingCase.addedDefendantForCourtProceedings(caseId, defendantDomain));
        }
    }

    /** {@code listing.command.add-hearing-to-case}. */
    public void addHearingToCase(final Envelope commandEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-hearing-to-case' received with payload {}", commandEnvelope.payload());
        }
        final AddHearingToCaseCommand command = jsonObjectConverter.convert(commandEnvelope.payload(), AddHearingToCaseCommand.class);

        final UUID caseId = command.getCaseId();
        final UUID hearingId = command.getHearingId();
        updateCaseEventStream(commandEnvelope, caseId, (CaseRules caseAggrgate) ->
                caseAggrgate.addHearing(caseId, hearingId));
    }

    /** {@code listing.command.update-hearing-to-case}. */
    public void updateHearingToCase(final Envelope commandEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-hearing-to-case' received with payload {}", commandEnvelope.payload());
        }
        final UpdateHearingToCaseCommand command = jsonObjectConverter.convert(commandEnvelope.payload(), UpdateHearingToCaseCommand.class);

        final UUID caseId = command.getCaseId();
        final UUID allocatedHearingId = command.getExistingHearingId();
        final UUID unAllocatedHearingId = command.getId();

        updateCaseEventStream(commandEnvelope, caseId, (CaseRules listingCase) ->
                listingCase.updateHearing(caseId, allocatedHearingId, unAllocatedHearingId));
    }

    /** {@code listing.command.eject-case-or-application}: the Case and/or the Application. */
    public void ejectCaseOrApplication(final Envelope commandEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.eject-case-or-application' received with payload {}", commandEnvelope.payload());
        }

        final CaseOrApplicationEjected command = jsonObjectConverter.convert(commandEnvelope.payload(), CaseOrApplicationEjected.class);
        final Optional<UUID> caseId = ofNullable(command.getProsecutionCaseId());
        final List<UUID> hearingIds = command.getHearingIds();
        final Optional<UUID> applicationId = ofNullable(command.getApplicationId());
        if (caseId.isPresent()) {
            updateCaseEventStream(commandEnvelope, caseId.get(), (CaseRules listingCase) ->
                    listingCase.ejectCaseForHearings(hearingIds, caseId.get(), ofNullable(command.getRemovalReason())));
        }
        if (applicationId.isPresent()) {
            updateApplicationEventStream(commandEnvelope, applicationId.get(), (ApplicationRules application) ->
                    application.ejectApplicationForHearings(hearingIds, applicationId.get(), ofNullable(command.getRemovalReason())));
        }
    }

    /** {@code listing.command.update-defendant-legalaid-status}. */
    public void updateDefendantLegalAidStatus(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-defendant-legalaid-status event received {}", envelope.payload());
        }
        final JsonObject payload = envelope.payload();
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final String legalAidStatus = payload.getString(LEGAL_AID_STATUS);
        final UUID caseId = fromString(payload.getString(CASE_ID));
        updateCaseEventStream(envelope, caseId, (CaseRules listingCase) ->
                listingCase.updateDefendantLegalAidStatus(caseId, defendantId, legalAidStatus));
    }

    /** {@code listing.command.update-case-resulted-defendant-proceedings-concluded}. */
    public void updateDefendantHearingResultedAndCaseResulted(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-case-resulted-defendant-proceedings-concluded event received {}", envelope.payload());
        }

        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(envelope.payload().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
        final UUID caseId = prosecutionCase.getId();
        updateCaseEventStream(envelope, caseId, (CaseRules listingCase) -> listingCase.updateDefendantCaseResultedAndUpdated(prosecutionCase));
    }

    /** {@code listing.command.mark-hearing-as-duplicate-for-case} (HearingMarkedAsDuplicateCommandHandler). */
    public void handleMarkHearingAsDuplicateForCase(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.mark-hearing-as-duplicate-for-case' received with payload {}", command.payload());
        }

        final MarkHearingAsDuplicateForCase markHearingAsDuplicateForCase = jsonObjectConverter.convert(command.payload(), MarkHearingAsDuplicateForCase.class);
        final UUID hearingId = markHearingAsDuplicateForCase.getHearingId();
        final UUID caseId = markHearingAsDuplicateForCase.getCaseId();

        updateCaseEventStream(command, caseId, (CaseRules listingCase) ->
                listingCase.markHearingAsDuplicate(hearingId, caseId));
    }

    /** {@code listing.command.remove-case-from-group-cases} (RemoveCaseFromGroupCasesCommandHandler). */
    public void removeCaseFromGroupCases(final Envelope jsonEnvelope) {
        LOGGER.info("{} event received with payload {}",
                "listing.command.remove-case-from-group-cases",
                jsonEnvelope.payload());

        final JsonObject payload = jsonEnvelope.payload();
        final UUID groupId = fromString(payload.getString("groupId"));
        final UUID masterCaseId = fromString(payload.getString("masterCaseId"));
        final ProsecutionCase removedCase = jsonObjectConverter.convert(payload.getJsonObject("removedCase"), ProsecutionCase.class);
        final ProsecutionCase newGroupMaster = payload.containsKey("newGroupMaster") ?
                jsonObjectConverter.convert(payload.getJsonObject("newGroupMaster"), ProsecutionCase.class) : null;
        final Integer numberOfGroupCases = getNumberOfGroupCases(payload);

        // The master case is only read.
        final CaseRules caseAggregate = hearingFlow.loadCase(masterCaseId);
        final List<UUID> hearingIds = caseAggregate.getHearingIds();

        if (nonNull(newGroupMaster) && !hearingIds.isEmpty()) {
            hearingFlow.updateCase(jsonEnvelope.metadata(), newGroupMaster.getId(),
                    newMasterCaseAggregate -> newMasterCaseAggregate.updateMasterCaseForGroup(newGroupMaster.getId(), hearingIds));
        }

        for (final UUID hearingId : hearingIds) {
            hearingFlow.update(jsonEnvelope.metadata(), hearingId,
                    (HearingRules hearingAggregate) -> hearingAggregate.removeCaseFromGroupCases(hearingId, groupId,
                            commandToDomainConverter.buildListedCases(removedCase),
                            commandToDomainConverter.buildListedCases(newGroupMaster),
                            numberOfGroupCases));
        }
    }

    /**
     * Returns null, not 0, when the count is not in the payload: the Hearing aggregate treats null as
     * "no count" and leaves the stored group size unchanged, whereas 0 would be stored as the group size.
     */
    private Integer getNumberOfGroupCases(final JsonObject payload) {
        return payload.containsKey(NUMBER_OF_GROUP_CASES) && !payload.isNull(NUMBER_OF_GROUP_CASES) ?
                payload.getInt(NUMBER_OF_GROUP_CASES) : null;
    }

    private void updateCaseEventStream(final Envelope command, final UUID caseId,
                                       final Function<CaseRules, Stream<Object>> aggregatorFunction) {
        hearingFlow.updateCase(command.metadata(), caseId, aggregatorFunction);
    }

    private void updateApplicationEventStream(final Envelope command, final UUID applicationId,
                                              final Function<ApplicationRules, Stream<Object>> aggregatorFunction) {
        hearingFlow.updateApplication(command.metadata(), applicationId, aggregatorFunction);
    }
}
