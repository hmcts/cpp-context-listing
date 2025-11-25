package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.getStringFromResource;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
import uk.gov.justice.listing.events.CaseUpdateDefendantProceedingsUpdated;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingResultStatusUpdated;
import uk.gov.justice.listing.events.HearingTrialVacated;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID  = randomUUID();
    private static final String LISTED_CASES = "listedCases";
    private static final UUID VACATE_TRIAL_REASON = randomUUID();

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private HearingListed hearingListed;

    @Mock
    private HearingAllocatedForListing hearingAllocated;

    @Mock
    private HearingAllocatedForListingV2 hearingAllocatedV2;

    @Mock
    private HearingResultStatusUpdated hearingResultStatusUpdated;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @Mock
    private HearingUnallocatedForListing hearingUnallocated;

    private uk.gov.justice.listing.events.Hearing hearingEvent = uk.gov.justice.listing.events.Hearing.hearing().withId(HEARING_ID).build();

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

    @Test
    public void shouldAllocateHearingForListing() {
        final Envelope<HearingAllocatedForListing> envelope = (Envelope<HearingAllocatedForListing>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingAllocated);
        given(hearingAllocated.getHearingId()).willReturn(HEARING_ID);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingAllocated(envelope);

        verify(properties).put(eq("allocated"), eq(true));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldAllocateHearingForListingV2() {
        final Envelope<HearingAllocatedForListingV2> envelope = (Envelope<HearingAllocatedForListingV2>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingAllocatedV2);
        given(hearingAllocatedV2.getHearingId()).willReturn(HEARING_ID);
        given(hearingAllocatedV2.getCourtRoomId()).willReturn(COURT_ROOM_ID);
        given(hearingAllocatedV2.getSendNotificationToParties()).willReturn(Boolean.TRUE);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingAllocatedV2(envelope);

        verify(properties).put(eq("allocated"), eq(true));
        verify(properties).put(eq("courtRoomId"), eq(COURT_ROOM_ID.toString()));
        verify(properties).put(eq("sendNotificationToParties"), eq(true));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldAllocateHearingForListingV2WhenSendNotificationToPartiesIsNull() {
        final Envelope<HearingAllocatedForListingV2> envelope = (Envelope<HearingAllocatedForListingV2>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingAllocatedV2);
        given(hearingAllocatedV2.getHearingId()).willReturn(HEARING_ID);
        given(hearingAllocatedV2.getCourtRoomId()).willReturn(COURT_ROOM_ID);
        given(hearingAllocatedV2.getSendNotificationToParties()).willReturn(null);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingAllocatedV2(envelope);

        verify(properties).put(eq("allocated"), eq(true));
        verify(properties).put(eq("courtRoomId"), eq(COURT_ROOM_ID.toString()));

        verify(hearingRepository).save(hearing);
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
        given(mapper.valueToTree(hearingEvent)).willReturn(jsonNode);

        hearingEventListener.hearingListed(envelope);

        final Hearing hearing = new Hearing(HEARING_ID, jsonNode);
        verify(hearingSearchSyncService).syncEntity(hearing);
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
                                .withProceedingsConcluded(true)
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
        verify(properties).replace(any(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldNotHandleDefendantProceedingsConcludedIfThereIsNoHearing() throws Exception {
        final UUID CASE_ID = fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID = fromString("ddc332a5-c141-40e2-b50f-94ab7552b763");
        final Envelope<CaseUpdateDefendantProceedingsUpdated> envelope = (Envelope<CaseUpdateDefendantProceedingsUpdated>) mock(Envelope.class);

        final CaseUpdateDefendantProceedingsUpdated caseUpdateDefendantProceedingsUpdated = CaseUpdateDefendantProceedingsUpdated
                .caseUpdateDefendantProceedingsUpdated()
                .withHearingId(HEARING_ID)
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant()
                                .withId(DEFENDANT_ID)
                                .withProceedingsConcluded(true)
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(caseUpdateDefendantProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(null);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.defendantProceedingsConcluded(envelope);
        verify(properties, never()).replace(any(), objectNodeCaptor.capture());
        verify(hearingRepository, never()).save(hearing);
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
                                .withProceedingsConcluded(true)
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(defendantCourtProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.updateDefendantCourtProceedings(envelope);

        verify(properties).replace(any(), objectNodeCaptor.capture());
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
                                .withProceedingsConcluded(true)
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(defendantCourtProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.updateDefendantCourtProceedingsV2(envelope);

        verify(properties).replace(any(), objectNodeCaptor.capture());
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
                                .withProceedingsConcluded(true)
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(defendantCourtProceedingsUpdated);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        hearingEventListener.updateDefendantCourtProceedings(envelope);

        verify(properties).replace(any(), objectNodeCaptor.capture());
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
    public void shouldHearingTrialVacatedIfThereIsNoHearing() {
        final Envelope<HearingTrialVacated> envelope = (Envelope<HearingTrialVacated>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingTrialVacated);
        given(hearingTrialVacated.getHearingId()).willReturn(HEARING_ID);

        when(hearingRepository.findBy(HEARING_ID)).thenReturn(null);


        hearingEventListener.hearingTrialVacated(envelope);

        verify(hearingRepository, never()).save(hearing);
        verify(hearingSearchSyncService, never()).sync(any());
    }

    @Test
    public void shouldHearingTrialVacated() {
        final Envelope<HearingTrialVacated> envelope = (Envelope<HearingTrialVacated>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingTrialVacated);
        given(hearingTrialVacated.getHearingId()).willReturn(HEARING_ID);
        given(hearingTrialVacated.getVacatedTrialReasonId()).willReturn(VACATE_TRIAL_REASON);

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
        verify(properties).replace(any(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
        ArrayNode actualValue = objectNodeCaptor.getValue();

        final String expectedCases1 = testCases1.replace("btx4uyUfIa", "btx4uyUfIb")
                .replace("f0ddeecf-fda8-46f8-a293-c1813e58b478", "f0ddeecf-fda8-46f8-a293-c1813e58b479");
        final JsonNode expectedCasesProperties = objectMapper.readTree(expectedCases1);
        assertThat(actualValue, CoreMatchers.equalTo(expectedCasesProperties));
    }

    @Test
    public void shouldHandleCaseIdentifierProceedingsConcludedWhenCaseIsNotThere() throws Exception {
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded-with-prosecutor.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<CaseIdentifierUpdated> envelope = (Envelope<CaseIdentifierUpdated>) mock(Envelope.class);

        final CaseIdentifierUpdated updateCaseIdentifier = CaseIdentifierUpdated.caseIdentifierUpdated()
                .withProsecutionCaseId(randomUUID())
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
        verify(properties).replace(any(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
        ArrayNode actualValue = objectNodeCaptor.getValue();

        final String expectedCases1 = testCases1.replace("btx4uyUfIa", "btx4uyUfIb")
                .replace("f0ddeecf-fda8-46f8-a293-c1813e58b478", "f0ddeecf-fda8-46f8-a293-c1813e58b479");
        final JsonNode expectedCasesProperties = objectMapper.readTree(expectedCases1);
        assertThat(actualValue, CoreMatchers.equalTo(expectedCasesProperties));
    }


    @Test
    public void shouldHandleResultStatusUpdatedConcluded()  {
        final Envelope<HearingResultStatusUpdated> envelope = (Envelope<HearingResultStatusUpdated>) mock(Envelope.class);

        given(envelope.payload()).willReturn(hearingResultStatusUpdated);
        given(hearingResultStatusUpdated.getHearingId()).willReturn(HEARING_ID);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        when(hearing.getProperties()).thenReturn(properties);

        hearingEventListener.hearingResultStatusUpdated(envelope);

        verify(properties).put(eq("resulted"), eq(true));
        verify(hearingRepository).save(hearing);
    }


}
