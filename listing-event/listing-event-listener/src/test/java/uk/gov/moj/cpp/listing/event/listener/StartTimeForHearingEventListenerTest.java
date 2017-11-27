package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.StartTimeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.StartTimeChangedForHearing;
import uk.gov.moj.cpp.listing.event.StartTimeRemovedFromHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StartTimeForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final LocalTime START_TIME = LocalTime.now();


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private StartTimeForHearingEventListener startTimeForHearingEventListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Test
    public void shouldAssignStartTimeToHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        StartTimeAssignedToHearing hearingData = new StartTimeAssignedToHearing(START_TIME, HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, StartTimeAssignedToHearing.class))
                .willReturn(hearingData);

        startTimeForHearingEventListener.startTimeAssignedForHearing(envelope);
        verify(hearingRepository).updateStartTime(START_TIME, HEARING_ID);
    }

    @Test
    public void shouldChangeStartTimeForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        StartTimeChangedForHearing hearingData = new StartTimeChangedForHearing(START_TIME, HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, StartTimeChangedForHearing.class))
                .willReturn(hearingData);

        startTimeForHearingEventListener.startTimeChangedForHearing(envelope);
        verify(hearingRepository).updateStartTime(START_TIME, HEARING_ID);
    }

    @Test
    public void shouldRemoveStartTimeFromHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        StartTimeRemovedFromHearing hearingData = new StartTimeRemovedFromHearing(HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, StartTimeRemovedFromHearing.class))
                .willReturn(hearingData);

        startTimeForHearingEventListener.startTimeRemovedFromHearing(envelope);
        verify(hearingRepository).updateStartTime(null, HEARING_ID);
    }
}