package uk.gov.moj.cpp.listing.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.ApplicantRespondent.applicantRespondent;
import static uk.gov.justice.listing.events.CourtApplication.courtApplication;
import static uk.gov.justice.listing.events.CourtListRestricted.courtListRestricted;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.Address;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
    private static final String EVENT_NAME = "listing.events.court-list-restricted";

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
                .withCourtApplicationType(Optional.empty())
                .build();
        final Envelope<CourtListRestricted> restrictCourtListEnvelope = envelopeFrom(metadataWithRandomUUID(EVENT_NAME), restrictCourtList);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(LISTED_CASES)).willReturn(testCasesProperties);

        target.hearingRestrictionForCourt(restrictCourtListEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        verify(hearingRepository).save(hearing);
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
                .withCourtApplicationType(ofNullable(COURT_APPLICATION_TYPE))
                .build();
        final Envelope<CourtListRestricted> restrictCourtListEnvelope = envelopeFrom(metadataWithRandomUUID(EVENT_NAME), restrictCourtList);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(hearing.getProperties()).willReturn(properties);
        given(properties.get(COURT_APPLICATIONS_FIELD)).willReturn(testCasesProperties);

        target.hearingRestrictionForCourt(restrictCourtListEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
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
                .withCourtApplicationType(empty())
                .build();
        final Envelope<CourtListRestricted> restrictCourtListEnvelope = envelopeFrom(metadataWithRandomUUID(EVENT_NAME), payload);
        final Hearing hearing = new Hearing(payload.getHearingId(), properties);

        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);
        given(properties.get(COURT_APPLICATIONS_FIELD)).willReturn(courtApplicationsProperties);

        target.hearingRestrictionForCourt(restrictCourtListEnvelope);

        verify(properties).replace(anyObject(), objectNodeCaptor.capture());
        final ArrayNode applicationArrayNode = objectNodeCaptor.getValue();
        final List<Matcher<? super ReadContext>> matchers = newArrayList();
        matchers.add(withJsonPath("$", hasSize(1)));
        matchers.addAll(getApplicantMatchers(expectedCourtApplication));
        matchers.addAll(getCourtApplicationMatchers(expectedCourtApplication));
        matchers.addAll(getRespondentMatchersForRestricted(expectedCourtApplication));
        assertThat(applicationArrayNode.toString(), isJson(allOf(matchers)));

        verify(hearingRepository).save(hearing);
    }

    private Collection<? extends Matcher<? super ReadContext>> getCourtApplicationMatchers(final CourtApplication courtApplication) {
        return newArrayList(
                withJsonPath("$[0].id", equalTo(courtApplication.getId().toString())),
                withJsonPath("$[0].applicationType", equalTo(courtApplication.getApplicationType())),
                withJsonPath("$[0].applicationParticulars", equalTo(courtApplication.getApplicationParticulars().orElse(null)))
        );
    }

    private Collection<? extends Matcher<? super ReadContext>> getApplicantMatchers(final CourtApplication courtApplication) {
        return newArrayList(
                withJsonPath("$[0].applicant.id", equalTo(courtApplication.getApplicant().getId().toString())),
                withJsonPath("$[0].applicant.firstName", equalTo(courtApplication.getApplicant().getFirstName().orElse(null))),
                withJsonPath("$[0].applicant.lastName", equalTo(courtApplication.getApplicant().getLastName())),
                withJsonPath("$[0].applicant.isRespondent", equalTo(courtApplication.getApplicant().getIsRespondent())),
                withJsonPath("$[0].applicant.restrictFromCourtList", equalTo(courtApplication.getApplicant().getRestrictFromCourtList().orElse(null))),
                withJsonPath("$[0].applicant.address.address1", equalTo(APPLICANT_ADDRESS.getAddress1())),
                withJsonPath("$[0].applicant.address.address2", equalTo(APPLICANT_ADDRESS.getAddress2().orElse(null))),
                withJsonPath("$[0].applicant.address.address3", equalTo(APPLICANT_ADDRESS.getAddress3().orElse(null))),
                withJsonPath("$[0].applicant.address.address4", equalTo(APPLICANT_ADDRESS.getAddress4().orElse(null))),
                withJsonPath("$[0].applicant.address.address5", equalTo(APPLICANT_ADDRESS.getAddress5().orElse(null))),
                withJsonPath("$[0].applicant.address.postcode", equalTo(APPLICANT_ADDRESS.getPostcode().orElse(null)))
        );
    }

    private Collection<? extends Matcher<? super ReadContext>> getRespondentMatchersForRestricted(final CourtApplication courtApplication) {
        return newArrayList(
                withJsonPath("$[0].respondents", hasSize(2)),

                withJsonPath("$[0].respondents[0].id", equalTo(courtApplication.getRespondents().get(0).getId().toString())),
                withJsonPath("$[0].respondents[0].firstName", equalTo(courtApplication.getRespondents().get(0).getFirstName().orElse(null))),
                withJsonPath("$[0].respondents[0].lastName", equalTo(courtApplication.getRespondents().get(0).getLastName())),
                withJsonPath("$[0].respondents[0].isRespondent", equalTo(courtApplication.getRespondents().get(0).getIsRespondent())),
                withJsonPath("$[0].respondents[0].restrictFromCourtList", equalTo(true)),
                withJsonPath("$[0].respondents[0].address.address1", equalTo(RESPONDENT_ADDRESS.getAddress1())),
                withJsonPath("$[0].respondents[0].address.address2", equalTo(RESPONDENT_ADDRESS.getAddress2().orElse(null))),
                withJsonPath("$[0].respondents[0].address.address3", equalTo(RESPONDENT_ADDRESS.getAddress3().orElse(null))),
                withJsonPath("$[0].respondents[0].address.address4", equalTo(RESPONDENT_ADDRESS.getAddress4().orElse(null))),
                withJsonPath("$[0].respondents[0].address.address5", equalTo(RESPONDENT_ADDRESS.getAddress5().orElse(null))),
                withJsonPath("$[0].respondents[0].address.postcode", equalTo(RESPONDENT_ADDRESS.getPostcode().orElse(null))),

                withJsonPath("$[0].respondents[1].id", equalTo(courtApplication.getRespondents().get(1).getId().toString())),
                withJsonPath("$[0].respondents[1].firstName", equalTo(courtApplication.getRespondents().get(1).getFirstName().orElse(null))),
                withJsonPath("$[0].respondents[1].lastName", equalTo(courtApplication.getRespondents().get(1).getLastName())),
                withJsonPath("$[0].respondents[1].isRespondent", equalTo(courtApplication.getRespondents().get(1).getIsRespondent())),
                withJsonPath("$[0].respondents[1].restrictFromCourtList", equalTo(courtApplication.getRespondents().get(1).getRestrictFromCourtList().orElse(null))),
                withJsonPath("$[0].respondents[1].address.address1", equalTo(RESPONDENT_ADDRESS.getAddress1())),
                withJsonPath("$[0].respondents[1].address.address2", equalTo(RESPONDENT_ADDRESS.getAddress2().orElse(null))),
                withJsonPath("$[0].respondents[1].address.address3", equalTo(RESPONDENT_ADDRESS.getAddress3().orElse(null))),
                withJsonPath("$[0].respondents[1].address.address4", equalTo(RESPONDENT_ADDRESS.getAddress4().orElse(null))),
                withJsonPath("$[0].respondents[1].address.address5", equalTo(RESPONDENT_ADDRESS.getAddress5().orElse(null))),
                withJsonPath("$[0].respondents[1].address.postcode", equalTo(RESPONDENT_ADDRESS.getPostcode().orElse(null)))
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
        assertThat(actualAddress.get("address2").asText(), equalTo(expectedAddress.getAddress2().orElse(null)));
        assertThat(actualAddress.get("address3").asText(), equalTo(expectedAddress.getAddress3().orElse(null)));
        assertThat(actualAddress.get("address4").asText(), equalTo(expectedAddress.getAddress4().orElse(null)));
        assertThat(actualAddress.get("address5").asText(), equalTo(expectedAddress.getAddress5().orElse(null)));
        assertThat(actualAddress.get("postcode").asText(), equalTo(expectedAddress.getPostcode().orElse(null)));
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
                                .withShadowListed(Optional.of(Boolean.FALSE))
                                .withStartDate(LocalDates.to(LocalDate.now()))
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withTitle(STRING.next())
                                        .build())
                                .build()))
                        .build()))
                .withId(UUID.randomUUID())
                .withShadowListed(Optional.of(Boolean.FALSE))
                .build());
    }

    private List<uk.gov.justice.listing.events.CourtApplication> createCourtApplications() {
        return singletonList(courtApplication()
                .withLinkedCaseIds(singletonList(randomUUID()))
                .withParentApplicationId(of(randomUUID()))
                .withId(COURT_APPLICATIONS_ID)
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(of(APPLICATION_PARTICULARS))
                .withApplicant(applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(randomUUID())
                        .withAddress(of(APPLICANT_ADDRESS))
                        .build())
                .withRespondents(singletonList(applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(true)
                        .withId(randomUUID())
                        .withAddress(of(RESPONDENT_ADDRESS))
                        .build()))
                .build());
    }

    private List<uk.gov.justice.listing.events.CourtApplication> createCourtApplicationsWithMultipleRespondents() {
        return singletonList(courtApplication()
                .withId(COURT_APPLICATIONS_ID)
                .withApplicationType(COURT_APPLICATION_TYPE)
                .withApplicationParticulars(of(APPLICATION_PARTICULARS))
                .withApplicant(applicantRespondent()
                        .withFirstName(of(STRING.next()))
                        .withLastName(STRING.next())
                        .withIsRespondent(false)
                        .withId(APPLICANT_ID)
                        .withAddress(of(APPLICANT_ADDRESS))
                        .withRestrictFromCourtList(of(FALSE))
                        .build())
                .withRespondents(asList(
                        applicantRespondent()
                                .withFirstName(of(STRING.next()))
                                .withLastName(STRING.next())
                                .withIsRespondent(true)
                                .withId(RESPONDENT_ID_1)
                                .withAddress(of(RESPONDENT_ADDRESS))
                                .withRestrictFromCourtList(of(FALSE))
                                .build(),
                        applicantRespondent()
                                .withFirstName(of(STRING.next()))
                                .withLastName(STRING.next())
                                .withIsRespondent(true)
                                .withId(RESPONDENT_ID_2)
                                .withAddress(of(RESPONDENT_ADDRESS))
                                .withRestrictFromCourtList(of(FALSE))
                                .build()
                ))
                .build());
    }

}
