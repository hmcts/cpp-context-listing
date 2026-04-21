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
    public static final String PANEL_ADULT_YOUTH = "ADULT,YOUTH";
    private static final String PANEL = "panel";
    public static final String HEARING_ID = "hearingId";
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
        params.put("courtCentreId", courtCentreId.toString());
        params.put("hearingDate", hearingDate.toString());
        params.put("durationInMinutes", Integer.toString(durationInMinutes));
        params.put("source", source.label());
        courtRoomId.ifPresent(id -> params.put(COURT_ROOM_ID, id.toString()));
        earliestHearingTime.ifPresent(t -> params.put("earliestHearingTime", t));

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
                body.containsKey("hearingId") ? UUID.fromString(body.getString("hearingId")) : null,
                body.containsKey("courtScheduleId") ? UUID.fromString(body.getString("courtScheduleId")) : null,
                body.containsKey("courtRoomId") && !body.isNull("courtRoomId") ? body.getInt("courtRoomId") : null,
                body.containsKey("sessionDate") && !body.isNull("sessionDate") ? LocalDate.parse(body.getString("sessionDate")) : null,
                body.containsKey("sessionStartTime") && !body.isNull("sessionStartTime") ? ZonedDateTime.parse(body.getString("sessionStartTime")) : null,
                body.containsKey("sessionEndTime") && !body.isNull("sessionEndTime") ? ZonedDateTime.parse(body.getString("sessionEndTime")) : null,
                body.containsKey("durationInMinutes") && !body.isNull("durationInMinutes") ? body.getInt("durationInMinutes") : null,
                body.containsKey("isDraft") && !body.isNull("isDraft") ? body.getBoolean("isDraft") : null,
                body.containsKey("businessType") && !body.isNull("businessType") ? body.getString("businessType") : null,
                body.containsKey("source") && !body.isNull("source") ? body.getString("source") : null,
                body.containsKey("overbooked") && !body.isNull("overbooked") ? body.getBoolean("overbooked") : null
        );
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

    public HearingIdsResponse getCourtSchedulerHearings(final String ouCode,
                                                        final Optional<String> courtSessionOptional,
                                                        final String courtRoomId, final String startDate,
                                                        final String endDate, final Optional<Instant> exactHearingStartDateTime,
                                                        final Optional<String> businessTypeOptional,
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
