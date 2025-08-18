package uk.gov.moj.cpp.listing.event.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.CacheRefDataCourtroomView;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtroomAddedEventProcessorTest {

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private CacheRefDataCourtroomView cacheRefDataCourtroomView;

    @InjectMocks
    private CourtroomAddedEventProcessor courtroomAddedEventProcessor;

    @Test
    void shouldAddReferenceDataCourtRoom() {

        UUID roomId = UUID.randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("id", roomId.toString())
                .add("courtroomName", "Court Room 1")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("public.referencedata.event.courtroom-added").build());

        final ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        courtroomAddedEventProcessor.addReferenceDataCourtRoom(jsonEnvelope);

        verify(cacheRefDataCourtroomView).addRefDataCourtroom(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("listing.update.add-courtroom"));
    }
}
