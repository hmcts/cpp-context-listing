package uk.gov.moj.cpp.listing.persistence.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.String.format;

public class JsonNodeUpdater {

    private final ObjectNode properties;
    private final ObjectNode updatedProperties;
    private final Consumer<ObjectNode> updaterFunction;
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    private JsonNodeUpdater(final ObjectNode properties,
                            final String pathToFind,
                            final Consumer<ObjectNode> updaterFunction) {
        this.properties = properties;
        this.updatedProperties = (ObjectNode) properties.findPath(pathToFind);
        this.updaterFunction = updaterFunction;
    }

    private JsonNodeUpdater(final ObjectNode properties,
                            final Consumer<ObjectNode> updaterFunction) {
        this.properties = properties;
        this.updatedProperties = properties;
        this.updaterFunction = updaterFunction;
    }

    public static JsonNodeUpdater createJsonNodeUpdater(final ObjectNode properties,
                                                        final String pathToFind,
                                                        final Consumer<ObjectNode> updaterFunction) {
        return new JsonNodeUpdater(
                properties,
                pathToFind,
                updaterFunction);
    }

    public static JsonNodeUpdater createJsonNodeUpdater(final ObjectNode properties,
                                                        final Consumer<ObjectNode> updaterFunction) {
        return new JsonNodeUpdater(
                properties,
                updaterFunction);
    }

    public JsonNodeUpdater put(String fieldName, UUID value) {
        updatedProperties.put(fieldName, value.toString());
        return this;
    }

    public <T> JsonNodeUpdater putObject(String fieldName, T value) {
        JsonNode jsonNode = convertToJsonNode(value);
        updatedProperties.set(fieldName, jsonNode);
        return this;
    }

    public <T> JsonNodeUpdater putObjectList(String fieldName, List<T> values) {
        values.stream().map(value -> convertToJsonNode(value))
                .collect(
                        () -> getJsonNodes(fieldName, updatedProperties),
                        (response, element) -> response.add(element),
                        (response, element) -> response.add(element)
                );
        return this;
    }

    public JsonNodeUpdater putLocalDateList(String fieldName, List<LocalDate> values) {
        values.stream().collect(
                        () -> getJsonNodes(fieldName, updatedProperties),
                        (response, element) -> response.add(LocalDates.to(element)),
                        (response, element) -> response.add(element)
                );
        return this;
    }

    public <T> JsonNodeUpdater putSubList(String fieldName, TypeReference subListType, Function<List<T>, List<T>> updateFunction) {
        final ArrayNode subListProperties = (ArrayNode) updatedProperties.get(fieldName);
        final List<T> subList = getSubList(subListProperties, subListType);
        List<T> updatedSubList = updateFunction.apply(subList);
        JsonNode newProperties = mapper.valueToTree(updatedSubList);
        updatedProperties.replace(fieldName, newProperties);
        return this;
    }

    public void save() {
        updaterFunction.accept(properties);
    }

    public JsonNodeUpdater put(String fieldName, String value) {
        updatedProperties.put(fieldName, value);
        return this;
    }

    public JsonNodeUpdater put(String fieldName, LocalDate value) {
        updatedProperties.put(fieldName, LocalDates.to(value));
        return this;
    }

    public JsonNodeUpdater put(String fieldName, boolean value) {
        updatedProperties.put(fieldName, value);
        return this;
    }

    public JsonNodeUpdater remove(String fieldName) {
        updatedProperties.remove(fieldName);
        return this;
    }

    private JsonNode convertToJsonNode(Object source) {
        return mapper.valueToTree(source);
    }

    private <T> List<T> getSubList(final ArrayNode fieldProperties, final TypeReference typeOfList) {
        try {
            return mapper.readValue(mapper.treeAsTokens(fieldProperties), mapper.getTypeFactory().constructType(typeOfList));
        } catch (IOException e) {
            throw new JsonUpdateException(format("Unable to convert properties %s to String", fieldProperties), e);
        }
    }

    private ArrayNode getJsonNodes(String fieldName, ObjectNode node) {
        return node.putArray(fieldName);
    }

}
