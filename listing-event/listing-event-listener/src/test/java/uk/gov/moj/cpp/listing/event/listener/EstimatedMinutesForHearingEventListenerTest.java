package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.EstimateMinutesChangedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class EstimatedMinutesForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final Integer ESTIMATE_MINUTES = RandomGenerator.INTEGER.next();


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private EstimateMinutesForHearingEventListener estimateMinutesForHearingEventListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Test
    public void shouldChangeEstimatedMinutesForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        EstimateMinutesChangedForHearing hearingData = new EstimateMinutesChangedForHearing(ESTIMATE_MINUTES, HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, EstimateMinutesChangedForHearing.class))
                .willReturn(hearingData);

        estimateMinutesForHearingEventListener.estimateMinutesChangedForHearing(envelope);
        verify(hearingRepository).updateEstimateMinutes(ESTIMATE_MINUTES, HEARING_ID);
    }
}