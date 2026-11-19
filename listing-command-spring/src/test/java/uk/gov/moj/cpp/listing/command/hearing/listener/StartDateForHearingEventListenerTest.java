package uk.gov.moj.cpp.listing.command.hearing.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.StartDateRemovedForHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.command.hearing.rules.utils.TestRandom;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StartDateForHearingEventListenerTest {
    private static final UUID HEARING_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.now().minusDays(30);
    private static final String START_DATE_FIELD = "startDate";

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @InjectMocks
    private StartDateForHearingEventListener startDateForHearingEventListener;

    @Test
    public void shouldChangeStartDateForHearing() throws Exception {
        RaisedEvent<StartDateChangedForHearing> envelope = (RaisedEvent<StartDateChangedForHearing>) mock(RaisedEvent.class);
        StartDateChangedForHearing hearingData = StartDateChangedForHearing.startDateChangedForHearing()
                .withHearingId( HEARING_ID)
                .withStartDate(START_DATE.toString())
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        startDateForHearingEventListener.startDateChangedForHearing(envelope);

        verify(properties).put(eq(START_DATE_FIELD), eq(START_DATE.toString()));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRemoveStartDateForHearing() throws Exception {
        final RaisedEvent<StartDateRemovedForHearing> envelope = (RaisedEvent<StartDateRemovedForHearing>) mock(RaisedEvent.class);
        final StartDateRemovedForHearing hearingData = StartDateRemovedForHearing.startDateRemovedForHearing().withHearingId(HEARING_ID).build();

        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        startDateForHearingEventListener.startDateRemovedForHearing(envelope);

        verify(properties).remove("startDate");
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRemoveStartDateAndSetUnscheduledForHearing() throws Exception {
        final RaisedEvent<StartDateRemovedForHearing> envelope = (RaisedEvent<StartDateRemovedForHearing>) mock(RaisedEvent.class);
        final StartDateRemovedForHearing hearingData = StartDateRemovedForHearing.startDateRemovedForHearing()
                .withHearingId(HEARING_ID)
                .withUnscheduled(true)
                .build();

        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        startDateForHearingEventListener.startDateRemovedForHearing(envelope);

        verify(properties).remove("startDate");
        verify(properties).put("unscheduled", true);
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRemoveStartDateAndNotSetUnscheduledForHearing() throws Exception {
        final RaisedEvent<StartDateRemovedForHearing> envelope = (RaisedEvent<StartDateRemovedForHearing>) mock(RaisedEvent.class);
        final StartDateRemovedForHearing hearingData = StartDateRemovedForHearing.startDateRemovedForHearing()
                .withHearingId(HEARING_ID)
                .withUnscheduled(false)
                .build();

        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        startDateForHearingEventListener.startDateRemovedForHearing(envelope);

        verify(properties).remove("startDate");
        verify(properties, never()).put("unscheduled", true);
        verify(hearingRepository).save(hearing);
    }
}