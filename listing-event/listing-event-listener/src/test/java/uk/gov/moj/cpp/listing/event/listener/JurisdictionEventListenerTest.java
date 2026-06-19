package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.JurisdictionChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JurisdictionEventListenerTest {
    private static final UUID HEARING_ID = randomUUID();

    @InjectMocks
    private JurisdictionEventListener jurisdictionEventListener;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private Envelope<JurisdictionChangedForHearing> envelope;

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;


    @Test
    public void souldCourtRoomAssignedToHearing(){

        final JurisdictionChangedForHearing caseMarkersToBeUpdated = JurisdictionChangedForHearing.jurisdictionChangedForHearing()
                .withHearingId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(envelope.payload()).willReturn(caseMarkersToBeUpdated);

        jurisdictionEventListener.courtRoomAssignedToHearing(envelope);

        verify(properties).put(anyString(), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue(), is(JurisdictionType.CROWN.toString())) ;
        verify(hearingRepository).save(hearing);
        verify(hearingSearchSyncService).sync(uuidArgumentCaptor.capture());
        assertThat(uuidArgumentCaptor.getValue(), is(HEARING_ID)) ;


    }
}