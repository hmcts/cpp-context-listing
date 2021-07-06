package uk.gov.moj.cpp.listing.common.azure.adapter;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RotaSLServiceAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotaSLServiceAdapter.class);

    @Inject
    private HearingSlotsService hearingSlotsService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public List<JudicialRole> getJudicialRoles(final String startDate,
                                               final String ouCode,
                                               final Optional<String> courtSessionOptional,
                                               final String courtRoomId) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("sessionStartDate", startDate);
        queryParams.put("sessionEndDate", startDate);
        queryParams.put("ouCode", ouCode);
        queryParams.put("pageSize", "1");
        queryParams.put("pageNumber", "1");
        queryParams.put("courtRoomId", courtRoomId);
        queryParams.put("panel", "ADULT,YOUTH");
        courtSessionOptional.ifPresent(courtSession -> {
            if (!StringUtils.equalsIgnoreCase(courtSession, "AD")) {
                courtSession += ",AD";
            }
            queryParams.put("courtSession", courtSession);
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

    public Response getHearingSlotResponse(final String startDate, final String endDate,final String ouCode,final String courtRoomId) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("sessionStartDate", startDate);
        queryParams.put("sessionEndDate", endDate);
        queryParams.put("ouCode", ouCode);
        queryParams.put("pageSize", "1");
        queryParams.put("pageNumber", "1");
        queryParams.put("courtRoomId", courtRoomId);
        queryParams.put("panel", "ADULT,YOUTH");

        return hearingSlotsSearch(queryParams);
    }

    private Response hearingSlotsSearch(final Map<String, String> queryParams) {
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
}
