package uk.gov.moj.cpp.listing.command.hearing.api;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.hearing.api.TestSupport.metadataWithRandomUUID;
import static uk.gov.moj.cpp.listing.command.hearing.api.TestSupport.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.courts.Courtrooms;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.ListNextHearingsV2;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearings;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.courts.UpdateExistingHearing;
import uk.gov.justice.listing.courts.UpdateRelatedHearing;
import uk.gov.moj.cpp.listing.command.hearing.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.hearing.api.service.HearingEnrichmentOrchestrator;
import uk.gov.moj.cpp.listing.command.hearing.api.service.HearingLookupService;
import uk.gov.moj.cpp.listing.command.hearing.api.service.PtphDetailEnrichmentService;
import uk.gov.moj.cpp.listing.command.hearing.handler.HearingCommands;
import uk.gov.moj.cpp.listing.command.hearing.handler.NextHearingCommands;
import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.service.ProgressionSplitHearingService;
import uk.gov.moj.cpp.listing.common.splithearing.SplitHearingRejectedException;
import uk.gov.moj.cpp.listing.domain.PtphDetail;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonValueConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Port of the JEE {@code ListingCommandApiTest} cases for the actions of MBD refactor P2.3.3: split
 * (forwarded to progression), next hearings (list, list-unscheduled, update-related with PTPH
 * inheritance, delete, delete-previous-and-create), migrate, duplicate, and the four RAML-only
 * pass-throughs. As in {@link HearingCommandApiTest}, {@link Sender} receives whatever envelope
 * reaches a {@link HearingCommands} / {@link NextHearingCommands} method (the JEE {@code Sender}).
 */
@ExtendWith(MockitoExtension.class)
class HearingCommandApiNextHearingsTest {

    interface Sender {
        void send(Envelope command);
    }

    private static final int SC_ACCEPTED = 202;
    private static final int SC_CONFLICT = 409;
    private static final int SC_NOT_FOUND = 404;

    @Mock
    private Sender sender;
    @Mock
    private Envelope envelope;
    @Mock
    private JsonObject payload;
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;
    @Mock
    private CourtCentreFactory courtCentreFactory;
    @Mock
    private HearingEnrichmentOrchestrator hearingEnrichmentOrchestrator;
    @Mock
    private HearingLookupService hearingLookupService;
    @Mock
    private PtphDetailEnrichmentService ptphDetailEnrichmentService;
    @Mock
    private ProgressionSplitHearingService progressionSplitHearingService;
    @InjectMocks
    private HearingCommandApi listingCommandApi;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @BeforeEach
    void sendToTheCommandServices() {
        ReflectionTestUtils.setField(listingCommandApi, "hearingCommands", mock(HearingCommands.class, invocation -> {
            forward(invocation.getArguments());
            return null;
        }));
        ReflectionTestUtils.setField(listingCommandApi, "nextHearingCommands", mock(NextHearingCommands.class, invocation -> {
            forward(invocation.getArguments());
            return null;
        }));
        // PTPH enrichment is exercised by PtphDetailEnrichmentServiceTest; a pass-through here.
        lenient().when(ptphDetailEnrichmentService.enrichWithPtphDetail(anyList(), any(), any(Envelope.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void forward(final Object[] arguments) {
        if (arguments.length == 1 && arguments[0] instanceof Envelope command) {
            sender.send(command);
        }
    }

    // ─── SPRDT-1363: split-hearing proxy ─────────────────────────────────

    private static final UUID SPLIT_HEARING_ID = randomUUID();
    private static final UUID SPLIT_COURT_CENTRE_ID = randomUUID();
    private static final UUID SPLIT_COURT_ROOM_ID = randomUUID();

    private JsonObject splitPayload(final boolean withRoom) {
        final jakarta.json.JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("hearingId", SPLIT_HEARING_ID.toString())
                .add("courtCentreId", SPLIT_COURT_CENTRE_ID.toString())
                .add("jurisdictionType", "CROWN")
                .add("type", Json.createObjectBuilder().add("id", randomUUID().toString()).add("description", "First hearing"))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("virtual", true)
                                .add("duration", 1080)
                                .add("startTime", "2026-09-10T09:00:00.000Z")
                                .add("courtScheduleId", randomUUID().toString())))
                .add("prosecutionCases", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("caseId", randomUUID().toString())
                                .add("defendants", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("defendantId", randomUUID().toString())
                                                .add("offences", Json.createArrayBuilder()
                                                        .add(Json.createObjectBuilder()
                                                                .add("offenceId", randomUUID().toString())))))))
                .add("sendNotificationToParties", false);
        if (withRoom) {
            builder.add("courtRoomId", SPLIT_COURT_ROOM_ID.toString());
        }
        return builder.build();
    }

