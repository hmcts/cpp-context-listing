package uk.gov.moj.cpp.listing.command.handler;


import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.moj.cpp.listing.command.handler.UnscheduledListingCommandBuilder.HEARING_TYPE;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.courts.ChangeNextHearingDay;
import uk.gov.justice.listing.courts.DeleteNextHearings;
import uk.gov.justice.listing.courts.DeleteSeededHearing;
import uk.gov.justice.listing.courts.ListNextHearing;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.ListNextHearingsV2;
import uk.gov.justice.listing.courts.RemoveSelectedOffencesFromExistingHearing;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.DeletedHearingInStagingHmi;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingListedCaseUpdated;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.NextHearingDayChanged;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.listing.events.UpdatedHearingInStagingHmi;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.command.factory.HearingTypeFactory;
import uk.gov.moj.cpp.listing.command.service.HmiService;
import uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter;
import uk.gov.moj.cpp.listing.common.service.ProvisionalBookingService;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListNextHearingCommandHandlerTest {

    public static final UUID HEARING_ID = randomUUID();
    public static final int OFFENCE_COUNT = 1;
    public static final int OFFENCE_ORDER_INDEX = 0;
    public static final String OFFENCE_LEGISLATION = "legislation";

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            HearingListed.class,
            NextHearingRequested.class,
            HearingAllocatedForListing.class,
            AllocatedHearingDeleted.class,
            UnallocatedHearingDeleted.class,
            OffencesRemovedFromHearing.class,
            DeleteNextHearingRequested.class,
            NextHearingDayChanged.class,
            UpdatedHearingInStagingHmi.class,
            DeletedHearingInStagingHmi.class,
            OffencesRemovedFromExistingAllocatedHearing.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private SeedHearingAggregate seedHearingAggregate;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private final CommandToDomainConverter commandToDomainConverter = new CommandToDomainConverter();

    @InjectMocks
    private ListNextHearingCommandHandler listNextHearingCommandHandler;


    @Mock
    private HearingTypeFactory hearingTypeFactory;

    @Mock
    private ProvisionalBookingService provisionalBookingService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private HmiService hmiService;

    @Test
    public void shouldHandleListNextCourtHearings() throws EventStreamException {
        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.list-next-hearings-enriched",
                commandPayload);
        final String adjournedFromDate = "2021-01-28";
        final LocalTime startTime = LocalTime.of(10, 0);
        final int defaultDuration = 30;
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID hearing2Id = randomUUID();
        final List<HearingListingNeeds> hearings = asList(
                HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .withDefendants(asList(Defendant.defendant()
                                        .withOffences(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build(),
                HearingListingNeeds.hearingListingNeeds()
                        .withId(hearing2Id)
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .withDefendants(asList(Defendant.defendant()
                                        .withOffences(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());
        final UUID courtCentreId = randomUUID();
        final List<CourtCentreDetails> courtCentresDetails = asList(CourtCentreDetails.courtCentreDetails()
                .withDefaultStartTime(startTime)
                .withDefaultDuration(defaultDuration)
                .withId(courtCentreId)
                .build());
        final UUID offenceId = randomUUID();
        final List<UUID> shadowListedOffences = asList(offenceId);
        final String sittingDay = LocalDate.now().toString();
        when(jsonObjectConverter.convert(commandPayload, ListNextHearingsEnrichedV2.class))
                .thenReturn(ListNextHearingsEnrichedV2.listNextHearingsEnrichedV2()
                        .withAdjournedFromDate(adjournedFromDate)
                        .withCourtCentresDetails(courtCentresDetails)
                        .withSeedingHearing(uk.gov.justice.core.courts.SeedingHearing.seedingHearing()
                                .withSeedingHearingId(seedingHearingId)
                                .withSittingDay(sittingDay)
                                .withJurisdictionType(CROWN)
                                .build())
                        .withListNextHearings(ListNextHearingsV2.listNextHearingsV2()
                                .withHearings(hearings)
                                .withShadowListedOffences(shadowListedOffences)
                                .build())
                        .build());

        when(aggregateService.get(eventStream, SeedHearingAggregate.class)).thenReturn(seedHearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        final ArgumentCaptor<List> hearingCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List> courtCentreDefaultsCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<Optional> adjournedFromDateCaptor = ArgumentCaptor.forClass(Optional.class);
        final ArgumentCaptor<List> shadowOffenceCaptor = ArgumentCaptor.forClass(List.class);

        listNextHearingCommandHandler.listNextHearings(commandEnvelope);

        verify(seedHearingAggregate).requestNextHearings(hearingCaptor.capture(), eq(sittingDay), courtCentreDefaultsCaptor.capture(), adjournedFromDateCaptor.capture(), shadowOffenceCaptor.capture());


        assertThat(hearingCaptor.getValue().size(), is(2));
        HearingListingNeeds hearingCaptorValue = (HearingListingNeeds) hearingCaptor.getValue().get(0);
        assertThat(hearingCaptorValue.getId(), is(hearingId));
        HearingListingNeeds hearingCaptor2Value = (HearingListingNeeds) hearingCaptor.getValue().get(1);
        assertThat(hearingCaptor2Value.getId(), is(hearing2Id));

        assertThat(courtCentreDefaultsCaptor.getValue().size(), is(1));
        CourtCentreDefaults courtCentreDefaultsCaptorValue = (CourtCentreDefaults) courtCentreDefaultsCaptor.getValue().get(0);
        assertThat(courtCentreDefaultsCaptorValue.getCourtCentreId(), is(courtCentreId));
        assertThat(courtCentreDefaultsCaptorValue.getDefaultStartTime(), is(startTime));
        assertThat(courtCentreDefaultsCaptorValue.getDefaultDuration(), is(defaultDuration));

        assertThat(adjournedFromDateCaptor.getValue().isPresent(), is(true));
        String adjournedFromDateCaptorValue = (String) adjournedFromDateCaptor.getValue().get();
        assertThat(adjournedFromDateCaptorValue, is(adjournedFromDate));

        assertThat(shadowOffenceCaptor.getValue().size(), is(1));
        UUID shadowOffenceCaptorValue = (UUID) shadowOffenceCaptor.getValue().get(0);
        assertThat(shadowOffenceCaptorValue, is(offenceId));

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(2));
        final JsonEnvelope nextHearingRequestedEventProduced = events.get(0);
        final JsonEnvelope nextHearingRequestedEvent2Produced = events.get(1);

        assertThat(nextHearingRequestedEventProduced.metadata().name(), is("listing.events.next-hearing-requested"));
        assertThat(nextHearingRequestedEventProduced.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));

        assertThat(nextHearingRequestedEvent2Produced.metadata().name(), is("listing.events.next-hearing-requested"));
        assertThat(nextHearingRequestedEvent2Produced.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearing2Id.toString()));

    }

    @Test
    public void shouldHandleListNextCourtHearing() throws EventStreamException {

        final JsonObject commandPayload = createObjectBuilder().build();
        when(hearingTypeFactory.getHearingTypesIdDurationMap(any(JsonEnvelope.class))).thenReturn(Collections.singletonMap(HEARING_TYPE.getId().toString(), 30));
        when(jsonObjectConverter.convert(commandPayload, ListNextHearing.class)).thenReturn(buildListNextHearing());

        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.list-next-hearing",
                commandPayload);

        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(new Hearing());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(provisionalBookingService.getSlots(any())).thenReturn(Response.accepted().build());
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder()
                .add("provisionalSlots", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("bookingId", "bookingId")
                                .add("courtScheduleId", UUID.randomUUID().toString())
                                .add("ouCode", "ouCode")
                                .add("courtRoomId", UUID.randomUUID().toString())
                                .add("courtRoomNumber", 1)
                                .add("maxSlots", 1)
                                .add("sessionDate", LocalDate.now().toString())
                                .build())
                        .build()
                ).build());

        listNextHearingCommandHandler.listNextCourtHearing(commandEnvelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());

        assertThat(events.size(), is(1));
        final JsonEnvelope listedEventProduced = events.get(0);

        assertThat(listedEventProduced.metadata().name(), is("listing.events.hearing-listed"));
        assertThat(listedEventProduced.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(HEARING_ID.toString()));
    }

    @Test
    public void shouldHandleDeleteHearingWhenAllOffencesAssosiatedWithSeedId() throws EventStreamException {
        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.delete-seeded-hearing", commandPayload);

        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final Hearing hearingAggregate = new Hearing();

        hearingAggregate.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(Type.type()
                                .withId(randomUUID())
                                .withDescription("type")
                                .build())
                        .withHearingDays(emptyList())
                        .withJurisdictionType(CROWN)
                        .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.ENGLISH)
                        .withListedCases(asList(ListedCase.listedCase()
                                .withId(caseId)
                                .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .withJurisdictionType(CROWN)
                                                        .build())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());
        when(jsonObjectConverter.convert(commandPayload, DeleteSeededHearing.class))
                .thenReturn(DeleteSeededHearing.deleteSeededHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .withHearingId(hearingId)
                        .build());
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(hmiService.isHmiEnabled(any(), any())).thenReturn(true);

        listNextHearingCommandHandler.deleteSeededHearing(commandEnvelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());

        assertThat(events.size(), is(2));
        final JsonEnvelope deletedEventProduced = events.get(0);
        assertThat(deletedEventProduced.metadata().name(), is("listing.events.unallocated-hearing-deleted"));
        final JsonObject deleteEventObject = deletedEventProduced.payloadAsJsonObject();
        assertThat(deleteEventObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(deleteEventObject.getJsonArray("caseIds").size(), is(1));
        assertThat(deleteEventObject.getJsonArray("caseIds").getString(0), is(caseId.toString()));

        final JsonEnvelope deletedEventForHmi = events.get(1);
        assertThat(deletedEventForHmi.metadata().name(), is("listing.events.deleted-hearing-in-staging-hmi"));
        final JsonObject deletedEventForHmiObject = deletedEventForHmi.payloadAsJsonObject();
        assertThat(deletedEventForHmiObject.getString("hearingId"), is(hearingId.toString()));


    }

    @Test
    public void shouldUnallocateHearingAndRemoveOffencesOnDeleteHearingWhenNotAllOffencesWereSeededBySeedId() throws EventStreamException {
        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.delete-seeded-hearing", commandPayload);

        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offence2Id = randomUUID();
        final Hearing hearingAggregate = new Hearing();

        hearingAggregate.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(Type.type()
                                .withId(randomUUID())
                                .withDescription("type")
                                .build())
                        .withHearingDays(emptyList())
                        .withJurisdictionType(CROWN)
                        .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.ENGLISH)
                        .withListedCases(asList(ListedCase.listedCase()
                                        .withId(caseId)
                                        .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                                .withId(offenceId)
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withSeedingHearingId(seedingHearingId)
                                                                        .withJurisdictionType(CROWN)
                                                                        .build())
                                                                .build(),
                                                        uk.gov.justice.listing.events.Offence.offence()
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withSeedingHearingId(seedingHearingId)
                                                                        .withJurisdictionType(CROWN)
                                                                        .build())
                                                                .withId(offence2Id).build()
                                                ))
                                                .build()))
                                        .build(),
                                ListedCase.listedCase()
                                        .withId(caseId)
                                        .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                                .withId(randomUUID())
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withSeedingHearingId(randomUUID())
                                                                        .withJurisdictionType(CROWN)
                                                                        .build())
                                                                .build(),
                                                        uk.gov.justice.listing.events.Offence.offence()
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withSeedingHearingId(randomUUID())
                                                                        .withJurisdictionType(CROWN)
                                                                        .build())
                                                                .withId(offence2Id).build()
                                                ))
                                                .build()))
                                        .build()))
                        .build())
                .build());


        when(jsonObjectConverter.convert(commandPayload, DeleteSeededHearing.class))
                .thenReturn(DeleteSeededHearing.deleteSeededHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .withHearingId(hearingId)
                        .build());
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(hmiService.isHmiEnabled(any(), any())).thenReturn(true);

        listNextHearingCommandHandler.deleteSeededHearing(commandEnvelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());

        assertThat(events.size(), is(2));
        final JsonEnvelope deletedEventProduced = events.get(0);
        assertThat(deletedEventProduced.metadata().name(), is("listing.events.offences-removed-from-hearing"));
        final JsonObject unallocateEventObject = deletedEventProduced.payloadAsJsonObject();
        assertThat(unallocateEventObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(unallocateEventObject.getString("seedingHearingId"), is(seedingHearingId.toString()));
        assertThat(unallocateEventObject.containsKey("unallocated"), is(true));
        assertThat(unallocateEventObject.getJsonArray("caseIdsSeededByOnlySeedingHearingId").size(), is(0));

        final JsonEnvelope deletedEventForHmi = events.get(1);
        assertThat(deletedEventForHmi.metadata().name(), is("listing.events.deleted-hearing-in-staging-hmi"));
        final JsonObject deletedEventForHmiObject = deletedEventForHmi.payloadAsJsonObject();
        assertThat(deletedEventForHmiObject.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldHandleDeleteNextHearingsCommand() throws EventStreamException {

        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.delete-next-hearings", commandPayload);
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();

        final DeleteNextHearings deleteNextHearings = DeleteNextHearings.deleteNextHearings()
                .withSeedingHearing(uk.gov.justice.core.courts.SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .withJurisdictionType(CROWN)
                        .withSittingDay(sittingDay)
                        .build())
                .build();

        seedHearingAggregate.apply(NextHearingRequested.nextHearingRequested()
                .withHearing(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build())
                .withHearingDay(sittingDay)
                .build());

        when(jsonObjectConverter.convert(commandPayload, DeleteNextHearings.class)).thenReturn(deleteNextHearings);
        when(aggregateService.get(eventStream, SeedHearingAggregate.class)).thenReturn(seedHearingAggregate);
        when(eventSource.getStreamById(seedingHearingId)).thenReturn(eventStream);

        listNextHearingCommandHandler.deleteNextHearings(commandEnvelope);

        verify(seedHearingAggregate).deleteNextHearings(eq(seedingHearingId), eq(sittingDay));

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(1));
        final JsonEnvelope deleteNextHearingRequestedEventProduced = events.get(0);

        assertThat(deleteNextHearingRequestedEventProduced.metadata().name(), is("listing.events.delete-next-hearing-requested"));
        assertThat(deleteNextHearingRequestedEventProduced.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(deleteNextHearingRequestedEventProduced.payloadAsJsonObject().getString("seedingHearingId"), is(seedingHearingId.toString()));

    }

    @Test
    public void shouldHandleChangeNextHearingDateCommand() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID centreId = randomUUID();
        final UUID roomId = randomUUID();
        final UUID scheduleId = randomUUID();
        final int durationMinutes = 30;
        final UtcClock utcClock = new UtcClock();
        final ZonedDateTime endTime = utcClock.now().plusDays(1);
        final LocalDate hearingDate = LocalDate.now();
        final int sequence = 1234567;
        final ZonedDateTime startTime = utcClock.now();
        final List<HearingDay> hearingDays = asList(HearingDay.hearingDay()
                .withCourtCentreId(centreId)
                .withCourtRoomId(roomId)
                .withCourtScheduleId(scheduleId)
                .withDurationMinutes(durationMinutes)
                .withEndTime(endTime)
                .withHearingDate(hearingDate)
                .withSequence(sequence)
                .withStartTime(startTime)
                .build());
        final Hearing hearingAggregate = new Hearing();

        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.update-seed-hearing-earliest-next-hearing-date", commandPayload);

        when(jsonObjectConverter.convert(commandPayload, ChangeNextHearingDay.class))
                .thenReturn(ChangeNextHearingDay.changeNextHearingDay()
                        .withHearingId(hearingId)
                        .build());
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        hearingAggregate.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withType(Type.type().build())
                        .withHearingDays(emptyList())
                        .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.ENGLISH)
                        .withJurisdictionType(CROWN)
                        .withListedCases(asList(ListedCase.listedCase()
                                .withId(randomUUID())
                                .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                .withId(randomUUID())
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .withJurisdictionType(CROWN)
                                                        .build())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());

        hearingAggregate.apply(HearingDaysChangedForHearing.hearingDaysChangedForHearing()
                .withHearingDays(hearingDays)
                .withHearingId(hearingId)
                .build());

        listNextHearingCommandHandler.changeNextHearingDay(commandEnvelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());

        assertThat(events.size(), is(1));
        final JsonEnvelope eventProduced = events.get(0);
        assertThat(eventProduced.metadata().name(), is("listing.events.next-hearing-day-changed"));

        final JsonObject eventObject = eventProduced.payloadAsJsonObject();
        assertThat(eventObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(eventObject.getString("seedingHearingId"), is(seedingHearingId.toString()));
        assertThat(eventObject.getString("hearingStartDate"), is(ZonedDateTimes.toString(startTime)));
    }

    @Test
    public void shouldHandleChangeNextHearingDateCommandWhenHearingSeededByMultiple() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID centreId = randomUUID();
        final UUID roomId = randomUUID();
        final UUID scheduleId = randomUUID();
        final int durationMinutes = 30;
        final ZonedDateTime endTime = ZonedDateTime.now().plusDays(1);
        final LocalDate hearingDate = LocalDate.now();
        final int sequence = 1234567;
        final ZonedDateTime startTime = ZonedDateTime.now();
        final List<HearingDay> hearingDays = asList(HearingDay.hearingDay()
                .withCourtCentreId(centreId)
                .withCourtRoomId(roomId)
                .withCourtScheduleId(scheduleId)
                .withDurationMinutes(durationMinutes)
                .withEndTime(endTime)
                .withHearingDate(hearingDate)
                .withSequence(sequence)
                .withStartTime(startTime)
                .build());
        final Hearing hearingAggregate = new Hearing();

        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.update-seed-hearing-earliest-next-hearing-date", commandPayload);

        when(jsonObjectConverter.convert(commandPayload, ChangeNextHearingDay.class))
                .thenReturn(ChangeNextHearingDay.changeNextHearingDay()
                        .withHearingId(hearingId)
                        .build());
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        hearingAggregate.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withType(Type.type().build())
                        .withHearingDays(emptyList())
                        .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.ENGLISH)
                        .withJurisdictionType(CROWN)
                        .withListedCases(asList(ListedCase.listedCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(randomUUID())
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .withJurisdictionType(CROWN)
                                                                .build())
                                                        .build(), uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(randomUUID())
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .withJurisdictionType(CROWN)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build(),
                                ListedCase.listedCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(randomUUID())
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withSeedingHearingId(seedingHearingId2)
                                                                .withJurisdictionType(CROWN)
                                                                .build())
                                                        .build(), uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(randomUUID())
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withSeedingHearingId(seedingHearingId2)
                                                                .withJurisdictionType(CROWN)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearingAggregate.apply(HearingDaysChangedForHearing.hearingDaysChangedForHearing()
                .withHearingDays(hearingDays)
                .withHearingId(hearingId)
                .build());

        listNextHearingCommandHandler.changeNextHearingDay(commandEnvelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());

        assertThat(events.size(), is(2));
        final JsonEnvelope eventProduced1 = events.get(0);
        assertThat(eventProduced1.metadata().name(), is("listing.events.next-hearing-day-changed"));

        final JsonObject eventObject = eventProduced1.payloadAsJsonObject();
        assertThat(eventObject.getString("hearingId"), is(hearingId.toString()));

        final JsonEnvelope eventProduced2 = events.get(1);
        assertThat(eventProduced2.metadata().name(), is("listing.events.next-hearing-day-changed"));

        final JsonObject eventObject2 = eventProduced2.payloadAsJsonObject();
        assertThat(eventObject2.getString("hearingId"), is(hearingId.toString()));
        assertThat(asList(eventObject.getString("seedingHearingId"), eventObject2.getString("seedingHearingId")).containsAll(
                asList(seedingHearingId.toString(), seedingHearingId2.toString())) , is(true));
        }

    @Test
    public void shouldHandleRemoveSelectedOffencesFromExistingHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final JsonObject commandPayload = createObjectBuilder().build();
        final JsonEnvelope commandEnvelope = createEnvelope("listing.command.remove-selected-offences-from-existing-hearing", commandPayload);
        final Hearing hearingAggregate = new Hearing();

        when(jsonObjectConverter.convert(commandPayload, RemoveSelectedOffencesFromExistingHearing.class))
                .thenReturn(RemoveSelectedOffencesFromExistingHearing.removeSelectedOffencesFromExistingHearing()
                        .withHearingId(hearingId)
                        .withOffenceIds(asList(offenceId1, offenceId2))
                        .build());
        when(aggregateService.get(eventStream, Hearing.class)).thenReturn(hearingAggregate);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        hearingAggregate.apply(HearingListedCaseUpdated.hearingListedCaseUpdated()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withAllocated(true)
                        .withType(Type.type().build())
                        .withHearingDays(emptyList())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(CROWN)
                        .withListedCases(asList(ListedCase.listedCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(offenceId1)
                                                        .build(), uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(offenceId2)
                                                        .build()))
                                                .build()))
                                        .build(),
                                ListedCase.listedCase()
                                        .withId(randomUUID())
                                        .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(offenceId1)
                                                        .build(), uk.gov.justice.listing.events.Offence.offence()
                                                        .withId(offenceId2)
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                    .withUnAllocatedListedCases(asList(ListedCase.listedCase()
                                    .withId(randomUUID())
                                    .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                            .withId(randomUUID())
                                            .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                    .withId(offenceId1)
                                                    .build(), uk.gov.justice.listing.events.Offence.offence()
                                                    .withId(offenceId2)
                                                    .build()))
                                            .build()))
                                    .withMarkers(singletonList(Marker.marker().build()))
                                    .build(),
                            ListedCase.listedCase()
                                    .withId(randomUUID())
                                    .withDefendants(asList(uk.gov.justice.listing.events.Defendant.defendant()
                                            .withId(randomUUID())
                                            .withOffences(asList(uk.gov.justice.listing.events.Offence.offence()
                                                    .withId(offenceId1)
                                                    .build(), uk.gov.justice.listing.events.Offence.offence()
                                                    .withId(offenceId2)
                                                    .build()))
                                            .build()))
                                    .withMarkers(singletonList(Marker.marker().build()))
                                    .build()))
                .build());

        listNextHearingCommandHandler.removeSelectedOffencesFromExistingHearing(commandEnvelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(1));
        final JsonEnvelope eventProduced1 = events.get(0);
        assertThat(eventProduced1.metadata().name(), is("listing.events.offences-removed-from-existing-allocated-hearing"));
        final JsonObject eventObject = eventProduced1.payloadAsJsonObject();
        assertThat(eventObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(eventObject.getJsonArray("offenceIds").getString(0), is(offenceId1.toString()));
        assertThat(eventObject.getJsonArray("offenceIds").getString(1), is(offenceId2.toString()));
        assertThat(eventObject.getString("sourceContext"), is("Hearing"));




    }


    private ListNextHearing buildListNextHearing() {
        final String adjournedFromDate = "2020-01-25";
        final UUID courtCentreId = randomUUID();
        final UUID bookingReference = randomUUID();

        final int estimatedMinutes = 60;
        final HearingLanguage hearingLanguage = HearingLanguage.ENGLISH;
        final String listingDirections = "listing direction";

        final String prosecutorDatesToAvoid = "Prosecutor Dates To Avoid";
        final ZonedDateTime startDate = ZonedDateTime.now();
        final String restrictionReason = "Reporting Restriction Reason";
        final UUID typeId = randomUUID();
        final String endDate = startDate.toLocalDate().plusDays(2).toString();
        final UUID prosecutionAuthorityId = randomUUID();

        final int weekCommencingDurationInWeeks = 25;

        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final ZonedDateTime courtProceedingsInitiated = ZonedDateTime.now();

        final JurisdictionType jurisdictionType = CROWN;
        final String prosecutionAuthorityCode = "authority-code";
        final String prosecutionAuthorityReference = "authority-reference";
        final String weekCommencingStartDate = "2021-01-26";
        return ListNextHearing
                .listNextHearing()
                .withAdjournedFromDate(adjournedFromDate)
                .withHearing(HearingListingNeeds.hearingListingNeeds()
                        .withBookingReference(bookingReference)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withEstimatedMinutes(estimatedMinutes)
                        .withEndDate(endDate)
                        .withId(HEARING_ID)
                        .withJudiciary(asList(JudicialRole.judicialRole()
                                .withJudicialId(randomUUID())
                                .withIsBenchChairman(true)
                                .withIsDeputy(false)
                                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("judiciary-type")
                                        .build())
                                .build()))
                        .withJurisdictionType(jurisdictionType)
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                                        .withProsecutionAuthorityCode(prosecutionAuthorityCode)
                                        .build())
                                .withDefendants(asList(Defendant.defendant()
                                        .withCourtProceedingsInitiated(courtProceedingsInitiated)
                                        .withId(defendantId)
                                        .withMasterDefendantId(masterDefendantId)
                                        .withOffences(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .withCount(OFFENCE_COUNT)
                                                .withOrderIndex(OFFENCE_ORDER_INDEX)
                                                .withOffenceLegislation(OFFENCE_LEGISLATION)
                                                .withSeedingHearing(null)
                                                .build()))
                                        .build()))
                                .build()))
                        .withListingDirections(listingDirections)
                        .withProsecutorDatesToAvoid(prosecutorDatesToAvoid)
                        .withReportingRestrictionReason(restrictionReason)
                        .withListedStartDateTime(startDate)
                        .withType(HearingType.hearingType().withId(HEARING_TYPE.getId()).build())
                        .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                                .withStartDate(weekCommencingStartDate)
                                .withDuration(weekCommencingDurationInWeeks)
                                .build())
                        .build())
                .withCourtCentresDetails(asList((CourtCentreDetails.courtCentreDetails()
                        .withId(courtCentreId)
                        .withDefaultDuration(30)
                        .withDefaultStartTime(LocalTime.of(10, 00))
                        .build())))
                .build();

    }

    private  <T> List<T> asList(T... a) {
        return new ArrayList<>(java.util.Arrays.asList(a));
    }

}
