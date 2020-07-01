package uk.gov.moj.cpp.listing.command.api;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.ExtendHearingForHearing;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;

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
    @InjectMocks
    private ListingCommandApi listingCommandApi;
    @Mock
    private ExtendHearingForHearing extendHearingForHearing;

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

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        final UUID caseId = UUID.randomUUID();
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
        final UUID hearingId1 = UUID.randomUUID();
        final UUID hearingId2 = UUID.randomUUID();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());


        when(jsonObjectConverter.convert(payload, ListUnscheduledCourtHearing.class)).thenReturn(listUnscheduledCourtHearing);


        when(listUnscheduledCourtHearing.getHearings()).thenReturn(createUnscheduledListingNeeds(hearingId1, hearingId2));
        when(courtCentreFactory.getCourtCentre(Matchers.any(), eq(envelope))).thenReturn(
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(10).build(),
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(20).build());

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

    public List<HearingUnscheduledListingNeeds> createUnscheduledListingNeeds(final UUID hearingId1, final UUID hearingId2) {
        return Arrays.asList(
                HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).build())
                        .withId(hearingId1)
                        .build(),
                HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).build())
                        .withId(hearingId2)
                        .build());
    }

    @Test
    public void shouldCallNonDefaultDayDurationBuilder() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getJurisdictionType()).willReturn(MAGISTRATES);
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());


        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        listingCommandApi.handleUpdateHearingForListing(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
    }

    private List<NonDefaultDay> getNonDefaultDays() {
        return asList(new NonDefaultDay.Builder().withCourtScheduleId(of("134452")).build());
    }

    @Test
    public void shouldExtendCourtHearing() {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId", null))
                .willReturn(UUID.randomUUID().toString());
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

}

