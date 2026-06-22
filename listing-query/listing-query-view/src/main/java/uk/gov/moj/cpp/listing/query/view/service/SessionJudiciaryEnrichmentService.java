package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.Objects.isNull;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressWarnings({"squid:S6813"})
public class SessionJudiciaryEnrichmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionJudiciaryEnrichmentService.class);

    static final String JUDICIARY_SOURCE_HEARING = "HEARING";
    static final String JUDICIARY_SOURCE_SESSION = "SESSION";

    private static final String JUDICIARY = "judiciary";
    private static final String JUDICIARY_SOURCE = "judiciarySource";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String COURT_SCHEDULES = "courtSchedules";
    private static final String SESSIONS = "sessions";
    private static final String COURT_SCHEDULE_IDS_PARAM = "courtScheduleIds";

    private static final String JUDICIARY_ID = "id";
    private static final String JUDICIAL_ID = "judicialId";
    private static final String JUDICIARY_TYPE = "judiciaryType";
    private static final String IS_BENCH_CHAIRMAN = "isBenchChairman";
    private static final String IS_DEPUTY = "isDeputy";
    private static final String SEQ_ID = "seqId";
    private static final String TITLE_JUDICIAL_PREFIX = "titleJudicialPrefix";
    private static final String TITLE_JUDICIAL_PREFIX_WELSH = "titleJudicialPrefixWelsh";
    private static final String PERSON_ID = "personId";
    private static final String SPECIALISMS = "specialisms";
    private static final String REQUESTED_NAME = "requestedName";
    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String SURNAME = "surname";
    private static final String FORENAMES = "forenames";
    private static final String EMAIL_ADDRESS = "emailAddress";

    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Inject
    HearingSlotsService hearingSlotsService;

    public void enrichWithSessionJudiciary(final List<Hearing> hearings) {
        if (hearings == null || hearings.isEmpty()) {
            return;
        }

        final List<Hearing> needsFallback = hearings.stream()
                .filter(h -> isJudiciaryEmpty(h.getProperties()))
                .toList();

        hearings.stream()
                .filter(h -> !isJudiciaryEmpty(h.getProperties()))
                .forEach(h -> setJudiciarySource(h.getProperties(), JUDICIARY_SOURCE_HEARING));

        if (needsFallback.isEmpty()) {
            return;
        }

        final Set<String> allCourtScheduleIds = new LinkedHashSet<>();
        for (final Hearing hearing : needsFallback) {
            allCourtScheduleIds.addAll(extractCourtScheduleIds(hearing.getProperties()));
        }

        if (allCourtScheduleIds.isEmpty()) {
            LOGGER.debug("No courtScheduleIds found in hearingDays for {} hearing(s) needing judiciary fallback", needsFallback.size());
            needsFallback.forEach(h -> setJudiciarySource(h.getProperties(), JUDICIARY_SOURCE_SESSION));
            return;
        }

        final Map<String, JsonArray> judiciaryByScheduleId = fetchJudiciaryByScheduleId(allCourtScheduleIds);

        for (final Hearing hearing : needsFallback) {
            final JsonArray judiciary = resolveJudiciaryForHearing(hearing.getProperties(), judiciaryByScheduleId);
            if (judiciary != null && !judiciary.isEmpty()) {
                injectJudiciary((ObjectNode) hearing.getProperties(), judiciary);
            }
            setJudiciarySource(hearing.getProperties(), JUDICIARY_SOURCE_SESSION);
        }
    }

    private boolean isJudiciaryEmpty(final JsonNode props) {
        if (isNull(props)) {
            return true;
        }
        final JsonNode judiciary = props.get(JUDICIARY);
        return isNull(judiciary) || !judiciary.isArray() || judiciary.isEmpty();
    }

    private List<String> extractCourtScheduleIds(final JsonNode props) {
        final List<String> ids = new ArrayList<>();
        if (isNull(props)) {
            return ids;
        }
        final JsonNode hearingDays = props.path(HEARING_DAYS);
        if (hearingDays.isMissingNode() || !hearingDays.isArray()) {
            return ids;
        }
        hearingDays.forEach(day -> {
            final JsonNode csId = day.get(COURT_SCHEDULE_ID);
            if (csId != null && !csId.isNull() && !csId.asText().isBlank()) {
                ids.add(csId.asText());
            }
        });
        return ids;
    }

    Map<String, JsonArray> fetchJudiciaryByScheduleId(final Set<String> courtScheduleIds) {
        final Response response = getCourtSchedulesResponse(courtScheduleIds);
        if (response == null || response.getStatus() != Response.Status.OK.getStatusCode()) {
            if (response != null) {
                LOGGER.warn("getCourtSchedulesById returned HTTP {} for courtScheduleIds: {}", response.getStatus(), courtScheduleIds);
            }
            return Map.of();
        }

        final JsonArray schedulesArray = getCourtSchedulesArray(response);
        if (schedulesArray == null || schedulesArray.isEmpty()) {
            return Map.of();
        }

        return extractJudiciaryByScheduleId(courtScheduleIds, schedulesArray);
    }

    private Map<String, JsonArray> extractJudiciaryByScheduleId(final Set<String> courtScheduleIds, final JsonArray schedulesArray) {
        final Map<String, JsonArray> result = new HashMap<>();
        for (int i = 0; i < schedulesArray.size(); i++) {
            final JsonObject courtRoom = schedulesArray.getJsonObject(i);
            final JsonArray sessions = courtRoom.getJsonArray(SESSIONS);
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }
            addSessionJudiciaries(courtScheduleIds, result, sessions);
        }
        return result;
    }

    private void addSessionJudiciaries(final Set<String> courtScheduleIds,
                                       final Map<String, JsonArray> result,
                                       final JsonArray sessions) {
        for (int j = 0; j < sessions.size(); j++) {
            final JsonObject session = sessions.getJsonObject(j);
            final String csId = session.getString(COURT_SCHEDULE_ID, null);
            if (csId == null || !courtScheduleIds.contains(csId)) {
                continue;
            }
            final JsonArray judiciaries = session.getJsonArray("judiciaries");
            if (judiciaries != null && !judiciaries.isEmpty()) {
                result.put(csId, judiciaries);
            }
        }
    }

    private Response getCourtSchedulesResponse(final Set<String> courtScheduleIds) {
        final Map<String, String> params = new HashMap<>();
        params.put(COURT_SCHEDULE_IDS_PARAM, String.join(",", courtScheduleIds));

        try {
            return hearingSlotsService.getCourtSchedulesById(params);
        } catch (final RuntimeException e) {
            LOGGER.warn("Could not call getCourtSchedulesById for judiciary fallback: {}", e.getMessage());
            return null;
        }
    }

    private JsonArray getCourtSchedulesArray(final Response response) {
        final JsonObject responseJson = (JsonObject) response.getEntity();
        if (responseJson == null || responseJson.isEmpty()) {
            return null;
        }
        return responseJson.getJsonArray(COURT_SCHEDULES);
    }

    private JsonArray resolveJudiciaryForHearing(final JsonNode props,
                                                 final Map<String, JsonArray> judiciaryByScheduleId) {
        final List<String> ids = extractCourtScheduleIds(props);
        final Map<String, JsonObject> byJudiciaryId = new HashMap<>();
        for (final String csId : ids) {
            final JsonArray judiciary = judiciaryByScheduleId.get(csId);
            if (judiciary == null) continue;
            for (int i = 0; i < judiciary.size(); i++) {
                final JsonObject j = judiciary.getJsonObject(i);
                final String judiciaryId = j.getString(JUDICIARY_ID, null);
                if (judiciaryId != null) {
                    byJudiciaryId.putIfAbsent(judiciaryId, j);
                }
            }
        }
        if (byJudiciaryId.isEmpty()) {
            return null;
        }
        final javax.json.JsonArrayBuilder arrayBuilder = javax.json.Json.createArrayBuilder();
        byJudiciaryId.values().forEach(arrayBuilder::add);
        return arrayBuilder.build();
    }

    private void injectJudiciary(final ObjectNode propertiesNode, final JsonArray courtSchedulerJudiciary) {
        final ArrayNode judiciaryArray = mapper.createArrayNode();
        for (int i = 0; i < courtSchedulerJudiciary.size(); i++) {
            final JsonObject j = courtSchedulerJudiciary.getJsonObject(i);
            final ObjectNode role = mapper.createObjectNode();

            role.put(JUDICIAL_ID, j.getString(JUDICIARY_ID, null));
            putStringIfPresent(j, role, JUDICIARY_TYPE);
            putBooleanIfPresent(j, role, IS_BENCH_CHAIRMAN);
            putBooleanIfPresent(j, role, IS_DEPUTY);
            putIntIfPresent(j, role, SEQ_ID);
            putStringIfPresent(j, role, TITLE_PREFIX);
            putStringIfPresent(j, role, TITLE_JUDICIAL_PREFIX);
            putStringIfPresent(j, role, TITLE_JUDICIAL_PREFIX_WELSH);
            putStringIfPresent(j, role, PERSON_ID);
            putStringIfPresent(j, role, REQUESTED_NAME);
            putStringIfPresent(j, role, SURNAME);
            putStringIfPresent(j, role, FORENAMES);
            putStringIfPresent(j, role, EMAIL_ADDRESS);
            if (j.containsKey(SPECIALISMS) && !j.isNull(SPECIALISMS)) {
                final ArrayNode specialismsNode = mapper.createArrayNode();
                j.getJsonArray(SPECIALISMS).forEach(s -> {
                    final String val = s.getValueType() == javax.json.JsonValue.ValueType.STRING
                            ? ((javax.json.JsonString) s).getString()
                            : s.toString();
                    specialismsNode.add(val);
                });
                role.set(SPECIALISMS, specialismsNode);
            }

            judiciaryArray.add(role);
        }
        propertiesNode.set(JUDICIARY, judiciaryArray);
    }

    private void putStringIfPresent(final JsonObject source, final ObjectNode target, final String key) {
        if (source.containsKey(key) && !source.isNull(key)) {
            target.put(key, source.getString(key));
        }
    }

    private void putBooleanIfPresent(final JsonObject source, final ObjectNode target, final String key) {
        if (source.containsKey(key) && !source.isNull(key)) {
            target.put(key, source.getBoolean(key));
        }
    }

    private void putIntIfPresent(final JsonObject source, final ObjectNode target, final String key) {
        if (source.containsKey(key) && !source.isNull(key)) {
            target.put(key, source.getInt(key));
        }
    }

    private void setJudiciarySource(final JsonNode props, final String source) {
        if (props != null) {
            ((ObjectNode) props).put(JUDICIARY_SOURCE, source);
        }
    }
}
