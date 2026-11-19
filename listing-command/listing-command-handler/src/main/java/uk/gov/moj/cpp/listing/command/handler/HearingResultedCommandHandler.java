package uk.gov.moj.cpp.listing.command.handler;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.aggregate.AggregateStore;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class HearingResultedCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedCommandHandler.class);



    // MBD refactor Phase 1 (ADR 011): migrated aggregates
    @Inject
    private AggregateStore aggregateStore;

    @Handles("listing.command.set-hearing-resulted-status")
    public void handleSetHearingResultStatus(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.set-hearing-resulted-status' received with payload {}", command.toObfuscatedDebugString());
        }
        final UUID hearingId = UUID.fromString(command.payloadAsJsonObject().getString("hearingId", null));
        if(nonNull(hearingId)) {
            updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                    hearing.setHearingResultStatus(hearingId));
        } else {
            LOGGER.warn("Hearing ID is null in the command payload");
        }


    }

    private void updateHearingEventStream(final JsonEnvelope command, final UUID hearingId,
                                          final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        // MBD refactor P1f: Hearing state in aggregate_snapshot, events recorded in-process
        aggregateStore.update(command, hearingId, Hearing.class, aggregatorFunction);
    }


}
