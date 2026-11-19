package uk.gov.moj.cpp.listing.command.hearing.handler;


import org.junit.jupiter.api.BeforeEach;
import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.HearingFlowTestBridge;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.createEnvelope;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.UpdateExistingHearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.AggregateService;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventSource;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventStream;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.domain.PtphDetail;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ported from the JEE {@code UpdateExistingHearingCommandHandlerTest} (MBD refactor P2.3.3, ADR 015). */
@ExtendWith(MockitoExtension.class)
public class UpdateExistingHearingCommandHandlerTest {

    public static final UUID HEARING_ID = randomUUID();

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

    @Spy
    private SeedHearingRules seedHearingAggregate;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private SeedHearingCommandHandler updateExistingHearingCommandHandler;

    @Test
    public void shouldHandleListNextCourtHearings() {
        final JsonObject commandPayload = createObjectBuilder().build();
        final Envelope commandEnvelope = createEnvelope("listing.command.update-existing-hearing", commandPayload);
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
        when(aggregateService.get(eventStream, SeedHearingRules.class)).thenReturn(seedHearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        final ArgumentCaptor<UUID> hearingIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<List> prosecutionCasesCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List> shadowOffencesCaptor = ArgumentCaptor.forClass(List.class);

        updateExistingHearingCommandHandler.updateExistingHearing(commandEnvelope);

        // LPT-2405 added a trailing PtphDetail: null here, because this command carries no
        // inherited tier or list type
        verify(seedHearingAggregate).requestUpdateExistingHearing(eq(seedingHearingId), hearingIdCaptor.capture(), eq(sittingDay), prosecutionCasesCaptor.capture(), shadowOffencesCaptor.capture(), isNull());

        assertThat(hearingIdCaptor.getValue(), is(hearingId));
        assertThat(prosecutionCasesCaptor.getValue().size(), is(1));
        ProsecutionCase prosecutionCaseCaptorValue = (ProsecutionCase) prosecutionCasesCaptor.getValue().get(0);
        assertThat(prosecutionCaseCaptorValue.getId(), is(prosecutionCaseId));

        assertThat(shadowOffencesCaptor.getValue().size(), is(1));
        UUID shadowOffenceCaptorValue = (UUID) shadowOffencesCaptor.getValue().get(0);
        assertThat(shadowOffenceCaptorValue, is(offenceId));

        final List<Envelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(1));
        final Envelope updateExistingHearingRequestedEventProduced = events.get(0);

        assertThat(updateExistingHearingRequestedEventProduced.metadata().name(), is("listing.events.update-existing-hearing-requested"));
        assertThat(updateExistingHearingRequestedEventProduced.payload().getString("hearingId"), is(hearingId.toString()));
        assertThat(updateExistingHearingRequestedEventProduced.payload().getJsonArray("prosecutionCases").size(), is(1));
        assertThat(updateExistingHearingRequestedEventProduced.payload().getJsonArray("shadowListedOffences").size(), is(1));
        assertThat(updateExistingHearingRequestedEventProduced.payload().getString("seedingHearingId"), is(seedingHearingId.toString()));

    }

    /**
     * LPT-2405: when the command carries inherited values they must reach the aggregate as a
     * PtphDetail. The other test covers the null case, so both sides of the guard are pinned.
     */
    @Test
    public void shouldPassInheritedPtphDetailToTheAggregate() throws Exception {
        final JsonObject commandPayload = createObjectBuilder().build();
        final Envelope commandEnvelope = createEnvelope("listing.command.update-existing-hearing", commandPayload);
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();

        final UpdateExistingHearing updateExistingHearing = UpdateExistingHearing.updateExistingHearing()
                .withHearingId(hearingId)
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSittingDay(sittingDay)
                        .withSeedingHearingId(seedingHearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .build())
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(randomUUID()).build()))
                .withShadowListedOffences(Arrays.asList(randomUUID()))
                .withTier("TIER_3")
                .withListType("TYPE_1_FIXED")
                .withKeyReason("Vulnerable witness")
                .build();

        when(jsonObjectConverter.convert(commandPayload, UpdateExistingHearing.class)).thenReturn(updateExistingHearing);
        when(aggregateService.get(eventStream, SeedHearingRules.class)).thenReturn(seedHearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        updateExistingHearingCommandHandler.updateExistingHearing(commandEnvelope);

        final ArgumentCaptor<PtphDetail> ptphCaptor = ArgumentCaptor.forClass(PtphDetail.class);
        verify(seedHearingAggregate).requestUpdateExistingHearing(eq(seedingHearingId), any(UUID.class), eq(sittingDay),
                any(List.class), any(List.class), ptphCaptor.capture());

        assertThat(ptphCaptor.getValue().getTier(), is("TIER_3"));
        assertThat(ptphCaptor.getValue().getListType(), is("TYPE_1_FIXED"));
        assertThat(ptphCaptor.getValue().getKeyReason(), is("Vulnerable witness"));
    }
}
