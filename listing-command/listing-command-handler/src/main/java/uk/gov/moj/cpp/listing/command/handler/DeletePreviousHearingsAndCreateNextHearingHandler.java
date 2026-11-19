package uk.gov.moj.cpp.listing.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.CreateNextHearing;
import uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.aggregate.AggregateStore;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class DeletePreviousHearingsAndCreateNextHearingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePreviousHearingsAndCreateNextHearingHandler.class);



    // MBD refactor Phase 1 (ADR 011): migrated aggregates
    @Inject
    private AggregateStore aggregateStore;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Handles("listing.command.delete-previous-hearings-and-create-next-hearing")
    public void handleDeletePreviousHearingsAndCreateNextHearing(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.delete-previous-hearings-and-create-next-hearing' received with payload {}", command.toObfuscatedDebugString());
        }
        final DeletePreviousHearingsAndCreateNextHearing deletePreviousHearingsAndCreateNextHearing = jsonObjectConverter.convert(command.payloadAsJsonObject(), DeletePreviousHearingsAndCreateNextHearing.class);
        final SeedingHearing seedingHearing = deletePreviousHearingsAndCreateNextHearing.getDeletePreviousHearings().getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();
        final CreateNextHearing createNextHearing = deletePreviousHearingsAndCreateNextHearing.getCreateNextHearing();
        final uk.gov.justice.listing.events.CreateNextHearing createNextHearingEvent = uk.gov.justice.listing.events.CreateNextHearing.createNextHearing()
                .withCommittingCourt(createNextHearing.getCommittingCourt())
                .withHearing(createNextHearing.getHearing())
                .withPreviousBookingReferencesWithCourtScheduleIds(createNextHearing.getPreviousBookingReferencesWithCourtScheduleIds())
                .withSeedingHearing(createNextHearing.getSeedingHearing())
                .withShadowListedOffences(createNextHearing.getShadowListedOffences())
                .build();

        updateSeedAggregateEventStream(command, seedingHearingId, (SeedHearingAggregate seedHearingAggregate) ->
                seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay,createNextHearingEvent));

    }

    private void updateSeedAggregateEventStream(final JsonEnvelope command, final UUID seedHearingId,
                                                final Function<SeedHearingAggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        // MBD refactor P1e: SeedHearingAggregate state in aggregate_snapshot, events recorded in-process
        aggregateStore.update(command, seedHearingId, SeedHearingAggregate.class, aggregatorFunction);
    }


}
