package uk.gov.moj.cpp.listing.command.hearing.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.EndDateRemovedFromHearing;
import uk.gov.moj.cpp.listing.domain.json.LocalDates;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
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
public class EndDateForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final LocalDate END_DATE = LocalDate.now();
    private static final String JSON_PATH = "";
    private static final String END_DATE_FIELD = "endDate";

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @InjectMocks
    private EndDateForHearingEventListener endDateForHearingEventListener;


    @Test
    public void shouldChangeEndDateForHearing() {
        RaisedEvent<EndDateChangedForHearing> envelope = (RaisedEvent<EndDateChangedForHearing>) mock(RaisedEvent.class);
        EndDateChangedForHearing hearingData = EndDateChangedForHearing.endDateChangedForHearing()
                .withEndDate(LocalDates.to(END_DATE))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        endDateForHearingEventListener.endDateChangedForHearing(envelope);

        verify(properties).put(eq(END_DATE_FIELD), eq(END_DATE.toString()));
        verify(hearingRepository).save(hearing);
    }


    @Test
    public void shouldRemoveEndDateFromHearing() {
        RaisedEvent<EndDateRemovedFromHearing> envelope = (RaisedEvent<EndDateRemovedFromHearing>) mock(RaisedEvent.class);
        EndDateRemovedFromHearing hearingData = EndDateRemovedFromHearing.endDateRemovedFromHearing()
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        endDateForHearingEventListener.endDateRemovedFromHearing(envelope);

        verify(properties).remove("endDate");
        verify(hearingRepository).save(hearing);
    }
}
