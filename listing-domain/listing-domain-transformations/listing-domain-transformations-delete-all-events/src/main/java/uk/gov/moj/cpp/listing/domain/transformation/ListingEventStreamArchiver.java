package uk.gov.moj.cpp.listing.domain.transformation;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class ListingEventStreamArchiver implements EventTransformation {

    @Override
    public Action actionFor(final JsonEnvelope event) {
        return DEACTIVATE;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // no implementation required
    }
}