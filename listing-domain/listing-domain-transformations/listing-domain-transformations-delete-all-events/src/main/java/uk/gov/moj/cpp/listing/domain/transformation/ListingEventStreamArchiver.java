package uk.gov.moj.cpp.listing.domain.transformation;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.List;

import javax.json.JsonObject;

@Transformation
public class ListingEventStreamArchiver implements EventTransformation {

    private static final String HEARING_LISTED = "listing.events.hearing-listed";

    private static final String HEARING_ADDED_TO_CASE = "listing.events.hearing-added-to-case";

    private static final List<String> EVENTS_TO_TRANSFORM = newArrayList(HEARING_LISTED);

    private Enveloper enveloper;

    @Override
    public Action actionFor(final JsonEnvelope event) {

        if (EVENTS_TO_TRANSFORM.stream().anyMatch(eventToArchive -> event.metadata().name().equalsIgnoreCase(eventToArchive))) {

            final JsonObject payload = event.payloadAsJsonObject();

            final JsonObject hearing = payload.getJsonObject("hearing");

            if (isNull(hearing)) { //This is old message listing.events.hearing-listed message

                return DEACTIVATE;
            }

            return NO_ACTION;
        }

        if(event.metadata().name().equals(HEARING_ADDED_TO_CASE)) {
            return NO_ACTION;
        }

        return DEACTIVATE;

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}