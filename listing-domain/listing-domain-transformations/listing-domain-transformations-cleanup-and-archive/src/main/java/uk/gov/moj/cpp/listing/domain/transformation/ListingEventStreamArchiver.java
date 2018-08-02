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
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class ListingEventStreamArchiver implements EventTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventStreamArchiver.class);

    private static final List<String> EVENTS_TO_ARCHIVE = newArrayList(
            "hearing-confirmed",
            "listing.events.hearing-listed",
            "listing.events.start-date-changed-for-hearing",
            "listing.events.estimate-minutes-changed-for-hearing",
            "listing.events.judge-changed-for-hearing",
            "listing.events.allocated-hearing-updated-for-listing",
            "listing.events.not-before-unselected-for-hearing",
            "listing.events.judge-removed-from-hearing",
            "listing.events.court-room-assigned-to-hearing",
            "listing.events.unallocated-hearing-listed",
            "listing.events.type-changed-for-hearing",
            "listing.events.not-before-selected-for-hearing",
            "listing.events.court-room-changed-for-hearing",
            "listing.events.start-time-changed-for-hearing",
            "listing.events.court-room-removed-from-hearing",
            "listing.events.start-time-assigned-to-hearing",
            "listing.events.case-sent-for-listing",
            "listing.events.hearing-allocated-for-listing",
            "listing.events.hearing-unallocated-for-listing",
            "listing.events.start-time-removed-from-hearing",
            "listing.events.judge-assigned-to-hearing"
    );

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (EVENTS_TO_ARCHIVE.stream()
                .anyMatch(eventToArchive -> event.metadata().name().equalsIgnoreCase(eventToArchive))) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deactivating event {}", event.metadata().name());
            }
            return DEACTIVATE;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("No action on event {}", event.metadata().name());
        }
        return NO_ACTION;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // no implementation required
    }
}