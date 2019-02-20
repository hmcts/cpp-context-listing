package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.EndDateRemovedFromHearing;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EndDateForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final LocalDate END_DATE = LocalDate.now();
    private static final String JSON_PATH = "";
    private static final String END_DATE_FIELD = "endDate";

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @InjectMocks
    private EndDateForHearingEventListener endDateForHearingEventListener;


    @Test
    public void shouldChangeEndDateForHearing() {
        Envelope<EndDateChangedForHearing> envelope = (Envelope<EndDateChangedForHearing>) mock(Envelope.class);
        EndDateChangedForHearing hearingData = EndDateChangedForHearing.endDateChangedForHearing()
                .withEndDate(LocalDates.to(END_DATE))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);
        when(properties.findPath(JSON_PATH)).thenReturn(properties);

        endDateForHearingEventListener.endDateChangedForHearing(envelope);

        verify(properties).put(eq(END_DATE_FIELD), eq(END_DATE.toString()));
        verify(hearingRepository).save(hearing);
    }


    @Test
    public void shouldRemoveEndDateFromHearing() {
        Envelope<EndDateRemovedFromHearing> envelope = (Envelope<EndDateRemovedFromHearing>) mock(Envelope.class);
        EndDateRemovedFromHearing hearingData = EndDateRemovedFromHearing.endDateRemovedFromHearing()
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);
        when(properties.findPath(JSON_PATH)).thenReturn(properties);

        endDateForHearingEventListener.endDateRemovedFromHearing(envelope);

        verify(properties).remove("endDate");
        verify(hearingRepository).save(hearing);
    }
}
