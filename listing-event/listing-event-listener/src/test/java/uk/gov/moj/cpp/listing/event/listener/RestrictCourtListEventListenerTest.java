package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.CourtListRestricted;
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
import java.util.Arrays;
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
public class RestrictCourtListEventListenerTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID DEFENDANTS_ID = randomUUID();
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
    private Envelope<CourtListRestricted> restrictCourtListEnvelopeForCase;

    @Spy
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();


    @InjectMocks
    private RestrictCourtListEventListener restrictCourtListEventListener;


    @Test
    public void shouldRestrictCourtListing() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        List<ListedCase> testCases = createListedCases();
        String testCasesString = objectMapper.writeValueAsString(testCases);
        JsonNode testCasesProperties = objectMapper.readTree(testCasesString);

        final Envelope<CourtListRestricted> restrictCourtListEnvelope = (Envelope<CourtListRestricted>) mock(Envelope.class);

        CourtListRestricted restrictCourtList = CourtListRestricted.courtListRestricted()
                .withDefendantIds(Arrays.asList(DEFENDANTS_ID))
                .withHearingId(HEARING_ID)
                .withRestrictCourtList(true)
                .withCourtApplicationType(Optional.empty())
                .build();
        given(restrictCourtListEnvelope.payload()).willReturn(restrictCourtList);
        given(restrictCourtListEnvelopeForCase.payload()).willReturn(restrictCourtList);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        restrictCourtListEventListener.hearingRestrictionForCourt(restrictCourtListEnvelopeForCase);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);

    }


    @Test
    public void shouldRestrictCourtListingForStandAloneApplications() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        List<CourtApplication> testCourtApplications = createCourtApplications();
        String testApplicationsString = mapper.writeValueAsString(testCourtApplications);
        JsonNode testCasesProperties = objectMapper.readTree(testApplicationsString);

        final Envelope<CourtListRestricted> restrictCourtListEnvelope = (Envelope<CourtListRestricted>) mock(Envelope.class);

        CourtListRestricted restrictCourtList = CourtListRestricted.courtListRestricted()
                .withCourtApplicationIds(Arrays.asList(COURT_APPLICATIONS_ID))
                .withHearingId(HEARING_ID)
                .withRestrictCourtList(true)
                .withCourtApplicationType(java.util.Optional.ofNullable(COURT_APPLICATION_TYPE))

                .build();
        given(restrictCourtListEnvelope.payload()).willReturn(restrictCourtList);
        given(restrictCourtListEnvelopeForCase.payload()).willReturn(restrictCourtList);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATION_FIELD)).willReturn(testCasesProperties);

        final ArgumentCaptor<ArrayNode> objectNodeCaptor =
                ArgumentCaptor.forClass(ArrayNode.class);

        restrictCourtListEventListener.hearingRestrictionForCourt(restrictCourtListEnvelope);
        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
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
                        .withId(DEFENDANTS_ID)
                        .withOffences(singletonList(Offence.offence()
                                .withId(randomUUID())
                                .withOffenceCode(STRING.next())
                                .withStartDate(LocalDates.to(LocalDate.now()))
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withTitle(STRING.next())
                                        .build())
                                .build()))
                        .build()))
                .withId(UUID.randomUUID())
                .build());
    }

    private List<uk.gov.justice.listing.events.CourtApplication> createCourtApplications() {
        return singletonList(CourtApplication.courtApplication()
                .withLinkedCaseId(of(randomUUID()))
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
                .build());
    }

}
