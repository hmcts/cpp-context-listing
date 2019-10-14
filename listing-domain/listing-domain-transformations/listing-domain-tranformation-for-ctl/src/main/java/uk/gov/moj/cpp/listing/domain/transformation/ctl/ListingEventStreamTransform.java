package uk.gov.moj.cpp.listing.domain.transformation.ctl;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.slf4j.Logger;

@Transformation
@SuppressWarnings({"pmd:BeanMembersShouldSerialize", "squid:S1450"})
public class ListingEventStreamTransform implements EventTransformation {



    public static final String DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated";
    public static final String CASE_SENT_FOR_LISTING = "listing.events.case-sent-for-listing";
    public static final String HEARING_LISTED = "listing.events.hearing-listed";
    public static final String NEW_DEFENDANT_DETAILS_UPDATED = "listing.events.new-defendant-details-updated";

    private Enveloper enveloper;

    private uk.gov.moj.cpp.listing.domain.transformation.ctl.BailStatusEnum2ObjectTransformer bailStatusTransformer;

    private static final Logger LOGGER = getLogger(ListingEventStreamTransform.class);

    protected static final Set<String> eventsToTransform = new HashSet<>(Arrays.asList(DEFENDANTS_TO_BE_UPDATED,
            CASE_SENT_FOR_LISTING, HEARING_LISTED, NEW_DEFENDANT_DETAILS_UPDATED));


    public ListingEventStreamTransform() {
        bailStatusTransformer = new uk.gov.moj.cpp.listing.domain.transformation.ctl.BailStatusEnum2ObjectTransformer();
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (eventsToTransform.contains(event.metadata().name())) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("----------------------event name------------ {}", event.metadata().name());
        }

        final JsonObject transformedPayload = bailStatusTransformer.transform(payload);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("-------------------transformedPayload---------------{}", transformedPayload);
        }

        return of(envelopeFrom(metadataFrom(event.metadata()), transformedPayload));

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }

}
