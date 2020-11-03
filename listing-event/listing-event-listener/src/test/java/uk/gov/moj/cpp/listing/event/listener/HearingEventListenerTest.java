package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.listener.utils.HearingUtils.getStringFromResource;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseUpdateDefendantProceedingsUpdated;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingTrialVacated;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final String LISTED_CASES = "listedCases";
    private static final UUID VACATE_TRIAL_REASON = randomUUID();

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
        final UUID CASE_ID = UUID.fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID = UUID.fromString("ddc332a5-c141-40e2-b50f-94ab7552b763");
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
        final UUID CASE_ID = UUID.fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID = UUID.fromString("ddc332a5-c141-40e2-b50f-94ab7552b763");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<DefendantCourtProceedingsUpdated> envelope = (Envelope<DefendantCourtProceedingsUpdated>) mock(Envelope.class);

        final DefendantCourtProceedingsUpdated defendantCourtProceedingsUpdated = DefendantCourtProceedingsUpdated
                .defendantCourtProceedingsUpdated()
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

        hearingEventListener.updateDefendantCourtProceedings(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldNotUpdateDefendantCourtProceedingsWhenDefendantIsNotPresent() throws Exception {
        final UUID CASE_ID = UUID.fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final UUID DEFENDANT_ID_NOT_MATCHED_IN_THE_EVENT = UUID.fromString("11c332a5-c141-40e2-b50f-94ab7552b722");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<DefendantCourtProceedingsUpdated> envelope = (Envelope<DefendantCourtProceedingsUpdated>) mock(Envelope.class);

        final DefendantCourtProceedingsUpdated defendantCourtProceedingsUpdated = DefendantCourtProceedingsUpdated
                .defendantCourtProceedingsUpdated()
                .withHearingId(HEARING_ID)
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
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
        final UUID CASE_ID = UUID.fromString("4ec3cbb8-2fb7-447c-9949-ad71436911f1");
        final String testCases1 = getStringFromResource("defendant-proceedings-concluded.json");
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode testCasesProperties = objectMapper.readTree(testCases1);
        final Envelope<CaseIdentifierUpdated> envelope = (Envelope<CaseIdentifierUpdated>) mock(Envelope.class);

        final CaseIdentifierUpdated updateCaseIdentifier = CaseIdentifierUpdated.caseIdentifierUpdated()
                .withProsecutionCaseId(CASE_ID)
                .withHearingId(HEARING_ID)
                .withProsecutionAuthorityCode("btx4uyUfIb")
                .withProsecutionAuthorityId(UUID.fromString("f0ddeecf-fda8-46f8-a293-c1813e58b479"))
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
}
