package uk.gov.moj.cpp.listing.event.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonObject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.listing.event.processor.CreateNextHearingRequestedEventProcessor.PUBLIC_EVENT_CREATE_NEXT_HEARING_REQUESTED;

@ExtendWith(MockitoExtension.class)
public class CreateNextHearingRequestedEventProcessorTest {

    @InjectMocks
    private CreateNextHearingRequestedEventProcessor createNextHearingRequestedEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Mock
    private Sender sender;

    @Mock
    private Logger logger;


    @Test
    public void shouldHandlePublicListingCreateNewHearingRequestedEvent() {
        //given
        final JsonEnvelope event = listingEventCreateNewHearingRequested();
        //when
        createNextHearingRequestedEventProcessor.createNextHearingRequestedEvent(event);
        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_EVENT_CREATE_NEXT_HEARING_REQUESTED));
    }

    private JsonEnvelope listingEventCreateNewHearingRequested() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("createNextHearing", Json.createObjectBuilder()
                                .add("committingCourt", "Birmingham Crown Court")
                                .add("hearing", Json.createObjectBuilder().build())
                        .build())
                .build();
        return envelopeFrom(metadataWithRandomUUIDAndName(), payload);


    }
}
