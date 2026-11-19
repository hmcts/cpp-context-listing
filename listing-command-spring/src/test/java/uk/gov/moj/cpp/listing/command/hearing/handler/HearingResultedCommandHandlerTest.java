package uk.gov.moj.cpp.listing.command.hearing.handler;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.AggregateService;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventSource;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventStream;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;

import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import jakarta.json.JsonObject;
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
    private uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow hearingFlow;

    @BeforeEach
    void bridgeMigratedAggregates() {
        uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.HearingFlowTestBridge.bridge(hearingFlow, eventSource, aggregateService);
    }

    @Mock
    private EventStream eventStream;

    @Mock
    private HearingRules hearing;

    @InjectMocks
    private HearingCommandHandler hearingResultedCommandHandler;

    private Envelope command;
    private UUID hearingId;

    @BeforeEach
    void setUp() {
        hearingId = UUID.randomUUID();
        JsonObject payload = JsonObjects.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();
        command = mock(Envelope.class);
        when(command.payload()).thenReturn(payload);
    }

    @Test
    void shouldHandleSetHearingResultStatus() {
        when(eventSource.getStreamById(hearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearing);
        when(hearing.setHearingResultStatus(hearingId)).thenReturn(Stream.empty());

        hearingResultedCommandHandler.handleSetHearingResultStatus(command);

        verify(eventSource).getStreamById(hearingId);
        verify(aggregateService).get(eventStream, HearingRules.class);
        verify(hearing).setHearingResultStatus(hearingId);
        verify(eventStream).append(any());
    }


}