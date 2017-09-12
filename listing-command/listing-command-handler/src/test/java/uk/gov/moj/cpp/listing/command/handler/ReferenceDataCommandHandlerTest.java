package uk.gov.moj.cpp.listing.command.handler;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.listing.event.CourtCentreAdded;
import uk.gov.moj.cpp.listing.event.CourtRoomAdded;
import uk.gov.moj.cpp.listing.event.JudgeAdded;

import javax.json.JsonObject;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataCommandHandlerTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String TITLE = "HHS";
    private static final String FIRST_NAME = "Test Recipe";
    private static final String LAST_NAME = "Last Name";
    public static final String COURT_CENTRE = "Liverpool";
    public static final String COURT_CENTRE_ROOM = "Court room 1";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(JudgeAdded.class,
            CourtCentreAdded.class, CourtRoomAdded.class);

    @InjectMocks
    private ReferenceDataCommandHandler referenceDataCommandHandler;

    @Test
    public void referenceDataCommandHandlerShouldTriggerJudgeAddedEvent() throws Exception {
        final JsonEnvelope commandEnvelope = addJudgeCommandEnvelop(ID);

        JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();

        when(eventSource.getStreamById(commandEnvelope.metadata().id())).thenReturn(eventStream);

        referenceDataCommandHandler.addJudge(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.events.judge-added")
                                .withCausationIds(commandEnvelope.metadata().id()),payload()
                                .isJson(allOf(
                                    withJsonPath("$.id", equalTo(commandPayload.getString("id"))),
                                    withJsonPath("$.title", equalTo(commandPayload.getString("title"))),
                                    withJsonPath("$.firstName", equalTo(commandPayload.getString("firstName"))),
                                    withJsonPath("$.lastName", equalTo(commandPayload.getString("lastName")))
                                )
                        )
                )
            )
        );
    }


    @Test
    public void referenceDataCommandHandlerShouldTriggerCourtCentreAddedEvent() throws Exception {
        final JsonEnvelope commandEnvelope = addCourtCentreCommandEnvelop(ID);

        JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();

        when(eventSource.getStreamById(commandEnvelope.metadata().id())).thenReturn(eventStream);

        referenceDataCommandHandler.addCourtCenter(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.events.court-centre-added")
                                .withCausationIds(commandEnvelope.metadata().id()),payload()
                                .isJson(allOf(
                                        withJsonPath("$.id", equalTo(commandPayload.getString("id"))),
                                        withJsonPath("$.courtCentreName", equalTo(commandPayload.getString("courtCentreName")))
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void referenceDataCommandHandlerShouldTriggerCourtRoomAddedEvent() throws Exception {
        final JsonEnvelope commandEnvelope = addCourtRoomCommandEnvelop(ID);

        JsonObject commandPayload = commandEnvelope.payloadAsJsonObject();

        when(eventSource.getStreamById(commandEnvelope.metadata().id())).thenReturn(eventStream);

        referenceDataCommandHandler.addCourtRoom(commandEnvelope);

        MatcherAssert.assertThat(verifyAppendAndGetArgumentFrom(eventStream),
                streamContaining(jsonEnvelope(
                        withMetadataEnvelopedFrom(commandEnvelope)
                                .withName("listing.events.court-room-added")
                                .withCausationIds(commandEnvelope.metadata().id()),payload()
                                .isJson(allOf(
                                        withJsonPath("$.id", equalTo(commandPayload.getString("id"))),
                                        withJsonPath("$.courtCentre", equalTo(commandPayload.getString("courtCentre"))),
                                        withJsonPath("$.courtRoomName", equalTo(commandPayload.getString("courtRoomName")))
                                        )
                                )
                        )
                )
        );
    }

    private JsonEnvelope addJudgeCommandEnvelop(final UUID id) {

        JsonObject judgeJson = createJudgeJson();
        return createEnvelope("listing.command.add-judge" , judgeJson);
    }

    private JsonObject createJudgeJson() {
        return createObjectBuilder()
                .add("id", ID.toString())
                .add("title", TITLE)
                .add("firstName", FIRST_NAME)
                .add("lastName", LAST_NAME)
                .build();

    }

    private JsonEnvelope addCourtCentreCommandEnvelop(final UUID id) {

        JsonObject courtCentreJson = createCourtCentreJson();
        return createEnvelope("listing.command.add-court-centre" , courtCentreJson);
    }

    private JsonObject createCourtCentreJson() {
        return createObjectBuilder()
                .add("id", ID.toString())
                .add("courtCentreName", COURT_CENTRE)
                .build();

    }

    private JsonEnvelope addCourtRoomCommandEnvelop(final UUID id) {

        JsonObject courtRoomJson = createCourtRoomJson();
        return createEnvelope("listing.command.add-court-room" , courtRoomJson);
    }

    private JsonObject createCourtRoomJson() {
        return createObjectBuilder()
                .add("id", ID.toString())
                .add("courtCentre", COURT_CENTRE)
                .add("courtRoomName", COURT_CENTRE_ROOM)
                .build();

    }

}
