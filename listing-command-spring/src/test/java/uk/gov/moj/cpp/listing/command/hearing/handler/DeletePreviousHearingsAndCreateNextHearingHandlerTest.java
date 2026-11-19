package uk.gov.moj.cpp.listing.command.hearing.handler;

import org.junit.jupiter.api.BeforeEach;
import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.HearingFlowTestBridge;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.createEnvelope;
import static uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.FrameworkTestSupport.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.courts.CreateNextHearing;
import uk.gov.justice.listing.courts.DeletePreviousHearings;
import uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing;
import uk.gov.justice.listing.events.CreateNextHearingRequested;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.AggregateService;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventSource;
import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventStream;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ported from the JEE {@code DeletePreviousHearingsAndCreateNextHearingHandlerTest} (MBD refactor P2.3.3, ADR 015). */
@ExtendWith(MockitoExtension.class)
class DeletePreviousHearingsAndCreateNextHearingHandlerTest {
    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingFlow hearingFlow;

    @BeforeEach
    void bridgeMigratedAggregates() {
        HearingFlowTestBridge.bridge(hearingFlow, eventSource, aggregateService);
    }


    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Mock
    private EventStream eventStream;

    @Spy
    private SeedHearingRules seedHearingAggregate;

    @InjectMocks
    private SeedHearingCommandHandler handler;




    @Test
    public void shouldHandleDeletePreviousHearingsAndCreateNextHearing() {

        final JsonObject commandPayload = createObjectBuilder().build();
        final Envelope commandEnvelope = createEnvelope("listing.command.delete-previous-hearings-and-create-next-hearing", commandPayload);
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();

        final CreateNextHearing createNextHearing = CreateNextHearing.createNextHearing()
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .withSittingDay(sittingDay)
                        .withJurisdictionType(CROWN)
                        .build())
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .build())
                .build();
        final uk.gov.justice.listing.events.CreateNextHearing createNextHearingEvent = uk.gov.justice.listing.events.CreateNextHearing.createNextHearing()
                .withCommittingCourt(createNextHearing.getCommittingCourt())
                .withHearing(createNextHearing.getHearing())
                .withPreviousBookingReferencesWithCourtScheduleIds(createNextHearing.getPreviousBookingReferencesWithCourtScheduleIds())
                .withSeedingHearing(createNextHearing.getSeedingHearing())
                .withShadowListedOffences(createNextHearing.getShadowListedOffences())
                .build();
        final DeletePreviousHearingsAndCreateNextHearing deletePreviousHearingsAndCreateNextHearing = DeletePreviousHearingsAndCreateNextHearing.deletePreviousHearingsAndCreateNextHearing()
                        .withDeletePreviousHearings(DeletePreviousHearings.deletePreviousHearings()
                                .withHearingId(hearingId)
                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                        .withSeedingHearingId(seedingHearingId)
                                        .withSittingDay(sittingDay)
                                        .withJurisdictionType(CROWN)
                                        .build())
                                .build())
                        .withCreateNextHearing(createNextHearing)
                        .build();

        seedHearingAggregate.apply(NextHearingRequested.nextHearingRequested()
                .withHearing(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build())
                .withHearingDay(sittingDay)
                .build());

        when(jsonObjectConverter.convert(commandPayload, DeletePreviousHearingsAndCreateNextHearing.class)).thenReturn(deletePreviousHearingsAndCreateNextHearing);
        when(aggregateService.get(eventStream, SeedHearingRules.class)).thenReturn(seedHearingAggregate);
        when(eventSource.getStreamById(seedingHearingId)).thenReturn(eventStream);

        handler.deletePreviousHearingsAndCreateNextHearing(commandEnvelope);

        verify(seedHearingAggregate).deletePreviousHearingsAndCreateNextHearing(eq(seedingHearingId), eq(sittingDay), eq(createNextHearingEvent));

        final List<Envelope> events = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(events.size(), is(2));
        final Envelope deleteNextHearingRequestedEventProduced = events.get(0);

        assertThat(deleteNextHearingRequestedEventProduced.metadata().name(), is("listing.events.delete-next-hearing-requested"));
        assertThat(deleteNextHearingRequestedEventProduced.payload().getString("hearingId"), is(hearingId.toString()));
        assertThat(deleteNextHearingRequestedEventProduced.payload().getString("seedingHearingId"), is(seedingHearingId.toString()));


    }
}
