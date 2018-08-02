package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

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

    @Test
    public void shouldChangeStartDateForHearing() throws Exception {
        Envelope<StartDateChangedForHearing> envelope = (Envelope<StartDateChangedForHearing>) mock(Envelope.class);
        StartDateChangedForHearing hearingData = StartDateChangedForHearing.startDateChangedForHearing()
                .withHearingId( HEARING_ID)
                .withStartDate(START_DATE.toString())
                .build();
        given(envelope.payload()).willReturn(hearingData);

        startDateForHearingEventListener.startDateChangedForHearing(envelope);
        verify(hearingRepository).updateStartDate(START_DATE, HEARING_ID);
    }
}