package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing.weekCommencingDateChangedForHearing;
import static uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing.weekCommencingDateRemovedForHearing;

import uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing;
import uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WeekCommencingDateEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.now();
    private static final LocalDate WEEK_COMMENCING_END_DATE = LocalDate.now().plusDays(7l);
    private static final Integer WEEK_COMMENCING_DURATION = 1;

    private static final String WEEK_COMMENCING_START_DATE_FIELD = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE_FIELD = "weekCommencingEndDate";
    private static final String WEEK_COMMENCING_DURATION_FIELD = "weekCommencingDurationInWeeks";


    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @InjectMocks
    private WeekCommencingDateEventListener weekCommencingDateEventListener;

    @Test
    public void shouldChangeWeekCommencingDateForHearing() throws Exception {
        final Envelope<WeekCommencingDateChangedForHearing> envelope = (Envelope<WeekCommencingDateChangedForHearing>) mock(Envelope.class);
        final WeekCommencingDateChangedForHearing hearingData = weekCommencingDateChangedForHearing()
                .withHearingId(HEARING_ID)
                .withWeekCommencingStartDate(WEEK_COMMENCING_START_DATE.toString())
                .withWeekCommencingEndDate(WEEK_COMMENCING_END_DATE.toString())
                .withWeekCommencingDurationInWeeks(WEEK_COMMENCING_DURATION)
                .build();

        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        weekCommencingDateEventListener.weekCommencingAssignedForHearing(envelope);

        verify(properties).put(eq(WEEK_COMMENCING_START_DATE_FIELD), eq(WEEK_COMMENCING_START_DATE.toString()));
        verify(properties).put(eq(WEEK_COMMENCING_END_DATE_FIELD), eq(WEEK_COMMENCING_END_DATE.toString()));
        verify(properties).put(eq(WEEK_COMMENCING_DURATION_FIELD), eq(WEEK_COMMENCING_DURATION.toString()));

        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRemoveWeekCommencingDateForHearing() throws Exception {
        final Envelope<WeekCommencingDateRemovedForHearing> envelope = (Envelope<WeekCommencingDateRemovedForHearing>) mock(Envelope.class);
        final WeekCommencingDateRemovedForHearing hearingData = weekCommencingDateRemovedForHearing()
                .withHearingId(HEARING_ID)
                .build();

        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        weekCommencingDateEventListener.weekCommencingRemovedForHearing(envelope);

        verify(properties).remove(WEEK_COMMENCING_START_DATE_FIELD);
        verify(properties).remove(WEEK_COMMENCING_END_DATE_FIELD);
        verify(properties).remove(WEEK_COMMENCING_DURATION_FIELD);

        verify(hearingRepository).save(hearing);
    }
}