    private Envelope splitEnvelope(final boolean withRoom) {
        return new Envelope(metadataWithRandomUUIDAndName().withUserId("user").build(), splitPayload(withRoom));
    }

    private void givenTheCourtCentre(final List<Courtrooms> rooms) {
        when(courtCentreFactory.getCourtCentre(eq(SPLIT_COURT_CENTRE_ID), any(Envelope.class))).thenReturn(CourtCentreDetails.courtCentreDetails()
                .withId(SPLIT_COURT_CENTRE_ID)
                .withName("Croydon Crown Court")
                .withCourtrooms(rooms)
                .build());
    }

    private void givenProgressionResponds(final int status, final JsonObject body) {
        when(progressionSplitHearingService.splitHearing(anyString(), any(JsonObject.class), any(), any(), any(), anyList()))
                .thenReturn(new HttpResult(status, body));
    }

    @Test
    void shouldAcceptSplitHearingAndResolveTheCourtCentre() {
        givenTheCourtCentre(List.of(Courtrooms.courtrooms().withId(SPLIT_COURT_ROOM_ID).withCourtroomName("Courtroom 01").build()));
        givenProgressionResponds(SC_ACCEPTED, null);

        listingCommandApi.handleSplitHearing(splitEnvelope(true));

        verify(courtCentreFactory).getCourtCentre(eq(SPLIT_COURT_CENTRE_ID), any(Envelope.class));
        verifyNoInteractions(sender);
    }

    @Test
    void shouldAcceptASplitRequestThatNamesNoRoom() {
        givenTheCourtCentre(List.of());
        givenProgressionResponds(SC_ACCEPTED, null);

        listingCommandApi.handleSplitHearing(splitEnvelope(false));

        verify(courtCentreFactory).getCourtCentre(eq(SPLIT_COURT_CENTRE_ID), any(Envelope.class));
    }

    @Test
    void shouldForwardTheConvertedSplitToProgressionAsTheActingUser() {
        givenTheCourtCentre(List.of());
        givenProgressionResponds(SC_ACCEPTED, null);

        listingCommandApi.handleSplitHearing(splitEnvelope(false));

        final ArgumentCaptor<JsonObject> forwardedCaptor = forClass(JsonObject.class);
        verify(progressionSplitHearingService).splitHearing(
                eq(SPLIT_HEARING_ID.toString()), forwardedCaptor.capture(), eq(Optional.of("user")), any(), any(), anyList());

        final JsonObject forwarded = forwardedCaptor.getValue();
        assertThat("progression receives the converted shape, not the front end's payload",
                forwarded.containsKey("listNewHearing"), is(true));
        assertThat("the hearing id fills the URI template, so it must not also sit in the body",
                forwarded.containsKey("hearingId"), is(false));
    }

