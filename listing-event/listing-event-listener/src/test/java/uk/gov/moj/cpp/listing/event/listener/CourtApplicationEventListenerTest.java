package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtApplicationUpdatedForHearing;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
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

    @Spy
    private ObjectMapper mapper =  new ObjectMapperProducer().objectMapper();

    @Mock
    private Envelope<CourtApplicationUpdatedForHearing> courtApplicationUpdatedForHearingEnvelope;
    @Mock
    private Envelope<CourtApplicationAddedForHearing> courtApplicationAddedForHearingsEnvelope;

    @Mock
    private HearingRepository hearingRepository;

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
                        .withLinkedCaseId(of(LINKED_CASE_ID))
                        .withParentApplicationId(of(LINKED_APPLICATION_ID))
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(of(UPDATED_FIRST_NAME))
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .build())
                        .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(of(FIRST_NAME))
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
                                .build()))
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
        assertThat(objectNodeCaptor.getValue().get(0).get("applicant").get("firstName").toString(), equalTo("\"" + UPDATED_FIRST_NAME + "\""));
        assertThat(objectNodeCaptor.getValue().get(0).get("applicant").get("lastName").toString(), equalTo("\"" + UPDATED_LAST_NAME + "\""));
        verify(hearingRepository).save(hearing);
    }
    @Test
    public void shouldAddCourtApplicationForHearing() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<CourtApplication> testCases = createCourtApplications();
        String testCasesString =  mapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);
        Envelope<CourtApplicationAddedForHearing> envelope = (Envelope<CourtApplicationAddedForHearing>) mock(Envelope.class);
        CourtApplicationAddedForHearing hearingData = CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseId(of(LINKED_CASE_ID))
                        .withParentApplicationId(of(LINKED_APPLICATION_ID))
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(of(UPDATED_FIRST_NAME))
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .build())
                        .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(of(FIRST_NAME))
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
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
        assertThat(objectNodeCaptor.getValue().get(0).get("applicant").get("firstName").toString(), equalTo("\"" + FIRST_NAME + "\""));
        assertThat(objectNodeCaptor.getValue().get(0).get("applicant").get("lastName").toString(), equalTo("\"" + LAST_NAME + "\""));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldAddCourtApplicationForHearingWhenCourtApplicationsArrayNotPresentInHearingPayload() {
        Envelope<CourtApplicationAddedForHearing> envelope = (Envelope<CourtApplicationAddedForHearing>) mock(Envelope.class);
        CourtApplicationAddedForHearing hearingData = CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(HEARING_ID)
                .withCourtApplication(CourtApplication.courtApplication()
                        .withLinkedCaseId(of(LINKED_CASE_ID))
                        .withParentApplicationId(of(LINKED_APPLICATION_ID))
                        .withId(ID)
                        .withApplicationType(APPLICATION_TYPE)
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withFirstName(of(UPDATED_FIRST_NAME))
                                .withLastName(UPDATED_LAST_NAME)
                                .withIsRespondent(false)
                                .build())
                        .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                                .withFirstName(of(FIRST_NAME))
                                .withLastName(LAST_NAME)
                                .withIsRespondent(true)
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
        assertThat(objectNodeCaptor.getValue().get(0).get("applicant").get("firstName").toString(), equalTo("\"" + UPDATED_FIRST_NAME + "\""));
        assertThat(objectNodeCaptor.getValue().get(0).get("applicant").get("lastName").toString(), equalTo("\"" + UPDATED_LAST_NAME + "\""));
        verify(hearingRepository).save(hearing);
    }



    private List<CourtApplication> createCourtApplications() {
        return singletonList(CourtApplication.courtApplication()
                .withLinkedCaseId(of(LINKED_CASE_ID))
                .withParentApplicationId(of(LINKED_APPLICATION_ID))
                .withId(ID)
                .withApplicationType(APPLICATION_TYPE)
                .withApplicant(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(FIRST_NAME))
                        .withLastName(LAST_NAME)
                        .withIsRespondent(false)
                        .build())
                .withRespondents(Arrays.asList(ApplicantRespondent.applicantRespondent()
                        .withFirstName(of(FIRST_NAME))
                        .withLastName(LAST_NAME)
                        .withIsRespondent(true)
                        .build()))
                .build());
    }
}
