package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.listing.events.*;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.ApplicantRespondent.applicantRespondent;
import static uk.gov.justice.listing.events.CourtApplication.courtApplication;
import static uk.gov.justice.listing.events.CourtListRestricted.courtListRestricted;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@ExtendWith(MockitoExtension.class)
public class RestrictCourtListEventListenerTest {
    private static final ObjectMapper MAPPER = new ObjectMapperProducer().objectMapper();

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID DEFENDANTS_ID = randomUUID();
    private static final UUID COURT_APPLICATIONS_ID = randomUUID();
    private static final String LISTED_CASES = "listedCases";
    private static final String COURT_APPLICATIONS_FIELD = "courtApplications";
    private static final String COURT_APPLICATION_TYPE = STRING.next();
    private static final String APPLICATION_PARTICULARS = STRING.next();
    private static final UUID APPLICANT_ID = randomUUID();
    private static final UUID RESPONDENT_ID_1 = randomUUID();
    private static final UUID RESPONDENT_ID_2 = randomUUID();
    private static final UUID RESPONDENT_MASTER_DEFENDANT_ID_1 = randomUUID();
    private static final UUID RESPONDENT_MASTER_DEFENDANT_ID_2 = randomUUID();
    private static final UUID SUBJECT_ID = randomUUID();
    private static final UUID SUBJECT_MASTER_DEFENDANT_ID = randomUUID();
    private static final String EVENT_NAME = "listing.events.court-list-restricted";

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

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Hearing hearing;

    @Mock
    private ObjectNode properties;

    @Captor
    private ArgumentCaptor<ArrayNode> objectNodeCaptor;

    @InjectMocks
    private RestrictCourtListEventListener target;

    @Test
    public void shouldRestrictCourtListing() throws IOException {
        List<ListedCase> testCases = createListedCases();
        String testCasesString = MAPPER.writeValueAsString(testCases);
        JsonNode testCasesProperties = MAPPER.readTree(testCasesString);

        CourtListRestricted restrictCourtList = courtListRestricted()
                .withDefendantIds(singletonList(DEFENDANTS_ID))
                .withHearingId(HEARING_ID)
                .withRestrictCourtList(true)
                .withCourtApplicationType(null)
                .build();
        final Envelope<CourtListRestricted> restrictCourtListEnvelope = envelopeFrom(metadataWithRandomUUID(EVENT_NAME), restrictCourtList);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        target.hearingRestrictionForCourt(restrictCourtListEnvelope);

        verify(properties).replace(any(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
        assertThat(objectNodeCaptor.getValue().toString(), isJson(allOf(
                withJsonPath("$[0].defendants[0].offences[0].listingNumber", equalTo(1)))));
    }

    @Test
    public void shouldRestrictCourtListingForStandAloneApplications() throws IOException {
        List<CourtApplication> testCourtApplications = createCourtApplications();
        String testApplicationsString = MAPPER.writeValueAsString(testCourtApplications);
        JsonNode testCasesProperties = MAPPER.readTree(testApplicationsString);

        CourtListRestricted restrictCourtList = courtListRestricted()
                .withCourtApplicationIds(singletonList(COURT_APPLICATIONS_ID))
                .withHearingId(HEARING_ID)
                .withRestrictCourtList(true)
                .withCourtApplicationType(COURT_APPLICATION_TYPE)
                .build();
        final Envelope<CourtListRestricted> restrictCourtListEnvelope = envelopeFrom(metadataWithRandomUUID(EVENT_NAME), restrictCourtList);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS_FIELD)).willReturn(testCasesProperties);

        target.hearingRestrictionForCourt(restrictCourtListEnvelope);

        verify(properties).replace(any(), objectNodeCaptor.capture());
        validateApplicantAndRespondents(objectNodeCaptor);
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldRestrictByAppropriateRespondentForStandAloneApplications() throws IOException {
        final List<CourtApplication> courtApplications = createCourtApplicationsWithMultipleRespondents();
        final CourtApplication expectedCourtApplication = courtApplications.get(0);
        final String courtApplicationsAsString = MAPPER.writeValueAsString(courtApplications);
        final JsonNode courtApplicationsProperties = MAPPER.readTree(courtApplicationsAsString);


        final CourtListRestricted payload = courtListRestricted()
                .withHearingId(HEARING_ID)
                .withRestrictCourtList(TRUE)
                .withCourtApplicationRespondentIds(singletonList(RESPONDENT_ID_1))
                .withCourtApplicationIds(asList(COURT_APPLICATIONS_ID))
                .withCourtApplicationType(null)
                .withCourtApplicationApplicantIds(asList(APPLICANT_ID))
                .withCaseIds(asList(COURT_APPLICATIONS_ID))
                .build();
        final Envelope<CourtListRestricted> restrictCourtListEnvelope = envelopeFrom(metadataWithRandomUUID(EVENT_NAME), payload);
        final Hearing hearing = new Hearing(payload.getHearingId(), properties);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(properties.get(COURT_APPLICATIONS_FIELD)).willReturn(courtApplicationsProperties);
        given(properties.get(LISTED_CASES)).willReturn(courtApplicationsProperties);

        target.hearingRestrictionForCourt(restrictCourtListEnvelope);

        verify(properties, times(4)).replace(any(), objectNodeCaptor.capture());
        final ArrayNode applicationArrayNode = objectNodeCaptor.getValue();
        final List<Matcher<? super ReadContext>> matchers = newArrayList();
        matchers.add(withJsonPath("$", hasSize(1)));
        matchers.addAll(getApplicantMatchers(expectedCourtApplication));
        matchers.addAll(getCourtApplicationMatchers(expectedCourtApplication));
        matchers.addAll(getRespondentMatchersForRestricted(expectedCourtApplication));
        assertThat(applicationArrayNode.toString(), isJson(allOf(matchers)));

        verify(hearingRepository, times(4)).save(hearing);
    }

    @Test
    public void shouldRestrictSubjectByIdForStandAloneApplications() throws IOException {
        final List<CourtApplication> courtApplications = createCourtApplicationsWithSubject();
        final String courtApplicationsAsString = MAPPER.writeValueAsString(courtApplications);
        final JsonNode courtApplicationsProperties = MAPPER.readTree(courtApplicationsAsString);

        final CourtListRestricted payload = courtListRestricted()
                .withHearingId(HEARING_ID)
                .withRestrictCourtList(TRUE)
                .withCourtApplicationSubjectIds(singletonList(SUBJECT_ID))
                .build();
        final Envelope<CourtListRestricted> restrictCourtListEnvelope = envelopeFrom(metadataWithRandomUUID(EVENT_NAME), payload);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS_FIELD)).willReturn(courtApplicationsProperties);

