package uk.gov.moj.cpp.listing.command.hearing.api;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createArrayBuilder;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.listing.courts.ListCourtHearingEnriched.listCourtHearingEnriched;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelop;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.ListCourtHearing;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.ExtendHearingForHearing;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonValueConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.HearingCommands;
import uk.gov.moj.cpp.listing.command.hearing.handler.NextHearingCommands;
import uk.gov.moj.cpp.listing.command.hearing.api.service.PtphDetailEnrichmentService;
import uk.gov.moj.cpp.listing.command.hearing.api.service.SplitHearingPayloadConverter;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackSource;
import uk.gov.moj.cpp.listing.common.service.ProgressionSplitHearingService;
import uk.gov.moj.cpp.listing.common.splithearing.SplitHearingRejectedException;
import uk.gov.moj.cpp.listing.domain.PtphDetail;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.Courtrooms;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.ListNextHearingsV2;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearings;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.courts.UpdateExistingHearing;
import uk.gov.justice.listing.courts.UpdateRelatedHearing;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.hearing.api.service.HearingEnrichmentOrchestrator;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.listing.command.hearing.api.service.HearingLookupService;
import uk.gov.moj.cpp.listing.common.courtroomchange.ChangeCourtRoomForMultidayException;
import uk.gov.moj.cpp.listing.common.courtroomchange.ChangedDaySession;
import uk.gov.moj.cpp.listing.common.courtroomchange.RequestedChangeDay;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateResult;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.VacateTrialEnriched;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.json.JsonArray;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import uk.gov.moj.cpp.listing.common.client.HttpResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Hearing part of the JEE {@code ListingCommandApi} (MBD refactor P2.3.2, ADR 015): validation
 * and enrichment of a command (reference data, courtscheduler search/book, the viewstore), then the
 * enriched command, as the JEE API sent it to the command handler, now a direct call to
 * {@link HearingCommands} (one transaction). This class is not transactional: the enrichment makes
 * HTTP calls and courtscheduler bookings outside the command's database transaction, as before.
 * Ported verbatim apart from the framework types (JAX-RS {@code Response} → {@code HttpResult},
 * {@code JsonEnvelope} → {@link Envelope}, {@code BadRequestException} → {@link CommandBadRequestException}).
 * P2.3.3 adds the remaining actions: next hearings (list, list-unscheduled, update-related, delete,
 * delete-previous-and-create), split (forwarded to progression), duplicate, migrate, and the four
 * RAML-only pass-throughs. Notes and court lists are P2.3.1 ({@code ListingNoteCommands},
 * {@code CourtListCommands}).
 */
@Component
@SuppressWarnings({"squid:S2629", "java:S6204"})
public class HearingCommandApi {

