package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.listing.command.api.service.HearingDaysEnrichmentService.log;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.api.util.SlotsToJsonStringConverter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;
import uk.gov.moj.cpp.listing.domain.HearingSlotSearchResponse;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.ListUpdateHearing;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressWarnings("java:S3776")
public class CourtScheduleEnrichmentService implements EnrichmentService {
    private static final String HEARING_SLOTS = "hearingSlots";
    private static final String COURT_SCHEDULE_IDS = "courtScheduleIds";
    private static final String JUDICIARIES = "judiciaries";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    @Inject
    private CourtSchedulerService courtSchedulerService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtScheduleEnrichmentService.class);

    public static final String HEARING_DATE = "hearingDate";
    public static final String HEARING_SESSION_DATE_CUT_OFF = "hearingSessionDateSearchCutOff";
    public static final String HEARING_START_TIME = "hearingStartTime";
    public static final String DURATION_MINUTES = "durationInMinutes";
    public static final String IS_POLICE = "isPolice";
    public static final String HEARING_ID = "hearingId";
    public static final String COURT_ROOM_ID = "courtRoomId";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    @Inject
    CourtCentreFactory courtCentreFactory;
    @Inject
    private HearingSlotsService hearingSlotsService;
    @Inject
    private SlotsToJsonStringConverter slotsToJsonStringConverter;

    public HearingListingNeeds enrichWithCourtSchedules(final HearingListingNeeds hearingEnrichedWithDurations, final JsonEnvelope envelope) {
        return checkAndUpdateListingCourtScheduler(hearingEnrichedWithDurations, envelope);
    }

    public UpdateHearingForListing enrichWithCourtSchedules(final UpdateHearingForListing updateHearingForListing, final JsonEnvelope envelope) {
        if (JurisdictionType.CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            return enrichCrownUpdateHearing(updateHearingForListing);
        }
        //HearingDays courtscheduleId provided in payload, we can list them directly
        List<HearingDay> hearingDaysWithCourScheduleId = new ArrayList<>();

        final boolean isMultiDay = updateHearingForListing.getHearingDays().size() > 1;
        updateHearingForListing.getHearingDays().forEach(hearingDay -> {
            if (isNull(hearingDay.getCourtScheduleId())) {
                HearingSlotSearchResponse hearingSlotSearchResponse = getFirstAvailableSlot(updateHearingForListing, hearingDay, envelope, isMultiDay);
                hearingDaysWithCourScheduleId.add(populateHearingDaysByHearingSlotSearch(hearingDay, hearingSlotSearchResponse));
                // No need to collect judiciaries from search - they will be included in the list response
            } else {
                hearingDaysWithCourScheduleId.add(hearingDay);
            }
        });
        final JsonArray courtScheduleIds = slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(hearingDaysWithCourScheduleId);
        final JsonObject updateSlotsPayload = getUpdateSlotsPayload(updateHearingForListing.getHearingId(), courtScheduleIds);
        final Response response = hearingSlotsService.listHearingInCourtSessions(updateSlotsPayload);
        final List<HearingDay> enrichedHearingDays = combineSearchAndBookResponseAndListResponse(response, hearingDaysWithCourScheduleId);
        
        // Extract judiciary information from list response (this includes all judiciaries for all court schedule IDs)
        final List<JudicialRole> enrichedJudiciaries = populateJudiciaryInfoFromSlots(response);

        UpdateHearingForListing.Builder hearingBuilder = UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(updateHearingForListing)
                .withHearingDays(enrichedHearingDays);
        
        // Apply conditional judiciary logic: use existing if present, otherwise use response judiciary
        if (isNotEmpty(updateHearingForListing.getJudiciary())) {
            // Keep existing judiciary information
            hearingBuilder.withJudiciary(updateHearingForListing.getJudiciary());
        } else if (isNotEmpty(enrichedJudiciaries)) {
            // Use judiciary from list response, converting from domain to core model
            hearingBuilder.withJudiciary(convertJudicialRoleDomainToCore(enrichedJudiciaries));
        }
        // If neither exists, don't call withJudiciary at all (no blank lists)
        
        return hearingBuilder.build();
    }

    /**
     * CROWN-first enrichment: determines case and calls appropriate court scheduler endpoint.
     * Called BEFORE HearingDays and Duration enrichment for CROWN hearings.
     *
     * Case 1: No courtScheduleId anywhere -> return unchanged
     * Case 2: Has courtScheduleId + aggregated duration > 360 -> multiDaySearchAndBook
     * Case 3: Has courtScheduleId + aggregated duration <= 360 -> listHearingInCourtSessions
     */
    public HearingListingNeeds enrichCrownCourtScheduleFirst(final HearingListingNeeds hearing) {
        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Starting for hearingId: {}", hearing.getId());

        final boolean hasCourtScheduleIdOnHearingDays = anyHearingDayHasCourtScheduleId(hearing);
        final boolean hasCourtScheduleId = hasCourtScheduleIdOnHearingDays || hasBookedSlotsWithCourtScheduleId(hearing);

        if (!hasCourtScheduleId) {
            LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Case 1: No courtScheduleId found. Returning unchanged for hearingId: {}", hearing.getId());
            return hearing;
        }

        // If courtScheduleId is only on bookedSlots (not on hearingDays), skip enrichment
        // because the enrichment methods require hearingDays with courtScheduleIds
        if (!hasCourtScheduleIdOnHearingDays) {
            LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] courtScheduleId found on bookedSlots only (no hearingDays). Returning unchanged for hearingId: {}", hearing.getId());
            return hearing;
        }

        final int aggregatedDuration = calculateAggregatedDuration(hearing);
        final boolean isMultiDay = aggregatedDuration > HearingDurationEnrichmentService.MINUTES_IN_DAY;

        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] hearingId: {}, aggregatedDuration={}, isMultiDay={} (threshold={})",
                hearing.getId(), aggregatedDuration, isMultiDay, HearingDurationEnrichmentService.MINUTES_IN_DAY);

        EnrichmentResult enrichmentResult;
        if (isMultiDay) {
            LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Case 2: Multi-day -> multiDaySearchAndBook for hearingId: {}", hearing.getId());
            enrichmentResult = handleCrownMultiDayEnrichment(hearing);
        } else {
            LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Case 3: Single-day -> listHearingInCourtSessions for hearingId: {}", hearing.getId());
            enrichmentResult = handleCrownSingleDayEnrichment(hearing);
        }

        final List<HearingDay> enrichedHearingDays = enrichmentResult.getHearingDays();
        final List<JudicialRole> enrichedJudiciaries = enrichmentResult.getJudiciaries();

        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Result: enrichedHearingDays={}, judiciaries={} for hearingId: {}",
                enrichedHearingDays.size(), enrichedJudiciaries.size(), hearing.getId());

        HearingListingNeeds.Builder hearingBuilder = HearingListingNeeds.hearingListingNeeds()
                .withValuesFrom(hearing)
                .withHearingDays(enrichedHearingDays);

        if (isNotEmpty(enrichedJudiciaries)) {
            hearingBuilder.withJudiciary(convertJudicialRoleDomainToCore(enrichedJudiciaries));
        }

        // Adjust court centre if scheduler returned a different room
        if (isNotEmpty(enrichedHearingDays) && nonNull(hearing.getCourtCentre())) {
            final CourtCentre adjustedCourtCentre = CourtCentre.courtCentre()
                    .withValuesFrom(hearing.getCourtCentre())
                    .withRoomId(enrichedHearingDays.get(0).getCourtRoomId())
                    .build();
            hearingBuilder.withCourtCentre(adjustedCourtCentre);
        }

        return hearingBuilder.build();
    }

    /**
     * CROWN-first enrichment for update path.
     * Called BEFORE HearingDays and Duration enrichment for CROWN update hearings.
     */
    public UpdateHearingForListing enrichCrownCourtScheduleFirst(final UpdateHearingForListing hearing) {
        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Update path starting for hearingId: {}", hearing.getHearingId());

        final boolean hasCourtScheduleIdOnHearingDays = !isEmpty(hearing.getHearingDays())
                && hearing.getHearingDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()));
        final boolean hasCourtScheduleIdOnNonDefaultDays = !isEmpty(hearing.getNonDefaultDays())
                && hearing.getNonDefaultDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()));
        final boolean hasCourtScheduleId = hasCourtScheduleIdOnHearingDays || hasCourtScheduleIdOnNonDefaultDays;

        if (!hasCourtScheduleId) {
            LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Update Case 1: No courtScheduleId on hearingDays or nonDefaultDays. Returning unchanged for hearingId: {}", hearing.getHearingId());
            return hearing;
        }

        final int aggregatedDuration = calculateAggregatedDuration(hearing);
        LOGGER.info("[CROWN-ENRICH][CourtSchedule-First] Update hearingId: {}, aggregatedDuration={}, isMultiDay={}",
                hearing.getHearingId(), aggregatedDuration, aggregatedDuration > HearingDurationEnrichmentService.MINUTES_IN_DAY);

        // Delegate to existing enrichCrownUpdateHearing which already handles multi-day vs single-day
        return enrichCrownUpdateHearing(hearing);
    }

    private UpdateHearingForListing enrichCrownUpdateHearing(final UpdateHearingForListing hearing) {
        LOGGER.info("CROWN update enrichment for hearingId: {}", hearing.getHearingId());

        final boolean anyHearingDayHasCourtScheduleId = !isEmpty(hearing.getHearingDays())
                && hearing.getHearingDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()));
        if (!anyHearingDayHasCourtScheduleId) {
            if (isCandidateForAllocation(hearing)) {
                LOGGER.info("CROWN update: no courtScheduleIds but allocation candidate for hearingId {}. Searching and booking.", hearing.getHearingId());
                return handleCrownUpdateSearchAndBook(hearing);
            }
            LOGGER.info("CROWN update: no courtScheduleIds on hearingDays for hearingId {}. Skipping court schedule enrichment.", hearing.getHearingId());
            return hearing;
        }

        final int totalDuration = hearing.getHearingDays().stream()
                .mapToInt(d -> d.getDurationMinutes() != null ? d.getDurationMinutes() : 0)
                .sum();
        final boolean isMultiDay = totalDuration > HearingDurationEnrichmentService.MINUTES_IN_DAY;

        EnrichmentResult enrichmentResult;
        if (isMultiDay) {
            final HearingDay firstDay = hearing.getHearingDays().stream()
                    .filter(d -> nonNull(d.getCourtScheduleId()))
                    .findFirst().orElse(null);

            if (firstDay == null) {
                LOGGER.error("CROWN multi-day update: no courtScheduleId on hearingDays for hearingId {}", hearing.getHearingId());
                return hearing;
            }

            final List<CourtSchedule> sessions = multiDaySearchAndBook(
                    firstDay.getCourtScheduleId().toString(),
                    totalDuration,
                    hearing.getHearingId().toString());

            if (isEmpty(sessions)) {
                LOGGER.warn("CROWN multi-day update: no sessions found for hearingId {}.", hearing.getHearingId());
                return hearing;
            }

            final int daysNeeded = sessions.size();
            final int durationPerDay = totalDuration / daysNeeded;
            final List<HearingDay> expandedDays = sessions.stream().map(session -> HearingDay.hearingDay()
                    .withCourtCentreId(fromString(session.getCourtHouseId()))
                    .withCourtScheduleId(fromString(session.getCourtScheduleId()))
                    .withCourtRoomId(fromString(session.getCourtRoomId()))
                    .withStartTime(nonNull(session.getHearingStartTime()) ? ZonedDateTime.parse(session.getHearingStartTime()) : null)
                    .withHearingDate(session.getSessionDate())
                    .withDurationMinutes(durationPerDay)
                    .withIsDraft(session.isDraft())
                    .build()
            ).toList();

            final boolean allNonDraft = sessions.stream().noneMatch(CourtSchedule::isDraft);
            if (!allNonDraft) {
                LOGGER.warn("CROWN multi-day update: isDraft=true sessions for hearingId {}. HearingDays will carry isDraft=true.", hearing.getHearingId());
                return UpdateHearingForListing.updateHearingForListing()
                        .withValuesFrom(hearing)
                        .withHearingDays(expandedDays)
                        .build();
            }

            enrichmentResult = listHearingSessionsAndExtractData(hearing.getHearingId(), expandedDays);
        } else {
            final List<String> courtScheduleIds = hearing.getHearingDays().stream()
                    .filter(d -> nonNull(d.getCourtScheduleId()))
                    .map(d -> d.getCourtScheduleId().toString())
                    .toList();

            final List<CourtSchedule> sessions = fetchCourtSchedulesByIds(courtScheduleIds);

            if (isEmpty(sessions)) {
                LOGGER.warn("CROWN single-day update: failed to fetch court schedules for hearingId {}. Returning unchanged.", hearing.getHearingId());
                return hearing;
            }

            final boolean allNonDraft = sessions.stream().noneMatch(CourtSchedule::isDraft);
            final List<HearingDay> sanityCheckedDays = sanityCheckAndEnrichCrown(hearing.getHearingDays(), sessions, hearing.getHearingId());

            if (!allNonDraft) {
                LOGGER.warn("CROWN single-day update: isDraft=true sessions for hearingId {}. HearingDays will carry isDraft=true.", hearing.getHearingId());
                return UpdateHearingForListing.updateHearingForListing()
                        .withValuesFrom(hearing)
                        .withHearingDays(sanityCheckedDays)
                        .build();
            }

            enrichmentResult = listHearingSessionsAndExtractData(hearing.getHearingId(), sanityCheckedDays);
        }

        final List<HearingDay> enrichedHearingDays = enrichmentResult.getHearingDays();
        final List<JudicialRole> enrichedJudiciaries = enrichmentResult.getJudiciaries();

        UpdateHearingForListing.Builder hearingBuilder = UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(hearing)
                .withHearingDays(enrichedHearingDays);

        if (isNotEmpty(hearing.getJudiciary())) {
            hearingBuilder.withJudiciary(hearing.getJudiciary());
        } else if (isNotEmpty(enrichedJudiciaries)) {
            hearingBuilder.withJudiciary(convertJudicialRoleDomainToCore(enrichedJudiciaries));
        }

        return hearingBuilder.build();
    }

    private UpdateHearingForListing handleCrownUpdateSearchAndBook(final UpdateHearingForListing hearing) {
        List<HearingDay> hearingDaysWithCourtScheduleId = new ArrayList<>();
        List<JudicialRole> judicialRolesBySearchAndBook = new ArrayList<>();

        hearing.getHearingDays().forEach(hearingDay -> {
            if (isNull(hearingDay.getCourtScheduleId())) {
                final String hearingDate = nonNull(hearingDay.getHearingDate())
                        ? hearingDay.getHearingDate().toString()
                        : hearing.getStartDate().toString();
                final String startTime = nonNull(hearingDay.getStartTime())
                        ? DateAndTimeUtils.toIsoString(hearingDay.getStartTime())
                        : null;
                final UUID courtRoomId = nonNull(hearingDay.getCourtRoomId())
                        ? hearingDay.getCourtRoomId()
                        : hearing.getCourtRoomId();
                HearingSlotSearchResponse hearingSlotSearchResponse = searchAndBookSlots(
                        hearing.getHearingId().toString(),
                        hearing.getCourtCentreId().toString(),
                        hearingDate,
                        nonNull(courtRoomId) ? courtRoomId.toString() : null,
                        nonNull(hearing.getEndDate()) ? hearing.getEndDate().toString() : null,
                        startTime,
                        hearingDay.getDurationMinutes(),
                        false
                );
                if (hearingSlotSearchResponse == null) {
                    hearingDaysWithCourtScheduleId.add(hearingDay);
                } else {
                    // Only take courtScheduleId from searchAndBook; preserve hearing day's original courtRoomId/courtCentreId/dates
                    hearingDaysWithCourtScheduleId.add(HearingDay.hearingDay()
                            .withValuesFrom(hearingDay)
                            .withCourtScheduleId(fromString(hearingSlotSearchResponse.courtScheduleId()))
                            .build());
                    if (hearingSlotSearchResponse.judiciaries() != null && !hearingSlotSearchResponse.judiciaries().isEmpty()) {
                        judicialRolesBySearchAndBook.addAll(hearingSlotSearchResponse.judiciaries());
                    }
                }
            } else {
                hearingDaysWithCourtScheduleId.add(hearingDay);
            }
        });

        if (hearingDaysWithCourtScheduleId.stream().allMatch(d -> isNull(d.getCourtScheduleId()))) {
            LOGGER.warn("CROWN update searchAndBook: no slots found for hearingId {}. Returning unchanged.", hearing.getHearingId());
            return hearing;
        }

        UpdateHearingForListing.Builder hearingBuilder = UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(hearing)
                .withHearingDays(hearingDaysWithCourtScheduleId);

        if (isNotEmpty(hearing.getJudiciary())) {
            hearingBuilder.withJudiciary(hearing.getJudiciary());
        } else if (isNotEmpty(judicialRolesBySearchAndBook)) {
            hearingBuilder.withJudiciary(convertJudicialRoleDomainToCore(judicialRolesBySearchAndBook));
        }

        return hearingBuilder.build();
    }

    public static boolean isCandidateForAllocation(final HearingListingNeeds hearing) {
        //This is derived from Hearing aggregate canAllocate()
        boolean hasValidStartDateTime = nonNull(hearing.getListedStartDateTime()) || nonNull(hearing.getEarliestStartDateTime());
        boolean hasAssignedCourtRoom = nonNull(hearing.getCourtCentre()) && nonNull(hearing.getCourtCentre().getRoomId());
        boolean hasJurisdictionType = nonNull(hearing.getJurisdictionType());


        return hasJurisdictionType
                && hasValidStartDateTime
                && hasAssignedCourtRoom;
    }

    public static boolean isCandidateForAllocation(final UpdateHearingForListing hearing) {
        //This is derived from Hearing aggregate canAllocate()
        boolean hasValidStartDateTime = nonNull(hearing.getStartDate());
        boolean hasAssignedCourtRoom = nonNull(hearing.getCourtRoomId());
        boolean hasJurisdictionType = nonNull(hearing.getJurisdictionType());


        return hasJurisdictionType
                && hasValidStartDateTime
                && hasAssignedCourtRoom;
    }

    private boolean isPolice(final HearingListingNeeds hearingListingNeeds, final JsonEnvelope envelope) {
        final boolean isPolice;
        if (hearingListingNeeds.getProsecutionCases() != null) {
            isPolice = courtCentreFactory.getPoliceFlagForProsecutorId(envelope, hearingListingNeeds.getProsecutionCases()
                    .get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityId().toString());
        } else {
            isPolice = false;
        }
        return isPolice;
    }

    private HearingListingNeeds checkAndUpdateListingCourtScheduler(final HearingListingNeeds hearing, final JsonEnvelope envelope) {

        if (needsCourtScheduleEnrichment(hearing)) {
            EnrichmentResult enrichmentResult;

            if (JurisdictionType.CROWN.equals(hearing.getJurisdictionType()) && anyHearingDayHasCourtScheduleId(hearing)) {
                enrichmentResult = handleCrownEnrichment(hearing);
            }
            // Case 1: All nondefault days have courtScheduleId
            else if (allHearingDaysHaveCourtScheduleId(hearing)) {
                enrichmentResult = handleDirectListingCase(hearing);
            }
            // Case 2: Has booked slots with courtScheduleId (Crown or MAGS)
            else if (hasBookedSlotsWithCourtScheduleId(hearing)) {
                enrichmentResult = handleBookedSlotsCase(hearing);
            }
            // Crown without courtScheduleIds: go directly to searchAndBook, skip provisional booking
            else if (JurisdictionType.CROWN.equals(hearing.getJurisdictionType()) && isCandidateForAllocation(hearing)) {
                LOGGER.info("CROWN hearing without courtScheduleIds, searching and booking for hearingId: {}", hearing.getId());
                enrichmentResult = handleAllocationCandidate(hearing, envelope);
            }
            // Case 3: Has booking reference (provisional booking) — MAGS only at this point
            else if (nonNull(hearing.getBookingReference())) {
                enrichmentResult = handleProvisionalBookingCase(hearing);
            }
            // Case 4: Is candidate for allocation
            else if (isCandidateForAllocation(hearing)) {
                LOGGER.info("Hearing is candidate for allocation, so we need to search and book hearingId : {}, hearingDays : {}", hearing.getId(), log(hearing.getHearingDays()));
                enrichmentResult = handleAllocationCandidate(hearing, envelope);
            } else {
                // Default case - no enrichment possible
                enrichmentResult = new EnrichmentResult(new ArrayList<>(), new ArrayList<>());
            }

            final List<HearingDay> enrichedHearingDays = enrichmentResult.getHearingDays();
            final List<JudicialRole> enrichedDomainJudiciaries = enrichmentResult.getJudiciaries();

            List<RotaSlot> newlyPopulatedRotaSlot = null;
            if (isNotEmpty(hearing.getBookedSlots())) {
                newlyPopulatedRotaSlot = populateBookedSlots(hearing.getBookedSlots(), enrichedHearingDays);
            }
            /**in case we land in a different courtroom then requested, this should be reflected to main CourtCentre Object
             will be removed with LPT-1090 along with LPT-1355*/
            final CourtCentre adjustedCourtCentre = CourtCentre.courtCentre().withValuesFrom(hearing.getCourtCentre())
                    .withRoomId(enrichedHearingDays.get(0).getCourtRoomId())
                    .build();

            HearingListingNeeds.Builder hearingBuilder = HearingListingNeeds.hearingListingNeeds()
                    .withValuesFrom(hearing)
                    .withCourtCentre(adjustedCourtCentre)
                    .withHearingDays(enrichedHearingDays);
            if (isNotEmpty(newlyPopulatedRotaSlot)) {
                hearingBuilder.withBookedSlots(newlyPopulatedRotaSlot);
            }
            if (isNotEmpty(enrichedDomainJudiciaries)) {
                hearingBuilder.withJudiciary(convertJudicialRoleDomainToCore(enrichedDomainJudiciaries));
            }
            return hearingBuilder.build();
        }
        return hearing;
    }

    private static List<uk.gov.justice.core.courts.JudicialRole> convertJudicialRoleDomainToCore(final List<JudicialRole> enrichedDomainJudiciaries) {
        if (isEmpty(enrichedDomainJudiciaries)) {
            return Collections.emptyList();
        }

        return enrichedDomainJudiciaries.stream()
                .map(domainRole -> uk.gov.justice.core.courts.JudicialRole.judicialRole()
                        .withIsBenchChairman(domainRole.getIsBenchChairman().orElse(null))
                        .withIsDeputy(domainRole.getIsDeputy().orElse(null))
                        .withJudicialId(domainRole.getJudicialId())
                        .withUserId(domainRole.getUserId())
                        .withJudicialRoleType(uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                                .withJudicialRoleTypeId(domainRole.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                                .withJudiciaryType(domainRole.getJudicialRoleType().getJudiciaryType())
                                .build())
                        .build())
                .toList();
    }

    private List<JudicialRole> populateJudiciaryInfoFromSlots(final Response response) {
        List<JudicialRole> judiciaryList = new ArrayList<>();
        if (isSuccess(response)) {
            final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());

            if (responseJson != null && !responseJson.isEmpty()) {
                final JsonObject hearingObject = (JsonObject) responseJson.getJsonArray("hearings").get(0);
                if (hearingObject.containsKey(JUDICIARIES)) {
                    final JsonArray judiciariesArray = hearingObject.getJsonArray(JUDICIARIES);
                    if (judiciariesArray != null && !judiciariesArray.isEmpty()) {
                        for (int i = 0; i < judiciariesArray.size(); i++) {
                            JsonObject judicialRoleJson = judiciariesArray.getJsonObject(i);
                            JudicialRole judicialRole = buildJudicialRoleFromJson(judicialRoleJson);
                            judiciaryList.add(judicialRole);
                        }
                    }
                }
            }
        }
        return judiciaryList;
    }

    private static JsonObject getUpdateSlotsPayload(final UUID hearingId, final JsonArray courtScheduleIds) {
        final JsonObject hearingSlotWithId = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(COURT_SCHEDULE_IDS, courtScheduleIds)
                .build();

        final JsonArray hearingSlotsArray = createArrayBuilder()
                .add(hearingSlotWithId)
                .build();

        return createObjectBuilder()
                .add(HEARING_SLOTS, hearingSlotsArray)
                .build();
    }

    private boolean allHearingDaysHaveCourtScheduleId(HearingListingNeeds hearing) {
        return !isEmpty(hearing.getHearingDays()) &&
                hearing.getHearingDays().stream()
                        .noneMatch(day -> isNull(day.getCourtScheduleId()));
    }

    private boolean anyHearingDayHasCourtScheduleId(HearingListingNeeds hearing) {
        return !isEmpty(hearing.getHearingDays()) &&
                hearing.getHearingDays().stream()
                        .anyMatch(day -> nonNull(day.getCourtScheduleId()));
    }

    private boolean hasBookedSlotsWithCourtScheduleId(HearingListingNeeds hearing) {
        return isNotEmpty(hearing.getBookedSlots()) &&
                hearing.getBookedSlots().stream()
                        .allMatch(slot -> !isBlank(slot.getCourtScheduleId()));
    }

    private AllocationResult handleAllocationCandidate(HearingListingNeeds hearing, JsonEnvelope envelope) {
        List<HearingDay> hearingDaysBySearchAndBook = new ArrayList<>();
        List<JudicialRole> judicialRolesBySearchAndBook = new ArrayList<>();

        final ZonedDateTime effectiveStartDateTime = nonNull(hearing.getListedStartDateTime())
                ? hearing.getListedStartDateTime()
                : hearing.getEarliestStartDateTime();

        hearing.getHearingDays().forEach(hearingDay -> {
            if (isNull(hearingDay.getCourtScheduleId())) {
                boolean isPolice = JurisdictionType.CROWN.equals(hearing.getJurisdictionType())
                        ? false
                        : isPolice(hearing, envelope);
                HearingSlotSearchResponse hearingSlotSearchResponse = searchAndBookSlots(
                        hearing.getId().toString(),
                        hearing.getCourtCentre().getId().toString(),
                        effectiveStartDateTime.toLocalDate().toString(),
                        hearing.getCourtCentre().getRoomId().toString(),
                        hearing.getEndDate(),
                        DateAndTimeUtils.toIsoString(effectiveStartDateTime),
                        hearing.getEstimatedMinutes(),
                        isPolice
                );
                if (hearingSlotSearchResponse == null) {
                    //If you can't find by searchandBook add HearingDay as it is, it will be unallocated.
                    hearingDaysBySearchAndBook.add(hearingDay);
                } else {
                    hearingDaysBySearchAndBook.add(populateHearingDaysByHearingSlotSearch(hearingDay, hearingSlotSearchResponse));
                    // Collect judiciaries from the search response
                    if (hearingSlotSearchResponse.judiciaries() != null && !hearingSlotSearchResponse.judiciaries().isEmpty()) {
                        judicialRolesBySearchAndBook.addAll(hearingSlotSearchResponse.judiciaries());
                    }
                }
            }
        });
        return new AllocationResult(hearingDaysBySearchAndBook, judicialRolesBySearchAndBook);
    }

    private EnrichmentResult handleCrownEnrichment(final HearingListingNeeds hearing) {
        final boolean isMultiDay = nonNull(hearing.getEstimatedMinutes())
                && hearing.getEstimatedMinutes() > HearingDurationEnrichmentService.MINUTES_IN_DAY;
        if (isMultiDay) {
            return handleCrownMultiDayEnrichment(hearing);
        }
        return handleCrownSingleDayEnrichment(hearing);
    }

    private EnrichmentResult handleCrownSingleDayEnrichment(final HearingListingNeeds hearing) {
        LOGGER.info("CROWN single-day enrichment for hearingId: {}", hearing.getId());

        final List<String> courtScheduleIds = hearing.getHearingDays().stream()
                .filter(d -> nonNull(d.getCourtScheduleId()))
                .map(d -> d.getCourtScheduleId().toString())
                .toList();

        final List<CourtSchedule> sessions = fetchCourtSchedulesByIds(courtScheduleIds);

        if (isEmpty(sessions)) {
            LOGGER.warn("CROWN single-day: failed to fetch court schedules for hearingId {}. Returning unchanged.", hearing.getId());
            return new EnrichmentResult(hearing.getHearingDays(), new ArrayList<>());
        }

        final boolean allNonDraft = sessions.stream().noneMatch(CourtSchedule::isDraft);

        final List<HearingDay> sanityCheckedDays = sanityCheckAndEnrichCrown(hearing.getHearingDays(), sessions, hearing.getId());

        if (!allNonDraft) {
            LOGGER.warn("CROWN single-day: isDraft=true sessions for hearingId {}. HearingDays will carry isDraft=true, allocation decided by aggregate.", hearing.getId());
            return new EnrichmentResult(sanityCheckedDays, new ArrayList<>());
        }

        return listHearingSessionsAndExtractData(hearing.getId(), sanityCheckedDays);
    }

    private EnrichmentResult handleCrownMultiDayEnrichment(final HearingListingNeeds hearing) {
        LOGGER.info("CROWN multi-day enrichment for hearingId: {}", hearing.getId());

        final HearingDay firstDay = hearing.getHearingDays().stream()
                .filter(d -> nonNull(d.getCourtScheduleId()))
                .findFirst().orElse(null);

        if (firstDay == null) {
            LOGGER.error("CROWN multi-day: no courtScheduleId on hearingDays for hearingId {}", hearing.getId());
            return new EnrichmentResult(hearing.getHearingDays(), new ArrayList<>());
        }

        final List<CourtSchedule> sessions = multiDaySearchAndBook(
                firstDay.getCourtScheduleId().toString(),
                hearing.getEstimatedMinutes(),
                hearing.getId().toString());

        if (isEmpty(sessions)) {
            LOGGER.warn("CROWN multi-day: no consecutive sessions found for hearingId {}. Unallocated.", hearing.getId());
            return new EnrichmentResult(hearing.getHearingDays(), new ArrayList<>());
        }

        final List<HearingDay> expandedDays = buildHearingDaysFromMultiDaySessions(sessions, hearing);

        final boolean allNonDraft = sessions.stream().noneMatch(CourtSchedule::isDraft);
        if (!allNonDraft) {
            LOGGER.warn("CROWN multi-day: isDraft=true sessions for hearingId {}. HearingDays will carry isDraft=true, allocation decided by aggregate.", hearing.getId());
            return new EnrichmentResult(expandedDays, new ArrayList<>());
        }

        return listHearingSessionsAndExtractData(hearing.getId(), expandedDays);
    }

    private List<CourtSchedule> fetchCourtSchedulesByIds(final List<String> courtScheduleIds) {
        final Map<String, String> params = new HashMap<>();
        params.put(COURT_SCHEDULE_IDS, String.join(",", courtScheduleIds));
        final Response response = hearingSlotsService.getCourtSchedulesById(params);

        if (!isSuccess(response)) {
            LOGGER.error("fetchCourtSchedulesByIds failed with status {}", response.getStatus());
            return new ArrayList<>();
        }

        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());
        if (responseJson == null || responseJson.isEmpty()) {
            return new ArrayList<>();
        }

        final JsonArray schedulesArray = responseJson.getJsonArray("courtSchedules");
        if (schedulesArray == null || schedulesArray.isEmpty()) {
            return new ArrayList<>();
        }

        final List<CourtSchedule> schedules = new ArrayList<>();
        for (int i = 0; i < schedulesArray.size(); i++) {
            final CourtSchedule cs = jsonObjectConverter.convert(schedulesArray.getJsonObject(i), CourtSchedule.class);
            schedules.add(cs);
        }
        return schedules;
    }

    private List<CourtSchedule> multiDaySearchAndBook(final String courtScheduleId, final Integer durationInMinutes, final String hearingId) {
        final Map<String, String> params = new HashMap<>();
        params.put(COURT_SCHEDULE_ID, courtScheduleId);
        params.put(DURATION_MINUTES, String.valueOf(durationInMinutes));
        params.put(HEARING_ID, hearingId);
        final Response response = hearingSlotsService.multiDaySearchAndBook(params);

        if (!isSuccess(response)) {
            LOGGER.error("multiDaySearchAndBook failed with status {} for hearingId {}", response.getStatus(), hearingId);
            return new ArrayList<>();
        }

        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());
        if (responseJson == null || responseJson.isEmpty()) {
            return new ArrayList<>();
        }

        final JsonArray schedulesArray = responseJson.getJsonArray("courtSchedules");
        if (schedulesArray == null || schedulesArray.isEmpty()) {
            return new ArrayList<>();
        }

        final List<CourtSchedule> schedules = new ArrayList<>();
        for (int i = 0; i < schedulesArray.size(); i++) {
            final CourtSchedule cs = jsonObjectConverter.convert(schedulesArray.getJsonObject(i), CourtSchedule.class);
            schedules.add(cs);
        }
        return schedules;
    }

    private List<HearingDay> sanityCheckAndEnrichCrown(final List<HearingDay> hearingDays, final List<CourtSchedule> sessions, final UUID hearingId) {
        final Map<String, CourtSchedule> sessionsById = sessions.stream()
                .collect(Collectors.toMap(CourtSchedule::getCourtScheduleId, s -> s));

        return hearingDays.stream().map(hd -> {
            if (isNull(hd.getCourtScheduleId())) {
                return hd;
            }
            final CourtSchedule session = sessionsById.get(hd.getCourtScheduleId().toString());
            if (session == null) {
                LOGGER.error("CROWN sanity: no session for courtScheduleId {} hearingId {}", hd.getCourtScheduleId(), hearingId);
                return hd;
            }
            if (nonNull(hd.getHearingDate()) && !hd.getHearingDate().equals(session.getSessionDate())) {
                LOGGER.error("CROWN sanity: hearingDate={} but sessionDate={} for hearingId {}. Using scheduler value.",
                        hd.getHearingDate(), session.getSessionDate(), hearingId);
            }
            final HearingDay.Builder builder = HearingDay.hearingDay()
                    .withValuesFrom(hd)
                    .withHearingDate(session.getSessionDate())
                    .withCourtRoomId(fromString(session.getCourtRoomId()))
                    .withIsDraft(session.isDraft());
            if (nonNull(session.getCourtHouseId())) {
                builder.withCourtCentreId(fromString(session.getCourtHouseId()));
            }
            if (nonNull(session.getHearingStartTime())) {
                builder.withStartTime(ZonedDateTime.parse(session.getHearingStartTime()));
            }
            return builder.build();
        }).toList();
    }

    private List<HearingDay> buildHearingDaysFromMultiDaySessions(final List<CourtSchedule> sessions, final HearingListingNeeds hearing) {
        final int daysNeeded = sessions.size();
        final int durationPerDay = hearing.getEstimatedMinutes() / daysNeeded;

        return sessions.stream().map(session -> HearingDay.hearingDay()
                .withCourtCentreId(fromString(session.getCourtHouseId()))
                .withCourtScheduleId(fromString(session.getCourtScheduleId()))
                .withCourtRoomId(fromString(session.getCourtRoomId()))
                .withStartTime(nonNull(session.getHearingStartTime()) ? ZonedDateTime.parse(session.getHearingStartTime()) : null)
                .withHearingDate(session.getSessionDate())
                .withDurationMinutes(durationPerDay)
                .withIsDraft(session.isDraft())
                .build()
        ).toList();
    }

    private List<HearingDay> generateHearingDaysFromCourtSchedule(final List<HearingDay> hearingDays, final List<CourtSchedule> courtScheduleList, final HearingListingNeeds hearing) {
        final List<HearingDay> hearingDaysUpdatedByCourtSchedules = new ArrayList<>();
        final Map<LocalDate, HearingDay> hearingDaysMapByDate = hearingDays.stream().collect(Collectors.toMap(HearingDay::getHearingDate, HearingDay -> HearingDay));
        courtScheduleList.forEach(cs -> {
            if (hearingDaysMapByDate.get(cs.getSessionDate()) != null) {
                hearingDaysUpdatedByCourtSchedules.add(HearingDay.hearingDay()
                        .withValuesFrom(hearingDaysMapByDate.get(cs.getSessionDate()))
                        .withCourtScheduleId(fromString(cs.getCourtScheduleId()))
                        .withCourtRoomId(fromString(cs.getCourtRoomId()))
                        .withStartTime(ZonedDateTime.parse(cs.getHearingStartTime()))
                        .build());
            } else {
                hearingDaysUpdatedByCourtSchedules.add(HearingDay.hearingDay()
                        .withCourtCentreId(fromString(cs.getCourtHouseId()))
                        .withCourtScheduleId(fromString(cs.getCourtScheduleId()))
                        .withCourtRoomId(fromString(cs.getCourtRoomId()))
                        .withStartTime(ZonedDateTime.parse(cs.getHearingStartTime()))
                        .withHearingDate(cs.getSessionDate())
                        .withDurationMinutes(hearing.getEstimatedMinutes())
                        .build());
            }
        });
        return hearingDaysUpdatedByCourtSchedules;
    }

    /**
     * Checks if the input hearing has courtScheduleId on hearingDays or bookedSlots.
     * Used by the orchestrator to decide enrichment order for CROWN.
     */
    public static boolean hasCourtScheduleIdOnInput(final HearingListingNeeds hearing) {
        final boolean onHearingDays = !isEmpty(hearing.getHearingDays())
                && hearing.getHearingDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()));
        final boolean onBookedSlots = isNotEmpty(hearing.getBookedSlots())
                && hearing.getBookedSlots().stream().anyMatch(s -> !isBlank(s.getCourtScheduleId()));
        return onHearingDays || onBookedSlots;
    }

    static boolean needsCourtScheduleEnrichment(final HearingListingNeeds hearing) {
        if (JurisdictionType.MAGISTRATES.equals(hearing.getJurisdictionType())) {
            return !isEmpty(hearing.getNonDefaultDays()) || nonNull(hearing.getBookingReference())
                    || nonNull(hearing.getBookedSlots()) || isCandidateForAllocation(hearing);
        }
        if (JurisdictionType.CROWN.equals(hearing.getJurisdictionType())) {
            return isCrownFixedDateWithCourtScheduleId(hearing)
                    || (isNull(hearing.getWeekCommencingDate()) && isCandidateForAllocation(hearing));
        }
        return false;
    }

    /**
     * Calculates the total duration for the CROWN multi-day vs single-day decision.
     * Priority: hearingDays durationMinutes → nonDefaultDays duration → estimatedMinutes → 0
     */
    static int calculateAggregatedDuration(final HearingListingNeeds hearing) {
        if (isNotEmpty(hearing.getHearingDays())) {
            return hearing.getHearingDays().stream()
                    .mapToInt(d -> d.getDurationMinutes() != null ? d.getDurationMinutes() : 0)
                    .sum();
        }
        if (isNotEmpty(hearing.getNonDefaultDays())) {
            return hearing.getNonDefaultDays().stream()
                    .mapToInt(d -> d.getDuration() != null ? d.getDuration() : 0)
                    .sum();
        }
        return hearing.getEstimatedMinutes() != null ? hearing.getEstimatedMinutes() : 0;
    }

    static int calculateAggregatedDuration(final UpdateHearingForListing hearing) {
        if (isNotEmpty(hearing.getHearingDays())) {
            return hearing.getHearingDays().stream()
                    .mapToInt(d -> d.getDurationMinutes() != null ? d.getDurationMinutes() : 0)
                    .sum();
        }
        if (isNotEmpty(hearing.getNonDefaultDays())) {
            return hearing.getNonDefaultDays().stream()
                    .mapToInt(d -> d.getDuration() != null ? d.getDuration() : 0)
                    .sum();
        }
        return 0;
    }

    private static boolean isCrownFixedDateWithCourtScheduleId(final HearingListingNeeds hearing) {
        if (nonNull(hearing.getWeekCommencingDate())) {
            return false;
        }
        return !isEmpty(hearing.getHearingDays())
                && hearing.getHearingDays().stream().anyMatch(d -> nonNull(d.getCourtScheduleId()));
    }


    protected HearingSlotSearchResponse searchAndBookSlots(final String hearingId,
                                                           final String ouCode,
                                                           final String hearingSessionDate,
                                                           final String courtRoomId,
                                                           final String hearingSessionDateSearchCutOff,
                                                           final String hearingStartTime,
                                                           final Integer durationInMinutes,
                                                           final boolean isPolice) {
        LOGGER.info("searchAndBookSlots hearingId : {}, ouCode : {}, hearingSessionDate : {}, courtRoomId : {}, hearingSessionDateSearchCutOff : {}, hearingStartTime : {}, durationInMinutes : {}",
                hearingId, ouCode, hearingSessionDate, courtRoomId, hearingSessionDateSearchCutOff, hearingStartTime, durationInMinutes);

        final Map<String, String> queryParams = new HashMap<>();
        //mandatory params
        queryParams.put(HEARING_ID, hearingId);
        queryParams.put(COURT_CENTRE_ID, ouCode);
        queryParams.put(HEARING_DATE, hearingSessionDate);
        queryParams.put(IS_POLICE, String.valueOf(isPolice));
        queryParams.put(DURATION_MINUTES, String.valueOf(durationInMinutes));
        //optional params
        if (nonNull(courtRoomId)) queryParams.put(COURT_ROOM_ID, courtRoomId);
        if (nonNull(hearingSessionDateSearchCutOff))
            queryParams.put(HEARING_SESSION_DATE_CUT_OFF, hearingSessionDateSearchCutOff);
        if (nonNull(hearingStartTime) && !hearingStartTime.isEmpty())
            queryParams.put(HEARING_START_TIME, hearingStartTime);

        final Response searchAndBookResponse = hearingSlotsService.searchBookSlots(queryParams);

        if (HttpStatus.SC_OK == searchAndBookResponse.getStatus()) {
            final JsonObject responseJson = objectToJsonObjectConverter.convert(searchAndBookResponse.getEntity()).getJsonObject(HEARING_SLOTS);
            if (responseJson == null || responseJson.isEmpty()) {
                LOGGER.error("searchAndBookResponse from listingCourtScheduler returned an empty response for params : {} ", queryParams);
                return null;
            }
            final String bookedHearingId = responseJson.getString(HEARING_ID);
            final String bookedCourtScheduleId = responseJson.getString(COURT_SCHEDULE_ID);
            final String bookedCourtRoomId = responseJson.getString(COURT_ROOM_ID);
            final String bookedSessionStartTime = responseJson.getString(HEARING_START_TIME);
            final Integer duration = responseJson.getInt("duration");

            // Extract judiciaries if present
            List<JudicialRole> judiciaries = new ArrayList<>();
            if (responseJson.containsKey(JUDICIARIES)) {
                final JsonArray judiciariesArray = responseJson.getJsonArray(JUDICIARIES);
                if (judiciariesArray != null && !judiciariesArray.isEmpty()) {
                    for (int i = 0; i < judiciariesArray.size(); i++) {
                        JsonObject judicialRoleJson = judiciariesArray.getJsonObject(i);
                        JudicialRole judicialRole = buildJudicialRoleFromJson(judicialRoleJson);
                        judiciaries.add(judicialRole);
                    }
                }
            }

            return new HearingSlotSearchResponse(bookedHearingId, bookedCourtScheduleId, bookedCourtRoomId, bookedSessionStartTime, duration, judiciaries);
        }

        String responsePayload = "";
        if (searchAndBookResponse.hasEntity()) {
            responsePayload = searchAndBookResponse.getEntity().toString();
        }
        LOGGER.error("searchAndBookResponse from listingCourtScheduler returned an error : {} with status {}", responsePayload, searchAndBookResponse.getStatus());
        return null;
    }

    //This should be called only if you're sure you will get a session.(There's a UI validation)
    private HearingSlotSearchResponse getFirstAvailableSlot(final UpdateHearingForListing updateHearingForListing, final HearingDay hearingDay, final JsonEnvelope envelope, final boolean isMultiDay) {
        LOGGER.info("getFirstAvailableSlot for hearingDay: {}", hearingDay.getHearingDate());
        final Map<String, String> queryParams = new HashMap<>();
        if (isMultiDay){
            queryParams.put("courtSession", "AD");
            queryParams.put("isSlotBased", Boolean.FALSE.toString());
        } else {
            queryParams.put(HEARING_START_TIME, hearingDay.getStartTime().toString());
        }
        queryParams.put(COURT_ROOM_ID, hearingDay.getCourtRoomId().toString());
        queryParams.put("ouCode", getOrRetrieveOucode(updateHearingForListing, envelope));
        queryParams.put("sessionStartDate", hearingDay.getHearingDate().toString());
        queryParams.put("sessionEndDate", hearingDay.getHearingDate().toString());
        queryParams.put("panel", "ADULT,YOUTH");
        queryParams.put("showOverbookedSlots",Boolean.TRUE.toString());
        queryParams.put("pageNumber", "1");
        queryParams.put("pageSize", "1");

        final Response searchResponse = hearingSlotsService.search(queryParams);

        if (isSuccess(searchResponse)) {
            final JsonObject responseJson = objectToJsonObjectConverter.convert(searchResponse.getEntity());
            if (responseJson == null || responseJson.isEmpty()) {
                LOGGER.error("Search response returned empty for params: {}", queryParams);
                throw new IllegalStateException("No available slots found for the given criteria");
            }

            // Assuming the response has an array of slots, take the first one
            final JsonArray slotsArray = responseJson.getJsonArray(HEARING_SLOTS);
            if (slotsArray == null || slotsArray.isEmpty()) {
                LOGGER.error("No slots found in response for params: {}", queryParams);
                throw new IllegalStateException("No available slots found for the given criteria");
            }

            final JsonObject firstSlot = slotsArray.getJsonObject(0);
            final String courtScheduleId = firstSlot.getString(COURT_SCHEDULE_ID);
            final String courtRoomId = firstSlot.getString(COURT_ROOM_ID);
            final String sessionStartTime = firstSlot.getString("sessionStartTime");

            // Extract judiciaries if present
            List<JudicialRole> judiciaries = new ArrayList<>();
            if (responseJson.containsKey(JUDICIARIES)) {
                final JsonArray judiciariesArray = responseJson.getJsonArray(JUDICIARIES);
                if (judiciariesArray != null && !judiciariesArray.isEmpty()) {
                    for (int i = 0; i < judiciariesArray.size(); i++) {
                        JsonObject judicialRoleJson = judiciariesArray.getJsonObject(i);
                        JudicialRole judicialRole = buildJudicialRoleFromJson(judicialRoleJson);
                        judiciaries.add(judicialRole);
                    }
                }
            }

            return new HearingSlotSearchResponse(null, courtScheduleId, courtRoomId, sessionStartTime, hearingDay.getDurationMinutes(), judiciaries);
        } else {
            String responsePayload = "";
            if (searchResponse.hasEntity()) {
                responsePayload = searchResponse.getEntity().toString();
            }
            LOGGER.error("Search available slots failed with status: {} and response: {}", searchResponse.getStatus(), responsePayload);
            throw new IllegalStateException("Failed to search available slots");
        }
    }

    private String getOrRetrieveOucode(final UpdateHearingForListing updateHearingForListing, final JsonEnvelope envelope) {
        return nonNull(updateHearingForListing.getSelectedCourtCentre()) ? updateHearingForListing.getSelectedCourtCentre().getOuCode() : courtCentreFactory.getCourtCentre(updateHearingForListing.getCourtCentreId(), envelope).getOucode();
    }

    private HearingDay populateHearingDaysByHearingSlotSearch(final HearingDay hearingDay, final HearingSlotSearchResponse hearingSlotSearchResponse) {
        final ZonedDateTime startTime = nonNull(hearingDay.getStartTime()) ? hearingDay.getStartTime() : ZonedDateTime.parse(hearingSlotSearchResponse.sessionStartTime()).withZoneSameInstant(ZoneOffset.UTC);
        final Integer duration = hearingDay.getDurationMinutes();
        final ZonedDateTime endTime = startTime.plusMinutes(duration);

        return HearingDay.hearingDay()
                .withValuesFrom(hearingDay)
                .withCourtRoomId(fromString(hearingSlotSearchResponse.courtRoomId()))
                .withCourtScheduleId(fromString(hearingSlotSearchResponse.courtScheduleId()))
                .withStartTime(startTime)
                .withDurationMinutes(duration)
                .withEndTime(endTime)
                .build();
    }

    private List<HearingDay> combineSearchAndBookResponseAndListResponse(final Response response, final List<HearingDay> requestedHearingDays) {
        final List<HearingDay> newlyPopulatedHearingDays = new ArrayList<>();
        final List<ListUpdateHearing> listUpdateHearings = new ArrayList<>();
        if (!isSuccess(response)) {
            throw new RuntimeException("Cannot combine SearchAndBookResponseAndListResponse because search & list call failed with status %s ".formatted(response.getStatus()));
        }
        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());

        final JsonArray listUpdateHearingResponse = responseJson != null
                ? responseJson.getJsonArray("hearings")
                : null;
        if (isNotEmpty(listUpdateHearingResponse)) {
            for (int i = 0; i < listUpdateHearingResponse.size(); i++) {
                ListUpdateHearing listUpdateHearing = jsonObjectConverter.
                        convert(listUpdateHearingResponse.getJsonObject(i), ListUpdateHearing.class);
                listUpdateHearings.add(listUpdateHearing);
            }
        } else {
            LOGGER.error("listUpdateHearingResponse from listingCourtScheduler returned an invalid response Error : {}", responseJson);
        }
        /** for each record in requestedHearingDays
         try to find a match in listResults by courtscheduleid,
         if you have a missing courtscheduleId then log and throw an error
         otherwise populate HearingDay and add to newlyPopulatedHearingDays*/
        Map<String, ListUpdateHearing> listUpdateHearingMap = listUpdateHearings.stream()
                .collect(Collectors.toMap(
                        ListUpdateHearing::getCourtScheduleId,
                        hearing -> hearing
                ));

        // Check if all requested courtScheduleIds are present
        List<String> missingCourtScheduleIds = new ArrayList<>();
        for (HearingDay requestedHearingDay : requestedHearingDays) {
            String courtScheduleId = requestedHearingDay.getCourtScheduleId().toString();
            if (!listUpdateHearingMap.containsKey(courtScheduleId)) {
                missingCourtScheduleIds.add(courtScheduleId);
            }
        }

        // If any courtScheduleIds are missing, log and throw error with all missing IDs
        if (isNotEmpty(missingCourtScheduleIds)) {
            LOGGER.error("Missing courtScheduleIds in listUpdateHearings: {}", missingCourtScheduleIds);
            throw new IllegalStateException("Missing courtScheduleIds in listUpdateHearings: " + missingCourtScheduleIds);
        }

        // All courtScheduleIds are present, now populate the HearingDay objects
        for (HearingDay requestedHearingDay : requestedHearingDays) {
            String courtScheduleId = requestedHearingDay.getCourtScheduleId().toString();
            ListUpdateHearing matchingHearing = listUpdateHearingMap.get(courtScheduleId);
            final ZonedDateTime startTime = ZonedDateTime.parse(matchingHearing.getHearingStartTime()).withZoneSameInstant(ZoneOffset.UTC);

            newlyPopulatedHearingDays.add(HearingDay.hearingDay()
                    .withValuesFrom(requestedHearingDay)
                    .withCourtScheduleId(fromString(matchingHearing.getCourtScheduleId()))
                    .withStartTime(startTime)
                    .withDurationMinutes(matchingHearing.getDuration())
                    .withEndTime(startTime.plusMinutes(matchingHearing.getDuration()))
                    .build());
        }

        return newlyPopulatedHearingDays;
    }

    private static boolean isSuccess(final Response response) {
        return HttpStatus.SC_ACCEPTED == response.getStatus() || HttpStatus.SC_OK == response.getStatus();
    }

    static List<RotaSlot> populateBookedSlots(final List<RotaSlot> bookedSlots, final List<HearingDay> hearingDays) {
        List<RotaSlot> newlyPopulatedRotaSlots = new ArrayList<>();
        for (RotaSlot listUpdateHearing : bookedSlots) {
            for (HearingDay hearingDay : hearingDays) {
                if (listUpdateHearing.getCourtCentreId().equals(hearingDay.getCourtCentreId().toString()) &&
                        listUpdateHearing.getRoomId().equals(hearingDay.getCourtRoomId().toString()) &&
                        listUpdateHearing.getStartTime().isEqual(hearingDay.getStartTime()) &&
                        listUpdateHearing.getDuration().equals(hearingDay.getDurationMinutes())) {
                    newlyPopulatedRotaSlots.add(RotaSlot.rotaSlot()
                            .withValuesFrom(listUpdateHearing)
                            .withCourtScheduleId(hearingDay.getCourtScheduleId().toString())
                            .withStartTime(hearingDay.getStartTime())
                            .withDuration(hearingDay.getDurationMinutes())
                            .build());
                }
            }
        }
        return newlyPopulatedRotaSlots;
    }

    /**
     * Common method to list hearing sessions and extract enrichment data
     */
    private EnrichmentResult listHearingSessionsAndExtractData(final UUID hearingId, final List<HearingDay> hearingDays) {
        final JsonArray courtScheduleIds = slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(hearingDays);
        final JsonObject updateSlotsPayload = getUpdateSlotsPayload(hearingId, courtScheduleIds);
        final Response response = hearingSlotsService.listHearingInCourtSessions(updateSlotsPayload);

        final List<HearingDay> enrichedHearingDays = combineSearchAndBookResponseAndListResponse(response, hearingDays);
        final List<JudicialRole> enrichedJudiciaries = populateJudiciaryInfoFromSlots(response);

        return new EnrichmentResult(enrichedHearingDays, enrichedJudiciaries);
    }

    /**
     * Case 1: Handle hearings where all hearing days already have court schedule IDs
     */
    private EnrichmentResult handleDirectListingCase(final HearingListingNeeds hearing) {
        LOGGER.info("All hearingdays have courtScheduleId, so we can list them directly hearingId : {}, hearingDays : {}",
                hearing.getId(), hearing.getHearingDays());
        return listHearingSessionsAndExtractData(hearing.getId(), hearing.getHearingDays());
    }

    /**
     * Case 2: Handle hearings with provisional booking reference
     */
    private EnrichmentResult handleProvisionalBookingCase(final HearingListingNeeds hearing) {
        LOGGER.info("Hearing has booking reference, so we can list them directly hearingId : {}, bookingReference : {}",
                hearing.getId(), hearing.getBookingReference());

        final List<CourtSchedule> courtScheduleList = courtSchedulerService.getCourtSchedulesByProvisionalBookingId(hearing.getBookingReference().toString());
        final List<HearingDay> hearingDaysFromProvisionalBooking = generateHearingDaysFromCourtSchedule(hearing.getHearingDays(), courtScheduleList, hearing);

        return listHearingSessionsAndExtractData(hearing.getId(), hearingDaysFromProvisionalBooking);
    }

    /**
     * Case 3: Handle hearings with booked slots that have court schedule IDs
     */
    private EnrichmentResult handleBookedSlotsCase(final HearingListingNeeds hearing) {
        LOGGER.info("Hearing has booked slots with courtScheduleId, so we can list them directly hearingId : {}, bookedSlots : {}",
                hearing.getId(), hearing.getBookedSlots());
        // bookedSlots are converted to HearingDays on HearingDaysEnrichment
        return listHearingSessionsAndExtractData(hearing.getId(), hearing.getHearingDays());
    }

    private JudicialRole buildJudicialRoleFromJson(final JsonObject judicialRoleJson) {
        // Extract fields from JSON and map to domain model
        UUID judicialId = UUID.fromString(judicialRoleJson.getString("judiciaryId"));
        String judiciaryType = judicialRoleJson.getString("judiciaryType");
        boolean benchChairman = judicialRoleJson.getBoolean("benchChairman", false);
        boolean deputy = judicialRoleJson.getBoolean("deputy", false);

        // Create JudicialRoleType
        JudicialRoleType roleType = JudicialRoleType.judicialRoleType()
                .withJudiciaryType(judiciaryType)
                .build();

        // Build the JudicialRole
        return JudicialRole.judicialRole()
                .withJudicialId(judicialId)
                .withUserId(judicialId) // Using judiciaryId as userId as that's what we have
                .withJudicialRoleType(roleType)
                .withIsBenchChairman(Optional.of(benchChairman))
                .withIsDeputy(Optional.of(deputy))
                .build();
    }

    /**
     * Inner class to hold both hearing days and judiciaries from enrichment processing
     */
    private static class EnrichmentResult {
        private final List<HearingDay> hearingDays;
        private final List<JudicialRole> judiciaries;

        public EnrichmentResult(List<HearingDay> hearingDays, List<JudicialRole> judiciaries) {
            this.hearingDays = hearingDays != null ? hearingDays : new ArrayList<>();
            this.judiciaries = judiciaries != null ? judiciaries : new ArrayList<>();
        }

        public List<HearingDay> getHearingDays() {
            return hearingDays;
        }

        public List<JudicialRole> getJudiciaries() {
            return judiciaries;
        }
    }

    /**
     * Inner class to hold both hearing days and judiciaries from allocation candidate processing
     */
    private static class AllocationResult extends EnrichmentResult {
        public AllocationResult(List<HearingDay> hearingDays, List<JudicialRole> judiciaries) {
            super(hearingDays, judiciaries);
        }
    }

}
