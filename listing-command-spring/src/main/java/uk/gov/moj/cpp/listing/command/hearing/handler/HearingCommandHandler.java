package uk.gov.moj.cpp.listing.command.hearing.handler;

import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.time.ZonedDateTime.now;
import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static jakarta.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay;
import static uk.gov.moj.cpp.listing.domain.HearingLanguage.valueFor;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.convertHoursAndMinutesToMinutes;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.getNextWorkingDay;
import static uk.gov.moj.cpp.listing.domain.utils.HearingUtil.getAdjustedDuration;
import static uk.gov.moj.cpp.listing.domain.utils.HmiConstants.SOURCE_HMI;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateCaseMarkersCommand;
import uk.gov.justice.core.courts.UpdateCaseMarkersToHearingCommand;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.CourtListRequestExport;
import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.ListCourtHearing;
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
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.courts.RemovePartiallyMergedOffencesFromOriginalHearing;
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
import uk.gov.justice.listing.courts.UpdateLaaReferenceForApplication;
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
import uk.gov.justice.listing.events.HearingDayCourtSchedule;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.UpdateHearingAddCaseBdf;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.factory.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.hearing.handler.factory.HearingFactory;
import uk.gov.moj.cpp.listing.command.hearing.handler.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.hearing.handler.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CaseMarkersToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CasesToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CommandDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CommandOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CommandSimpleOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtApplicationToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsAddedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsDefendantToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsDeletedOffenceToDomainCaseSimpleOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.CourtsUpdatedOffenceToDomainOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.HearingDaysCoreToDomainConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.ProsecutionCaseDefendantOffenceIdsBuilder;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.ProsecutionCasesBuilder;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.hearing.ExtendHearingUtils;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.hearing.HearingUpdateOperationType;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.DefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.domain.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.OffenceIds;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.PtphDetail;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;

import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.listing.courts.ChangeNextHearingDay;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched;
import uk.gov.justice.listing.courts.RemoveOffencesFromExistingHearing;
import uk.gov.justice.listing.courts.RemoveSelectedOffencesFromExistingHearing;
import uk.gov.moj.cpp.listing.common.duration.HearingDurationDefaults;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JEE command-handler methods for the Hearing commands (MBD refactor P2.3.2, ADR 015), ported
 * as verbatim as possible from {@code ListingCommandHandler}: {@code updateHearingEventStream}
 * runs the ported rules on the hearing's viewstore state through {@link HearingFlow}. Called by
 * {@link HearingCommands}, which owns the transaction and the processor queue.
 */
