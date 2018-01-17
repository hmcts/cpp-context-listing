package uk.gov.moj.cpp.listing.command.handler;

import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.utils.JsonDomainUtils;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188"})
public class ListingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingCommandHandler.class);

    private static final String HEARING_ID = "hearingId";
    private static final String TYPE = "type";
    private static final String START_DATE = "startDate";
    private static final String ESTIMATE_MINUTES = "estimateMinutes";
    private static final String JUDGE_ID = "judgeId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String START_TIME = "startTime";
    private static final String NOT_BEFORE = "notBefore";
    private static final String CASE_ID = "caseId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String URN = "urn";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;


    @Handles("listing.command.send-case-for-listing")
    public void sendCaseForListing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug(format("'listing.command.send-case-for-listing' received with payload %s", payload));

        final String caseId = payload.getString(CASE_ID);
        final String urn = payload.getString(URN);
        final List<uk.gov.moj.cpp.listing.domain.Hearing> hearings = createHearingsFrom(payload);

        updateCaseEventStream(command, caseId, (Case listingCase) ->
                listingCase.sendForListing(caseId, urn, hearings));
    }

    @Handles("listing.command.list-hearing")
    public void listHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug(format("'listing.command.list-hearing' received with payload %s", payload));

        final String hearingId = payload.getString(HEARING_ID);
        final String type = payload.getString(TYPE);
        final LocalDate startDate = LocalDates.from(payload.getString(START_DATE));
        final Integer estimateMinutes = payload.getInt(ESTIMATE_MINUTES);
        final String caseId = payload.getString(CASE_ID);
        final String courtCentreId = payload.getString(COURT_CENTRE_ID);
        final List<Defendant> defendants = createDefendantsFrom(payload);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.list(hearingId, type, startDate, estimateMinutes, caseId, courtCentreId, defendants));
    }

    @Handles("listing.command.handler.update-hearing-for-listing")
    public void updateHearingForListing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug(format("'listing.command.handler.update-hearing-for-listing' received with payload %s", payload));

        // Mandatory fields that always require a value
        final String hearingId = payload.getString(HEARING_ID);
        final String type = payload.getString(TYPE);
        final LocalDate startDate = LocalDates.from(payload.getString(START_DATE));
        final Integer estimateMinutes = payload.getInt(ESTIMATE_MINUTES);
        // Fields that may not have a value
        final LocalTime startTime = getStartTime(payload);
        final boolean notBefore = payload.getBoolean(NOT_BEFORE, false);
        final String judgeId = payload.getString(JUDGE_ID, null);
        final String courtRoomId = payload.getString(COURT_ROOM_ID, null);

        updateHearingEventStream(command, hearingId, (Hearing hearing) -> {
            final Stream<Object> typeEvents = hearing.changeType(type, hearingId);
            final Stream<Object> startDateEvents = hearing.changeStartDate(startDate, hearingId);
            final Stream<Object> estimateEvents = hearing.changeEstimate(estimateMinutes, hearingId);

            final Stream<Object> startTimeEvents = startTime != null ?
                    hearing.assignStartTime(startTime, hearingId) : hearing.removeStartTime(hearingId);

            final Stream<Object> notBeforeEvents = hearing.selectNotBefore(notBefore, hearingId);

            // Check judge and court-room last as these are the key fields for allocation
            final Stream<Object> judgeEvents = judgeId != null ?
                    hearing.assignJudge(judgeId, hearingId) : hearing.removeJudge(hearingId);

            final Stream<Object> courtRoomEvents = courtRoomId != null ?
                    hearing.assignCourtRoom(courtRoomId, hearingId) : hearing.removeCourtRoom(hearingId);

            final Stream<Object> allocationEvents = hearing.applyAllocationRules();

            return Stream.of(typeEvents, startDateEvents, estimateEvents, startTimeEvents,
                    notBeforeEvents, judgeEvents, courtRoomEvents, allocationEvents).flatMap(i -> i);
        });
    }

    private void updateHearingEventStream(final JsonEnvelope command, final String hearingId,
                                   final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(UUID.fromString(hearingId));
        final Hearing hearing = aggregateService.get(eventStream, Hearing.class);

        final Stream<Object> events = aggregatorFunction.apply(hearing);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    private void updateCaseEventStream(final JsonEnvelope command, final String caseId,
                                          final Function<Case, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(UUID.fromString(caseId));
        final Case listingCase = aggregateService.get(eventStream, Case.class);

        final Stream<Object> events = aggregatorFunction.apply(listingCase);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    private List<uk.gov.moj.cpp.listing.domain.Hearing> createHearingsFrom(final JsonObject caseJson) {
        return JsonDomainUtils.createHearingsFrom(caseJson);
    }

    private List<Defendant> createDefendantsFrom(JsonObject hearingJson) {
        return JsonDomainUtils.createDefendantsFrom(hearingJson);
    }

    private LocalTime getStartTime(JsonObject payload) {
        return payload.containsKey(START_TIME) ? LocalTime.parse(payload.getString(START_TIME)) : null;
    }

}
