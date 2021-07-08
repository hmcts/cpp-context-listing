package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.listing.events.HearingLanguageChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HearingLanguageEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final String HEARING_LANGUAGE = HearingLanguage.WELSH.toString();
    private static final String HEARING_LANGUAGE_FIELD = "hearingLanguage";

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HearingLanguageEventListener hearingLanguageEventListener;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @Test
    public void shouldChangeHearingLanguage() throws Exception {
        Envelope<HearingLanguageChangedForHearing> envelope = (Envelope<HearingLanguageChangedForHearing>) mock(Envelope.class);
        HearingLanguageChangedForHearing hearingData = HearingLanguageChangedForHearing.hearingLanguageChangedForHearing()
                .withHearingLanguage(HearingLanguage.valueOf(HEARING_LANGUAGE))
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);


        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingLanguageEventListener.hearingLanguageChanged(envelope);

        verify(properties).put(eq(HEARING_LANGUAGE_FIELD), eq(HEARING_LANGUAGE));
        verify(hearingRepository).save(hearing);
    }
}