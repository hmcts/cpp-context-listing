package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.CourtCentreChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtCentreEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String JSON_PATH = "";

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @InjectMocks
    private CourtCentreEventListener courtCentreEventListener;

    @Test
    public void shouldChangeCourtRoomForHearing() {
        Envelope<CourtCentreChangedForHearing> envelope = (Envelope<CourtCentreChangedForHearing>) mock(Envelope.class);
        CourtCentreChangedForHearing hearingData = CourtCentreChangedForHearing.courtCentreChangedForHearing()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);
        when(properties.findPath(JSON_PATH)).thenReturn(properties);

        courtCentreEventListener.courtCentreChangedForHearing(envelope);

        verify(properties).put(eq("courtCentreId"), eq(COURT_CENTRE_ID.toString()));
        verify(hearingRepository).save(hearing);
    }
}
