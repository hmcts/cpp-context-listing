package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
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
         Envelope<TypeChangedForHearing> envelope = (Envelope<TypeChangedForHearing>) mock(Envelope.class);
        TypeChangedForHearing hearingData = TypeChangedForHearing.typeChangedForHearing()
                .withType(TYPE)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        typeForHearingEventListener.typeChangedForHearing(envelope);
        verify(hearingRepository).updateType(TYPE, HEARING_ID);
    }
}