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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

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
    private static final String LISTING_COMMAND_CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING_ENRICHED = "listing.command.change-court-room-for-multiday-hearing-enriched";
    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";
    private static final String NO_HEARING_FOUND_FOR_HEARING_ID = "No hearing found for hearingId ";
    private static final String NON_DEFAULT_DAY_DURATION = "duration";
    private static final String SEND_NOTIFICATION_TO_PARTIES = "sendNotificationToParties";
    private static final String CHANGED_DAYS = "changedDays";
    private static final String HEARING_DATE = "hearingDate";
    public static final String NOT_CROWN_HEARING = "NOT_CROWN_HEARING";
    public static final String NOT_MULTIDAY_HEARING = "NOT_MULTIDAY_HEARING";
    public static final String DUPLICATE_DAY_DATES = "DUPLICATE_DAY_DATES";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String START_DATE = "startDate";
    private static final String JURISDICTION = "jurisdiction";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String ESTIMATED_MINUTES = "estimatedMinutes";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String ROOM_ID = "roomId";
    private static final String VIRTUAL = "virtual";
    private static final String IS_DRAFT = "isDraft";
    private static final String SESSION_DATE = "sessionDate";
    private static final String SESSION_START_TIME = "sessionStartTime";
    private static final String SESSION_END_TIME = "sessionEndTime";
    private static final String DURATION_IN_MINUTES = "durationInMinutes";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String DAY_START_TIME = "startTime";
    private static final String DAY_END_TIME = "endTime";
    private static final String DAY_DURATION_MINUTES = "durationMinutes";
    private static final String ERROR_CODE = "errorCode";
    private static final String MESSAGE = "message";
    public static final String HEARING_ID_NOT_FOUND = "HEARING_ID_NOT_FOUND";
    public static final String FUTURE_DATE_NOT_ALLOWED = "FUTURE_DATE_NOT_ALLOWED";
    private static final String CROWN_JURISDICTION = "CROWN";
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
        final List<HearingListingNeeds> enrichedHearings = hearingEnrichmentOrchestrator.enrichListCourtHearing(
                listNextHearings.getHearings(), envelope,
                uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackSource.LIST_NEXT_HEARINGS_V2);
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
        final LocalDate startDate = LocalDate.parse(payload.getString(START_DATE));

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

        if (CROWN_JURISDICTION.equals(jurisdictionType)) {
            // Baris decision D1: CROWN moves are listing-side only, courtscheduler is never called.
            rejectCrownMoveToFutureDate(startDate);
            enrichWithExistingDayDetails(enrichedBuilder, hearing, startDate);
        } else {
            enrichWithBookedPastDateSlot(enrichedBuilder, hearingId, courtCentreId, startDate, hearing);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_MOVE_HEARING_TO_PAST_DATE_ENRICHED),
                enrichedBuilder.build()));
    }

    /**
     * CROWN moves never call courtscheduler, so the re-dated hearing day is rebuilt from the
     * hearing's own current first sitting day — same room and time-of-day, on the new past date.
     */
    private static void enrichWithExistingDayDetails(final JsonObjectBuilder enrichedBuilder, final JsonObject hearing, final LocalDate startDate) {
        final JsonArray hearingDays = hearing.containsKey(HEARING_DAYS) ? hearing.getJsonArray(HEARING_DAYS) : null;
        if (hearingDays == null || hearingDays.isEmpty()) {
            return;
        }
        final JsonObject day = hearingDays.getJsonObject(0);
        enrichedBuilder.add(SESSION_DATE, startDate.toString());
        if (day.containsKey(COURT_ROOM_ID) && !day.isNull(COURT_ROOM_ID)) {
            enrichedBuilder.add(COURT_ROOM_ID, day.getString(COURT_ROOM_ID));
        }
        if (day.containsKey(DAY_START_TIME) && !day.isNull(DAY_START_TIME)) {
            enrichedBuilder.add(SESSION_START_TIME,
                    java.time.ZonedDateTime.parse(day.getString(DAY_START_TIME)).with(startDate).toString());
        }
        if (day.containsKey(DAY_END_TIME) && !day.isNull(DAY_END_TIME)) {
            enrichedBuilder.add(SESSION_END_TIME,
                    java.time.ZonedDateTime.parse(day.getString(DAY_END_TIME)).with(startDate).toString());
        }
        if (day.containsKey(DAY_DURATION_MINUTES) && !day.isNull(DAY_DURATION_MINUTES)) {
            enrichedBuilder.add(DURATION_IN_MINUTES, day.getInt(DAY_DURATION_MINUTES));
        }
    }

    private static void rejectCrownMoveToFutureDate(final LocalDate startDate) {
        if (startDate.isAfter(LocalDate.now())) {
            throw new MoveHearingToPastDateException(422,
                    buildMoveHearingToPastDateErrorBody(FUTURE_DATE_NOT_ALLOWED, "Hearings can only be moved to today or an earlier date"),
                    "Hearings can only be moved to today or an earlier date");
        }
    }

    private void enrichWithBookedPastDateSlot(final JsonObjectBuilder enrichedBuilder, final UUID hearingId,
                                              final UUID courtCentreId, final LocalDate startDate, final JsonObject hearing) {
        final Integer durationInMinutes = (hearing.containsKey(ESTIMATED_MINUTES) && !hearing.isNull(ESTIMATED_MINUTES))
                ? hearing.getInt(ESTIMATED_MINUTES) : null;

        final MoveHearingToPastDateResult slot =
                courtSchedulerServiceAdapter.moveHearingToPastDate(hearingId, courtCentreId, startDate, durationInMinutes);

        if (slot.courtScheduleId() != null) {
            enrichedBuilder.add(COURT_SCHEDULE_ID, slot.courtScheduleId().toString());
        }
        if (slot.courtRoomId() != null) {
            enrichedBuilder.add(COURT_ROOM_ID, slot.courtRoomId());
        }
        if (slot.sessionDate() != null) {
            enrichedBuilder.add(SESSION_DATE, slot.sessionDate().toString());
        }
        if (slot.sessionStartTime() != null) {
            enrichedBuilder.add(SESSION_START_TIME, slot.sessionStartTime());
        }
        if (slot.sessionEndTime() != null) {
            enrichedBuilder.add(SESSION_END_TIME, slot.sessionEndTime());
        }
        if (slot.durationInMinutes() != null) {
            enrichedBuilder.add(DURATION_IN_MINUTES, slot.durationInMinutes());
        }
    }

    private static JsonObject buildMoveHearingToPastDateErrorBody(final String errorCode, final String message) {
        return createObjectBuilder()
                .add(ERROR_CODE, errorCode)
                .add(MESSAGE, message)
                .build();
    }

    /**
     * CROWN-only. Changes the courtroom of one or more SELECTED days of a multi-day CROWN
     * hearing. Days not present in {@code nonDefaultDays} are never touched. Schema violations
     * (missing/malformed fields) are rejected as 400 by the framework via the request schema;
     * business failures (unknown hearing, non-CROWN, non-multiday, duplicate day dates, or a
     * courtscheduler rejection) are all surfaced as 422 via {@link ChangeCourtRoomForMultidayException}
     * so no command is ever sent.
     */
    @Handles("listing.command.change-court-room-for-multiday-hearing")
    public void handleChangeCourtRoomForMultidayHearing(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.change-court-room-for-multiday-hearing' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final JsonArray nonDefaultDays = payload.getJsonArray(NON_DEFAULT_DAYS);

        validateCrownMultidayHearingOrThrow(hearingId, envelope);

        // Each requested day is either VIRTUAL (virtual=true -> (re)booked in courtscheduler and
        // converted into a hearing day) or REAL (virtual false/absent -> persisted as a nonDefaultDay,
        // never booked). Dates must be unique across the WHOLE request regardless of type.
        //
        // Virtual days are keyed by date, so the adapter response (booked sessions) can be joined back
        // to the originating request BY DATE - the adapter is free to return its sessions in any order,
        // so a positional join would silently mismatch rooms/times across days.
        final Set<LocalDate> seenDates = new HashSet<>();
        final Map<LocalDate, JsonObject> virtualRequestedByDate = new LinkedHashMap<>();
        final List<RequestedChangeDay> virtualDays = new ArrayList<>();
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
                virtualRequestedByDate.put(date, nonDefaultDay);
                virtualDays.add(new RequestedChangeDay(date, fromString(nonDefaultDay.getString(COURT_SCHEDULE_ID)),
                        nonDefaultDay.getInt(NON_DEFAULT_DAY_DURATION)));
            } else {
                realNonDefaultDays.add(buildEnrichedRealNonDefaultDay(nonDefaultDay));
            }
        }

        // Only virtual days are sent to courtscheduler; real days are never booked.
        final JsonArrayBuilder changedDays = bookVirtualDaysIntoChangedDays(hearingId, virtualDays, virtualRequestedByDate);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_CHANGE_COURT_ROOM_FOR_MULTIDAY_HEARING_ENRICHED),
                createObjectBuilder()
                        .add(HEARING_ID, hearingId.toString())
                        .add(SEND_NOTIFICATION_TO_PARTIES, payload.getBoolean(SEND_NOTIFICATION_TO_PARTIES, true))
                        .add(CHANGED_DAYS, changedDays.build())
                        .add(NON_DEFAULT_DAYS, realNonDefaultDays.build())
                        .build()));
    }

    /** CROWN-only business validations for change-court-room-for-multiday: hearing must exist, be CROWN, and be multiday. */
    private void validateCrownMultidayHearingOrThrow(final UUID hearingId, final JsonEnvelope envelope) {
        final JsonObject hearing = hearingLookupService.findHearing(hearingId, envelope)
                .orElseThrow(() -> new ChangeCourtRoomForMultidayException(422,
                        buildChangeCourtRoomForMultidayErrorBody(HEARING_ID_NOT_FOUND, NO_HEARING_FOUND_FOR_HEARING_ID + hearingId),
                        NO_HEARING_FOUND_FOR_HEARING_ID + hearingId));

        if (!CROWN_JURISDICTION.equals(hearing.getString(JURISDICTION_TYPE, null))) {
            throw new ChangeCourtRoomForMultidayException(422,
                    buildChangeCourtRoomForMultidayErrorBody(NOT_CROWN_HEARING, "change-court-room-for-multiday-hearing is CROWN-only"),
                    "change-court-room-for-multiday-hearing is CROWN-only");
        }

        final JsonArray hearingDays = hearing.containsKey(HEARING_DAYS) ? hearing.getJsonArray(HEARING_DAYS) : null;
        if (hearingDays == null || hearingDays.size() < 2) {
            throw new ChangeCourtRoomForMultidayException(422,
                    buildChangeCourtRoomForMultidayErrorBody(NOT_MULTIDAY_HEARING, "Hearing " + hearingId + " is not a multiday hearing"),
                    "Hearing " + hearingId + " is not a multiday hearing");
        }
    }

    /** Enriched nonDefaultDay for a real day. roomId is the uuid room identity; the FE's legacy integer courtRoomId (if sent) is ignored. */
    private JsonObjectBuilder buildEnrichedRealNonDefaultDay(final JsonObject nonDefaultDay) {
        return createObjectBuilder()
                .add(DAY_START_TIME, nonDefaultDay.getString(DAY_START_TIME))
                .add(DAY_DURATION_MINUTES, nonDefaultDay.getInt(NON_DEFAULT_DAY_DURATION))
                .add(COURT_CENTRE_ID, nonDefaultDay.getString(COURT_CENTRE_ID))
                .add(ROOM_ID, nonDefaultDay.getString(ROOM_ID))
                .add(COURT_SCHEDULE_ID, nonDefaultDay.getString(COURT_SCHEDULE_ID));
    }

    /** Sends the virtual days to courtscheduler (once) and enriches the booked sessions into changedDays, joined by date. */
    private JsonArrayBuilder bookVirtualDaysIntoChangedDays(final UUID hearingId, final List<RequestedChangeDay> virtualDays,
            final Map<LocalDate, JsonObject> virtualRequestedByDate) {
        final JsonArrayBuilder changedDays = createArrayBuilder();
        if (virtualDays.isEmpty()) {
            return changedDays;
        }
        final List<ChangedDaySession> booked = courtSchedulerServiceAdapter.changeCourtRoomForMultidayHearing(hearingId, virtualDays);
        for (final ChangedDaySession session : booked) {
            final JsonObject requested = virtualRequestedByDate.get(session.sessionDate());
            final JsonObjectBuilder dayBuilder = createObjectBuilder()
                    .add(HEARING_DATE, session.sessionDate().toString())
                    .add(DAY_START_TIME, session.sessionStartTime() != null ? session.sessionStartTime() : requested.getString(DAY_START_TIME))
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
