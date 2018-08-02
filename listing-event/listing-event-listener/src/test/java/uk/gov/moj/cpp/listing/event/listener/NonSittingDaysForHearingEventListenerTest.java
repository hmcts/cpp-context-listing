package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.nonSittingDaysAssignedToHearing;
import static uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.nonSittingDaysChangedForHearing;

import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.converter.NonSittingDaysJsonConverter;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class NonSittingDaysForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final LocalDate NON_SITTING_DAY = LocalDate.now();


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private NonSittingDaysForHearingEventListener nonSittingDaysForHearingEventListener;

    @Mock
    private NonSittingDaysJsonConverter nonSittingDaysConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<String> captor;

    @Test
    public void shouldAssignNonSittingDaysToHearing() throws Exception {
        //given
        Envelope<NonSittingDaysAssignedToHearing> envelope = (Envelope< NonSittingDaysAssignedToHearing>) mock(Envelope.class);
        NonSittingDaysAssignedToHearing hearingData = nonSittingDaysAssignedToHearing()
                .withNonSittingDays(Arrays.asList(NON_SITTING_DAY))
                .withHearingId(HEARING_ID)
                .build();
        String nonSittingDaysJSon ="{nonSittingDays: [\""+NON_SITTING_DAY.toString()+"\"]";
        given(envelope.payload()).willReturn(hearingData);
        given(nonSittingDaysConverter.convertNonSittingDaysTo(hearingData.getNonSittingDays())).willReturn(nonSittingDaysJSon);

        //when
        nonSittingDaysForHearingEventListener.nonSittingDaysAssignedForHearing(envelope);

        //then
        verify(hearingRepository).updateNonSittingDays(captor.capture(), any(UUID.class));
        String actualJson = captor.getValue();
        assertThat(actualJson, equalTo(nonSittingDaysJSon));
    }

    @Test
    public void shouldChangeNonSittingDaysForHearing() throws Exception {
        //given
        Envelope<NonSittingDaysChangedForHearing> envelope = (Envelope< NonSittingDaysChangedForHearing>) mock(Envelope.class);
        NonSittingDaysChangedForHearing hearingData = nonSittingDaysChangedForHearing()
                .withNonSittingDays(Arrays.asList(NON_SITTING_DAY))
                .withHearingId(HEARING_ID)
                .build();
        String nonSittingDaysJSon ="{nonSittingDays: [\""+NON_SITTING_DAY.toString()+"\"]";
        given(envelope.payload()).willReturn(hearingData);
        given(nonSittingDaysConverter.convertNonSittingDaysTo(hearingData.getNonSittingDays())).willReturn(nonSittingDaysJSon);

         //when
        nonSittingDaysForHearingEventListener.nonSittingDaysChangedForHearing(envelope);

        //then
        verify(hearingRepository).updateNonSittingDays(captor.capture(), any(UUID.class));
        String actualJson = captor.getValue();
        assertThat(actualJson, equalTo(nonSittingDaysJSon));
    }
}