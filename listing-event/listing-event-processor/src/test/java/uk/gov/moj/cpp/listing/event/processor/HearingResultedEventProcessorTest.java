package uk.gov.moj.cpp.listing.event.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

@ExtendWith(MockitoExtension.class)
class HearingResultedEventProcessorTest {


    @InjectMocks
    private HearingResultedEventProcessor hearingResultedEventProcessor;

    @Mock
    private Sender sender;

    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Test
    void shouldSendCommandWhenHearingIsNotSJP() {
        JsonObject hearingJson = JsonObjects.createObjectBuilder()
                .add("id", "hearing-id-123")
                .add("isSJPHearing", false)
                .build();
        JsonObject eventPayload = JsonObjects.createObjectBuilder()
                .add("hearing", hearingJson)
                .build();

        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), eventPayload);

        hearingResultedEventProcessor.handlePublicHearingResulted(envelope);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope commandJsonEnvelope = senderJsonEnvelopeCaptor.getValue();
        assertThat(commandJsonEnvelope.metadata().name(), is("listing.command.set-hearing-resulted-status"));
        final JsonObject commandPayload = commandJsonEnvelope.payloadAsJsonObject();
        assertThat(commandPayload.getString("hearingId"), is("hearing-id-123"));

    }

    @Test
    void shouldNotSendCommandWhenHearingIsSJP() {
        JsonObject hearingJson = JsonObjects.createObjectBuilder()
                .add("id", "hearing-id-123")
                .add("isSJPHearing", true)
                .build();
        JsonObject eventPayload = JsonObjects.createObjectBuilder()
                .add("hearing", hearingJson)
                .build();

        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), eventPayload);


        hearingResultedEventProcessor.handlePublicHearingResulted(envelope);

        verifyNoInteractions(sender);
    }
}