package uk.gov.moj.cpp.listing.query.view.hearing;

import static java.lang.String.format;
import static java.time.ZonedDateTime.parse;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.query.view.hearing.JsonArrayCollector.toArrayNode;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingJsonListConverterFilterEjectCases implements ListOfJsontoJsonArrayConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingJsonListConverterFilterEjectCases.class);

    private static final String LISTED_CASES = "listedCases";
    private static final String IS_EJECTED = "isEjected";
    private static final String JUDICIARY = "judiciary";
    private static final String HEARINGS = "hearings";
    private static final String HEARING_DAYS = "hearingDays";
    private static final JsonArray EMPTY_JSON_ARRAY = createArrayBuilder().build();
    public static final String HEARINGS_BY_COURT_CENTRE_ID = "hearingsByCourtCentreId";
    public static final String HEARINGS_BY_HEARING_DATE = "hearingsByHearingDate";
    public static final String HEARING = "hearing";
    public static final String COURT_APPLICATIONS = "courtApplications";

    private static final String ERROR_MESSAGE_FORMAT = "Resource %s unable to parse. Reason: %s";

    private static final String START_TIME = "startTime";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String SEARCH_DATE = "searchDate";
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Override
    public JsonArray convert(final List<Hearing> hearings) {
        return hearings.stream()
                .map(this::preprocessPropertiesIfHearingIsFlattened)
                .map(this::filterEjectCaseAndCourtApplications)
                .filter(this::casesOrApplicationsExists)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.toString()))
                .collect(toArrayNode());
    }

    private JsonNode preprocessPropertiesIfHearingIsFlattened(final Hearing h) {
        if (isNull(h) || isNull(h.getProperties())) {
            return null;
        }
        JsonNode props = h.getProperties();
        final LocalDate hearingDate = h.getHearingDate();
        Long hearingDayCount = h.getHearingDayCount();
        Long position = h.getHearingDayPosition();

        if (hearingDate == null || hearingDayCount == null || hearingDayCount <= 0) {
            // no flattening needed
            return props;
        }

        // from here on, we have a flattened hearing ... so, pre-process fields needed for rendering a flattened hearing
        ((ObjectNode) h.getProperties()).put("hearingDayCount", hearingDayCount);
        ((ObjectNode) h.getProperties()).put("hearingDayPosition", position);
        final JsonNode hd = props.findPath("hearingDays");

        if (!hd.isMissingNode() && hd.isArray()) {
            final ArrayNode arrayNode = (ArrayNode) hd;
            int idx = 0;
            while (idx < arrayNode.size()) {
                final JsonNode el = arrayNode.get(idx);
                final JsonNode hhd = el.findPath("hearingDate");
                if (!hhd.isMissingNode()) {
                    final String txt = hhd.asText();
                    if (!hearingDate.toString().equals(txt)) {
                        arrayNode.remove(idx);//infinite loop???
                        continue; // not incrementing index as array size is reduced by one
                    }
                    idx++;
                }
            }
        }

        return props;
    }

    @Override
    public JsonArray convertForSearchHearing(final List<Hearing> hearings, final Map<String, String> hearingDayMatchedCriteriaMap) {
        return hearings.stream()
                .map(Hearing::getProperties)
                .map(this::filterEjectCaseAndCourtApplications)
                .map(hearingJsonNode -> setMatchedHearingDay(hearingJsonNode, hearingDayMatchedCriteriaMap))
                .filter(this::casesOrApplicationsExists)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.toString()))
                .collect(toArrayNode());
    }

    @Override
    public JsonArray convertHearingResultForAlphabeticalList(final List<Hearing> hearings) {
        return hearings.stream()
                .map(this::deepCopyProperties)
                .map(this::filterEjectedCasesAndCourtApplicationsForAlphabeticalList)
                .map(this::removeHearingsWhenBothCasesAndApplicationsAreEmpty)
                .filter(Objects::nonNull)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.get(0).toString()))
                .collect(toArrayNode());
    }

    @Override
    public JsonArray convertHearingResultForPublicList(final Hearing hearing) {
        if (nonNull(hearing)) {
            final JsonObject publicHearingResult = this.jsonFromString(hearing.getProperties().toString());
            final JsonArray judiciary = publicHearingResult.isNull(JUDICIARY) ? EMPTY_JSON_ARRAY : publicHearingResult.getJsonArray(JUDICIARY);
            if (publicHearingResult.isNull(HEARINGS)) {
                return createArrayBuilder().build();
            }
            return publicHearingResult.getJsonArray(HEARINGS).getValuesAs(JsonObject.class).stream()
                    .filter(Objects::nonNull)
                    .map(hearingByCourtCentreId -> this.enrich(hearingByCourtCentreId, JUDICIARY, judiciary))
                    .collect(toArrayNode());
        }
        return createArrayBuilder().build();
    }

    private JsonNode setMatchedHearingDay(final JsonNode properties, final Map<String, String> hearingDayMatchedCriteriaMap) {
        final String startTimeCriteria = hearingDayMatchedCriteriaMap.get(START_TIME);
        final String searchDate = hearingDayMatchedCriteriaMap.get(SEARCH_DATE);
        final String courtCentreId = hearingDayMatchedCriteriaMap.get(COURT_CENTRE_ID);
        final String courtRoomId = hearingDayMatchedCriteriaMap.get(COURT_ROOM_ID);
        final LocalDateTime searchStartTime = LocalDateTime.parse(startTimeCriteria);

        if (!properties.findPath(HEARING_DAYS).isMissingNode()) {
            final ArrayNode hearingDays = (ArrayNode) properties.findPath(HEARING_DAYS);

            final List<HearingDay> hearingDayList = new ArrayList<>();
            final Map<HearingDay, JsonNode> hearingDayJsonNodeMap = new HashMap<>();
            hearingDays.forEach(
                    hearingDayJson -> {
                        final HearingDay hearingDay = createHearingDay(hearingDayJson);
                        hearingDayList.add(hearingDay);
                        hearingDayJsonNodeMap.put(hearingDay, hearingDayJson);
                    }
            );

            final List<HearingDay> filteredHearingDayList = hearingDayList.stream()
                    .filter(sortedHearingDay -> sortedHearingDay.getCourtCentreId().equals(courtCentreId))
                    .filter(sortedHearingDay -> sortedHearingDay.getCourtRoomId().equals(courtRoomId))
                    .filter(sortedHearingDay -> sortedHearingDay.getHearingDate().equals(LocalDate.parse(searchDate)))
                    .sorted(Comparator.comparing(HearingDay::getStartTime))
                    .collect(Collectors.toList());

            if (isNotEmpty(filteredHearingDayList)) {
                processMatchedWithQueryForHearingDay(searchStartTime, filteredHearingDayList);
            }

            hearingDayList.forEach(sortedHearingDay -> ((ObjectNode) hearingDayJsonNodeMap.get(sortedHearingDay)).put("matchedWithQuery", sortedHearingDay.isMatchedWithQuery()));
        }

        return properties;
    }

    private void processMatchedWithQueryForHearingDay(final LocalDateTime searchStartTime, final List<HearingDay> filteredHearingDayList) {
        final Pair<Boolean, Boolean> isAllOfHearingDaysAfterOrBeforeSearchStartTime = isAllOfHearingDaysAfterOrBeforeSearchStartTime(searchStartTime, filteredHearingDayList);
        final boolean isAllOfHearingDaysAfterSearchStartTime = isAllOfHearingDaysAfterOrBeforeSearchStartTime.getLeft().booleanValue();
        final boolean isAllOfHearingDaysBeforeSearchStartTime = isAllOfHearingDaysAfterOrBeforeSearchStartTime.getRight().booleanValue();
        if (isAllOfHearingDaysAfterSearchStartTime || isAllOfHearingDaysBeforeSearchStartTime) {
            // find the closest time so
            if (isAllOfHearingDaysAfterSearchStartTime) {
                filteredHearingDayList.get(0).setMatchedWithQuery(true);
            } else {
                filteredHearingDayList.get(filteredHearingDayList.size() - 1).setMatchedWithQuery(true);
            }
        } else {
            // find the first greater startTime than searchStartTime
            for (final HearingDay sortedHearingDay : filteredHearingDayList) {

                if (sortedHearingDay.getStartTime().toLocalDateTime().isAfter(searchStartTime) || sortedHearingDay.getStartTime().toLocalDateTime().isEqual(searchStartTime)) {
                    sortedHearingDay.setMatchedWithQuery(true);
                    break;
                }
            }
        }
    }

    private HearingDay createHearingDay(final JsonNode hearingDayJson) {
        return new HearingDay.Builder()
                .withHearingDate(LocalDate.parse(convertJsonNodeToString(hearingDayJson.path("hearingDate"))))
                .withCourtScheduleId(convertJsonNodeToString(hearingDayJson.path("courtScheduleId")))
                .withCourtRoomId(convertJsonNodeToString(hearingDayJson.path(COURT_ROOM_ID)))
                .withCourtCentreId(convertJsonNodeToString(hearingDayJson.path(COURT_CENTRE_ID)))
                .withStartTime(parse(convertJsonNodeToString(hearingDayJson.path(START_TIME))))
                .withEndTime(parse(convertJsonNodeToString(hearingDayJson.path("endTime"))))
                .build();
    }

    private String convertJsonNodeToString(JsonNode jsonNode) {
        if (jsonNode.isMissingNode()) {
            return StringUtils.EMPTY;
        }
        return jsonNode.asText();
    }

    private Pair<Boolean, Boolean> isAllOfHearingDaysAfterOrBeforeSearchStartTime(final LocalDateTime searchStartTime, final List<HearingDay> sortedHearingDayList) {
        final LocalDateTime earliestHearingDayStartTime = sortedHearingDayList.get(0).getStartTime().toLocalDateTime();
        final LocalDateTime latestHearingDayStartTime = sortedHearingDayList.get(sortedHearingDayList.size() - 1).getStartTime().toLocalDateTime();

        final Boolean allHearingStartTimeAfterSearchStartTime = Boolean.valueOf(earliestHearingDayStartTime.isAfter(searchStartTime) || earliestHearingDayStartTime.isEqual(searchStartTime));
        final Boolean allHearingStartTimeBeforeSearchStartTime = Boolean.valueOf(latestHearingDayStartTime.isBefore(searchStartTime) || latestHearingDayStartTime.isEqual(searchStartTime));

        return Pair.of(allHearingStartTimeAfterSearchStartTime, allHearingStartTimeBeforeSearchStartTime);
    }

    private JsonNode filterEjectedCasesAndCourtApplicationsForAlphabeticalList(final JsonNode properties) {
        if (isNull(properties)) {
            return null;
        }
        final List<JsonNode> listedCasesNodes = properties.findValues(LISTED_CASES);
        final List<JsonNode> courtApplicationsNodes = properties.findValues(COURT_APPLICATIONS);
        listedCasesNodes.forEach(this::removeNodeForEjectedFlag);
        courtApplicationsNodes.forEach(this::removeNodeForEjectedFlag);
        return properties;
    }

    private JsonNode removeHearingsWhenBothCasesAndApplicationsAreEmpty(final JsonNode properties) {
        if (isNull(properties)) {
            return null;
        }
        final JsonNode hearingsByHearingDate = properties.findPath(HEARINGS_BY_HEARING_DATE);
        if (!hearingsByHearingDate.isMissingNode() && hearingsByHearingDate.isArray()) {
            final ArrayNode arrayNode = (ArrayNode) hearingsByHearingDate;
            int index = 0;
            while (index < arrayNode.size()) {
                if (!casesOrApplicationsExists(arrayNode.get(index))) {
                    arrayNode.remove(index);
                    continue; // not incrementing index as array size is reduced by one
                }
                index++;
            }
        }
        return properties;
    }

    private JsonNode filterEjectCaseAndCourtApplications(final JsonNode properties) {
        final JsonNode listedCaseNode = properties.findPath(LISTED_CASES);
        final JsonNode courtApplicationsNode = properties.findPath(COURT_APPLICATIONS);
        removeNodeForEjectedFlag(listedCaseNode);
        removeNodeForEjectedFlag(courtApplicationsNode);
        return properties;
    }

    private boolean casesOrApplicationsExists(final JsonNode properties) {
        final JsonNode listedCaseNode = properties.findPath(LISTED_CASES);
        final JsonNode courtApplicationsNode = properties.findPath(COURT_APPLICATIONS);
        return arrayContainElements(listedCaseNode) || arrayContainElements(courtApplicationsNode);
    }

    private boolean arrayContainElements(final JsonNode jsonNode) {
        return !jsonNode.isMissingNode() && jsonNode.size() > 0;
    }

    private void removeNodeForEjectedFlag(final JsonNode jsonNode) {
        if (nonNull(jsonNode) && !jsonNode.isMissingNode()) {
            final ArrayNode arrayNode = (ArrayNode) jsonNode;
            int index = 0;
            while (index < arrayNode.size()) {
                final JsonNode caseOrApplication = arrayNode.get(index);
                final JsonNode isEjectedNode = caseOrApplication.path(IS_EJECTED);
                if (!isEjectedNode.isMissingNode() && isEjectedNode.asBoolean()) {
                    arrayNode.remove(index);
                    continue; // not incrementing index as array size is reduced by one
                }
                index++;
            }
        }
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {
        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        final JsonObjectBuilder builder = Json.createObjectBuilder();
        object.forEach((key, value) -> {
            if (!object.isNull(key)) {
                builder.add(key, object.get(key));
            } else {
                builder.addNull(key);
            }
        });

        if (!object.containsKey(JUDICIARY)) {
            builder.add(JUDICIARY, EMPTY_JSON_ARRAY);
        }

        return builder.build();
    }

    private JsonObject enrich(final JsonObject source, final String judiciaryKey, final JsonArray judiciaryValue) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(judiciaryKey, judiciaryValue);
        source.forEach((key, value) -> {
            if (!source.isNull(key)) {
                if (key.equals(HEARINGS_BY_COURT_CENTRE_ID)) {
                    builder.add(key, filterEjectCaseAndCourtApplicationFromHearing(source.get(key)));
                } else {
                    builder.add(key, source.get(key));
                }
            }
        });

        return builder.build();
    }

    @SuppressWarnings({"squid:S1166", "squid:S2139"})
    private JsonValue filterEjectCaseAndCourtApplicationFromHearing(final JsonValue hearingsByCourtCentreId) {
        final JsonNode hearingByCourtCenterIdNode = mapper.valueToTree(hearingsByCourtCentreId);
        if (hearingByCourtCenterIdNode.isArray()) {
            final ArrayNode hearingByCourtCenterArrayNode = (ArrayNode) hearingByCourtCenterIdNode;
            hearingByCourtCenterArrayNode.forEach(h -> {
                if (h.isObject()) {
                    final ObjectNode hearingObjectNode = (ObjectNode) h;
                    final ArrayNode hearingsByHearingDateArrayNode = (ArrayNode) hearingObjectNode.get(HEARINGS_BY_HEARING_DATE);
                    hearingsByHearingDateArrayNode.forEach(s -> {
                        final ObjectNode hearingJsonObject = (ObjectNode) s.get(HEARING);
                        removeNodeForEjectedFlag(hearingJsonObject.get(LISTED_CASES));
                        removeNodeForEjectedFlag(hearingJsonObject.get(COURT_APPLICATIONS));
                    });
                }
            });
        }
        try {
            return mapper.readValue(hearingByCourtCenterIdNode.toString(), JsonValue.class);
        } catch (IOException e) {
            LOGGER.error(format(ERROR_MESSAGE_FORMAT, hearingByCourtCenterIdNode.toString(), e.getMessage()), e.getCause());
            throw new IllegalStateException(format(ERROR_MESSAGE_FORMAT, hearingByCourtCenterIdNode.toString(), e.getMessage()));
        }

    }

    @SuppressWarnings({"squid:S1166", "squid:S2139"})
    private JsonNode deepCopyProperties(Hearing hearing) {
        final JsonNode properties = hearing.getProperties();
        if (isNull(properties)) {
            return null;
        }
        try {
            return mapper.readTree(properties.toString());
        } catch (IOException e) {
            LOGGER.error(format(ERROR_MESSAGE_FORMAT, properties.toString(), e.getMessage()), e.getCause());
            throw new IllegalStateException(format(ERROR_MESSAGE_FORMAT, properties.toString(), e.getMessage()));
        }
    }
}
