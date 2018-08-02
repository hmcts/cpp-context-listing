package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.HearingDate.hearingDate;
import static uk.gov.justice.listing.events.HearingUnallocatedForListing.hearingUnallocatedForListing;

import uk.gov.justice.listing.events.CaseSentForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.converter.HearingListedConverter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
    private static final String URN = RandomGenerator.STRING.next();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final boolean UNALLOCATED = false;
    private static final String START_DATE = LocalDate.now().toString();

    @InjectMocks
    private HearingEventListener hearingEventListener;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ListingCaseRepository listingCaseRepository;

    @Mock
    private HearingListedConverter hearingListedConverter;

    @Mock
    private Envelope<CaseSentForListing> caseSentForListingEnvelope;

    @Mock
    private HearingListed hearingListed;

    @Mock
    private Hearing hearing;


    @Test
    public void shouldHandleCaseSentForListingEventAndPersistListingCase() throws Exception {
        givenAPayloadWithListingCaseData();
        given(listingCaseRepository.findBy(any())).willReturn(null);

        hearingEventListener.caseSentForListing(caseSentForListingEnvelope);

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

        hearingEventListener.caseSentForListing(caseSentForListingEnvelope);

        verify(listingCaseRepository, never()).save(any());
    }

    private void givenAPayloadWithListingCaseData() {
        CaseSentForListing caseSentForListing = CaseSentForListing.caseSentForListing()
                .withCaseId(CASE_ID)
                .withUrn(URN)
                .build();
        given(caseSentForListingEnvelope.payload()).willReturn(caseSentForListing);
    }

    @Test
    public void shouldHandleHearingListedEvent() throws Exception {
        Envelope<HearingListed> envelope = (Envelope<HearingListed>) mock(Envelope.class);
        given(envelope.payload()).willReturn(hearingListed);

        given(hearingListedConverter.convert(hearingListed)).willReturn(hearing);

        hearingEventListener.hearingListed(envelope);

        verify(hearingListedConverter).convert(hearingListed);
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldAllocateHearingForListing() throws Exception {
        Envelope<HearingAllocatedForListing> envelope = (Envelope<HearingAllocatedForListing>) mock(Envelope.class);
        HearingAllocatedForListing hearingData = HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingDate(hearingDate()
                        .withStartDate(START_DATE).build())
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearingData);

        hearingEventListener.hearingAllocatedForHearing(envelope);
        verify(hearingRepository).updateAllocated(ALLOCATED, HEARING_ID);
    }

    @Test
    public void shouldUnallocateHearingForListing() throws Exception {
        Envelope<HearingUnallocatedForListing> envelope = (Envelope<HearingUnallocatedForListing>) mock(Envelope.class);
        HearingUnallocatedForListing hearingData = hearingUnallocatedForListing()
                .withHearingId(HEARING_ID)
                .withHearingDate(hearingDate()
                        .withStartDate(START_DATE)
                        .build())
                .build();
        given(envelope.payload()).willReturn(hearingData);

        hearingEventListener.hearingUnallocatedForHearing(envelope);
        verify(hearingRepository).updateAllocated(UNALLOCATED, HEARING_ID);
    }
}
