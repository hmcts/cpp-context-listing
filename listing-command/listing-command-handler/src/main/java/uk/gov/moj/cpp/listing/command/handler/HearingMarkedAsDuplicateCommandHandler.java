package uk.gov.moj.cpp.listing.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.listing.courts.MarkHearingAsDuplicate;
import uk.gov.justice.listing.courts.MarkHearingAsDuplicateForCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.aggregate.AggregateStore;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class HearingMarkedAsDuplicateCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingMarkedAsDuplicateCommandHandler.class);



    // MBD refactor Phase 1 (ADR 011): migrated aggregates
    @Inject
    private AggregateStore aggregateStore;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("listing.command.mark-hearing-as-duplicate")
    public void handleMarkHearingAsDuplicate(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.mark-hearing-as-duplicate' received with payload {}", command.toObfuscatedDebugString());
        }

        final MarkHearingAsDuplicate markHearingAsDuplicate = jsonObjectConverter.convert(command.payloadAsJsonObject(), MarkHearingAsDuplicate.class);
        final UUID hearingId = markHearingAsDuplicate.getHearingId();

        final List<UUID> caseIds = markHearingAsDuplicate.getProsecutionCaseIds();
        updateHearingEventStream(command, hearingId, (Hearing hearing) -> hearing.markHearingAsDuplicate(hearingId, caseIds));
    }

    @Handles("listing.command.mark-unallocated-hearing-as-duplicate")
    public void handleMarkUnallocatedHearingAsDuplicate(final JsonEnvelope command) throws EventStreamException {

        final UUID hearingId = UUID.fromString(command.payloadAsJsonObject().getString("hearingId"));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.mark-unallocated-hearing-as-duplicate' for hearingId {}", hearingId);
        }

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.markUnallocatedHearingAsDuplicate(hearingId));
    }

    @Handles("listing.command.mark-hearing-as-duplicate-for-case")
    public void handleMarkHearingAsDuplicateForCase(final JsonEnvelope command) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.mark-hearing-as-duplicate-for-case' received with payload {}", command.toObfuscatedDebugString());
        }

        final MarkHearingAsDuplicateForCase markHearingAsDuplicateForCase = jsonObjectConverter.convert(command.payloadAsJsonObject(), MarkHearingAsDuplicateForCase.class);
        final UUID hearingId = markHearingAsDuplicateForCase.getHearingId();
        final UUID caseId = markHearingAsDuplicateForCase.getCaseId();

        updateCaseEventStream(command, caseId, (Case listingCase) ->
                listingCase.markHearingAsDuplicate(hearingId, caseId));
    }

    // MBD refactor P1d: Case state in aggregate_snapshot, events recorded in-process
    private void updateCaseEventStream(final JsonEnvelope command, final UUID caseId,
                                       final Function<Case, Stream<Object>> aggregatorFunction) throws EventStreamException {
        aggregateStore.update(command, caseId, Case.class, aggregatorFunction);
    }

    private void updateHearingEventStream(final JsonEnvelope command, final UUID hearingId,
                                          final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        // MBD refactor P1f: Hearing state in aggregate_snapshot, events recorded in-process
        aggregateStore.update(command, hearingId, Hearing.class, aggregatorFunction);
    }
}
