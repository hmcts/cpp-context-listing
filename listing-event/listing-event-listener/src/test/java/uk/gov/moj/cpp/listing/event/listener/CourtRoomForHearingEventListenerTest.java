package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CourtRoomAssignedToHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomChangedForHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomRemovedFromHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.json.JsonObject;

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

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Test
    public void shouldAssignCourtRoomToHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        CourtRoomAssignedToHearing hearingData = new CourtRoomAssignedToHearing(COURT_ROOM_ID.toString(), HEARING_ID.toString());
        
        given(jsonObjectToObjectConverter.convert(payload, CourtRoomAssignedToHearing.class))
                .willReturn(hearingData);

        courtRoomForHearingEventListener.courtRoomAssignedToHearing(envelope);
        verify(hearingRepository).updateCourtRoomId(COURT_ROOM_ID, HEARING_ID);


    }

    @Test
    public void shouldChangeCourtRoomForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        CourtRoomChangedForHearing hearingData = new CourtRoomChangedForHearing(COURT_ROOM_ID.toString(), HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, CourtRoomChangedForHearing.class))
                .willReturn(hearingData);

        courtRoomForHearingEventListener.courtRoomChangedForHearing(envelope);
        verify(hearingRepository).updateCourtRoomId(COURT_ROOM_ID, HEARING_ID);
    }

    @Test
    public void shouldRemoveCourtRoomFromHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        CourtRoomRemovedFromHearing hearingData = new CourtRoomRemovedFromHearing(HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, CourtRoomRemovedFromHearing.class))
                .willReturn(hearingData);

        courtRoomForHearingEventListener.courtRoomRemovedFromHearing(envelope);
        verify(hearingRepository).updateCourtRoomId(null, HEARING_ID);
    }
}