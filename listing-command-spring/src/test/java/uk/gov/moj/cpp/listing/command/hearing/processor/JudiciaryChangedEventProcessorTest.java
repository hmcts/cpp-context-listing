package uk.gov.moj.cpp.listing.command.hearing.processor;

import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.listing.command.hearing.processor.ProcessorTestSupport.metadataWithRandomUUIDAndName;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import jakarta.json.Json;
import jakarta.json.JsonObject;

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
    private ProcessorSender sender;

    private Envelope envelope;

    @Captor
    private ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldProcessJudiciaryChangedEvent() {

        final JsonObject payload = Json.createObjectBuilder()
                .add(STATUS, "Success")
                .build();

        envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);
        judiciaryChangedEventProcessor.handlesJudiciaryChangedForHearingsStatus(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        final Envelope pubJsonEnv = senderJsonEnvelopeCaptor.getValue();
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_LISTING_EVENTS_JUDICIARY_CHANGED_FOR_HEARINGS_STATUS));

        final JsonObject pubJsonObj = pubJsonEnv.payload();
        assertThat(pubJsonObj.getString(STATUS), is("Success"));
    }

}
