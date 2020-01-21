package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TypeForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final Type TYPE = Type.type().withId(UUID.randomUUID()).withDescription("TRIAL").build();
    private static final String TYPE_FIELD = "type";

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private TypeForHearingEventListener typeForHearingEventListener;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @Mock
    private JsonObject hearingTypeJsonObject;

    @Test
    public void shouldChangeTypeForHearing() {
        Envelope<TypeChangedForHearing> envelope = (Envelope<TypeChangedForHearing>) mock(Envelope.class);
        TypeChangedForHearing hearingData = TypeChangedForHearing.typeChangedForHearing()
                .withType(TYPE)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);


        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);
        when(objectToJsonObjectConverter.convert(TYPE)).thenReturn(hearingTypeJsonObject);
        when(hearingTypeJsonObject.toString()).thenReturn("{ \"Hello\": \"World\"}");

        typeForHearingEventListener.typeChangedForHearing(envelope);

        verify(properties).set(eq(TYPE_FIELD), any(JsonNode.class));
        verify(hearingRepository).save(hearing);
    }
}