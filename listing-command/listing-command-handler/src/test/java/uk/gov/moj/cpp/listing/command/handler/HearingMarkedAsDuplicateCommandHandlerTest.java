package uk.gov.moj.cpp.listing.command.handler;


import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.command.utils.FileUtil;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingMarkedAsDuplicateCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            HearingMarkedAsDuplicate.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private Stream<Object> events;

    @Mock
    private Hearing hearingAggregate;

    @Mock
    private Case caseAggregate;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    @Spy
    private HearingMarkedAsDuplicateCommandHandler hearingMarkedAsDuplicateCommandHandler;

    @Test
    public void shouldHandleHearingMarkedAsDuplicate() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID case1Id = UUID.randomUUID();
        final UUID case2Id = UUID.randomUUID();

        final List<UUID> caseIds = Arrays.asList(case1Id, case2Id);
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.mark-hearing-as-duplicate.json").toString()
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE1_ID", case1Id.toString())
                .replace("CASE2_ID", case2Id.toString());


        final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.mark-hearing-as-duplicate", jsonReader.readObject());

        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(hearingAggregate.markHearingAsDuplicate(eq(hearingId), eq(caseIds))).thenReturn(mock(Stream.class));

        hearingMarkedAsDuplicateCommandHandler.handleMarkHearingAsDuplicate(commandEnvelope);

        verify(hearingAggregate).markHearingAsDuplicate(eq(hearingId), eq(caseIds));
        verify(hearingAggregate).deleteHearingForHmi();
    }

    @Test
    public void shouldHandleUnallocatedHearingMarkedAsDuplicate() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.mark-unallocated-hearing-as-duplicate",
                createObjectBuilder()
                        .add("hearingId", hearingId.toString())
                        .build());

        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(new Hearing());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        hearingMarkedAsDuplicateCommandHandler.handleMarkUnallocatedHearingAsDuplicate(commandEnvelope);

        final JsonEnvelope actualEventProduced = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList()).get(0);
        assertThat(actualEventProduced.metadata().name(), is("listing.events.hearing-marked-as-duplicate"));
        assertThat(actualEventProduced.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldHandleHearingMarkedAsDuplicateForCase() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.mark-hearing-as-duplicate-for-case.json").toString()
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID", caseId.toString());


        final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.mark-hearing-as-duplicate-for-case", jsonReader.readObject());

        when(aggregateService.get(eventStream, Case.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(caseAggregate.markHearingAsDuplicate(eq(hearingId), eq(caseId))).thenReturn(mock(Stream.class));

        hearingMarkedAsDuplicateCommandHandler.handleMarkHearingAsDuplicateForCase(commandEnvelope);

        verify(caseAggregate).markHearingAsDuplicate(eq(hearingId), eq(caseId));
    }
}
