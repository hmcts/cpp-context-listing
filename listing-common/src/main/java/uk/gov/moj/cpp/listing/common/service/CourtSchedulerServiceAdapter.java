package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;
import static java.util.Optional.empty;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackInvalidRequestException;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackNoSessionException;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackResult;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackSource;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;

import java.time.Instant;
import java.time.LocalDate;
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
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
    public static final String JURISDICTION = "jurisdiction";
    public static final String PANEL_ADULT_YOUTH = "ADULT,YOUTH";
    private static final String PANEL = "panel";
    public static final String HEARING_ID = "hearingId";
    // Crown fallback wire-field constants (used by crownFallbackSearchAndBook + parseCrownFallbackResult)
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String HEARING_DATE = "hearingDate";
    private static final String DURATION_IN_MINUTES = "durationInMinutes";
    private static final String SOURCE = "source";
    private static final String EARLIEST_HEARING_TIME = "earliestHearingTime";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String SESSION_DATE = "sessionDate";
    private static final String SESSION_START_TIME = "sessionStartTime";
    private static final String SESSION_END_TIME = "sessionEndTime";
    private static final String IS_DRAFT = "isDraft";
    private static final String DRAFT = "draft";
    private static final String OVERBOOKED = "overbooked";
    private static final String ANY_DRAFT = "anyDraft";
    @Inject
    private HearingSlotsService hearingSlotsService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    public List<JudicialRole> getJudicialRoles(final String startDate,
                                               final String ouCode,
                                               final Optional<String> courtSessionOptional,
                                               final String courtRoomId) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(SESSION_START_DATE, startDate);
        queryParams.put(SESSION_END_DATE, startDate);
        queryParams.put(OU_CODE, ouCode);
        queryParams.put(PAGE_SIZE, "1");
        queryParams.put(PAGE_NUMBER, "1");
        queryParams.put(COURT_ROOM_ID, courtRoomId);
        queryParams.put(PANEL, PANEL_ADULT_YOUTH);
        courtSessionOptional.ifPresent(courtSession -> {
            if (!StringUtils.equalsIgnoreCase(courtSession, "AD")) {
                courtSession += ",AD";
            }
            queryParams.put(COURT_SESSION, courtSession);
        });

        final Response hearingSlotResponse = hearingSlotsSearch(queryParams);

        if (HttpStatus.SC_OK == hearingSlotResponse.getStatus()) {
            return getJudiciariesFromRota(hearingSlotResponse);
        }
        return Collections.emptyList();
    }

    private List<JudicialRole> getJudiciariesFromRota(final Response response) {
        final List<JudicialRole> judiciaries = new ArrayList<>();
        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());

        responseJson.getJsonArray(HEARING_SLOTS)
                .stream()
                .map(JsonObject.class::cast)
                .forEach(hearingSlotJsonObject -> {
                    final JsonArray judiciariesJsonArray = hearingSlotJsonObject.getJsonArray("judiciaries");
                    judiciariesJsonArray.stream()
                            .map(JsonObject.class::cast)
                            .forEach(rotaSlJudiciaryJsonObject ->
                                    judiciaries.add(new JudicialRole.Builder()
                                            .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                                                    .withJudiciaryType("MAGISTRATE")
                                                    .build())
                                            .withJudicialId(UUID.fromString(rotaSlJudiciaryJsonObject.getString("judiciaryId")))
                                            .withIsDeputy(Optional.of(rotaSlJudiciaryJsonObject.getBoolean("deputy")))
                                            .withIsBenchChairman(Optional.of(rotaSlJudiciaryJsonObject.getBoolean("benchChairman")))
                                            .build())
                            );
                });

        return judiciaries;
    }

    public Response getHearingSlotResponse(final String startDate, final String endDate, final String ouCode, final String courtRoomId) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(SESSION_START_DATE, startDate);
        queryParams.put(SESSION_END_DATE, endDate);
        queryParams.put(OU_CODE, ouCode);
        queryParams.put(PAGE_SIZE, "1");
        queryParams.put(PAGE_NUMBER, "1");
        queryParams.put(COURT_ROOM_ID, courtRoomId);
        queryParams.put(PANEL, PANEL_ADULT_YOUTH);

        return hearingSlotsSearch(queryParams);
    }

    public Optional<String> getPanelInfo(final Optional<String> panelInfoFromPayload, final LocalDate startDate, final LocalDate endDate, final UUID courtRoomId, final String ouCode) {
        return panelInfoFromPayload.isPresent() ? panelInfoFromPayload : getCourtPanelFromRota(startDate, endDate, courtRoomId, ouCode);
    }

    private Optional<String> getCourtPanelFromRota(final LocalDate startDate, final LocalDate endDate, final UUID courtRoomId, final String ouCode) {
        Optional<String> panelOptional = empty();
        final Response hearingSlotResponse = getHearingSlotResponse(startDate.toString(), endDate.toString(), ouCode, courtRoomId.toString());
        if (HttpStatus.SC_OK != hearingSlotResponse.getStatus()) {
            final String errorMessage = format("getHearingSlotResponse from rota returned an error : {%s} with status {%s}",
                    (hearingSlotResponse.hasEntity() ? hearingSlotResponse.getEntity().toString() : ""), hearingSlotResponse.getStatus());

            LOGGER.warn(errorMessage);
        } else {
            final JsonObject responseJson = objectToJsonObjectConverter.convert(hearingSlotResponse.getEntity());
            final List<JsonObject> hearingSlots = responseJson.getJsonArray(HEARING_SLOTS)
                    .stream()
                    .map(JsonObject.class::cast)
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(hearingSlots)) {
                panelOptional = Optional.of(hearingSlots.get(0).getString(PANEL));
            } else {
                final String errorMessage = format("No hearingSlots found from rota with given filter, startDate {%s}, endDate {%s}, courtRoomId {%s}, ouCode {%s},",
                        startDate, endDate, courtRoomId, ouCode);

                LOGGER.warn(errorMessage);
            }
        }

        return panelOptional;
    }

    public Response hearingSlotsSearch(final Map<String, String> queryParams) {
        final Response hearingSlotResponse = hearingSlotsService.search(queryParams);

        if (HttpStatus.SC_OK == hearingSlotResponse.getStatus()) {
            return hearingSlotResponse;
        }

        String responsePayload = "";
        if (hearingSlotResponse.hasEntity()) {
            responsePayload = hearingSlotResponse.getEntity().toString();
        }
        LOGGER.error("hearingSlotsSearch from rota returned an error : {} with status {}", responsePayload, hearingSlotResponse.getStatus());
        return hearingSlotResponse;
    }

    /**
     * Calls the courtscheduler Crown fallback search-and-book endpoint. Returns the booked session
     * parsed into a {@link CrownFallbackResult}. On 404 (no session available), throws
     * {@link CrownFallbackNoSessionException}. On 400 (invalid request, typically multi-day with
     * duration > 360), throws {@link CrownFallbackInvalidRequestException}.
     *
     * Uses courtCentreId (UUID) as the single canonical court identifier — the courtscheduler
     * filters on court_schedule.court_house_id directly, so no ouCode lookup is needed.
     *
     * @param hearingId           listing hearingId
     * @param courtCentreId       court centre UUID (hard match on court_schedule.court_house_id)
     * @param hearingDate         hearing date (hard match)
     * @param durationInMinutes   hearing duration (max 360 — multi-day rejected)
     * @param courtRoomId         optional court room UUID; if supplied, courtscheduler prefers non-draft at that room
     * @param earliestHearingTime optional earliest start time (ISO-8601); courtscheduler honors it if provided
     * @param source              which listing flow initiated this call
     */
    public CrownFallbackResult crownFallbackSearchAndBook(final UUID hearingId,
                                                           final UUID courtCentreId,
                                                           final LocalDate hearingDate,
                                                           final int durationInMinutes,
                                                           final Optional<UUID> courtRoomId,
                                                           final Optional<String> earliestHearingTime,
                                                           final CrownFallbackSource source) {
        final Map<String, String> params = new HashMap<>();
        params.put(HEARING_ID, hearingId.toString());
        params.put(COURT_CENTRE_ID, courtCentreId.toString());
        params.put(HEARING_DATE, hearingDate.toString());
        params.put(DURATION_IN_MINUTES, Integer.toString(durationInMinutes));
        params.put(SOURCE, source.label());
        courtRoomId.ifPresent(id -> params.put(COURT_ROOM_ID, id.toString()));
        earliestHearingTime.ifPresent(t -> params.put(EARLIEST_HEARING_TIME, t));

        final Response response = hearingSlotsService.crownFallbackSearchAndBook(params);
        final int status = response.getStatus();

        if (status == HttpStatus.SC_NOT_FOUND) {
            throw new CrownFallbackNoSessionException(
                    "Crown fallback: no session at courtCentreId=" + courtCentreId + " on " + hearingDate
                            + " (hearingId=" + hearingId + ")");
        }
        if (status == HttpStatus.SC_BAD_REQUEST) {
            throw new CrownFallbackInvalidRequestException(
                    "Crown fallback rejected by courtscheduler (status=" + status + ") for hearingId=" + hearingId
                            + ": " + (response.hasEntity() ? response.getEntity().toString() : ""));
        }
        if (status != HttpStatus.SC_OK) {
            LOGGER.error("Crown fallback search-and-book returned unexpected status {} for hearingId {}", status, hearingId);
            throw new CrownFallbackNoSessionException(
                    "Crown fallback: unexpected courtscheduler status " + status + " for hearingId " + hearingId);
        }

        final JsonObject body = objectToJsonObjectConverter.convert(response.getEntity());
        return parseCrownFallbackResult(body);
    }

    private static CrownFallbackResult parseCrownFallbackResult(final JsonObject body) {
        return new CrownFallbackResult(
                uuidOrNull(body, HEARING_ID),
                uuidOrNull(body, COURT_SCHEDULE_ID),
                intOrNull(body, COURT_ROOM_ID),
                localDateOrNull(body, SESSION_DATE),
                zonedDateTimeOrNull(body, SESSION_START_TIME),
                zonedDateTimeOrNull(body, SESSION_END_TIME),
                intOrNull(body, DURATION_IN_MINUTES),
                booleanOrNull(body, IS_DRAFT),
                stringOrNull(body, BUSINESS_TYPE),
                stringOrNull(body, SOURCE),
                booleanOrNull(body, OVERBOOKED)
        );
    }

    private static boolean hasValue(final JsonObject body, final String key) {
        return body.containsKey(key) && !body.isNull(key);
    }

    private static String stringOrNull(final JsonObject body, final String key) {
        return hasValue(body, key) ? body.getString(key) : null;
    }

    private static UUID uuidOrNull(final JsonObject body, final String key) {
        return hasValue(body, key) ? UUID.fromString(body.getString(key)) : null;
    }

    private static Integer intOrNull(final JsonObject body, final String key) {
        return hasValue(body, key) ? body.getInt(key) : null;
    }

    private static Boolean booleanOrNull(final JsonObject body, final String key) {
        return hasValue(body, key) ? body.getBoolean(key) : null;
    }

    private static LocalDate localDateOrNull(final JsonObject body, final String key) {
        return hasValue(body, key) ? LocalDate.parse(body.getString(key)) : null;
    }

    private static ZonedDateTime zonedDateTimeOrNull(final JsonObject body, final String key) {
        return hasValue(body, key) ? ZonedDateTime.parse(body.getString(key)) : null;
    }

    public Response validateSessionAvailability(final JsonObject requestPayload) {
        final Response response = hearingSlotsService.validateSessionAvailability(requestPayload);

        if (HttpStatus.SC_OK == response.getStatus()) {
            return response;
        }

        String responsePayload = "";
        if (response.hasEntity()) {
            responsePayload = response.getEntity().toString();
        }
        LOGGER.error("validateSessionAvailability from courtscheduler returned an error : {} with status {}", responsePayload, response.getStatus());
        return response;
    }

    public Response extendMultiDayHearing(final JsonObject requestPayload) {
        final Response response = hearingSlotsService.extendMultiDayHearing(requestPayload);

        if (HttpStatus.SC_OK == response.getStatus()) {
            return response;
        }

        String responsePayload = "";
        if (response.hasEntity()) {
            responsePayload = response.getEntity().toString();
        }
        LOGGER.error("extendMultiDayHearing from courtscheduler returned an error : {} with status {}", responsePayload, response.getStatus());
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
     * @return Response with body {"anyDraft": true|false}
     */
    public JsonObject getCourtScheduleDraftStatus(final JsonObject requestPayload) {
        final List<String> courtScheduleIds = extractCourtScheduleIds(requestPayload);
        if (courtScheduleIds.isEmpty()) {
            return javax.json.Json.createObjectBuilder().add(ANY_DRAFT, false).build();
        }

        final Map<String, String> params = new HashMap<>();
        params.put("ids", String.join(",", courtScheduleIds));

        final Response response;
        try {
            response = hearingSlotsService.getCourtSchedulesById(params);
        } catch (Exception ex) {
            LOGGER.warn("courtscheduler getCourtSchedulesById threw {} for {} ids - failing-safe by returning anyDraft=true",
                    ex.getClass().getSimpleName(), courtScheduleIds.size());
            return javax.json.Json.createObjectBuilder().add(ANY_DRAFT, true).build();
        }

        if (response == null || HttpStatus.SC_OK != response.getStatus()) {
            LOGGER.warn("courtscheduler getCourtSchedulesById returned status {} for {} ids - failing-safe by returning anyDraft=true",
                    response == null ? "null" : response.getStatus(), courtScheduleIds.size());
            return javax.json.Json.createObjectBuilder().add(ANY_DRAFT, true).build();
        }

        final boolean anyDraft = scanForDraftSession(objectToJsonObjectConverter.convert(response.getEntity()));
        return javax.json.Json.createObjectBuilder().add(ANY_DRAFT, anyDraft).build();
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
        // The /courtschedule/search.court-schedules-by-id wire response is FLAT:
        //   { "courtSchedules": [ {...one CourtSchedule (a session) per element...}, ... ] }
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

        final Response hearingsResponse = getCourtSchedulerHearingIds(queryParams);

        return getHearingIds(hearingsResponse);
    }

    Response getCourtSchedulerHearingIds(final Map<String, String> queryParams) {
        final Response courtSchedulerHearingResponse = hearingSlotsService.getCourtSchedulerHearingIds(queryParams);

        if (HttpStatus.SC_OK == courtSchedulerHearingResponse.getStatus()) {
            return courtSchedulerHearingResponse;
        }

        String responsePayload = "";
        if (courtSchedulerHearingResponse.hasEntity()) {
            responsePayload = courtSchedulerHearingResponse.getEntity().toString();
        }
        LOGGER.error("courtSchedulerHearingResponse from rota returned an error : {} with status {}", responsePayload, courtSchedulerHearingResponse.getStatus());
        return courtSchedulerHearingResponse;
    }

    HearingIdsResponse getHearingIds(final Response response) {
        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());

       List<IdResponse> uuids = new ArrayList<>();
        final JsonArray li = responseJson.getJsonArray("hearingIds");
        for (int i = 0; i < li.size(); i++) {
            IdResponse res = jsonObjectConverter.convert(li.getJsonObject(i), IdResponse.class);
            uuids.add(res);

        }
        final int results = responseJson.getInt("results");
        final int pageCount = responseJson.getInt("pageCount");

        return new HearingIdsResponse(uuids, results, pageCount);
    }
}
