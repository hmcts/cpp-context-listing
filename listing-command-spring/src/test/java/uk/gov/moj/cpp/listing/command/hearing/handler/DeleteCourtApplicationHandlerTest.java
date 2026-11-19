package uk.gov.moj.cpp.listing.command.hearing.handler;

import org.junit.jupiter.api.BeforeEach;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.createEnvelope;

import uk.gov.justice.listing.event.CourtApplicationHearingDeleted;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.AggregateService;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventSource;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventStream;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;

import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteCourtApplicationHandlerTest {


    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow hearingFlow;

    @BeforeEach
    void bridgeMigratedAggregates() {
        uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.HearingFlowTestBridge.bridge(hearingFlow, eventSource, aggregateService);
    }

    @Mock
    private Stream<Object> events;

    @Mock
    private HearingRules hearingAggregate;

    @InjectMocks
    private HearingCommandHandler deleteCourtApplicationHandler;

    @Test
    public void shouldHandleDeleteCourtApplication() {
        final UUID hearingId = randomUUID();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.deleteCourtApplicationHearing(hearingId)).thenReturn(mock(Stream.class));
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();
        final Envelope commandEnvelope = createEnvelope("listing.command.delete-court-application-hearing", payload);


        deleteCourtApplicationHandler.handleDeleteCourtApplicationHearing(commandEnvelope);

        verify(hearingAggregate).deleteCourtApplicationHearing(hearingId);
    }

}
