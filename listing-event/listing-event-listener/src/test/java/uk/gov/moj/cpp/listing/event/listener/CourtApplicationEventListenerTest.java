package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;


import javax.inject.Inject;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtApplicationUpdatedForHearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
public class CourtApplicationEventListenerTest {
    private static final UUID OFFENCE_ID = randomUUID();
    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID ID = randomUUID();
    private static final UUID LINKED_CASE_ID = randomUUID();
    private static final UUID LINKED_APPLICATION_ID = randomUUID();
    private static final String APPLICATION_TYPE = STRING.next();
    private static final String FIRST_NAME = "A";
    private static final String LAST_NAME = "B";
    private static final String UPDATED_FIRST_NAME = STRING.next();
    private static final String UPDATED_LAST_NAME = STRING.next();
    private static final String APPLICATION_PARTICULARS = STRING.next();
    private static final Address APPLICANT_ADDRESS = Address
            .address()
            .withAddress1(STRING.next())
            .withAddress2(STRING.next())
            .withAddress3(STRING.next())
            .withAddress4(STRING.next())
            .withAddress5(STRING.next())
            .withPostcode(STRING.next())
            .build();
    private static final Address RESPONDENT_ADDRESS = Address
            .address()
            .withAddress1(STRING.next())
            .withAddress2(STRING.next())
            .withAddress3(STRING.next())
            .withAddress4(STRING.next())
            .withAddress5(STRING.next())
            .withPostcode(STRING.next())
            .build();

    @Spy
    private ObjectMapper mapper =  new ObjectMapperProducer().objectMapper();

    @Mock
    private Envelope<CourtApplicationUpdatedForHearing> courtApplicationUpdatedForHearingEnvelope;
    @Mock
    private Envelope<CourtApplicationAddedForHearing> courtApplicationAddedForHearingsEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Mock
    Hearing hearing;

    @Captor
    private ArgumentCaptor<JsonEnvelope> objectNodeCaptur;


    @Mock
    ObjectNode properties;

    @InjectMocks
    private CourtApplicationEventListener courtApplicationEventListener;

    @Test
    public void shouldHandleCourtApplicationUpdatedAndPersist() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<uk.gov.justice.listing.events.CourtApplication> testCases = createCourtApplications();
        String testCasesString =  mapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        Envelope<CourtApplicationUpdatedForHearing>  envelope = (Envelope<CourtApplicationUpdatedForHearing>) mock(Envelope.class);