    @Test
    void shouldSurfaceProgressionsRejectionStatusAndErrorCode() {
        givenTheCourtCentre(List.of());
        givenProgressionResponds(SC_CONFLICT, Json.createObjectBuilder()
                .add("errorCode", "HEARING_ALREADY_CHANGED")
                .add("message", "the hearing has moved on since this request was built")
                .build());

        final SplitHearingRejectedException thrown = assertThrows(SplitHearingRejectedException.class,
                () -> listingCommandApi.handleSplitHearing(splitEnvelope(false)));

        assertThat(thrown.getHttpStatus(), is(SC_CONFLICT));
        assertThat(thrown.getErrorCode(), is("HEARING_ALREADY_CHANGED"));
    }

    @Test
    void shouldSurfaceProgressionsRejectionWhenItSendsNoBody() {
        givenTheCourtCentre(List.of());
        givenProgressionResponds(SC_NOT_FOUND, null);

        final SplitHearingRejectedException thrown = assertThrows(SplitHearingRejectedException.class,
                () -> listingCommandApi.handleSplitHearing(splitEnvelope(false)));

        assertThat(thrown.getHttpStatus(), is(SC_NOT_FOUND));
        assertThat(thrown.getErrorCode(), is(nullValue()));
    }

    // ─── next hearings ─────────────────────────────────

