package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.getStringFromResource;


import java.util.List;
import java.util.stream.Collectors;
import org.mockito.Captor;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
import uk.gov.justice.listing.events.CaseUpdateDefendantProceedingsUpdated;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.DefendantOffenceIdsV2;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingTrialVacated;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingNumbers;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.listing.persistence.repository.ListingNumbersRepository;

@RunWith(MockitoJUnitRunner.class)
public class HearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final String LISTED_CASES = "listedCases";
    private static final UUID VACATE_TRIAL_REASON = randomUUID();
    public static final String ALLOCATED = "allocated";

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private ListingNumbersRepository listingNumbersRepository;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private HearingListed hearingListed;

    @Mock
    private HearingAllocatedForListing hearingAllocated;

    @Mock
    private HearingAllocatedForListingV2 hearingAllocatedV2;

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

    @Mock
    private TrialVacated trialVacated;

    @Mock
    private HearingTrialVacated hearingTrialVacated;

    @Mock
    private HearingRescheduled hearingRescheduled;


    @InjectMocks
    private HearingEventListener hearingEventListener;

    @Captor
    private ArgumentCaptor<UUID> listingNumbersCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingCaptor;

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    final UUID offenceId = randomUUID();

    @Test
    public void shouldAllocateHearingForListing() {
        final Envelope<HearingAllocatedForListing> envelope = getHearingAllocatedForListingEnvelope(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(singletonList(offenceId));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 1));

        hearingEventListener.hearingAllocated(envelope);
        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get(ALLOCATED).asBoolean(), is(true));
    }

    @Test
    public void shouldAllocateHearingForListingV2() {
        final Envelope<HearingAllocatedForListingV2> envelope = getHearingAllocatedForListingV2EnvelopeForCase(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(singletonList(offenceId));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 1));

        hearingEventListener.hearingAllocatedV2(envelope);
        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("allocated").asBoolean(), is(true));
    }

    @Test
    public void shouldSetListingNumbersWhenOffenceUnderCaseDoesNotExistInViewStore(){

        final Envelope<HearingAllocatedForListing> envelope = getHearingAllocatedWithCaseForListingEnvelope(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(singletonList(offenceId));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 1));

        hearingEventListener.hearingAllocated(envelope);

        verify(listingNumbersRepository).upset(listingNumbersCaptor.capture());

        assertThat(listingNumbersCaptor.getValue(), is(offenceId));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(1));
    }

    @Test
    public void shouldIncreaseListingNumbersWhenOffenceUnderCaseExistsInViewStore(){
        final Envelope<HearingAllocatedForListing> envelope = getHearingAllocatedWithCaseForListingEnvelope(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(singletonList(offenceId));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 3));

        hearingEventListener.hearingAllocated(envelope);

        verify(listingNumbersRepository).upset(listingNumbersCaptor.capture());

        assertThat(listingNumbersCaptor.getValue(), is(offenceId));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(3));
    }

    @Test
    public void shouldCreateListingNumbersCorrectlyForMixOffencesUnderCaseInViewStore(){
        final UUID offenceId2 = randomUUID();
        final Envelope<HearingAllocatedForListing> envelope = getHearingAllocatedWithCaseForListingEnvelope(asList(offenceId, offenceId2));

        final Hearing hearing = getHearingEntityWithCase(asList(offenceId, offenceId2));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 3));
        when(listingNumbersRepository.upset(eq(offenceId2))).thenReturn(new ListingNumbers(offenceId2, 1));

        hearingEventListener.hearingAllocated(envelope);

        verify(listingNumbersRepository, times(2)).upset(listingNumbersCaptor.capture());


        assertThat(listingNumbersCaptor.getAllValues().get(0), is(offenceId));
        assertThat(listingNumbersCaptor.getAllValues().get(1), is(offenceId2));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(3));
        assertThat(savedHearing.getProperties().get("listedCases").get(1).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(1));
    }

    @Test
    public void shouldCreateListingNumbersCorrectlyForOneOffencesUnderCaseInViewStore(){
        final UUID offenceId2 = randomUUID();
        final Envelope<HearingAllocatedForListing> envelope = getHearingAllocatedWithCaseForListingEnvelope(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(asList(offenceId, offenceId2));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 3));

        hearingEventListener.hearingAllocated(envelope);

        verify(listingNumbersRepository, times(1)).upset(listingNumbersCaptor.capture());


        assertThat(listingNumbersCaptor.getAllValues().get(0), is(offenceId));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(3));
        assertNull(savedHearing.getProperties().get("listedCases").get(1).get("defendants").get(0).get("offences").get(0).get("listingNumber"));
    }

    @Test
    public void shouldSetListingNumbersWhenOffenceUnderCaseDoesNotExistInViewStoreV2(){

        final Envelope<HearingAllocatedForListingV2> envelope = getHearingAllocatedForListingV2EnvelopeForCase(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(singletonList(offenceId));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 1));

        hearingEventListener.hearingAllocatedV2(envelope);

        verify(listingNumbersRepository).upset(listingNumbersCaptor.capture());

        assertThat(listingNumbersCaptor.getValue(), is(offenceId));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(1));
    }

    @Test
    public void shouldIncreaseListingNumbersWhenOffenceUnderCaseExistsInViewStoreV2(){
        final Envelope<HearingAllocatedForListingV2> envelope = getHearingAllocatedForListingV2EnvelopeForCase(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(singletonList(offenceId));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 3));

        hearingEventListener.hearingAllocatedV2(envelope);

        verify(listingNumbersRepository).upset(listingNumbersCaptor.capture());

        assertThat(listingNumbersCaptor.getValue(), is(offenceId));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(3));
   }

    @Test
    public void shouldCreateListingNumbersCorrectlyForMixOffencesUnderCaseInViewStoreV2(){
        final UUID offenceId2 = randomUUID();
        final Envelope<HearingAllocatedForListingV2> envelope = getHearingAllocatedForListingV2EnvelopeForCase(asList(offenceId, offenceId2));

        final Hearing hearing = getHearingEntityWithCase(asList(offenceId, offenceId2));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 3));
        when(listingNumbersRepository.upset(eq(offenceId2))).thenReturn(new ListingNumbers(offenceId2, 1));

        hearingEventListener.hearingAllocatedV2(envelope);

        verify(listingNumbersRepository, times(2)).upset(listingNumbersCaptor.capture());


        assertThat(listingNumbersCaptor.getAllValues().get(0), is(offenceId));
        assertThat(listingNumbersCaptor.getAllValues().get(1), is(offenceId2));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(3));
        assertThat(savedHearing.getProperties().get("listedCases").get(1).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(1));
    }

    @Test
    public void shouldCreateListingNumbersCorrectlyForOneOffencesUnderCaseInViewStoreV2(){
        final UUID offenceId2 = randomUUID();
        final Envelope<HearingAllocatedForListingV2> envelope = getHearingAllocatedForListingV2EnvelopeForCase(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithCase(asList(offenceId, offenceId2));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        when(listingNumbersRepository.upset(eq(offenceId))).thenReturn(new ListingNumbers(offenceId, 3));

        hearingEventListener.hearingAllocatedV2(envelope);

        verify(listingNumbersRepository, times(1)).upset(listingNumbersCaptor.capture());


        assertThat(listingNumbersCaptor.getAllValues().get(0), is(offenceId));

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("listingNumber").asInt(), is(3));
        assertNull(savedHearing.getProperties().get("listedCases").get(1).get("defendants").get(0).get("offences").get(0).get("listingNumber"));
    }

    @Test
    public void shouldNotSetListingNumbersWhenOffenceUnderApplicationDoesNotExistInViewStoreV2(){

        final Envelope<HearingAllocatedForListingV2> envelope = getHearingAllocatedForListingV2EnvelopeForApplication(singletonList(offenceId));

        final Hearing hearing = getHearingEntityWithApplication(singletonList(offenceId));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);

        hearingEventListener.hearingAllocatedV2(envelope);

        verify(listingNumbersRepository, times(0)).upset(any());

        verify(hearingRepository).save(hearingCaptor.capture());

        final Hearing savedHearing = hearingCaptor.getValue();
        assertNull(savedHearing.getProperties().get("courtApplications").get(0).get("offences").get(0).get("listingNumber"));
    }

    @Test
    public void shouldUnallocateHearingForListing() {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final Envelope<HearingUnallocatedForListing> envelope = (Envelope<HearingUnallocatedForListing>) mock(Envelope.class);
        final UUID courtCentreId = randomUUID();

        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(HEARING_ID)
                .withAllocated(true)
                .withCourtRoomId(of(randomUUID()))
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withCourtCentreId(of(courtCentreId))
                        .withCourtRoomId(of(randomUUID()))
                        .build()))
                .build();
        final JsonNode hearingProperties = objectMapper.valueToTree(domainHearing);
        final Hearing hearing = Hearing.builder().withId(HEARING_ID)
                .withProperties(hearingProperties)
                .build();

        given(envelope.payload()).willReturn(hearingUnallocated);
        given(hearingUnallocated.getHearingId()).willReturn(HEARING_ID);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        final ArgumentCaptor<Hearing> argumentCaptor = ArgumentCaptor.forClass(Hearing.class);

        hearingEventListener.hearingUnallocated(envelope);

        verify(hearingRepository).save(argumentCaptor.capture());

        final Hearing savedHearing = argumentCaptor.getValue();
        assertThat(savedHearing.getProperties().get("allocated").asBoolean(), is(false));
        assertThat(savedHearing.getProperties().get("courtRoomId"), nullValue());
        assertThat(savedHearing.getProperties().get("hearingDays").get(0).get("courtRoomId"), nullValue());
        assertThat(savedHearing.getProperties().get("hearingDays").get(0).get("courtCentreId").asText(), is(courtCentreId.toString()));

    }

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
    public void shouldHandleDefendantProceedingsConcluded() throws Exception {
        final UUID CASE_ID = fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID = fromString("ddc332a5-c141-40e2-b50f-94ab7552b763");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<CaseUpdateDefendantProceedingsUpdated> envelope = (Envelope<CaseUpdateDefendantProceedingsUpdated>) mock(Envelope.class);

        final CaseUpdateDefendantProceedingsUpdated caseUpdateDefendantProceedingsUpdated = CaseUpdateDefendantProceedingsUpdated
                .caseUpdateDefendantProceedingsUpdated()
                .withHearingId(HEARING_ID)
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant()
                                .withId(DEFENDANT_ID)
                                .withProceedingsConcluded(of(Boolean.TRUE))
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(caseUpdateDefendantProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.defendantProceedingsConcluded(envelope);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldUpdateDefendantCourtProceedingsWhenDefendantIsPresent() throws Exception {
        final UUID CASE_ID = fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID = fromString("ddc332a5-c141-40e2-b50f-94ab7552b763");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<DefendantCourtProceedingsUpdated> envelope = (Envelope<DefendantCourtProceedingsUpdated>) mock(Envelope.class);

        final DefendantCourtProceedingsUpdated defendantCourtProceedingsUpdated = DefendantCourtProceedingsUpdated
                .defendantCourtProceedingsUpdated()
                .withHearingId(HEARING_ID)
                .withProsecutionCase(uk.gov.justice.listing.events.ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant()
                                .withId(DEFENDANT_ID)
                                .withProceedingsConcluded(of(Boolean.TRUE))
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(defendantCourtProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.updateDefendantCourtProceedings(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldUpdateDefendantCourtProceedingsV2WhenDefendantIsPresent() throws Exception {
        final UUID CASE_ID = fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID = fromString("ddc332a5-c141-40e2-b50f-94ab7552b763");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<DefendantCourtProceedingsUpdatedV2> envelope = (Envelope<DefendantCourtProceedingsUpdatedV2>) mock(Envelope.class);

        final DefendantCourtProceedingsUpdatedV2 defendantCourtProceedingsUpdated = DefendantCourtProceedingsUpdatedV2
                .defendantCourtProceedingsUpdatedV2()
                .withHearingId(HEARING_ID)
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant()
                                .withId(DEFENDANT_ID)
                                .withProceedingsConcluded(of(Boolean.TRUE))
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(defendantCourtProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.updateDefendantCourtProceedingsV2(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldNotUpdateDefendantCourtProceedingsWhenDefendantIsNotPresent() throws Exception {
        final UUID CASE_ID = fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID_NOT_MATCHED_IN_THE_EVENT = fromString("11c332a5-c141-40e2-b50f-94ab7552b722");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<DefendantCourtProceedingsUpdated> envelope = (Envelope<DefendantCourtProceedingsUpdated>) mock(Envelope.class);

        final DefendantCourtProceedingsUpdated defendantCourtProceedingsUpdated = DefendantCourtProceedingsUpdated
                .defendantCourtProceedingsUpdated()
                .withHearingId(HEARING_ID)
                .withProsecutionCase(uk.gov.justice.listing.events.ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant()
                                .withId(DEFENDANT_ID_NOT_MATCHED_IN_THE_EVENT)
                                .withProceedingsConcluded(of(Boolean.TRUE))
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(defendantCourtProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.updateDefendantCourtProceedings(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldNotUpdateDefendantCourtProceedingsWhenDefendantListIsEmpty() throws Exception {
        final UUID CASE_ID = fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final Envelope<DefendantCourtProceedingsUpdated> envelope = (Envelope<DefendantCourtProceedingsUpdated>) mock(Envelope.class);

        final DefendantCourtProceedingsUpdated defendantCourtProceedingsUpdated = DefendantCourtProceedingsUpdated
                .defendantCourtProceedingsUpdated()
                .withHearingId(HEARING_ID)
                .withProsecutionCase(uk.gov.justice.listing.events.ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(asList())
                        .build())
                .build();
        given(envelope.payload()).willReturn(defendantCourtProceedingsUpdated);

        hearingEventListener.updateDefendantCourtProceedings(envelope);

        verify(hearingRepository, never()).save(hearing);
    }

    @Test
    public void shouldTrialVacated() {
        final Envelope<TrialVacated> envelope = (Envelope<TrialVacated>) mock(Envelope.class);

        given(envelope.payload()).willReturn(trialVacated);
        given(trialVacated.getHearingId()).willReturn(HEARING_ID);
        given(trialVacated.getVacatedTrialReasonId()).willReturn(VACATE_TRIAL_REASON);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.trialVacated(envelope);

        verify(properties).put(eq("isVacatedTrial"), eq(true));
        verify(properties).put(eq("vacatedTrialReasonId"), eq(VACATE_TRIAL_REASON.toString()));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHearingTrialVacated() {
        final Envelope<HearingTrialVacated> envelope = (Envelope<HearingTrialVacated>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingTrialVacated);
        given(hearingTrialVacated.getHearingId()).willReturn(HEARING_ID);
        given(hearingTrialVacated.getVacatedTrialReasonId()).willReturn(of(VACATE_TRIAL_REASON));

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingTrialVacated(envelope);

        verify(properties).put(eq("isVacatedTrial"), eq(true));
        verify(properties).put(eq("vacatedTrialReasonId"), eq(VACATE_TRIAL_REASON.toString()));
        verify(hearingRepository).save(hearing);
    }


    @Test
    public void shouldHearingRescheduled() {
        final Envelope<HearingRescheduled> envelope = (Envelope<HearingRescheduled>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingRescheduled);
        given(hearingRescheduled.getHearingId()).willReturn(HEARING_ID);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingRescheduled(envelope);

        verify(properties).put(eq("isVacatedTrial"), eq(false));
        verify(properties).put(eq("vacatedTrialReasonId"), eq(""));
        verify(hearingRepository).save(hearing);
    }


    @Test
    public void shouldHandleCaseIdentifierProceedingsConcluded() throws Exception {
        final UUID CASE_ID = fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded-with-prosecutor.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<CaseIdentifierUpdated> envelope = (Envelope<CaseIdentifierUpdated>) mock(Envelope.class);

        final CaseIdentifierUpdated updateCaseIdentifier = CaseIdentifierUpdated.caseIdentifierUpdated()
                .withProsecutionCaseId(CASE_ID)
                .withHearingId(HEARING_ID)
                .withProsecutionAuthorityCode("btx4uyUfIb")
                .withProsecutionAuthorityId(fromString("f0ddeecf-fda8-46f8-a293-c1813e58b479"))
                .build();

        given(envelope.payload()).willReturn(updateCaseIdentifier);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.updateCaseIdentifier(envelope);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
        ArrayNode actualValue = objectNodeCaptor.getValue();

        final String expectedCases1 = testCases1.replace("btx4uyUfIa", "btx4uyUfIb")
                .replace("f0ddeecf-fda8-46f8-a293-c1813e58b478", "f0ddeecf-fda8-46f8-a293-c1813e58b479");
        final JsonNode expectedCasesProperties = objectMapper.readTree(expectedCases1);
        assertThat(actualValue, CoreMatchers.equalTo(expectedCasesProperties));
    }

    private Envelope<HearingAllocatedForListingV2> getHearingAllocatedForListingV2EnvelopeForCase(final List<UUID> offenceIds) {
        final Envelope<HearingAllocatedForListingV2> envelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-allocated-for-listing-v2"),
                HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                        .withHearingId(HEARING_ID)
                        .withProsecutionCaseDefendantsOffenceIds(offenceIds.stream().map(offenceId -> ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withDefendants(singletonList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withOffenceIds(singletonList(OffenceIds.offenceIds().withId(offenceId).build())).build()))
                                .build()).collect(Collectors.toList()))
                        .build());
        return envelope;
    }

    private Envelope<HearingAllocatedForListingV2> getHearingAllocatedForListingV2EnvelopeForApplication(final List<UUID> offenceIds) {
        final Envelope<HearingAllocatedForListingV2> envelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-allocated-for-listing-v2"),
                HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                        .withHearingId(HEARING_ID)
                        .withApplicationOffenceIds(offenceIds)
                        .build());
        return envelope;
    }

    private Envelope<HearingAllocatedForListing> getHearingAllocatedWithCaseForListingEnvelope(final List<UUID> offenceIds) {
        final Envelope<HearingAllocatedForListing> envelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-allocated-for-listing"),
                HearingAllocatedForListing.hearingAllocatedForListing()
                        .withHearingId(HEARING_ID)
                        .withProsecutionCaseDefendantsOffenceIds(offenceIds.stream().map(offenceId -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                                .withDefendants(singletonList(DefendantOffenceIds.defendantOffenceIds()
                                        .withOffenceIds(singletonList(offenceId)).build()))
                                .build()).collect(Collectors.toList()))
                        .build());
        return envelope;
    }

    private Envelope<HearingAllocatedForListing> getHearingAllocatedForListingEnvelope(final List<UUID> offenceIds) {
        final Envelope<HearingAllocatedForListing> envelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-allocated-for-listing"),
                HearingAllocatedForListing.hearingAllocatedForListing()
                        .withHearingId(HEARING_ID).build());
        return envelope;
    }

    private Hearing getHearingEntityWithCase(final List<UUID> offenceIds) {
        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(HEARING_ID)
                .withListedCases(offenceIds.stream().map(offenceId -> ListedCase.listedCase()
                        .withId(randomUUID())
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build()).collect(Collectors.toList()))
                .build();

        final JsonNode hearingProperties = objectMapper.valueToTree(domainHearing);
        final Hearing hearing = Hearing.builder().withId(HEARING_ID)
                .withProperties(hearingProperties)
                .build();
        return hearing;
    }

    private Hearing getHearingEntityWithApplication(final List<UUID> offenceIds) {
        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(HEARING_ID)
                .withCourtApplication(singletonList(CourtApplication.courtApplication()
                        .withOffences(offenceIds.stream().map(offenceId -> Offence.offence().withId(offenceId).build()).collect(Collectors.toList())).build()))
                .build();

        final JsonNode hearingProperties = objectMapper.valueToTree(domainHearing);
        final Hearing hearing = Hearing.builder().withId(HEARING_ID)
                .withProperties(hearingProperties)
                .build();
        return hearing;
    }
}
