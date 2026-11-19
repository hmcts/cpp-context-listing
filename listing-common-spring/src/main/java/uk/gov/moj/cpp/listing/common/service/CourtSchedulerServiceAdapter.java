package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Court-scheduler adapter (copied from listing-common). P2.1 carries the query-side methods only
 * (hearing slots, session availability, draft status, hearing ids for range search); the
 * command-side methods (judicial roles, panel info, crown fallback, move to past date, change
 * court room for multi-day hearings) are ported with the commands in P2.3. Behaviour is unchanged;
 * JAX-RS {@code Response} is replaced by {@link HttpResult}.
 */
@SuppressWarnings({"squid:S1192"})
@Component
public class CourtSchedulerServiceAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulerServiceAdapter.class);
    public static final String SESSION_START_DATE = "sessionStartDate";
    public static final String SESSION_END_DATE = "sessionEndDate";
    public static final String EXACT_HEARING_START_DATETIME = "exactHearingStartDateTime";
    public static final String OU_CODE = "ouCode";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String COURT_ROOM_ID = "courtRoomId";
    public static final String HEARING_SLOTS = "hearingSlots";
    public static final String COURT_SESSION = "courtSession";
    public static final String BUSINESS_TYPE = "businessType";
    public static final String HEARING_IDS = "hearingIds";
    public static final String JURISDICTION = "jurisdiction";
    public static final String STATUS = "status";
    public static final String PANEL_ADULT_YOUTH = "ADULT,YOUTH";
    private static final String PANEL = "panel";
    public static final String HEARING_ID = "hearingId";
    // Crown fallback wire-field constants (used by crownFallbackSearchAndBook + parseCrownFallbackResult)
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String HEARING_DATE = "hearingDate";
    private static final String DURATION_IN_MINUTES = "durationInMinutes";
    private static final String SOURCE = "source";
    private static final String EARLIEST_HEARING_TIME = "earliestHearingTime";
    // SPRDT-1283: optional centre metadata so courtscheduler can auto-create a session at a centre
    // that has no session at all to copy metadata from (never-seeded centre). ouCode reuses the
    // OU_CODE constant declared above.
    private static final String COURT_CENTRE_NAME = "courtCentreName";
    private static final String COURT_ROOM_NAME = "courtRoomName";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String ALLOCATED_SCHEDULES = "allocatedSchedules";
    private static final String SESSION_DATE = "sessionDate";
    private static final String SESSION_START_TIME = "sessionStartTime";
    private static final String SESSION_END_TIME = "sessionEndTime";
    private static final String IS_DRAFT = "isDraft";
    private static final String DRAFT = "draft";
    private static final String OVERBOOKED = "overbooked";
    private static final String ANY_DRAFT = "anyDraft";
    // move-hearing-to-past-date wire-field constants; JURISDICTION/COURT_ROOM_ID reuse the fields
    // declared above. startTime/endTime (UTC instants) replace the old date-only startDate/endDate
    // fields, aligning the outbound contract with main (see the review artifact this reconciles).
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String SESSIONS = "sessions";
    private static final String COURT_HOUSE_ID = "courtHouseId";
    private static final String MAGISTRATES_JURISDICTION = "MAGISTRATES";
    // no-session normalisation (422 NO_SESSION_FOUND) constants
    public static final String NO_SESSION_FOUND = "NO_SESSION_FOUND";
    private static final String NO_AVAILABILITY = "NO_AVAILABILITY";
    // Fixed user-facing copy (ported from main) replacing whatever diagnostic message courtscheduler
    // itself supplies, on both the legacy-404 normalisation and a genuine 422 NO_SESSION_FOUND passthrough.
    public static final String NO_SESSION_FOUND_MESSAGE =
            "No suitable sessions are available for the selected date. Please select another date.";
    private static final String ERROR_CODE = "errorCode";
    private static final String MESSAGE = "message";
    private final HearingSlotsService hearingSlotsService;
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    public CourtSchedulerServiceAdapter(final HearingSlotsService hearingSlotsService) {
        this.hearingSlotsService = hearingSlotsService;
    }

    public HttpResult hearingSlotsSearch(final Map<String, String> queryParams) {
        final HttpResult hearingSlotResponse = hearingSlotsService.search(queryParams);

        if (hearingSlotResponse.isOk()) {
            return hearingSlotResponse;
        }

        String responsePayload = "";
        if (hearingSlotResponse.hasEntity()) {
            responsePayload = hearingSlotResponse.entity().toString();
        }
        LOGGER.error("hearingSlotsSearch from rota returned an error : {} with status {}", responsePayload, hearingSlotResponse.status());
        return hearingSlotResponse;
    }

    public HttpResult validateSessionAvailability(final JsonObject requestPayload) {
        final HttpResult response = hearingSlotsService.validateSessionAvailability(requestPayload);

        if (response.isOk()) {
            return response;
        }

        String responsePayload = "";
        if (response.hasEntity()) {
            responsePayload = response.entity().toString();
        }
        LOGGER.error("validateSessionAvailability from courtscheduler returned an error : {} with status {}", responsePayload, response.status());
        return response;
    }


    /**
     * Reports whether any of the supplied courtScheduleIds resolves to a DRAFT
     * (unallocated) court-schedule session. Used by cpp-context-progression to
     * decide whether to strip courtroom info from outgoing unallocated CROWN
     * hearings before they are persisted or surfaced in notifications.
     *
     * <p>Fails-safe by reporting anyDraft=true when the downstream courtscheduler
     * call fails - leaking a phantom courtroom is worse than dropping room info
     * for what may actually be a confirmed-allocated hearing.
     *
     * @param requestPayload JSON object with a courtScheduleIdList array
     * @return HttpResult with body {"anyDraft": true|false}
     */
    public JsonObject getCourtScheduleDraftStatus(final JsonObject requestPayload) {
        final List<String> courtScheduleIds = extractCourtScheduleIds(requestPayload);
        if (courtScheduleIds.isEmpty()) {
            return jakarta.json.Json.createObjectBuilder().add(ANY_DRAFT, false).build();
        }

        final Map<String, String> params = new HashMap<>();
        params.put("ids", String.join(",", courtScheduleIds));

        final HttpResult response;
        try {
            response = hearingSlotsService.getCourtSchedulesById(params);
        } catch (Exception ex) {
            LOGGER.warn("courtscheduler getCourtSchedulesById threw {} for {} ids - failing-safe by returning anyDraft=true",
                    ex.getClass().getSimpleName(), courtScheduleIds.size());
            return jakarta.json.Json.createObjectBuilder().add(ANY_DRAFT, true).build();
        }

        if (response == null || !response.isOk()) {
            LOGGER.warn("courtscheduler getCourtSchedulesById returned status {} for {} ids - failing-safe by returning anyDraft=true",
                    response == null ? "null" : response.status(), courtScheduleIds.size());
            return jakarta.json.Json.createObjectBuilder().add(ANY_DRAFT, true).build();
        }

        final boolean anyDraft = scanForDraftSession(response.jsonEntity());
        return jakarta.json.Json.createObjectBuilder().add(ANY_DRAFT, anyDraft).build();
    }

    private static List<String> extractCourtScheduleIds(final JsonObject requestPayload) {
        if (requestPayload == null || !requestPayload.containsKey("courtScheduleIdList")) {
            return Collections.emptyList();
        }
        // Wire shape: { "courtScheduleIdList": ["uuid", "uuid", ...] } - flat array of UUID
        // strings. The caller is progression (or any inter-context system caller); we don't
        // accept the object-wrapped {"courtScheduleId": "..."} form because every entry IS a
        // UUID and the wrapper added nothing but ceremony.
        final JsonArray list = requestPayload.getJsonArray("courtScheduleIdList");
        final List<String> ids = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.isNull(i)) {
                continue;
            }
            final String id = list.getString(i, null);
            if (StringUtils.isNotBlank(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static boolean scanForDraftSession(final JsonObject responseJson) {
        if (responseJson == null || responseJson.isEmpty() || !responseJson.containsKey("courtSchedules")) {
            return false;
        }
        // The /courtschedule/search.court-schedules-by-id wire response is FLAT: a top-level
        // courtSchedules array whose every element is one CourtSchedule (a session).
        // Each array element is a single CourtSchedule. The schema/example file in
        // courtscheduler-api shows a misleading nested "sessions" array copied from a
        // different endpoint. The actual implementation serialises List<CourtSchedule> flat
        // via ListToJsonArrayConverter -> ObjectMapper.writeValueAsString.
        //
        // The boolean draft field appears in the wire JSON under two possible names depending
        // on how Jackson resolves the CourtSchedule pair `isDraft()` getter + `setIsDraft(...)`
        // setter:
        //
        //   - getter `isDraft()` for a boolean -> Jackson conventionally exposes it as
        //     property "draft" (the "is" prefix is stripped for boolean getters)
        //   - setter `setIsDraft(...)`         -> Jackson exposes it as property "isDraft"
        //
        // The framework's ObjectMapperProducer doesn't configure either USE_GETTERS_AS_SETTERS
        // or USE_STD_BEAN_NAMING explicitly, so we cover both names here. This means a future
        // change to either getter or setter naming does not silently break the strip.
        final JsonArray schedules = responseJson.getJsonArray("courtSchedules");
        for (int i = 0; i < schedules.size(); i++) {
            final JsonObject schedule = schedules.getJsonObject(i);
            if (isTrue(schedule, IS_DRAFT) || isTrue(schedule, DRAFT)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTrue(final JsonObject obj, final String key) {
        return obj.containsKey(key) && !obj.isNull(key) && obj.getBoolean(key);
    }

    public HearingIdsResponse getCourtSchedulerHearings(final String ouCode,
                                                        final Optional<String> courtSessionOptional,
                                                        final String courtRoomId, final String startDate,
                                                        final String endDate, final Optional<Instant> exactHearingStartDateTime,
                                                        final Optional<String> businessTypeOptional,
                                                        final Optional<String> jurisdiction,
                                                        final String status,
                                                        final String panel, final Integer pageSize, final Integer pageNumber
    ) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(PANEL, panel);
        queryParams.put(SESSION_START_DATE, startDate);
        queryParams.put(SESSION_END_DATE, endDate);
        queryParams.put(OU_CODE, ouCode);
        queryParams.put(PAGE_SIZE, pageSize.toString());
        queryParams.put(PAGE_NUMBER, pageNumber.toString());
        queryParams.put(COURT_ROOM_ID, courtRoomId);
        courtSessionOptional.ifPresent(courtSession -> queryParams.put(COURT_SESSION, courtSession));
        businessTypeOptional.ifPresent(businessType -> queryParams.put(BUSINESS_TYPE, businessType));
        jurisdiction.ifPresent(j -> queryParams.put(JURISDICTION, j));
        exactHearingStartDateTime.ifPresent(s -> queryParams.put(EXACT_HEARING_START_DATETIME, s.toString()));
        // Draft (unallocated) court-schedule sessions are excluded at source when status="FINAL" is
        // supplied; a null status leaves the courtscheduler result unfiltered (returns ALL).
        if (status != null) {
            queryParams.put(STATUS, status);
        }

        final HttpResult hearingsResponse = getCourtSchedulerHearingIds(queryParams);

        return getHearingIds(hearingsResponse);
    }

    HttpResult getCourtSchedulerHearingIds(final Map<String, String> queryParams) {
        final HttpResult courtSchedulerHearingResponse = hearingSlotsService.getCourtSchedulerHearingIds(queryParams);

        if (courtSchedulerHearingResponse.isOk()) {
            return courtSchedulerHearingResponse;
        }

        String responsePayload = "";
        if (courtSchedulerHearingResponse.hasEntity()) {
            responsePayload = courtSchedulerHearingResponse.entity().toString();
        }
        LOGGER.error("courtSchedulerHearingResponse from rota returned an error : {} with status {}", responsePayload, courtSchedulerHearingResponse.status());
        return courtSchedulerHearingResponse;
    }

    HearingIdsResponse getHearingIds(final HttpResult response) {
        final int status = response.status();
        if (200 != status) {
            throw new HearingQueryException(format("Court scheduler hearing id search failed with status %d", status));
        }

        final JsonObject responseJson = response.jsonEntity();
        if (responseJson == null || !responseJson.containsKey(HEARING_IDS) || responseJson.isNull(HEARING_IDS)) {
            throw new HearingQueryException("Court scheduler hearing id search returned no hearingIds");
        }

        final List<IdResponse> uuids = new ArrayList<>();
        final JsonArray li = responseJson.getJsonArray(HEARING_IDS);
        for (int i = 0; i < li.size(); i++) {
            uuids.add(jsonObjectConverter.convert(li.getJsonObject(i), IdResponse.class));
        }

        final int results = intOrDefault(responseJson, "results", uuids.size());
        final int pageCount = intOrDefault(responseJson, "pageCount", 1);

        return new HearingIdsResponse(uuids, results, pageCount);
    }

    private static int intOrDefault(final JsonObject json, final String key, final int fallback) {
        return json.containsKey(key) && !json.isNull(key) ? json.getInt(key) : fallback;
    }

}
