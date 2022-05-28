package uk.gov.moj.cpp.listing.command.api;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.CreateListingNote;
import uk.gov.justice.listing.commands.DeleteListingNote;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.ExtendHearingForHearing;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.ListNextHearingsV2;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearings;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.courts.UpdateRelatedHearing;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.staginghmi.common.StagingHmiService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingCommandApiTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Mock
    private Sender sender;
    @Mock
    private JsonEnvelope envelope;
    @Mock
    private JsonObject payload;
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Mock
    private CourtCentreFactory courtCentreFactory;
    @Mock
    private UpdateHearingForListing updateHearingForListing;
    @Mock
    private ListCourtHearing listCourtHearing;
    @Mock
    private CreateListingNote createListingNote;
    @Mock
    private DeleteListingNote deleteListingNote;
    @InjectMocks
    private ListingCommandApi listingCommandApi;
    @Mock
    private ExtendHearingForHearing extendHearingForHearing;
    @Mock
    private StagingHmiService stagingHmiService;
    @Mock
    private ReferenceDataService referenceDataService;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Test
    public void testIfMethodsArePassThrough() {

        assertThat(ListingCommandApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleChangeJudiciaryForHearings")
                        .thatHandles("listing.command.change-judiciary-for-hearings"))
                .with(method("handleSequenceHearings")
                        .thatHandles("listing.command.sequence-hearings")));
    }


    @Test
    public void shouldUpdateHearingForListingWithProsecutionCases() {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getSelectedCourtCentre()).willReturn(null);
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());

        final UUID courtCentreId = randomUUID();
        given(updateHearingForListing.getCourtCentreId()).willReturn(courtCentreId);
        OrganisationUnit organisationUnit = new OrganisationUnit(randomUUID(), "abc");
        when(referenceDataService.getOrganizationUnitById(courtCentreId, envelope)).thenReturn(organisationUnit);
        when(stagingHmiService.isHmiListingEnabled(Matchers.any())).thenReturn(true);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        final UUID caseId = randomUUID();
        final JsonObject prosecutionCases = Json.createObjectBuilder().add("caseId", caseId.toString()).build();
        final JsonArray prosecutionCasesArray = Json.createArrayBuilder().add(prosecutionCases).build();
        given(payload.getJsonArray("prosecutionCases")).willReturn(prosecutionCasesArray);
        given(jsonObjectConverter.convert(prosecutionCases, ProsecutionCases.class)).willReturn(ProsecutionCases.prosecutionCases().build());

        //when
        listingCommandApi.handleUpdateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        verify(jsonObjectConverter).convert(prosecutionCases, ProsecutionCases.class);
    }

    @Test
    public void shouldUpdateHearingForListing() {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getSelectedCourtCentre()).willReturn(null);
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());

        final UUID courtCentreId = randomUUID();
        given(updateHearingForListing.getCourtCentreId()).willReturn(courtCentreId);
        OrganisationUnit organisationUnit = new OrganisationUnit(randomUUID(), "abc");
        when(referenceDataService.getOrganizationUnitById(courtCentreId, envelope)).thenReturn(organisationUnit);
        when(stagingHmiService.isHmiListingEnabled(Matchers.any())).thenReturn(true);

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonArray prosecutionCasesArray = Json.createArrayBuilder().build();
        given(payload.getJsonArray("prosecutionCases")).willReturn(prosecutionCasesArray);

        //when
        listingCommandApi.handleUpdateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        verify(jsonObjectConverter, never()).convert(Matchers.any(), eq(ProsecutionCases.class));
    }

    @Test
    public void shouldVacateTheTrial() throws Exception {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        //when
        listingCommandApi.handleVacateTrial(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldListCourtHearing() throws Exception {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, ListCourtHearing.class)).willReturn(listCourtHearing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingCommandApi.handleListCourtHearing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldIssueListNextHearingsEnrichedCommand() {

        final ListNextHearingsV2 listNextHearings = mock(ListNextHearingsV2.class);
        final UUID seedingHearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();
        final ArgumentCaptor<ListNextHearingsEnrichedV2> payloadCaptor = ArgumentCaptor.forClass(ListNextHearingsEnrichedV2.class);

        //given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        when(jsonObjectConverter.convert(payload, ListNextHearingsV2.class)).thenReturn(listNextHearings);
        when(listNextHearings.getSeedingHearing()).thenReturn(SeedingHearing.seedingHearing()
                .withSittingDay(sittingDay)
                .withSeedingHearingId(seedingHearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build());

        final JsonValue finalPayload = mock(JsonValue.class);
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(finalPayload);

        //when
        listingCommandApi.listNextHearings(envelope);

        //then
        final ListNextHearingsEnrichedV2 value = payloadCaptor.getValue();
        assertThat(value, notNullValue(ListNextHearingsEnrichedV2.class));
        assertThat(value.getListNextHearings(), notNullValue());
        final SeedingHearing seedingHearing = value.getSeedingHearing();
        assertThat(seedingHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(seedingHearing.getSittingDay(), is(sittingDay));

    }

    @Test
    public void shouldPublishCourtList() {

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        listingCommandApi.handlePublishCourtList(envelope);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldPublishCourtListForCrownCourtsForwardsAsExpected() {

        listingCommandApi.handlePublishCourtListForCrownCourts(envelope);

        verify(sender).send(envelope);
    }

    @Test
    public void shouldCourtListRequestExportAsExpected() {

        listingCommandApi.handleCourtListRequestExport(envelope);

        verify(sender).send(envelope);
    }

    @Test
    public void shouldListUnscheduledCourtHearingAsExcepted() {

        final ListUnscheduledCourtHearing listUnscheduledCourtHearing = mock(ListUnscheduledCourtHearing.class);
        final ArgumentCaptor<ListUnscheduledCourtHearingEnriched> payloadCaptor = ArgumentCaptor.forClass(ListUnscheduledCourtHearingEnriched.class);
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        when(jsonObjectConverter.convert(payload, ListUnscheduledCourtHearing.class)).thenReturn(listUnscheduledCourtHearing);

        when(listUnscheduledCourtHearing.getHearings()).thenReturn(createUnscheduledListingNeeds(hearingId1, hearingId2));
        mockCourtCentres();

        final JsonValue finalPayload = mock(JsonValue.class);
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(finalPayload);

        // run
        listingCommandApi.handleListUnscheduledCourtHearing(envelope);

        // verify
        final ListUnscheduledCourtHearingEnriched value = payloadCaptor.getValue();
        assertThat(value, notNullValue(ListUnscheduledCourtHearingEnriched.class));
        assertThat(value.getCourtCentresDetails().size(), is(2));
        assertThat(value.getCourtCentresDetails().get(0).getDefaultDuration(), anyOf(is(10), is(20)));
        assertThat(value.getHearings().size(), is(2));
        assertThat(value.getHearings().get(0).getId(), is(hearingId1));
        assertThat(value.getHearings().get(1).getId(), is(hearingId2));

    }

    @Test
    public void shouldListUnscheduledNextHearings() {

        final ListUnscheduledNextHearings listUnscheduledNextHearings = mock(ListUnscheduledNextHearings.class);
        final ArgumentCaptor<ListUnscheduledNextHearingsEnriched> payloadCaptor = ArgumentCaptor.forClass(ListUnscheduledNextHearingsEnriched.class);
        final UUID seedHearingId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final String sittingDay = LocalDate.now().toString();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        when(jsonObjectConverter.convert(payload, ListUnscheduledNextHearings.class)).thenReturn(listUnscheduledNextHearings);

        when(listUnscheduledNextHearings.getHearings()).thenReturn(createUnscheduledListingNeeds(hearingId1, hearingId2));
        when(listUnscheduledNextHearings.getSeedingHearing()).thenReturn(SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedHearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withSittingDay(sittingDay)
                .build());
        mockCourtCentres();

        final JsonValue finalPayload = mock(JsonValue.class);
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(finalPayload);

        // run
        listingCommandApi.handleListUnscheduledNextCourtHearings(envelope);

        // verify
        final ListUnscheduledNextHearingsEnriched value = payloadCaptor.getValue();
        assertThat(value, notNullValue(ListUnscheduledNextHearingsEnriched.class));
        assertThat(value.getCourtCentresDetails().size(), is(2));
        assertThat(value.getCourtCentresDetails().get(0).getDefaultDuration(), anyOf(is(10), is(20)));
        assertThat(value.getHearings().size(), is(2));
        assertThat(value.getHearings().get(0).getId(), is(hearingId1));
        assertThat(value.getHearings().get(1).getId(), is(hearingId2));
        final SeedingHearing seedingHearing = value.getSeedingHearing();
        assertThat(seedingHearing.getSeedingHearingId(), is(seedHearingId));
        assertThat(seedingHearing.getSittingDay(), is(sittingDay));

    }

    @Test
    public void shouldDeleteNextHearings() {

        final ArgumentCaptor<DefaultEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(DefaultEnvelope.class);
        final UUID seedHearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();
        final JsonObject seedingHearing = Json.createObjectBuilder()
                .add("seedHearingId", seedHearingId.toString())
                .add("sittingDay", sittingDay)
                .add("jurisdictionType", "CROWN")
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("seedingHearing")).thenReturn(seedingHearing);

        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        // run
        listingCommandApi.handleDeleteNextHearings(envelope);

        // verify
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        final JsonObject jsonPayload = (JsonObject) senderJsonEnvelopeCaptor.getValue().payload();
        assertThat(jsonPayload.getJsonObject("seedingHearing"), is(seedingHearing));

    }


    @Test
    public void shouldCallNonDefaultDayDurationBuilder() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getJurisdictionType()).willReturn(MAGISTRATES);
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());
        given(updateHearingForListing.getSelectedCourtCentre()).willReturn(null);


        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());

        final UUID courtCentreId = randomUUID();
        given(updateHearingForListing.getCourtCentreId()).willReturn(courtCentreId);
        OrganisationUnit organisationUnit = new OrganisationUnit(randomUUID(), "abc");
        when(referenceDataService.getOrganizationUnitById(courtCentreId, envelope)).thenReturn(organisationUnit);
        when(stagingHmiService.isHmiListingEnabled(Matchers.any())).thenReturn(true);

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        listingCommandApi.handleUpdateHearingForListing(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestExceptionWhenJurisdictionTypeIsMagistratesCourtAndCourtRoomIdIsNull() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getJurisdictionType()).willReturn(MAGISTRATES);
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());
        given(updateHearingForListing.getSelectedCourtCentre()).willReturn(null);

        final UUID courtCentreId = randomUUID();
        given(updateHearingForListing.getCourtCentreId()).willReturn(courtCentreId);
        OrganisationUnit organisationUnit = new OrganisationUnit(randomUUID(), "abc");
        when(referenceDataService.getOrganizationUnitById(courtCentreId, envelope)).thenReturn(organisationUnit);
        when(stagingHmiService.isHmiListingEnabled(Matchers.any())).thenReturn(true);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        listingCommandApi.handleUpdateHearingForListing(envelope);
    }

    private List<NonDefaultDay> getNonDefaultDays() {
        return asList(new NonDefaultDay.Builder().withCourtScheduleId("134452").build());
    }

    @Test
    public void shouldExtendCourtHearing() {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId", null))
                .willReturn(randomUUID().toString());
        given(jsonObjectConverter.convert(payload, ExtendHearingForHearing.class))
                .willReturn(extendHearingForHearing);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(enveloperFunction.apply(any(ExtendHearingForHearingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingCommandApi.handleExtendHearingForHearing(envelope);

        //then
        verify(jsonObjectConverter, times(1)).convert(payload, ExtendHearingForHearing.class);
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldCreateListingNote() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CreateListingNote.class)).willReturn(createListingNote);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.command.create-listing-note");
        given(envelope.metadata()).willReturn(metadataBuilder.build());

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        listingCommandApi.handleCreateNote(envelope);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.create-listing-note"));
    }

    @Test
    public void shouldDeleteListingNote() {
        final JsonEnvelope command = mock(JsonEnvelope.class);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.command.handler.delete-listing-note");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        listingCommandApi.handleDeleteNote(command);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.handler.delete-listing-note"));
    }

    @Test
    public void shouldEditNote() {
        final JsonEnvelope command = mock(JsonEnvelope.class);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.command.handler.edit-listing-note");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        listingCommandApi.handleEditNote(command);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.handler.edit-listing-note"));

    }

    @Test
    public void shouldHandleCorrectHearingDaysWithoutCourtCentre() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleCorrectHearingDaysWithoutCourtCentre(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.correct-hearing-days-without-court-centre"));
    }

    @Test
    public void shouldHandleMarkUnallocatedHearingAsDuplicate() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleMarkUnallocatedHearingAsDuplicate(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.mark-unallocated-hearing-as-duplicate"));
    }

    @Test
    public void shouldUpdateRelatedHearing() {
        UpdateRelatedHearing updateRelatedHearing = UpdateRelatedHearing.updateRelatedHearing()
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(randomUUID())
                        .build())
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .build()))
                .build();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateRelatedHearing.class)).willReturn(updateRelatedHearing);

        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);
        when(payload.getString(anyString())).thenReturn(randomUUID().toString());

        listingCommandApi.updateRelatedHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.update-existing-hearing"));
    }

    @Test
    public void shouldUpdateHearingForListingFromHmi() {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getSelectedCourtCentre()).willReturn(null);
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());

        final UUID courtCentreId = randomUUID();
        given(updateHearingForListing.getCourtCentreId()).willReturn(courtCentreId);
        OrganisationUnit organisationUnit = new OrganisationUnit(randomUUID(), "abc");
        when(referenceDataService.getOrganizationUnitById(courtCentreId, envelope)).thenReturn(organisationUnit);
        when(stagingHmiService.isHmiListingEnabled(Matchers.any())).thenReturn(true);

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        final JsonArray prosecutionCasesArray = Json.createArrayBuilder().build();
        given(payload.getJsonArray("prosecutionCases")).willReturn(prosecutionCasesArray);

        //when
        listingCommandApi.handleUpdateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        verify(jsonObjectConverter, never()).convert(Matchers.any(), eq(ProsecutionCases.class));

    }

    @Test
    public void shouldHandleListingCommandDeleteHearing() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleDeleteHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.delete-hearing"));
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
        when(courtCentreFactory.getCourtCentre(Matchers.any(), eq(envelope))).thenReturn(
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(10).build(),
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(20).build());
    }



}