        CourtApplicationUpdatedForHearing hearingData = CourtApplicationUpdatedForHearing.courtApplicationUpdatedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseIds(singletonList(LINKED_CASE_ID))
                        .withParentApplicationId(LINKED_APPLICATION_ID)
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicationParticulars(APPLICATION_PARTICULARS)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(UPDATED_FIRST_NAME)
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .withAddress(APPLICANT_ADDRESS)
                                .build())
                        .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
                                .withAddress(RESPONDENT_ADDRESS)
                                .build()))
                        .withOffences(Arrays.asList(Offence.offence().withId(OFFENCE_ID).build()))
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(courtApplicationUpdatedForHearingEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS)).willReturn(testCasesProperties);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        courtApplicationEventListener.courtApplicationUpdated(courtApplicationUpdatedForHearingEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        validateApplicantAndRespondents(objectNodeCaptor, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);
        verify(hearingRepository).save(hearing);
        verify(hearingSearchSyncService).sync(HEARING_ID);
    }

    @Test
    public void shouldAddCourtApplicationForHearing() {
        Envelope<CourtApplicationAddedForHearing> envelope = (Envelope<CourtApplicationAddedForHearing>) mock(Envelope.class);
        CourtApplicationAddedForHearing hearingData = CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseIds(singletonList(LINKED_CASE_ID))
                        .withParentApplicationId(LINKED_APPLICATION_ID)
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicationParticulars(APPLICATION_PARTICULARS)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withIsRespondent(false)
                                .withAddress(APPLICANT_ADDRESS)
                                .build())
                        .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
                                .withAddress(RESPONDENT_ADDRESS)
                                .build()))
                        .build())
                .build();
        given(envelope.payload()).willReturn(hearingData);
        given(courtApplicationAddedForHearingsEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS)).willReturn(null);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        courtApplicationEventListener.courtApplicationAdded(envelope);

        verify(properties).set(anyObject(), objectNodeCaptor.capture());
        validateApplicantAndRespondents(objectNodeCaptor, FIRST_NAME, LAST_NAME);
        verify(hearingRepository).save(hearing);
        verify(hearingSearchSyncService).sync(HEARING_ID);
    }

    @Test
    public void shouldTestAddCourtApplicationForExistingApplicationInHearing() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<CourtApplication> testCases = createCourtApplications();
        String testCasesString =  mapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        Envelope<CourtApplicationAddedForHearing> envelope = (Envelope<CourtApplicationAddedForHearing>) mock(Envelope.class);
        CourtApplicationAddedForHearing hearingData = CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseIds(singletonList(LINKED_CASE_ID))
                        .withParentApplicationId(LINKED_APPLICATION_ID)
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicationParticulars(APPLICATION_PARTICULARS)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(UPDATED_FIRST_NAME)
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .withAddress(APPLICANT_ADDRESS)
                                .build())
                        .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
                                .withAddress(RESPONDENT_ADDRESS)
                                .build()))
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(courtApplicationAddedForHearingsEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        courtApplicationEventListener.courtApplicationAdded(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        validateApplicantAndRespondents(objectNodeCaptor, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);
        verify(hearingRepository).save(hearing);
        verify(hearingSearchSyncService).sync(HEARING_ID);
    }

    @Test
    public void shouldTestManyAddCourtApplicationForHearing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<CourtApplication> testCases = createCourtApplications();
        String testCasesString =  mapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        Envelope<CourtApplicationAddedForHearing> envelope = (Envelope<CourtApplicationAddedForHearing>) mock(Envelope.class);
        CourtApplicationAddedForHearing hearingData = CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseIds(singletonList(LINKED_CASE_ID))
                        .withParentApplicationId(LINKED_APPLICATION_ID)
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicationParticulars(APPLICATION_PARTICULARS)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(UPDATED_FIRST_NAME)
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .withAddress(APPLICANT_ADDRESS)
                                .build())
                        .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
                                .withAddress(RESPONDENT_ADDRESS)
                                .build()))
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(courtApplicationAddedForHearingsEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        courtApplicationEventListener.courtApplicationAdded(envelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        validateApplicantAndRespondents(objectNodeCaptor, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);
        verify(hearingRepository).save(hearing);
        verify(hearingSearchSyncService).sync(HEARING_ID);

        CourtApplicationAddedForHearing newHearingData = CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseIds(singletonList(LINKED_CASE_ID))
                        .withParentApplicationId(LINKED_APPLICATION_ID)
                        .withId(randomUUID())
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicationParticulars(APPLICATION_PARTICULARS)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(UPDATED_FIRST_NAME)
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .withAddress(APPLICANT_ADDRESS)
                                .build())
                        .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
                                .withAddress(RESPONDENT_ADDRESS)
                                .build()))
                        .build())
                .build();

        given(envelope.payload()).willReturn(newHearingData);
        given(courtApplicationAddedForHearingsEnvelope.payload()).willReturn(newHearingData);
        given(properties.get(COURT_APPLICATIONS)).willReturn(testCasesProperties);

        courtApplicationEventListener.courtApplicationAdded(envelope);
        verify(properties, times(2)).replace(anyObject(), objectNodeCaptor.capture());
        validateApplicantAndRespondents(objectNodeCaptor, FIRST_NAME, LAST_NAME);
        verify(hearingRepository,times(2)).save(hearing);
        verify(hearingSearchSyncService, times(2)).sync(HEARING_ID);
    }

    @Test
    public void shouldAddCourtApplicationForHearingWhenCourtApplicationsArrayNotPresentInHearingPayload() {
        Envelope<CourtApplicationAddedForHearing> envelope = (Envelope<CourtApplicationAddedForHearing>) mock(Envelope.class);
        CourtApplicationAddedForHearing hearingData = CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseIds(singletonList(LINKED_CASE_ID))
                        .withParentApplicationId(LINKED_APPLICATION_ID)
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicationParticulars(APPLICATION_PARTICULARS)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(UPDATED_FIRST_NAME)
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .withAddress(APPLICANT_ADDRESS)
                                .build())
                        .withRespondents(singletonList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
                                .withAddress(RESPONDENT_ADDRESS)
                                .build()))
                        .build())
                .build();

        given(envelope.payload()).willReturn(hearingData);
        given(courtApplicationAddedForHearingsEnvelope.payload()).willReturn(hearingData);
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS)).willReturn(null);


        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        courtApplicationEventListener.courtApplicationAdded(envelope);

        verify(properties).set(anyString(), objectNodeCaptor.capture());
        validateApplicantAndRespondents(objectNodeCaptor, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);
        verify(hearingRepository).save(hearing);
        verify(hearingSearchSyncService).sync(HEARING_ID);
    }

    private List<CourtApplication> createCourtApplications() {
        return singletonList(CourtApplication.courtApplication()
                .withLinkedCaseIds(singletonList(LINKED_CASE_ID))
                .withParentApplicationId(LINKED_APPLICATION_ID)
                .withId(ID)
                .withApplicationType(APPLICATION_TYPE)
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsRespondent(false)
                        .withAddress(APPLICANT_ADDRESS)
                        .build())
                .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsRespondent(true)
                        .withAddress(RESPONDENT_ADDRESS)
                        .build()))
                .build());
    }

    private void validateApplicantAndRespondents(final ArgumentCaptor<ArrayNode> objectNodeCaptor, final String firstName, final String lastName) {
        final JsonNode applicant = objectNodeCaptor.getValue().get(0).get("applicant");
        final JsonNode applicantAddress = applicant.get("address");
        final JsonNode respondentAddress = objectNodeCaptor.getValue().get(0).get("respondents").get(0).get("address");
        assertThat(applicant.get("firstName").asText(), equalTo(firstName));
        assertThat(applicant.get("lastName").asText(), equalTo(lastName));
        assertThat(objectNodeCaptor.getValue().get(0).get("applicationParticulars").asText(), equalTo(APPLICATION_PARTICULARS));
        validateAddress(applicantAddress, APPLICANT_ADDRESS);
        validateAddress(respondentAddress, RESPONDENT_ADDRESS);
    }

    private void validateAddress(final JsonNode actualAddress, final Address expectedAddress) {
        assertThat(actualAddress.get("address1").asText(), equalTo(expectedAddress.getAddress1()));
        assertThat(actualAddress.get("address2").asText(), equalTo(expectedAddress.getAddress2()));
        assertThat(actualAddress.get("address3").asText(), equalTo(expectedAddress.getAddress3()));
        assertThat(actualAddress.get("address4").asText(), equalTo(expectedAddress.getAddress4()));
        assertThat(actualAddress.get("address5").asText(), equalTo(expectedAddress.getAddress5()));
        assertThat(actualAddress.get("postcode").asText(), equalTo(expectedAddress.getPostcode()));
    }
}
