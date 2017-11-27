package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.JudgeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.JudgeChangedForHearing;
import uk.gov.moj.cpp.listing.event.JudgeRemovedFromHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.json.JsonObject;

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

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Test
    public void shouldAssignJudgeToHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        JudgeAssignedToHearing hearingData = new JudgeAssignedToHearing(JUDGE_ID.toString(), HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, JudgeAssignedToHearing.class))
                .willReturn(hearingData);

        judgeForHearingEventListener.judgeAssignedToHearing(envelope);
        verify(hearingRepository).updateJudgeId(JUDGE_ID, HEARING_ID);
    }

    @Test
    public void shouldChangeJudgeForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        JudgeChangedForHearing hearingData = new JudgeChangedForHearing(JUDGE_ID.toString(), HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, JudgeChangedForHearing.class))
                .willReturn(hearingData);

        judgeForHearingEventListener.judgeChangedForHearing(envelope);
        verify(hearingRepository).updateJudgeId(JUDGE_ID, HEARING_ID);
    }

    @Test
    public void shouldRemoveJudgeFromHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        JudgeRemovedFromHearing hearingData = new JudgeRemovedFromHearing(HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, JudgeRemovedFromHearing.class))
                .willReturn(hearingData);

        judgeForHearingEventListener.judgeRemovedFromHearing(envelope);
        verify(hearingRepository).updateJudgeId(null, HEARING_ID);
    }
}