package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.StartTimesAssignedToHearing.startTimesAssignedToHearing;
import static uk.gov.justice.listing.events.StartTimesChangedForHearing.startTimesChangedForHearing;

import uk.gov.justice.listing.events.StartTimesAssignedToHearing;
import uk.gov.justice.listing.events.StartTimesChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.converter.StartTimesJsonConverter;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class StartTimesForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime START_TIME = ZonedDateTime.now();


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private StartTimesForHearingEventListener startTimeForHearingEventListener;

    @Mock
    private StartTimesJsonConverter startTimesJsonConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<String> captor;

    @Test
    public void shouldAssignStartTimeToHearing() throws Exception {

        //given
        Envelope<StartTimesAssignedToHearing> envelope = (Envelope<StartTimesAssignedToHearing>) mock(Envelope.class);
        StartTimesAssignedToHearing hearingData = startTimesAssignedToHearing()
                .withStartTimes(Arrays.asList(START_TIME))
                .withHearingId(HEARING_ID)
                .build();
        String startTimesJson = "{startTimes: [\"" + START_TIME.toString() + "\"]";
        given(envelope.payload()).willReturn(hearingData);
        given(startTimesJsonConverter.convertStartTimesTo(hearingData.getStartTimes())).willReturn(startTimesJson);

        //when
        startTimeForHearingEventListener.startTimesAssignedForHearing(envelope);

        //then
        verify(hearingRepository).updateStartTimes(captor.capture(), any(UUID.class));
        String actualJson = captor.getValue();
        assertThat(actualJson, equalTo(startTimesJson));

    }
    @Test
    public void shouldChangeStartTimeForHearing() throws Exception {
        //given
        Envelope<StartTimesChangedForHearing> envelope = (Envelope<StartTimesChangedForHearing>) mock(Envelope.class);
        StartTimesChangedForHearing hearingData = startTimesChangedForHearing()
                .withStartTimes(Arrays.asList(START_TIME))
                .withHearingId(HEARING_ID)
                .build();
        String startTimesJson ="{startTimes: [\""+START_TIME.toString()+"\"]";
        given(envelope.payload()).willReturn(hearingData);
        given(startTimesJsonConverter.convertStartTimesTo(hearingData.getStartTimes())).willReturn(startTimesJson);

        //when
        startTimeForHearingEventListener.startTimesChangedForHearing(envelope);

        //then
        verify(hearingRepository).updateStartTimes(captor.capture(), any(UUID.class));
        String actualJson = captor.getValue();
        assertThat(actualJson, equalTo(startTimesJson));
    }
}