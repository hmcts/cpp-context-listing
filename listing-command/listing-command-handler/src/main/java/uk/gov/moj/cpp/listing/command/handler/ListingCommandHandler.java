package uk.gov.moj.cpp.listing.command.handler;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
public class ListingCommandHandler {
    private static final boolean UNALLOCATED = false;

    @Inject
    EventSource eventSource;

    @Inject
    Enveloper enveloper;

    @Handles("listing.command.send-case-for-listing")
    public void sendCaseForListing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID streamId = command.metadata().id();
        final Stream<CaseSentForListing> events = Stream.of(createCaseSentForListingFrom(payload));
        eventSource.getStreamById(streamId).append(events.map(enveloper.withMetadataFrom(command)));

    }

    private CaseSentForListing createCaseSentForListingFrom(final JsonObject command) {
        return new CaseSentForListing(
                getStringOrNull(command, "caseId"),
                getStringOrNull(command, "urn"),
                getLocalDate(command, "sendingCommittalDate"),
                createHearingsFrom(command)
        );
    }

    private List<Defendant> createDefendantsFrom(JsonObject hearing) {
        return hearing.getJsonArray("defendants")
                .getValuesAs(JsonObject.class).stream()
                .map(this::createDefendantFrom)
                .collect(toList());
    }

    private Defendant createDefendantFrom(final JsonObject defendant) {
        return new Defendant(
                getStringOrNull(defendant, "id"),
                getStringOrNull(defendant, "personId"),
                getStringOrNull(defendant, "firstName"),
                getStringOrNull(defendant, "lastName"),
                getLocalDate(defendant, "dateOfBirth"),
                getStringOrNull(defendant, "bailStatus"),
                getStringOrNull(defendant, "defenceOrganisation"),
                createOffencesFrom(defendant)
        );
    }


    private List<Offence> createOffencesFrom(JsonObject defendant) {
        return defendant.getJsonArray("offences")
                .getValuesAs(JsonObject.class).stream()
                .map(this::createOffenceFrom)
                .collect(toList());
    }

    private Offence createOffenceFrom(final JsonObject offence) {
        return new Offence(
                getStringOrNull(offence, "id"),
                getStringOrNull(offence, "offenceCode"),
                getLocalDate(offence, "startDate"),
                getLocalDate(offence, "endDate"),
                createStatementOfOffenceFrom(offence)
        );
    }

    private StatementOfOffence createStatementOfOffenceFrom(final JsonObject offence) {
        final JsonObject statementOfOffenceJsonObject = offence.getJsonObject("statementOfOffence");
        return new StatementOfOffence(
                getStringOrNull(statementOfOffenceJsonObject, "title"),
                getStringOrNull(statementOfOffenceJsonObject, "legislation")
        );
    }

    private List<Hearing> createHearingsFrom(final JsonObject command) {
        return command.getJsonArray("hearings")
                .getValuesAs(JsonObject.class).stream()
                .map(this::createHearingFrom)
                .collect(toList());
    }



    private Hearing createHearingFrom(JsonObject hearing) {
        final String hearingType = getString(hearing, "type").get();
        final Integer estimateMinutes = hearing.getInt("estimateMinutes", getHearingEstimateMinutes());
        return new Hearing(
                getStringOrNull(hearing, "id"),
                getStringOrNull(hearing, "courtCentreId"),
                hearingType,
                getLocalDate(hearing, "startDate"),
                estimateMinutes,
                UNALLOCATED,
                createDefendantsFrom(hearing)
        );
    }

    private String getStringOrNull(final JsonObject object, final String fieldName) {
        return getString(object, fieldName).orElse(null);
    }

    private LocalDate getLocalDate(JsonObject command, final String fieldName) {
        return getString(command, fieldName)
                .map(LocalDate::parse).orElse(null);
    }

    private int getHearingEstimateMinutes() {
        return 15;
    }
}
