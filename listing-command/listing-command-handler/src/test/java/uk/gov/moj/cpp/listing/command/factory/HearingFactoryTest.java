package uk.gov.moj.cpp.listing.command.factory;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.service.HearingService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingFactoryTest {
    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private HearingService hearingService;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private Hearing hearing;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private HearingFactory hearingFactory;

    private static final UUID HEARING_ID = randomUUID();

    @Test
    public void shouldReturnHearingTypes() {

        hearing = Hearing.hearing().withId(HEARING_ID).build();

        //given
        given(hearingService.getHearingById(HEARING_ID, envelope)).willReturn(jsonEnvelope);
        given(jsonEnvelope.payloadAsJsonObject()).willReturn(jsonObject);
        given(jsonObjectToObjectConverter.convert(jsonObject, Hearing.class)).willReturn(hearing);

        //when
        Hearing resultHearing = hearingFactory.getHearingById(HEARING_ID, envelope);

        //verify
        verify(hearingService, times(1)).getHearingById(HEARING_ID, envelope);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, Hearing.class);

        //then
        assertThat(resultHearing.getId(), is(HEARING_ID));
    }
}
