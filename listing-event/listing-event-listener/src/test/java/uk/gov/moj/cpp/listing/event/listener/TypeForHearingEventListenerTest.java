package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.TypeChangedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TypeForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private TypeForHearingEventListener typeForHearingEventListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Test
    public void shouldChangeTypeForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        TypeChangedForHearing hearingData = new TypeChangedForHearing(TYPE, HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, TypeChangedForHearing.class))
                .willReturn(hearingData);

        typeForHearingEventListener.typeChangedForHearing(envelope);
        verify(hearingRepository).updateType(TYPE, HEARING_ID);
    }
}