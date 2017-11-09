package uk.gov.moj.cpp.listing.event.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.listing.event.HearingUpdatedForListing;
import uk.gov.moj.cpp.listing.event.converter.HearingConverter;
import uk.gov.moj.cpp.listing.event.converter.HearingUpdatedConverter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingEventListenerTest {

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HearingEventListener hearingEventListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private HearingConverter hearingConverter;

    @Mock
    private HearingUpdatedConverter hearingUpdatedConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseSentForListing caseSentForListing;

    @Mock
    private HearingUpdatedForListing hearingUpdatedForListing;

    @Mock
    private Hearing hearing;

    @Mock
    private JsonObject payload;

    @Test
    public void shouldHandleCaseSentForListingEvent() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectToObjectConverter.convert(payload, CaseSentForListing.class))
                .willReturn(caseSentForListing);
        Set<Hearing> hearings = new HashSet<>(Arrays.asList(hearing));
        given(hearingConverter.convert(caseSentForListing)).willReturn(hearings);

        hearingEventListener.caseSentForListing(envelope);

        verify(envelope).payloadAsJsonObject();
        verify(jsonObjectToObjectConverter).convert(payload, CaseSentForListing.class);
        verify(hearingConverter).convert(caseSentForListing);
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleHearingUpdatedForListingEvent() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectToObjectConverter.convert(payload, HearingUpdatedForListing.class))
                .willReturn(hearingUpdatedForListing);
        given(hearingUpdatedConverter.convert(hearingUpdatedForListing)).willReturn(hearing);

        hearingEventListener.hearingUpdatedForListing(envelope);

        verify(envelope).payloadAsJsonObject();
        verify(jsonObjectToObjectConverter).convert(payload, HearingUpdatedForListing.class);
        verify(hearingUpdatedConverter).convert(hearingUpdatedForListing);
        verify(hearingRepository).save(hearing);
    }
}