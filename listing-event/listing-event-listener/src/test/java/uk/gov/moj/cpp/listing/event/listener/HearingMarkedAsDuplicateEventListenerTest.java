package uk.gov.moj.cpp.listing.event.listener;


import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingMarkedAsDuplicateEventListenerTest {


    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Envelope<HearingMarkedAsDuplicate> envelope;

    @InjectMocks
    private HearingMarkedAsDuplicateEventListener hearingMarkedAsDuplicateEventListener;

    @Test
    public void shouldDeleteHearingWhenMarkedAsDuplicate() {

        final UUID hearingId = UUID.randomUUID();
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId).build();

        when(envelope.payload()).thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .build());
        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(hearing);

        hearingMarkedAsDuplicateEventListener.deleteHearing(envelope);

        verify(hearingRepository).remove(eq(hearing));
    }

    @Test
    public void shouldNotDeleteHearingWhenHearingNotExistsInViewStore() {

        final UUID hearingId = UUID.randomUUID();
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId).build();

        when(envelope.payload()).thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .build());
        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(null);

        hearingMarkedAsDuplicateEventListener.deleteHearing(envelope);

        verify(hearingRepository, never()).remove(eq(hearing));
    }
}
