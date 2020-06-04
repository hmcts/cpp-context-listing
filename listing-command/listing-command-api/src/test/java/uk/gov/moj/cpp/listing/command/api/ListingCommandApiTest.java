package uk.gov.moj.cpp.listing.command.api;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.ExtendHearingForHearing;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
    public void testIfMethodsArePassThrough() throws Exception {

        assertThat(ListingCommandApi.class, isHandlerClass(COMMAND_API)
                .with(method("changeJudiciaryForHeraings")
                        .thatHandles("listing.command.change-judiciary-for-hearings"))
                .with(method("sequenceHearings")
                        .thatHandles("listing.command.sequence-hearings")));
    }


    @Test
    public void listingCommandHandlerShouldUpdateHearingForListing() throws Exception {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        //when
        listingCommandApi.updateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void listingCommandHandlerShouldVacateTheTrial() throws Exception {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        //when
        listingCommandApi.vacateTrial(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void listingCommandHandlerShouldListCourtHearing() throws Exception {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, ListCourtHearing.class)).willReturn(listCourtHearing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingCommandApi.listCourtHearing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void listingCommandPublishCourtList() {

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        listingCommandApi.publishCourtList(envelope);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    public void publishCourtListForCrownCourtsForwardsAsExpected() {

        listingCommandApi.publishCourtListForCrownCourts(envelope);

        verify(sender).send(envelope);
    }

    public void courtListRequestExportAsExpected() {

        listingCommandApi.courtListRequestExport(envelope);

        verify(sender).send(envelope);
    }

    @Test
    public void listingCommandApiShouldCallNonDefaultDayDurationBuilder() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getJurisdictionType()).willReturn(MAGISTRATES);
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());


        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getNonDefaultDays()).willReturn(getNonDefaultDays());

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        listingCommandApi.updateHearingForListing(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
    }

    private List<NonDefaultDay> getNonDefaultDays() {
        return asList(new NonDefaultDay.Builder().withCourtScheduleId(of("134452")).build());
    }

    @Test
    public void listingCommandHandlerShouldExtendCourtHearing() throws Exception {

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
        listingCommandApi.extendHearingForHearing(envelope);

        //then
        verify(jsonObjectConverter, times(1)).convert(payload, ExtendHearingForHearing.class);
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

}

