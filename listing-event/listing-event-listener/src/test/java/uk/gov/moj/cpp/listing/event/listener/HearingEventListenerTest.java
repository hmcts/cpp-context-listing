package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private HearingListed hearingListed;

    @Mock
    private HearingAllocatedForListing hearingAllocated;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @Mock
    private HearingUnallocatedForListing hearingUnallocated;

    @Mock
    private uk.gov.justice.listing.events.Hearing hearingEvent;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonNode jsonNode;

    @InjectMocks
    private HearingEventListener hearingEventListener;

    @Test
    public void shouldHandleHearingListedEvent() {
        final Envelope<HearingListed> envelope = (Envelope<HearingListed>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingListed);
        given(envelope.payload().getHearing()).willReturn(hearingEvent);
        given(hearingEvent.getId()).willReturn(HEARING_ID);
        given(mapper.valueToTree(hearingEvent)).willReturn(jsonNode);

        hearingEventListener.hearingListed(envelope);

        final Hearing hearing = new Hearing(HEARING_ID, jsonNode);
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldAllocateHearingForListing() {
        final Envelope<HearingAllocatedForListing> envelope = (Envelope<HearingAllocatedForListing>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingAllocated);
        given(hearingAllocated.getHearingId()).willReturn(HEARING_ID);
        given(jsonObject.toString()).willReturn("\"hello\": \"world\"");

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingAllocated(envelope);

        verify(properties).put(eq("allocated"), eq(true));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldUnallocateHearingForListing() {
        final Envelope<HearingUnallocatedForListing> envelope = (Envelope<HearingUnallocatedForListing>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingUnallocated);
        given(hearingUnallocated.getHearingId()).willReturn(HEARING_ID);
        given(jsonObject.toString()).willReturn("\"hello\": \"world\"");

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingUnallocated(envelope);

        verify(properties).put(eq("allocated"), eq(false));
        verify(hearingRepository).save(hearing);
    }
}
