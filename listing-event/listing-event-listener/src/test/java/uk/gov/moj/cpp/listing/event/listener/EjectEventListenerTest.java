package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import org.junit.Assert;
import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.ApplicationEjected;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
public class EjectEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID COURT_APPLICATIONS_ID = randomUUID();
    private static final String LISTED_CASES = "listedCases";
    private static final String COURT_APPLICATION_FIELD = "courtApplications";
    private static final String COURT_APPLICATION_TYPE = STRING.next();
    @Mock
    private HearingRepository hearingRepository;


    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;


    @Mock
    private Envelope<CaseEjected> ejectCaseEnvelopeForCase;

    @Mock
    private Envelope<ApplicationEjected> ejectApplicationEnvelopeForCase;

    @Spy
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();


    @InjectMocks
    private EjectEventListener ejectEventListener;


    @Test
    public void shouldEjectCaseForListing() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        List<ListedCase> testCases = createListedCases();
        List<CourtApplication> testCourtApplications = createCourtApplications();
        String testCasesString = objectMapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        String testApplicationsString = mapper.writeValueAsString(testCourtApplications);
        JsonNode testApplicationProperties = objectMapper.readTree(testApplicationsString);

        final Envelope<CaseEjected> caseEjectedEnvelope = (Envelope<CaseEjected>) mock(Envelope.class);

        CaseEjected ejectCase = CaseEjected.caseEjected()
                .withHearingIds(Arrays.asList(HEARING_ID))
                .withProsecutionCaseId(CASE_ID)
                .build();
        given(caseEjectedEnvelope.payload()).willReturn(ejectCase);
        given(ejectCaseEnvelopeForCase.payload()).willReturn(ejectCase);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);
        given(properties.get(COURT_APPLICATION_FIELD)).willReturn(testApplicationProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        ejectEventListener.caseEjected(ejectCaseEnvelopeForCase);
        verify(properties, times(2)).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository, times(2)).save(hearing);

    }


    @Test
    public void shouldEjectApplicationForListing() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        List<CourtApplication> testCourtApplications = createCourtApplications();
        String testApplicationsString = mapper.writeValueAsString(testCourtApplications);
        JsonNode testCasesProperties = objectMapper.readTree(testApplicationsString);

        final Envelope<ApplicationEjected> applicationEjectedEnvelope = (Envelope<ApplicationEjected>) mock(Envelope.class);

        ApplicationEjected ejectApplication = ApplicationEjected.applicationEjected()
                .withHearingIds(Arrays.asList(HEARING_ID))
                .withApplicationId(COURT_APPLICATIONS_ID)
                .build();
        given(applicationEjectedEnvelope.payload()).willReturn(ejectApplication);
        given(ejectApplicationEnvelopeForCase.payload()).willReturn(ejectApplication);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATION_FIELD)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        ejectEventListener.applicationEjected(applicationEjectedEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        final ArrayNode applicationArrayNode =objectNodeCaptor.getValue();
        applicationArrayNode.forEach(applicationNode -> {
            if (applicationNode.get("id").asText().equals(COURT_APPLICATIONS_ID.toString()) || applicationNode.get("parentApplicationId").asText().equals(COURT_APPLICATIONS_ID.toString())) {
                Assert.assertEquals("Check if the application status is ejected", "true",
                        applicationNode.path("isEjected").asText());
            } else {
                Assert.assertEquals("Check if the application status is ejected", true,
                        applicationNode.path("isEjected").isMissingNode());
            }
        });
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
                        .withId(randomUUID())
                        .withOffences(singletonList(Offence.offence()
                                .withId(randomUUID())
                                .withOffenceCode(STRING.next())
                                .withStartDate(LocalDates.to(LocalDate.now()))
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withTitle(STRING.next())
                                        .build())
                                .build()))
                        .build()))
                .withId(CASE_ID)
                .build());
    }

    private List<CourtApplication> createCourtApplications() {
       CourtApplication parentCourtApplication = CourtApplication.courtApplication()
                .withLinkedCaseId(of(CASE_ID))
                .withParentApplicationId(of(randomUUID()))
                .withId(COURT_APPLICATIONS_ID)
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .build())
                .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .build()))
                .build();
        CourtApplication childApplication = CourtApplication.courtApplication()
                .withParentApplicationId(of(COURT_APPLICATIONS_ID))
                .withId(randomUUID())
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .build())
                .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .build()))
                .build();
        CourtApplication noChildApplication = CourtApplication.courtApplication()
                .withParentApplicationId(of(randomUUID()))
                .withId(randomUUID())
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .build())
                .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .build()))
                .build();

        List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(parentCourtApplication);
        courtApplications.add(childApplication);
        courtApplications.add(noChildApplication);
        return courtApplications;

    }

}
