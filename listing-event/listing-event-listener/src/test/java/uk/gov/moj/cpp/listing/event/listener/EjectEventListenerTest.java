package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.Address;
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
import java.util.List;
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
public class EjectEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID COURT_APPLICATIONS_ID = randomUUID();
    private static final String LISTED_CASES = "listedCases";
    private static final String COURT_APPLICATION_FIELD = "courtApplications";
    private static final String COURT_APPLICATION_TYPE = STRING.next();
    private static final String APPLICATION_PARTICULARS = STRING.next();
    private static final Address APPLICANT_ADDRESS = Address
            .address()
            .withAddress1(STRING.next())
            .withAddress2(of(STRING.next()))
            .withAddress3(of(STRING.next()))
            .withAddress4(of(STRING.next()))
            .withAddress5(of(STRING.next()))
            .withPostcode(of(STRING.next()))
            .build();
    private static final Address RESPONDENT_ADDRESS = Address
            .address()
            .withAddress1(STRING.next())
            .withAddress2(of(STRING.next()))
            .withAddress3(of(STRING.next()))
            .withAddress4(of(STRING.next()))
            .withAddress5(of(STRING.next()))
            .withPostcode(of(STRING.next()))
            .build();

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
        ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

        List<ListedCase> testCases = createListedCases();
        List<CourtApplication> testCourtApplications = createCourtApplications();

        String testCasesString = objectMapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        String testApplicationsString = mapper.writeValueAsString(testCourtApplications);
        JsonNode testApplicationProperties = objectMapper.readTree(testApplicationsString);

        final Envelope<CaseEjected> caseEjectedEnvelope = (Envelope<CaseEjected>) mock(Envelope.class);

        CaseEjected ejectCase = CaseEjected.caseEjected()
                .withHearingId(HEARING_ID)
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
                .withHearingId(HEARING_ID)
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
        final ArrayNode applicationArrayNode = objectNodeCaptor.getValue();
        applicationArrayNode.forEach(applicationNode -> {
            if (applicationNode.get("id").asText().equals(COURT_APPLICATIONS_ID.toString()) || applicationNode.get("parentApplicationId").asText().equals(COURT_APPLICATIONS_ID.toString())) {
                assertEquals("Check if the application status is ejected", "true",
                        applicationNode.path("isEjected").asText());
            } else {
                assertEquals("Check if the application status is ejected", true,
                        applicationNode.path("isEjected").isMissingNode());
            }
            assertThat(APPLICATION_PARTICULARS, equalTo(applicationNode.get("applicationParticulars").asText()));
            validateAddress(applicationNode.get("applicant").get("address"), APPLICANT_ADDRESS);
            validateAddress(applicationNode.get("respondents").get(0).get("address"), RESPONDENT_ADDRESS);
        });
        verify(hearingRepository).save(hearing);

    }

    private void validateAddress(final JsonNode actualAddress, final Address expectedAddress) {
        assertThat(actualAddress.get("address1").asText(), equalTo(expectedAddress.getAddress1()));
        assertThat(actualAddress.get("address2").asText(), equalTo(expectedAddress.getAddress2().get()));
        assertThat(actualAddress.get("address3").asText(), equalTo(expectedAddress.getAddress3().get()));
        assertThat(actualAddress.get("address4").asText(), equalTo(expectedAddress.getAddress4().get()));
        assertThat(actualAddress.get("address5").asText(), equalTo(expectedAddress.getAddress5().get()));
        assertThat(actualAddress.get("postcode").asText(), equalTo(expectedAddress.getPostcode().get()));
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
                .withLinkedCaseIds(singletonList(CASE_ID))
                .withParentApplicationId(of(randomUUID()))
                .withId(COURT_APPLICATIONS_ID)
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(of(APPLICATION_PARTICULARS))
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .withAddress(of(APPLICANT_ADDRESS))
                        .build())
                .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .withAddress(of(RESPONDENT_ADDRESS))
                        .build()))
                .build();
        CourtApplication childApplication = CourtApplication.courtApplication()
                .withLinkedCaseIds(singletonList(CASE_ID))
                .withParentApplicationId(of(COURT_APPLICATIONS_ID))
                .withId(randomUUID())
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(of(APPLICATION_PARTICULARS))
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .withAddress(of(APPLICANT_ADDRESS))
                        .build())
                .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .withAddress(of(RESPONDENT_ADDRESS))
                        .build()))
                .build();
        CourtApplication noChildApplication = CourtApplication.courtApplication()
                .withLinkedCaseIds(singletonList(CASE_ID))
                .withParentApplicationId(of(randomUUID()))
                .withId(randomUUID())
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(of(APPLICATION_PARTICULARS))
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .withAddress(of(APPLICANT_ADDRESS))
                        .build())
                .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .withAddress(of(RESPONDENT_ADDRESS))
                        .build()))
                .build();

        List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(parentCourtApplication);
        courtApplications.add(childApplication);
        courtApplications.add(noChildApplication);
        return courtApplications;

    }

}
