package uk.gov.moj.cpp.listing.event.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.CacheRefDataCourtroomView;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtroomClosedEventProcessorTest {

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private CacheRefDataCourtroomView cacheRefDataCourtroomView;

    @InjectMocks
    private CourtroomClosedEventProcessor courtroomClosedEventProcessor;

    @Test
    public void shouldCloseReferenceDataCourtRoom() {

        UUID roomId = UUID.randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("id", roomId.toString())
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("public.referencedata.event.courtroom-closed").build());

        final ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        courtroomClosedEventProcessor.closeReferenceDataCourtRoom(jsonEnvelope);

        verify(cacheRefDataCourtroomView).closeRefDataCourtroom(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is("listing.update.close-courtroom"));
    }
} 