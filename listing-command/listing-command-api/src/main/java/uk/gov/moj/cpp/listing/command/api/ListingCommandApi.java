package uk.gov.moj.cpp.listing.command.api;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.listing.courts.ListCourtHearingEnriched.listCourtHearingEnriched;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.ListCourtHearing;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.ExtendHearingForHearing;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.ListNextHearingsV2;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearings;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.courts.UpdateExistingHearing;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.courts.UpdateRelatedHearing;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.api.service.HearingEnrichmentOrchestrator;
import uk.gov.moj.cpp.listing.command.api.service.HearingLookupService;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateResult;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.VacateTrialEnriched;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
@SuppressWarnings({"squid:S2629", "java:S6204"})
public class ListingCommandApi {

    private static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";
    private static final String LISTING_COMMAND_UPDATE_HEARINGS_FOR_LISTING_ENRICHED = "listing.command.update-hearings-for-listing-enriched";
    private static final String LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED = "listing.command.list-court-hearing-enriched";
    private static final String LISTING_COMMAND_LIST_NEXT_HEARINGS_ENRICHED = "listing.command.list-next-hearings-enriched-v2";
    private static final String LISTING_COMMAND_LIST_UNSCHEDULED_COURT_HEARING_ENRICHED = "listing.command.list-unscheduled-court-hearing-enriched";
    private static final String LISTING_COMMAND_LIST_UNSCHEDULED_NEXT_HEARINGS_ENRICHED = "listing.command.list-unscheduled-next-hearings-enriched";
    private static final String LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING_ENRICHED = "listing.command.extend-hearing-for-hearing-enriched";
    private static final String LISTING_COMMAND_VACATE_TRIAL = "listing.command.vacate-trial-enriched";
    private static final String LISTING_COMMAND_MOVE_HEARING_TO_PAST_DATE_ENRICHED = "listing.command.move-hearing-to-past-date-enriched";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String START_DATE = "startDate";
    private static final String JURISDICTION = "jurisdiction";
    private static final String OUCODE_L1_NAME = "oucodeL1Name";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String SESSION_DATE = "sessionDate";
    private static final String SESSION_START_TIME = "sessionStartTime";
    private static final String SESSION_END_TIME = "sessionEndTime";
    private static final String DURATION_IN_MINUTES = "durationInMinutes";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String ERROR_CODE = "errorCode";
    private static final String MESSAGE = "message";
    public static final String FUTURE_DATE_NOT_ALLOWED = "FUTURE_DATE_NOT_ALLOWED";
    public static final String INVALID_DATE = "INVALID_DATE";
    public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String MULTI_DAY_NOT_ALLOWED = "MULTI_DAY_NOT_ALLOWED";
    public static final String START_DATE_TOO_OLD = "START_DATE_TOO_OLD";
    public static final String HEARING_ID_NOT_FOUND = "HEARING_ID_NOT_FOUND";
    private static final String NO_HEARING_FOUND_FOR_HEARING_ID = "No hearing found for hearingId ";
    private static final String MOVE_TO_EARLIER_DATE_ONLY = "Hearings can only be moved to an earlier date";
    private static final String END_DATE = "endDate";
    private static final String START_DATE_TIME = "startDateTime";
    private static final String END_DATE_TIME = "endDateTime";
    private static final String SEQUENCE = "sequence";
    private static final int MAX_PAST_MONTHS = 6;
    private static final String CROWN_JURISDICTION = "CROWN";
    private static final String MAGISTRATES_JURISDICTION = "MAGISTRATES";
    private static final String LISTING_COMMAND_CORRECT_HEARING_DAYS_WO_CC = "listing.command.correct-hearing-days-without-court-centre";
    private static final String LISTING_COMMAND_DUPLICATE_UNALLOCATED_HEARING = "listing.command.mark-unallocated-hearing-as-duplicate";
    private static final String LISTING_COMMAND_UPDATE_EXISTING_HEARING = "listing.command.update-existing-hearing";
    private static final String LISTING_COMMAND_DELETE_NEXT_HEARINGS = "listing.command.delete-next-hearings";
    private static final String LISTING_COMMAND_DELETE_HEARING = "listing.command.delete-hearing";
    private static final String LISTING_COMMAND_DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING = "listing.command.delete-previous-hearings-and-create-next-hearing";
    private static final String LISTING_COMMAND_UPDATE_HEARING_DAY_COURT_SCHEDULE = "listing.command.update-hearing-day-court-schedule";
    private static final String LISTING_COMMAND_MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES = "listing.command.migrate-crown-hearings-to-courtschedules";
    public static final String LISTING_COMMAND_UPDATE_HEARING_ADD_CASE_BDF = "listing.command.update-hearing-add-case-bdf";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListingCommandApi.class);
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String HEARING_ID = "hearingId";
    public static final String START_DATE_MUST_BE_SMALLER_THAN_END_DATE = "startDate must be smaller than endDate";
    public static final String WEEK_COMMENCING_START_DATE_MUST_BE_SMALLER_THAN_WEEK_COMMENCING_END_DATE = "Week commencing start date must be smaller than week commencing end date";

    public static final String OUCODE = "oucode";

    @Inject
    private Sender sender;

    @Inject
    private CourtCentreFactory courtCentreFactory;
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;
    @Inject
    private HearingSlotsService hearingSlotsService;
    @Inject
    private HearingEnrichmentOrchestrator hearingEnrichmentOrchestrator;
    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;
    @Inject
    private HearingLookupService hearingLookupService;

    @Handles("listing.command.list-court-hearing")
    public void handleListCourtHearing(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-court-hearing' received with payload {}", envelope.toObfuscatedDebugString());
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

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED),
                objectToJsonValueConverter.convert(listCourtHearingEnriched)));
    }

    @Handles("listing.list-next-hearings-v2")
    public void listNextHearings(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.list-next-hearings-v2' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final JsonObject payload = envelope.payloadAsJsonObject();
        final ListNextHearingsV2 listNextHearings = jsonObjectConverter.convert(payload, ListNextHearingsV2.class);
        final List<HearingListingNeeds> enrichedHearings = hearingEnrichmentOrchestrator.enrichListCourtHearing(listNextHearings.getHearings(), envelope);
        final Set<CourtCentreDetails> courtCentres = getCourtCentreDetails(envelope, enrichedHearings);

        final ListNextHearingsEnrichedV2 listNextHearingsEnriched = ListNextHearingsEnrichedV2.listNextHearingsEnrichedV2()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withListNextHearings(ListNextHearingsV2.listNextHearingsV2()
                        .withValuesFrom(listNextHearings)
                        .withHearings(enrichedHearings).build())
                .withAdjournedFromDate(listNextHearings.getAdjournedFromDate())
                .withSeedingHearing(listNextHearings.getSeedingHearing())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_LIST_NEXT_HEARINGS_ENRICHED),
                objectToJsonValueConverter.convert(listNextHearingsEnriched)));
    }

    @Handles("listing.update-related-hearing")
    public void updateRelatedHearing(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.update-related-hearing' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final JsonObject payload = envelope.payloadAsJsonObject();
        final UpdateRelatedHearing updateRelatedHearing = jsonObjectConverter.convert(payload, UpdateRelatedHearing.class);
        final UUID hearingId = fromString(payload.getString(HEARING_ID));

        final UpdateExistingHearing updateExistingHearing = UpdateExistingHearing.updateExistingHearing()
                .withHearingId(hearingId)
                .withSeedingHearing(updateRelatedHearing.getSeedingHearing())
                .withProsecutionCases(updateRelatedHearing.getProsecutionCases())
                .withShadowListedOffences(updateRelatedHearing.getShadowListedOffences())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_EXISTING_HEARING), objectToJsonValueConverter.convert(updateExistingHearing)));
    }

    @Handles("listing.command.list-unscheduled-court-hearing")
    public void handleListUnscheduledCourtHearing(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-unscheduled-court-hearing' received with payload {}", envelope.toObfuscatedDebugString());
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

        sender.send(envelop(objectToJsonValueConverter.convert(listCourtHearingEnriched)).withName(LISTING_COMMAND_LIST_UNSCHEDULED_COURT_HEARING_ENRICHED)
                .withMetadataFrom(envelope));
    }

    @Handles("listing.list-unscheduled-next-hearings")
    public void handleListUnscheduledNextCourtHearings(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.list-unscheduled-next-hearings' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final ListUnscheduledNextHearings unscheduledNextHearings = jsonObjectConverter.convert(payload, ListUnscheduledNextHearings.class);

        final Set<CourtCentreDetails> courtCentres = new HashSet<>();

        for (final HearingUnscheduledListingNeeds commandHearing : unscheduledNextHearings.getHearings()) {
            courtCentres.add(courtCentreFactory.getCourtCentre(commandHearing.getCourtCentre().getId(), envelope));
        }
        final ListUnscheduledNextHearingsEnriched listCourtHearingEnriched = ListUnscheduledNextHearingsEnriched.listUnscheduledNextHearingsEnriched()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withHearings(unscheduledNextHearings.getHearings())
                .withSeedingHearing(unscheduledNextHearings.getSeedingHearing())
                .build();

        sender.send(envelop(objectToJsonValueConverter.convert(listCourtHearingEnriched)).withName(LISTING_COMMAND_LIST_UNSCHEDULED_NEXT_HEARINGS_ENRICHED)
                .withMetadataFrom(envelope));
    }

    @Handles("listing.delete-next-hearings")
    public void handleDeleteNextHearings(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.delete-next-hearings' received with payload {}", envelope.toObfuscatedDebugString());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DELETE_NEXT_HEARINGS),
                createObjectBuilder()
                        .add("seedingHearing", payload.getJsonObject("seedingHearing"))
                        .build()
        ));
    }

    @Handles("listing.delete-previous-hearings-and-create-next-hearing")
    public void handleDeletePreviousHearingsAndCreateNextHearing(final JsonEnvelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.delete-previous-hearings-and-create-next-hearing' received with payload {}", envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DELETE_PREVIOUS_HEARINGS_AND_CREATE_NEXT_HEARING),
                envelope.payload()));
    }

    @Handles("listing.command.update-hearing-for-listing")
    public void handleUpdateHearingForListing(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-hearing-for-listing' received with payload {}", envelope.toObfuscatedDebugString());
        }

        UpdateHearingForListing updateHearingForListing = jsonObjectConverter.convert(payload, UpdateHearingForListing.class);

        if (updateHearingForListing.getStartDate() != null &&
                updateHearingForListing.getEndDate() != null &&
                updateHearingForListing.getStartDate().isAfter(updateHearingForListing.getEndDate())) {
            throw new BadRequestException(START_DATE_MUST_BE_SMALLER_THAN_END_DATE);
        }

        if (updateHearingForListing.getWeekCommencingStartDate() != null &&
                updateHearingForListing.getWeekCommencingEndDate() != null &&
                updateHearingForListing.getWeekCommencingStartDate().isAfter(updateHearingForListing.getWeekCommencingEndDate())) {
            throw new BadRequestException(WEEK_COMMENCING_START_DATE_MUST_BE_SMALLER_THAN_WEEK_COMMENCING_END_DATE);
        }

        LOGGER.info("HandleUpdateHearingForListing for the hearing: {} ", updateHearingForListing.getHearingId());
        final CourtCentreDetails courtCentre =
                courtCentreFactory.getCourtCentre(getCourtCentreId(updateHearingForListing), envelope);
        updateHearingForListing = hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(updateHearingForListing, envelope, courtCentre);

        final UpdateHearingForListingEnriched updateHearingForListingEnriched =
                updateHearingForListingEnriched(updateHearingForListing, courtCentre, payload);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED), objectToJsonValueConverter.convert(updateHearingForListingEnriched)));
    }

    @Handles("listing.command.update-hearings-for-listing")
    public void handleUpdateHearingsForListing(final JsonEnvelope envelope) {
        final JsonObject hearingsPayload = envelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-hearings-for-listing' received with payload {}", envelope.toObfuscatedDebugString());
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

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).
                withName(LISTING_COMMAND_UPDATE_HEARINGS_FOR_LISTING_ENRICHED), hearingsJsonObjBuilder.build()));
    }

    @Handles("listing.command.vacate-trial")
    public void handleVacateTrial(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.vacate-trial' received with payload {}", envelope.toObfuscatedDebugString());
        }
        final VacateTrialEnriched vacateTrialEnriched = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), VacateTrialEnriched.class);

        LOGGER.info("HandleVacateTrial for the hearing: {} ", vacateTrialEnriched.getHearingId());
        hearingSlotsService.delete(vacateTrialEnriched.getHearingId());

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_VACATE_TRIAL),
                envelope.payload()));
    }

    @Handles("listing.command.move-hearing-to-past-date")
    public void handleMoveHearingToPastDate(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.move-hearing-to-past-date' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final UUID courtCentreId = fromString(payload.getString(COURT_CENTRE_ID));
        final UUID courtRoomId = fromString(payload.getString(COURT_ROOM_ID));
        // startDateTime/endDateTime are absolute UTC instants (e.g. 2026-07-20T10:30:00.000Z) and both
        // mandatory. The request schema only range-checks digits with a regex, so an impossible calendar
        // date such as 2026-06-31 passes schema validation and reaches here - parseInstant converts that
        // into a clean 422 INVALID_DATE rather than an unhandled 500. Keep the raw strings for the MAGS
        // courtscheduler call (its request schema needs the exact T HH:mm:ss wire format).
        final String startTimeStr = payload.getString(START_DATE_TIME);
        final String endTimeStr = payload.getString(END_DATE_TIME);
        final ZonedDateTime startInstant = parseInstant(startTimeStr, START_DATE_TIME);
        final ZonedDateTime endInstant = parseInstant(endTimeStr, END_DATE_TIME);
        final LocalDate startDate = startInstant.toLocalDate();
        final LocalDate endDate = endInstant.toLocalDate();

        validateMoveDates(startInstant, endInstant);
        // Weekends are permitted target dates: magistrates courts sit Saturdays (remand courts), so
        // whether a session exists on the requested day is courtscheduler's decision - not a
        // listing-side calendar rule. CROWN moves take the requested date as-is.
        final List<LocalDate> sittingDays = datesBetween(startDate, endDate);

        // The hearing must already exist in the listing viewstore. An unknown hearingId is rejected
        // synchronously with a 422 here - before any court-centre lookup, courtscheduler booking, or
        // enriched event - so a move can never be issued for a hearing that was never listed. Checked
        // after the cheap in-memory date validation so a malformed request still fails fast
        // without a viewstore read.
        hearingLookupService.findHearing(hearingId, envelope)
                .orElseThrow(() -> new MoveHearingToPastDateException(422,
                        buildMoveHearingToPastDateErrorBody(HEARING_ID_NOT_FOUND, NO_HEARING_FOUND_FOR_HEARING_ID + hearingId),
                        NO_HEARING_FOUND_FOR_HEARING_ID + hearingId));

        // Single-day only (multi-day is rejected by validateMoveDates): the duration is the
        // submitted window between startDateTime and endDateTime.
        final int durationInMinutes = (int) java.time.temporal.ChronoUnit.MINUTES.between(startInstant, endInstant);

        // Jurisdiction is taken from the target court centre (not the hearing): a Crown court centre's
        // top-level OU name (oucodeL1Name) contains "Crown"; anything else is treated as MAGISTRATES.
        final JsonObject courtCentre = courtCentreFactory.getOrganisationUnit(courtCentreId, envelope);
        final String oucodeL1Name = courtCentre.getString(OUCODE_L1_NAME, "");
        final String jurisdictionType = oucodeL1Name.toUpperCase(Locale.ROOT).contains(CROWN_JURISDICTION)
                ? CROWN_JURISDICTION : MAGISTRATES_JURISDICTION;

        final JsonObjectBuilder enrichedBuilder = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(JURISDICTION, jurisdictionType)
                .add(COURT_CENTRE_ID, courtCentreId.toString());

        // CROWN moves are listing-side only (Baris decision D1); MAGS delegates to courtscheduler. Both
        // return the actual hearing-day dates they produced (CROWN = sitting days; MAGS = booked slots).
        final List<LocalDate> hearingDayDates = CROWN_JURISDICTION.equals(jurisdictionType)
                ? enrichWithReissuedDays(enrichedBuilder, sittingDays, courtRoomId, startInstant, endInstant, durationInMinutes)
                : enrichWithBookedPastDateSlots(enrichedBuilder, hearingId, courtCentreId, courtRoomId, startTimeStr, endTimeStr);

        // Main-level startDate/endDate track the hearing days: earliest and latest hearing-day date
        // (mirrors the update-hearing-for-listing / list-court-hearing enrichment that derives them from days).
        final List<LocalDate> sortedDayDates = hearingDayDates.stream().sorted().collect(Collectors.toList());
        enrichedBuilder.add(START_DATE, sortedDayDates.get(0).toString());
        enrichedBuilder.add(END_DATE, sortedDayDates.get(sortedDayDates.size() - 1).toString());

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_MOVE_HEARING_TO_PAST_DATE_ENRICHED),
                enrichedBuilder.build()));
    }

    /**
     * Validated for ALL jurisdictions in the synchronous layer so the caller gets a 422 before any
     * event is sent or slot booked: the target must be strictly earlier than today (today and any
     * future date are rejected), endDateTime not before startDateTime (full instant comparison, so a
     * same-day end time earlier than the start time is also rejected), single day only (endDate must
     * equal startDate), and startDate no older than {@value #MAX_PAST_MONTHS} months before today.
     */
    private static void validateMoveDates(final ZonedDateTime startInstant, final ZonedDateTime endInstant) {
        final LocalDate startDate = startInstant.toLocalDate();
        final LocalDate endDate = endInstant.toLocalDate();
        final LocalDate today = LocalDate.now();
        // A hearing can only be moved to a date strictly before today - today itself and any future
        // date are rejected (isBefore is exclusive of today).
        if (!startDate.isBefore(today) || !endDate.isBefore(today)) {
            throw new MoveHearingToPastDateException(422,
                    buildMoveHearingToPastDateErrorBody(FUTURE_DATE_NOT_ALLOWED, MOVE_TO_EARLIER_DATE_ONLY),
                    MOVE_TO_EARLIER_DATE_ONLY);
        }
        if (endInstant.isBefore(startInstant)) {
            throw new MoveHearingToPastDateException(422,
                    buildMoveHearingToPastDateErrorBody(INVALID_DATE_RANGE, "endDateTime must not be earlier than startDateTime"),
                    "endDateTime " + endInstant + " is earlier than startDateTime " + startInstant);
        }
        if (endDate.isAfter(startDate)) {
            throw new MoveHearingToPastDateException(422,
                    buildMoveHearingToPastDateErrorBody(MULTI_DAY_NOT_ALLOWED, "Hearings can only be moved to a single date"),
                    "Multi-day move rejected: startDate " + startDate + " to endDate " + endDate);
        }
        if (startDate.isBefore(today.minusMonths(MAX_PAST_MONTHS))) {
            throw new MoveHearingToPastDateException(422,
                    buildMoveHearingToPastDateErrorBody(START_DATE_TOO_OLD,
                            "startDate cannot be earlier than " + MAX_PAST_MONTHS + " months before today"),
                    "startDate " + startDate + " is more than " + MAX_PAST_MONTHS + " months in the past");
        }
    }

    /**
     * Expands an inclusive [startDate, endDate] span into one sitting day per calendar date. Weekends
     * are permitted, so a Saturday or Sunday becomes a sitting day like any other date.
     * validateMoveDates has already guaranteed endDate is not before startDate, so this never returns
     * an empty list.
     */
    private static List<LocalDate> datesBetween(final LocalDate startDate, final LocalDate endDate) {
        final List<LocalDate> days = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            days.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    /**
     * CROWN moves never call courtscheduler. Each sitting day is re-issued on the new date at the
     * submitted start/end time-of-day, in the requested room (mandatory), with the caller's computed
     * duration (the submitted startDateTime-endDateTime window).
     */
    private static List<LocalDate> enrichWithReissuedDays(final JsonObjectBuilder enrichedBuilder,
                                                          final List<LocalDate> sittingDays, final UUID courtRoomId,
                                                          final ZonedDateTime startInstant, final ZonedDateTime endInstant,
                                                          final int durationInMinutes) {
        final LocalTime startLocalTime = startInstant.toLocalTime();
        final LocalTime endLocalTime = endInstant.toLocalTime();
        final JsonArrayBuilder daysBuilder = createArrayBuilder();
        int sequence = 1;
        for (final LocalDate day : sittingDays) {
            final ZonedDateTime start = ZonedDateTime.of(day, startLocalTime, ZoneOffset.UTC);
            final ZonedDateTime end = ZonedDateTime.of(day, endLocalTime, ZoneOffset.UTC);
            daysBuilder.add(createObjectBuilder()
                    .add(SESSION_DATE, day.toString())
                    .add(SESSION_START_TIME, start.toString())
                    .add(SESSION_END_TIME, end.toString())
                    .add(DURATION_IN_MINUTES, durationInMinutes)
                    .add(SEQUENCE, sequence++)
                    .add(COURT_ROOM_ID, courtRoomId.toString()));
        }
        enrichedBuilder.add(HEARING_DAYS, daysBuilder);
        return sittingDays;
    }

    /**
     * MAGS path. courtscheduler books the requested day in one atomic call (releasing prior
     * allocations once), returning the booked slot, which we map into the enriched hearingDays[].
     * A no-session/booking failure surfaces synchronously as a 422.
     */
    private List<LocalDate> enrichWithBookedPastDateSlots(final JsonObjectBuilder enrichedBuilder, final UUID hearingId,
                                                          final UUID courtCentreId, final UUID courtRoomId, final String startTime,
                                                          final String endTime) {
        // courtscheduler derives the submitted start/end time-of-day and the duration from
        // startTime/endTime and returns the booked slot(s), which we map straight into the
        // enriched hearingDays.
        final List<MoveHearingToPastDateResult> slots = courtSchedulerServiceAdapter.moveHearingToPastDate(
                hearingId, courtCentreId, courtRoomId, startTime, endTime);

        final JsonArrayBuilder daysBuilder = createArrayBuilder();
        final List<LocalDate> dayDates = new ArrayList<>();
        int sequence = 1;
        for (final MoveHearingToPastDateResult slot : slots) {
            final JsonObjectBuilder dayBuilder = createObjectBuilder().add(SEQUENCE, sequence++);
            if (slot.sessionDate() != null) {
                dayBuilder.add(SESSION_DATE, slot.sessionDate().toString());
                dayDates.add(slot.sessionDate());
            }
            if (slot.courtScheduleId() != null) {
                dayBuilder.add(COURT_SCHEDULE_ID, slot.courtScheduleId().toString());
            }
            if (slot.courtRoomId() != null) {
                dayBuilder.add(COURT_ROOM_ID, slot.courtRoomId());
            }
            if (slot.sessionStartTime() != null) {
                dayBuilder.add(SESSION_START_TIME, slot.sessionStartTime());
            }
            if (slot.sessionEndTime() != null) {
                dayBuilder.add(SESSION_END_TIME, slot.sessionEndTime());
            }
            if (slot.durationInMinutes() != null) {
                dayBuilder.add(DURATION_IN_MINUTES, slot.durationInMinutes());
            }
            daysBuilder.add(dayBuilder);
        }
        enrichedBuilder.add(HEARING_DAYS, daysBuilder);
        return dayDates;
    }

    /**
     * The request schema's regex only range-checks digits, so an impossible calendar date (e.g.
     * 2026-06-31) reaches this handler - convert the parse failure into a clean 422 INVALID_DATE.
     */
    private static ZonedDateTime parseInstant(final String value, final String field) {
        try {
            return ZonedDateTime.parse(value);
        } catch (final DateTimeParseException e) {
            throw new MoveHearingToPastDateException(422,
                    buildMoveHearingToPastDateErrorBody(INVALID_DATE, field + " is not a valid date"),
                    field + " '" + value + "' is not a valid calendar date/time");
        }
    }

    private static JsonObject buildMoveHearingToPastDateErrorBody(final String errorCode, final String message) {
        return createObjectBuilder()
                .add(ERROR_CODE, errorCode)
                .add(MESSAGE, message)
                .build();
    }

    @Handles("listing.command.extend-hearing-for-hearing")
    public void handleExtendHearingForHearing(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String unAllocatedHearingId = payload.getString(HEARING_ID, null);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.extend-hearing-for-hearing' received with payload {}", envelope.toObfuscatedDebugString());
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

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING_ENRICHED),
                objectToJsonValueConverter.convert(extendHearingForHearingEnriched)));
    }

    @Handles("listing.command.change-judiciary-for-hearings")
    public void handleChangeJudiciaryForHearings(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.sequence-hearings")
    public void handleSequenceHearings(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.restrict-court-list")
    public void handleRestrictCourtList(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.publish-court-list")
    public void handlePublishCourtList(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.publish-court-lists-for-crown-courts")
    @SuppressWarnings("WeakerAccess") // Must be public for the framework
    public void handlePublishCourtListForCrownCourts(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.court-list-request-export")
    public void handleCourtListRequestExport(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.create-listing-note")
    public void handleCreateNote(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.edit-listing-note")
    public void handleEditNote(final JsonEnvelope jsonEnvelope) {
        sender.send(JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName("listing.command.handler.edit-listing-note"),
                jsonEnvelope.payloadAsJsonObject()));
    }

    @Handles("listing.command.delete-listing-note")
    public void handleDeleteNote(final JsonEnvelope jsonEnvelope) {
        sender.send(JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName("listing.command.handler.delete-listing-note"),
                jsonEnvelope.payloadAsJsonObject()));
    }

    @Handles("listing.correct-hearing-days-without-court-centre")
    public void handleCorrectHearingDaysWithoutCourtCentre(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_CORRECT_HEARING_DAYS_WO_CC),
                envelope.payload()));
    }

    @Handles("listing.update-hearing-day-court-schedule")
    public void handleUpdateHearingDayCourtSchedule(JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_HEARING_DAY_COURT_SCHEDULE),
                envelope.payload()));
    }

    @Handles("listing.migrate-crown-hearings-to-courtschedules")
    public void handleMigrateCrownHearingsToCourtSchedules(JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES),
                envelope.payload()));
    }

    @Handles("listing.mark-unallocated-hearing-as-duplicate")
    public void handleMarkUnallocatedHearingAsDuplicate(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DUPLICATE_UNALLOCATED_HEARING),
                envelope.payload()));
    }

    @Handles("listing.command.delete-hearing")
    public void handleDeleteHearing(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_DELETE_HEARING),
                envelope.payload()));
    }

    @Handles("listing.update-hearing-add-case-bdf")
    public void handleUpdateHearingAddCaseBdf(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_HEARING_ADD_CASE_BDF),
                envelope.payload()));
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

        throw new BadRequestException("courtRoomId must not be empty for this case");
    }

    private Set<CourtCentreDetails> getCourtCentreDetails(final JsonEnvelope envelope, final List<HearingListingNeeds> hearingListingNeeds) {
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