    @Test
    void shouldIssueListNextHearingsEnrichedCommand() {
        final ListNextHearingsV2 listNextHearings = mock(ListNextHearingsV2.class);
        final UUID seedingHearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();
        final ArgumentCaptor<ListNextHearingsEnrichedV2> payloadCaptor = forClass(ListNextHearingsEnrichedV2.class);

        when(envelope.payload()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        when(jsonObjectConverter.convert(payload, ListNextHearingsV2.class)).thenReturn(listNextHearings);
        when(listNextHearings.getSeedingHearing()).thenReturn(SeedingHearing.seedingHearing()
                .withSittingDay(sittingDay)
                .withSeedingHearingId(seedingHearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build());
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(JsonValue.EMPTY_JSON_OBJECT);

        listingCommandApi.listNextHearings(envelope);

        final ListNextHearingsEnrichedV2 value = payloadCaptor.getValue();
        assertThat(value, notNullValue());
        assertThat(value.getListNextHearings(), notNullValue());
        assertThat(value.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(value.getSeedingHearing().getSittingDay(), is(sittingDay));
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().name(), is("listing.command.list-next-hearings-enriched-v2"));
    }

    @Test
    void shouldListUnscheduledNextHearings() {
        final ListUnscheduledNextHearings listUnscheduledNextHearings = mock(ListUnscheduledNextHearings.class);
        final ArgumentCaptor<ListUnscheduledNextHearingsEnriched> payloadCaptor = forClass(ListUnscheduledNextHearingsEnriched.class);
        final UUID seedHearingId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final String sittingDay = LocalDate.now().toString();

        when(envelope.payload()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        when(jsonObjectConverter.convert(payload, ListUnscheduledNextHearings.class)).thenReturn(listUnscheduledNextHearings);
        when(listUnscheduledNextHearings.getHearings()).thenReturn(createUnscheduledListingNeeds(hearingId1, hearingId2));
        when(listUnscheduledNextHearings.getSeedingHearing()).thenReturn(SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedHearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withSittingDay(sittingDay)
                .build());
        mockCourtCentres();
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(JsonValue.EMPTY_JSON_OBJECT);

        listingCommandApi.handleListUnscheduledNextCourtHearings(envelope);

        final ListUnscheduledNextHearingsEnriched value = payloadCaptor.getValue();
        assertThat(value.getCourtCentresDetails().size(), is(2));
        assertThat(value.getCourtCentresDetails().get(0).getDefaultDuration(), anyOf(is(10), is(20)));
        assertThat(value.getHearings().size(), is(2));
        assertThat(value.getHearings().get(0).getId(), is(hearingId1));
        assertThat(value.getHearings().get(1).getId(), is(hearingId2));
        assertThat(value.getSeedingHearing().getSeedingHearingId(), is(seedHearingId));
        assertThat(value.getSeedingHearing().getSittingDay(), is(sittingDay));
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().name(), is("listing.command.list-unscheduled-next-hearings-enriched"));
    }

    @Test
    void shouldDeleteNextHearings() {
        final JsonObject seedingHearing = Json.createObjectBuilder()
                .add("seedHearingId", randomUUID().toString())
                .add("sittingDay", LocalDate.now().toString())
                .add("jurisdictionType", "CROWN")
                .build();
        when(envelope.payload()).thenReturn(payload);
        when(payload.getJsonObject("seedingHearing")).thenReturn(seedingHearing);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        listingCommandApi.handleDeleteNextHearings(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().name(), is("listing.command.delete-next-hearings"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(Json.createObjectBuilder().add("seedingHearing", seedingHearing).build()));
    }

    @Test
    void shouldHandleDeletePreviousHearingsAndCreateNextHearing() {
        final JsonObject body = Json.createObjectBuilder().add("x", 1).build();
        final Envelope command = new Envelope(metadataWithRandomUUID("listing.delete-previous-hearings-and-create-next-hearing").build(), body);

        listingCommandApi.handleDeletePreviousHearingsAndCreateNextHearing(command);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().name(), is("listing.command.delete-previous-hearings-and-create-next-hearing"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(body));
    }

    @Test
    void shouldForwardMigrateCrownHearingsToCourtSchedulesToCommandHandler() {
        final JsonObject body = Json.createObjectBuilder().add("hearings", Json.createArrayBuilder()).build();
        final Envelope command = new Envelope(metadataWithRandomUUID("listing.migrate-crown-hearings-to-courtschedules").build(), body);

        listingCommandApi.handleMigrateCrownHearingsToCourtSchedules(command);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().name(), is("listing.command.migrate-crown-hearings-to-courtschedules"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(body));
    }

    @Test
    void shouldHandleMarkUnallocatedHearingAsDuplicate() {
        final Metadata metadata = metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(metadata);

        listingCommandApi.handleMarkUnallocatedHearingAsDuplicate(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().name(), is("listing.command.mark-unallocated-hearing-as-duplicate"));
    }

    @Test
    void shouldPassTheRamlOnlyHearingCommandsThroughUnchanged() {
        for (final String name : List.of("listing.command.update-defendants-for-hearing", "listing.command.update-offences-for-hearing",
                "listing.command.delete-offences-for-hearing", "listing.command.add-offences-for-hearing")) {
            final Envelope command = new Envelope(metadataWithRandomUUID(name).build(), Json.createObjectBuilder().add("hearingId", "h").build());

            listingCommandApi.handlePassThroughHearingCommand(command);

            verify(sender).send(command);
        }
    }

    @Test
    void shouldUpdateRelatedHearing() {
        givenAnUpdateRelatedHearingCommand();

        listingCommandApi.updateRelatedHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().name(), is("listing.command.update-existing-hearing"));
    }

    // ─── LPT-2405 existing-hearing flow ─────────────────────────────────

    private UpdateRelatedHearing updateRelatedHearingCommand() {
        return UpdateRelatedHearing.updateRelatedHearing()
                .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(randomUUID()).build())
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase().withId(randomUUID()).build()))
                .build();
    }

    private void givenAnUpdateRelatedHearingCommand() {
        given(envelope.payload()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateRelatedHearing.class)).willReturn(updateRelatedHearingCommand());
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        when(payload.getString(anyString())).thenReturn(randomUUID().toString());
    }

    private UpdateExistingHearing captureSentCommand() {
        final ArgumentCaptor<UpdateExistingHearing> captor = forClass(UpdateExistingHearing.class);
        verify(objectToJsonValueConverter).convert(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldInheritTierAndListTypeOntoAnExistingHearing() {
        givenAnUpdateRelatedHearingCommand();
        given(hearingLookupService.findHearing(any(), any())).willReturn(Optional.of(Json.createObjectBuilder()
                .add("type", Json.createObjectBuilder().add("id", randomUUID().toString()).add("description", "Trial"))
                .add("jurisdictionType", "CROWN")
                .build()));
        given(ptphDetailEnrichmentService.resolveForExistingHearing(any(), any(), any(), any()))
                .willReturn(Optional.of(new PtphDetail("TIER_3", "TYPE_1_FIXED", "Vulnerable witness")));

        listingCommandApi.updateRelatedHearing(envelope);

        final UpdateExistingHearing sent = captureSentCommand();
        assertThat(sent.getTier(), is("TIER_3"));
        assertThat(sent.getListType(), is("TYPE_1_FIXED"));
        assertThat(sent.getKeyReason(), is("Vulnerable witness"));
    }

    @Test
    void shouldSendTheCommandWithoutPtphDetailWhenNothingIsInherited() {
        givenAnUpdateRelatedHearingCommand();
        given(hearingLookupService.findHearing(any(), any())).willReturn(Optional.of(Json.createObjectBuilder()
                .add("type", Json.createObjectBuilder().add("id", randomUUID().toString()))
                .add("jurisdictionType", "CROWN")
                .build()));
        given(ptphDetailEnrichmentService.resolveForExistingHearing(any(), any(), any(), any())).willReturn(Optional.empty());

        listingCommandApi.updateRelatedHearing(envelope);

        final UpdateExistingHearing sent = captureSentCommand();
        assertThat(sent.getTier(), is(nullValue()));
        assertThat(sent.getListType(), is(nullValue()));
        assertThat(sent.getKeyReason(), is(nullValue()));
    }

    @Test
    void shouldNotAskForPtphDetailWhenTheExistingHearingIsNotFound() {
        givenAnUpdateRelatedHearingCommand();
        given(hearingLookupService.findHearing(any(), any())).willReturn(Optional.empty());

        listingCommandApi.updateRelatedHearing(envelope);

        verify(ptphDetailEnrichmentService, never()).resolveForExistingHearing(any(), any(), any(), any());
        assertThat(captureSentCommand().getTier(), is(nullValue()));
    }

    @Test
    void shouldNotAskForPtphDetailWhenTheStoredHearingHasNoType() {
        givenAnUpdateRelatedHearingCommand();
        given(hearingLookupService.findHearing(any(), any()))
                .willReturn(Optional.of(Json.createObjectBuilder().add("jurisdictionType", "CROWN").build()));

        listingCommandApi.updateRelatedHearing(envelope);

        verify(ptphDetailEnrichmentService, never()).resolveForExistingHearing(any(), any(), any(), any());
        assertThat(captureSentCommand().getTier(), is(nullValue()));
    }

    @Test
    void shouldTolerateAStoredHearingWithoutAJurisdictionType() {
        givenAnUpdateRelatedHearingCommand();
        given(hearingLookupService.findHearing(any(), any()))
                .willReturn(Optional.of(Json.createObjectBuilder()
                        .add("type", Json.createObjectBuilder().add("id", randomUUID().toString()))
                        .build()));
        given(ptphDetailEnrichmentService.resolveForExistingHearing(isNull(), any(), any(), any())).willReturn(Optional.empty());

        listingCommandApi.updateRelatedHearing(envelope);

        verify(ptphDetailEnrichmentService).resolveForExistingHearing(isNull(), any(), any(), any());
        assertThat(captureSentCommand().getTier(), is(nullValue()));
    }

    private List<HearingUnscheduledListingNeeds> createUnscheduledListingNeeds(final UUID hearingId1, final UUID hearingId2) {
        return Arrays.asList(
                HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withId(hearingId1)
                        .build(),
                HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withId(hearingId2)
                        .build());
    }

    private void mockCourtCentres() {
        when(courtCentreFactory.getCourtCentre(any(), eq(envelope))).thenReturn(
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(10).build(),
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(20).build());
    }
}
