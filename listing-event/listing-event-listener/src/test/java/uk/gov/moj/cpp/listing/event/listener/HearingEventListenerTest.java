package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.HearingDate;
import uk.gov.moj.cpp.listing.event.HearingUnallocatedForListing;
import uk.gov.moj.cpp.listing.event.UnallocatedHearingListed;
import uk.gov.moj.cpp.listing.event.converter.UnallocatedHearingListedConverter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final boolean ALLOCATED = true;
    private static final String URN_PARAM = "urn";
    private static final String URN = RandomGenerator.STRING.next();
    private static final String CASE_ID_PARAM = "caseId";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final boolean UNALLOCATED = false;

    @InjectMocks
    private HearingEventListener hearingEventListener;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ListingCaseRepository listingCaseRepository;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private UnallocatedHearingListedConverter unallocatedHearingListedConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private UnallocatedHearingListed unallocatedHearingListed;

    @Mock
    private Hearing hearing;

    @Mock
    private JsonObject payload;


    @Test
    public void shouldHandleCaseSentForListingEventAndPersistListingCase() throws Exception {
        givenAPayloadWithListingCaseData();
        given(listingCaseRepository.findBy(any())).willReturn(null);

        hearingEventListener.caseSentForListing(envelope);

        verify(envelope).payloadAsJsonObject();
        ArgumentCaptor<ListingCase> listingCaseCaptor = ArgumentCaptor.forClass(ListingCase.class);
        verify(listingCaseRepository).save(listingCaseCaptor.capture());
        List<ListingCase> listingCases = listingCaseCaptor.getAllValues();
        assertThat(listingCases.get(0).getCaseId(), is(CASE_ID));
        assertThat(listingCases.get(0).getUrn(), is(URN));
    }

    @Test
    public void shouldHandleCaseSentForListingEventAndNotPersistListingCaseIfItAlreadyExists() throws Exception {
        givenAPayloadWithListingCaseData();
        given(listingCaseRepository.findBy(any())).willReturn(new ListingCase());

        hearingEventListener.caseSentForListing(envelope);

        verify(listingCaseRepository, never()).save(any());
    }

    private void givenAPayloadWithListingCaseData() {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString(URN_PARAM)).willReturn(URN);
        given(payload.getString(CASE_ID_PARAM)).willReturn(CASE_ID.toString());
    }

    @Test
    public void shouldHandleUnallocatedHearingListedEvent() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectToObjectConverter.convert(payload, UnallocatedHearingListed.class))
                .willReturn(unallocatedHearingListed);
   
        given(unallocatedHearingListedConverter.convert(unallocatedHearingListed)).willReturn(hearing);

        hearingEventListener.unallocatedHearingListed(envelope);

        verify(envelope).payloadAsJsonObject();
        verify(jsonObjectToObjectConverter).convert(payload, UnallocatedHearingListed.class);
        verify(unallocatedHearingListedConverter).convert(unallocatedHearingListed);
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldAllocateHearingForListing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        HearingAllocatedForListing hearingData = new HearingAllocatedForListing( HEARING_ID.toString(),
                null,null,null,null,new HearingDate(null,null,false));

        given(jsonObjectToObjectConverter.convert(payload, HearingAllocatedForListing.class))
                .willReturn(hearingData);

        hearingEventListener.hearingAllocatedForHearing(envelope);
        verify(hearingRepository).updateAllocated(ALLOCATED, HEARING_ID);
    }

    @Test
    public void shouldUnallocateHearingForListing() throws Exception {
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        HearingUnallocatedForListing hearingData = new HearingUnallocatedForListing( HEARING_ID.toString(),
                null,null,null,null, new HearingDate(null,null,false));

        given(jsonObjectToObjectConverter.convert(payload, HearingUnallocatedForListing.class))
                .willReturn(hearingData);

        hearingEventListener.hearingUnallocatedForHearing(envelope);
        verify(hearingRepository).updateAllocated(UNALLOCATED, HEARING_ID);
    }
}