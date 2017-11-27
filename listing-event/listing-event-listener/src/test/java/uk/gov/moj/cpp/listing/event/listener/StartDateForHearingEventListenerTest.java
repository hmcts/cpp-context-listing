package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.StartDateChangedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StartDateForHearingEventListenerTest {
    private static final UUID HEARING_ID = randomUUID();
    private static final LocalDate START_DATE = RandomGenerator.PAST_LOCAL_DATE.next();


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private StartDateForHearingEventListener startDateForHearingEventListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;
    
    @Test
    public void shouldChangeStartDateForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        StartDateChangedForHearing hearingData = new StartDateChangedForHearing(START_DATE, HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, StartDateChangedForHearing.class))
                .willReturn(hearingData);

        startDateForHearingEventListener.startDateChangedForHearing(envelope);
        verify(hearingRepository).updateStartDate(START_DATE, HEARING_ID);
    }
}