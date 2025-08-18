package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.listing.command.api.service.HearingDaysEnrichmentService.log;

import uk.gov.justice.core.courts.CourtCentre;
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
import uk.gov.moj.cpp.listing.domain.ListUpdateHearing;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class CourtScheduleEnrichmentService implements EnrichmentService {
    private static final String HEARING_SLOTS = "hearingSlots";
    private static final String COURT_SCHEDULE_IDS = "courtScheduleIds";
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
        //HearingDays courtscheduleId provided in payload, we can list them directly
        //HearingDaysPopulatedWithSearchAndBook courtscheduleId is not provided, we need to search and book
        List<HearingDay> hearingDaysWithCourScheduleId = new ArrayList<>();
        updateHearingForListing.getHearingDays().forEach(hearingDay -> {
            if (isNull(hearingDay.getCourtScheduleId())) {
                HearingSlotSearchResponse hearingSlotSearchResponse = getFirstAvailableSlot(updateHearingForListing, hearingDay, envelope);
                hearingDaysWithCourScheduleId.add(populateHearingDaysByHearingSlotSearch(hearingDay, hearingSlotSearchResponse));
            } else {
                hearingDaysWithCourScheduleId.add(hearingDay);
            }
        });
        //we've listed hearingDaysPopulatedWithSearchAndBook already, now we need to list hearingDaysWithCourScheduleId
        final JsonArray courtScheduleIds = slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(hearingDaysWithCourScheduleId);
        final JsonObject updateSlotsPayload = getUpdateSlotsPayload(updateHearingForListing.getHearingId(), courtScheduleIds);
        final Response response = hearingSlotsService.listHearingInCourtSessions(updateSlotsPayload);
        final List<HearingDay> enrichedHearingDays = combineSearchAndBookResponseAndListResponse(response, hearingDaysWithCourScheduleId);

        return UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(updateHearingForListing)
                .withHearingDays(enrichedHearingDays)
                .build();
    }

    public static boolean isCandidateForAllocation(final HearingListingNeeds hearing) {
        //This is derived from Hearing aggregate canAllocate()
        boolean hasValidStartDateTime = nonNull(hearing.getListedStartDateTime()) || nonNull(hearing.getEarliestStartDateTime());
        boolean hasAssignedCourtRoom = nonNull(hearing.getCourtCentre().getRoomId());
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
            List<HearingDay> enrichedHearingDays = new ArrayList<>();
            // Case 1: All nondefault days have courtScheduleId
            if (allHearingDaysHaveCourtScheduleId(hearing)) {
                LOGGER.info("All hearingdays have courtScheduleId, so we can list them directly hearingId : {}, hearingDays : {}", hearing.getId(), hearing.getHearingDays());
                //create Request for list court hearing
                final JsonArray courtScheduleIds = slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(hearing.getHearingDays());
                final JsonObject updateSlotsPayload = getUpdateSlotsPayload(hearing.getId(), courtScheduleIds);
                final Response response = hearingSlotsService.listHearingInCourtSessions(updateSlotsPayload);
                enrichedHearingDays = combineSearchAndBookResponseAndListResponse(response, hearing.getHearingDays());
            }
            // Case 2: Has booking reference (provisional booking)
            else if (nonNull(hearing.getBookingReference())) {
                LOGGER.info("Hearing has booking reference, so we can list them directly hearingId : {}, bookingReference : {}", hearing.getId(), hearing.getBookingReference());
                enrichedHearingDays = handleProvisionalBooking(hearing);
            }
            // Case 3: Has booked slots with courtScheduleId
            else if (hasBookedSlotsWithCourtScheduleId(hearing)) {
                LOGGER.info("Hearing has booked slots with courtScheduleId, so we can list them directly hearingId : {}, bookedSlots : {}", hearing.getId(), hearing.getBookedSlots());
                //bookedSlots are converted to HearingDays on HearingDaysEnrichment
                final JsonArray courtScheduleIds = slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(hearing.getHearingDays());
                final JsonObject updateSlotsPayload = getUpdateSlotsPayload(hearing.getId(), courtScheduleIds);
                final Response response = hearingSlotsService.listHearingInCourtSessions(updateSlotsPayload);
                enrichedHearingDays = combineSearchAndBookResponseAndListResponse(response, hearing.getHearingDays());
            }
            // Case 4: Is candidate for allocation
            else if (isCandidateForAllocation(hearing)) {
                LOGGER.info("Hearing is candidate for allocation, so we need to search and book hearingId : {}, hearingDays : {}", hearing.getId(), log(hearing.getHearingDays()));
                enrichedHearingDays = handleAllocationCandidate(hearing, envelope);
            }

            List<RotaSlot> newlyPopulatedRotaSlot = null;
            if (isNotEmpty(hearing.getBookedSlots())) {
                newlyPopulatedRotaSlot = populateBookedSlots(hearing.getBookedSlots(), enrichedHearingDays);
            }
            /**in case we land in a different courtroom then requested, this should be reflected to main CourtCentre Object
             will be removed with LPT-1090 along with LPT-1355*/
            final CourtCentre adjustedCourtCentre = CourtCentre.courtCentre().withValuesFrom(hearing.getCourtCentre())
                    .withRoomId(enrichedHearingDays.get(0).getCourtRoomId())
                    .build();

            return HearingListingNeeds.hearingListingNeeds()
                    .withValuesFrom(hearing)
                    .withCourtCentre(adjustedCourtCentre)
                    .withHearingDays(enrichedHearingDays)
                    .withBookedSlots(newlyPopulatedRotaSlot)
                    .build();
        }
        return hearing;
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

    private List<HearingDay> handleProvisionalBooking(HearingListingNeeds hearing) {
        List<CourtSchedule> courtScheduleList = courtSchedulerService.getCourtSchedulesByProvisionalBookingId(hearing.getBookingReference().toString());
        final List<HearingDay> hearingDaysFromProvisionalBooking = generateHearingDaysFromCourtSchedule(hearing.getHearingDays(), courtScheduleList, hearing);
        final JsonArray courtScheduleIds = slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(hearingDaysFromProvisionalBooking);
        final JsonObject updateSlotsPayload = getUpdateSlotsPayload(hearing.getId(), courtScheduleIds);
        final Response response = hearingSlotsService.listHearingInCourtSessions(updateSlotsPayload);
        return combineSearchAndBookResponseAndListResponse(response, hearingDaysFromProvisionalBooking);
    }

    private boolean hasBookedSlotsWithCourtScheduleId(HearingListingNeeds hearing) {
        return isNotEmpty(hearing.getBookedSlots()) &&
                hearing.getBookedSlots().stream()
                        .allMatch(slot -> !isBlank(slot.getCourtScheduleId()));
    }

    private List<HearingDay> handleAllocationCandidate(HearingListingNeeds hearing, JsonEnvelope envelope) {
        List<HearingDay> hearingDaysBySearchAndBook = new ArrayList<>();
        hearing.getHearingDays().forEach(hearingDay -> {
            if (isNull(hearingDay.getCourtScheduleId())) {
                boolean isPolice = isPolice(hearing, envelope);
                HearingSlotSearchResponse hearingSlotSearchResponse = searchAndBookSlots(
                        hearing.getId().toString(),
                        hearing.getCourtCentre().getId().toString(),
                        hearing.getListedStartDateTime().toLocalDate().toString(),
                        hearing.getCourtCentre().getRoomId().toString(),
                        hearing.getEndDate(),
                        DateAndTimeUtils.toIsoString(hearing.getListedStartDateTime()),
                        hearing.getEstimatedMinutes(),
                        isPolice
                );
                if (hearingSlotSearchResponse == null) {
                    //If you can't find by searchandBook add HearingDay as it is, it will be unallocated.
                    hearingDaysBySearchAndBook.add(hearingDay);
                } else {
                    hearingDaysBySearchAndBook.add(populateHearingDaysByHearingSlotSearch(hearingDay, hearingSlotSearchResponse));
                }
            }
        });
        return hearingDaysBySearchAndBook;
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

    static boolean needsCourtScheduleEnrichment(final HearingListingNeeds hearing) {
        return hearing.getJurisdictionType().equals(JurisdictionType.MAGISTRATES) && (!isEmpty(hearing.getNonDefaultDays()) || nonNull(hearing.getBookingReference()) || nonNull(hearing.getBookedSlots()) || isCandidateForAllocation(hearing));
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
            final String bookedCourtScheduleId = responseJson.getString("courtScheduleId");
            final String bookedCourtRoomId = responseJson.getString(COURT_ROOM_ID);
            final String bookedSessionStartTime = responseJson.getString(HEARING_START_TIME);
            final Integer duration = responseJson.getInt("duration");

            return new HearingSlotSearchResponse(bookedHearingId, bookedCourtScheduleId, bookedCourtRoomId, bookedSessionStartTime, duration);
        }

        String responsePayload = "";
        if (searchAndBookResponse.hasEntity()) {
            responsePayload = searchAndBookResponse.getEntity().toString();
        }
        LOGGER.error("searchAndBookResponse from listingCourtScheduler returned an error : {} with status {}", responsePayload, searchAndBookResponse.getStatus());
        return null;
    }

    //This should be called only if you're sure you will get a session.(There's a UI validation)
    private HearingSlotSearchResponse getFirstAvailableSlot(final UpdateHearingForListing updateHearingForListing, final HearingDay hearingDay, final JsonEnvelope envelope) {
        LOGGER.info("getFirstAvailableSlot for hearingDay: {}", hearingDay.getHearingDate());
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(COURT_ROOM_ID, hearingDay.getCourtRoomId().toString());
        queryParams.put("ouCode", getOrRetrieveOucode(updateHearingForListing, envelope));
        queryParams.put("sessionStartDate", hearingDay.getHearingDate().toString());
        queryParams.put("sessionEndDate", hearingDay.getHearingDate().toString());
        queryParams.put(HEARING_START_TIME, hearingDay.getStartTime().toString());
        queryParams.put("panel", "ADULT,YOUTH");
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
            final String courtScheduleId = firstSlot.getString("courtScheduleId");
            final String courtRoomId = firstSlot.getString(COURT_ROOM_ID);
            final String sessionStartTime = firstSlot.getString("sessionStartTime");

            return new HearingSlotSearchResponse(null, courtScheduleId, courtRoomId, sessionStartTime, hearingDay.getDurationMinutes());
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
        if (isSuccess(response)) {
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


}