@Component
@SuppressWarnings({"squid:S1188", "squid:CallToDeprecatedMethod", "squid:S2629", "squid:S00112"})
public class HearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingCommandHandler.class);

    public static final String SPLIT_VIA_UPDATE_HEARING_REJECTED = "SPLIT_VIA_UPDATE_HEARING_REJECTED";
    public static final String HEARING_ID = "hearingId";
    private static final String HEARING_DAY_COURT_SCHEDULES = "hearingDayCourtSchedules";
    public static final String OUCODE = "oucode";
    private static final String JURISDICTION = "jurisdiction";
    private static final String START_DATE = "startDate";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String SESSION_DATE = "sessionDate";
    private static final String MOVE_COURT_CENTRE_ID = "courtCentreId";
    private static final String MOVE_COURT_ROOM_ID = "courtRoomId";
    private static final String MOVE_END_DATE = "endDate";
    private static final String MOVE_SESSIONS = "sessions";
    private static final String MOVE_IS_DRAFT = "isDraft";
    private static final String IS_DRAFT = "isDraft";
    private static final String SESSION_START_TIME = "sessionStartTime";
    private static final String SESSION_END_TIME = "sessionEndTime";
    private static final String DURATION_IN_MINUTES = "durationInMinutes";
    private static final String CROWN_JURISDICTION = "CROWN";

    @Autowired
    private HearingFlow hearingFlow;

    @Autowired
    private HearingTypeFactory hearingTypeFactory;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Autowired
    private CommandToDomainConverter commandToDomainConverter;

    @Autowired
    private CourtApplicationToDomainConverter courtApplicationToDomainConverter;

    @Autowired
    private HearingDaysCoreToDomainConverter hearingDaysCoreToDomainConverter;

    @Autowired
    private ProsecutionCasesBuilder prosecutionCasesBuilder;

    @Autowired
    private ExtendHearingUtils extendHearingUtils;

    @Autowired
    private ProsecutionCaseDefendantOffenceIdsBuilder prosecutionCaseDefendantOffenceIdsBuilder;

    @Autowired
    private HearingFactory hearingFactory;

    @Autowired
    private CourtCentreFactory courtCentreFactory;

    @Autowired
    private CommandDefendantToDomainConverter commandDefendantToDomainConverter;

    @Autowired
    private CommandOffenceToDomainOffence commandOffenceToDomainOffence;

    @Autowired
    private CommandSimpleOffenceToDomainOffence commandSimpleOffenceToDomainOffence;

    @Autowired
    private CaseMarkersToDomainConverter caseMarkersToDomainConverter;

    @SuppressWarnings({"squid:S3655", "squid:S3776"})
    /** {@code listing.command.list-court-hearing-enriched}. */
    public void listCourtHearing(final Envelope command) {
        final JsonObject payload = command.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-court-hearing' received with payload {}", command.payload());
        }
        final ListCourtHearingEnriched listCourtHearingEnriched = jsonObjectConverter.convert(payload, ListCourtHearingEnriched.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-court-hearing' listCourtHearingV2 received with payload {}", command.payload());
        }
        LOGGER.debug("'listing.command.list-court-hearing' listCourtHearingV2: {}", listCourtHearingEnriched);
        final Optional<String> adjournedFromDate = ofNullable(listCourtHearingEnriched.getAdjournedFromDate());
        final ListCourtHearing listCourtHearing = listCourtHearingEnriched.getListCourtHearing();
        final Map<UUID, CourtCentreDetails> courtCentres = listCourtHearingEnriched.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));
        LOGGER.info("'listing.command.list-court-hearing' listCourtHearingV2 courtCentres: {}", courtCentres);

        for (final HearingListingNeeds commandHearing : listCourtHearing.getHearings()) {

            final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = commandToDomainConverter.convert(commandHearing, convertNonDefaultDaysCoreToDomain(commandHearing.getNonDefaultDays()), listCourtHearing.getShadowListedOffences());
            final CourtCentreDetails courtCentre = courtCentres.get(domainHearing.getCourtCentreId());
            final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                    .withDefaultDuration(courtCentre.getDefaultDuration())
                    .withDefaultStartTime(courtCentre.getDefaultStartTime())
                    .withCourtCentreId(courtCentre.getId())
                    .build();

            final boolean isSlotsBooked = isNotEmpty(commandHearing.getBookedSlots());
            final Optional<UUID> finalBookingReference = Optional.ofNullable(commandHearing.getBookingReference());
            updateHearingEventStream(command, commandHearing.getId(), (HearingRules hearing) -> {
                final Stream<Object> listingEvents = hearing.list(domainHearing.getId(),
                        domainHearing.getType(),
                        domainHearing.getEstimatedMinutes(),
                        domainHearing.getEstimatedDuration(),
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
                        adjournedFromDate,
                        domainHearing.getWeekCommencingStartDate(),
                        domainHearing.getWeekCommencingEndDate(),
                        domainHearing.getWeekCommencingDurationInWeeks(),
                        domainHearing.getHearingDays(),
                        domainHearing.getNonDefaultDays(),
                        domainHearing.getNonSittingDays(),
                        isSlotsBooked,
                        commandHearing.getBookingType(),
                        commandHearing.getPriority(),
                        commandHearing.getSpecialRequirements(),
                        domainHearing.getIsPossibleDisqualification(),
                        domainHearing.getGroupProceedings(),
                        domainHearing.getNumberOfGroupCases(),
                        domainHearing.getPtphDetail()
                );
                final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = buildFromProsecutionCases(commandHearing.getProsecutionCases());
                final Stream<Object> allocationEvents = hearing.applyAllocationRules(finalBookingReference, false, false, prosecutionCaseDefendantOffenceIds, empty(), commandHearing.getIsGroupProceedings());

                return Stream.of(listingEvents, allocationEvents).flatMap(i -> i);
            });

        }

    }

    private ZonedDateTime buildStartDateTime(final LocalDate localDate, final LocalTime localTime) {
        return ZonedDateTime.of(localDate, localTime, ZoneOffset.UTC);
    }

    public List<ProsecutionCaseDefendantOffenceIds> buildFromProsecutionCases(final List<ProsecutionCase> prosecutionCases) {
        return isNull(prosecutionCases) ? Collections.emptyList() :
                prosecutionCases.stream()
                        .map(p -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                                .withId(p.getId())
                                .withIsCivil(p.getIsCivil())
                                .withGroupId(p.getGroupId())
                                .withIsGroupMaster(p.getIsGroupMaster())
                                .withIsGroupMember(p.getIsGroupMember())
                                .withDefendants(p.getDefendants().stream()
                                        .map(d -> DefendantOffenceIds.defendantOffenceIds()
                                                .withId(d.getId())
                                                .withOffences(d.getOffences().stream()
                                                        .map(o -> OffenceIds.offenceIds()
                                                                .withId(o.getId())
                                                                .build()).collect(toList()))
                                                .build()).collect(toList()))
                                .build())
                        .collect(toList());
    }







    /** {@code listing.command.vacate-trial-enriched}. */
    public void vacateTrial(final Envelope command) {

        LOGGER.info("'listing.command.vacate-trial-enriched' received with payload {}", command.payload());

        final VacateTrialEnriched vacateTrialEnriched = jsonObjectConverter.convert(command.payload(), VacateTrialEnriched.class);

        updateHearingEventStream(command, vacateTrialEnriched.getHearingId(), (HearingRules hearing) -> hearing.vacateTrial(vacateTrialEnriched.getHearingId(), vacateTrialEnriched.getVacatedTrialReasonId()));
    }

    /** {@code listing.command.move-hearing-to-past-date-enriched}. */
    public void moveHearingToPastDate(final Envelope command) {

        LOGGER.info("'listing.command.move-hearing-to-past-date-enriched' received with payload {}", command.payload());

        final JsonObject payload = command.payload();
        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final String jurisdiction = payload.getString(JURISDICTION);
        final LocalDate startDate = parse(payload.getString(START_DATE));

        // hearing-day-court-schedule-updated matches days BY DATE in the projection, so it cannot
        // move a day to a new date. Instead the hearing's days are re-issued on the past date(s)
        // carrying the sessions booked by courtscheduler (command-api books them for both
        // MAGISTRATES and CROWN): one hearing day per booked session, so a multi-day hearing keeps N
        // days matching courtscheduler's N allocations. An enrichment without a sessions array (older
        // producer) falls back to the single flat slot.
        // New end date = last session courtscheduler booked; absent on older enrichments, in which case
        // the hearing keeps its current end date (never silently removed).
        final Optional<LocalDate> endDate = payload.containsKey(MOVE_END_DATE) && !payload.isNull(MOVE_END_DATE)
                ? Optional.of(parse(payload.getString(MOVE_END_DATE))) : Optional.empty();
        final uk.gov.justice.core.courts.JurisdictionType jurisdictionType = CROWN_JURISDICTION.equals(jurisdiction)
                ? uk.gov.justice.core.courts.JurisdictionType.CROWN
                : uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
        final List<uk.gov.moj.cpp.listing.domain.HearingDay> movedDays = buildMovedHearingDays(payload);
        // courtscheduler may have booked the past run in a different room from the hearing's current one;
        // when every booked day shares one room, the hearing-level courtRoomId follows it, mirroring what
        // the update flow does when it assigns a court room, so hearing and days never disagree. Panel is left untouched.
        final Optional<UUID> bookedRoom = singleBookedRoom(movedDays);
        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> {
            // Materialised (not left as a lazy Stream) so applyRescheduledCheck below can inspect
            // whether a StartDateChangedForHearing actually occurred - same pattern as
            // updateHearingForListingEnriched.
            final List<Object> startDateEventList = hearing.changeStartDate(startDate, hearingId).collect(toList());
            final Stream<Object> endDateEvents = endDate.map(date -> hearing.changeEndDate(date, hearingId)).orElseGet(Stream::empty);
            final Stream<Object> courtRoomEvents = bookedRoom.map(room -> hearing.assignCourtRoom(room, hearingId, Optional.empty())).orElseGet(Stream::empty);
            final Stream<Object> hearingDayEvents = hearing.assignHearingDaysV2(hearingId, movedDays, null, null, jurisdictionType, emptyList());
            // Ported from main and applied uniformly for BOTH jurisdictions (closes the SPRDT-1214 gap
            // flagged in the review artifact: without these, public.listing.hearing-updated and
            // public.listing.vacated-trial-updated never fired from this command). Sequenced after
            // assignHearingDaysV2, mirroring updateHearingForListingEnriched's ordering, since
            // canAllocate()/canUnallocate() depend on the day/room state just mutated. Both flags are
            // hard-coded false: a past-date move must never notify parties.
            final Stream<Object> allocationEvents = hearing.applyAllocationRules(emptyList(), false, false);
            final Stream<Object> rescheduledEvents = hearing.applyRescheduledCheck(startDateEventList);
            return Stream.of(startDateEventList.stream(), endDateEvents, courtRoomEvents, hearingDayEvents, allocationEvents, rescheduledEvents)
                    .flatMap(events -> events);
        });
    }

    /** The one room every moved day is booked in, or empty when rooms differ / are unknown. */
    private static Optional<UUID> singleBookedRoom(final List<uk.gov.moj.cpp.listing.domain.HearingDay> days) {
        final java.util.Set<UUID> rooms = days.stream()
                .map(uk.gov.moj.cpp.listing.domain.HearingDay::getCourtRoomId)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toSet());
        return rooms.size() == 1 ? Optional.of(rooms.iterator().next()) : Optional.empty();
    }

    /**
     * One hearing day per booked session in {@code sessions} (date order, sequence 1..N), each falling
     * back to the enrichment's top-level values (courtCentreId of the command, per-day duration) for
     * anything the session entry does not carry. Without a sessions array the single flat slot
     * (courtScheduleId/sessionDate/...) is the one and only day.
     */
    private static List<uk.gov.moj.cpp.listing.domain.HearingDay> buildMovedHearingDays(final JsonObject payload) {
        if (payload.containsKey(MOVE_SESSIONS) && !payload.isNull(MOVE_SESSIONS)
                && !payload.getJsonArray(MOVE_SESSIONS).isEmpty()) {
            final List<uk.gov.moj.cpp.listing.domain.HearingDay> days = new ArrayList<>();
            int sequence = 1;
            for (final JsonValue value : payload.getJsonArray(MOVE_SESSIONS)) {
                final JsonObject session = (JsonObject) value;
                days.add(buildMovedHearingDay(session, payload, parse(session.getString(SESSION_DATE)),
                        Optional.of(fromString(session.getString(COURT_SCHEDULE_ID))), sequence++));
            }
            return days;
        }
        return List.of(buildMovedHearingDay(payload, payload, parse(payload.getString(SESSION_DATE)),
                Optional.of(fromString(payload.getString(COURT_SCHEDULE_ID))), 1));
    }

    private static uk.gov.moj.cpp.listing.domain.HearingDay buildMovedHearingDay(final JsonObject day,
                                                                                 final JsonObject defaults,
                                                                                 final LocalDate dayDate,
                                                                                 final Optional<UUID> courtScheduleId,
                                                                                 final int sequence) {
        final Optional<UUID> courtCentreId = optionalUuid(day, MOVE_COURT_CENTRE_ID)
                .or(() -> optionalUuid(defaults, MOVE_COURT_CENTRE_ID));
        final Optional<UUID> courtRoomId = optionalUuid(day, MOVE_COURT_ROOM_ID);
        final ZonedDateTime dayStartTime = day.containsKey(SESSION_START_TIME) && !day.isNull(SESSION_START_TIME)
                ? ZonedDateTime.parse(day.getString(SESSION_START_TIME))
                : dayDate.atStartOfDay(java.time.ZoneOffset.UTC);
        final Integer durationInMinutes = optionalInt(day, DURATION_IN_MINUTES)
                .or(() -> optionalInt(defaults, DURATION_IN_MINUTES))
                .orElse(null);
        // hearing-days-changed-for-hearing requires endTime on every day. The normal listing flows
        // always derive it as startTime + the day's duration (the session's own end time is the
        // courtroom's closing time, not the hearing's), so do the same; only without a duration does
        // the session end time stand in.
        final ZonedDateTime dayEndTime;
        if (durationInMinutes != null) {
            dayEndTime = dayStartTime.plusMinutes(durationInMinutes);
        } else if (day.containsKey(SESSION_END_TIME) && !day.isNull(SESSION_END_TIME)) {
            dayEndTime = ZonedDateTime.parse(day.getString(SESSION_END_TIME));
        } else {
            dayEndTime = dayStartTime;
        }
        final Optional<Boolean> isDraft = day.containsKey(MOVE_IS_DRAFT) && !day.isNull(MOVE_IS_DRAFT)
                ? Optional.of(day.getBoolean(MOVE_IS_DRAFT)) : Optional.empty();

        return uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                .withHearingDate(dayDate)
                .withStartTime(dayStartTime)
                .withEndTime(dayEndTime)
                .withDurationMinutes(durationInMinutes)
                .withSequence(sequence)
                .withCourtScheduleId(courtScheduleId)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withIsDraft(isDraft)
                .build();
    }

    private static Optional<Integer> optionalInt(final JsonObject json, final String key) {
        return json.containsKey(key) && !json.isNull(key) ? Optional.of(json.getInt(key)) : Optional.empty();
    }

    private static Optional<UUID> optionalUuid(final JsonObject json, final String key) {
        if (!json.containsKey(key) || json.isNull(key)) {
            return Optional.empty();
        }
        final String value = json.getString(key, null);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(fromString(value));
    }

    /** {@code listing.command.change-court-room-for-multiday-hearing-enriched}. */
    public void changeCourtRoomForMultidayHearing(final Envelope command) {
        LOGGER.info("'listing.command.change-court-room-for-multiday-hearing-enriched' received with payload {}",
                command.payload());

        final JsonObject payload = command.payload();
        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final Boolean sendNotificationToParties = payload.getBoolean("sendNotificationToParties", true);

        // Days already (re)booked by COMMAND_API (virtual days, plus real days whose schedule changed
        // - SPRDT-1225) -> become hearing days + schedule updates.
        final List<uk.gov.moj.cpp.listing.domain.HearingDay> changedDays = new ArrayList<>();
        final List<HearingDayCourtSchedule> changedSchedules = new ArrayList<>();
        if (payload.containsKey("changedDays")) {
            for (final JsonValue value : payload.getJsonArray("changedDays")) {
                final JsonObject day = (JsonObject) value;
                final ZonedDateTime start = ZonedDateTime.parse(day.getString("startTime"));
                final int durationMinutes = day.getInt("durationMinutes");
                // The day carries the NEW courtScheduleId and the booked session's draft state, so the
                // aggregate merge doesn't wipe them on the room change; the day never carries a sequence
                // (assignHearingDaysV2 re-derives it by startTime).
                changedDays.add(hearingDay()
                        .withHearingDate(LocalDate.parse(day.getString("hearingDate")))
                        .withStartTime(start)
                        .withEndTime(start.plusMinutes(durationMinutes))
                        .withDurationMinutes(durationMinutes)
                        .withCourtCentreId(of(fromString(day.getString(MOVE_COURT_CENTRE_ID))))
                        .withCourtRoomId(of(fromString(day.getString(MOVE_COURT_ROOM_ID))))
                        .withCourtScheduleId(of(fromString(day.getString(COURT_SCHEDULE_ID))))
                        .withIsDraft(day.containsKey(IS_DRAFT) && !day.isNull(IS_DRAFT)
                                ? of(day.getBoolean(IS_DRAFT)) : Optional.empty())
                        .build());
                changedSchedules.add(HearingDayCourtSchedule.hearingDayCourtSchedule()
                        .withHearingDate(LocalDate.parse(day.getString("hearingDate")))
                        .withCourtScheduleId(fromString(day.getString(COURT_SCHEDULE_ID)))
                        .build());
            }
        }

        // REAL days (virtual false/absent) are persisted as nonDefaultDays. A schedule-changing real
        // day ALSO travels pre-booked in changedDays above; the aggregate dedupes by date so it is
        // applied exactly once. courtCentreId/roomId/courtScheduleId are carried as uuid strings.
        // courtScheduleId may be absent (the UI has none when no bookable slot exists for the
        // room/date) - the aggregate's merge then keeps the stored day's existing schedule.
        final List<NonDefaultDay> changedNonDefaultDays = new ArrayList<>();
        if (payload.containsKey("nonDefaultDays")) {
            for (final JsonValue value : payload.getJsonArray("nonDefaultDays")) {
                final JsonObject day = (JsonObject) value;
                changedNonDefaultDays.add(NonDefaultDay.nonDefaultDay()
                        .withStartTime(ZonedDateTime.parse(day.getString("startTime")))
                        .withDuration(of(day.getInt("durationMinutes")))
                        .withCourtCentreId(of(day.getString(MOVE_COURT_CENTRE_ID)))
                        .withRoomId(of(day.getString("roomId")))
                        .withCourtScheduleId(ofNullable(day.getString(COURT_SCHEDULE_ID, null)))
                        .withVirtual(of(false))
                        .build());
            }
        }

        updateHearingEventStream(command, hearingId, (HearingRules hearing) ->
                hearing.changeCourtRoomForMultidayHearing(hearingId, changedDays, changedSchedules, changedNonDefaultDays, sendNotificationToParties));
    }

    /** {@code listing.command.hearing-vacate-trial}. */
    public void hearingVacateTrial(final Envelope command) {
        LOGGER.info("'listing.command.hearing-vacate-trial' received with payload {}", command.payload());
        final HearingVacateTrial hearingVacateTrial = jsonObjectConverter.convert(command.payload(), HearingVacateTrial.class);

        updateHearingEventStream(command, hearingVacateTrial.getHearingId(), (HearingRules hearing) -> hearing.hearingVacateTrial(ofNullable(hearingVacateTrial.getVacatedTrialReasonId())));
    }

    @SuppressWarnings({"squid:S3776", "squid:S106"})
    /** {@code listing.command.update-hearing-for-listing-enriched}. */
    public void updateHearingForListing(final Envelope command) {
        LOGGER.info("'listing.command.handler.update-hearing-for-listing-enriched' received with payload {}", command.payload());
        final UpdateHearingForListingEnriched enrichedUpdateHearingForListing = jsonObjectConverter.convert(command.payload(), UpdateHearingForListingEnriched.class);
        updateHearingForListingEnriched(command, enrichedUpdateHearingForListing);
    }

    @SuppressWarnings({"squid:S3776", "squid:S106"})
    /** {@code listing.command.update-hearings-for-listing-enriched}. */
    public void updateHearingsForListing(final Envelope command) {
        LOGGER.info("listing.command.handler.update-hearings-for-listing-enriched' received with payload {}", command.payload());
        final JsonArray updateHearingsForListingJsonArr = command.payload().getJsonArray("updateHearingsForListing");
        if (!updateHearingsForListingJsonArr.isEmpty()) {
            final Set<UUID> failedHearingIds = new LinkedHashSet<>();
            UUID lastHearingId = null;
            for (JsonValue enrichedHearingJsonVal : updateHearingsForListingJsonArr) {
                final UpdateHearingForListingEnriched currUpdateHearingForListingEnriched =
                        jsonObjectConverter.convert((JsonObject) enrichedHearingJsonVal, UpdateHearingForListingEnriched.class);
                lastHearingId = currUpdateHearingForListingEnriched.getUpdateHearingForListing().getHearingId();
                try {
                    updateHearingForListingEnriched(command, currUpdateHearingForListingEnriched);
                } catch (Exception e) {
                    final UUID hearingId = currUpdateHearingForListingEnriched.getUpdateHearingForListing().getHearingId();
                    failedHearingIds.add(hearingId);
                    String msg = format("Failed to update hearingId=%s", hearingId);
                    LOGGER.error(msg, e);
                }
            }
            // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process
            updateHearingEventStream(command, lastHearingId,
                    hearingAggregate -> hearingAggregate.hearingsUpdateCompleted(failedHearingIds));
        }
    }

    @SuppressWarnings({"squid:S3776"})
    public void updateHearingForListingEnriched(final Envelope command,
                                                final UpdateHearingForListingEnriched updateHearingForListingEnriched) {
        UpdateHearingForListing updateHearingForListing = updateHearingForListingEnriched.getUpdateHearingForListing();
        final Optional<String> source = ofNullable(updateHearingForListing.getSource());//will be removed with HMI
        final CourtCentreDetails courtCentre = updateHearingForListingEnriched.getCourtCentreDetails();

        // Mandatory fields that always require a value
        final UUID courtCentreId = getCourtCentreId(updateHearingForListing);
        final UUID hearingId = updateHearingForListing.getHearingId();

        LOGGER.info("UpdateHearingForListing for hearing id : {}", hearingId);

        final Type type = convertHearingTypeCoreToDomain(updateHearingForListing.getType());
        //will be removed because both JurisdictionType and HearingLanguage are required in schema
        final JurisdictionType jurisdictionType = JurisdictionType.valueFor(updateHearingForListing.getJurisdictionType().toString()).orElseThrow(IllegalArgumentException::new);
        final HearingLanguage hearingLanguage = valueFor(updateHearingForListing.getHearingLanguage().toString()).orElseThrow(IllegalArgumentException::new);

        final List<uk.gov.justice.listing.commands.HearingDay> hearingDays = isNotEmpty(updateHearingForListing.getHearingDays()) ? updateHearingForListing.getHearingDays() : emptyList();
        final List<NonDefaultDay> nonDefaultDays = convertNonDefaultDaysCommandToDomain(isNotEmpty(updateHearingForListing.getNonDefaultDays()) ? updateHearingForListing.getNonDefaultDays() : emptyList());
        final List<LocalDate> nonSittingDays = isNotEmpty(updateHearingForListing.getNonSittingDays()) ? updateHearingForListing.getNonSittingDays() : emptyList();
        final List<JudicialRole> judiciary = convertJudicialRolesCoreToDomain(updateHearingForListing.getJudiciary());
        final String judiciaryAssignmentSource = nonNull(updateHearingForListing.getJudiciaryAssignmentSource()) ? updateHearingForListing.getJudiciaryAssignmentSource().toString() : null;
        final boolean isJudiciaryFromCourtSchedule = Boolean.TRUE.equals(updateHearingForListing.getIsJudiciaryFromCourtSchedule());

        // Fields that may not have a value
        final UUID courtRoomId = getCourtRoomId(updateHearingForListing);
        final LocalDate startDate = updateHearingForListing.getStartDate();
        final LocalDate endDate = updateHearingForListing.getEndDate();
        final LocalDate weekCommencingStartDate = updateHearingForListing.getWeekCommencingStartDate();
        final LocalDate weekCommencingEndDate = updateHearingForListing.getWeekCommencingEndDate();
        final Integer weekCommencingDurationInWeeks = updateHearingForListing.getWeekCommencingDurationInWeeks();
        final Boolean unscheduled = source.map(value -> SOURCE_HMI.equals(value) && weekCommencingStartDate == null).orElse(false);

        final Envelope jsonEnvelope = command;

        final uk.gov.justice.listing.events.Hearing storedHearing = hearingFactory.getHearingById(hearingId);

        final boolean hasVideoLink = nonNull(updateHearingForListing.getHasVideoLink()) ? updateHearingForListing.getHasVideoLink() : false;
        final String publicListNote = updateHearingForListing.getPublicListNote();

        final Optional<String> panelFromCommand = ofNullable(updateHearingForListing.getPanel());
        final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap = new HashMap<>();
        final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap = new HashMap<>();
        final List<ProsecutionCases> prosecutionCases = updateHearingForListingEnriched.getProsecutionCases();
        final HearingUpdateOperationType operationType = extendHearingUtils.getOperationType(hearingId, storedHearing, prosecutionCases, unallocatedHearingRequestCaseMap, persistedUnallocatedHearingCasesMap, hearingDays, courtRoomId, weekCommencingStartDate);
        final Optional<String> splitHearing = StringUtils.isNotEmpty(updateHearingForListingEnriched.getUpdateHearingForListing().getSplitHearing()) ? Optional.of(updateHearingForListingEnriched.getUpdateHearingForListing().getSplitHearing()) : Optional.empty();

        LOGGER.info("HearingUpdateOperationType for hearing id: {}  is {} splitHearing is {} ", updateHearingForListing.getHearingId(), operationType, splitHearing);

        updateHearingEventStream(jsonEnvelope, hearingId, (HearingRules hearing) -> {

            LOGGER.info("UpdateHearingEventStream for hearing id: {} with operationType: {} ", hearingId, operationType);

            // Splits are performed by progression via the listing proxy, never through this command.
            // Rejected before any event is built so the original hearing's stream is left untouched:
            // a partial update here would move the original's room/date, which is what SPRDT-1227 was.
            if (HearingUpdateOperationType.SPLIT.equals(operationType)) {
                LOGGER.error("{} hearingId={} correlationId={}",
                        SPLIT_VIA_UPDATE_HEARING_REJECTED,
                        hearingId,
                        command.metadata().clientCorrelationId().orElse(null));
                return Stream.empty();
            }

            final Stream<Object> hearingPartiallyEvents = extendHearingUtils.createPartiallyAllocationEventForUpdateHearing(hearing,
                    hearingId,
                    unallocatedHearingRequestCaseMap,
                    persistedUnallocatedHearingCasesMap,
                    operationType,
                    splitHearing);

            final Boolean isNotificationRelatedAllocatedFieldsUpdated = hearing.isNotificationRelatedAllocatedFieldsUpdated(hearingDays);

            final Stream<Object> typeEvents = hearing.changeType(type, hearingId);
            final Stream<Object> jurisdictionTypeEvents = hearing.changeJurisdictionType(jurisdictionType, hearingId);
            final Stream<Object> hearingLanguageEvents = hearing.changeHearingLanguage(hearingLanguage, hearingId);

            final Stream<Object> startDateEvents = startDate != null ?
                    hearing.changeStartDate(startDate, hearingId) : hearing.removeStartDate(hearingId, unscheduled);
            final Stream<Object> endDateEvents = endDate != null ?
                    hearing.changeEndDate(endDate, hearingId) : hearing.removeEndDate(hearingId);

            final Stream<Object> nonDefaultDaysEvents = hearing.assignNonDefaultDays(nonDefaultDays.stream()
                    .filter(nonDefaultDay -> Boolean.FALSE.equals(nonDefaultDay.getVirtual().orElse(Boolean.FALSE)))
                    .collect(toList()), hearingId);

            final Stream<Object> nonSittingDaysEvents = hearing.assignNonSittingDays(nonSittingDays, hearingId);
            final Stream<Object> courtCentreEvents = hearing.changeCourtCentre(courtCentreId, hearingId);

            final JsonObject organisationUnitJsonObject = courtCentreFactory.getOrganisationUnit(courtCentreId);
            final String ouCode = organisationUnitJsonObject.getString(OUCODE);

            // Must run before court room / hearing day events so the aggregate still holds the old court sessions
            final Stream<Object> judiciaryEvents = getJudiciaryEvents(hearingId, judiciary, hearing, judiciaryAssignmentSource, isJudiciaryFromCourtSchedule, hearingDays);

            //Capture old courtRoomId before it is updated
            final UUID oldCourtRoomId = hearing.getCurrentHearingEventState() == null ? null: hearing.getCurrentHearingEventState().getCourtRoomId();
            // Check endDate and court-room last as these are the key fields for allocation
            final Stream<Object> courtRoomEvents = courtRoomId != null ?
                    hearing.assignCourtRoom(courtRoomId, hearingId, panelFromCommand) : hearing.removeCourtRoom(hearingId);

            final List<LocalDate> daysOfNonDefaultDays =  updateHearingForListing.getNonDefaultDays() == null ? emptyList(): updateHearingForListing.getNonDefaultDays().stream().filter(ndd -> ndd.getStartTime() != null).map(ndd -> ndd.getStartTime().toLocalDate()).toList();
            final Stream<Object> hearingDayEvents = hearing.assignHearingDaysV2(hearingId, convertHearingDaysCommandToDomain(updateHearingForListing.getHearingDays()), oldCourtRoomId, courtRoomId, uk.gov.justice.core.courts.JurisdictionType.valueOf(jurisdictionType.name()), daysOfNonDefaultDays);

            final Stream<Object> weekCommencingDateEvents = weekCommencingStartDate != null && weekCommencingEndDate != null ?
                    hearing.changeWeekCommencingDate(weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingId) : hearing.removeWeekCommencingDates(hearingId);

            final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = prosecutionCaseDefendantOffenceIdsBuilder.buildFromProsecutionCases(updateHearingForListingEnriched.getProsecutionCases());
            final Stream<Object> allocationEvents = hearing.applyAllocationRules(prosecutionCaseDefendantOffenceIds, source, updateHearingForListing.getSendNotificationToParties(), isNotificationRelatedAllocatedFieldsUpdated, null);


            final List<Object> startDateEventList = startDateEvents.collect(toList());

            final Stream<Object> rescheduledEvents = hearing.applyRescheduledCheck(startDateEventList);

            Stream<Object> publicListNoteUpdateEvent = Stream.empty();

            if (publicListNote != null) {
                publicListNoteUpdateEvent = hearing.assignPublicListNote(publicListNote, hearingId);
            }
            final Stream<Object> videoLinkUpdateEvent = hearing.assignVideoLink(hasVideoLink, hearingId);

            return Stream.of(typeEvents, jurisdictionTypeEvents, hearingLanguageEvents, startDateEventList.stream(), nonDefaultDaysEvents, endDateEvents,
                    nonSittingDaysEvents, courtCentreEvents, judiciaryEvents, courtRoomEvents, hearingDayEvents, allocationEvents, weekCommencingDateEvents, hearingPartiallyEvents,
                    rescheduledEvents, videoLinkUpdateEvent, publicListNoteUpdateEvent).flatMap(i -> i);
        });
    }

    private Stream<Object> getJudiciaryEvents(final UUID hearingId, final List<JudicialRole> judiciary, final HearingRules hearing, final String judiciaryAssignmentSource,
                                              final boolean isJudiciaryFromCourtSchedule, final List<uk.gov.justice.listing.commands.HearingDay> hearingDays) {
        return hearing.updateJudiciary(judiciary, hearingId, judiciaryAssignmentSource, convertHearingDaysCommandToDomain(hearingDays), isJudiciaryFromCourtSchedule);
    }

    private UUID getCourtRoomId(final UpdateHearingForListing updateHearingForListing) {
        final Optional<SelectedCourtCentre> selectedCourtCentre = ofNullable(updateHearingForListing.getSelectedCourtCentre());
        if (selectedCourtCentre.isPresent() && CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            return selectedCourtCentre.get().getCourtRoomId();
        }
        return updateHearingForListing.getCourtRoomId();
    }

    private UUID getCourtCentreId(final UpdateHearingForListing updateHearingForListing) {
        final Optional<SelectedCourtCentre> selectedCourtCentre = ofNullable(updateHearingForListing.getSelectedCourtCentre());
        if (selectedCourtCentre.isPresent() && CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            return selectedCourtCentre.get().getId();
        }
        return updateHearingForListing.getCourtCentreId();
    }

    /** {@code listing.command.extend-hearing-for-hearing-enriched}. */
    public void extendHearingForHearing(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.extend-hearing-for-hearing-enriched' received with payload {}", command.payload());
        }

        final ExtendHearingForHearingEnriched extendHearingForHearingEnriched = jsonObjectConverter.convert(payload, ExtendHearingForHearingEnriched.class);

        final UUID allocatedHearingId = extendHearingForHearingEnriched.getAllocatedHearingId();
        final UUID unAllocatedHearingId = extendHearingForHearingEnriched.getUnAllocatedHearingId();

        if (isHearingDeleted(allocatedHearingId, unAllocatedHearingId)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(" listing.command.extend-hearing-for-hearing-enriched one of hearings is deleted for allocatedHearingId {}, or unAllocatedHearingId{} ", allocatedHearingId, unAllocatedHearingId);
            }
        } else {
            final uk.gov.justice.listing.events.Hearing allocatedHearing = hearingFactory.getHearingById(allocatedHearingId);
            final uk.gov.justice.listing.events.Hearing unAllocatedHearingPersisted = hearingFactory.getHearingById(unAllocatedHearingId);

            boolean partialExtension = false;
            final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap = new HashMap<>();
            final List<ProsecutionCases> prosecutionCases = extendHearingForHearingEnriched.getProsecutionCases();

            if (prosecutionCases != null && !prosecutionCases.isEmpty()) {
                partialExtension = comparePersistedAndRequestedCaseMaps(unallocatedHearingRequestCaseMap, unAllocatedHearingPersisted, prosecutionCases, extendHearingForHearingEnriched);
            }
            final boolean fullExtension = !partialExtension;

            if (partialExtension) {

                final List<ListedCase> casesToMove = extractListedCasesToAllocate(unAllocatedHearingPersisted, unallocatedHearingRequestCaseMap);

                //remove the cases/defendants/offences given in the request, from persisted hearing
                final uk.gov.justice.listing.events.Hearing unallocatedHearingPersisted = extendHearingUtils.updateUnallocatedHearing(unAllocatedHearingPersisted, unallocatedHearingRequestCaseMap);

                final List<uk.gov.justice.listing.events.ProsecutionCases> prosecutionCasesToBeRemovedFromHearing = prosecutionCasesBuilder.buildEventProsecutionCase(unallocatedHearingRequestCaseMap);

                updateHearingEventStream(command, allocatedHearingId, (HearingRules hearing) -> {
                    final Stream<Object> updatedHearing = hearing.updatedListedCasesInHearing(allocatedHearing, unallocatedHearingPersisted, casesToMove);
                    final Stream<Object> allocationEvents = hearing.applyAllocationRulesForExtendedHearing(unallocatedHearingPersisted, fullExtension, extendHearingForHearingEnriched.getSendNotificationToParties());
                    final Stream<Object> addCaseEvent = hearing.addCasesToUnAllocatedHearing(casesToMove, unAllocatedHearingId);
                    final Stream<Object> hearingMarkedForPartialUpdated = hearing.markUnallocatedHearingForPartialUpdate(unAllocatedHearingId, prosecutionCasesToBeRemovedFromHearing);
                    final Stream<Object> emitYouthCourtListRestrictionsEvents = hearing.emitYouthCourtListRestrictions();
                    return Stream.of(addCaseEvent, updatedHearing, allocationEvents, hearingMarkedForPartialUpdated, emitYouthCourtListRestrictionsEvents).flatMap(i -> i);
                });

            } else {
                //do the whole extension as in before GPE-13108
                // do not add the call to delete Unallocated hearing here as we are sending command for the same in progression when hearing confirmation event is processed.
                final List<UUID> allocatedHearingCasesId = extractListCasesId(allocatedHearing);
                final List<UUID> unAllocatedHearingCasesId = extractListCasesId(unAllocatedHearingPersisted);

                if (Boolean.FALSE.equals(unAllocatedHearingPersisted.getAllocated())) {
                    updateHearingEventStream(command, allocatedHearingId, (HearingRules hearing) -> {
                        final Stream<Object> updatedHearing = hearing.updatedListedCasesInHearing(allocatedHearing, unAllocatedHearingPersisted, unAllocatedHearingPersisted.getListedCases());
                        final Stream<Object> allocationEvents = hearing.applyAllocationRulesForExtendedHearing(unAllocatedHearingPersisted, fullExtension, extendHearingForHearingEnriched.getSendNotificationToParties());
                        final Stream<Object> addCaseEvent = hearing.addCasesToUnAllocatedHearing(unAllocatedHearingPersisted.getListedCases(), unAllocatedHearingId);
                        return Stream.of(addCaseEvent, updatedHearing, allocationEvents).flatMap(i -> i);
                    });
                } else {
                    LOGGER.info("incoming list cases : {} cannot be added in allocated hearing as same case id : {} ", unAllocatedHearingCasesId, allocatedHearingCasesId);
                }
            }
        }
    }


    private boolean isHearingDeleted(final UUID allocatedHearingId, final UUID unAllocatedHearingId) {
        // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process
        final HearingRules allocatedHearing = hearingFlow.load(allocatedHearingId);
        final HearingRules unAllocatedHearing = hearingFlow.load(unAllocatedHearingId);
        return allocatedHearing.isDuplicateOrDeleted() || unAllocatedHearing.isDuplicateOrDeleted();
    }

    /** {@code listing.command.remove-partially-merged-offences-from-original-hearing}. */
    public void handleRemovePartiallyMergedOffencesFromOriginalHearing(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.remove-partially-merged-offences-from-original-hearing' received with payload {}", command.payload());
        }

        final RemovePartiallyMergedOffencesFromOriginalHearing removePartiallyMergedOffencesFromOriginalHearing = jsonObjectConverter.convert(payload, RemovePartiallyMergedOffencesFromOriginalHearing.class);

        updateHearingEventStream(command, removePartiallyMergedOffencesFromOriginalHearing.getHearingIdToBeUpdated(), (HearingRules hearing) -> {
            final List<uk.gov.justice.listing.events.ProsecutionCases> prosecutionCases = prosecutionCasesBuilder.buildEventProsecutionCasesToRemove(removePartiallyMergedOffencesFromOriginalHearing.getProsecutionCasesToRemove());
            return hearing.updateUnallocatedHearingPartially(removePartiallyMergedOffencesFromOriginalHearing.getHearingIdToBeUpdated(), prosecutionCases, Optional.empty());
        });
    }

    /** {@code listing.command.mark-hearing-as-deleted}. */
    public void handleMarkHearingAsDeleted(final Envelope command) {

        final JsonObject payload = command.payload();
        final UUID hearingIdToMarkAsDeleted = fromString(payload.getString("hearingIdToMarkAsDeleted"));
        LOGGER.info("'listing.command.mark-hearing-as-deleted' received for hearing {}", hearingIdToMarkAsDeleted);

        updateHearingEventStream(command, hearingIdToMarkAsDeleted, (HearingRules hearing) -> {
            final Stream<Object> hearingMarked = hearing.deleteUnAllocatedHearing();
            return Stream.of(hearingMarked).flatMap(i -> i);
        });

    }

    /** {@code listing.command.add-cases-to-hearing}. */
    public void handleAddCasesToHearing(final Envelope command) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-cases-to-hearing' received with payload {}", command.payload());
        }

        final AddCasesToHearing addCasesToHearing = jsonObjectConverter.convert
                (command.payload(), AddCasesToHearing.class);

        // LPT-2405: inherited tier / list type for an already-existing next hearing
        final PtphDetail ptphDetail = (isNull(addCasesToHearing.getTier()) && isNull(addCasesToHearing.getListType()) && isNull(addCasesToHearing.getKeyReason()))
                ? null
                : new PtphDetail(addCasesToHearing.getTier(), addCasesToHearing.getListType(), addCasesToHearing.getKeyReason());

        updateHearingEventStream(command, addCasesToHearing.getHearingId(), (HearingRules hearing) -> hearing.addCasesToHearing(addCasesToHearing.getProsecutionCases(), addCasesToHearing.getShadowListedOffences(), ofNullable(addCasesToHearing.getSeedingHearingId()), ptphDetail));
    }








    /** {@code listing.command.change-judiciary-for-hearings}. */
    public void changeJudiciaryForHearings(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.change-judiciary-for-hearings' received with payload {}", command.payload());
        }
        final ChangeJudiciaryForHearings changeJudiciaryForHearings = jsonObjectConverter.convert(command.payload(), ChangeJudiciaryForHearings.class);
        final List<UUID> hearingIds = changeJudiciaryForHearings.getHearings();
        final List<uk.gov.justice.core.courts.JudicialRole> judicialRolesCommand = changeJudiciaryForHearings.getJudiciary();
        final List<JudicialRole> judicialRoles = convertJudicialRolesCoreToDomain(judicialRolesCommand);

        for (final UUID hearingId : hearingIds) {
            updateHearingEventStream(command, hearingId, (HearingRules hearing) -> {
                final String judiciaryAssignmentSource = nonNull(changeJudiciaryForHearings.getJudiciaryAssignmentSource()) ? changeJudiciaryForHearings.getJudiciaryAssignmentSource().toString() : null;
                final Stream<Object> judicialEvents = hearing.assignJudiciary(judicialRoles, hearingId, judiciaryAssignmentSource);
                final Stream<Object> allocationEvents = hearing.applyAllocationRules(Collections.emptyList(), false, false);
                return Stream.of(allocationEvents, judicialEvents).flatMap(i -> i);
            });
        }

        if (isNotEmpty(hearingIds)) {
            final UUID last = hearingIds.get(hearingIds.size() - 1);
            // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process
            updateHearingEventStream(command, last, HearingRules::judiciaryChangedForHearingsStatus);
        }

    }

    /** {@code listing.command.sequence-hearings}. */
    public void sequenceHearings(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.sequence-hearings' received with payload {}", command.payload());
        }
        final SequenceHearings sequenceHearings = jsonObjectConverter.convert(command.payload(), SequenceHearings.class);
        final List<uk.gov.moj.cpp.listing.domain.SequenceHearing> sequenceHearingList = convertSequenceHearingsToDomain(sequenceHearings);

        for (final uk.gov.moj.cpp.listing.domain.SequenceHearing sequenceHearing : sequenceHearingList) {
            updateHearingEventStream(command, sequenceHearing.getId(), (HearingRules hearing) -> hearing.sequenceHearingDays(sequenceHearing));
        }
    }

    /** {@code listing.command.update-court-application-for-hearings}. */
    public void updateCourtApplicationForHearings(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-court-application-for-hearings' received with payload {}", command.payload());
        }

        final UpdateCourtApplicationForHearings updateCourtApplicationForHearings = jsonObjectConverter.convert
                (command.payload(), UpdateCourtApplicationForHearings.class);
        final List<UUID> hearingIds = updateCourtApplicationForHearings.getHearingIds();
        final uk.gov.justice.listing.courts.CourtApplication courtApplication = updateCourtApplicationForHearings.getCourtApplication();
        final uk.gov.moj.cpp.listing.domain.CourtApplication courtApplicationDomain = courtApplicationToDomainConverter.convertListingCoreCourtApplication(courtApplication);

        for (final UUID hearingId : hearingIds) {
            updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.updateCourtApplication(hearingId, courtApplicationDomain));
        }
    }

    /** {@code listing.command.add-court-application-for-hearing}. */
    public void addCourtApplicationForHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-court-application-for-hearing' received with payload {}", command.payload());
        }

        final AddCourtApplicationForHearing addCourtApplicationToHearing = jsonObjectConverter.convert
                (command.payload(), AddCourtApplicationForHearing.class);
        final UUID hearingId = addCourtApplicationToHearing.getHearingId();
        final CourtApplication courtApplication = addCourtApplicationToHearing.getCourtApplication();

        final uk.gov.moj.cpp.listing.domain.CourtApplication courtApplicationDomain = courtApplicationToDomainConverter.convert(courtApplication);

        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.addCourtApplication(hearingId, courtApplicationDomain));
    }












    /** {@code listing.command.restrict-court-list}. */
    public void restrictFromCourtList(final Envelope command) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.restrict-court-list' received with payload {}", command.payload());
        }

        final RestrictCourtList restrictCourtList = jsonObjectConverter.convert(command.payload(), RestrictCourtList.class);

        updateHearingEventStream(command, restrictCourtList.getHearingId(), (HearingRules hearing) ->
                hearing.restrictDetailsFromCourt(restrictCourtList.getHearingId(), convertRestrictCourtListToDomain(restrictCourtList)));


    }


















    /** {@code listing.command.handler.modify-hearing-counsel}. */
    public void modifyHearingCounsels(final Envelope commandEnvelope) {
        final JsonObject payload = commandEnvelope.payload();

        LOGGER.info("'listing.command.handler.modify-hearing-counsel' received with payload {}",
                commandEnvelope.payload());

        final ModifyHearingCounselCommand modifyHearingCounsels = jsonObjectConverter.convert(payload, ModifyHearingCounselCommand.class);
        // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process
        // The event is built here, not by the aggregate; apply() keeps the snapshot as a replay would have.
        updateHearingEventStream(commandEnvelope, modifyHearingCounsels.getHearingId(), hearingAggregate -> {
        Stream<Object> events = Stream.empty();
        if(!hearingAggregate.isDuplicateOrDeleted()) {
            final HearingCounselModified event = new HearingCounselModified(
                    uk.gov.justice.listing.event.Action.valueFor(modifyHearingCounsels.getAction().name()).orElse(null),
                    uk.gov.justice.listing.event.CounselType.valueFor(modifyHearingCounsels.getCounselType().name()).orElse(null),
                    modifyHearingCounsels.getHearingId(),
                    modifyHearingCounsels.getPayload());
            events = Stream.of(event);
        }
        final List<Object> raised = events.collect(toList());
        raised.forEach(hearingAggregate::apply);
        return raised.stream();
        });
    }

    /** {@code listing.command.cancel-hearing-days}. */
    public void cancelHearingDays(final Envelope envelope) {
        final CancelHearingDays payload = jsonObjectConverter.convert(envelope.payload(), CancelHearingDays.class);

        // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process
        updateHearingEventStream(envelope, payload.getHearingId(),
                hearingAggregate -> hearingAggregate.cancelHearingDays(payload.getHearingId(), hearingDaysCoreToDomainConverter.convert(payload.getHearingDays())));
    }

    /** {@code listing.command.update-hearing-day-court-schedule}. */
    public void updateHearingDayCourtSchedule(final Envelope commandEnvelope) {
        final JsonObject payload = commandEnvelope.payload();
        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final List<HearingDayCourtSchedule> hearingDayCourtSchedules = toHearingDayCourtSchedules(payload);
        updateHearingEventStream(commandEnvelope,
                hearingId,
                hearing -> hearing.raiseHearingDayCourtSchedulesUpdated(hearingId, hearingDayCourtSchedules));
    }


    private List<HearingDayCourtSchedule> toHearingDayCourtSchedules(final JsonObject source) {
        final List<HearingDayCourtSchedule> hearingDayCourtSchedules = new ArrayList<>();
        source.getJsonArray(HEARING_DAY_COURT_SCHEDULES)
                .getValuesAs(JsonObject.class)
                .forEach(hearingDayCourtSchedule -> hearingDayCourtSchedules.add(
                        jsonObjectConverter.convert(hearingDayCourtSchedule, HearingDayCourtSchedule.class)));
        return hearingDayCourtSchedules;
    }

    /** {@code listing.command.update-cps-prosecutor-with-associated-hearings}. */
    public void updateProsecutorForAssociatedHearings(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-cps-prosecutor-with-associated-hearings event received {}", envelope.payload());
        }

        final UpdateCaseIdentifierWithHearings updateCpsProsecutorWithHearings = jsonObjectConverter.convert(envelope.payload(), UpdateCaseIdentifierWithHearings.class);

        for (final UUID hearingId : updateCpsProsecutorWithHearings.getHearingIds()) {
            // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process
            updateHearingEventStream(envelope, hearingId,
                    hearingAggregate -> hearingAggregate.updateCaseIdentifier(updateCpsProsecutorWithHearings.getProsecutionAuthorityId(), updateCpsProsecutorWithHearings.getProsecutionAuthorityCode(), updateCpsProsecutorWithHearings.getProsecutionCaseId()));
        }
    }


    /** {@code listing.command.delete-hearing}. */
    public void deleteHearing(final Envelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.delete-hearing event received {}", envelope.payload());
        }

        final JsonObject payload = envelope.payload();

        final UUID deletedHearingId = fromString(payload.getString(HEARING_ID));
        LOGGER.info("'listing.command.delete-hearing' received for hearing {}", deletedHearingId);

        updateHearingEventStream(envelope, deletedHearingId, (HearingRules hearing) -> {
            final Stream<Object> hearingDeleted = hearing.markHearingAsDeleted(deletedHearingId);
            return Stream.of(hearingDeleted).flatMap(i -> i);
        });
    }

    /** {@code listing.command.update-hearing-add-case-bdf}. */
    public void updateHearingAddCaseBdf(final Envelope command) {
        final JsonObject payload = command.payload();
        final UpdateHearingAddCaseBdf updateHearingAddCaseBdf = jsonObjectConverter.convert(payload, UpdateHearingAddCaseBdf.class);
        updateHearingEventStream(command, updateHearingAddCaseBdf.getHearingId(), (HearingRules hearing) -> hearing.addCasesForHearing(Collections.singletonList(updateHearingAddCaseBdf.getProsecutionCase()), new ArrayList<>()));
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

    private List<JudicialRole> convertJudicialRolesCoreToDomain(final List<uk.gov.justice.core.courts.JudicialRole> commandJudiciary) {
        List<JudicialRole> domainJudiciary = new ArrayList<>();
        if (commandJudiciary != null && !commandJudiciary.isEmpty()) {
            domainJudiciary = commandJudiciary.stream().map(jr -> JudicialRole.judicialRole()
                    .withIsBenchChairman(ofNullable(jr.getIsBenchChairman()))
                    .withIsDeputy(ofNullable(jr.getIsDeputy()))
                    .withJudicialId(jr.getJudicialId())
                    .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                            .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                            .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
                            .build())
                    .withUserId(jr.getUserId())
                    .build()).collect(toList());
        }
        return domainJudiciary;
    }

    private Type convertHearingTypeCoreToDomain(final uk.gov.justice.core.courts.HearingType hearingType) {
        return Type.type()
                .withId(hearingType.getId())
                .withDescription(hearingType.getDescription())
                .withWelshDescription(hearingType.getWelshDescription())
                .build();
    }

    private List<NonDefaultDay> convertNonDefaultDaysCommandToDomain(final List<uk.gov.justice.listing.commands.NonDefaultDay> commandDefaultDays) {
        List<NonDefaultDay> domainDefaultDays;
        if (commandDefaultDays != null && !commandDefaultDays.isEmpty()) {
            domainDefaultDays = commandDefaultDays.stream().map(ndd -> NonDefaultDay.nonDefaultDay()
                            .withStartTime(ndd.getStartTime())
                            .withDuration(of(getAdjustedDuration(ndd.getDuration())))
                            .withCourtScheduleId(ofNullable(ndd.getCourtScheduleId()))
                            .withCourtRoomId(ofNullable(ndd.getCourtRoomId()))
                            .withOucode(ofNullable(ndd.getOucode()))
                            .withSession(ofNullable(ndd.getSession()))
                            .withRoomId(ofNullable(ndd.getRoomId()))
                            .withCourtCentreId(ofNullable(ndd.getCourtCentreId()))
                            .withVirtual(Optional.ofNullable(ndd.getVirtual()))
                            .build())
                    .collect(toList());
        } else {
            domainDefaultDays = new ArrayList<>();
        }
        return domainDefaultDays;
    }

    private static List<HearingDay> convertHearingDaysCommandToDomain(final List<uk.gov.justice.listing.commands.HearingDay> commandHearingDays) {
        if (isEmpty(commandHearingDays)) {
            return emptyList();
        }

        return commandHearingDays.stream()
                .map(commandHearingDay -> HearingDay.hearingDay()
                        .withCourtCentreId(Optional.ofNullable(commandHearingDay.getCourtCentreId()))
                        .withCourtRoomId(Optional.ofNullable(commandHearingDay.getCourtRoomId()))
                        .withCourtScheduleId(Optional.ofNullable(commandHearingDay.getCourtScheduleId()))
                        .withDurationMinutes(commandHearingDay.getDurationMinutes())
                        .withStartTime(commandHearingDay.getStartTime())
                        .withEndTime(nonNull(commandHearingDay.getEndTime()) ? commandHearingDay.getEndTime() : commandHearingDay.getStartTime().plusMinutes(commandHearingDay.getDurationMinutes()))
                        .withHearingDate(commandHearingDay.getHearingDate())
                        .withIsCancelled(Optional.ofNullable(commandHearingDay.getIsCancelled()))
                        .withIsDraft(Optional.ofNullable(commandHearingDay.getIsDraft()))
                        .withSequence(commandHearingDay.getSequence())
                        .build())
                .toList();
    }

    private List<NonDefaultDay> convertNonDefaultDaysCoreToDomain(final List<uk.gov.justice.core.courts.NonDefaultDay> coreNonDefaultDays) {
        if (isEmpty(coreNonDefaultDays)) {
            return Collections.emptyList();
        }
        return coreNonDefaultDays.stream().map(cnnd -> NonDefaultDay.nonDefaultDay()
                .withCourtCentreId(Optional.of(cnnd.getCourtCentreId()))
                .withCourtRoomId(Optional.ofNullable(cnnd.getCourtRoomId()))
                .withSession(ofNullable(cnnd.getSession()))
                .withRoomId(ofNullable(cnnd.getRoomId()))
                .withDuration(of(cnnd.getDuration()))
                .withStartTime(cnnd.getStartTime())
                .withOucode(Optional.ofNullable(cnnd.getOucode()))
                .withCourtScheduleId(ofNullable(cnnd.getCourtScheduleId()))
                .build()).toList();
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
                .withCourtApplicationSubjectIds(restrictCourtList.getCourtApplicationSubjectIds())
                .withCourtApplicationType(restrictCourtList.getCourtApplicationType())
                .withRestrictFromCourtList(restrictCourtList.getRestrictCourtList())
                .build();
    }

    // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process
    private void updateHearingEventStream(final Envelope command, final UUID hearingId,
                                          final Function<HearingRules, Stream<Object>> aggregatorFunction) {
        hearingFlow.update(command.metadata(), hearingId, aggregatorFunction);
    }

    // MBD refactor P1f: HearingRules state in aggregate_snapshot, events recorded in-process







    private List<UUID> extractListCasesId(final uk.gov.justice.listing.events.Hearing hearing) {
        return hearing.getListedCases().stream().map(ListedCase::getId).collect(toList());
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


    // ---------------------------------------------------------------------------------------------
    // UnscheduledListingCommandHandler (the Hearing-stream commands; list-unscheduled-next-hearing is
    // below, list-unscheduled-next-hearings-enriched is in SeedHearingCommandHandler)
    // ---------------------------------------------------------------------------------------------

    /** {@code listing.command.list-unscheduled-court-hearing-enriched}. */
    @SuppressWarnings({"squid:S3655", "squid:S1188"})
    public void handleListUnscheduledCourtHearing(final Envelope command) {
        final JsonObject payload = command.payload();

        final ListUnscheduledCourtHearingEnriched listCourtHearingEnriched = jsonObjectConverter.convert(payload, ListUnscheduledCourtHearingEnriched.class);

        LOGGER.info("'listing.command.list-unscheduled-court-hearing-enriched' listCourtHearing: {}", listCourtHearingEnriched);

        final List<HearingUnscheduledListingNeeds> listCourtHearing = listCourtHearingEnriched.getHearings();
        final Map<UUID, CourtCentreDetails> courtCentres = listCourtHearingEnriched.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));

        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap();

        for (final HearingUnscheduledListingNeeds commandHearing : listCourtHearing) {
            listUnscheduledHearing(command, hearingTypesIdDurationMap, commandHearing, courtCentres, null);
        }
    }

    @SuppressWarnings({"squid:S3655", "squid:S1188"})
    private void listUnscheduledHearing(final Envelope command, final Map<String, Integer> hearingTypesIdDurationMap, final HearingUnscheduledListingNeeds commandHearing,
                                        final Map<UUID, CourtCentreDetails> courtCentres, final PtphDetail ptphDetail) {
        final Optional<LocalDate> weekCommencingStartDate = commandToDomainConverter.getWeekCommencingStartDate(commandHearing);
        final Optional<Integer> weekCommencingDurationInWeeks = commandToDomainConverter.getWeekCommencingDurationInWeeks(commandHearing);
        final Optional<LocalDate> weekCommencingEndDate = commandToDomainConverter.getWeekCommencingEndDate(weekCommencingStartDate, weekCommencingDurationInWeeks);

        final Integer hearingDuration = resolveHearingDuration(commandHearing, hearingTypesIdDurationMap);

        updateHearingEventStream(command, commandHearing.getId(), (HearingRules hearing) -> hearing.listUnscheduled(
                commandHearing.getId(),
                commandToDomainConverter.buildHearingType(commandHearing.getType()),
                commandToDomainConverter.mapToListedCases(commandHearing, commandHearing.getProsecutionCases()),
                commandHearing.getCourtCentre().getId(),
                commandToDomainConverter.getJudicialRoles(commandHearing),
                commandHearing.getCourtCentre().getRoomId(),
                commandHearing.getListingDirections(),
                commandToDomainConverter.getJurisdictionType(commandHearing),
                commandHearing.getProsecutorDatesToAvoid(),
                commandHearing.getReportingRestrictionReason(),
                CommandToDomainConverter.extractStartDate(commandHearing),
                nonNull(commandHearing.getEndDate()) ? LocalDate.parse(commandHearing.getEndDate()) : null,
                commandToDomainConverter.getCourtCentreDefaults(courtCentres, commandHearing),
                commandToDomainConverter.getCourtApplications(commandHearing),
                commandToDomainConverter.getCourtApplicationPartyListingNeeds(commandHearing),
                hearingDuration,
                weekCommencingStartDate,
                weekCommencingEndDate,
                weekCommencingDurationInWeeks,
                commandToDomainConverter.convertTypeOfList(commandHearing.getTypeOfList()),
                ptphDetail));
    }

    /** The user-entered estimatedMinutes when meaningful (&gt; 1), else the hearing type's default (SPRDT-806/807). */
    private static Integer resolveHearingDuration(final HearingUnscheduledListingNeeds commandHearing,
                                                  final Map<String, Integer> hearingTypesIdDurationMap) {
        final Integer userEntered = commandHearing.getEstimatedMinutes();
        if (userEntered != null && userEntered > 1) {
            return HearingDurationDefaults.coerceToValidDuration(userEntered);
        }
        return HearingDurationDefaults.resolveHearingTypeDuration(
                commandHearing.getType() != null && commandHearing.getType().getId() != null
                        ? commandHearing.getType().getId().toString() : null,
                hearingTypesIdDurationMap);
    }

    // ---------------------------------------------------------------------------------------------
    // DeleteCourtApplicationHandler, HearingResultedCommandHandler
    // ---------------------------------------------------------------------------------------------

    /** {@code listing.command.delete-court-application-hearing}. */
    public void handleDeleteCourtApplicationHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.delete-court-application-hearing' received with payload {}", command.payload());
        }
        final JsonObject payload = command.payload();
        final UUID hearingId = fromString(payload.getString("hearingId"));

        updateHearingEventStream(command, hearingId, (HearingRules hearing) ->
                hearing.deleteCourtApplicationHearing(hearingId));
    }

    /** {@code listing.command.set-hearing-resulted-status}. */
    public void handleSetHearingResultStatus(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.set-hearing-resulted-status' received with payload {}", command.payload());
        }
        final UUID hearingId = UUID.fromString(command.payload().getString("hearingId", null));
        if (nonNull(hearingId)) {
            updateHearingEventStream(command, hearingId, (HearingRules hearing) ->
                    hearing.setHearingResultStatus(hearingId));
        } else {
            LOGGER.warn("Hearing ID is null in the command payload");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ListNextHearingCommandHandler (the Hearing-stream commands; the seed ones are in SeedHearingCommandHandler)
    // ---------------------------------------------------------------------------------------------

    /** {@code listing.command.change-next-hearing-day}. */
    public void changeNextHearingDay(final Envelope command) {
        final JsonObject payload = command.payload();
        final ChangeNextHearingDay changeNextHearingDay = jsonObjectConverter.convert(payload, ChangeNextHearingDay.class);

        final UUID hearingId = changeNextHearingDay.getHearingId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.change-next-hearing-day hearingId: {} ", hearingId);
        }

        updateHearingEventStream(command, hearingId, (HearingRules hearing) ->
                hearing.changeNextHearingDate(hearingId));
    }

    /** {@code listing.command.remove-offences-from-existing-hearing}. */
    public void removeOffencesFromExistingHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.remove-offences-from-existing-hearing' received with payload {}", command.payload());
        }

        final RemoveOffencesFromExistingHearing removeOffencesFromExistingHearing = jsonObjectConverter.convert(command.payload(), RemoveOffencesFromExistingHearing.class);
        final UUID hearingId = removeOffencesFromExistingHearing.getHearingId();
        final UUID seedingHearingId = removeOffencesFromExistingHearing.getSeedingHearingId();

        updateHearingEventStream(command, removeOffencesFromExistingHearing.getHearingId(), (HearingRules hearing) -> hearing.removeOffencesFromExistingHearing(seedingHearingId, hearingId));
    }

    /** {@code listing.command.remove-selected-offences-from-existing-hearing}. */
    public void removeSelectedOffencesFromExistingHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.remove-selected-offences-from-existing-hearing' received with payload {}", command.payload());
        }

        final RemoveSelectedOffencesFromExistingHearing removeSelectedOffencesFromExistingHearing = jsonObjectConverter.convert(command.payload(), RemoveSelectedOffencesFromExistingHearing.class);
        final UUID hearingId = removeSelectedOffencesFromExistingHearing.getHearingId();

        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.removeSelectedOffencesFromExistingHearing(hearingId, removeSelectedOffencesFromExistingHearing.getOffenceIds(), HearingRules.SOURCE_HEARING, false));
    }

    // ---------------------------------------------------------------------------------------------
    // P2.3.3: the remaining Hearing-stream commands (ListingCommandHandler, ListNextHearingCommandHandler,
    // UnscheduledListingCommandHandler, HearingMarkedAsDuplicateCommandHandler)
    // ---------------------------------------------------------------------------------------------

    private static final String HEARINGS = "hearings";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    private static final String LEGAL_AID_STATUS = "legalAidStatus";
    private static final String APPLICATION_ID = "applicationId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String REMOVAL_REASON = "removalReason";
    private static final String SUMMONS_REJECTED_RESULT_TYPE_ID = "d8837a45-8281-49b3-8349-49b423193148";

    /** {@code listing.command.migrate-crown-hearings-to-courtschedules}. */
    public void migrateCrownHearingsToCourtSchedules(final Envelope commandEnvelope) {
        final JsonObject payload = commandEnvelope.payload();
        for (final JsonObject hearing : payload.getJsonArray(HEARINGS).getValuesAs(JsonObject.class)) {
            final UUID hearingId = fromString(hearing.getString(HEARING_ID));
            final List<HearingDayCourtSchedule> hearingDayCourtSchedules = toHearingDayCourtSchedules(hearing);
            updateHearingEventStream(commandEnvelope, hearingId,
                    hearingAggregate -> hearingAggregate.raiseCrownHearingMigratedToCourtSchedule(hearingId, hearingDayCourtSchedules));
        }
    }

    /** {@code listing.command.mark-hearing-as-duplicate} (HearingMarkedAsDuplicateCommandHandler). */
    public void markHearingAsDuplicate(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.mark-hearing-as-duplicate' received with payload {}", command.payload());
        }

        final uk.gov.justice.listing.courts.MarkHearingAsDuplicate markHearingAsDuplicate =
                jsonObjectConverter.convert(command.payload(), uk.gov.justice.listing.courts.MarkHearingAsDuplicate.class);
        final UUID hearingId = markHearingAsDuplicate.getHearingId();

        final List<UUID> caseIds = markHearingAsDuplicate.getProsecutionCaseIds();
        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.markHearingAsDuplicate(hearingId, caseIds));
    }

    /** {@code listing.command.mark-unallocated-hearing-as-duplicate} (HearingMarkedAsDuplicateCommandHandler). */
    public void markUnallocatedHearingAsDuplicate(final Envelope command) {
        final UUID hearingId = UUID.fromString(command.payload().getString(HEARING_ID));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.mark-unallocated-hearing-as-duplicate' for hearingId {}", hearingId);
        }

        updateHearingEventStream(command, hearingId, (HearingRules hearing) ->
                hearing.markUnallocatedHearingAsDuplicate(hearingId));
    }

    /** {@code listing.command.list-next-hearing} (ListNextHearingCommandHandler). */
    @SuppressWarnings("squid:S3655")
    public void listNextCourtHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-next-hearing' received with payload {}", command.payload());
        }

        final uk.gov.justice.listing.courts.ListNextHearing listNextHearing =
                jsonObjectConverter.convert(command.payload(), uk.gov.justice.listing.courts.ListNextHearing.class);
        final uk.gov.justice.listing.commands.HearingListingNeeds commandHearing = listNextHearing.getHearing();
        final Map<UUID, uk.gov.justice.listing.commands.CourtCentreDetails> courtCentres = listNextHearing.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(uk.gov.justice.listing.commands.CourtCentreDetails::getId, cc -> cc));

        final List<NonDefaultDay> nonDefaultDaysList = Collections.emptyList();

        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = commandToDomainConverter.convert(commandHearing, nonDefaultDaysList, listNextHearing.getShadowListedOffences());

        final uk.gov.justice.listing.commands.CourtCentreDetails courtCentre = courtCentres.get(domainHearing.getCourtCentreId());

        final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(courtCentre.getDefaultDuration())
                .withDefaultStartTime(courtCentre.getDefaultStartTime())
                .withCourtCentreId(courtCentre.getId())
                .build();
        final String adjournedFromDate = listNextHearing.getAdjournedFromDate();

        final UUID finalBookingReference = commandHearing.getBookingReference();
        final boolean isSlotsBooked = isNotEmpty(commandHearing.getBookedSlots());
        updateHearingEventStream(command, commandHearing.getId(), (HearingRules hearing) -> {
            final Stream<Object> listingEvents = hearing.list(domainHearing.getId(),
                    domainHearing.getType(),
                    domainHearing.getEstimatedMinutes(),
                    domainHearing.getEstimatedDuration(),
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
                    ofNullable(adjournedFromDate),
                    domainHearing.getWeekCommencingStartDate(),
                    domainHearing.getWeekCommencingEndDate(),
                    domainHearing.getWeekCommencingDurationInWeeks(),
                    domainHearing.getHearingDays(),
                    domainHearing.getNonDefaultDays(),
                    domainHearing.getNonSittingDays(),
                    isSlotsBooked,
                    listNextHearing.getHearing().getBookingType(),
                    listNextHearing.getHearing().getPriority(),
                    listNextHearing.getHearing().getSpecialRequirements(),
                    domainHearing.getIsPossibleDisqualification(),
                    domainHearing.getGroupProceedings(),
                    domainHearing.getNumberOfGroupCases(),
                    domainHearing.getPtphDetail()
            );

            final Stream<Object> allocationEvents = hearing.applyAllocationRules(ofNullable(finalBookingReference), false, false, Collections.emptyList(), empty(), commandHearing.getIsGroupProceedings());

            return Stream.of(listingEvents, allocationEvents).flatMap(i -> i);
        });
    }

    /** {@code listing.command.delete-seeded-hearing} (ListNextHearingCommandHandler). */
    public void deleteSeededHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.delete-seeded-hearing' received with payload {}", command.payload());
        }
        final uk.gov.justice.listing.courts.DeleteSeededHearing deleteSeededHearing =
                jsonObjectConverter.convert(command.payload(), uk.gov.justice.listing.courts.DeleteSeededHearing.class);
        final UUID hearingId = deleteSeededHearing.getHearingId();
        final UUID seedingHearingId = deleteSeededHearing.getSeedingHearingId();

        updateHearingEventStream(command, deleteSeededHearing.getHearingId(), (HearingRules hearing) -> hearing.deleteHearing(seedingHearingId, hearingId));
    }

    /** {@code listing.command.list-unscheduled-next-hearing} (UnscheduledListingCommandHandler). */
    public void handleListUnscheduledNextHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-unscheduled-next-hearing' received with payload {}", command.payload());
        }
        final Map<String, Integer> hearingTypesIdDurationMap = hearingTypeFactory.getHearingTypesIdDurationMap();
        final uk.gov.justice.listing.courts.ListUnscheduledNextHearing listUnscheduledNextHearing =
                jsonObjectConverter.convert(command.payload(), uk.gov.justice.listing.courts.ListUnscheduledNextHearing.class);
        final HearingUnscheduledListingNeeds commandHearing = listUnscheduledNextHearing.getHearing();
        final Map<UUID, CourtCentreDetails> courtCentres = listUnscheduledNextHearing.getCourtCentresDetails().stream()
                .collect(Collectors.toMap(CourtCentreDetails::getId, cc -> cc));

        listUnscheduledHearing(command, hearingTypesIdDurationMap, commandHearing, courtCentres,
                toDomain(listUnscheduledNextHearing.getPtphDetail()));
    }

    /** The command carries the detail as a sibling of the hearing (UnscheduledListingCommandHandler.toDomain). */
    private static PtphDetail toDomain(final uk.gov.justice.listing.commands.PtphDetail ptphDetail) {
        if (isNull(ptphDetail)) {
            return null;
        }
        return new PtphDetail(ptphDetail.getTier(), ptphDetail.getListType(), ptphDetail.getKeyReason());
    }

    /** {@code listing.command.update-defendants-for-hearing}. */
    public void updateDefendantsForHearing(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-defendants-for-hearing' received with payload {}", command.payload());
        }
        final UpdateDefendantsForHearing updateDefendantsForHearing = jsonObjectConverter.convert(payload, UpdateDefendantsForHearing.class);
        final UUID hearingId = updateDefendantsForHearing.getHearingId();
        final UUID caseId = updateDefendantsForHearing.getCaseId();
        final List<Defendant> defendants = updateDefendantsForHearing.getDefendants();
        final List<uk.gov.moj.cpp.listing.domain.Defendant> domainDefendants = commandDefendantToDomainConverter.convert(defendants);

        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.updateDefendants(caseId, domainDefendants));
    }

    /** {@code listing.command.update-offences-for-hearing}. */
    public void updateOffencesForHearing(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload {}", command.payload());
        }

        final UpdateOffencesForHearing updateOffencesForHearing = jsonObjectConverter.convert(payload, UpdateOffencesForHearing.class);
        final UUID hearingId = updateOffencesForHearing.getHearingId();
        final UUID caseId = updateOffencesForHearing.getCaseId();
        final UUID defendantId = updateOffencesForHearing.getDefendantId();
        final List<Offence> offences = updateOffencesForHearing.getOffences();
        final List<uk.gov.moj.cpp.listing.domain.Offence> domainOffences = commandOffenceToDomainOffence.convert(offences);

        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.updateOffences(caseId, defendantId, domainOffences));
    }

    /** {@code listing.command.delete-offences-for-hearing}. */
    public void deleteOffencesForHearing(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload %{}", command.payload());
        }
        final DeleteOffencesForHearing deleteOffencesForHearing = jsonObjectConverter.convert(payload, DeleteOffencesForHearing.class);
        final UUID hearingId = deleteOffencesForHearing.getHearingId();
        final UUID caseId = deleteOffencesForHearing.getCaseId();
        final UUID defendantId = deleteOffencesForHearing.getDefendantId();
        final List<SimpleOffence> offences = deleteOffencesForHearing.getOffences();
        final List<uk.gov.moj.cpp.listing.domain.SimpleOffence> domainOffences = commandSimpleOffenceToDomainOffence.convert(offences);

        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.deleteOffences(caseId, defendantId, domainOffences));
    }

    /** {@code listing.command.add-offences-for-hearing}. */
    public void addOffencesForHearing(final Envelope command) {
        final JsonObject payload = command.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload {}", command.payload());
        }
        final AddOffencesForHearing addOffencesForHearing = jsonObjectConverter.convert(payload, AddOffencesForHearing.class);
        final UUID hearingId = addOffencesForHearing.getHearingId();
        final UUID caseId = addOffencesForHearing.getCaseId();
        final UUID defendantId = addOffencesForHearing.getDefendantId();
        final List<Offence> offences = addOffencesForHearing.getOffences();
        final List<uk.gov.moj.cpp.listing.domain.Offence> domainOffences = commandOffenceToDomainOffence.convert(offences);

        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.addOffences(caseId, defendantId, domainOffences));
    }

    /** {@code listing.command.update-case-markers-to-hearing}. */
    public void updateCaseMarkersToHearing(final Envelope commandEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-case-markers-to-hearing' received with payload {}", commandEnvelope.payload());
        }

        final UpdateCaseMarkersToHearingCommand command =
                jsonObjectConverter.convert(commandEnvelope.payload(), UpdateCaseMarkersToHearingCommand.class);

        final UUID hearingId = command.getHearingId();
        final UUID caseId = command.getProsecutionCaseId();
        final List<CaseMarker> caseMarkers = caseMarkersToDomainConverter.convert(command.getCaseMarkers());

        updateHearingEventStream(commandEnvelope, hearingId, (HearingRules hearing) -> hearing.updateCaseMarkers(caseId, caseMarkers));
    }

    /** {@code listing.command.update-linked-case-in-hearing}. */
    public void updateLinkedCaseInHearing(final Envelope commandEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-linked-case-in-hearing' received with payload {}", commandEnvelope.payload());
        }
        final UpdateLinkedCaseInHearing command = jsonObjectConverter.convert(commandEnvelope.payload(), UpdateLinkedCaseInHearing.class);

        updateHearingEventStream(commandEnvelope, command.getHearingId(), (HearingRules hearing) ->
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

    /** {@code listing.command.add-defendants-to-court-proceedings-for-hearing}. */
    public void addDefendantsForCourtProceedingsForHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.add-defendants-to-court-proceedings-for-hearing' received with payload {}", command.payload());
        }

        final AddDefendantsToCourtProceedingsForHearing addDefendantsToCourtProceedingsForHearing = jsonObjectConverter.convert
                (command.payload(), AddDefendantsToCourtProceedingsForHearing.class);
        final UUID hearingId = addDefendantsToCourtProceedingsForHearing.getHearingId();
        final UUID caseId = addDefendantsToCourtProceedingsForHearing.getCaseId();
        final List<Defendant> defendants = addDefendantsToCourtProceedingsForHearing.getDefendants();
        final List<uk.gov.moj.cpp.listing.domain.Defendant> domainDefendants = commandDefendantToDomainConverter.convert(defendants);

        updateHearingEventStream(command, hearingId, (HearingRules hearing) -> hearing.addDefendantsForCourtProceedings(caseId, domainDefendants));
    }

    /** {@code listing.command.eject-case}. */
    public void ejectCase(final Envelope commandEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.eject-case' received with payload {}", commandEnvelope.payload());
        }
        final JsonObject commandPayload = commandEnvelope.payload();
        final UUID caseId = fromString(commandPayload.getString(PROSECUTION_CASE_ID));
        final UUID hearingId = fromString(commandPayload.getString(HEARING_ID));
        final String removalReason = commandPayload.getString(REMOVAL_REASON);
        updateHearingEventStream(commandEnvelope, hearingId, (HearingRules hearing) ->
                hearing.ejectCase(hearingId, caseId, removalReason));
    }

    /** {@code listing.command.eject-application}. */
    public void ejectApplication(final Envelope commandEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.eject-application' received with payload {}", commandEnvelope.payload());
        }
        final JsonObject commandPayload = commandEnvelope.payload();
        final UUID applicationId = fromString(commandPayload.getString(APPLICATION_ID));
        final UUID hearingId = fromString(commandPayload.getString(HEARING_ID));
        final String removalReason = commandPayload.getString(REMOVAL_REASON);
        updateHearingEventStream(commandEnvelope, hearingId, (HearingRules hearing) ->
                hearing.ejectApplication(hearingId, applicationId, removalReason));
    }

    /** {@code listing.command.update-defendant-legalaid-status-for-hearing}. */
    public void updateDefendantLegalAidStatusForHearing(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-defendant-legalaid-status-for-hearing event received {}", envelope.payload());
        }
        final JsonObject payload = envelope.payload();
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final String legalAidStatus = payload.getString(LEGAL_AID_STATUS);
        final UUID caseId = fromString(payload.getString(CASE_ID));
        final UUID hearingId = fromString(payload.getString(HEARING_ID));

        updateHearingEventStream(envelope, hearingId, (HearingRules hearing) ->
                hearing.updateDefendantLegalAidStatusForHearing(hearingId, caseId, defendantId, legalAidStatus));
    }

    /** {@code listing.command.update-defendant-court-proceedings} (both branches, incl. the HMI one). */
    public void updateDefendantCourtProceedings(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.command.update-defendant-court-proceedings event received {}", envelope.payload());
        }
        final UUID hearingId = fromString(envelope.payload().getString(HEARING_ID));
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(envelope.payload().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);

        if (prosecutionCase.getDefendants().stream()
                .map(uk.gov.justice.core.courts.Defendant::getDefendantCaseJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(judicialResult -> judicialResult.getJudicialResultTypeId().equals(UUID.fromString(SUMMONS_REJECTED_RESULT_TYPE_ID)))) {
            updateHearingEventStream(envelope, hearingId, (HearingRules hearing) ->
                    createHmiEvents(hearingId, prosecutionCase, hearing));
        } else {
            updateHearingEventStream(envelope, hearingId, (HearingRules hearing) -> hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase));
        }
    }

    /** The JEE {@code createHmiEvents}: both branches raise the same events (the summons flag decides nothing). */
    private Stream<Object> createHmiEvents(final UUID hearingId, final ProsecutionCase prosecutionCase, final HearingRules hearing) {
        if (hearing.getIsSummonsApprovedExists()) {
            return Stream.of(hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase), Stream.empty()).flatMap(i -> i);
        } else {
            return hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase);
        }
    }
}
