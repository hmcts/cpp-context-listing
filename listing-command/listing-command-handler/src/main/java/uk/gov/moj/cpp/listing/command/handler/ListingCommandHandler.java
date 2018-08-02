package uk.gov.moj.cpp.listing.command.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.gov.moj.cpp.listing.domain.*;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

@ServiceComponent(COMMAND_HANDLER)
@SuppressWarnings({"squid:S1188"})
public class ListingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingCommandHandler.class);

    private static final String HEARING_ID = "hearingId";
    private static final String TYPE = "type";
    private static final String URN = "urn";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String ESTIMATE_MINUTES = "estimateMinutes";
    private static final String JUDGE_ID = "judgeId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String START_TIME = "startTime";
    private static final String CASE_ID = "caseId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String START_TIMES = "startTimes";
    private static final String NON_SITTING_DAYS = "nonSittingDays";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;


    @Handles("listing.command.send-case-for-listing")
    public void sendCaseForListing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.send-case-for-listing' received with payload {}", payload);

        final UUID caseId = fromString(payload.getString(CASE_ID));
        final String urn = payload.getString(URN);
        final List<uk.gov.moj.cpp.listing.domain.Hearing> hearings = createHearingsFrom(payload);

        updateCaseEventStream(command, caseId, (Case listingCase) ->
                listingCase.sendForListing(caseId, urn, hearings));
    }

    @Handles("listing.command.list-hearing")
    public void listHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.list-hearing' received with payload {}", payload);

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final String type = payload.getString(TYPE);
        final String urn = payload.getString(URN);
        final LocalDate startDate = LocalDates.from(payload.getString(START_DATE));
        final Integer estimateMinutes = payload.getInt(ESTIMATE_MINUTES);
        final UUID caseId = fromString(payload.getString(CASE_ID));
        final UUID courtCentreId = fromString(payload.getString(COURT_CENTRE_ID));
        final List<Defendant> defendants = createDefendantsFrom(payload);

        //optional occurs on relisting
        final LocalDate endDate = getLocalDateOrNull(payload.getString(END_DATE, null));
        final UUID judgeId = getUUIDOrNull(payload.getString(JUDGE_ID, null));
        final UUID courtRoomId = getUUIDOrNull(payload.getString(COURT_ROOM_ID, null));
        final LocalTime startTime = getStartTime(payload);

        updateHearingEventStream(command, hearingId, (Hearing hearing) -> {
            final Stream<Object> listingEvents  = hearing.list(hearingId, type,
                        startDate, estimateMinutes, caseId, urn, courtCentreId,
                        defendants, judgeId,  courtRoomId, startTime, endDate);
            final Stream<Object> allocationEvents = hearing.applyAllocationRules();
            return Stream.of(listingEvents, allocationEvents).flatMap(i -> i);
        });
    }

    @Handles("listing.command.handler.update-hearing-for-listing")
    public void updateHearingForListing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.handler.update-hearing-for-listing' received with payload {}", payload);

        // Mandatory fields that always require a value
        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final String type = payload.getString(TYPE);
        final LocalDate startDate = LocalDates.from(payload.getString(START_DATE));
        final LocalDate endDate = LocalDates.from(payload.getString(END_DATE));
        final List<ZonedDateTime> startTimes = getStartTimes(payload);
        final List<LocalDate> nonSittingDays = getNonSittingDays(payload);

        // Fields that may not have a value
        final UUID judgeId = getUUIDOrNull(payload.getString(JUDGE_ID, null));
        final UUID courtRoomId = getUUIDOrNull(payload.getString(COURT_ROOM_ID, null));

        updateHearingEventStream(command, hearingId, (Hearing hearing) -> {
            final Stream<Object> typeEvents = hearing.changeType(type, hearingId);
            final Stream<Object> startDateEvents = hearing.changeStartDate(startDate, hearingId);
            final Stream<Object> startTimesEvents = hearing.assignStartTimes(startTimes, hearingId);
            final Stream<Object> endDateEvents = hearing.assignEndDate(endDate, hearingId);
            final Stream<Object> nonSittingDaysEvents = hearing.assignNonSittingDays(nonSittingDays, hearingId);

            // Check judge and court-room last as these are the key fields for allocation
            final Stream<Object> judgeEvents = judgeId != null ?
                    hearing.assignJudge(judgeId, hearingId) : hearing.removeJudge(hearingId);

            final Stream<Object> courtRoomEvents = courtRoomId != null ?
                    hearing.assignCourtRoom(courtRoomId, hearingId) : hearing.removeCourtRoom(hearingId);

            final Stream<Object> allocationEvents = hearing.applyAllocationRules();

            return Stream.of(typeEvents, startDateEvents, startTimesEvents, endDateEvents, nonSittingDaysEvents, judgeEvents, courtRoomEvents, allocationEvents).flatMap(i -> i);
        });
    }

    @Handles("listing.command.update-case-defendant-details")
    public void updateCaseDefendantDetails(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.update-case-defendant-details' received with payload {}", payload);

        final UUID caseId = fromString(payload.getString(CASE_ID));
        final List<Defendant> defendants = createDefendantsFromProgression(payload);

        updateCaseEventStream(command, caseId, (Case listingCase) ->
                listingCase.updateDefendants(defendants));
    }

    @Handles("listing.command.update-case-defendant-offences")
    public void updateCaseDefendantOffences(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.update-case-defendant-offences' received with payload {}", payload);

        final List<CaseOffences> updatedOffences = createUpdatedCaseBaseOffencesForCase(payload);

        for (final CaseOffences caseBaseOffences : updatedOffences) {
            final UUID caseId = caseBaseOffences.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.updateDefendantOffences(caseBaseOffences)
            );
        }

        final List<CaseSimpleOffences> deleteOffences = createDeletedSimpleOffencesForCase(payload);

        for (final CaseSimpleOffences caseSimpleOffences : deleteOffences) {
            final UUID caseId = caseSimpleOffences.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.deleteDefendantOffences(caseSimpleOffences)
            );
        }

        final List<CaseOffences> addedOffences = createAddedOffencesForCase(payload);

        for (final CaseOffences caseBaseOffences : addedOffences) {
            final UUID caseId = caseBaseOffences.getCaseId();
            updateCaseEventStream(command, caseId, (Case listingCase) ->
                    listingCase.addedDefendantOffences(caseBaseOffences)
            );
        }
    }

    @Handles("listing.command.update-defendants-for-hearing")
    public void updateDefendantsForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.update-defendants-for-hearing' received with payload {}", payload);

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final List<Defendant> defendants = createDefendantsFrom(payload);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.updateDefendants(defendants));
    }

    @Handles("listing.command.update-offences-for-hearing")
    public void updateOffencesForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload {}", payload);

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final List<Offence> offences = createUpdatedOffencesForHearing(payload);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.updateOffences(offences));
    }

    @Handles("listing.command.delete-offences-for-hearing")
    public void deleteOffencesForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload %{}", payload);

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final List<SimpleOffence> offences = createDeletedOffencesForHearing(payload);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.deleteOffences(offences));
    }

    @Handles("listing.command.add-offences-for-hearing")
    public void addOffencesForHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        LOGGER.debug("'listing.command.update-offences-for-hearing' received with payload {}", payload);

        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final List<Offence> offences = createAddedOffencesForHearing(payload);

        updateHearingEventStream(command, hearingId, (Hearing hearing) ->
                hearing.addOffences(offences));
    }


    private List<ZonedDateTime> getStartTimes(JsonObject payload) {
        return payload.getJsonArray(START_TIMES).stream()
                    .map(json -> (ZonedDateTime.parse(((JsonString) json).getString())))
                    .collect(Collectors.toList());
    }

   
    private List<LocalDate> getNonSittingDays(JsonObject payload) {
        return payload.getJsonArray(NON_SITTING_DAYS).stream()
                .map(json -> LocalDate.parse(((JsonString) json).getString()))
                .collect(Collectors.toList());
    }




    private void updateHearingEventStream(final JsonEnvelope command, final UUID hearingId,
                                   final Function<Hearing, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final Hearing hearing = aggregateService.get(eventStream, Hearing.class);

        final Stream<Object> events = aggregatorFunction.apply(hearing);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    private void updateCaseEventStream(final JsonEnvelope command, final UUID caseId,
                                          final Function<Case, Stream<Object>> aggregatorFunction) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final Case listingCase = aggregateService.get(eventStream, Case.class);

        final Stream<Object> events = aggregatorFunction.apply(listingCase);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    private List<uk.gov.moj.cpp.listing.domain.Hearing> createHearingsFrom(final JsonObject caseJson) {
        return JsonDomainUtils.createHearingsFrom(caseJson);
    }

    private List<Defendant> createDefendantsFrom(final JsonObject hearingJson) {
        return JsonDomainUtils.createDefendantsFrom(hearingJson);
    }

    private List<Defendant> createDefendantsFromProgression(final JsonObject progressionDefendantsChanged) {
        return JsonDomainUtils.createDefendantsFromProgression(progressionDefendantsChanged);
    }

    private List<CaseOffences> createUpdatedCaseBaseOffencesForCase(JsonObject payload) {
        return JsonDomainUtils.createUpdatedCaseBasesOffencesFrom(payload);
    }

    private List<Offence> createUpdatedOffencesForHearing(JsonObject payload) {
        return JsonDomainUtils.createUpdatedOffencesFrom(payload);
    }

    private List<CaseSimpleOffences> createDeletedSimpleOffencesForCase(JsonObject payload) {
        return JsonDomainUtils.createDeletedCaseSimpleOffencesFrom(payload);
    }

    private List<SimpleOffence> createDeletedOffencesForHearing(JsonObject payload) {
        return JsonDomainUtils.createDeletedSimpleOffencesFrom(payload);
    }

    private List<CaseOffences> createAddedOffencesForCase(JsonObject payload) {
        return JsonDomainUtils.createAddedCaseOffencesFrom(payload);
    }

    private List<Offence> createAddedOffencesForHearing(JsonObject payload) {
        return JsonDomainUtils.createAddedOffencesFrom(payload);
    }

    private LocalTime getStartTime(final JsonObject payload) {
        return payload.containsKey(START_TIME) ? LocalTime.parse(payload.getString(START_TIME)) : null;
    }

    private UUID getUUIDOrNull(final String uuid) {
        if (uuid!=null && !uuid.isEmpty()) {
            return fromString(uuid);
        }
        return null;
    }

    private LocalDate getLocalDateOrNull(final String localDate){
        if(localDate!=null){
            return LocalDate.parse(localDate);
        }
        return null;
    }
}
