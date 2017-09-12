package uk.gov.moj.cpp.listing.command.handler;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CourtCentreAdded;
import uk.gov.moj.cpp.listing.event.CourtRoomAdded;
import uk.gov.moj.cpp.listing.event.JudgeAdded;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

@ServiceComponent(COMMAND_HANDLER)
public class ReferenceDataCommandHandler {

    @Inject
    EventSource eventSource;

    @Inject
    Enveloper enveloper;

    @Handles("listing.command.add-judge")
    public void addJudge(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID streamId = command.metadata().id();
        final Stream<JudgeAdded> events = Stream.of(createJudgeFrom(payload));
        eventSource.getStreamById(streamId).append(events.map(enveloper.withMetadataFrom(command)));

    }

    private JudgeAdded createJudgeFrom(final JsonObject command) {
        return new JudgeAdded(
                getStringOrNull(command, "id"),
                getStringOrNull(command, "title"),
                getStringOrNull(command, "firstName"),
                getStringOrNull(command, "lastName")
        );
    }

    @Handles("listing.command.add-court-centre")
    public void addCourtCenter(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID streamId = command.metadata().id();
        final Stream<CourtCentreAdded> events = Stream.of(createCourtCentreFrom(payload));
        eventSource.getStreamById(streamId).append(events.map(enveloper.withMetadataFrom(command)));

    }

    private CourtCentreAdded createCourtCentreFrom(final JsonObject command) {
        return new CourtCentreAdded(
                getStringOrNull(command, "id"),
                getStringOrNull(command, "courtCentreName")
        );
    }

    @Handles("listing.command.add-court-room")
    public void addCourtRoom(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID streamId = command.metadata().id();
        final Stream<CourtRoomAdded> events = Stream.of(createCourtRoomFrom(payload));
        eventSource.getStreamById(streamId).append(events.map(enveloper.withMetadataFrom(command)));

    }

    private CourtRoomAdded createCourtRoomFrom(final JsonObject command) {
        return new CourtRoomAdded(
                getStringOrNull(command, "id"),
                getStringOrNull(command, "courtCentre"),
                getStringOrNull(command, "courtRoomName")
        );
    }

    private String getStringOrNull(final JsonObject object, final String fieldName) {
        return getString(object, fieldName).orElse(null);
    }

}
