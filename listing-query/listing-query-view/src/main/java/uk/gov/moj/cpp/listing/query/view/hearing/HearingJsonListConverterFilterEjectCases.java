package uk.gov.moj.cpp.listing.query.view.hearing;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static uk.gov.moj.cpp.listing.query.view.hearing.JsonArrayCollector.toArrayNode;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingJsonListConverterFilterEjectCases.class);

    private static final String LISTED_CASES = "listedCases";
    private static final String IS_EJECTED = "isEjected";
    private static final String JUDICIARY = "judiciary";
    private static final String HEARINGS = "hearings";
    private static final JsonArray EMPTY_JSON_ARRAY = createArrayBuilder().build();
    public static final String HEARINGS_BY_COURT_CENTRE_ID = "hearingsByCourtCentreId";
    public static final String HEARINGS_BY_HEARING_DATE = "hearingsByHearingDate";
    public static final String HEARING = "hearing";
    public static final String COURT_APPLICATIONS = "courtApplications";

    private static final String ERROR_MESSAGE_FORMAT = "Resource %s unable to parse. Reason: %s";

    @Override
    public JsonArray convert(final List<Hearing> hearings) {
        return hearings.stream()
                .map(Hearing::getProperties)
                .map(this::filterEjectCaseAndCourtApplications)
                .filter(this::casesOrApplicationsExists)
                .map(hearingJsonNode -> this.jsonFromString(hearingJsonNode.toString()))
                .collect(toArrayNode());
    }

    @Override
    public JsonArray convertHearingResultForAlphabeticalList(final List<Hearing> hearings) {
        return hearings.stream()
                .map(Hearing::getProperties)
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

        return object;
    }

    private JsonObject enrich(final JsonObject source, final String judiciaryKey, final JsonArray judiciaryValue) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(judiciaryKey, judiciaryValue);
        final Iterator<String> keys = source.keySet().iterator();
        while (keys.hasNext()) {
            final String key = keys.next();
            if (!source.isNull(key)) {
                if (key.equals(HEARINGS_BY_COURT_CENTRE_ID)) {
                    builder.add(key, filterEjectCaseAndCourtApplicationFromHearing(source.get(key)));
                } else {
                    builder.add(key, source.get(key));
                }
            }
        }

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
            LOGGER.error(format(ERROR_MESSAGE_FORMAT, hearingByCourtCenterIdNode.toString(), e.getMessage()), e.getCause());
            throw new IllegalStateException(format(ERROR_MESSAGE_FORMAT, hearingByCourtCenterIdNode.toString(), e.getMessage()));
        }

    }
}
