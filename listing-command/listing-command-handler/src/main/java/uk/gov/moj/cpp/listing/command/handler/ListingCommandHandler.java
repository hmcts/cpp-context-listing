package uk.gov.moj.cpp.listing.command.handler;

import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.listing.event.PublishCourtListType.valueOf;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.eventsourcing.source.core.Events.streamOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.moj.cpp.listing.command.utils.json.PublishCourtListJsonSupport.asJson;
import static uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay;
import static uk.gov.moj.cpp.listing.domain.HearingLanguage.valueFor;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.BST;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.convertHoursAndMinutesToMinutes;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.getNextWorkingDay;
import static uk.gov.moj.cpp.listing.domain.utils.HearingUtil.getAdjustedDuration;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateCaseMarkersCommand;
import uk.gov.justice.core.courts.UpdateCaseMarkersToHearingCommand;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.CourtListRequestExport;
import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.listing.commands.ModifyHearingCounselCommand;
import uk.gov.justice.listing.commands.Offence;
import uk.gov.justice.listing.commands.PublishCourtList;
import uk.gov.justice.listing.commands.PublishCourtListType;
import uk.gov.justice.listing.commands.RecordCourtListExportFailed;
import uk.gov.justice.listing.commands.RecordCourtListExportSuccessful;
import uk.gov.justice.listing.commands.RecordCourtListProduced;
import uk.gov.justice.listing.commands.SimpleOffence;
import uk.gov.justice.listing.commands.StorePublishedCourtList;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.AddCasesToHearing;
import uk.gov.justice.listing.courts.AddCourtApplicationForHearing;
import uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand;
import uk.gov.justice.listing.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.listing.courts.AddDefendantsToCourtProceedingsForHearing;
import uk.gov.justice.listing.courts.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.AddOffencesForHearing;
import uk.gov.justice.listing.courts.AddedOffences;
import uk.gov.justice.listing.courts.CancelHearingDays;
import uk.gov.justice.listing.courts.CaseOrApplicationEjected;
import uk.gov.justice.listing.courts.Cases;
import uk.gov.justice.listing.courts.ChangeJudiciaryForHearings;
import uk.gov.justice.listing.courts.DeleteOffencesForHearing;
import uk.gov.justice.listing.courts.DeletedOffences;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.HearingVacateTrial;
import uk.gov.justice.listing.courts.LinkedToCases;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.courts.RestrictCourtList;
import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.listing.courts.SequenceHearings;
import uk.gov.justice.listing.courts.UpdateCaseDefendantDetails;
import uk.gov.justice.listing.courts.UpdateCaseDefendantOffences;
import uk.gov.justice.listing.courts.UpdateCaseIdentifierWithHearings;
import uk.gov.justice.listing.courts.UpdateCourtApplicationCommand;
import uk.gov.justice.listing.courts.UpdateCourtApplicationForHearings;
import uk.gov.justice.listing.courts.UpdateDefendantsForHearing;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.courts.UpdateHearingToCaseCommand;
import uk.gov.justice.listing.courts.UpdateLinkedCaseInHearing;
import uk.gov.justice.listing.courts.UpdateLinkedCases;
import uk.gov.justice.listing.courts.UpdateOffencesForHearing;
import uk.gov.justice.listing.courts.UpdatedOffences;
import uk.gov.justice.listing.courts.VacateTrialEnriched;
import uk.gov.justice.listing.event.CourtListExportRequested;
import uk.gov.justice.listing.event.HearingCounselModified;
import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishedCourtListStored;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
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
import uk.gov.moj.cpp.listing.command.factory.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.factory.HearingFactory;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.command.service.UUIDService;
import uk.gov.moj.cpp.listing.command.utils.CaseMarkersToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CasesToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CommandDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CommandOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CommandSimpleOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtApplicationToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtsAddedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.CourtsDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.CourtsDeletedOffenceToDomainCaseSimpleOffence;
import uk.gov.moj.cpp.listing.command.utils.CourtsUpdatedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.utils.HearingDaysToDomainConverter;
import uk.gov.moj.cpp.listing.command.utils.NonDefaultDayDurationBuilder;
import uk.gov.moj.cpp.listing.command.utils.ProsecutionCaseDefendantOffenceIdsBuilder;
import uk.gov.moj.cpp.listing.command.utils.ProsecutionCasesBuilder;
import uk.gov.moj.cpp.listing.command.utils.RotaSlotToNonDefaultDayConverter;
import uk.gov.moj.cpp.listing.command.utils.hearing.ExtendHearingUtils;
import uk.gov.moj.cpp.listing.common.azure.ProvisionalBookingService;
import uk.gov.moj.cpp.listing.common.azure.adapter.RotaSLServiceAdapter;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;
import uk.gov.moj.cpp.listing.domain.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.aggregate.Application;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.PublishCourtListRequestAggregate;
import uk.gov.moj.cpp.platform.data.utils.date.MeridianUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188", "squid:CallToDeprecatedMethod", "squid:S2629", "squid:S00112"})
public class ListingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingCommandHandler.class);
    public static final String HEARING_ID = "hearingId";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CommandToDomainConverter commandToDomainConverter;

    @Inject
    private CourtsDefendantToDomainConverter defendantUpdatedToDomainConverter;

    @Inject
    private CaseMarkersToDomainConverter caseMarkersToDomainConverter;

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
    private CasesToDomainConverter casesToDomainConverter;

    @Inject
    private HearingDaysToDomainConverter hearingDaysToDomainConverter;

    @Inject
    private ProsecutionCasesBuilder prosecutionCasesBuilder;

    @Inject
    private ExtendHearingUtils extendHearingUtils;

    @Inject
    private ProsecutionCaseDefendantOffenceIdsBuilder prosecutionCaseDefendantOffenceIdsBuilder;

    @Inject
    private HearingTypeFactory hearingTypeFactory;

    @Inject
    private HearingFactory hearingFactory;

    @Inject
    ProvisionalBookingService provisionalBookingService;

    @Inject
    private NonDefaultDayDurationBuilder nonDefaultDayDurationBuilder;

    @Inject
    private RotaSlotToNonDefaultDayConverter rotaSlotToNonDefaultDayConverter;

    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    private static final String LEGAL_AID_STATUS = "legalAidStatus";

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private UUIDService uuidService;

    @Inject
    private Clock clock;

    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private RotaSLServiceAdapter rotaSLServiceAdapter;

    private static final String APPLICATION_ID = "applicationId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String REMOVAL_REASON = "removalReason";

    @SuppressWarnings("squid:S3655")
    @Handles("listing.command.list-court-hearing-enriched")
    public void listCourtHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-court-hearing' received with payload {}", command.toObfuscatedDebugString());
        }
        final ListCourtHearingEnriched listCourtHearingEnriched = jsonObjectConverter.convert(payload, ListCourtHearingEnriched.class);
        LOGGER.debug("'listing.command.list-court-hearing' listCourtHearing: {}", listCourtHearingEnriched);
        final Optional<String> adjournedFromDate = listCourtHearingEnriched.getAdjournedFromDate();
        final ListCourtHearing listCourtHearing = listCourtHearingEnriched.getListCourtHearing();
        final Map<UUID, CourtCentreDetails> courtCentres = listCourtHearingEnriched.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));
        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(command);
        LOGGER.info("'listing.command.list-court-hearing' courtCentres: {}", courtCentres);

        for (final HearingListingNeeds commandHearing : listCourtHearing.getHearings()) {

            List<uk.gov.justice.listing.commands.NonDefaultDay> nonDefaultDaysList = new ArrayList<>();
            Optional<UUID> bookingReference = empty();
            final boolean isSlotsBooked = isNotEmpty(commandHearing.getBookedSlots());

            final CourtCentre defaultCourtCentre = commandHearing.getCourtCentre();

            if (isSlotsBooked) {
                final List<uk.gov.justice.listing.commands.NonDefaultDay> finalNonDefaultDaysList = nonDefaultDaysList;
                commandHearing.getBookedSlots()
                        .forEach(b -> finalNonDefaultDaysList.add(rotaSlotToNonDefaultDayConverter.convert(b, defaultCourtCentre)));
                nonDefaultDaysList = finalNonDefaultDaysList;
            } else {
                if (listCourtHearing.getAdjournedFromDate().isPresent() && commandHearing.getBookingReference().isPresent()) {
                    bookingReference = commandHearing.getBookingReference();
                    final List<CourtSchedule> courtScheduleList = getCourtSchedules(bookingReference.get().toString());
                    Collections.sort(courtScheduleList);
                    generateNonDefaultDays(nonDefaultDaysList, courtScheduleList, commandHearing);
                }

                final uk.gov.justice.core.courts.JurisdictionType jurisdictionType = commandHearing.getJurisdictionType();
                if (isSchedulingAndListingUpdateRequired(jurisdictionType, nonDefaultDaysList)) {
                    nonDefaultDaysList = nonDefaultDayDurationBuilder.updateNonDefaultDayWithNewDuration(nonDefaultDaysList, commandHearing.getEstimatedMinutes());
                }
            }
            final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = commandToDomainConverter.convert(commandHearing, convertNonDefaultDaysToDomain(nonDefaultDaysList), listCourtHearing.getShadowListedOffences());
            final CourtCentreDetails courtCentre = courtCentres.get(domainHearing.getCourtCentreId());

            final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                    .withDefaultDuration(courtCentre.getDefaultDuration())
                    .withDefaultStartTime(courtCentre.getDefaultStartTime())
                    .withCourtCentreId(courtCentre.getId())
                    .build();

            final Optional<UUID> finalBookingReference = bookingReference;
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
                        hearingTypesIdDurationMap.get(domainHearing.getType().getId().toString()),
                        adjournedFromDate,
                        domainHearing.getWeekCommencingStartDate(),
                        domainHearing.getWeekCommencingEndDate(),
                        domainHearing.getWeekCommencingDurationInWeeks(),
                        domainHearing.getNonDefaultDays(),
                        isSlotsBooked
                );

                final Stream<Object> allocationEvents = hearing.applyAllocationRules(finalBookingReference);

                return Stream.of(listingEvents, allocationEvents).flatMap(i -> i);
            });

        }
    }

    private AddHearingToCaseCommand getAddHearingToCaseCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AddHearingToCaseCommand.class);
    }

    private UpdateHearingToCaseCommand getUpdateHearingToCaseCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateHearingToCaseCommand.class);
    }

    private AddCourtApplicationToHearingCommand getAddApplicationToHearingCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AddCourtApplicationToHearingCommand.class);
    }

    private UpdateCaseMarkersToHearingCommand getUpdateCaseMarkerToHearingCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateCaseMarkersToHearingCommand.class);
    }

    private UpdateCourtApplicationCommand getUpdateCourtApplicationCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateCourtApplicationCommand.class);
    }

    private UpdateCaseMarkersCommand getUpdateCaseMarkersCommand(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateCaseMarkersCommand.class);
    }

    @Handles("listing.command.vacate-trial-enriched")
    public void vacateTrial(final JsonEnvelope command) throws EventStreamException {

        LOGGER.info("'listing.command.vacate-trial-enriched' received with payload {}", command.toObfuscatedDebugString());

        final VacateTrialEnriched vacateTrialEnriched = jsonObjectConverter.convert(command.payloadAsJsonObject(), VacateTrialEnriched.class);

        updateHearingEventStream(command, vacateTrialEnriched.getHearingId(), (Hearing hearing) ->
                hearing.vacateTrial(vacateTrialEnriched.getHearingId(), vacateTrialEnriched.getVacatedTrialReasonId()));

    }

    @Handles("listing.command.hearing-vacate-trial")
    public void hearingVacateTrial(final JsonEnvelope command) throws EventStreamException {
        LOGGER.info("'listing.command.hearing-vacate-trial' received with payload {}", command.toObfuscatedDebugString());
        final HearingVacateTrial hearingVacateTrial = jsonObjectConverter.convert(command.payloadAsJsonObject(), HearingVacateTrial.class);

        updateHearingEventStream(command, hearingVacateTrial.getHearingId(), (Hearing hearing) ->
                hearing.hearingVacateTrial(hearingVacateTrial.getVacatedTrialReasonId()));
    }

    @SuppressWarnings({"squid:S3776"})
    @Handles("listing.command.update-hearing-for-listing-enriched")
    public void updateHearingForListing(final JsonEnvelope command) throws EventStreamException {

        LOGGER.info("'listing.command.handler.update-hearing-for-listing-enriched' received with payload {}", command.toObfuscatedDebugString());

        final UpdateHearingForListingEnriched updateHearingForListingEnriched = jsonObjectConverter.convert(command.payloadAsJsonObject(), UpdateHearingForListingEnriched.class);
        UpdateHearingForListing updateHearingForListing = updateHearingForListingEnriched.getUpdateHearingForListing();

        final CourtCentreDetails courtCentre = updateHearingForListingEnriched.getCourtCentreDetails();
        if (isSchedulingAndListingUpdateRequired(updateHearingForListing.getJurisdictionType(), updateHearingForListing.getNonDefaultDays())) {
            updateHearingForListing = nonDefaultDayDurationBuilder.buildNewUpdateHearingForListingWithNewNonDefaultDays(updateHearingForListing, updateHearingForListing.getNonDefaultDays());

        }

        final LocalTime defaultStartTime = courtCentre.getDefaultStartTime();
        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap(command);

        // Mandatory fields that always require a value
        final UUID courtCentreId = getCourtCentreId(updateHearingForListing);
        final UUID hearingId = updateHearingForListing.getHearingId();
        final Type type = convertTypeToDomain(updateHearingForListing.getType());
        final Integer defaultDuration = hearingTypesIdDurationMap.get(type.getId().toString());
        final JurisdictionType jurisdictionType = JurisdictionType.valueFor(updateHearingForListing.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new);
        final HearingLanguage hearingLanguage = valueFor(updateHearingForListing.getHearingLanguage().toString()).orElseThrow(IllegalArgumentException::new);

        final List<LocalDate> nonSittingDays = updateHearingForListing.getNonSittingDays();
        final List<JudicialRole> judiciary = convertJudicialRolesToDomain(updateHearingForListing.getJudiciary());

        // Fields that may not have a value
        final UUID courtRoomId = getCourtRoomId(updateHearingForListing);
        final LocalDate startDate = updateHearingForListing.getStartDate().orElse(null);
        final LocalDate endDate = updateHearingForListing.getEndDate().orElse(null);
        final LocalDate weekCommencingStartDate = updateHearingForListing.getWeekCommencingStartDate().orElse(null);
        final LocalDate weekCommencingEndDate = updateHearingForListing.getWeekCommencingEndDate().orElse(null);
        final Integer weekCommencingDurationInWeeks = updateHearingForListing.getWeekCommencingDurationInWeeks().orElse(null);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(command.metadata(), JsonValue.NULL);

        final uk.gov.justice.listing.events.Hearing storedHearing = hearingFactory.getHearingById(hearingId, command);

        final boolean hasVideoLink = updateHearingForListing.getHasVideoLink().orElse(false);
        final String publicListNote = updateHearingForListing.getPublicListNote().orElse(null);

        final List<NonDefaultDay> nonDefaultDays = generateNewNonDefaultDays(command, updateHearingForListing, nonSittingDays, endDate, startDate,
                defaultDuration, jurisdictionType);

        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withRoomId(courtRoomId).build();
        final Optional<String> panelFromCommand = updateHearingForListing.getPanel();

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

            Optional<String> panel = empty();
            if(JurisdictionType.MAGISTRATES.equals(jurisdictionType)) {
                final JsonObject organisationUnitJsonObject = courtCentreFactory.getOrganisationUnit(courtCentreId, command);
                final String ouCode = organisationUnitJsonObject.getString("oucode");
                setJudiciaryFromRotaSLIfJudiciaryIsEmptyAndMagistrates(judiciary, courtRoomId, nonDefaultDays, ouCode);
                panel = rotaSLServiceAdapter.getPanelInfo(panelFromCommand, startDate, endDate, courtRoomId, ouCode);
            }

            final Stream<Object> judiciaryEvents = getJudiciaryEvents(hearingId, judiciary, hearing);

            // Check endDate and court-room last as these are the key fields for allocation
            final Stream<Object> courtRoomEvents = courtRoomId != null ?
                    hearing.assignCourtRoom(courtRoomId, hearingId, panel) : hearing.removeCourtRoom(hearingId);


            final Stream<Object> hearingDayEvents =
                    hearing.assignHearingDays(startDate, endDate, nonSittingDays, nonDefaultDays, defaultStartTime, defaultDuration, hearingId, defaultCourtCentre);

            final Stream<Object> weekCommencingDateEvents = weekCommencingStartDate != null && weekCommencingEndDate != null ?
                    hearing.changeWeekCommencingDate(weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingId) : hearing.removeWeekCommencingDates(hearingId);

            final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = prosecutionCaseDefendantOffenceIdsBuilder.buildFromProsecutionCases(updateHearingForListingEnriched.getProsecutionCases());
            final Stream<Object> allocationEvents = hearing.applyAllocationRules(prosecutionCaseDefendantOffenceIds);
            final Stream<Object> hearingPartiallyEvents = extendHearingUtils.createPartiallyAllocationEventForUpdateHearing(hearing, hearingId, updateHearingForListingEnriched.getProsecutionCases(), storedHearing);

            final List<Object> startDateEventList = startDateEvents.collect(toList());

            final Stream<Object> rescheduledEvents = hearing.applyRescheduledCheck(startDateEventList);

            Stream<Object> publicListNoteUpdateEvent = Stream.empty();

            if(publicListNote != null) {
                publicListNoteUpdateEvent = hearing.assignPublicListNote(publicListNote, hearingId);
            }
            final Stream<Object> videoLinkUpdateEvent = hearing.assignVideoLink(hasVideoLink, hearingId);

            return Stream.of(typeEvents, jurisdictionTypeEvents, hearingLanguageEvents, startDateEventList.stream(), nonDefaultDaysEvents, endDateEvents,
                    nonSittingDaysEvents, courtCentreEvents, judiciaryEvents, courtRoomEvents, hearingDayEvents, allocationEvents, weekCommencingDateEvents, hearingPartiallyEvents,
                    rescheduledEvents, videoLinkUpdateEvent, publicListNoteUpdateEvent).flatMap(i -> i);
        });
    }

    private void setJudiciaryFromRotaSLIfJudiciaryIsEmptyAndMagistrates(final List<JudicialRole> judiciary,
             final UUID courtRoomId, final List<NonDefaultDay> nonDefaultDays, final String ouCode) {
        if (isEmpty(judiciary)) {
            nonDefaultDays.stream()
                    .filter(nonDefaultDay -> nonNull(nonDefaultDay.getStartTime()))
                    .min(Comparator.comparing(NonDefaultDay::getStartTime))
                    .ifPresent(firstStartTimeNonDefaultDay -> {
                        final LocalDate firstStartDate = firstStartTimeNonDefaultDay.getStartTime().toLocalDate();
                        judiciary.addAll(rotaSLServiceAdapter.getJudicialRoles(firstStartDate.toString(),
                                ouCode,
                                Optional.of(MeridianUtil.getMeridian(firstStartTimeNonDefaultDay.getStartTime())),
                                courtRoomId.toString()));
                    });
        }
    }

    private Stream<Object> getJudiciaryEvents(final UUID hearingId, final List<JudicialRole> judiciary, final Hearing hearing) {
        return isNotEmpty(judiciary) ? hearing.assignJudiciary(judiciary, hearingId) : hearing.removeJudiciary(hearingId);
    }

    private List<NonDefaultDay> generateNewNonDefaultDays(final JsonEnvelope command, final UpdateHearingForListing updateHearingForListing,
                                                          final List<LocalDate> nonSittingDays, final LocalDate endDate, final LocalDate startDate,
                                                          final Integer defaultDuration, final JurisdictionType jurisdictionType) {

        final List<NonDefaultDay> domainNonDefaultDays = convertNonDefaultDaysToDomain(updateHearingForListing.getNonDefaultDays());

        if (startDate == null || endDate == null) {
            return domainNonDefaultDays;
        }

        final List<NonDefaultDay> filteredNonDefaultDays = domainNonDefaultDays.stream()
                .filter(nonDefaultDay -> nonDefaultDay.getStartTime().toLocalDate().compareTo(startDate) >= 0 && nonDefaultDay.getStartTime().toLocalDate().compareTo(endDate) <= 0)
                .filter(nonDefaultDay -> !nonSittingDays.contains(nonDefaultDay.getStartTime().toLocalDate()))
                .collect(toList());

        final Optional<SelectedCourtCentre> selectedCourtCentre = updateHearingForListing.getSelectedCourtCentre();
        if (JurisdictionType.MAGISTRATES.equals(jurisdictionType) && selectedCourtCentre.isPresent() && !filteredNonDefaultDays.isEmpty()) {
            final Map<UUID, JsonObject> organisationUnitMap = new HashMap<>();
            final UUID selectedCourtCentreId = selectedCourtCentre.get().getId();
            final UUID selectedCourtRoomId = selectedCourtCentre.get().getCourtRoomId();

            // Populate details required for updating slots
            filteredNonDefaultDays.replaceAll(nonDefaultDay -> {
                if (nonDefaultDay.getCourtScheduleId().isPresent()) {
                    return nonDefaultDay;
                }
                final String courtCentreId = nonDefaultDay.getCourtCentreId().orElse(selectedCourtCentreId.toString());
                final String courtRoomId = nonDefaultDay.getRoomId().orElse(selectedCourtRoomId.toString());

                organisationUnitMap.computeIfAbsent(fromString(courtCentreId), k -> courtCentreFactory.getOrganisationUnit(fromString(courtCentreId), command));

                return getNonDefaultDay(nonDefaultDay.getDuration(), fromString(courtCentreId), fromString(courtRoomId),
                        organisationUnitMap.get(fromString(courtCentreId)), nonDefaultDay.getStartTime());

            });

            // Create missing nonDefaultDays and populate details for updating slots
            final List<LocalDate> nonDefaultDates = filteredNonDefaultDays.stream().map(NonDefaultDay::getStartTime).map(ZonedDateTime::toLocalDate).collect(Collectors.toList());
            final long numOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            IntStream.rangeClosed(0, (int) numOfDaysBetween)
                    .mapToObj(startDate::plusDays)
                    .filter(d -> !nonDefaultDates.contains(d) && !nonSittingDays.contains(d))
                    .forEach(date -> {
                        organisationUnitMap.computeIfAbsent(selectedCourtCentreId, k -> courtCentreFactory.getOrganisationUnit(selectedCourtCentreId, command));

                        final JsonObject courtCentreObj = organisationUnitMap.get(selectedCourtCentreId);
                        final LocalTime defaultStartTime = LocalTime.parse(courtCentreObj.getString("defaultStartTime"));
                        final ZonedDateTime hearingStartTime = ZonedDateTime.of(date, defaultStartTime, BST).withZoneSameInstant(UTC);

                        final NonDefaultDay nonDefaultDay = getNonDefaultDay(of(defaultDuration), selectedCourtCentreId, selectedCourtRoomId,
                                courtCentreObj, hearingStartTime);
                        filteredNonDefaultDays.add(nonDefaultDay);
                    });
        }

        return filteredNonDefaultDays;
    }

    private NonDefaultDay getNonDefaultDay(final Optional<Integer> defaultDuration, final UUID courtCentreId, final UUID courtRoomId,
                                           final JsonObject courtCentreObj, final ZonedDateTime hearingStartTime) {
        final Optional<String> ouCode = getString(courtCentreObj, "oucode");
        final Optional<Integer> courtRoomNumber = courtCentreFactory.getCourtRoomNumber(courtCentreObj, courtRoomId.toString());

        return NonDefaultDay.nonDefaultDay()
                .withDuration(defaultDuration)
                .withStartTime(hearingStartTime)
                .withOucode(ouCode)
                .withCourtCentreId(of(courtCentreId.toString()))
                .withCourtRoomId(courtRoomNumber)
                .withSession(of(MeridianUtil.getMeridian(hearingStartTime)))
                .withRoomId(of(courtRoomId.toString())).build();
    }

    private UUID getCourtRoomId(final UpdateHearingForListing updateHearingForListing) {
        final Optional<SelectedCourtCentre> selectedCourtCentre = updateHearingForListing.getSelectedCourtCentre();
        if (selectedCourtCentre.isPresent() && CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            return selectedCourtCentre.get().getCourtRoomId();
        }
        return updateHearingForListing.getCourtRoomId().orElse(null);
    }

    private UUID getCourtCentreId(final UpdateHearingForListing updateHearingForListing) {
        final Optional<SelectedCourtCentre> selectedCourtCentre = updateHearingForListing.getSelectedCourtCentre();
        if (selectedCourtCentre.isPresent() && CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            return selectedCourtCentre.get().getId();
        }
        return updateHearingForListing.getCourtCentreId();
    }

    @Handles("listing.command.extend-hearing-for-hearing-enriched")
    public void extendHearingForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.extend-hearing-for-hearing-enriched' received with payload {}", command.toObfuscatedDebugString());
        }

        final ExtendHearingForHearingEnriched extendHearingForHearingEnriched = jsonObjectConverter.convert(payload, ExtendHearingForHearingEnriched.class);

        final UUID allocatedHearingId = extendHearingForHearingEnriched.getAllocatedHearingId();
        final UUID unAllocatedHearingId = extendHearingForHearingEnriched.getUnAllocatedHearingId();
        final uk.gov.justice.listing.events.Hearing allocatedHearing = hearingFactory.getHearingById(allocatedHearingId, command);
        final uk.gov.justice.listing.events.Hearing unAllocatedHearingPersisted = hearingFactory.getHearingById(unAllocatedHearingId, command);

        boolean partialExtension = false;
        final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap = new HashMap<>();
        final List<ProsecutionCases> prosecutionCases = extendHearingForHearingEnriched.getProsecutionCases();

        if (prosecutionCases != null && !prosecutionCases.isEmpty()) {
            partialExtension = comparePersistedAndRequestedCaseMaps(unallocatedHearingRequestCaseMap, unAllocatedHearingPersisted, prosecutionCases, extendHearingForHearingEnriched);
        }

        if (partialExtension) {

            final List<ListedCase> casesToMove = extractListedCasesToAllocate(unAllocatedHearingPersisted, unallocatedHearingRequestCaseMap);

            //remove the cases/defendants/offences given in the request, from persisted hearing
            final uk.gov.justice.listing.events.Hearing unallocatedHearingPersisted = extendHearingUtils.updateUnallocatedHearing(unAllocatedHearingPersisted, unallocatedHearingRequestCaseMap);

            final List<uk.gov.justice.listing.events.ProsecutionCases> prosecutionCasesToBeRemovedFromHearing = prosecutionCasesBuilder.buildEventProsecutionCase(unallocatedHearingRequestCaseMap);

            updateHearingEventStream(command, allocatedHearingId, (Hearing hearing) -> {
                final Stream<Object> updatedHearing = hearing.updatedListedCasesInHearing(allocatedHearing, unallocatedHearingPersisted, casesToMove);
                final Stream<Object> allocationEvents = hearing.applyAllocationRulesForExtendedHearing(unallocatedHearingPersisted);
                final Stream<Object> hearingPartiallyUpdated = hearing.updateUnallocatedHearingPartially(unAllocatedHearingId, prosecutionCasesToBeRemovedFromHearing);
                return Stream.of(updatedHearing, allocationEvents, hearingPartiallyUpdated).flatMap(i -> i);
            });

        } else {
            //do the whole extension as in before GPE-13108
            final List<UUID> allocatedHearingCasesId = extractListCasesId(allocatedHearing);
            final List<UUID> unAllocatedHearingCasesId = extractListCasesId(unAllocatedHearingPersisted);

            if (!unAllocatedHearingPersisted.getAllocated()) {
                updateHearingEventStream(command, allocatedHearingId, (Hearing hearing) -> {
                    final Stream<Object> updatedHearing = hearing.updatedListedCasesInHearing(allocatedHearing, unAllocatedHearingPersisted, unAllocatedHearingPersisted.getListedCases());
                    final Stream<Object> allocationEvents = hearing.applyAllocationRulesForExtendedHearing(unAllocatedHearingPersisted);
                    final Stream<Object> deleteUnAllocatedHearing = hearing.deleteUnAllocatedHearing(unAllocatedHearingId);
                    return Stream.of(updatedHearing, allocationEvents, deleteUnAllocatedHearing).flatMap(i -> i);
                });
            } else {
                LOGGER.info("incoming list cases : {} cannot be added in allocated hearing as same case id : {} ", unAllocatedHearingCasesId, allocatedHearingCasesId);
            }
        }
    }

    @Handles("listing.command.add-cases-to-hearing")
    public void handleAddCasesToHearing(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-cases-to-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final AddCasesToHearing addCasesToHearing = jsonObjectConverter.convert
                (command.payloadAsJsonObject(), AddCasesToHearing.class);

        updateHearingEventStream(command, addCasesToHearing.getHearingId(), (Hearing hearing) -> {
            final Stream<Object> addedCases = hearing.addCasesToHearing(addCasesToHearing.getProsecutionCases(), addCasesToHearing.getShadowListedOffences(), addCasesToHearing.getSeedingHearingId());
            return Stream.of(addedCases).flatMap(i -> i);
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
        final List<UpdatedOffences> updatedOffence = updateCaseDefendantOffences.getUpdatedOffences();
        final List<CaseOffences> multipleCaseOffences = courtsUpdatedOffenceToDomainOffence.convert(updatedOffence);
        for (final CaseOffences caseOffences : multipleCaseOffences) {
            final UUID caseId = caseOffences.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.updateDefendantOffences(caseOffences)
            );
        }

        // Handle the deleted offences
        final List<DeletedOffences> deletedOffence = updateCaseDefendantOffences.getDeletedOffences();
        final List<CaseSimpleOffences> offencesToBeDeleted = courtsDeletedOffenceToDomainCaseSimpleOffence.convert(deletedOffence);
        for (final CaseSimpleOffences offenceToBeDeleted : offencesToBeDeleted) {
            final UUID caseId = offenceToBeDeleted.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.deleteDefendantOffences(offenceToBeDeleted)
            );
        }

        // Handle the added offences
        final List<AddedOffences> addedOffence = updateCaseDefendantOffences.getAddedOffences();
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
                final Stream<Object> allocationEvents = hearing.applyAllocationRules(Collections.emptyList());

                return Stream.of(allocationEvents, judicialEvents).flatMap(i -> i);
            });
        }
    }

    @Handles("listing.command.sequence-hearings")
    public void sequenceHearings(final JsonEnvelope command) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.sequence-hearings' received with payload {}", command.toObfuscatedDebugString());
        }
        final SequenceHearings sequenceHearings = jsonObjectConverter.convert(command.payloadAsJsonObject(), SequenceHearings.class);
        final List<uk.gov.moj.cpp.listing.domain.SequenceHearing> sequenceHearingList = convertSequenceHearingsToDomain(sequenceHearings);

        for (final uk.gov.moj.cpp.listing.domain.SequenceHearing sequenceHearing : sequenceHearingList) {
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

    @Handles("listing.command.update-case-markers-to-hearing")
    public void updateCaseMarkersToHearing(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-case-markers-to-hearing' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }

        final UpdateCaseMarkersToHearingCommand command =
                getUpdateCaseMarkerToHearingCommand(commandEnvelope);

        final UUID hearingId = command.getHearingId();
        final UUID caseId = command.getProsecutionCaseId();
        final List<CaseMarker> caseMarkers = caseMarkersToDomainConverter.convert(command.getCaseMarkers());

        updateHearingEventStream(commandEnvelope, hearingId, (Hearing hearing) ->
                hearing.updateCaseMarkers(caseId, caseMarkers));

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

    @Handles("listing.command.update-case-markers")
    public void updateCaseMarker(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-case-markers' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final UpdateCaseMarkersCommand command = getUpdateCaseMarkersCommand(commandEnvelope);
        final List<CaseMarker> caseMarkers = caseMarkersToDomainConverter.convert(command.getCaseMarkers());
        final UUID prosecutionCaseId = command.getProsecutionCaseId();
        updateCaseEventStream(commandEnvelope, prosecutionCaseId, (Case listingCase) ->
                listingCase.addedCaseMarkers(prosecutionCaseId, caseMarkers));
    }

    @Handles("listing.command.update-linked-cases")
    public void updateLinkedCases(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-linked-cases' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final UpdateLinkedCases command = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), UpdateLinkedCases.class);
        for (final Cases cases : command.getCases()) {
            final UUID caseId = cases.getCaseId();
            final uk.gov.moj.cpp.listing.domain.Cases casesDomain = casesToDomainConverter.convert(cases);
            updateCaseEventStream(commandEnvelope, caseId, (Case listingCase) -> listingCase.linkCases(command.getLinkActionType().toString(), cases.getCaseId(), cases.getCaseUrn(), casesDomain));
        }
    }

    @Handles("listing.command.update-linked-case-in-hearing")
    public void updateLinkedCaseInHearing(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-linked-case-in-hearing' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final UpdateLinkedCaseInHearing command = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), UpdateLinkedCaseInHearing.class);

        updateHearingEventStream(commandEnvelope, command.getHearingId(), (Hearing hearing) ->
                hearing.linkCaseToHearing(command.getLinkActionType(), command.getCaseId(), command.getCaseUrn(), convertLinkedToCases(command.getLinkedToCases())));

    }

    private List<uk.gov.moj.cpp.listing.domain.LinkedToCases> convertLinkedToCases(final List<LinkedToCases> linkedToCases) {
        return linkedToCases.stream()
                .map(lc -> uk.gov.moj.cpp.listing.domain.LinkedToCases.linkedToCases()
                        .withCaseId(lc.getCaseId())
                        .withCaseUrn(lc.getCaseUrn())
                        .build())
                .collect(toList());
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

    @Handles("listing.command.update-hearing-to-case")
    public void updateHearingToCase(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-hearing-to-case' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final UpdateHearingToCaseCommand command = getUpdateHearingToCaseCommand(commandEnvelope);

        final UUID caseId = command.getCaseId();
        final UUID allocatedHearingId = command.getExistingHearingId();
        final UUID unAllocatedHearingId = command.getId();

        updateCaseEventStream(commandEnvelope, caseId, (Case listingCase) ->
                listingCase.updateHearing(caseId, allocatedHearingId, unAllocatedHearingId));
    }

    @Handles("listing.command.eject-case-or-application")
    public void ejectCaseOrApplication(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.eject-case-or-application' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }

        final CaseOrApplicationEjected command = jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), CaseOrApplicationEjected.class);
        final Optional<UUID> caseId = command.getProsecutionCaseId();
        final List<UUID> hearingIds = command.getHearingIds();
        final Optional<UUID> applicationId = command.getApplicationId();
        if (caseId.isPresent()) {
            updateCaseEventStream(commandEnvelope, caseId.get(), (Case listingCase) ->
                    listingCase.ejectCaseForHearings(hearingIds, caseId.get(), command.getRemovalReason()));
        }
        if (applicationId.isPresent()) {
            updateApplicationEventStream(commandEnvelope, applicationId.get(), (Application application) ->
                    application.ejectApplicationForHearings(hearingIds, applicationId.get(), (command.getRemovalReason())));
        }

    }

    @Handles("listing.command.eject-case")
    public void ejectCase(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.eject-case' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();
        final UUID caseId = fromString(commandPayload.getString(PROSECUTION_CASE_ID));
        final UUID hearingId = fromString(commandPayload.getString(HEARING_ID));
        final String removalReason = commandPayload.getString(REMOVAL_REASON);
        updateHearingEventStream(commandEnvelope, hearingId, (Hearing hearing) ->
                hearing.ejectCase(hearingId, caseId, removalReason));
    }

    @Handles("listing.command.eject-application")
    public void ejectApplication(final JsonEnvelope commandEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.eject-application' received with payload {}", commandEnvelope.toObfuscatedDebugString());
        }
        final JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();
        final UUID applicationId = fromString(commandPayload.getString(APPLICATION_ID));
        final UUID hearingId = fromString(commandPayload.getString(HEARING_ID));
        final String removalReason = commandPayload.getString(REMOVAL_REASON);
        updateHearingEventStream(commandEnvelope, hearingId, (Hearing hearing) ->
                hearing.ejectApplication(hearingId, applicationId, removalReason));
    }

    @Handles("listing.command.update-defendant-legalaid-status")
    public void updateDefendantLegalAidStatus(final JsonEnvelope envelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-defendant-legalaid-status event received {}", envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final String legalAidStatus = payload.getString(LEGAL_AID_STATUS);
        final UUID caseId = fromString(payload.getString(CASE_ID));
        updateCaseEventStream(envelope, caseId, (Case listingCase) ->
                listingCase.updateDefendantLegalAidStatus(caseId, defendantId, legalAidStatus));
    }

    @Handles("listing.command.update-defendant-legalaid-status-for-hearing")
    public void updateDefendantLegalAidStatusForHearing(final JsonEnvelope envelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-defendant-legalaid-status-for-hearing event received {}", envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final String legalAidStatus = payload.getString(LEGAL_AID_STATUS);
        final UUID caseId = fromString(payload.getString(CASE_ID));
        final UUID hearingId = fromString(payload.getString(HEARING_ID));

        updateHearingEventStream(envelope, hearingId, (Hearing hearing) ->
                hearing.updateDefendantLegalAidStatusForHearing(hearingId, caseId, defendantId, legalAidStatus));
    }

    @Handles("listing.command.update-case-resulted-defendant-proceedings-concluded")
    public void updateDefendantHearingResultedAndCaseResulted(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-case-resulted-defendant-proceedings-concluded event received {}", envelope.toObfuscatedDebugString());
        }

        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(envelope.payloadAsJsonObject().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
        final UUID caseId = prosecutionCase.getId();
        updateCaseEventStream(envelope, caseId, (Case listingCase) -> listingCase.updateDefendantCaseResultedAndUpdated(prosecutionCase));
    }

    @Handles("listing.command.update-defendant-court-proceedings")
    public void updateDefendantCourtProceedings(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-defendant-court-proceedings event received {}", envelope.toObfuscatedDebugString());
        }
        final UUID hearingId = fromString(envelope.payloadAsJsonObject().getString(HEARING_ID));
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(envelope.payloadAsJsonObject().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
        updateHearingEventStream(envelope, hearingId, (Hearing hearing) -> hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase));
    }


    @Handles("listing.command.court-list-request-export")
    public void courtListRequestExport(final JsonEnvelope commandEnvelope) throws EventStreamException {

        final CourtListRequestExport courtListRequestExport =
                jsonObjectConverter.convert(commandEnvelope.payloadAsJsonObject(), CourtListRequestExport.class);

        final UUID courtListId = uuidService.getCourtListId(
                courtListRequestExport.getCourtCentreId(),
                courtListRequestExport.getPublishCourtListType(),
                courtListRequestExport.getStartDate());

        final CourtListExportRequested event = new CourtListExportRequested(
                courtListRequestExport.getCourtCentreId(),
                courtListId,
                courtListRequestExport.getCourtListJson(),
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
                command.getCourtListFileName(),
                command.getCourtListId(),
                command.getEndDate(),
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
                command.getCourtListFileName(),
                command.getCourtListId(),
                command.getEndDate(),
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

        final UUID courtListId = uuidService.getCourtListId(
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

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(commandEnvelope)));
    }

    @Handles("listing.command.handler.modify-hearing-counsel")
    public void modifyHearingCounsels(final JsonEnvelope commandEnvelope) throws EventStreamException {
        final JsonObject payload = commandEnvelope.payloadAsJsonObject();

        LOGGER.info("'listing.command.handler.modify-hearing-counsel' received with payload {}",
                commandEnvelope.toObfuscatedDebugString());

        final ModifyHearingCounselCommand modifyHearingCounsels = jsonObjectConverter.convert(payload, ModifyHearingCounselCommand.class);
        final HearingCounselModified event = new HearingCounselModified(
                uk.gov.justice.listing.event.Action.valueFor(modifyHearingCounsels.getAction().name()).orElse(null),
                uk.gov.justice.listing.event.CounselType.valueFor(modifyHearingCounsels.getCounselType().name()).orElse(null),
                modifyHearingCounsels.getHearingId(),
                modifyHearingCounsels.getPayload());

        final Stream<Object> events = streamOf(event);

        final EventStream eventStream = eventSource.getStreamById(modifyHearingCounsels.getHearingId());

        eventStream.append(events.map(enveloper.withMetadataFrom(commandEnvelope)));
    }

    @Handles("listing.command.cancel-hearing-days")
    public void cancelHearingDays(final Envelope<CancelHearingDays> envelope) throws EventStreamException {
        final CancelHearingDays payload = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(payload.getHearingId());
        final Hearing hearingAggregate = aggregateService.get(eventStream, Hearing.class);
        final Stream<Object> events = hearingAggregate.cancelHearingDays(payload.getHearingId(), hearingDaysToDomainConverter.convert(payload.getHearingDays()));

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("listing.command.correct-hearing-days-without-court-centre")
    public void correctHearingDaysWithoutCourtCentre(final JsonEnvelope commandEnvelope) throws EventStreamException {
        final JsonObject payload = commandEnvelope.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString("id"));

        final EventStream eventStream = eventSource.getStreamById(hearingId);

        final List<uk.gov.justice.listing.events.HearingDay> hearingDays = new ArrayList<>();

        payload.getJsonArray("hearingDays").getValuesAs(JsonObject.class).stream()
                .forEach(hearingDay -> hearingDays.add(jsonObjectConverter.convert(hearingDay, uk.gov.justice.listing.events.HearingDay.class)));

        final HearingDaysWithoutCourtCentreCorrected event = HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withId(hearingId)
                .withHearingDays(hearingDays)
                .build();

        final Stream<Object> events = Stream.of(event);
        appendEventsToStream(commandEnvelope, eventStream, events);
    }

    @Handles("listing.command.update-cps-prosecutor-with-associated-hearings")
    public void updateProsecutorForAssociatedHearings(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-cps-prosecutor-with-associated-hearings event received {}", envelope.toObfuscatedDebugString());
        }

        final UpdateCaseIdentifierWithHearings updateCpsProsecutorWithHearings = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateCaseIdentifierWithHearings.class);

        for (final UUID hearingId : updateCpsProsecutorWithHearings.getHearingIds()) {
            final EventStream eventStream = eventSource.getStreamById(hearingId);
            final Hearing hearingAggregate = aggregateService.get(eventStream, Hearing.class);
            final Stream<Object> events = hearingAggregate.updateCaseIdentifier(updateCpsProsecutorWithHearings.getProsecutionAuthorityId(), updateCpsProsecutorWithHearings.getProsecutionAuthorityCode(), updateCpsProsecutorWithHearings.getProsecutionCaseId());
            appendEventsToStream(envelope, eventStream, events);
        }
    }


    @VisibleForTesting
    void setClock(final Clock clock) {
        this.clock = clock;
    }


    private void publishFinalCourtList(final Metadata commandMetaData, final CourtCentreDetails courtCentreDetails) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Attempting to request publication of the final court list for crown court centre [{}] ...", courtCentreDetails.getId());
        }
        try {
            publishCourtList(envelopeFrom(commandMetaData, asJson(generatePublishCourtListCommand(courtCentreDetails))));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Successfully requested publication of the final court list for crown court centre [{}].", courtCentreDetails.getId());
            }
        } catch (final EventStreamException | RuntimeException e) {
            // This should be robust, so to allow subsequent attempts.
            if (LOGGER.isErrorEnabled()) {
                final String message
                        = format(
                        "Exception thrown While trying to publish the final court list for Court Centre [%s]",
                        courtCentreDetails.getId());
                LOGGER.error(message, e);
            }
        }
    }

    private PublishCourtList generatePublishCourtListCommand(final CourtCentreDetails courtCentreDetails) {

        final ZonedDateTime zonedNow = clock.now();
        final LocalDate today = zonedNow.toLocalDate();
        final LocalDate nextWorkingDay = getNextWorkingDay(today);
        return PublishCourtList.publishCourtList()
                .withCourtCentreId(courtCentreDetails.getId())
                .withStartDate(nextWorkingDay)
                .withEndDate(nextWorkingDay)
                .withPublishCourtListType(PublishCourtListType.FINAL)
                .withRequestedTime(of(zonedNow))
                .build();
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private List<uk.gov.moj.cpp.listing.domain.SequenceHearing> convertSequenceHearingsToDomain(final SequenceHearings sequenceHearingsCommand) {
        List<uk.gov.moj.cpp.listing.domain.SequenceHearing> domainSequenceHearings = Collections.emptyList();
        if (sequenceHearingsCommand != null && !sequenceHearingsCommand.getHearings().isEmpty()) {
            domainSequenceHearings = sequenceHearingsCommand.getHearings()
                    .stream()
                    .map(sh -> uk.gov.moj.cpp.listing.domain.SequenceHearing.sequenceHearing()
                            .withHearingDays(sh.getSequenceHearingDays()
                                    .stream()
                                    .map(shd -> hearingDay()
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

    private List<JudicialRole> convertJudicialRolesToDomain(final List<uk.gov.justice.core.courts.JudicialRole> commandJudiciary) {
        List<JudicialRole> domainJudiciary = new ArrayList<>();
        if (commandJudiciary != null && !commandJudiciary.isEmpty()) {
            domainJudiciary = commandJudiciary.stream().map(jr -> JudicialRole.judicialRole()
                    .withIsBenchChairman(jr.getIsBenchChairman())
                    .withIsDeputy(jr.getIsDeputy())
                    .withJudicialId(jr.getJudicialId())
                    .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                            .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                            .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                            .build())
                    .withUserId(jr.getUserId().orElse(null))
                    .build()).collect(toList());
        }
        return domainJudiciary;
    }

    private Type convertTypeToDomain(final uk.gov.justice.core.courts.HearingType hearingType) {
        return Type.type()
                .withId(hearingType.getId())
                .withDescription(hearingType.getDescription())
                .withWelshDescription(hearingType.getWelshDescription())
                .build();
    }

    private List<NonDefaultDay> convertNonDefaultDaysToDomain(final List<uk.gov.justice.listing.commands.NonDefaultDay> commandDefaultDays) {
        List<NonDefaultDay> domainDefaultDays = Collections.emptyList();
        if (commandDefaultDays != null && !commandDefaultDays.isEmpty()) {
            domainDefaultDays = commandDefaultDays.stream().map(ndd -> NonDefaultDay.nonDefaultDay()
                    .withStartTime(ndd.getStartTime())
                    .withDuration(of(getAdjustedDuration(ndd.getDuration().isPresent() ? ndd.getDuration().get() : null)))
                    .withCourtScheduleId(ndd.getCourtScheduleId())
                    .withCourtRoomId(ndd.getCourtRoomId())
                    .withOucode(ndd.getOucode())
                    .withSession(ndd.getSession())
                    .withRoomId(ndd.getRoomId())
                    .withCourtCentreId(ndd.getCourtCentreId()).build())
                    .collect(toList());
        }
        return domainDefaultDays;
    }

    private uk.gov.moj.cpp.listing.domain.RestrictCourtList convertRestrictCourtListToDomain(final RestrictCourtList restrictCourtList) {

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
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
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
                .map(JsonObject.class::cast)
                .map(this::toCourtCentreDetails)
                .collect(toList());
    }

    private CourtCentreDetails toCourtCentreDetails(final JsonObject courtCentreDetailsAsJson) {
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

    private List<UUID> extractListCasesId(final uk.gov.justice.listing.events.Hearing hearing) {
        return hearing.getListedCases().stream().map(ListedCase::getId).collect(toList());
    }

    private List<CourtSchedule> getCourtSchedules(final String bookingId) {
        final Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("bookingIds", bookingId);
        final Response slotsResponse = provisionalBookingService.getSlots(paramsMap);
        final JsonObject resultJson = objectToJsonObjectConverter.convert(slotsResponse.getEntity());

        final List<CourtSchedule> courtScheduleList = new ArrayList<>();

        final JsonArray provisionalSlots = resultJson.getJsonArray("provisionalSlots");
        for (int i = 0; i < provisionalSlots.size(); i++) {

            courtScheduleList.add(jsonObjectConverter.convert(provisionalSlots.getJsonObject(i), CourtSchedule.class));
        }
        return courtScheduleList;
    }

    private void generateNonDefaultDays(final List<uk.gov.justice.listing.commands.NonDefaultDay> nonDefaultDaysList,
                                        final List<CourtSchedule> courtScheduleList, final HearingListingNeeds commandHearing) {
        final OffsetDateTime startDate = CommandToDomainConverter.getStartDateTime(commandHearing)
                .toInstant().atOffset(ZoneOffset.UTC);
        final int hour = startDate.getHour();
        final long minute = startDate.getMinute();
        courtScheduleList.forEach(cs ->
                nonDefaultDaysList.add(
                        uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(of(cs.getCourtScheduleId()))
                                .withOucode(of(cs.getOuCode()))
                                .withSession(of(cs.getCourtSession()))
                                .withDuration(getAdjustedDuration(commandHearing.getEstimatedMinutes()))
                                .withCourtRoomId(of(cs.getCourtRoomNumber())) // for prospect developers, names mismatch but fields point to the same context
                                .withStartTime(isBlank(cs.getHearingStartTime()) ? cs.getSessionDate().atStartOfDay(UTC).withHour(hour).minusMinutes(minute) : ZonedDateTime.parse(cs.getHearingStartTime()))
                                .withRoomId(cs.getCourtRoomId())
                                .withCourtCentreId(cs.getCourtHouseId())
                                .build()
                )

        );
    }

    private boolean isSchedulingAndListingUpdateRequired(final uk.gov.justice.core.courts.JurisdictionType jurisdictionType, final List<uk.gov.justice.listing.commands.NonDefaultDay> nonDefaultDays) {
        return MAGISTRATES.equals(jurisdictionType)
                && !nonDefaultDays.isEmpty()
                && nonDefaultDays.stream().anyMatch(ndd -> ndd.getCourtScheduleId().isPresent());
    }

    private boolean comparePersistedAndRequestedCaseMaps(final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap,
                                                         final uk.gov.justice.listing.events.Hearing unAllocatedHearingPersisted,
                                                         final List<ProsecutionCases> prosecutionCases,
                                                         final ExtendHearingForHearingEnriched extendHearingForHearingEnriched) {
        //build <case, <defendant, <offences>>> map of the persisted unallocated hearing
        final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap = extendHearingUtils.buildPersistedCaseDefendantOffenceMap(unAllocatedHearingPersisted);

        //build <case, <defendant, <offences>>> map of the unallocated hearing in request
        unallocatedHearingRequestCaseMap.putAll(extendHearingUtils.buildRequestedCaseDefendantOffenceMap(prosecutionCases, extendHearingForHearingEnriched.getUnAllocatedHearingId()));

        //compare if all cases from the persisted hearing are in the request
        return !persistedUnallocatedHearingCasesMap.equals(unallocatedHearingRequestCaseMap);
    }

    private List<ListedCase> extractListedCasesToAllocate(final uk.gov.justice.listing.events.Hearing unAllocatedHearingPersisted, final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap) {
        //prepare a deep copy of unallocated hearing's cases, so we can extract offences to allocate into allocatedHearing
        final List<ListedCase> listedCasesDeepCopy = jsonObjectConverter.convert(objectToJsonObjectConverter.convert(unAllocatedHearingPersisted), uk.gov.justice.listing.events.Hearing.class).getListedCases();
        return extendHearingUtils.extractCasesToMove(listedCasesDeepCopy, unallocatedHearingRequestCaseMap);
    }
}
