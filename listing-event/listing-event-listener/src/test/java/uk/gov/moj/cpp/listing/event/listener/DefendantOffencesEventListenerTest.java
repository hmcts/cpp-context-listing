package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CivilOffence;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.stream.JsonParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantOffencesEventListenerTest {

    private static final String LISTED_CASES = "listedCases";
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final String EXPECTED_OFFENCE_CODE = "Offence Code";

    @Spy
    private ObjectMapper mapper =  new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private DefendantOffencesEventListener defendantOffencesEventListener;

    @Mock
    private Envelope<OffenceUpdated> offenceUpdatedEnvelope;

    @Mock
    private Envelope<OffenceAdded> offenceAddedEnvelope;

    @Mock
    Hearing hearing;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Envelope<OffenceDeleted> offenceDeletedEnvelope;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    ObjectNode properties;

    @Test
    public void shouldHandleOffenceUpdatedAndPersistSimpleOffenceIfThereIsNoHearing() throws Exception {



        final OffenceUpdated hearingData = OffenceUpdated.offenceUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(OFFENCE_ID)
                        .withEndDate(LocalDates.to(LocalDate.now()))
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .build())
                .build();

        given(offenceUpdatedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(null);

        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceUpdated(offenceUpdatedEnvelope);

        verify(properties, never()).replace(any(), objectNodeCaptur.capture());
        verify(hearingSearchSyncService, never()).sync(HEARING_ID);
       verify(hearingRepository, never()).save(hearing);
    }

    @Test
    public void shouldHandleOffenceUpdatedAndPersistSimpleOffence() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final OffenceUpdated hearingData = OffenceUpdated.offenceUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(OFFENCE_ID)
                        .withEndDate(LocalDates.to(LocalDate.now()))
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                        .build())
                .build();

        given(offenceUpdatedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceUpdated(offenceUpdatedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());
        final JsonNode newListedCase = objectNodeCaptur.getValue().get(0);
        final ListedCase testListedCase = testCases.get(0);
        String expectedOffenceCode = newListedCase.get("defendants").get(0).get("offences")
                .get(0).get("offenceCode").toString();

        int numberOffence = newListedCase.get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(1));
        assertThat(expectedOffenceCode, equalTo("\"" + EXPECTED_OFFENCE_CODE + "\""));

        assertThat(newListedCase.get("shadowListed").asBoolean(), equalTo(testListedCase.getShadowListed()));
        assertThat(newListedCase.get("defendants").get(0).get("offences").get(0).get("shadowListed").asBoolean(),
                equalTo(testListedCase.getDefendants().get(0).getOffences().get(0).getShadowListed()));
        final boolean isExparte = newListedCase.get("defendants").get(0).get("offences").get(0).get("civilOffence").get("isExParte").asBoolean();
        assertThat(isExparte, equalTo(testListedCase.getDefendants().get(0).getOffences().get(0).getCivilOffence().getIsExParte()));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceUpdatedAndPersistSimpleOffenceWhenCaseIsNotInDB() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final OffenceUpdated hearingData = OffenceUpdated.offenceUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(randomUUID())
                .withDefendantId(DEFENDANT_ID)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(OFFENCE_ID)
                        .withEndDate(LocalDates.to(LocalDate.now()))
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .build())
                .build();

        given(offenceUpdatedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceUpdated(offenceUpdatedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());
        final JsonNode newListedCase = objectNodeCaptur.getValue().get(0);
        final ListedCase testListedCase = testCases.get(0);
        String expectedOffenceCode = newListedCase.get("defendants").get(0).get("offences")
                .get(0).get("offenceCode").toString();

        int numberOffence = newListedCase.get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(1));
        assertThat(expectedOffenceCode, equalTo("\"" + testCases.get(0).getDefendants().get(0).getOffences().get(0).getOffenceCode() + "\""));

        assertThat(newListedCase.get("shadowListed").asBoolean(), equalTo(testListedCase.getShadowListed()));
        assertThat(newListedCase.get("defendants").get(0).get("offences").get(0).get("shadowListed").asBoolean(),
                equalTo(testListedCase.getDefendants().get(0).getOffences().get(0).getShadowListed()));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceUpdatedWhenDefendantIsNotInListedCasedDb() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final OffenceUpdated hearingData = OffenceUpdated.offenceUpdated()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID_2)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(randomUUID())
                        .withShadowListed(null)
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .build())
                .build();

        given(offenceUpdatedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceUpdated(offenceUpdatedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(1));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceAddedIfThereIsNoHearing() throws Exception {
        final OffenceAdded hearingData = OffenceAdded.offenceAdded()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(randomUUID())
                        .withShadowListed(null)
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .build())
                .build();

        given(offenceAddedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(null);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceAdded(offenceAddedEnvelope);

        verify(properties, never()).replace(any(), objectNodeCaptur.capture());
        verify(hearingRepository, never()).save(hearing);
    }

    @Test
    public void shouldHandleOffenceAdded() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final OffenceAdded hearingData = OffenceAdded.offenceAdded()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(randomUUID())
                        .withShadowListed(null)
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .build())
                .build();

        given(offenceAddedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceAdded(offenceAddedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());
        String expectedOffenceCode = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences")
                .get(1).get("offenceCode").toString();

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(2));
        assertThat(expectedOffenceCode, equalTo("\"" + EXPECTED_OFFENCE_CODE + "\""));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceAddedWhenCaseIsNotInDB() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final OffenceAdded hearingData = OffenceAdded.offenceAdded()
                .withHearingId(HEARING_ID)
                .withCaseId(randomUUID())
                .withDefendantId(DEFENDANT_ID)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(randomUUID())
                        .withShadowListed(null)
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .build())
                .build();

        given(offenceAddedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceAdded(offenceAddedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());
        String expectedOffenceCode = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences")
                .get(0).get("offenceCode").toString();

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(1));
        assertThat(expectedOffenceCode, equalTo("\"" + testCases.get(0).getDefendants().get(0).getOffences().get(0).getOffenceCode()+ "\""));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceAddedWhenDefendantIsNotInListedCasedDb() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final OffenceAdded hearingData = OffenceAdded.offenceAdded()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID_2)
                .withOffence(Offence.offence()
                        .withStartDate(LocalDates.to(LocalDate.now()))
                        .withOffenceCode(EXPECTED_OFFENCE_CODE)
                        .withId(randomUUID())
                        .withShadowListed(null)
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(STRING.next())
                                .build())
                        .build())
                .build();

        given(offenceAddedEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceAdded(offenceAddedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(1));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceDeleteAndDeleteSimpleOffence() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final OffenceDeleted offenceDeleted = createOffenceDeleted();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        given(offenceDeletedEnvelope.payload()).willReturn(offenceDeleted);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceDeleted(offenceDeletedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(0));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceDeleteAndDeleteSimpleOffenceWhenCaseIsNotThere() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final OffenceDeleted offenceDeleted =  new OffenceDeleted.Builder()
                .withHearingId(HEARING_ID)
                .withCaseId(randomUUID())
                .withOffenceId(OFFENCE_ID)
                .withDefendantId(DEFENDANT_ID)
                .build();
        final List<ListedCase> testCases = createListedCases();
        final String testCasesString = mapper.writeValueAsString(testCases);
        final JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        given(offenceDeletedEnvelope.payload()).willReturn(offenceDeleted);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceDeleted(offenceDeletedEnvelope);

        verify(properties).replace(any(), objectNodeCaptur.capture());

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        assertThat(numberOffence, equalTo(1));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldHandleOffenceDeleteAndDeleteSimpleOffenceIfThereIsNoHearing() throws Exception {

        final OffenceDeleted offenceDeleted = createOffenceDeleted();

        given(offenceDeletedEnvelope.payload()).willReturn(offenceDeleted);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(null);


        final ArgumentCaptor<ArrayNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantOffencesEventListener.offenceDeleted(offenceDeletedEnvelope);

        verify(properties, never()).replace(any(), objectNodeCaptur.capture());
        verify(hearingRepository, never()).save(hearing);
        verify(hearingSearchSyncService, never()).sync(any());
    }

    private OffenceDeleted createOffenceDeleted() {
        return new OffenceDeleted.Builder()
                .withHearingId(HEARING_ID)
                .withCaseId(CASE_ID)
                .withOffenceId(OFFENCE_ID)
                .withDefendantId(DEFENDANT_ID)
                .build();
    }

    private List<ListedCase> createListedCases() {
        return singletonList(ListedCase.listedCase()
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(STRING.next())
                        .withAuthorityId(randomUUID())
                        .withCaseReference(STRING.next())
                        .build())
                .withDefendants(singletonList(Defendant.defendant()
                        .withSpecificRequirements(null)
                        .withFirstName("FirstName")
                        .withDatesToAvoid("Dates to avoid")
                        .withId(DEFENDANT_ID)
                        .withBailStatus(new BailStatus.Builder().withCode("C").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).withDescription("Custody or remanded into custody").build())
                        .withOffences(singletonList(Offence.offence()
                                .withId(OFFENCE_ID)
                                .withOffenceCode(STRING.next())
                                .withStartDate(LocalDates.to(LocalDate.now()))
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withLegislation(STRING.next())
                                        .withTitle(STRING.next())
                                        .build())
                                .withShadowListed(true)
                                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                                .build()))
                        .build()))
                .withId(CASE_ID)
                .withShadowListed(true)
                .build());
    }
}