package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.JudgeAssignedToHearing.judgeAssignedToHearing;
import static uk.gov.justice.listing.events.JudgeChangedForHearing.judgeChangedForHearing;
import static uk.gov.justice.listing.events.JudgeRemovedFromHearing.judgeRemovedFromHearing;

import uk.gov.justice.listing.events.JudgeAssignedToHearing;
import uk.gov.justice.listing.events.JudgeChangedForHearing;
import uk.gov.justice.listing.events.JudgeRemovedFromHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JudgeForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID JUDGE_ID = randomUUID();


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private JudgeForHearingEventListener judgeForHearingEventListener;

    @Test
    public void shouldAssignJudgeToHearing() throws Exception {
        Envelope<JudgeAssignedToHearing> envelope = (Envelope<JudgeAssignedToHearing>) mock(Envelope.class);
        JudgeAssignedToHearing hearingData = judgeAssignedToHearing()
                .withJudgeId(JUDGE_ID)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);


        judgeForHearingEventListener.judgeAssignedToHearing(envelope);
        verify(hearingRepository).updateJudgeId(JUDGE_ID, HEARING_ID);
    }

    @Test
    public void shouldChangeJudgeForHearing() throws Exception {
        Envelope<JudgeChangedForHearing> envelope = (Envelope<JudgeChangedForHearing>) mock(Envelope.class);
        JudgeChangedForHearing hearingData = judgeChangedForHearing()
                .withJudgeId(JUDGE_ID)
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        judgeForHearingEventListener.judgeChangedForHearing(envelope);
        verify(hearingRepository).updateJudgeId(JUDGE_ID, HEARING_ID);
    }

    @Test
    public void shouldRemoveJudgeFromHearing() throws Exception {
        Envelope<JudgeRemovedFromHearing> envelope = (Envelope<JudgeRemovedFromHearing>) mock(Envelope.class);
        JudgeRemovedFromHearing hearingData = judgeRemovedFromHearing()
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        judgeForHearingEventListener.judgeRemovedFromHearing(envelope);
        verify(hearingRepository).updateJudgeId(null, HEARING_ID);
    }
}
