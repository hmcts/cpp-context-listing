package uk.gov.moj.cpp.listing.command.handler;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HearingResultedCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private Hearing hearing;

    @InjectMocks
    private HearingResultedCommandHandler hearingResultedCommandHandler;

    private JsonEnvelope command;
    private UUID hearingId;

    @BeforeEach
    void setUp() {
        hearingId = UUID.randomUUID();
        JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();
        command = mock(JsonEnvelope.class);
        when(command.payloadAsJsonObject()).thenReturn(payload);
    }

    @Test
    void shouldHandleSetHearingResultStatus() throws EventStreamException {
        when(eventSource.getStreamById(hearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearing);
        when(hearing.setHearingResultStatus(hearingId)).thenReturn(Stream.empty());

        hearingResultedCommandHandler.handleSetHearingResultStatus(command);

        verify(eventSource).getStreamById(hearingId);
        verify(aggregateService).get(eventStream, Hearing.class);
        verify(hearing).setHearingResultStatus(hearingId);
        verify(eventStream).append(any());
    }


}