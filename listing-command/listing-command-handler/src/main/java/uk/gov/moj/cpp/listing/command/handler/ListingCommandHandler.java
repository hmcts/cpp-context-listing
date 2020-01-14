package uk.gov.moj.cpp.listing.command.handler;

import static java.time.LocalDate.parse;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.listing.event.PublishCourtListType.valueOf;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.eventsourcing.source.core.Events.streamOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.utils.json.PublishCourtListJsonSupport.asJson;
import static uk.gov.moj.cpp.listing.domain.HearingLanguage.valueFor;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.convertHoursAndMinutesToMinutes;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.CourtListRequestExport;
import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.listing.commands.Offence;
import uk.gov.justice.listing.commands.PublishCourtList;
import uk.gov.justice.listing.commands.PublishCourtListType;
import uk.gov.justice.listing.commands.RecordCourtListExportFailed;
import uk.gov.justice.listing.commands.RecordCourtListExportSuccessful;
import uk.gov.justice.listing.commands.RecordCourtListProduced;
import uk.gov.justice.listing.commands.SimpleOffence;
import uk.gov.justice.listing.commands.StorePublishedCourtList;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.AddCourtApplicationForHearing;
import uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand;
import uk.gov.justice.listing.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.listing.courts.AddDefendantsToCourtProceedingsForHearing;
import uk.gov.justice.listing.courts.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.AddOffencesForHearing;
import uk.gov.justice.listing.courts.AddedOffences;
import uk.gov.justice.listing.courts.CaseOrApplicationEjected;
import uk.gov.justice.listing.courts.ChangeJudiciaryForHearings;
import uk.gov.justice.listing.courts.DeleteOffencesForHearing;
import uk.gov.justice.listing.courts.DeletedOffences;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;
import uk.gov.justice.listing.courts.RestrictCourtList;
import uk.gov.justice.listing.courts.SequenceHearings;
import uk.gov.justice.listing.courts.UpdateCaseDefendantDetails;
import uk.gov.justice.listing.courts.UpdateCaseDefendantOffences;
import uk.gov.justice.listing.courts.UpdateCourtApplicationCommand;
import uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings;
import uk.gov.justice.listing.courts.UpdateDefendantsForHearing;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.courts.UpdateOffencesForHearing;
import uk.gov.justice.listing.courts.UpdatedOffences;
import uk.gov.justice.listing.event.CourtListExportRequested;
import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishedCourtListStored;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.command.service.SystemIdMapperService;
import uk.gov.moj.cpp.listing.command.utils.CommandDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CommandOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CommandSimpleOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtApplicationToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtsAddedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CourtsDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtsDeletedOffenceToDomainCaseSimpleOffence;
import uk.gov.moj.cpp.listing.command.utils.CourtsUpdatedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.domain.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.aggregate.Application;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.PublishCourtListRequestAggregate;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188", "squid:CallToDeprecatedMethod"})
public class ListingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CommandToDomainConverter commandToDomainConverter;

    @Inject
    private CourtsDefendantToDomainConverter defendantUpdatedToDomainConverter;

    @Inject
    private CourtsUpdatedOffenceToDomainOffence courtsUpdatedOffenceToDomainOffence;

    @Inject
    private CourtsAddedOffenceToDomainOffence courtsAddedOffenceToDomainOffence;

    @Inject
    private CourtsDeletedOffenceToDomainCaseSimpleOffence courtsDeletedOffenceToDomainCaseSimpleOffence;

    @Inject
    private CommandDefendantToDomainConverter commandDefendantToDomainConverter;

    @Inject
    private CommandOffenceToDomainOffence commandOffenceToDomainOffence;

    @Inject
    private CommandSimpleOffenceToDomainOffence commandSimpleOffenceToDomainOffence;

    @Inject
    private CourtApplicationToDomainConverter courtApplicationToDomainConverter;

    @Inject
    private HearingTypeFactory hearingTypeFactory;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private Clock clock;

    @Inject
    private Enveloper enveloper;

    @Handles("listing.command.list-court-hearing-enriched")
    public void listCourtHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-court-hearing' received with payload {}", command.toObfuscatedDebugString());
        }
        final ListCourtHearingEnriched listCourtHearingEnriched = jsonObjectConverter.convert(payload, ListCourtHearingEnriched.class);
        LOGGER.debug("'listing.command.list-court-hearing' listCourtHearing: {}", listCourtHearingEnriched);
        final ListCourtHearing listCourtHearing = listCourtHearingEnriched.getListCourtHearing();
        final Map<UUID, CourtCentreDetails> courtCentres = listCourtHearingEnriched.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));
        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(command);
        LOGGER.info("'listing.command.list-court-hearing' courtCentres: {}", courtCentres);


        for (final HearingListingNeeds commandHearing : listCourtHearing.getHearings()) {

            final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = commandToDomainConverter.convert(commandHearing);
            final CourtCentreDetails courtCentre = courtCentres.get(domainHearing.getCourtCentreId());

            CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                    .withDefaultDuration(courtCentre.getDefaultDuration())
                    .withDefaultStartTime(courtCentre.getDefaultStartTime())
                    .withCourtCentreId(courtCentre.getId())
                    .build();

            updateHearingEventStream(command, commandHearing.getId(), (Hearing hearing) -> {
                final Stream<Object> listingEvents = hearing.list(domainHearing.getId(),
                        domainHearing.getType(),
                        domainHearing.getEstimatedMinutes(),
                        domainHearing.getListedCases(), domainHearing.getCourtCentreId(),
                        domainHearing.getJudiciary(),
                        domainHearing.getCourtRoomId().orElse(null),
                        domainHearing.getListingDirections().orElse(null),
                        domainHearing.getJurisdictionType(),
                        domainHearing.getProsecutorDatesToAvoid().orElse(null),
                        domainHearing.getReportingRestrictionReason().orElse(null),
                        domainHearing.getStartDateTime(),
                        domainHearing.getEndDate().orElse(null),
                        courtCentreDefaults,
                        domainHearing.getCourtApplications(),
                        domainHearing.getCourtApplicationPartyListingNeeds(),
                        hearingTypesIdDurationMap.get(domainHearing.getType().getId().toString())
                );

                final Stream<Object> allocationEvents = hearing.applyAllocationRules();

                return Stream.of(listingEvents, allocationEvents).flatMap(i -> i);
            });

        }
    }

    private AddHearingToCaseCommand getAddHearingToCaseCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AddHearingToCaseCommand.class);
    }

    private AddCourtApplicationToHearingCommand getAddApplicationToHearingCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AddCourtApplicationToHearingCommand.class);
    }

    private UpdateCourtApplicationCommand getUpdateCourtApplicationCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateCourtApplicationCommand.class);
    }

    @Handles("listing.command.update-hearing-for-listing-enriched")
    public void updateHearingForListing(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.handler.update-hearing-for-listing-enriched' received with payload {}", command.toObfuscatedDebugString());
        }
        final UpdateHearingForListingEnriched updateHearingForListingEnriched = jsonObjectConverter.convert(command.payloadAsJsonObject(), UpdateHearingForListingEnriched.class);
        final UpdateHearingForListing updateHearingForListing = updateHearingForListingEnriched.getUpdateHearingForListing();

        final CourtCentreDetails courtCentre = updateHearingForListingEnriched.getCourtCentreDetails();

        final LocalTime defaultStartTime = courtCentre.getDefaultStartTime();
        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(command);


        // Mandatory fields that always require a value
        final UUID hearingId = updateHearingForListing.getHearingId();
        final UUID courtCentreId = updateHearingForListing.getCourtCentreId();
        final Type type = convertTypeToDomain(updateHearingForListing.getType());
        final Integer defaultDuration = hearingTypesIdDurationMap.get(type.getId().toString());
        final JurisdictionType jurisdictionType = JurisdictionType.valueFor(updateHearingForListing.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new);
        final HearingLanguage hearingLanguage = valueFor(updateHearingForListing.getHearingLanguage().toString()).orElseThrow(IllegalArgumentException::new);

        final List<NonDefaultDay> nonDefaultDays = convertNonDefaultDaysToDomain(updateHearingForListing.getNonDefaultDays());
        final List<LocalDate> nonSittingDays = updateHearingForListing.getNonSittingDays();
        final List<JudicialRole> judiciary = convertJudicialRolesToDomain(updateHearingForListing.getJudiciary());

        // Fields that may not have a value
        final UUID courtRoomId = updateHearingForListing.getCourtRoomId().orElse(null);
        final LocalDate startDate = updateHearingForListing.getStartDate().orElse(null);
        final LocalDate endDate = updateHearingForListing.getEndDate().orElse(null);
        final LocalDate weekCommencingStartDate = updateHearingForListing.getWeekCommencingStartDate().orElse(null);
        final LocalDate weekCommencingEndDate = updateHearingForListing.getWeekCommencingEndDate().orElse(null);
        final Integer weekCommencingDurationInWeeks = updateHearingForListing.getWeekCommencingDurationInWeeks().orElse(null);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(command.metadata(), JsonValue.NULL);

        updateHearingEventStream(jsonEnvelope, hearingId, (Hearing hearing) -> {
            final Stream<Object> typeEvents = hearing.changeType(type, hearingId);
            final Stream<Object> jurisdictionTypeEvents = hearing.changeJurisdictionType(jurisdictionType, hearingId);
            final Stream<Object> hearingLanguageEvents = hearing.changeHearingLanguage(hearingLanguage, hearingId);

            final Stream<Object> startDateEvents = startDate != null ?
                    hearing.changeStartDate(startDate, hearingId) : hearing.removeStartDate(hearingId);
            final Stream<Object> endDateEvents = endDate != null ?
                    hearing.changeEndDate(endDate, hearingId) : hearing.removeEndDate(hearingId);


            final Stream<Object> nonDefaultDaysEvents = hearing.assignNonDefaultDays(nonDefaultDays, hearingId);

            final Stream<Object> nonSittingDaysEvents = hearing.assignNonSittingDays(nonSittingDays, hearingId);

            final Stream<Object> courtCentreEvents = hearing.changeCourtCentre(courtCentreId, hearingId);
            final Stream<Object> judiciaryEvents = judiciary != null ?
                    hearing.assignJudiciary(judiciary, hearingId) : hearing.removeJudiciary(hearingId);


            // Check endDate and court-room last as these are the key fields for allocation
            final Stream<Object> courtRoomEvents = courtRoomId != null ?
                    hearing.assignCourtRoom(courtRoomId, hearingId) : hearing.removeCourtRoom(hearingId);


            final Stream<Object> hearingDayEvents =
                    hearing.assignHearingDays(startDate, endDate, nonSittingDays, nonDefaultDays, defaultStartTime, defaultDuration, hearingId);

            final Stream<Object> weekCommencingDateEvents = weekCommencingStartDate != null && weekCommencingEndDate != null ?
                    hearing.changeWeekCommencingDate(weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingId) : hearing.removeWeekCommencingDates(hearingId);

            final Stream<Object> allocationEvents = hearing.applyAllocationRules();
            return Stream.of(typeEvents, jurisdictionTypeEvents, hearingLanguageEvents, startDateEvents, nonDefaultDaysEvents, endDateEvents,
                    nonSittingDaysEvents, courtCentreEvents, judiciaryEvents, courtRoomEvents, hearingDayEvents, allocationEvents, weekCommencingDateEvents).flatMap(i -> i);
        });
    }


    @Handles("listing.command.update-case-defendant-details")
    public void updateCaseDefendantDetails(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-case-defendant-details' received with payload {}", command.toObfuscatedDebugString());
        }
        final UpdateCaseDefendantDetails updateCaseDefendantDetails = jsonObjectConverter.convert(payload, UpdateCaseDefendantDetails.class);

        final uk.gov.justice.listing.courts.Defendant courtsDefendant = updateCaseDefendantDetails.getDefendant();
        final UUID caseId = courtsDefendant.getProsecutionCaseId();
        final uk.gov.moj.cpp.listing.domain.Defendant defendant = defendantUpdatedToDomainConverter.convert(courtsDefendant);

        updateCaseEventStream(command, caseId, (Case listingCase) ->
                listingCase.updateDefendant(caseId, defendant));
    }

    @Handles("listing.command.update-case-defendant-offences")
    public void updateCaseDefendantOffences(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-case-defendant-offences' received with payload {}", command.toObfuscatedDebugString());
        }

        final UpdateCaseDefendantOffences updateCaseDefendantOffences = jsonObjectConverter.convert(payload, UpdateCaseDefendantOffences.class);

        // Handle the updated offences
        List<UpdatedOffences> updatedOffence = updateCaseDefendantOffences.getUpdatedOffences();
        final List<CaseOffences> multipleCaseOffences = courtsUpdatedOffenceToDomainOffence.convert(updatedOffence);
        for (final CaseOffences caseOffences : multipleCaseOffences) {
            final UUID caseId = caseOffences.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.updateDefendantOffences(caseOffences)
            );
        }

        // Handle the deleted offences
        List<DeletedOffences> deletedOffence = updateCaseDefendantOffences.getDeletedOffences();
        final List<CaseSimpleOffences> offencesToBeDeleted = courtsDeletedOffenceToDomainCaseSimpleOffence.convert(deletedOffence);
        for (final CaseSimpleOffences offenceToBeDeleted : offencesToBeDeleted) {
            final UUID caseId = offenceToBeDeleted.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.deleteDefendantOffences(offenceToBeDeleted)
            );
        }

        // Handle the added offences
        List<AddedOffences> addedOffence = updateCaseDefendantOffences.getAddedOffences();
        final List<CaseOffences> addedOffences = courtsAddedOffenceToDomainOffence.convert(addedOffence);
        for (final CaseOffences caseBaseOffences : addedOffences) {
            final UUID caseId = caseBaseOffences.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.addedDefendantOffences(caseBaseOffences)
            );
        }
    }


    @Handles("listing.command.update-defendants-for-hearing")
    public void updateDefendantsForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-defendants-for-hearing' received with payload {}", command.toObfuscatedDebugString());
        }
        final UpdateDefendantsForHearing updateDefendantsForHearing = jsonObjectConverter.convert(payload, UpdateDefendantsForHearing.class);
        final UUID hearingId = updateDefendantsForHearing.getHearingId();
        final UUID caseId = updateDefendantsForHearing.getCaseId();
        final List<Defendant> defendants = updateDefendantsForHearing.getDefendants();
        final List<uk.gov.moj.cpp.listing.domain.Defendant> domainDefendants = commandDefendantToDomainConverter.convert(defendants);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.updateDefendants(caseId, domainDefendants));
    }

    @Handles("listing.command.update-offences-for-hearing")
    public void updateOffencesForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final UpdateOffencesForHearing updateOffencesForHearing = jsonObjectConverter.convert(payload, UpdateOffencesForHearing.class);
        final UUID hearingId = updateOffencesForHearing.getHearingId();
        final UUID caseId = updateOffencesForHearing.getCaseId();
        final UUID defendantId = updateOffencesForHearing.getDefendantId();
        final List<Offence> offences = updateOffencesForHearing.getOffences();
        final List<uk.gov.moj.cpp.listing.domain.Offence> domainOffences = commandOffenceToDomainOffence.convert(offences);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.updateOffences(caseId, defendantId, domainOffences));
    }

    @Handles("listing.command.delete-offences-for-hearing")
    public void deleteOffencesForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload %{}", command.toObfuscatedDebugString());
        }
        final DeleteOffencesForHearing deleteOffencesForHearing = jsonObjectConverter.convert(payload, DeleteOffencesForHearing.class);
        final UUID hearingId = deleteOffencesForHearing.getHearingId();
        final UUID caseId = deleteOffencesForHearing.getCaseId();
        final UUID defendantId = deleteOffencesForHearing.getDefendantId();
        final List<SimpleOffence> offences = deleteOffencesForHearing.getOffences();
        final List<uk.gov.moj.cpp.listing.domain.SimpleOffence> domainOffences = commandSimpleOffenceToDomainOffence.convert(offences);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.deleteOffences(caseId, defendantId, domainOffences));
    }

    @Handles("listing.command.add-offences-for-hearing")
    public void addOffencesForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload {}", command.toObfuscatedDebugString());
        }
        final AddOffencesForHearing addOffencesForHearing = jsonObjectConverter.convert(payload, AddOffencesForHearing.class);
        final UUID hearingId = addOffencesForHearing.getHearingId();
        final UUID caseId = addOffencesForHearing.getCaseId();
        final UUID defendantId = addOffencesForHearing.getDefendantId();
        final List<Offence> offences = addOffencesForHearing.getOffences();
        final List<uk.gov.moj.cpp.listing.domain.Offence> domainOffences = commandOffenceToDomainOffence.convert(offences);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.addOffences(caseId, defendantId, domainOffences));
    }

    @Handles("listing.command.change-judiciary-for-hearings")
    public void changeJudiciaryForHearings(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.change-judiciary-for-hearings' received with payload {}", command.toObfuscatedDebugString());
        }
        final ChangeJudiciaryForHearings changeJudiciaryForHearings = jsonObjectConverter.convert(command.payloadAsJsonObject(), ChangeJudiciaryForHearings.class);
        final List<UUID> hearingIds = changeJudiciaryForHearings.getHearings();
        final List<uk.gov.justice.core.courts.JudicialRole> judicialRolesCommand = changeJudiciaryForHearings.getJudiciary();
        final List<JudicialRole> judicialRoles = convertJudicialRolesToDomain(judicialRolesCommand);

        for (final UUID hearingId : hearingIds) {
            updateHearingEventStream(command, hearingId, (Hearing hearing) -> {
                final Stream<Object> judicialEvents = hearing.assignJudiciary(judicialRoles, hearingId);
                final Stream<Object> allocationEvents = hearing.applyAllocationRules();

                return Stream.of(allocationEvents, judicialEvents).flatMap(i -> i);
            });
        }
    }

    @Handles("listing.command.sequence-hearings")
    public void sequenceHearings(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.sequence-hearings' received with payload {}", command.toObfuscatedDebugString());
        }
        SequenceHearings sequenceHearings = jsonObjectConverter.convert(command.payloadAsJsonObject(), SequenceHearings.class);
        List<uk.gov.moj.cpp.listing.domain.SequenceHearing> sequenceHearingList = convertSequenceHearingsToDomain(sequenceHearings);

        for (uk.gov.moj.cpp.listing.domain.SequenceHearing sequenceHearing : sequenceHearingList) {
            updateHearingEventStream(command, sequenceHearing.getId(), (Hearing hearing) ->
                    hearing.sequenceHearingDays(sequenceHearing));
        }
    }

    @Handles("listing.command.update-court-application-for-hearings")
    public void updateCourtApplicationForHearings(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-court-application-for-hearings' received with payload {}", command.toObfuscatedDebugString());
        }

        final UpdateCourtApplicationForHearings updateCourtApplicationForHearings = jsonObjectConverter.convert
                (command.payloadAsJsonObject(), UpdateCourtApplicationForHearings.class);
        final List<UUID> hearingIds = updateCourtApplicationForHearings.getHearingIds();
        final uk.gov.justice.listing.courts.CourtApplication courtApplication = updateCourtApplicationForHearings.getCourtApplication();

        final uk.gov.moj.cpp.listing.domain.CourtApplication courtApplicationDomain = courtApplicationToDomainConverter.convertListingCoreCourtApplication(courtApplication);

        for (final UUID hearingId : hearingIds) {
            updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                    hearing.updateCourtApplication(hearingId, courtApplicationDomain));
        }

    }

    @Handles("listing.command.add-court-application-for-hearing")
    public void addCourtApplicationForHearing(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-court-application-for-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final AddCourtApplicationForHearing addCourtApplicationToHearing = jsonObjectConverter.convert
                (command.payloadAsJsonObject(), AddCourtApplicationForHearing.class);
        final UUID hearingId = addCourtApplicationToHearing.getHearingId();
        final CourtApplication courtApplication = addCourtApplicationToHearing.getCourtApplication();

        final uk.gov.moj.cpp.listing.domain.CourtApplication courtApplicationDomain = courtApplicationToDomainConverter.convert(courtApplication);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.addCourtApplication(hearingId, courtApplicationDomain));


    }

    @Handles("listing.command.add-court-application-to-hearing")
    public void addCourtApplicationToHearing(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.add-court-application-to-hearing' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final AddCourtApplicationToHearingCommand command = getAddApplicationToHearingCommand(commandEnvelope);

        final UUID applicationId = command.getApplicationId();
        final UUID hearingId = command.getHearingId();
        updateApplicationEventStream(commandEnvelope, applicationId, (Application application) ->
                application.addToHearing(applicationId, hearingId));
    }

    @Handles("listing.command.update-court-application")
    public void updateCourtApplication(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-court-application' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final UpdateCourtApplicationCommand command = getUpdateCourtApplicationCommand(commandEnvelope);
        final uk.gov.moj.cpp.listing.domain.CourtApplication courtApplicationDomain = courtApplicationToDomainConverter.convert(command.getCourtApplication());

        final UUID applicationId = courtApplicationDomain.getId();
        updateApplicationEventStream(commandEnvelope, applicationId, (Application application) ->
                application.update(courtApplicationDomain));
    }

    @Handles("listing.command.add-defendants-to-court-proceedings")
    public void addDefendantsToCourtProceedings(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-defendants-to-court-proceedings' received with payload {}", command.toObfuscatedDebugString());
        }

        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = jsonObjectConverter.convert
                (command.payloadAsJsonObject(), AddDefendantsToCourtProceedings.class);
        final List<uk.gov.justice.core.courts.Defendant> defendantList = addDefendantsToCourtProceedings.getDefendants();
        final List<ListHearingRequest> listHearingRequestsList = addDefendantsToCourtProceedings.getListHearingRequests();
        final List<uk.gov.moj.cpp.listing.domain.Defendant> defendantDomainList = commandToDomainConverter.convertDefendant(defendantList, listHearingRequestsList);

        for (final uk.gov.moj.cpp.listing.domain.Defendant defendantDomain : defendantDomainList) {
            final UUID caseId = defendantDomain.getProsecutionCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.addedDefendantForCourtProceedings(caseId, defendantDomain));
        }

    }

    @Handles("listing.command.add-defendants-to-court-proceedings-for-hearing")
    public void addDefendantsToCourtProceedingsForHearing(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-defendants-to-court-proceedings-for-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final AddDefendantsToCourtProceedingsForHearing addDefendantsToCourtProceedingsForHearing = jsonObjectConverter.convert
                (command.payloadAsJsonObject(), AddDefendantsToCourtProceedingsForHearing.class);
        final UUID hearingId = addDefendantsToCourtProceedingsForHearing.getHearingId();
        final UUID caseId = addDefendantsToCourtProceedingsForHearing.getCaseId();
        final List<Defendant> defendants = addDefendantsToCourtProceedingsForHearing.getDefendants();
        final List<uk.gov.moj.cpp.listing.domain.Defendant> domainDefendants = commandDefendantToDomainConverter.convert(defendants);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.addDefendantsForCourtProceedings(caseId, domainDefendants));
    }

    @Handles("listing.command.restrict-court-list")
    public void restrictFromCourtList(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.restrict-court-list' received with payload {}", command.toObfuscatedDebugString());
        }

        final RestrictCourtList restrictCourtList = jsonObjectConverter.convert(command.payloadAsJsonObject(), RestrictCourtList.class);

        updateHearingEventStream(command, restrictCourtList.getHearingId(), (Hearing hearing) ->
                hearing.restrictDetailsFromCourt(restrictCourtList.getHearingId(), convertRestrictCourtListToDomain(restrictCourtList)));
    }

    @Handles("listing.command.add-hearing-to-case")
    public void addHearingToCase(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-hearing-to-case' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final AddHearingToCaseCommand command = getAddHearingToCaseCommand(commandEnvelope);

        final UUID caseId = command.getCaseId();
        final UUID hearingId = command.getHearingId();
        updateCaseEventStream(commandEnvelope, caseId, (Case listingCase) ->
                listingCase.addHearing(caseId, hearingId));
    }

    @Handles("listing.command.eject-case-or-application")
    public void ejectCaseOrApplication(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-hearing-to-case' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }

        final CaseOrApplicationEjected command = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), CaseOrApplicationEjected.class);
        final Optional<UUID> caseId = command.getProsecutionCaseId();
        final List<UUID> hearingIds = command.getHearingIds();
        final Optional<UUID> applicationId = command.getApplicationId();
        if (caseId.isPresent()) {
            updateCaseEventStream(commandEnvelope, caseId.get(), (Case listingCase) ->
                    listingCase.ejectCase(hearingIds, caseId.get(), command.getRemovalReason()));
        }
        if (applicationId.isPresent()) {
            updateApplicationEventStream(commandEnvelope, applicationId.get(), (Application application) ->
                    application.ejectApplication(hearingIds, applicationId.get(), (command.getRemovalReason())));
        }
    }


    @Handles("listing.command.court-list-request-export")
    public void courtListRequestExport(final JsonEnvelope commandEnvelope) throws EventStreamException {

        final CourtListRequestExport courtListRequestExport =
                jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), CourtListRequestExport.class);

        final UUID courtListId = systemIdMapperService.getCourtListId(
                courtListRequestExport.getCourtCentreId(),
                courtListRequestExport.getPublishCourtListType(),
                courtListRequestExport.getStartDate());

        final CourtListExportRequested event = new CourtListExportRequested(
                courtListRequestExport.getCourtCentreId(),
                courtListId,
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(courtListRequestExport.getPublishCourtListType().name()),
                now(),
                courtListRequestExport.getStartDate()
        );

        final Stream<Object> events = streamOf(event);

        final UUID streamId = event.getCourtListId();
        final EventStream eventStream = eventSource.getStreamById(streamId);

        eventStream.append(events.map(enveloper.withMetadataFrom(commandEnvelope)));
    }

    @Handles("listing.command.record-court-list-export-successful")
    public void recordCourtListExportSuccessful(final JsonEnvelope commandEnvelope) throws EventStreamException {
        final RecordCourtListExportSuccessful command =
                jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), RecordCourtListExportSuccessful.class);

        final PublishCourtListExportSuccessful event = new PublishCourtListExportSuccessful(
                command.getCourtCentreId(),
                command.getCourtListId(),
                command.getExportedTime(),
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(command.getPublishCourtListType().name()),
                command.getStartDate()
        );

        final Stream<Object> events = streamOf(event);

        final UUID streamId = event.getCourtListId();
        final EventStream eventStream = eventSource.getStreamById(streamId);

        eventStream.append(events.map(enveloper.withMetadataFrom(commandEnvelope)));
    }

    @Handles("listing.command.record-court-list-export-failed")
    public void recordCourtListExportFailed(final JsonEnvelope commandEnvelope) throws EventStreamException {
        final RecordCourtListExportFailed command =
                jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), RecordCourtListExportFailed.class);

        final PublishCourtListExportFailed event = new PublishCourtListExportFailed(
                command.getCourtCentreId(),
                command.getCourtListId(),
                command.getErrorMessage(),
                command.getFailedTime(),
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(command.getPublishCourtListType().name()),
                command.getStartDate()
        );

        final Stream<Object> events = streamOf(event);

        final UUID streamId = event.getCourtListId();
        final EventStream eventStream = eventSource.getStreamById(streamId);

        eventStream.append(events.map(enveloper.withMetadataFrom(commandEnvelope)));
    }

    @Handles("listing.command.publish-court-list")
    public void publishCourtList(final JsonEnvelope commandEnvelope) throws EventStreamException {
        final PublishCourtList publishCourtList =
                jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), PublishCourtList.class);
        final UUID publishCourtListRequestId = randomUUID();
        final EventStream eventStream = eventSource.getStreamById(publishCourtListRequestId);
        final PublishCourtListRequestAggregate publishCourtListRequestAggregate = aggregateService.get(eventStream, PublishCourtListRequestAggregate.class);
        final Stream<Object> events = publishCourtListRequestAggregate.recordCourtListRequested(
                publishCourtListRequestId,
                publishCourtList.getCourtCentreId(),
                publishCourtList.getStartDate(),
                publishCourtList.getEndDate(),
                valueOf(publishCourtList.getPublishCourtListType().toString()),
                clock.now());
        appendEventsToStream(commandEnvelope, eventStream, events);
    }

    @Handles("listing.command.record-court-list-produced")
    public void recordCourtListProduced(final JsonEnvelope commandEnvelope) throws EventStreamException {
        final RecordCourtListProduced recordCourtListProduced =
                jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), RecordCourtListProduced.class);

        final EventStream eventStream = eventSource.getStreamById(recordCourtListProduced.getPublishCourtListRequestId());
        final PublishCourtListRequestAggregate publishCourtListRequestAggregate = aggregateService.get(eventStream, PublishCourtListRequestAggregate.class);
        final Stream<Object> events = publishCourtListRequestAggregate.recordCourtListProduced(
                recordCourtListProduced.getPublishCourtListRequestId(),
                recordCourtListProduced.getCourtCentreId(),
                recordCourtListProduced.getCourtListFileId(),
                recordCourtListProduced.getCourtListFileName(),
                valueOf(recordCourtListProduced.getPublishCourtListType().toString()),
                recordCourtListProduced.getProducedTime(),
                parse(recordCourtListProduced.getPublishDate()));
        appendEventsToStream(commandEnvelope, eventStream, events);
    }

    @Handles("listing.command.publish-court-lists-for-crown-courts")
    @SuppressWarnings("WeakerAccess") // Required for framework
    public void publishFinalCourtListsForAllCrownCourts(final JsonEnvelope commandEnvelope) {
        final List<CourtCentreDetails> crownCourtCentres = getAllCrownCourtCentres(commandEnvelope);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Attempting to request publication of the final court lists for [{}] crown court centres ...", crownCourtCentres.size());
        }
        crownCourtCentres
                .forEach(courtCentreDetails -> publishFinalCourtList(commandEnvelope.metadata(), courtCentreDetails));
    }

    @Handles("listing.command.store-published-court-list")
    public void storePublishedCourtList(final JsonEnvelope commandEnvelope) throws EventStreamException {

        final StorePublishedCourtList command =
                jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), StorePublishedCourtList.class);

        final UUID courtListId = systemIdMapperService.getCourtListId(
                command.getCourtCentreId(),
                command.getPublishCourtListType(),
                command.getStartDate());

        final PublishedCourtListStored event = new PublishedCourtListStored(
                command.getCourtCentreId(),
                courtListId,
                command.getCourtListJson(),
                ZonedDateTime.now(),
                uk.gov.justice.listing.event.PublishCourtListType.valueOf(command.getPublishCourtListType().name()),
                command.getStartDate()
        );

        final Stream<Object> events = streamOf(event);

        final UUID streamId = courtListId;
        final EventStream eventStream = eventSource.getStreamById(streamId);

        eventStream.append(events.map(enveloper.withMetadataFrom(commandEnvelope)));
    }

    @VisibleForTesting
    void setClock(final Clock clock) {
        this.clock = clock;
    }


    private void publishFinalCourtList(final Metadata commandMetaData, CourtCentreDetails courtCentreDetails) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Attempting to request publication of the final court list for crown court centre [{}] ...", courtCentreDetails.getId());
        }
        try {
            publishCourtList(envelopeFrom(commandMetaData, asJson(generatePublishCourtListCommand(courtCentreDetails))));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Successfully requested publication of the final court list for crown court centre [{}].", courtCentreDetails.getId());
            }
        } catch (EventStreamException | RuntimeException e) {
            // This should be robust, so to allow subsequent attempts.
            if (LOGGER.isErrorEnabled()) {
                final String message
                        = String.format(
                        "Exception thrown While trying to publish the final court list for Court Centre [%s]",
                        courtCentreDetails.getId());
                LOGGER.error(message, e);
            }
        }
    }

    private PublishCourtList generatePublishCourtListCommand(CourtCentreDetails courtCentreDetails) {

        final ZonedDateTime zonedNow = clock.now();
        final LocalDate today = zonedNow.toLocalDate();
        final LocalDate nextWorkingDay = DateAndTimeUtils.getNextWorkingDay(today);
        return PublishCourtList.publishCourtList()
                .withCourtCentreId(courtCentreDetails.getId())
                .withStartDate(nextWorkingDay)
                .withEndDate(nextWorkingDay)
                .withPublishCourtListType(PublishCourtListType.FINAL)
                .withRequestedTime(Optional.of(zonedNow))
                .build();

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private List<uk.gov.moj.cpp.listing.domain.SequenceHearing> convertSequenceHearingsToDomain(SequenceHearings sequenceHearingsCommand) {
        List<uk.gov.moj.cpp.listing.domain.SequenceHearing> domainSequenceHearings = Collections.emptyList();
        if (sequenceHearingsCommand != null && !sequenceHearingsCommand.getHearings().isEmpty()) {
            domainSequenceHearings = sequenceHearingsCommand.getHearings()
                    .stream()
                    .map(sh -> uk.gov.moj.cpp.listing.domain.SequenceHearing.sequenceHearing()
                            .withHearingDays(sh.getSequenceHearingDays()
                                    .stream()
                                    .map(shd -> HearingDay.hearingDay()
                                            .withHearingDate(shd.getHearingDate())
                                            .withSequence(shd.getSequence())
                                            .build())
                                    .collect(toList()))
                            .withId(sh.getId())
                            .build())
                    .collect(toList());
        }
        return domainSequenceHearings;

    }

    private List<JudicialRole> convertJudicialRolesToDomain(List<uk.gov.justice.core.courts.JudicialRole> commandJudiciary) {
        List<JudicialRole> domainJudiciary = Collections.emptyList();
        if (commandJudiciary != null && !commandJudiciary.isEmpty()) {
            domainJudiciary = commandJudiciary.stream().map(jr -> JudicialRole.judicialRole()
                    .withIsBenchChairman(jr.getIsBenchChairman())
                    .withIsDeputy(jr.getIsDeputy())
                    .withJudicialId(jr.getJudicialId())
                    .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                            .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                            .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                            .build())
                    .build()).collect(toList());
        }
        return domainJudiciary;
    }

    private Type convertTypeToDomain(uk.gov.justice.core.courts.HearingType hearingType) {
        return Type.type()
                .withId(hearingType.getId())
                .withDescription(hearingType.getDescription())
                .build();
    }

    private List<NonDefaultDay> convertNonDefaultDaysToDomain(List<uk.gov.justice.listing.commands.NonDefaultDay> commandDefaultDays) {
        List<NonDefaultDay> domainDefaultDays = Collections.emptyList();
        if (commandDefaultDays != null && !commandDefaultDays.isEmpty()) {
            domainDefaultDays = commandDefaultDays.stream().map(ndd -> NonDefaultDay.nonDefaultDay()
                    .withStartTime(ndd.getStartTime())
                    .withDuration(ndd.getDuration())
                    .withCourtScheduleId(ndd.getCourtScheduleId())
                    .withCourtRoomId(ndd.getCourtRoomId())
                    .withOucode(ndd.getOucode())
                    .withSession(ndd.getSession())
                    .build())
                    .collect(toList());
        }
        return domainDefaultDays;
    }

    private uk.gov.moj.cpp.listing.domain.RestrictCourtList convertRestrictCourtListToDomain(RestrictCourtList restrictCourtList) {

        return uk.gov.moj.cpp.listing.domain.RestrictCourtList.restrictCourtList()
                .withCaseIds(restrictCourtList.getCaseIds())
                .withDefendantIds(restrictCourtList.getDefendantIds())
                .withHearingId(restrictCourtList.getHearingId())
                .withOffenceIds(restrictCourtList.getOffenceIds())
                .withCourtApplicationApplicantIds(restrictCourtList.getCourtApplicationApplicantIds())
                .withCourtApplicatonIds(restrictCourtList.getCourtApplicationIds())
                .withCourtApplicatonRespondentIds(restrictCourtList.getCourtApplicationRespondentIds())
                .withCourtApplicationType(restrictCourtList.getCourtApplicationType().orElse(null))
                .withRestrictFromCourtList(restrictCourtList.getRestrictCourtList())
                .build();


    }

    private void updateHearingEventStream(final JsonEnvelope command, final UUID hearingId,
                                          final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final Hearing hearing = aggregateService.get(eventStream, Hearing.class);

        final Stream<Object> events = aggregatorFunction.apply(hearing);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    private void updateCaseEventStream(final JsonEnvelope command, final UUID caseId,
                                       final Function<Case, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final Case listingCase = aggregateService.get(eventStream, Case.class);

        final Stream<Object> events = aggregatorFunction.apply(listingCase);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));

    }

    private void updateApplicationEventStream(final JsonEnvelope command, final UUID applicationId,
                                              final Function<Application, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final Application application = aggregateService.get(eventStream, Application.class);

        final Stream<Object> events = aggregatorFunction.apply(application);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    private List<CourtCentreDetails> getAllCrownCourtCentres(final JsonEnvelope eventEnvelope) {
        final JsonEnvelope responseEnvelope = referenceDataService.getAllCrownCourtCentres(eventEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();

        return jsonObject.getJsonArray("organisationunits")
                .stream()
                .filter(x -> x.getValueType() == JsonValue.ValueType.OBJECT)
                .map(x -> (JsonObject) x)
                .map(this::toCourtCentreDetails)
                .collect(Collectors.toList());
    }


    // Justin: "These private methods are only used by the export command. Move to a dedicated 'helper' class."
    // TODO [SCSL-176] I'd like to find a common place for this, accessible to both this class
    // as well as uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory.
    // I thought that 'listing-domain-common' module would be a good choice, but
    // we don't have CourtCentreDetails in there.
    private CourtCentreDetails toCourtCentreDetails(JsonObject courtCentreDetailsAsJson) {
        final UUID courtCentreId = UUID.fromString(courtCentreDetailsAsJson.getString("id"));
        final Integer defaultDurationMinutes =
                convertHoursAndMinutesToMinutes(courtCentreDetailsAsJson.getString("defaultDurationHrs"))
                        .orElse(0);
        final LocalTime defaultStartTime = LocalTime.parse(courtCentreDetailsAsJson.getString("defaultStartTime"));

        return CourtCentreDetails.courtCentreDetails()
                .withDefaultStartTime(defaultStartTime)
                .withDefaultDuration(defaultDurationMinutes)
                .withId(courtCentreId)
                .build();

    }


}