        target.hearingRestrictionForCourt(restrictCourtListEnvelope);

        verify(properties).replace(any(), objectNodeCaptor.capture());
        final ArrayNode applicationArrayNode = objectNodeCaptor.getValue();
        assertThat(applicationArrayNode.toString(), isJson(allOf(
                withJsonPath("$[0].subject.id", equalTo(SUBJECT_ID.toString())),
                withJsonPath("$[0].subject.masterDefendantId", equalTo(SUBJECT_MASTER_DEFENDANT_ID.toString())),
                withJsonPath("$[0].subject.restrictFromCourtList", equalTo(true))
        )));
        verify(hearingRepository).save(hearing);
    }

    private Collection<? extends Matcher<? super ReadContext>> getCourtApplicationMatchers(final CourtApplication courtApplication) {
        return newArrayList(
                withJsonPath("$[0].id", equalTo(courtApplication.getId().toString())),
                withJsonPath("$[0].applicationType", equalTo(courtApplication.getApplicationType())),
                withJsonPath("$[0].applicationParticulars", equalTo(courtApplication.getApplicationParticulars()))
        );
    }

    private Collection<? extends Matcher<? super ReadContext>> getApplicantMatchers(final CourtApplication courtApplication) {
        return newArrayList(
                withJsonPath("$[0].applicant.id", equalTo(courtApplication.getApplicant().getId().toString())),
                withJsonPath("$[0].applicant.firstName", equalTo(courtApplication.getApplicant().getFirstName())),
                withJsonPath("$[0].applicant.lastName", equalTo(courtApplication.getApplicant().getLastName())),
                withJsonPath("$[0].applicant.isRespondent", equalTo(courtApplication.getApplicant().getIsRespondent())),
                withJsonPath("$[0].applicant.restrictFromCourtList", equalTo(courtApplication.getApplicant().getRestrictFromCourtList())),
                withJsonPath("$[0].applicant.address.address1", equalTo(APPLICANT_ADDRESS.getAddress1())),
                withJsonPath("$[0].applicant.address.address2", equalTo(APPLICANT_ADDRESS.getAddress2())),
                withJsonPath("$[0].applicant.address.address3", equalTo(APPLICANT_ADDRESS.getAddress3())),
                withJsonPath("$[0].applicant.address.address4", equalTo(APPLICANT_ADDRESS.getAddress4())),
                withJsonPath("$[0].applicant.address.address5", equalTo(APPLICANT_ADDRESS.getAddress5())),
                withJsonPath("$[0].applicant.address.postcode", equalTo(APPLICANT_ADDRESS.getPostcode()))
        );
    }

    private Collection<? extends Matcher<? super ReadContext>> getRespondentMatchersForRestricted(final CourtApplication courtApplication) {
        return newArrayList(
                withJsonPath("$[0].respondents", hasSize(2)),

                withJsonPath("$[0].respondents[0].id", equalTo(courtApplication.getRespondents().get(0).getId().toString())),
                withJsonPath("$[0].respondents[0].firstName", equalTo(courtApplication.getRespondents().get(0).getFirstName())),
                withJsonPath("$[0].respondents[0].lastName", equalTo(courtApplication.getRespondents().get(0).getLastName())),
                withJsonPath("$[0].respondents[0].isRespondent", equalTo(courtApplication.getRespondents().get(0).getIsRespondent())),
                withJsonPath("$[0].respondents[0].restrictFromCourtList", equalTo(true)),
                withJsonPath("$[0].respondents[0].address.address1", equalTo(RESPONDENT_ADDRESS.getAddress1())),
                withJsonPath("$[0].respondents[0].address.address2", equalTo(RESPONDENT_ADDRESS.getAddress2())),
                withJsonPath("$[0].respondents[0].address.address3", equalTo(RESPONDENT_ADDRESS.getAddress3())),
                withJsonPath("$[0].respondents[0].address.address4", equalTo(RESPONDENT_ADDRESS.getAddress4())),
                withJsonPath("$[0].respondents[0].address.address5", equalTo(RESPONDENT_ADDRESS.getAddress5())),
                withJsonPath("$[0].respondents[0].address.postcode", equalTo(RESPONDENT_ADDRESS.getPostcode())),

                withJsonPath("$[0].respondents[1].id", equalTo(courtApplication.getRespondents().get(1).getId().toString())),
                withJsonPath("$[0].respondents[1].firstName", equalTo(courtApplication.getRespondents().get(1).getFirstName())),
                withJsonPath("$[0].respondents[1].lastName", equalTo(courtApplication.getRespondents().get(1).getLastName())),
                withJsonPath("$[0].respondents[1].isRespondent", equalTo(courtApplication.getRespondents().get(1).getIsRespondent())),
                withJsonPath("$[0].respondents[1].restrictFromCourtList", equalTo(courtApplication.getRespondents().get(1).getRestrictFromCourtList())),
                withJsonPath("$[0].respondents[1].address.address1", equalTo(RESPONDENT_ADDRESS.getAddress1())),
                withJsonPath("$[0].respondents[1].address.address2", equalTo(RESPONDENT_ADDRESS.getAddress2())),
                withJsonPath("$[0].respondents[1].address.address3", equalTo(RESPONDENT_ADDRESS.getAddress3())),
                withJsonPath("$[0].respondents[1].address.address4", equalTo(RESPONDENT_ADDRESS.getAddress4())),
                withJsonPath("$[0].respondents[1].address.address5", equalTo(RESPONDENT_ADDRESS.getAddress5())),
                withJsonPath("$[0].respondents[1].address.postcode", equalTo(RESPONDENT_ADDRESS.getPostcode()))
        );
    }

    private void validateApplicantAndRespondents(final ArgumentCaptor<ArrayNode> objectNodeCaptor) {
        final ArrayNode applicationArrayNode = objectNodeCaptor.getValue();
        applicationArrayNode.forEach(applicationNode -> {
            assertThat(applicationNode.get("applicationParticulars").asText(), is(APPLICATION_PARTICULARS));
            validateAddress(applicationNode.get("applicant").get("address"), APPLICANT_ADDRESS);
            validateAddress(applicationNode.get("respondents").get(0).get("address"), RESPONDENT_ADDRESS);
        });
    }

    private void validateAddress(final JsonNode actualAddress, final Address expectedAddress) {
        assertThat(actualAddress.get("address1").asText(), equalTo(expectedAddress.getAddress1()));
        assertThat(actualAddress.get("address2").asText(), equalTo(expectedAddress.getAddress2()));
        assertThat(actualAddress.get("address3").asText(), equalTo(expectedAddress.getAddress3()));
        assertThat(actualAddress.get("address4").asText(), equalTo(expectedAddress.getAddress4()));
        assertThat(actualAddress.get("address5").asText(), equalTo(expectedAddress.getAddress5()));
        assertThat(actualAddress.get("postcode").asText(), equalTo(expectedAddress.getPostcode()));
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
                                .withShadowListed(false)
                                .withStartDate(LocalDates.to(LocalDate.now()))
                                .withListingNumber(1)
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withTitle(STRING.next())
                                        .build())
                                .build()))
                        .build()))
                .withId(UUID.randomUUID())
                .withShadowListed(false)
                .build());
    }

    private List<uk.gov.justice.listing.events.CourtApplication> createCourtApplications() {
        return singletonList(courtApplication()
                .withLinkedCaseIds(singletonList(randomUUID()))
                .withParentApplicationId(randomUUID())
                .withId(COURT_APPLICATIONS_ID)
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withApplicant(applicantRespondent()
                        .withFirstName(STRING.next())
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .withAddress(APPLICANT_ADDRESS)
                        .build())
                .withRespondents(singletonList(applicantRespondent()
                        .withFirstName(STRING.next())
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .withAddress(RESPONDENT_ADDRESS)
                        .build()))
                .build());
    }

    private List<uk.gov.justice.listing.events.CourtApplication> createCourtApplicationsWithSubject() {
        return singletonList(courtApplication()
                .withId(COURT_APPLICATIONS_ID)
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withApplicant(applicantRespondent()
                        .withFirstName(STRING.next())
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(APPLICANT_ID)
                        .withAddress(APPLICANT_ADDRESS)
                        .withRestrictFromCourtList(FALSE)
                        .build())
                .withSubject(applicantRespondent()
                        .withFirstName(STRING.next())
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(SUBJECT_ID)
                        .withMasterDefendantId(SUBJECT_MASTER_DEFENDANT_ID)
                        .withAddress(RESPONDENT_ADDRESS)
                        .withRestrictFromCourtList(FALSE)
                        .build())
                .withRespondents(singletonList(applicantRespondent()
                        .withFirstName(STRING.next())
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(RESPONDENT_ID_1)
                        .withMasterDefendantId(RESPONDENT_MASTER_DEFENDANT_ID_1)
                        .withAddress(RESPONDENT_ADDRESS)
                        .withRestrictFromCourtList(FALSE)
                        .build()))
                .build());
    }

    private List<uk.gov.justice.listing.events.CourtApplication> createCourtApplicationsWithMultipleRespondents() {
        return singletonList(courtApplication()
                .withId(COURT_APPLICATIONS_ID)
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withApplicant(applicantRespondent()
                        .withFirstName(STRING.next())
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(APPLICANT_ID)
                        .withAddress(APPLICANT_ADDRESS)
                        .withRestrictFromCourtList(FALSE)
                        .build())
                .withRespondents(asList(
                        applicantRespondent()
                                .withFirstName(STRING.next())
                                .withLastName(STRING.next())
                                .withIsRespondent(true)
                                .withId(RESPONDENT_ID_1)
                                .withMasterDefendantId(RESPONDENT_MASTER_DEFENDANT_ID_1)
                                .withAddress(RESPONDENT_ADDRESS)
                                .withRestrictFromCourtList(FALSE)
                                .build(),
                        applicantRespondent()
                                .withFirstName(STRING.next())
                                .withLastName(STRING.next())
                                .withIsRespondent(true)
                                .withId(RESPONDENT_ID_2)
                                .withMasterDefendantId(RESPONDENT_MASTER_DEFENDANT_ID_2)
                                .withAddress(RESPONDENT_ADDRESS)
                                .withRestrictFromCourtList(FALSE)
                                .build()
                ))
                .build());
    }

}
