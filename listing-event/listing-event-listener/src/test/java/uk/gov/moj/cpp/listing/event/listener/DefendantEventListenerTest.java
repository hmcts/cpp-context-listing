package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.listing.events.BailStatus;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.justice.listing.events.NewDefendantDetailsUpdated;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
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
import org.mockito.Captor;
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
    private static final BailStatus EXPECTED_BAIL_STATUS = BailStatus.CONDITIONAL;

    @Spy
    private ObjectMapper mapper =  new ObjectMapperProducer().objectMapper();

    @Mock
    private Envelope<NewDefendantDetailsUpdated> defendantDetailsUpdatedEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    Hearing hearing;

    @Captor
    private ArgumentCaptor<JsonEnvelope> objectNodeCaptur;


    @Mock
    ObjectNode properties;

    @InjectMocks
    private DefendantEventListener defendantEventListener;

    @Test
    public void shouldHandleDefendantDetailsUpdatedAndPersistSimpleDefendant() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ListedCase> testCases = createListedCases();
        String testCasesString =  mapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        Envelope<NewDefendantDetailsUpdated>  envelope = (Envelope<NewDefendantDetailsUpdated>) mock(Envelope.class);

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


        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        defendantEventListener.defendantDetailsUpdated(defendantDetailsUpdatedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        String actualBailStatus = objectNodeCaptor.getValue().get(0).get("defendants").get(0).get("bailStatus").toString();
        assertThat(actualBailStatus, equalTo("\"" + EXPECTED_BAIL_STATUS.toString() + "\""));
        verify(hearingRepository).save(hearing);
    }

    private List<ListedCase> createListedCases() {
        return singletonList(ListedCase.listedCase()
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(STRING.next())
                        .withAuthorityId(randomUUID())
                        .withCaseReference(STRING.next())
                        .build())
                .withDefendants(singletonList(Defendant.defendant()
                        .withSpecificRequirements(Optional.empty())
                        .withId(DEFENDANT_ID)
                        .withBailStatus(of(BailStatus.IN_CUSTODY))
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
                .withId(CASE_ID)
                .build());
    }
}