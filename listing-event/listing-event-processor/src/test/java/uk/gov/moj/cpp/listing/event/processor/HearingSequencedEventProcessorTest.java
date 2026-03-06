package uk.gov.moj.cpp.listing.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.listing.event.processor.HearingSequencedEventProcessor.PUBLIC_LISTING_EVENTS_HEARING_SEQUENCED;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingSequencedEventProcessorTest {

    public static final String ID = "id";
    public static final String HEARING_DATE = "hearingDate";
    public static final String SEQUENCE = "sequence";
    public static final String SEQUENCE_HEARING_DAYS = "sequenceHearingDays";

    @InjectMocks
    private HearingSequencedEventProcessor hearingSequencedEventProcessor;

    @Mock
    private Sender sender;

    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldHandleHearingSequencedEvent() {
        UUID id = UUID.randomUUID();
        final JsonObject seq1JsonObject = JsonObjects.createObjectBuilder().add(HEARING_DATE, "2025-01-08").add(SEQUENCE, "1").build();
        final JsonObject seq2JsonObject = JsonObjects.createObjectBuilder().add(HEARING_DATE, "2025-01-10").add(SEQUENCE, "2").build();
        final JsonArray seqJsonArr = JsonObjects.createArrayBuilder().add(seq1JsonObject).add(seq2JsonObject).build();
        final JsonObject payLoad = JsonObjects.createObjectBuilder()
                .add(ID, id.toString())
                .add(SEQUENCE_HEARING_DAYS, seqJsonArr)
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);
        hearingSequencedEventProcessor.handleHearingSequencedEvent(envelope);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope pubJsonEnv = senderJsonEnvelopeCaptor.getValue();
        assertThat(pubJsonEnv.metadata().name(), is(PUBLIC_LISTING_EVENTS_HEARING_SEQUENCED));
        final JsonObject pubJsonObj = pubJsonEnv.payloadAsJsonObject();
        assertThat(pubJsonObj.getString(ID), is(id.toString()));

        final JsonArray pubJsonArr = pubJsonObj.getJsonArray(SEQUENCE_HEARING_DAYS);
        assertThat(pubJsonArr.size(), is(2));

        final JsonObject pubSeq1JsonObj = pubJsonArr.getJsonObject(0);
        assertThat(pubSeq1JsonObj.getString(HEARING_DATE), is("2025-01-08"));
        assertThat(pubSeq1JsonObj.getString(SEQUENCE), is("1"));

        final JsonObject pubSeq2JsonObj = pubJsonArr.getJsonObject(1);
        assertThat(pubSeq2JsonObj.getString(HEARING_DATE), is("2025-01-10"));
        assertThat(pubSeq2JsonObj.getString(SEQUENCE), is("2"));
    }
}

