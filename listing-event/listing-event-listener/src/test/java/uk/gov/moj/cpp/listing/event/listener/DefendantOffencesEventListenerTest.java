package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.BailStatus;
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
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantOffencesEventListenerTest {

    private static final String LISTED_CASES = "listedCases";
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
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
    ObjectNode properties;

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
                        .withEndDate(of(LocalDates.to(LocalDate.now())))
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(of(STRING.next()))
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

        verify(properties).replace(anyObject(), objectNodeCaptur.capture());
        final JsonNode newListedCase = objectNodeCaptur.getValue().get(0);
        final ListedCase testListedCase = testCases.get(0);
        String expectedOffenceCode = newListedCase.get("defendants").get(0).get("offences")
                .get(0).get("offenceCode").toString();

        int numberOffence = newListedCase.get("defendants").get(0).get("offences").size();
        MatcherAssert.assertThat(numberOffence, equalTo(1));
        MatcherAssert.assertThat(expectedOffenceCode, equalTo("\"" + EXPECTED_OFFENCE_CODE + "\""));

        MatcherAssert.assertThat(newListedCase.get("shadowListed").asBoolean(), equalTo(testListedCase.getShadowListed().get()));
        MatcherAssert.assertThat(newListedCase.get("defendants").get(0).get("offences").get(0).get("shadowListed").asBoolean(),
                equalTo(testListedCase.getDefendants().get(0).getOffences().get(0).getShadowListed().get()));

        verify(hearingRepository).save(hearing);
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
                        .withId(OFFENCE_ID)
                        .withShadowListed(Optional.ofNullable(null))
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withTitle(STRING.next())
                                .withWelshTitle(STRING.next())
                                .withLegislation(of(STRING.next()))
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

        verify(properties).replace(anyObject(), objectNodeCaptur.capture());
        String expectedOffenceCode = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences")
                .get(1).get("offenceCode").toString();

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        MatcherAssert.assertThat(numberOffence, equalTo(2));
        MatcherAssert.assertThat(expectedOffenceCode, equalTo("\"" + EXPECTED_OFFENCE_CODE + "\""));
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

        verify(properties).replace(anyObject(), objectNodeCaptur.capture());

        int numberOffence = objectNodeCaptur.getValue().get(0).get("defendants").get(0).get("offences").size();
        MatcherAssert.assertThat(numberOffence, equalTo(0));
        verify(hearingRepository).save(hearing);
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
                        .withSpecificRequirements(empty())
                        .withFirstName(of("FirstName"))
                        .withDatesToAvoid(of("Dates to avoid"))
                        .withId(DEFENDANT_ID)
                        .withBailStatus(of(new BailStatus.Builder().withCode("C").withId(fromString("12e69486-4d01-3403-a50a-7419ca040635")).withDescription("Custody or remanded into custody").build()))
                        .withOffences(singletonList(Offence.offence()
                                .withId(OFFENCE_ID)
                                .withOffenceCode(STRING.next())
                                .withStartDate(LocalDates.to(LocalDate.now()))
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withLegislation(of(STRING.next()))
                                        .withTitle(STRING.next())
                                        .build())
                                .withShadowListed(Optional.of(Boolean.TRUE))
                                .build()))
                        .build()))
                .withId(CASE_ID)
                .withShadowListed(Optional.of(Boolean.TRUE))
                .build());
    }
}