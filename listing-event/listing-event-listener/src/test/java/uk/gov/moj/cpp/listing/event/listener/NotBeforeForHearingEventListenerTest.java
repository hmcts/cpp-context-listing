package uk.gov.moj.cpp.listing.event.listener;


import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.NotBeforeSelectedForHearing;
import uk.gov.moj.cpp.listing.event.NotBeforeUnselectedForHearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotBeforeForHearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final Boolean NOT_BEFORE_SELECTED = true;
    private static final Boolean NOT_BEFORE_UNSELECTED = false;

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private NotBeforeForHearingEventListener notBeforeForHearingEventListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Test
    public void shouldSelectNotBeforeForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        NotBeforeSelectedForHearing hearingData = new NotBeforeSelectedForHearing(HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, NotBeforeSelectedForHearing.class))
                .willReturn(hearingData);

        notBeforeForHearingEventListener.notBeforeSelectedForHearing(envelope);
        verify(hearingRepository).updateNotBefore(NOT_BEFORE_SELECTED, HEARING_ID);
    }

    @Test
    public void shouldUnselectNotBeforeForHearing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        NotBeforeUnselectedForHearing hearingData = new NotBeforeUnselectedForHearing(HEARING_ID.toString());

        given(jsonObjectToObjectConverter.convert(payload, NotBeforeUnselectedForHearing.class))
                .willReturn(hearingData);

        notBeforeForHearingEventListener.notBeforeUnselectedForHearing(envelope);
        verify(hearingRepository).updateNotBefore(NOT_BEFORE_UNSELECTED, HEARING_ID);
    }
}