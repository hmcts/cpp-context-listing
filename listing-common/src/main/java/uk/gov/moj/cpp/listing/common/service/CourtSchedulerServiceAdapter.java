package uk.gov.moj.cpp.listing.common.service;

import static java.lang.String.format;
import static java.util.Optional.empty;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;

import java.time.LocalDate;
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
import javax.json.JsonString;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CourtSchedulerServiceAdapter {

    public static final String SESSION_START_DATE = "sessionStartDate";
    public static final String SESSION_END_DATE = "sessionEndDate";
    public static final String OU_CODE = "ouCode";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String COURT_ROOM_ID = "courtRoomId";
    public static final String COURT_SESSION = "courtSession";
    public static final String BUSINESS_TYPE = "businessType";
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulerServiceAdapter.class);
    private static final String PANEL_ADULT_YOUTH = "ADULT,YOUTH";
    private static final String PANEL = "panel";
    @Inject
    private HearingSlotsService hearingSlotsService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

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

        responseJson.getJsonArray("hearingSlots")
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
            final List<JsonObject> hearingSlots = responseJson.getJsonArray("hearingSlots")
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

    public HearingIdsResponse getCourtSchedulerHearings(final String ouCode,
                                              final Optional<String> courtSessionOptional,
                                              final String courtRoomId, final String startDate,
                                              final String endDate, final Optional<String> businessTypeOptional,
                                              final Optional<String> panelOptional, final Integer pageSize, final Integer pageNumber
    ) {
        final Map<String, String> queryParams = new HashMap<>();
        final String panel = panelOptional.orElse(PANEL_ADULT_YOUTH);
        queryParams.put(PANEL, panel);
        queryParams.put(SESSION_START_DATE, startDate);
        queryParams.put(SESSION_END_DATE, endDate);
        queryParams.put(OU_CODE, ouCode);
        queryParams.put(PAGE_SIZE, pageSize.toString());
        queryParams.put(PAGE_NUMBER, pageNumber.toString());
        queryParams.put(COURT_ROOM_ID, courtRoomId);
        courtSessionOptional.ifPresent(courtSession -> queryParams.put(COURT_SESSION, courtSession));
        businessTypeOptional.ifPresent(businessType -> queryParams.put(BUSINESS_TYPE, businessType));

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

        final List<UUID> uuids = responseJson.getJsonArray("hearingIds").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(Collectors.toList());

        final int results = responseJson.getInt("results");
        final int pageCount = responseJson.getInt("pageCount");

        return new HearingIdsResponse(uuids, results, pageCount);
    }

}
