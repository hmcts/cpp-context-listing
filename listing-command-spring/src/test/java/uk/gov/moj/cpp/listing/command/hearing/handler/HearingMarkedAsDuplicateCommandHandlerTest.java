package uk.gov.moj.cpp.listing.command.hearing.handler;


import org.junit.jupiter.api.BeforeEach;
import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.HearingFlowTestBridge;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.createEnvelope;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.AggregateService;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventSource;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventStream;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.FileUtil;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.command.hearing.rules.CaseRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import jakarta.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


/** Ported from the JEE {@code HearingMarkedAsDuplicateCommandHandlerTest} (MBD refactor P2.3.3, ADR 015). */
@ExtendWith(MockitoExtension.class)
public class HearingMarkedAsDuplicateCommandHandlerTest {


    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingFlow hearingFlow;

    @BeforeEach
    void bridgeMigratedAggregates() {
        HearingFlowTestBridge.bridge(hearingFlow, eventSource, aggregateService);
    }

    @Mock
    private Stream<Object> events;

    @Mock
    private HearingRules hearingAggregate;

    @Mock
    private CaseRules caseAggregate;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private CaseCommandHandler caseCommandHandler;

    @InjectMocks
    @Spy
    private HearingCommandHandler hearingMarkedAsDuplicateCommandHandler;

    @Test
    public void shouldHandleHearingMarkedAsDuplicate() {
        final UUID hearingId = UUID.randomUUID();
        final UUID case1Id = UUID.randomUUID();
        final UUID case2Id = UUID.randomUUID();

        final List<UUID> caseIds = Arrays.asList(case1Id, case2Id);
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.mark-hearing-as-duplicate.json").toString()
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE1_ID", case1Id.toString())
                .replace("CASE2_ID", case2Id.toString());


        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonString));
        final Envelope commandEnvelope = createEnvelope("listing.command.mark-hearing-as-duplicate", jsonReader.readObject());

        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(hearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(hearingAggregate.markHearingAsDuplicate(eq(hearingId), eq(caseIds))).thenReturn(mock(Stream.class));


        hearingMarkedAsDuplicateCommandHandler.markHearingAsDuplicate(commandEnvelope);

        verify(hearingAggregate).markHearingAsDuplicate(eq(hearingId), eq(caseIds));
    }

    @Test
    public void shouldHandleUnallocatedHearingMarkedAsDuplicate() {
        final UUID hearingId = UUID.randomUUID();

        final Envelope commandEnvelope = createEnvelope("listing.command.mark-unallocated-hearing-as-duplicate",
                createObjectBuilder()
                        .add("hearingId", hearingId.toString())
                        .build());

        when(aggregateService.get(eventStream, HearingRules.class)).thenReturn(new HearingRules());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        hearingMarkedAsDuplicateCommandHandler.markUnallocatedHearingAsDuplicate(commandEnvelope);

        final Envelope actualEventProduced = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList()).get(0);
        assertThat(actualEventProduced.metadata().name(), is("listing.events.hearing-marked-as-duplicate"));
        assertThat(actualEventProduced.payload().getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldHandleHearingMarkedAsDuplicateForCase() {
        final UUID hearingId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.mark-hearing-as-duplicate-for-case.json").toString()
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID", caseId.toString());


        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonString));
        final Envelope commandEnvelope = createEnvelope("listing.command.mark-hearing-as-duplicate-for-case", jsonReader.readObject());

        when(aggregateService.get(eventStream, CaseRules.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(caseAggregate.markHearingAsDuplicate(eq(hearingId), eq(caseId))).thenReturn(mock(Stream.class));

        caseCommandHandler.handleMarkHearingAsDuplicateForCase(commandEnvelope);

        verify(caseAggregate).markHearingAsDuplicate(eq(hearingId), eq(caseId));
    }
}
