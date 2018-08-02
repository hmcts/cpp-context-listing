package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtRoomForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private CourtRoomForHearingEventListener courtRoomForHearingEventListener;


    @Test
    public void shouldAssignCourtRoomToHearing() throws Exception {
        Envelope<CourtRoomAssignedToHearing> envelope = (Envelope<CourtRoomAssignedToHearing>) mock(Envelope.class);
        CourtRoomAssignedToHearing hearingData = CourtRoomAssignedToHearing.courtRoomAssignedToHearing()
                .withCourtRoomId(COURT_ROOM_ID)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        courtRoomForHearingEventListener.courtRoomAssignedToHearing(envelope);
        verify(hearingRepository).updateCourtRoomId(COURT_ROOM_ID, HEARING_ID);


    }

    @Test
    public void shouldChangeCourtRoomForHearing() throws Exception {
        Envelope<CourtRoomChangedForHearing> envelope = (Envelope<CourtRoomChangedForHearing>) mock(Envelope.class);
        CourtRoomChangedForHearing hearingData = CourtRoomChangedForHearing.courtRoomChangedForHearing()
                .withCourtRoomId(COURT_ROOM_ID)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);


        courtRoomForHearingEventListener.courtRoomChangedForHearing(envelope);
        verify(hearingRepository).updateCourtRoomId(COURT_ROOM_ID, HEARING_ID);
    }

    @Test
    public void shouldRemoveCourtRoomFromHearing() throws Exception {
        Envelope<CourtRoomRemovedFromHearing> envelope = (Envelope<CourtRoomRemovedFromHearing>) mock(Envelope.class);
        CourtRoomRemovedFromHearing hearingData = CourtRoomRemovedFromHearing.courtRoomRemovedFromHearing()
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        courtRoomForHearingEventListener.courtRoomRemovedFromHearing(envelope);
        verify(hearingRepository).updateCourtRoomId(null, HEARING_ID);
    }
}
