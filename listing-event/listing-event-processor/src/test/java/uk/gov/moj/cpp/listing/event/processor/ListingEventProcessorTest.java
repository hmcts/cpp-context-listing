package uk.gov.moj.cpp.listing.event.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_CASE_SENT_FOR_LISTING;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingEventProcessorTest {

    @InjectMocks
    private ListingEventProcessor listingEventProcessor;
    @Mock
    private Enveloper enveloper;
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
    private CaseSentForListing caseSentForListing;

    @Mock
    private List<Hearing> hearings;

    @Test
    public void shouldHandleCaseSentForListingEventMessage() throws Exception {
        //Given
        hearings = Arrays.asList(new Hearing(UUID.randomUUID().toString(), new StringGenerator().next(), new StringGenerator().next(), new StringGenerator().next(), null, 15, Arrays.asList(), false));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, CaseSentForListing.class)).thenReturn(caseSentForListing);
        when(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_CASE_SENT_FOR_LISTING)).thenReturn
                (enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "listing.command.list-hearing")).thenReturn
                (enveloperFunction);
        when(enveloperFunction.apply(any(CaseSentForListing.class))).thenReturn(finalEnvelope);
        when(enveloperFunction.apply(any(Hearing.class))).thenReturn(finalEnvelope);
        when(caseSentForListing.getHearings()).thenReturn(hearings);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleCaseSentForListingMessage(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
    }

}