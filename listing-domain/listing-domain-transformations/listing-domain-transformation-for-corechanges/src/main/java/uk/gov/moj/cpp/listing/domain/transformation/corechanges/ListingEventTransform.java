package uk.gov.moj.cpp.listing.domain.transformation.corechanges;

import org.slf4j.Logger;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform.ListingEventTransformer;
import uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform.TransformFactory;

import javax.json.JsonObject;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

@Transformation
public class ListingEventTransform implements EventTransformation {

    private static final Logger LOGGER = getLogger(ListingEventTransform.class);

    private TransformFactory transformFactory;

    public static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String DEFENDANT_ID = "id";
    public static final String COURT_PROCEEDINGS_INITIATED = "courtProceedingsInitiated";

    public static final String EVENT_DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated";
    public static final String EVENT_HEARING_LISTED = "listing.events.hearing-listed";
    public static final String EVENT_NEW_DEFENDANT_DETAILS_UPDATED = "listing.events.new-defendant-details-updated";
    public static final String EVENT_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED = "listing.events" +
                                                                                           ".case-resulted-defendant" +
                                                                                           "-proceedings-updated";
    public static final String EVENT_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED = "listing.events" +
                                                                                         ".case-update-defendant" +
                                                                                         "-proceedings-updated";

    public ListingEventTransform() {
        transformFactory = new TransformFactory();
    }


    @Override
    public Action actionFor(final JsonEnvelope event) {
        final List<ListingEventTransformer> eventTransformer =
                transformFactory.getEventTransformer(event.metadata().name().toLowerCase());
        if (eventTransformer != null && !eventTransformer.isEmpty()) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        JsonObject payload = event.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("----------------------event name------------ {}", event.metadata().name());
        }

        final String eventName = event.metadata().name().toLowerCase();
        for (final ListingEventTransformer listingEventTransformer : transformFactory.getEventTransformer(eventName)) {
            payload = listingEventTransformer.transform(event.metadata(), payload);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("-------------------transformedPayload---------------{}", payload);
        }

        return of(envelopeFrom(metadataFrom(event.metadata()), payload));

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        //Not needed
    }

    void setTransformFactory(final TransformFactory transformFactory) {
        this.transformFactory = transformFactory;
    }
}
