package uk.gov.moj.cpp.listing.command.handler;


import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.UpdateExistingHearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;
import javax.json.JsonObject;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class UpdateExistingHearingCommandHandlerTest {

    public static final UUID HEARING_ID = randomUUID();
    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            HearingListed.class,
            UpdateExistingHearingRequested.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private SeedHearingAggregate seedHearingAggregate;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private UpdateExistingHearingCommandHandler updateExistingHearingCommandHandler;

    @Test
    public void shouldHandleListNextCourtHearings() throws EventStreamException {
        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.update-existing-hearing", commandPayload);
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();
        final List<UUID> shadowListedOffences = Arrays.asList(offenceId);
        final String sittingDay = LocalDate.now().toString();

        final UpdateExistingHearing updateExistingHearing = UpdateExistingHearing.updateExistingHearing()
                .withHearingId(hearingId)
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSittingDay(sittingDay)
                        .withSeedingHearingId(seedingHearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .build())
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .build()))
                .withShadowListedOffences(shadowListedOffences)
                .build();


        when(jsonObjectConverter.convert(commandPayload, UpdateExistingHearing.class))
                .thenReturn(updateExistingHearing);
        when(aggregateService.get(eventStream, SeedHearingAggregate.class)).thenReturn(seedHearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        final ArgumentCaptor<UUID> hearingIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<List> prosecutionCasesCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List> shadowOffencesCaptor = ArgumentCaptor.forClass(List.class);

        updateExistingHearingCommandHandler.updateExistingHearing(commandEnvelope);

        verify(seedHearingAggregate).requestUpdateExistingHearing(eq(seedingHearingId), hearingIdCaptor.capture(), eq(sittingDay), prosecutionCasesCaptor.capture(), shadowOffencesCaptor.capture());

        assertThat(hearingIdCaptor.getValue(), is(hearingId));
        assertThat(prosecutionCasesCaptor.getValue().size(), is(1));
        ProsecutionCase prosecutionCaseCaptorValue = (ProsecutionCase) prosecutionCasesCaptor.getValue().get(0);
        assertThat(prosecutionCaseCaptorValue.getId(), is(prosecutionCaseId));

        assertThat(shadowOffencesCaptor.getValue().size(), is(1));
        UUID shadowOffenceCaptorValue = (UUID) shadowOffencesCaptor.getValue().get(0);
        assertThat(shadowOffenceCaptorValue, is(offenceId));

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(1));
        final JsonEnvelope updateExistingHearingRequestedEventProduced = events.get(0);

        assertThat(updateExistingHearingRequestedEventProduced.metadata().name(), is("listing.events.update-existing-hearing-requested"));
        assertThat(updateExistingHearingRequestedEventProduced.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(updateExistingHearingRequestedEventProduced.payloadAsJsonObject().getJsonArray("prosecutionCases").size(), is(1));
        assertThat(updateExistingHearingRequestedEventProduced.payloadAsJsonObject().getJsonArray("shadowListedOffences").size(), is(1));
        assertThat(updateExistingHearingRequestedEventProduced.payloadAsJsonObject().getString("seedingHearingId"), is(seedingHearingId.toString()));

    }


}
