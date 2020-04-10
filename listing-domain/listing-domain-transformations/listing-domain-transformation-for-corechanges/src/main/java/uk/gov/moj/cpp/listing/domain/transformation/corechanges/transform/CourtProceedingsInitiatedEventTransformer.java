package uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.domain.transformation.corechanges.TransformUtil;
import uk.gov.moj.cpp.listing.domain.transformation.corechanges.exceptions.EventTransformationException;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.COURT_PROCEEDINGS_INITIATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_DEFENDANTS_TO_BE_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_HEARING_LISTED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_NEW_DEFENDANT_DETAILS_UPDATED;

public class CourtProceedingsInitiatedEventTransformer implements ListingEventTransformer {

    private static final Map<String, Pattern> eventAndJsonPaths = Collections.unmodifiableMap(
            Stream.of(new String[][]{
                    {EVENT_DEFENDANTS_TO_BE_UPDATED, "defendants\\.\\d"},
                    {EVENT_HEARING_LISTED, "hearing\\.listedCases\\.\\d\\.defendants\\.\\d"},
                    {EVENT_NEW_DEFENDANT_DETAILS_UPDATED, "defendant"},
                    {EVENT_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED, "prosecutionCase\\.defendants\\.\\d"},
                    {EVENT_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED, "prosecutionCase\\.defendants\\.\\d"},
            }).collect(Collectors.toMap(data -> data[0], data -> Pattern.compile(data[1]))));

    static Map<String, Pattern> getEventAndJsonPaths() {
        return eventAndJsonPaths;
    }

    @Override
    public JsonObject transform(final Metadata eventMetadata, final JsonObject payload) {
        final Pattern jsonPath = eventAndJsonPaths.get(eventMetadata.name().toLowerCase());
        final Optional<ZonedDateTime> createdAt = eventMetadata.createdAt();
        final ZonedDateTime courtProceedingsInitiated = createdAt.orElseThrow(() -> new EventTransformationException("Created At Metadata is null"));

        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            if (!path.isEmpty() && match(jsonPath, path) && (jsonValue instanceof JsonObject)) {
                return defendantTransform((JsonObject) jsonValue, courtProceedingsInitiated);
            } else {
                return jsonValue;
            }
        };

        final JsonObjectBuilder transformedPayloadObjectBuilder = TransformUtil.cloneObjectWithPathFilter(payload, filter);

        return transformedPayloadObjectBuilder.build();
    }

    private static boolean match(final Pattern jsonPath, final Deque<String> path) {
        final String pathMerged = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(path.descendingIterator(), 0),
                        false)
                .collect(Collectors.joining("."));
        return jsonPath.matcher(String.join(".", pathMerged)).matches();
    }


    private static Object defendantTransform(final JsonObject jsonObject, final ZonedDateTime courtProceedingsInitiated) {
        final JsonObjectBuilder result = createObjectBuilder();
        jsonObject.forEach(result::add);
        result.add(COURT_PROCEEDINGS_INITIATED, ZonedDateTimes.toString(courtProceedingsInitiated));
        return result;
    }

}
