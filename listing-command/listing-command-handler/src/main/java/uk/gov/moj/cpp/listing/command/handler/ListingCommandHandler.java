package uk.gov.moj.cpp.listing.command.handler;

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
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188", "squid:S1135", "squid:CommentedOutCodeLine"})
public class ListingCommandHandler {

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
        final String caseId = payload.getString(CASE_ID);
        updateCaseEventStream(command, caseId, (CaseAggregate listingCase) -> {
            final String urn = payload.getString(URN);
            final List<Hearing> hearings = createHearingsFrom(payload);
            return listingCase.sendForListing(caseId, urn, hearings);
        });
    }

    @Handles("listing.command.list-hearing")
    public void listHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final String hearingId = payload.getString(HEARING_ID);
        updateHearingEventStream(command, hearingId, (HearingAggregate hearing) -> {
            final String type = payload.getString(TYPE);
            final LocalDate startDate = LocalDates.from(payload.getString(START_DATE));
            final Integer estimateMinutes = payload.getInt(ESTIMATE_MINUTES);
            final String caseId = payload.getString(CASE_ID);
            final String courtCentreId = payload.getString(COURT_CENTRE_ID);
            final List<Defendant> defendants = createDefendantsFrom(payload);
            return hearing.list(hearingId, type, startDate, estimateMinutes, caseId, courtCentreId, defendants);
        });
    }

    @Handles("listing.command.update-hearing-for-listing")
    public void updateHearingForListing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final String hearingId = payload.getString(HEARING_ID);
        updateHearingEventStream(command, hearingId, (HearingAggregate hearing) -> {
            final String type = payload.getString(TYPE);
            final Stream<Object> typeEvents = hearing.changeType(type, hearingId);
            final LocalDate startDate = LocalDates.from(payload.getString(START_DATE));
            final Stream<Object> startDateEvents = hearing.changeStartDate(startDate, hearingId);
            final Integer estimateMinutes = payload.getInt(ESTIMATE_MINUTES);
            final Stream<Object> estimateEvents = hearing.changeEstimate(estimateMinutes, hearingId);

            Stream<Object> startTimeEvents = Stream.empty();
            if (payload.containsKey(START_TIME)) {
                final LocalTime startTime = LocalTime.parse(payload.getString(START_TIME));
                startTimeEvents = startTime != null ? hearing.assignStartTime(startTime, hearingId) : hearing.removeStartTime(hearingId);
            }

            Stream<Object> notBeforeEvents = Stream.empty();
            if (payload.containsKey(NOT_BEFORE)) {
                final boolean notBefore = payload.getBoolean(NOT_BEFORE);
                notBeforeEvents = hearing.selectNotBefore(notBefore, hearingId);
            }

            // Check judge and court-room last as these are the key fields for allocation
            Stream<Object> judgeEvents = Stream.empty();
            if (payload.containsKey(JUDGE_ID)) {
                final String judgeId = payload.getString(JUDGE_ID);
                judgeEvents = judgeId != null ? hearing.assignJudge(judgeId, hearingId) : hearing.removeJudge(hearingId);
            }

            Stream<Object> courtRoomEvents = Stream.empty();
            if (payload.containsKey(COURT_ROOM_ID)) {
                final String courtRoomId = payload.getString(COURT_ROOM_ID);
                courtRoomEvents = courtRoomId != null ? hearing.assignCourtRoom(courtRoomId, hearingId) : hearing.removeCourtRoom(hearingId);
            }

            return Stream.of(typeEvents, startDateEvents, estimateEvents,
                    startTimeEvents, notBeforeEvents, judgeEvents, courtRoomEvents).flatMap(i -> i);
        });
    }

    private void updateHearingEventStream(final JsonEnvelope command, final String hearingId,
                                   final Function<HearingAggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(UUID.fromString(hearingId));
        final HearingAggregate hearing = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events = aggregatorFunction.apply(hearing);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    private void updateCaseEventStream(final JsonEnvelope command, final String caseId,
                                          final Function<CaseAggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(UUID.fromString(caseId));
        final CaseAggregate listingCase = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = aggregatorFunction.apply(listingCase);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }


    // TODO: Must be able to combine both of these into one method

/*    private void updateEventStream(final JsonEnvelope command, final String aggregateId, Class aggregateClass,
                                       final Function<Aggregate, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(UUID.fromString(aggregateId));
        final Aggregate aggregate = aggregateService.get(eventStream, aggregateClass);

        final Stream<Object> events = aggregatorFunction.apply(aggregate);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }*/


    private List<Hearing> createHearingsFrom(final JsonObject caseJson) {
        return JsonDomainUtils.createHearingsFrom(caseJson);
    }

    private List<Defendant> createDefendantsFrom(JsonObject hearingJson) {
        return JsonDomainUtils.createDefendantsFrom(hearingJson);
    }

}
