package uk.gov.moj.cpp.listing.command.handler;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.courts.CreateNextHearing;
import uk.gov.justice.listing.courts.DeletePreviousHearings;
import uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing;
import uk.gov.justice.listing.events.CreateNextHearingRequested;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeletePreviousHearingsAndCreateNextHearingHandlerTest {
    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            DeleteNextHearingRequested.class,
            CreateNextHearingRequested.class);

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Mock
    private EventStream eventStream;

    @Spy
    private SeedHearingAggregate seedHearingAggregate;

    @InjectMocks
    private DeletePreviousHearingsAndCreateNextHearingHandler handler;




    @Test
    public void shouldHandleDeletePreviousHearingsAndCreateNextHearing() throws EventStreamException {

        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.delete-previous-hearings-and-create-next-hearing", commandPayload);
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
        when(aggregateService.get(eventStream, SeedHearingAggregate.class)).thenReturn(seedHearingAggregate);
        when(eventSource.getStreamById(seedingHearingId)).thenReturn(eventStream);

        handler.handleDeletePreviousHearingsAndCreateNextHearing(commandEnvelope);

        verify(seedHearingAggregate).deletePreviousHearingsAndCreateNextHearing(eq(seedingHearingId), eq(sittingDay), eq(createNextHearingEvent));

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(events.size(), is(2));
        final JsonEnvelope deleteNextHearingRequestedEventProduced = events.get(0);

        assertThat(deleteNextHearingRequestedEventProduced.metadata().name(), is("listing.events.delete-next-hearing-requested"));
        assertThat(deleteNextHearingRequestedEventProduced.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(deleteNextHearingRequestedEventProduced.payloadAsJsonObject().getString("seedingHearingId"), is(seedingHearingId.toString()));


    }
}
