package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;


import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.NewDefendantDetailsUpdated;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantEventListenerTest {

    private static final String LISTED_CASES = "listedCases";
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final BailStatus EXPECTED_BAIL_STATUS = new BailStatus.Builder().withCode("B").withId(fromString("dd4073b6-22be-3875-9d63-5da286bb3ece")).withDescription("Conditional Bail").build();

    @Spy
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private Envelope<NewDefendantDetailsUpdated> defendantDetailsUpdatedEnvelope;

    @Mock
    private Envelope<NewDefendantAddedForCourtProceedings> defendantAddedForCourtProceedingsEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    Hearing hearing;


    @Mock
    ObjectNode properties;

    @InjectMocks
    private DefendantEventListener defendantEventListener;

    @Test
    public void shouldHandleDefendantDetailsUpdatedAndPersistSimpleDefendant() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ListedCase> testCases = createListedCases(CASE_ID, DEFENDANT_ID);
        String testCasesString = mapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        Envelope<NewDefendantDetailsUpdated> envelope = (Envelope<NewDefendantDetailsUpdated>) mock(Envelope.class);

        NewDefendantDetailsUpdated hearingData = NewDefendantDetailsUpdated.newDefendantDetailsUpdated()
                .withCaseId(CASE_ID)
                .withHearingId(HEARING_ID)
                .withDefendant(NewBaseDefendant.newBaseDefendant()
                        .withBailStatus(of(EXPECTED_BAIL_STATUS))
                        .withId(DEFENDANT_ID)
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(defendantDetailsUpdatedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantDetailsUpdated(defendantDetailsUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        JsonNode actualBailStatus = objectNodeCaptor.getValue().get(0).get("defendants").get(0).get("bailStatus");
        String actualBailStatusId = actualBailStatus.get("id").toString();
        String actualBailStatusCode = actualBailStatus.get("code").toString();
        String actualBailStatusDescription = actualBailStatus.get("description").toString();
        assertThat(actualBailStatusId, equalTo("\"dd4073b6-22be-3875-9d63-5da286bb3ece\""));
        assertThat(actualBailStatusCode, equalTo("\"B\""));
        assertThat(actualBailStatusDescription, equalTo("\"Conditional Bail\""));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldNotUpdateHearingDefendantWhenCaseIsNotAssociatedToTheHearing() throws Exception {
        final UUID defendantId = randomUUID();
        final UUID nonAssociatedDefendantId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID nonAssociateCaseId = randomUUID();
        final List<ListedCase> testCases = createListedCases(caseId, defendantId);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = mapper.readTree(testCasesString);
        final Envelope<NewDefendantDetailsUpdated> envelope = (Envelope<NewDefendantDetailsUpdated>) mock(Envelope.class);

        final NewDefendantDetailsUpdated hearingData = NewDefendantDetailsUpdated.newDefendantDetailsUpdated()
                .withCaseId(nonAssociateCaseId)
                .withHearingId(HEARING_ID)
                .withDefendant(NewBaseDefendant.newBaseDefendant()
                        .withBailStatus(of(EXPECTED_BAIL_STATUS))
                        .withId(nonAssociatedDefendantId)
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(defendantDetailsUpdatedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantDetailsUpdated(defendantDetailsUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());

        final JsonNode caseList = objectNodeCaptor.getValue();
        assertThat(caseList.size(), equalTo(1));
        assertThat(caseList.get(0).get("id").asText(), equalTo(caseId.toString()));
        final JsonNode defendants = caseList.get(0).get("defendants");
        assertThat(defendants.size(), equalTo(1));
        assertThat(defendants.get(0).get("id").asText(), equalTo(defendantId.toString()));

        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldNotUpdateHearingDefendantWhenDefendantIsNotAssociatedToTheHearing() throws Exception {
        final UUID defendantId = randomUUID();
        final UUID nonAssociatedDefendantId = randomUUID();
        final List<ListedCase> testCases = createListedCases(CASE_ID, defendantId);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = mapper.readTree(testCasesString);
        final Envelope<NewDefendantDetailsUpdated> envelope = (Envelope<NewDefendantDetailsUpdated>) mock(Envelope.class);

        final NewDefendantDetailsUpdated hearingData = NewDefendantDetailsUpdated.newDefendantDetailsUpdated()
                .withCaseId(CASE_ID)
                .withHearingId(HEARING_ID)
                .withDefendant(NewBaseDefendant.newBaseDefendant()
                        .withBailStatus(of(EXPECTED_BAIL_STATUS))
                        .withId(nonAssociatedDefendantId)
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(defendantDetailsUpdatedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor = ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantDetailsUpdated(defendantDetailsUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());

        final JsonNode defendants = objectNodeCaptor.getValue().get(0).get("defendants");
        assertThat(defendants.size(), equalTo(1));
        assertThat(defendants.get(0).get("id").asText(), equalTo(defendantId.toString()));

        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleDefendantAddedAndPersistSimpleDefendant() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ListedCase> testCases = createListedCases(CASE_ID, DEFENDANT_ID);
        String testCasesString = mapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        Envelope<NewDefendantAddedForCourtProceedings> envelope = (Envelope<NewDefendantAddedForCourtProceedings>) mock(Envelope.class);

        NewDefendantAddedForCourtProceedings hearingData = NewDefendantAddedForCourtProceedings.newDefendantAddedForCourtProceedings()
                .withCaseId(CASE_ID)
                .withHearingId(HEARING_ID)
                .withDefendant(Defendant.defendant()
                        .withBailStatus(of(EXPECTED_BAIL_STATUS))
                        .withId(DEFENDANT_ID)
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(defendantAddedForCourtProceedingsEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantDetailsAddedForCourtProceedings(defendantAddedForCourtProceedingsEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleDefendantLegalAidStatusUpdated() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases(CASE_ID, DEFENDANT_ID);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        final Envelope<DefendantLegalaidStatusUpdatedForHearing> envelope = (Envelope<DefendantLegalaidStatusUpdatedForHearing>) mock(Envelope.class);
        final DefendantLegalaidStatusUpdatedForHearing defendantLegalaidStatusData = DefendantLegalaidStatusUpdatedForHearing.defendantLegalaidStatusUpdatedForHearing()
                .withDefendantId(DEFENDANT_ID)
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withLegalAidStatus("Granted")
                .build();
        given(envelope.payload()).willReturn(defendantLegalaidStatusData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantLegalStatusUpdatedForHearing(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);


    }

    @Test
    public void shouldNotUpdateHearingDefendantsLegalAidStatusWhenCaseIsNotAssociatedToTheHearing() throws Exception {
        final UUID defendantId = randomUUID();
        final UUID nonAssociatedDefendantId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID nonAssociateCaseId = randomUUID();

        final List<ListedCase> testCases = createListedCases(caseId, defendantId);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = mapper.readTree(testCasesString);
        final Envelope<DefendantLegalaidStatusUpdatedForHearing> envelope = (Envelope<DefendantLegalaidStatusUpdatedForHearing>) mock(Envelope.class);
        final DefendantLegalaidStatusUpdatedForHearing defendantLegalaidStatusData = DefendantLegalaidStatusUpdatedForHearing.defendantLegalaidStatusUpdatedForHearing()
                .withDefendantId(nonAssociatedDefendantId)
                .withHearingId(HEARING_ID)
                .withCaseId(nonAssociateCaseId)
                .withLegalAidStatus("Granted")
                .build();
        given(envelope.payload()).willReturn(defendantLegalaidStatusData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantLegalStatusUpdatedForHearing(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());

        final JsonNode caseList = objectNodeCaptor.getValue();
        assertThat(caseList.size(), equalTo(1));
        assertThat(caseList.get(0).get("id").asText(), equalTo(caseId.toString()));
        final JsonNode defendants = caseList.get(0).get("defendants");
        assertThat(defendants.size(), equalTo(1));
        assertThat(defendants.get(0).get("id").asText(), equalTo(defendantId.toString()));

        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldNotUpdateHearingDefendantsLegalAidStatusWhenDefendantIsNotAssociatedToTheHearing() throws Exception {
        final UUID defendantId = randomUUID();
        final UUID nonAssociatedDefendantId = randomUUID();

        final List<ListedCase> testCases = createListedCases(CASE_ID, defendantId);
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = mapper.readTree(testCasesString);
        final Envelope<DefendantLegalaidStatusUpdatedForHearing> envelope = (Envelope<DefendantLegalaidStatusUpdatedForHearing>) mock(Envelope.class);
        final DefendantLegalaidStatusUpdatedForHearing defendantLegalaidStatusData = DefendantLegalaidStatusUpdatedForHearing.defendantLegalaidStatusUpdatedForHearing()
                .withDefendantId(nonAssociatedDefendantId)
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withLegalAidStatus("Granted")
                .build();
        given(envelope.payload()).willReturn(defendantLegalaidStatusData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantLegalStatusUpdatedForHearing(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());

        final JsonNode defendants = objectNodeCaptor.getValue().get(0).get("defendants");
        assertThat(defendants.size(), equalTo(1));
        assertThat(defendants.get(0).get("id").asText(), equalTo(defendantId.toString()));

        verify(hearingRepository).save(hearing);
    }

    private List<ListedCase> createListedCases(final UUID caseId, final UUID defendantId) {
        return singletonList(ListedCase.listedCase()
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(STRING.next())
                        .withAuthorityId(randomUUID())
                        .withCaseReference(STRING.next())
                        .build())
                .withDefendants(singletonList(Defendant.defendant()
                        .withSpecificRequirements(Optional.empty())
                        .withId(defendantId)
                        .withBailStatus(of(new BailStatus.Builder().withCode("C").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).withDescription("Custody or remanded into custody").build()))
                        .withOffences(singletonList(Offence.offence()
                                .withId(randomUUID())
                                .withOffenceCode(STRING.next())
                                .withStartDate(LocalDates.to(LocalDate.now()))
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withLegislation(of(STRING.next()))
                                        .withTitle(STRING.next())
                                        .build())
                                .build()))
                        .build()))
                .withId(caseId)
                .build());
    }
}