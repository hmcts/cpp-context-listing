package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CourtRoomForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final String JSON_PATH = "";

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @InjectMocks
    private CourtRoomForHearingEventListener courtRoomForHearingEventListener;

    @Test
    public void shouldAssignCourtRoomToHearing() {
        Envelope<CourtRoomAssignedToHearing> envelope = (Envelope<CourtRoomAssignedToHearing>) mock(Envelope.class);
        CourtRoomAssignedToHearing hearingData = CourtRoomAssignedToHearing.courtRoomAssignedToHearing()
                .withCourtRoomId(COURT_ROOM_ID)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);


        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);
        when(properties.findPath(JSON_PATH)).thenReturn(properties);

        courtRoomForHearingEventListener.courtRoomAssignedToHearing(envelope);

        verify(properties).put(eq("courtRoomId"), eq(COURT_ROOM_ID.toString()));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldChangeCourtRoomForHearing() {
        Envelope<CourtRoomChangedForHearing> envelope = (Envelope<CourtRoomChangedForHearing>) mock(Envelope.class);
        CourtRoomChangedForHearing hearingData = CourtRoomChangedForHearing.courtRoomChangedForHearing()
                .withCourtRoomId(COURT_ROOM_ID)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);
        when(properties.findPath(JSON_PATH)).thenReturn(properties);

        courtRoomForHearingEventListener.courtRoomChangedForHearing(envelope);

        verify(properties).put(eq("courtRoomId"), eq(COURT_ROOM_ID.toString()));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRemoveCourtRoomFromHearing() {
        Envelope<CourtRoomRemovedFromHearing> envelope = (Envelope<CourtRoomRemovedFromHearing>) mock(Envelope.class);
        CourtRoomRemovedFromHearing hearingData = CourtRoomRemovedFromHearing.courtRoomRemovedFromHearing()
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);
        when(properties.findPath(JSON_PATH)).thenReturn(properties);

        courtRoomForHearingEventListener.courtRoomRemovedFromHearing(envelope);

        verify(properties).remove("courtRoomId");
        verify(hearingRepository).save(hearing);
    }
}
