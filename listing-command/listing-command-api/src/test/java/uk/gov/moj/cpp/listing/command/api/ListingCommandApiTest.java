package uk.gov.moj.cpp.listing.command.api;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.helper.ServiceComponents.verifyPassThroughCommandHandlerMethod;
import static uk.gov.moj.cpp.listing.command.api.ListingCommandApi.LISTING_COMMAND_SEND_CASE_FOR_LISTING_ENRICHED;
import static uk.gov.moj.cpp.listing.command.api.ListingCommandApi.LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED;

import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.commands.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.courts.SendCaseForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;

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
    private SendCaseForListing sendCaseForListing;

    @InjectMocks
    private ListingCommandApi ListingCommandApi;

    @Spy
    private Enveloper enveloper = createEnveloper();


    @Test
    public void testIfMethodsArePassThrough() throws Exception {
        verifyPassThroughCommandHandlerMethod(ListingCommandApi.class, "changeJudiciaryForHeraings", "sequenceHearings");
    }


    @Test
    public void listingCommandHandlerShouldUpdateHearingForListing() throws Exception {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(enveloper.withMetadataFrom(envelope, LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED)).willReturn
                (enveloperFunction);

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        //when
        ListingCommandApi.updateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void listingCommandHandlerShouldSendCaseForListing() throws Exception {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, SendCaseForListing.class)).willReturn(sendCaseForListing);
        given(enveloper.withMetadataFrom(envelope, LISTING_COMMAND_SEND_CASE_FOR_LISTING_ENRICHED)).willReturn
                (enveloperFunction);

        given(enveloperFunction.apply(any(UpdateHearingForListingEnriched.class))).willReturn(finalEnvelope);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        ListingCommandApi.sendCaseForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }
}

