package uk.gov.moj.cpp.listing.command.handler;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.UpdateExistingHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.PtphDetail;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;
import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188"})
public class UpdateExistingHearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateExistingHearingCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @SuppressWarnings("squid:S3655")
    @Handles("listing.command.update-existing-hearing")
    public void updateExistingHearing(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-existing-hearing' received with payload {}", command.toObfuscatedDebugString());
        }

        final JsonObject payload = command.payloadAsJsonObject();
        final UpdateExistingHearing updateExistingHearing = jsonObjectConverter.convert(payload, UpdateExistingHearing.class);
        final SeedingHearing seedingHearing = updateExistingHearing.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final UUID existingHearingHearingId = updateExistingHearing.getHearingId();
        final List<ProsecutionCase> prosecutionCases = updateExistingHearing.getProsecutionCases();
        final List<UUID> shadowListedOffences = updateExistingHearing.getShadowListedOffences();
        final String hearingDay = seedingHearing.getSittingDay();

        // LPT-2405: tier / list type resolved in the command API from the stored hearing
        final PtphDetail ptphDetail = buildPtphDetail(updateExistingHearing.getTier(),
                updateExistingHearing.getListType(), updateExistingHearing.getKeyReason());

        updateSeedAggregateEventStream(command, seedingHearingId, (SeedHearingAggregate seedHearingAggregate) ->
                seedHearingAggregate.requestUpdateExistingHearing(seedingHearingId, existingHearingHearingId, hearingDay, prosecutionCases, shadowListedOffences, ptphDetail));

    }

    private PtphDetail buildPtphDetail(final String tier, final String listType, final String keyReason) {
        if (isNull(tier) && isNull(listType) && isNull(keyReason)) {
            return null;
        }
        return new PtphDetail(tier, listType, keyReason);
    }

    private void updateSeedAggregateEventStream(final JsonEnvelope command, final UUID seedHearingId,
                                          final Function<SeedHearingAggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(seedHearingId);
        final SeedHearingAggregate seedHearingAggregate = aggregateService.get(eventStream, SeedHearingAggregate.class);

        final Stream<Object> events = aggregatorFunction.apply(seedHearingAggregate);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

}
