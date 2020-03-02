package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingJsonListConverterFilterEjectCases implements ListOfJsontoJsonArrayConverter {

    private static final String LISTED_CASES = "listedCases";
    private static final String IS_EJECTED = "isEjected";
    private static final String JUDICIARY = "judiciary";
    private static final String HEARINGS = "hearings";
    private static final JsonArray EMPTY_JSON_ARRAY = Json.createArrayBuilder().build();
    public static final String HEARINGS_BY_COURT_CENTRE_ID = "hearingsByCourtCentreId";
    public static final String HEARINGS_BY_HEARING_DATE = "hearingsByHearingDate";
    public static final String HEARING = "hearing";
    public static final String COURT_APPLICATIONS = "courtApplications";
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingJsonListConverterFilterEjectCases.class);


    @Override
    public JsonArray convert(final List<Hearing> hearings) {
        return hearings.stream()
                .map(Hearing::getProperties)
                .map(this::filterEjectCaseAndCourtApplications)
                .filter(this::casesOrApplicationsExists)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.toString()))
                .collect(JsonArrayCollector.toArrayNode());
    }

    @Override
    public JsonArray convertHearingResultForAlphbeticalList(final List<Hearing> hearings) {
        return hearings.stream().filter(hearing -> Objects.nonNull(hearing.getProperties()))
                .map(Hearing::getProperties)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.get(0).toString()))
                .collect(JsonArrayCollector.toArrayNode());
    }

    @Override
    public JsonArray convertHearingResultForPublicList(final Hearing hearing) {
        if (hearing != null) {
            final JsonObject publicHearingResult = this.jsonFromString(hearing.getProperties().toString());
            final JsonArray judiciary = publicHearingResult.isNull(JUDICIARY) ?  EMPTY_JSON_ARRAY : publicHearingResult.getJsonArray(JUDICIARY);
            if(publicHearingResult.isNull(HEARINGS)) {
                return Json.createArrayBuilder().build();
            }
            return publicHearingResult.getJsonArray(HEARINGS).getValuesAs(JsonObject.class).stream()
                    .map(hearingByCourtCentreId -> this.enrich(hearingByCourtCentreId, JUDICIARY, judiciary))
                    .collect(JsonArrayCollector.toArrayNode());

        }
        return Json.createArrayBuilder().build();
    }


    private JsonNode filterEjectCaseAndCourtApplications(final JsonNode properties) {
        final JsonNode listedCaseNode = properties.path(LISTED_CASES);
        final JsonNode courtApplicationsNode = properties.path(COURT_APPLICATIONS);
        removeNodeForEjectedFlag(listedCaseNode);
        removeNodeForEjectedFlag(courtApplicationsNode);
        return properties;

    }

    private boolean casesOrApplicationsExists(final JsonNode properties) {
        final JsonNode listedCaseNode = properties.path(LISTED_CASES);
        final JsonNode courtApplicationsNode = properties.path(COURT_APPLICATIONS);
        return arrayContainElements(listedCaseNode) || arrayContainElements(courtApplicationsNode);
    }
    private boolean arrayContainElements(final JsonNode jsonNode) {
        if (!jsonNode.isMissingNode()) {
            final ArrayNode arrayNode = (ArrayNode) jsonNode;
            return arrayNode.size() > 0;
        }
        return false;
    }

    private void removeNodeForEjectedFlag(final JsonNode jsonNode) {
        if (jsonNode != null && !jsonNode.isMissingNode()) {
            final ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (int index = 0; index < arrayNode.size(); index++) {
                final JsonNode listedCase = arrayNode.get(index);
                final JsonNode isEjectedNode = listedCase.path(IS_EJECTED);
                if (!isEjectedNode.isMissingNode() && isEjectedNode.asBoolean()) {
                    arrayNode.remove(index);
                }
            }
        }
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }

    private JsonObject enrich(final JsonObject source, final String key, final JsonArray value) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, value);
        source.entrySet().
                forEach(e -> {
                    if (e.getKey().equals(HEARINGS_BY_COURT_CENTRE_ID)) {
                        builder.add(e.getKey(), filterEjectCaseAndCourtApplicationFromHearing(e.getValue()));
                    } else {
                        builder.add(e.getKey(), e.getValue());
                    }
                });
        return builder.build();
    }

    @SuppressWarnings({"squid:S1166", "squid:S2139"})
    private JsonValue filterEjectCaseAndCourtApplicationFromHearing(final JsonValue hearingsByCourtCentreId) {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonNode hearingByCourtCenterIdNode = mapper.valueToTree(hearingsByCourtCentreId);
        if (hearingByCourtCenterIdNode.isArray()) {
            final ArrayNode hearingByCourtCenterArrayNode = (ArrayNode) hearingByCourtCenterIdNode;
            hearingByCourtCenterArrayNode.forEach(h -> {
                if (h.isObject()) {
                    final ObjectNode hearingObjectNode = (ObjectNode) h;
                    final ArrayNode hearingsByHearingDateArrayNode = (ArrayNode) hearingObjectNode.get(HEARINGS_BY_HEARING_DATE);
                    final ObjectNode hearingJsonObject = (ObjectNode) hearingsByHearingDateArrayNode.get(0).get(HEARING);
                    removeNodeForEjectedFlag(hearingJsonObject.get(LISTED_CASES));
                    removeNodeForEjectedFlag(hearingJsonObject.get(COURT_APPLICATIONS));
                }
            });
        }
        try {
            return mapper.readValue(hearingByCourtCenterIdNode.toString(), JsonValue.class);
        } catch (IOException e) {
            LOGGER.error("Resource " + hearingByCourtCenterIdNode.toString() + " unable to parse: " + e.getMessage(), e.getCause());
            throw new IllegalStateException("Resource " + hearingByCourtCenterIdNode.toString() + " unable to parse: " + e.getMessage());
        }

    }
}
