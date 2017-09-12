package uk.gov.moj.cpp.listing.event.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.CASE_SENT_FOR_LISTING_PUB_EVENT;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
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
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Mock
    private CaseSentForListing caseSentForListing;

    @Test
    public void shouldHandleCaseSentForListingEventMessage() throws Exception {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, CaseSentForListing.class)).thenReturn(caseSentForListing);
        when(enveloper.withMetadataFrom(envelope, CASE_SENT_FOR_LISTING_PUB_EVENT)).thenReturn
                (enveloperFunction);
        when(enveloperFunction.apply(any(CaseSentForListing.class))).thenReturn(finalEnvelope);

        listingEventProcessor.handleCaseSentForListingMessage(envelope);
        verify(sender).send(finalEnvelope);
    }

}