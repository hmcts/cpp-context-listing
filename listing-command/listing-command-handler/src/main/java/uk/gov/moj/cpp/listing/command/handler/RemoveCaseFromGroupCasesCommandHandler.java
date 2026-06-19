package uk.gov.moj.cpp.listing.command.handler;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class RemoveCaseFromGroupCasesCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveCaseFromGroupCasesCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CommandToDomainConverter commandToDomainConverter;

    @Handles("listing.command.remove-case-from-group-cases")
    public void removeCaseFromGroupCases(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        LOGGER.info("{} event received with payload {}",
                "listing.command.remove-case-from-group-cases",
                jsonEnvelope.payloadAsJsonObject());

        final UUID groupId = fromString(jsonEnvelope.payloadAsJsonObject().getString("groupId"));
        final UUID masterCaseId = fromString(jsonEnvelope.payloadAsJsonObject().getString("masterCaseId"));
        final ProsecutionCase removedCase = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject().getJsonObject("removedCase"), ProsecutionCase.class);
        final ProsecutionCase newGroupMaster = jsonEnvelope.payloadAsJsonObject().containsKey("newGroupMaster") ?
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject().getJsonObject("newGroupMaster"), ProsecutionCase.class) : null;

        final EventStream eventStream = eventSource.getStreamById(masterCaseId);
        final Case caseAggregate = aggregateService.get(eventStream, Case.class);
        final List<UUID> hearingIds = caseAggregate.getHearingIds();

        if (nonNull(newGroupMaster) && !hearingIds.isEmpty()) {
            final EventStream newMasterCaseStream = eventSource.getStreamById(newGroupMaster.getId());
            final Case newMasterCaseAggregate = aggregateService.get(newMasterCaseStream, Case.class);
            appendEventsToStream(jsonEnvelope, newMasterCaseStream,
                    newMasterCaseAggregate.updateMasterCaseForGroup(newGroupMaster.getId(), hearingIds));
        }

        for (final UUID hearingId : hearingIds) {
            final EventStream hearingEventStream = eventSource.getStreamById(hearingId);
            final Hearing hearingAggregate = aggregateService.get(hearingEventStream, Hearing.class);
            appendEventsToStream(jsonEnvelope, hearingEventStream,
                    hearingAggregate.removeCaseFromGroupCases(hearingId, groupId,
                            commandToDomainConverter.buildListedCases(removedCase),
                            commandToDomainConverter.buildListedCases(newGroupMaster)));
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