    private static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";
    private static final String LISTING_COMMAND_UPDATE_HEARINGS_FOR_LISTING_ENRICHED = "listing.command.update-hearings-for-listing-enriched";
    private static final String LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED = "listing.command.list-court-hearing-enriched";
    private static final String LISTING_COMMAND_LIST_UNSCHEDULED_COURT_HEARING_ENRICHED = "listing.command.list-unscheduled-court-hearing-enriched";
    private static final String LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING_ENRICHED = "listing.command.extend-hearing-for-hearing-enriched";
    private static final String LISTING_COMMAND_VACATE_TRIAL = "listing.command.vacate-trial-enriched";
    private static final String LISTING_COMMAND_MOVE_HEARING_TO_PAST_DATE_ENRICHED = "listing.command.move-hearing-to-past-date-enriched";
    private static final String LISTING_COMMAND_CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING_ENRICHED = "listing.command.change-court-room-for-multiday-hearing-enriched";
    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";
    private static final String NO_HEARING_FOUND_FOR_HEARING_ID = "No hearing found for hearingId ";
    private static final String NON_DEFAULT_DAY_DURATION = "duration";
    private static final String SEND_NOTIFICATION_TO_PARTIES = "sendNotificationToParties";
    private static final String CHANGED_DAYS = "changedDays";
    private static final String HEARING_DATE = "hearingDate";
    public static final String UNSUPPORTED_JURISDICTION = "UNSUPPORTED_JURISDICTION";
    public static final String NOT_MULTIDAY_HEARING = "NOT_MULTIDAY_HEARING";
    public static final String DUPLICATE_DAY_DATES = "DUPLICATE_DAY_DATES";
    public static final String MISSING_COURT_SCHEDULE_ID = "MISSING_COURT_SCHEDULE_ID";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String START_DATE = "startDate";
    private static final String START_DATE_TIME = "startDateTime";
    private static final String END_DATE_TIME = "endDateTime";
    private static final String JURISDICTION = "jurisdiction";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String ROOM_ID = "roomId";
    private static final String VIRTUAL = "virtual";
    private static final String IS_DRAFT = "isDraft";
    private static final String SESSION_DATE = "sessionDate";
    private static final String MOVE_END_DATE = "endDate";
    private static final String MOVE_SESSIONS = "sessions";
    private static final String MOVE_IS_DRAFT = "isDraft";
    private static final String SESSION_START_TIME = "sessionStartTime";
    private static final String SESSION_END_TIME = "sessionEndTime";
    private static final String DURATION_IN_MINUTES = "durationInMinutes";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String DAY_START_TIME = "startTime";
    private static final String DAY_DURATION_MINUTES = "durationMinutes";
    private static final String ERROR_CODE = "errorCode";
    private static final String MESSAGE = "message";
    public static final String HEARING_ID_NOT_FOUND = "HEARING_ID_NOT_FOUND";
    public static final String FUTURE_DATE_NOT_ALLOWED = "FUTURE_DATE_NOT_ALLOWED";
    public static final String INVALID_DATE = "INVALID_DATE";
    public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String MULTI_DAY_NOT_ALLOWED = "MULTI_DAY_NOT_ALLOWED";
    public static final String START_DATE_TOO_OLD = "START_DATE_TOO_OLD";
    private static final int MAX_PAST_MONTHS = 6;
    private static final String CROWN_JURISDICTION = "CROWN";
    private static final String MAGISTRATES_JURISDICTION = "MAGISTRATES";
    private static final Set<String> CHANGE_COURT_ROOM_JURISDICTIONS = Set.of(CROWN_JURISDICTION, MAGISTRATES_JURISDICTION);
    private static final String LISTING_COMMAND_DELETE_HEARING = "listing.command.delete-hearing";
    private static final String LISTING_COMMAND_LIST_NEXT_HEARINGS_ENRICHED = "listing.command.list-next-hearings-enriched-v2";
    private static final String LISTING_COMMAND_LIST_UNSCHEDULED_NEXT_HEARINGS_ENRICHED = "listing.command.list-unscheduled-next-hearings-enriched";
    private static final String LISTING_COMMAND_DUPLICATE_UNALLOCATED_HEARING = "listing.command.mark-unallocated-hearing-as-duplicate";
    private static final String LISTING_COMMAND_UPDATE_EXISTING_HEARING = "listing.command.update-existing-hearing";
    private static final String LISTING_COMMAND_DELETE_NEXT_HEARINGS = "listing.command.delete-next-hearings";
    private static final String LISTING_COMMAND_DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING = "listing.command.delete-previous-hearings-and-create-next-hearing";
    private static final String LISTING_COMMAND_MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES = "listing.command.migrate-crown-hearings-to-courtschedules";
    private static final int SC_ACCEPTED = 202;
    private static final String LISTING_COMMAND_UPDATE_HEARING_DAY_COURT_SCHEDULE = "listing.command.update-hearing-day-court-schedule";
    public static final String LISTING_COMMAND_UPDATE_HEARING_ADD_CASE_BDF = "listing.command.update-hearing-add-case-bdf";
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingCommandApi.class);
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String HEARING_ID = "hearingId";
    public static final String START_DATE_MUST_BE_SMALLER_THAN_END_DATE = "startDate must be smaller than endDate";
    public static final String WEEK_COMMENCING_START_DATE_MUST_BE_SMALLER_THAN_WEEK_COMMENCING_END_DATE = "Week commencing start date must be smaller than week commencing end date";

    public static final String OUCODE = "oucode";

    @Autowired
    private HearingCommands hearingCommands;

    @Autowired
    private CourtCentreFactory courtCentreFactory;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter();
    @Autowired
    private HearingSlotsService hearingSlotsService;
    @Autowired
    private HearingEnrichmentOrchestrator hearingEnrichmentOrchestrator;
    @Autowired
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;
    @Autowired
    private HearingLookupService hearingLookupService;
    @Autowired
    private NextHearingCommands nextHearingCommands;
    @Autowired
    private PtphDetailEnrichmentService ptphDetailEnrichmentService;
    @Autowired
    private ProgressionSplitHearingService progressionSplitHearingService;

    /** {@code listing.list-next-hearings-v2}. */
    public void listNextHearings(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.list-next-hearings-v2' received with payload {}", envelope.payload());
        }

        final JsonObject payload = envelope.payload();
        final ListNextHearingsV2 listNextHearings = jsonObjectConverter.convert(payload, ListNextHearingsV2.class);
        final List<HearingListingNeeds> enrichedHearings = ptphDetailEnrichmentService.enrichWithPtphDetail(
                hearingEnrichmentOrchestrator.enrichListCourtHearing(
                        listNextHearings.getHearings(), envelope,
                        CrownFallbackSource.LIST_NEXT_HEARINGS_V2),
                listNextHearings.getSeedingHearing(),
                envelope);
        final Set<CourtCentreDetails> courtCentres = getCourtCentreDetails(envelope, enrichedHearings);

        final ListNextHearingsEnrichedV2 listNextHearingsEnriched = ListNextHearingsEnrichedV2.listNextHearingsEnrichedV2()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withListNextHearings(ListNextHearingsV2.listNextHearingsV2()
                        .withValuesFrom(listNextHearings)
                        .withHearings(enrichedHearings).build())
                .withAdjournedFromDate(listNextHearings.getAdjournedFromDate())
                .withSeedingHearing(listNextHearings.getSeedingHearing())
                .build();

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_LIST_NEXT_HEARINGS_ENRICHED),
                objectToJsonValueConverter.convert(listNextHearingsEnriched)));
    }

    /** {@code listing.update-related-hearing}. */
    public void updateRelatedHearing(final Envelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.update-related-hearing' received with payload {}", envelope.payload());
        }

        final JsonObject payload = envelope.payload();
        final UpdateRelatedHearing updateRelatedHearing = jsonObjectConverter.convert(payload, UpdateRelatedHearing.class);
        final UUID hearingId = fromString(payload.getString(HEARING_ID));

        // LPT-2405: the next hearing already exists, so nothing is created and this command
        // carries no hearing type. Enrich it from the stored hearing before applying the same
        // gates as the create flows — an absent hearing simply inherits nothing.
        final Optional<PtphDetail> ptphDetail = resolvePtphDetailForExistingHearing(
                hearingId, updateRelatedHearing.getSeedingHearing(), envelope);

        final UpdateExistingHearing updateExistingHearing = UpdateExistingHearing.updateExistingHearing()
                .withHearingId(hearingId)
                .withSeedingHearing(updateRelatedHearing.getSeedingHearing())
                .withProsecutionCases(updateRelatedHearing.getProsecutionCases())
                .withShadowListedOffences(updateRelatedHearing.getShadowListedOffences())
                .withTier(ptphDetail.map(PtphDetail::getTier).orElse(null))
                .withListType(ptphDetail.map(PtphDetail::getListType).orElse(null))
                .withKeyReason(ptphDetail.map(PtphDetail::getKeyReason).orElse(null))
                .build();

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_EXISTING_HEARING), objectToJsonValueConverter.convert(updateExistingHearing)));
    }

    /**
     * Reads the jurisdiction and hearing type of an already-existing hearing from the listing
     * view store, so the PTPH gates can be applied to a command that carries neither.
     * Returns empty when the hearing is not found — the id came from a result prompt, so it is
     * not guaranteed to exist here.
     */
    private Optional<PtphDetail> resolvePtphDetailForExistingHearing(final UUID hearingId,
                                                                     final SeedingHearing seedingHearing,
                                                                     final Envelope envelope) {
        final Optional<JsonObject> storedHearing = hearingLookupService.findHearing(hearingId, envelope);
        if (storedHearing.isEmpty()) {
            LOGGER.info("Existing hearing {} not found in listing; nothing inherited", hearingId);
            return Optional.empty();
        }

        final JsonObject hearing = storedHearing.get();
        final JsonObject storedType = hearing.getJsonObject("type");
        if (isNull(storedType) || !storedType.containsKey("id")) {
            LOGGER.info("Existing hearing {} has no hearing type stored; nothing inherited", hearingId);
            return Optional.empty();
        }

        final HearingType hearingType = HearingType.hearingType()
                .withId(fromString(storedType.getString("id")))
                .withDescription(storedType.getString("description", null))
                .build();
        final JurisdictionType jurisdictionType = hearing.containsKey(JURISDICTION_TYPE)
                ? JurisdictionType.valueOf(hearing.getString(JURISDICTION_TYPE))
                : null;

        return ptphDetailEnrichmentService.resolveForExistingHearing(jurisdictionType, hearingType, seedingHearing, envelope);
    }

    /** {@code listing.list-unscheduled-next-hearings}. */
    public void handleListUnscheduledNextCourtHearings(final Envelope envelope) {
        final JsonObject payload = envelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.list-unscheduled-next-hearings' received with payload {}", envelope.payload());
        }

        final ListUnscheduledNextHearings unscheduledNextHearings = jsonObjectConverter.convert(payload, ListUnscheduledNextHearings.class);

        final Set<CourtCentreDetails> courtCentres = new HashSet<>();

        for (final HearingUnscheduledListingNeeds commandHearing : unscheduledNextHearings.getHearings()) {
            courtCentres.add(courtCentreFactory.getCourtCentre(commandHearing.getCourtCentre().getId(), envelope));
        }
        final ListUnscheduledNextHearingsEnriched listCourtHearingEnriched = ListUnscheduledNextHearingsEnriched.listUnscheduledNextHearingsEnriched()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withHearings(unscheduledNextHearings.getHearings())
                .withPtphDetails(ptphDetailEnrichmentService.resolvePtphDetails(
                        unscheduledNextHearings.getHearings(),
                        unscheduledNextHearings.getSeedingHearing(),
                        envelope))
                .withSeedingHearing(unscheduledNextHearings.getSeedingHearing())
                .build();

        send(envelop(objectToJsonValueConverter.convert(listCourtHearingEnriched)).withName(LISTING_COMMAND_LIST_UNSCHEDULED_NEXT_HEARINGS_ENRICHED)
                .withMetadataFrom(envelope));
    }

    /** {@code listing.delete-next-hearings}: the payload is reduced to {@code {seedingHearing}}. */
    public void handleDeleteNextHearings(final Envelope envelope) {
        final JsonObject payload = envelope.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.delete-next-hearings' received with payload {}", envelope.payload());
        }

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DELETE_NEXT_HEARINGS),
                createObjectBuilder()
                        .add("seedingHearing", payload.getJsonObject("seedingHearing"))
                        .build()
        ));
    }

    /** {@code listing.delete-previous-hearings-and-create-next-hearing}: renamed, payload unchanged. */
    public void handleDeletePreviousHearingsAndCreateNextHearing(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.delete-previous-hearings-and-create-next-hearing' received with payload {}", envelope.payload());
        }
        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING),
                envelope.payload()));
    }

    /**
     * {@code listing.command.split-hearing}: validates the front-end's split payload, converts it to
     * progression's list-new-hearing shape and forwards it to progression, which performs the split.
     * No listing command is sent; a non-202 answer becomes {@link SplitHearingRejectedException}
     * (mapped to the HTTP answer in listing-api, as the JEE {@code SplitHearingRejectedExceptionMapper}).
     */
    public void handleSplitHearing(final Envelope envelope) {
        final JsonObject payload = envelope.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.split-hearing' received with payload {}", envelope.payload());
        }

        final String hearingId = payload.getString(HEARING_ID);
        final UUID courtCentreId = payload.containsKey(COURT_CENTRE_ID) && !payload.isNull(COURT_CENTRE_ID)
                ? fromString(payload.getString(COURT_CENTRE_ID))
                : null;
        final CourtCentreDetails courtCentre = isNull(courtCentreId)
                ? null
                : courtCentreFactory.getCourtCentre(courtCentreId, envelope);

        final JsonObject progressionRequest = SplitHearingPayloadConverter.toProgressionSplitRequest(
                payload,
                isNull(courtCentre) ? null : courtCentre.getName(),
                courtRoomName(courtCentre, payload),
                null);

        LOGGER.info("split-hearing accepted for hearing {}; forwarding {} defendant request(s) to progression",
                hearingId,
                progressionRequest.getJsonObject("listNewHearing").getJsonArray("listDefendantRequests").size());

        final HttpResult response = progressionSplitHearingService.splitHearing(hearingId, progressionRequest,
                envelope.metadata().userId(), envelope.metadata().sessionId(), envelope.metadata().clientCorrelationId(),
                envelope.metadata().causation());
        final int status = response.getStatus();
        if (SC_ACCEPTED == status) {
            return;
        }

        final JsonObject errorBody = (response.hasEntity() && response.getEntity() instanceof JsonObject body)
                ? body
                : Json.createObjectBuilder().build();

        LOGGER.error("split-hearing for hearing {} rejected by progression with status {}: {}", hearingId, status, errorBody);

        throw new SplitHearingRejectedException(status, errorBody,
                "progression returned " + status + " for the split of hearing " + hearingId);
    }

    private static String courtRoomName(final CourtCentreDetails courtCentre, final JsonObject payload) {
        if (isNull(courtCentre) || isNull(courtCentre.getCourtrooms())
                || !payload.containsKey(COURT_ROOM_ID) || payload.isNull(COURT_ROOM_ID)) {
            return null;
        }
        final UUID courtRoomId = fromString(payload.getString(COURT_ROOM_ID));
        return courtCentre.getCourtrooms().stream()
                .filter(room -> courtRoomId.equals(room.getId()))
                .map(Courtrooms::getCourtroomName)
                .findFirst()
                .orElse(null);
    }

    /** {@code listing.migrate-crown-hearings-to-courtschedules}: renamed, payload unchanged. */
    public void handleMigrateCrownHearingsToCourtSchedules(final Envelope envelope) {
        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES),
                envelope.payload()));
    }

    /** {@code listing.mark-unallocated-hearing-as-duplicate}: renamed, payload unchanged. */
    public void handleMarkUnallocatedHearingAsDuplicate(final Envelope envelope) {
        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DUPLICATE_UNALLOCATED_HEARING),
                envelope.payload()));
    }

    /**
     * The four commands the RAML declares on {@code POST /hearings/{hearingId}} without a JEE
     * command-API {@code @Handles} ({@code listing.command.update-defendants-for-hearing},
     * {@code -update-offences-for-hearing}, {@code -delete-offences-for-hearing},
     * {@code -add-offences-for-hearing}): passed through unchanged to the command service of the same
     * name (MBD refactor P2.3.3 decision; see the controller).
     */
    public void handlePassThroughHearingCommand(final Envelope envelope) {
        send(envelope);
    }
    public void handleListCourtHearing(final Envelope envelope) {
        final JsonObject payload = envelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-court-hearing' received with payload {}", envelope.payload());
        }
        //your hearingdays in listcourthearing should match nondefault days.
        final ListCourtHearing listCourtHearing = jsonObjectConverter.convert(payload, ListCourtHearing.class);
        final List<HearingListingNeeds> hearingListingNeeds = listCourtHearing.getHearings();
        final List<HearingListingNeeds> enrichedHearings = hearingEnrichmentOrchestrator.enrichListCourtHearing(hearingListingNeeds, envelope);
        final Set<CourtCentreDetails> courtCentres = getCourtCentreDetails(envelope, enrichedHearings);

        final ListCourtHearingEnriched listCourtHearingEnriched = listCourtHearingEnriched()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withListCourtHearing(
                        ListCourtHearing.listCourtHearing()
                                .withValuesFrom(listCourtHearing)
                                .withHearings(enrichedHearings)
                                .build()
                )
                .withAdjournedFromDate(listCourtHearing.getAdjournedFromDate())
                .build();

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED),
                objectToJsonValueConverter.convert(listCourtHearingEnriched)));
    }
    public void handleListUnscheduledCourtHearing(final Envelope envelope) {
        final JsonObject payload = envelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-unscheduled-court-hearing' received with payload {}", envelope.payload());
        }

        final ListUnscheduledCourtHearing listCourtHearing = jsonObjectConverter.convert(payload, ListUnscheduledCourtHearing.class);

        final Set<CourtCentreDetails> courtCentres = new HashSet<>();

        for (final HearingUnscheduledListingNeeds commandHearing : listCourtHearing.getHearings()) {
            courtCentres.add(courtCentreFactory.getCourtCentre(commandHearing.getCourtCentre().getId(), envelope));
        }
        final ListUnscheduledCourtHearingEnriched listCourtHearingEnriched = ListUnscheduledCourtHearingEnriched.listUnscheduledCourtHearingEnriched()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withHearings(listCourtHearing.getHearings())
                .build();

        send(envelop(objectToJsonValueConverter.convert(listCourtHearingEnriched)).withName(LISTING_COMMAND_LIST_UNSCHEDULED_COURT_HEARING_ENRICHED)
                .withMetadataFrom(envelope));
    }
    public void handleUpdateHearingForListing(final Envelope envelope) {
        final JsonObject payload = envelope.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-hearing-for-listing' received with payload {}", envelope.payload());
        }

        UpdateHearingForListing updateHearingForListing = jsonObjectConverter.convert(payload, UpdateHearingForListing.class);

        if (updateHearingForListing.getStartDate() != null &&
                updateHearingForListing.getEndDate() != null &&
                updateHearingForListing.getStartDate().isAfter(updateHearingForListing.getEndDate())) {
            throw new CommandBadRequestException(START_DATE_MUST_BE_SMALLER_THAN_END_DATE);
        }

        if (updateHearingForListing.getWeekCommencingStartDate() != null &&
                updateHearingForListing.getWeekCommencingEndDate() != null &&
                updateHearingForListing.getWeekCommencingStartDate().isAfter(updateHearingForListing.getWeekCommencingEndDate())) {
            throw new CommandBadRequestException(WEEK_COMMENCING_START_DATE_MUST_BE_SMALLER_THAN_WEEK_COMMENCING_END_DATE);
        }

        LOGGER.info("HandleUpdateHearingForListing for the hearing: {} ", updateHearingForListing.getHearingId());
        final CourtCentreDetails courtCentre =
                courtCentreFactory.getCourtCentre(getCourtCentreId(updateHearingForListing), envelope);
        updateHearingForListing = hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(updateHearingForListing, envelope, courtCentre);

        final UpdateHearingForListingEnriched updateHearingForListingEnriched =
                updateHearingForListingEnriched(updateHearingForListing, courtCentre, payload);

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED), objectToJsonValueConverter.convert(updateHearingForListingEnriched)));
    }


    public void handleUpdateHearingsForListing(final Envelope envelope) {
        final JsonObject hearingsPayload = envelope.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-hearings-for-listing' received with payload {}", envelope.payload());
        }

        final List<UpdateHearingForListingPayloadWrapper> updateHearingsForListing = new ArrayList<>();
        final Set<UUID> courtCenterIds = new HashSet<>();
        hearingsPayload.getJsonArray("hearings").forEach(element -> {
            final JsonObject hearingJsonObj = (JsonObject) element;
            final UpdateHearingForListing updateHearing = jsonObjectConverter.convert(hearingJsonObj, UpdateHearingForListing.class);
            updateHearingsForListing.add(new UpdateHearingForListingPayloadWrapper(updateHearing, hearingJsonObj));
            courtCenterIds.add(getCourtCentreId(updateHearing));
        });
        final Map<UUID, CourtCentreDetails> courtCentreDetailsById = courtCentreFactory.getCourtCentreDetailsById(courtCenterIds, envelope);

        final JsonArrayBuilder hearingsEnrichedArrayBuilder = createArrayBuilder();
        updateHearingsForListing.forEach(wrapper -> {
            final UUID courtCentreId = getCourtCentreId(wrapper.updateHearingForListing());

            final CourtCentreDetails courtCentreDetails = courtCentreDetailsById.get(courtCentreId);
            final UpdateHearingForListing enrichedHearing = hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(wrapper.updateHearingForListing(), envelope, courtCentreDetails) ;
            final UpdateHearingForListingEnriched updateHearingEnriched =
                    updateHearingForListingEnriched(enrichedHearing, courtCentreDetails, wrapper.fullPayload());
            hearingsEnrichedArrayBuilder.add(objectToJsonValueConverter.convert(updateHearingEnriched));
        });

        final JsonObjectBuilder hearingsJsonObjBuilder = createObjectBuilder();
        hearingsJsonObjBuilder.add("updateHearingsForListing", hearingsEnrichedArrayBuilder.build());

        send(envelopeFrom(metadataFrom(envelope.metadata()).
                withName(LISTING_COMMAND_UPDATE_HEARINGS_FOR_LISTING_ENRICHED), hearingsJsonObjBuilder.build()));
    }
    public void handleVacateTrial(final Envelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.vacate-trial' received with payload {}", envelope.payload());
        }
        final VacateTrialEnriched vacateTrialEnriched = jsonObjectConverter.convert(envelope.payload(), VacateTrialEnriched.class);

        LOGGER.info("HandleVacateTrial for the hearing: {} ", vacateTrialEnriched.getHearingId());
        hearingSlotsService.delete(vacateTrialEnriched.getHearingId());

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_VACATE_TRIAL),
                envelope.payload()));
    }

    /**
     * courtRoomId/startDateTime/endDateTime mirror main's contract (the review artifact this
     * reconciles); jurisdiction is still resolved from the hearing's own {@code jurisdictionType} in
     * the listing viewstore (NOT main's court-centre reference-data lookup — that lookup is
     * deliberately not ported, see the plan's Part B.2 note 4 / decisions). Date validation
     * ({@link #validateMoveDates}) mirrors main's rules for BOTH jurisdictions, including the
     * single-day request window (MULTI_DAY_NOT_ALLOWED, D-C/Q5). The moved day's duration is the
     * submitted window (endDateTime - startDateTime), not the hearing's previous estimate, so its
     * end time is the submitted endDateTime (SPRDT-1447).
     */
    public void handleMoveHearingToPastDate(final Envelope envelope) {
        final JsonObject payload = envelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.move-hearing-to-past-date' received with payload {}", envelope.payload());
        }

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final UUID courtCentreId = fromString(payload.getString(COURT_CENTRE_ID));
        final UUID courtRoomId = fromString(payload.getString(COURT_ROOM_ID));
        final ZonedDateTime startInstant = parseInstant(payload.getString(START_DATE_TIME), START_DATE_TIME);
        final ZonedDateTime endInstant = parseInstant(payload.getString(END_DATE_TIME), END_DATE_TIME);
        final LocalDate startDate = startInstant.toLocalDate();

        final JsonObject hearing = hearingLookupService.findHearing(hearingId, envelope)
                .orElseThrow(() -> new MoveHearingToPastDateException(422,
                        buildMoveHearingToPastDateErrorBody(HEARING_ID_NOT_FOUND, NO_HEARING_FOUND_FOR_HEARING_ID + hearingId),
                        NO_HEARING_FOUND_FOR_HEARING_ID + hearingId));

        final String jurisdictionType = hearing.getString(JURISDICTION_TYPE, null);

        final JsonObjectBuilder enrichedBuilder = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(JURISDICTION, jurisdictionType == null ? "" : jurisdictionType)
                .add(START_DATE, startDate.toString())
                .add(COURT_CENTRE_ID, courtCentreId.toString());

        // Ported from main, applied uniformly to BOTH jurisdictions (main runs this before any
        // CROWN/MAGISTRATES branching too) — closes the gap where MAGS previously had no listing-side
        // date validation at all, and CROWN only had a future-date check that still allowed today.
        validateMoveDates(startInstant, endInstant);

        enrichWithBookedPastDateSlot(enrichedBuilder, hearingId, courtCentreId, courtRoomId,
                startInstant, endInstant, jurisdictionType);

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_MOVE_HEARING_TO_PAST_DATE_ENRICHED),
                enrichedBuilder.build()));
    }

    /** Mirrors main's {@code parseInstant}: malformed startDateTime/endDateTime => 422 INVALID_DATE. */
    private static ZonedDateTime parseInstant(final String value, final String field) {
        try {
            return ZonedDateTime.parse(value);
        } catch (final DateTimeParseException e) {
            final String message = field + " is not a valid date";
            throw new MoveHearingToPastDateException(422, buildMoveHearingToPastDateErrorBody(INVALID_DATE, message), message);
        }
    }

    /**
     * Ported from main's {@code validateMoveDates}, applied to both jurisdictions: target strictly
     * before today, endDateTime not before startDateTime, a single-day request window (endDate must
     * equal startDate - D-C/Q5), and startDate no older than {@value #MAX_PAST_MONTHS} months.
     */
    private static void validateMoveDates(final ZonedDateTime startInstant, final ZonedDateTime endInstant) {
        final LocalDate startDate = startInstant.toLocalDate();
        final LocalDate endDate = endInstant.toLocalDate();
        final LocalDate today = LocalDate.now();
        // today itself and any future date are rejected (isBefore is exclusive of today) - SPRDT-1185.
        if (!startDate.isBefore(today) || !endDate.isBefore(today)) {
            final String message = "Hearings can only be moved to an earlier date";
            throw new MoveHearingToPastDateException(422, buildMoveHearingToPastDateErrorBody(FUTURE_DATE_NOT_ALLOWED, message), message);
        }
        if (endInstant.isBefore(startInstant)) {
            final String message = "endDateTime must not be earlier than startDateTime";
            throw new MoveHearingToPastDateException(422, buildMoveHearingToPastDateErrorBody(INVALID_DATE_RANGE, message), message);
        }
        if (endDate.isAfter(startDate)) {
            final String message = "Only Single day hearing can be moved to past date";
            throw new MoveHearingToPastDateException(422, buildMoveHearingToPastDateErrorBody(MULTI_DAY_NOT_ALLOWED, message), message);
        }
        if (startDate.isBefore(today.minusMonths(MAX_PAST_MONTHS))) {
            final String message = "startDate cannot be earlier than " + MAX_PAST_MONTHS + " months before today";
            throw new MoveHearingToPastDateException(422, buildMoveHearingToPastDateErrorBody(START_DATE_TOO_OLD, message), message);
        }
    }

    private void enrichWithBookedPastDateSlot(final JsonObjectBuilder enrichedBuilder, final UUID hearingId,
                                              final UUID courtCentreId, final UUID courtRoomId,
                                              final ZonedDateTime startInstant, final ZonedDateTime endInstant,
                                              final String jurisdictionType) {
        // The moved day lasts exactly the submitted window, not the hearing's previous estimate:
        // the handler derives the day's endTime as startTime + this duration, so it lands on the
        // submitted endDateTime. validateMoveDates has already restricted the move to a single day.
        final int durationInMinutes = (int) ChronoUnit.MINUTES.between(startInstant, endInstant);

        final MoveHearingToPastDateResult slot = courtSchedulerServiceAdapter.moveHearingToPastDate(
                hearingId, courtCentreId, courtRoomId, startInstant, endInstant, durationInMinutes, jurisdictionType);

        if (slot.sessions().isEmpty()) {
            // courtscheduler can return 200 with an empty sessions[] for a genuine multi-day
            // (duration-sized) search that matched nothing (exploratory - it deliberately leaves the
            // prior allocation intact rather than erroring). Nothing to enrich/move in that case -
            // surface it as the same NO_SESSION_FOUND failure a single-date miss gets, rather than
            // sending a broken enriched command downstream.
            throw new MoveHearingToPastDateException(422,
                    buildMoveHearingToPastDateErrorBody(CourtSchedulerServiceAdapter.NO_SESSION_FOUND,
                            CourtSchedulerServiceAdapter.NO_SESSION_FOUND_MESSAGE),
                    CourtSchedulerServiceAdapter.NO_SESSION_FOUND_MESSAGE);
        }

        // Flat single-slot fields mirror the FIRST booked session (the day the hearing now starts on).
        if (!slot.sessions().isEmpty()) {
            addBookedSessionFields(enrichedBuilder, slot.sessions().get(0), durationInMinutes);
        }
        // The hearing's new end date is the last day courtscheduler booked (== sessionDate for single-day).
        addIfPresent(enrichedBuilder, MOVE_END_DATE, slot.lastSessionDate());

        // Every booked session, in date order - the handler re-issues one hearing day per entry so a
        // multi-day hearing keeps N days matching courtscheduler's N allocations.
        final JsonArrayBuilder sessions = createArrayBuilder();
        for (final MoveHearingToPastDateResult.BookedSession session : slot.sessions()) {
            final JsonObjectBuilder day = createObjectBuilder();
            addBookedSessionFields(day, session, durationInMinutes);
            addIfPresent(day, COURT_CENTRE_ID, session.courtCentreId());
            addIfPresent(day, MOVE_IS_DRAFT, session.isDraft());
            sessions.add(day);
        }
        enrichedBuilder.add(MOVE_SESSIONS, sessions);
    }

    /** courtScheduleId / courtRoomId / sessionDate / session times / duration of one booked session. */
    private static void addBookedSessionFields(final JsonObjectBuilder target,
                                               final MoveHearingToPastDateResult.BookedSession session,
                                               final int durationInMinutes) {
        addIfPresent(target, COURT_SCHEDULE_ID, session.courtScheduleId());
        addIfPresent(target, COURT_ROOM_ID, session.courtRoomId());
        addIfPresent(target, SESSION_DATE, session.sessionDate());
        addIfPresent(target, SESSION_START_TIME, session.sessionStartTime());
        addIfPresent(target, SESSION_END_TIME, session.sessionEndTime());
        addIfPresent(target, DURATION_IN_MINUTES, durationInMinutes);
    }

    /** Adds {@code value} under {@code key} unless null: Integers and Booleans natively, anything else via toString(). */
    private static void addIfPresent(final JsonObjectBuilder target, final String key, final Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Integer integer) {
            target.add(key, integer);
        } else if (value instanceof Boolean bool) {
            target.add(key, bool);
        } else {
            target.add(key, value.toString());
        }
    }

    private static JsonObject buildMoveHearingToPastDateErrorBody(final String errorCode, final String message) {
        return createObjectBuilder()
                .add(ERROR_CODE, errorCode)
                .add(MESSAGE, message)
                .build();
    }

    /**
     * Changes the courtroom of one or more SELECTED days of a multi-day CROWN or MAGISTRATES
     * hearing. Days not present in {@code nonDefaultDays} are never touched. Virtual days are always
     * (re)booked in courtscheduler; a REAL day is (re)booked too when its {@code courtScheduleId}
     * differs from the hearing day's current schedule on that date, so the old session's duration is
     * paid back and the new session's deducted (SPRDT-1225). A real day WITHOUT a courtScheduleId is
     * persisted and never booked - the UI omits the id when no bookable slot exists for the room/date
     * (e.g. the day's own session is fully consumed), which must not be a contract violation. Schema
     * violations (missing/malformed fields) are rejected as 400 by the framework via the request
     * schema; business failures (unknown hearing, unsupported jurisdiction, non-multiday, duplicate day dates, a
     * virtual day without a courtScheduleId, or a courtscheduler rejection) are all surfaced as 422
     * via {@link ChangeCourtRoomForMultidayException} so no command is ever sent.
     */
    public void handleChangeCourtRoomForMultidayHearing(final Envelope envelope) {
        final JsonObject payload = envelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.change-court-room-for-multiday-hearing' received with payload {}", envelope.payload());
        }

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final JsonArray nonDefaultDays = payload.getJsonArray(NON_DEFAULT_DAYS);

        final JsonObject hearing = validateMultidayHearingOrThrow(hearingId, envelope);
        final Map<LocalDate, String> currentScheduleByDate = currentCourtScheduleIdsByDate(hearing);

        // Each requested day is either VIRTUAL (virtual=true -> (re)booked in courtscheduler and
        // converted into a hearing day) or REAL (virtual false/absent -> persisted as a nonDefaultDay).
        // A REAL day whose courtScheduleId differs from the hearing day's CURRENT schedule on that
        // date is a room change onto another session, so it is (re)booked in courtscheduler exactly
        // like a virtual day - the old session must pay its duration back and the new session must
        // be deducted (SPRDT-1225). A real day echoing its current schedule, carrying no
        // courtScheduleId at all, or on a date without a scheduled hearing day is never booked.
        // Dates must be unique across the WHOLE request.
        //
        // Requested days are keyed by date, so the adapter response (booked sessions) can be joined back
        // to the originating request BY DATE - the adapter is free to return its sessions in any order,
        // so a positional join would silently mismatch rooms/times across days.
        final Set<LocalDate> seenDates = new HashSet<>();
        final Map<LocalDate, JsonObject> virtualRequestedByDate = new LinkedHashMap<>();
        final Map<LocalDate, JsonObject> rebookedRealRequestedByDate = new LinkedHashMap<>();
        final List<RequestedChangeDay> daysToBook = new ArrayList<>();
        final JsonArrayBuilder realNonDefaultDays = createArrayBuilder();
        for (final JsonValue value : nonDefaultDays) {
            final JsonObject nonDefaultDay = (JsonObject) value;
            final LocalDate date = ZonedDateTime.parse(nonDefaultDay.getString(DAY_START_TIME)).toLocalDate();
            if (!seenDates.add(date)) {
                throw new ChangeCourtRoomForMultidayException(422,
                        buildChangeCourtRoomForMultidayErrorBody(DUPLICATE_DAY_DATES, "Duplicate day " + date + " in nonDefaultDays"),
                        "Duplicate day " + date + " in nonDefaultDays");
            }
            if (nonDefaultDay.getBoolean(VIRTUAL, false)) {
                if (nonDefaultDay.getString(COURT_SCHEDULE_ID, null) == null) {
                    throw new ChangeCourtRoomForMultidayException(422,
                            buildChangeCourtRoomForMultidayErrorBody(MISSING_COURT_SCHEDULE_ID, "Virtual day " + date + " requires a courtScheduleId"),
                            "Virtual day " + date + " requires a courtScheduleId");
                }
                virtualRequestedByDate.put(date, nonDefaultDay);
                daysToBook.add(new RequestedChangeDay(date, fromString(nonDefaultDay.getString(COURT_SCHEDULE_ID)),
                        nonDefaultDay.getInt(NON_DEFAULT_DAY_DURATION)));
            } else {
                realNonDefaultDays.add(buildEnrichedRealNonDefaultDay(nonDefaultDay));
                if (isRealDayScheduleChange(nonDefaultDay, date, currentScheduleByDate)) {
                    rebookedRealRequestedByDate.put(date, nonDefaultDay);
                    daysToBook.add(new RequestedChangeDay(date, fromString(nonDefaultDay.getString(COURT_SCHEDULE_ID)),
                            nonDefaultDay.getInt(NON_DEFAULT_DAY_DURATION)));
                }
            }
        }

        // Virtual days and schedule-changing real days are sent to courtscheduler in ONE call.
        final JsonArrayBuilder changedDays = bookRequestedDaysIntoChangedDays(hearingId, daysToBook,
                virtualRequestedByDate, rebookedRealRequestedByDate);

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING_ENRICHED),
                createObjectBuilder()
                        .add(HEARING_ID, hearingId.toString())
                        .add(SEND_NOTIFICATION_TO_PARTIES, payload.getBoolean(SEND_NOTIFICATION_TO_PARTIES, true))
                        .add(CHANGED_DAYS, changedDays.build())
                        .add(NON_DEFAULT_DAYS, realNonDefaultDays.build())
                        .build()));
    }

    /**
     * Business validations for change-court-room-for-multiday: hearing must exist, be CROWN or
     * MAGISTRATES, and be multiday. Returns the looked-up hearing so callers can compare requested
     * days against its current hearingDays without a second query.
     */
    private JsonObject validateMultidayHearingOrThrow(final UUID hearingId, final Envelope envelope) {
        final JsonObject hearing = hearingLookupService.findHearing(hearingId, envelope)
                .orElseThrow(() -> new ChangeCourtRoomForMultidayException(422,
                        buildChangeCourtRoomForMultidayErrorBody(HEARING_ID_NOT_FOUND, NO_HEARING_FOUND_FOR_HEARING_ID + hearingId),
                        NO_HEARING_FOUND_FOR_HEARING_ID + hearingId));

        final String jurisdictionType = hearing.getString(JURISDICTION_TYPE, null);
        if (jurisdictionType == null || !CHANGE_COURT_ROOM_JURISDICTIONS.contains(jurisdictionType)) {
            final String message = "change-court-room-for-multiday-hearing supports CROWN and MAGISTRATES hearings, not " + jurisdictionType;
            throw new ChangeCourtRoomForMultidayException(422,
                    buildChangeCourtRoomForMultidayErrorBody(UNSUPPORTED_JURISDICTION, message), message);
        }

        final JsonArray hearingDays = hearing.containsKey(HEARING_DAYS) ? hearing.getJsonArray(HEARING_DAYS) : null;
        if (hearingDays == null || hearingDays.size() < 2) {
            throw new ChangeCourtRoomForMultidayException(422,
                    buildChangeCourtRoomForMultidayErrorBody(NOT_MULTIDAY_HEARING, "Hearing " + hearingId + " is not a multiday hearing"),
                    "Hearing " + hearingId + " is not a multiday hearing");
        }
        return hearing;
    }

    /**
     * The hearing's current courtScheduleId per calendar date, from its hearingDays. When two days share
     * a date (a cancelled day plus its re-listed replacement), the non-cancelled day wins; days without
     * a schedule contribute nothing, so a real day targeting them is never treated as a schedule change.
     */
    private static Map<LocalDate, String> currentCourtScheduleIdsByDate(final JsonObject hearing) {
        final Map<LocalDate, String> scheduleByDate = new LinkedHashMap<>();
        for (final JsonValue value : hearing.getJsonArray(HEARING_DAYS)) {
            final JsonObject day = (JsonObject) value;
            final String rawDate = day.getString(HEARING_DATE, null);
            final String scheduleId = day.getString(COURT_SCHEDULE_ID, null);
            if (rawDate == null || scheduleId == null) {
                continue;
            }
            final LocalDate date = LocalDate.parse(rawDate.length() > 10 ? rawDate.substring(0, 10) : rawDate);
            final boolean cancelled = day.getBoolean("isCancelled", false);
            if (cancelled) {
                scheduleByDate.putIfAbsent(date, scheduleId);
            } else {
                scheduleByDate.put(date, scheduleId);
            }
        }
        return scheduleByDate;
    }

    /**
     * A real day changes schedule when it carries a courtScheduleId and its date has a scheduled
     * hearing day whose courtScheduleId differs. A day without a courtScheduleId never books - the
     * UI omits the id when it has no bookable slot for the room/date.
     */
    private static boolean isRealDayScheduleChange(final JsonObject nonDefaultDay, final LocalDate date,
            final Map<LocalDate, String> currentScheduleByDate) {
        final String requestedScheduleId = nonDefaultDay.getString(COURT_SCHEDULE_ID, null);
        final String currentScheduleId = currentScheduleByDate.get(date);
        return requestedScheduleId != null && currentScheduleId != null && !currentScheduleId.equals(requestedScheduleId);
    }

    /**
     * Enriched nonDefaultDay for a real day. roomId is the uuid room identity; the FE's legacy integer
     * courtRoomId (if sent) is ignored. courtScheduleId is carried only when supplied - the aggregate's
     * merge keeps the stored day's existing schedule when it is absent.
     */
    private JsonObjectBuilder buildEnrichedRealNonDefaultDay(final JsonObject nonDefaultDay) {
        final JsonObjectBuilder enriched = createObjectBuilder()
                .add(DAY_START_TIME, nonDefaultDay.getString(DAY_START_TIME))
                .add(DAY_DURATION_MINUTES, nonDefaultDay.getInt(NON_DEFAULT_DAY_DURATION))
                .add(COURT_CENTRE_ID, nonDefaultDay.getString(COURT_CENTRE_ID))
                .add(ROOM_ID, nonDefaultDay.getString(ROOM_ID));
        final String courtScheduleId = nonDefaultDay.getString(COURT_SCHEDULE_ID, null);
        if (courtScheduleId != null) {
            enriched.add(COURT_SCHEDULE_ID, courtScheduleId);
        }
        return enriched;
    }

    /**
     * Sends the days requiring a booking (virtual days plus schedule-changing real days) to
     * courtscheduler in ONE call and enriches the booked sessions into changedDays, joined by date.
     * Virtual days take the booked session's startTime (the session defines the day); a rebooked
     * real day keeps its REQUESTED startTime - a custom start time is the whole point of a
     * non-default day, only its session (room/schedule/draft state) moves.
     */
    private JsonArrayBuilder bookRequestedDaysIntoChangedDays(final UUID hearingId, final List<RequestedChangeDay> daysToBook,
            final Map<LocalDate, JsonObject> virtualRequestedByDate,
            final Map<LocalDate, JsonObject> rebookedRealRequestedByDate) {
        final JsonArrayBuilder changedDays = createArrayBuilder();
        if (daysToBook.isEmpty()) {
            return changedDays;
        }
        final List<ChangedDaySession> booked = courtSchedulerServiceAdapter.changeCourtRoomForMultidayHearing(hearingId, daysToBook);
        for (final ChangedDaySession session : booked) {
            final JsonObject virtualRequested = virtualRequestedByDate.get(session.sessionDate());
            final JsonObject requested = virtualRequested != null
                    ? virtualRequested : rebookedRealRequestedByDate.get(session.sessionDate());
            final String startTime = virtualRequested != null && session.sessionStartTime() != null
                    ? session.sessionStartTime() : requested.getString(DAY_START_TIME);
            final JsonObjectBuilder dayBuilder = createObjectBuilder()
                    .add(HEARING_DATE, session.sessionDate().toString())
                    .add(DAY_START_TIME, startTime)
                    .add(DAY_DURATION_MINUTES, requested.getInt(NON_DEFAULT_DAY_DURATION))
                    .add(COURT_CENTRE_ID, requested.getString(COURT_CENTRE_ID))
                    .add(COURT_ROOM_ID, session.courtRoomId() != null ? session.courtRoomId() : requested.getString(ROOM_ID))
                    .add(COURT_SCHEDULE_ID, session.courtScheduleId().toString());
            // The booked session's draft state travels with the day so the aggregate can stamp it
            // onto the hearing day (isDraft would otherwise be lost on the room change).
            if (session.isDraft() != null) {
                dayBuilder.add(IS_DRAFT, session.isDraft());
            }
            changedDays.add(dayBuilder);
        }
        return changedDays;
    }

    private static JsonObject buildChangeCourtRoomForMultidayErrorBody(final String errorCode, final String message) {
        return createObjectBuilder()
                .add(ERROR_CODE, errorCode)
                .add(MESSAGE, message)
                .build();
    }
    public void handleExtendHearingForHearing(final Envelope envelope) {

        final JsonObject payload = envelope.payload();
        final String unAllocatedHearingId = payload.getString(HEARING_ID, null);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.extend-hearing-for-hearing' received with payload {}", envelope.payload());
        }

        final ExtendHearingForHearing extendHearingForHearing = jsonObjectConverter.convert(payload, ExtendHearingForHearing.class);
        LOGGER.info("'listing.command.extend-hearing-for-hearing' extendHearingForHearing: {}", extendHearingForHearing);

        final UUID allocatedHearingId = extendHearingForHearing.getAllocatedHearingId();

        final ExtendHearingForHearingEnriched.Builder builder = ExtendHearingForHearingEnriched
                .extendHearingForHearingEnriched().withAllocatedHearingId(allocatedHearingId)
                .withUnAllocatedHearingId(fromString(unAllocatedHearingId))
                .withSendNotificationToParties(extendHearingForHearing.getSendNotificationToParties());

        if (extendHearingForHearing.getProsecutionCases() != null) {
            builder.withProsecutionCases(extendHearingForHearing.getProsecutionCases());
        }

        final ExtendHearingForHearingEnriched extendHearingForHearingEnriched = builder
                .build();

        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING_ENRICHED),
                objectToJsonValueConverter.convert(extendHearingForHearingEnriched)));
    }
    public void handleChangeJudiciaryForHearings(final Envelope envelope) {
        send(envelope);
    }
    public void handleSequenceHearings(final Envelope envelope) {
        send(envelope);
    }
    public void handleRestrictCourtList(final Envelope jsonEnvelope) {
        send(jsonEnvelope);
    }

    // MBD refactor slice P1a: listing notes run in-process (ListingNoteService + outbox),
    // no longer via listing.handler.command and the event store.
    public void handleUpdateHearingDayCourtSchedule(Envelope envelope) {
        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_HEARING_DAY_COURT_SCHEDULE),
                envelope.payload()));
    }
    public void handleDeleteHearing(final Envelope envelope) {
        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DELETE_HEARING),
                envelope.payload()));
    }
    public void handleUpdateHearingAddCaseBdf(final Envelope envelope) {
        send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_HEARING_ADD_CASE_BDF),
                envelope.payload()));
    }

    /**
     * The JEE {@code sender.send(command)} to the command handler: the command service method for the
     * command's name, called directly.
     */
    private void send(final Envelope command) {
        switch (command.name()) {
            case HearingCommands.LIST_COURT_HEARING_ENRICHED -> hearingCommands.listCourtHearingEnriched(command);
            case HearingCommands.LIST_UNSCHEDULED_COURT_HEARING_ENRICHED -> hearingCommands.listUnscheduledCourtHearingEnriched(command);
            case HearingCommands.UPDATE_HEARING_FOR_LISTING_ENRICHED -> hearingCommands.updateHearingForListingEnriched(command);
            case HearingCommands.UPDATE_HEARINGS_FOR_LISTING_ENRICHED -> hearingCommands.updateHearingsForListingEnriched(command);
            case HearingCommands.VACATE_TRIAL_ENRICHED -> hearingCommands.vacateTrialEnriched(command);
            case HearingCommands.MOVE_HEARING_TO_PAST_DATE_ENRICHED -> hearingCommands.moveHearingToPastDateEnriched(command);
            case HearingCommands.CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING_ENRICHED -> hearingCommands.changeCourtRoomForMultidayHearingEnriched(command);
            case HearingCommands.EXTEND_HEARING_FOR_HEARING_ENRICHED -> hearingCommands.extendHearingForHearingEnriched(command);
            case HearingCommands.CHANGE_JUDICIARY_FOR_HEARINGS -> hearingCommands.changeJudiciaryForHearings(command);
            case HearingCommands.SEQUENCE_HEARINGS -> hearingCommands.sequenceHearings(command);
            case HearingCommands.RESTRICT_COURT_LIST -> hearingCommands.restrictCourtList(command);
            case HearingCommands.UPDATE_HEARING_DAY_COURT_SCHEDULE -> hearingCommands.updateHearingDayCourtSchedule(command);
            case HearingCommands.DELETE_HEARING -> hearingCommands.deleteHearing(command);
            case HearingCommands.UPDATE_HEARING_ADD_CASE_BDF -> hearingCommands.updateHearingAddCaseBdf(command);
            case HearingCommands.MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES -> hearingCommands.migrateCrownHearingsToCourtschedules(command);
            case HearingCommands.MARK_UNALLOCATED_HEARING_AS_DUPLICATE -> hearingCommands.markUnallocatedHearingAsDuplicate(command);
            case HearingCommands.UPDATE_DEFENDANTS_FOR_HEARING -> hearingCommands.updateDefendantsForHearing(command);
            case HearingCommands.UPDATE_OFFENCES_FOR_HEARING -> hearingCommands.updateOffencesForHearing(command);
            case HearingCommands.DELETE_OFFENCES_FOR_HEARING -> hearingCommands.deleteOffencesForHearing(command);
            case HearingCommands.ADD_OFFENCES_FOR_HEARING -> hearingCommands.addOffencesForHearing(command);
            case NextHearingCommands.LIST_NEXT_HEARINGS_ENRICHED_V2 -> nextHearingCommands.listNextHearingsEnrichedV2(command);
            case NextHearingCommands.LIST_UNSCHEDULED_NEXT_HEARINGS_ENRICHED -> nextHearingCommands.listUnscheduledNextHearingsEnriched(command);
            case NextHearingCommands.DELETE_NEXT_HEARINGS -> nextHearingCommands.deleteNextHearings(command);
            case NextHearingCommands.DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING -> nextHearingCommands.deletePreviousHearingsAndCreateNextHearing(command);
            case NextHearingCommands.UPDATE_EXISTING_HEARING -> nextHearingCommands.updateExistingHearing(command);
            default -> throw new IllegalStateException("No command service for " + command.name());
        }
    }

    private UpdateHearingForListingEnriched updateHearingForListingEnriched(final UpdateHearingForListing updateHearingForListing,
                                                                            final CourtCentreDetails courtCentreDetails,
                                                                            final JsonObject payload) {
        checkCourtRoomIsOptionalForCrownCourts(updateHearingForListing);
        final JsonArray prosecutionCases = payload.getJsonArray(PROSECUTION_CASES);


        return UpdateHearingForListingEnriched.updateHearingForListingEnriched()
                .withCourtCentreDetails(courtCentreDetails)
                .withUpdateHearingForListing(updateHearingForListing)
                .withProsecutionCases(nonNull(prosecutionCases) ? prosecutionCases.stream()
                        .map(p -> jsonObjectConverter.convert((JsonObject) p, ProsecutionCases.class))
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private void checkCourtRoomIsOptionalForCrownCourts(final UpdateHearingForListing updateHearingForListing) {

        /*We have courtRoom? don't go any further */
        if (!isNull(updateHearingForListing.getCourtRoomId())) {
            return;
        }

        /* We are OK if we have no courtRoom and it's crown*/
        if (CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            return;
        }

        throw new CommandBadRequestException("courtRoomId must not be empty for this case");
    }

    private Set<CourtCentreDetails> getCourtCentreDetails(final Envelope envelope, final List<HearingListingNeeds> hearingListingNeeds) {
        final Set<CourtCentreDetails> courtCentres = new HashSet<>();
        hearingListingNeeds.forEach(
                hln -> courtCentres.add(courtCentreFactory.getCourtCentre(hln.getCourtCentre().getId(), envelope))
        );
        return courtCentres;
    }

    public static UUID getCourtCentreId(final UpdateHearingForListing updateHearingForListing) {
        return nonNull(updateHearingForListing.getSelectedCourtCentre()) ? updateHearingForListing.getSelectedCourtCentre().getId() : updateHearingForListing.getCourtCentreId();
    }

    record UpdateHearingForListingPayloadWrapper(UpdateHearingForListing updateHearingForListing,
                                                 JsonObject fullPayload) {
    }
}
