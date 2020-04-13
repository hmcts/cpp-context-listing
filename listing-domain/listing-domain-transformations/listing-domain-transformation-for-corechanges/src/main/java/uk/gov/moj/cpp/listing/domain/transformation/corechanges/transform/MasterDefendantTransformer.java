package uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform;

import org.slf4j.Logger;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.domain.transformation.corechanges.TransformUtil;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.DEFENDANT_ID;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_DEFENDANTS_TO_BE_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_HEARING_LISTED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_NEW_DEFENDANT_DETAILS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.MASTER_DEFENDANT_ID;

public class MasterDefendantTransformer implements ListingEventTransformer {

    protected static final Map<String, Pattern> eventAndJsonPaths = Collections.unmodifiableMap(
            Stream.of(new String[][]{
                    {EVENT_DEFENDANTS_TO_BE_UPDATED, "defendants\\.\\d"},
                    {EVENT_HEARING_LISTED, "hearing\\.listedCases\\.\\d\\.defendants\\.\\d"},
                    {EVENT_NEW_DEFENDANT_DETAILS_UPDATED, "defendant"},
                    {EVENT_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED, "prosecutionCase\\.defendants\\.\\d"},
                    {EVENT_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED, "prosecutionCase\\.defendants\\.\\d"},
            }).collect(Collectors.toMap(data -> data[0], data -> Pattern.compile(data[1]))));

    private static final Logger LOGGER = getLogger(MasterDefendantTransformer.class);

    public static Map<String, Pattern> getEventAndJsonPaths() {
        return eventAndJsonPaths;
    }

    @Override
    public JsonObject transform(final Metadata eventMetadata, final JsonObject payload) {
        final JsonObjectBuilder transformedPayloadObjectBuilder;
        final Pattern jsonPath = eventAndJsonPaths.get(eventMetadata.name().toLowerCase());

        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            if (!path.isEmpty() && match(jsonPath, path) && (jsonValue instanceof JsonObject)) {
                return masterDefendantIdTransform((JsonObject) jsonValue);
            } else {
                return jsonValue;
            }
        };

        transformedPayloadObjectBuilder = TransformUtil.cloneObjectWithPathFilter(payload, filter);

        return transformedPayloadObjectBuilder.build();
    }

    private Object masterDefendantIdTransform(final JsonObject jsonObject) {

        final JsonObjectBuilder result = createObjectBuilder();
        for (final Map.Entry<String, JsonValue> property : jsonObject.entrySet()) {
            final String key = property.getKey();
            final JsonValue value = property.getValue();
            result.add(key, value);
            if (key.equalsIgnoreCase(DEFENDANT_ID)) {
                if (jsonObject.containsKey(MASTER_DEFENDANT_ID)) {
                    LOGGER.warn("Defendant {} already have {} with value {} ", value, MASTER_DEFENDANT_ID, jsonObject.getString(MASTER_DEFENDANT_ID));
                } else {
                    result.add(MASTER_DEFENDANT_ID, value);
                }
            }
        }
        return result;
    }

    public boolean match(final Pattern jsonPath, final Deque<String> path) {
        final String pathMerged = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(path.descendingIterator(), 0),
                        false)
                .collect(Collectors.joining("."));
        return jsonPath.matcher(String.join(".", pathMerged)).matches();
    }

}
