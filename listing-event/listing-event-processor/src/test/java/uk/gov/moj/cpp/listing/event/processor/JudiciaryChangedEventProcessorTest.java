package uk.gov.moj.cpp.listing.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JudiciaryChangedEventProcessorTest {

    private static final String PUBLIC_LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS = "public.listing.judiciary-changed-for-hearings-status";
    public static final String STATUS = "status";

    @InjectMocks
    private JudiciaryChangedEventProcessor judiciaryChangedEventProcessor;

    @Mock
    private Sender sender;

    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldProcessJudiciaryChangedEvent() {

        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add(STATUS, "Success")
                .build();

        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payload);
        judiciaryChangedEventProcessor.handlesJudiciaryChangedForHearingsStatus(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope pubJsonEnv = senderJsonEnvelopeCaptor.getValue();
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS));

        final JsonObject pubJsonObj = pubJsonEnv.payloadAsJsonObject();
        assertThat(pubJsonObj.getString(STATUS), is("Success"));
    }

}
