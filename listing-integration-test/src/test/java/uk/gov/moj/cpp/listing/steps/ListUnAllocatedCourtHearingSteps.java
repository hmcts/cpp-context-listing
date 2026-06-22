package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.endpoint.UnallocatedHearingsEndpoint.pollForUnallocatedHearings;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.listing.courts.TypeOfList;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListUnAllocatedCourtHearingSteps extends ListCourtHearingSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListUnAllocatedCourtHearingSteps.class);
    private static final String LISTING_COMMAND_UNSCHEDULED_LIST_COURT_HEARING = "listing.command.list-unscheduled-court-hearing";
    private static final String MEDIA_TYPE_LIST_UNSCHEDULED_COURT_HEARING = "application/vnd.listing.command.list-unscheduled-court-hearing+json";


    public ListUnAllocatedCourtHearingSteps(final HearingsData hearingsData) {
        super(hearingsData);
    }

    public void whenCaseIsSubmittedForUnallocatedListing() {
        final Response response = getResponseCaseSubmittedForUnscheduledListing(false);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    private Response getResponseCaseSubmittedForUnscheduledListing(final boolean isStandaloneApp) {

        stubReferenceDataForFirstHearing();

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UNSCHEDULED_LIST_COURT_HEARING)));

        ListUnscheduledCourtHearing listCourtHearingData = null;
        if (isStandaloneApp) {
            listCourtHearingData = getListCourtHearingDataStandaloneApplication(getHearingsData());
        } else {
            listCourtHearingData = getListCourtHearingData(getHearingsData());
        }
        final JsonObject listCourtHearingJsonObject = (JsonObject) objectToJsonValueConverter.convert(listCourtHearingData);

        request = listCourtHearingJsonObject.toString();

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_UNSCHEDULED_COURT_HEARING,
                request, getLoggedInHeader());
    }

    protected ListUnscheduledCourtHearing getListCourtHearingData(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        return ListUnscheduledCourtHearing.listUnscheduledCourtHearing()
                .withHearings(asList(
                        HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                                .withTypeOfList(TypeOfList.typeOfList()
                                        .withId(randomUUID())
                                        .withDescription("Warrant for arrest without bail")
                                        .build())
                                .withCourtCentre(CourtCentre.courtCentre()
                                        .withId(hearingData.getCourtCentreId())
                                        .withName(hearingData.getName())
                                        .withRoomId(hearingData.getCourtRoomId())
                                        .build())
                                .withBookingReference(randomUUID())
                                .withCourtApplications(getCourtApplications(hearingData))
                                .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                                .withId(hearingData.getId())
                                .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                                .withEndDate(hearingData.getHearingEndDate() != null ? hearingData.getHearingEndDate().toString() : null)
                                .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                                .withJudiciary(getJudiciary(hearingData))
                                .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                                .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? null :
                                        WeekCommencingDate.weekCommencingDate()
                                                .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                                .withDuration(hearingData.getWeekCommencingDuration())
                                                .build())
                                .withProsecutionCases(convertToListedCases(hearingData, listedCaseData))
                                .withDefendantListingNeeds(hearingData.getListedCases().stream()
                                        .map(lc -> lc.getDefendants().stream().map(d ->
                                                DefendantListingNeeds.defendantListingNeeds()
                                                        .withDefendantId(d.getDefendantId())
                                                        .withProsecutionCaseId(lc.getCaseId())
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .flatMap(List::stream)
                                        .collect(Collectors.toList()))

                                .withType(HearingType.hearingType()
                                        .withDescription(hearingData.getHearingTypeData().getTypeDescription())
                                        .withWelshDescription(hearingData.getHearingTypeData().getWelshDescription())
                                        .withId(hearingData.getHearingTypeData().getTypeId())
                                        .build())
                                .withReportingRestrictionReason(hearingData.getReportingRestrictionReason())
                                .build()))
                .build();
    }


    private List<JudicialRole> getJudiciary(final HearingData hearingData) {
        return hearingData.getJudiciary() != null
                ? asList(JudicialRole.judicialRole()
                .withJudicialId(hearingData.getJudiciary().get(0).getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                        .build())
                .withIsDeputy(hearingData.getJudiciary().get(0).getIsDeputy().orElse(null))
                .withIsBenchChairman(hearingData.getJudiciary().get(0).getIsBenchChairman().orElse(null))
                .build())
                : null;
    }

    private List<ProsecutionCase> convertToListedCases(final HearingData hearingData, final ListedCaseData listedCaseData) {
        return hearingData.getListedCases().stream()
                .map(lc -> ProsecutionCase.prosecutionCase().withId(lc.getCaseId())
                        .withInitiationCode(InitiationCode.C)
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withProsecutionAuthorityCode(lc.getAuthorityCode())
                                .withProsecutionAuthorityId(lc.getAuthorityId())
                                .withProsecutionAuthorityReference(lc.getCaseReference())
                                .build())
                        .withCaseMarkers(asList(Marker.marker()
                                .withId(randomUUID())
                                .withMarkerTypeCode("C")
                                .withMarkerTypeDescription("Description")
                                .withMarkerTypeid(randomUUID()).build()))
                        .withDefendants(lc.getDefendants().stream().map(d -> Defendant.defendant()
                                .withId(d.getDefendantId())
                                .withMasterDefendantId(d.getMasterDefendantId())
                                .withCourtProceedingsInitiated(ItClock.nowUtc())
                                .withIsYouth(d.getIsYouth())
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withBailStatus(new BailStatus.Builder().withCode(d.getBailStatus().getCode()).withDescription(d.getBailStatus().getDescription()).withId(d.getBailStatus().getId()).build())
                                        .withPersonDetails(Person.person()
                                                .withTitle(PERSON_TITLE)
                                                .withNationalityId(randomUUID())
                                                .withNationalityDescription(PERSON_NATIONALITY_DESCRIPTION)
                                                .withAddress(Address.address()
                                                        .withAddress1(PERSON_ADDRESS_1)
                                                        .withAddress2(PERSON_ADDRESS_2)
                                                        .withAddress3(PERSON_ADDRESS_3)
                                                        .withAddress4(PERSON_ADDRESS_4)
                                                        .withAddress5(PERSON_ADDRESS_5)
                                                        .withPostcode(PERSON_POSTCODE)
                                                        .build())
                                                .withFirstName(d.getFirstName())
                                                .withLastName(d.getLastName())
                                                .withGender(Gender.MALE)
                                                .withAdditionalNationalityId(randomUUID())
                                                .withEthnicity(Ethnicity.ethnicity()
                                                        .withObservedEthnicityId(randomUUID())
                                                        .withObservedEthnicityDescription(STRING.next())
                                                        .build())
                                                .withDateOfBirth(ItClock.today().minusYears(21).toString())
                                                .build())
                                        .build())
                                .withAssociatedPersons(asList(AssociatedPerson.associatedPerson()
                                        .withRole(STRING.next())
                                        .withPerson(Person.person()
                                                .withAdditionalNationalityId(randomUUID())
                                                .withGender(Gender.FEMALE)
                                                .withLastName(d.getLastName())
                                                .withNationalityId(randomUUID())
                                                .withTitle(PERSON_TITLE)
                                                .withEthnicity(Ethnicity.ethnicity()
                                                        .withObservedEthnicityId(randomUUID())
                                                        .withObservedEthnicityDescription(STRING.next())
                                                        .build())
                                                .build())
                                        .build()))
                                .withOffences(d.getOffences().stream()
                                        .map(o -> Offence.offence()
                                                .withCount(INTEGER.next())
                                                .withId(o.getOffenceId())
                                                .withOffenceCode(STRING.next())
                                                .withOffenceDefinitionId(randomUUID())
                                                .withWording(STRING.next())
                                                .withStartDate(ItClock.today().toString())
                                                .withOrderIndex(INTEGER.next())
                                                .withOffenceTitle(o.getStatementOfOffenceTitle())
                                                .withLaaApplnReference(
                                                        LaaReference.laaReference()
                                                                .withApplicationReference(STRING.next())
                                                                .withStatusCode(STRING.next())
                                                                .withStatusDate((format(ItClock.today().toString())))
                                                                .withStatusDescription(STRING.next())
                                                                .withStatusId(randomUUID()).build())
                                                .build())
                                        .collect(Collectors.toList()))
                                .withProsecutionCaseId(listedCaseData.getCaseId())
                                .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<CourtApplication> getCourtApplications(final HearingData hearingData) {
        CourtApplicationParty applicant = CourtApplicationParty.courtApplicationParty()
                .withId(hearingData.getCourtApplications().get(0).getApplicant().getId())
                .withPersonDetails(Person.person().withLastName(hearingData.getCourtApplications().get(0).getApplicant().getLastName())
                        .withFirstName(hearingData.getCourtApplications().get(0).getApplicant().getFirstName())
                        .withGender(Gender.FEMALE)
                        .build())
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();
        return asList(CourtApplication.courtApplication()
                .withId(hearingData.getCourtApplications().get(0).getId())
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(hearingData.getCourtApplications().get(0).getLinkedCaseId())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(STRING.next())
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode(STRING.next()).build())
                        .withIsSJP(false)
                        .withCaseStatus("ACTIVE")
                .build()))
                .withParentApplicationId(hearingData.getCourtApplications().get(0).getParentApplicationId())
                .withType(CourtApplicationType.courtApplicationType()
                        .withId(randomUUID())
                        .withCode(STRING.next())
                        .withType(hearingData.getCourtApplications().get(0).getType())
                        .withLegislation(STRING.next())
                        .withCategoryCode(STRING.next())
                        .withLinkType(LinkType.LINKED)
                        .withJurisdiction(Jurisdiction.CROWN)
                        .withSummonsTemplateType(SummonsTemplateType.GENERIC_APPLICATION)
                        .withBreachType(BreachType.GENERIC_BREACH)
                        .withAppealFlag(false)
                        .withApplicantAppellantFlag(false)
                        .withPleaApplicableFlag(false)
                        .withCommrOfOathFlag(false)
                        .withCourtOfAppealFlag(false)
                        .withCourtExtractAvlFlag(false)
                        .withProsecutorThirdPartyFlag(false)
                        .withSpiOutApplicableFlag(false)
                        .withOffenceActiveOrder(OffenceActiveOrder.COURT_ORDER)
                        .build())
                .withApplicationReceivedDate(ItClock.today().toString())
                .withApplicationReference(STRING.next())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(applicant)
                .withRespondents(asList(
                        CourtApplicationParty.courtApplicationParty()
                                .withId(hearingData.getCourtApplications().get(0).getRespondent().getId())
                                .withPersonDetails(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                        .withFirstName(hearingData.getCourtApplications().get(0).getRespondent().getFirstName())
                                        .withGender(Gender.FEMALE)
                                        .build())
                                .withSummonsRequired(false)
                                .withNotificationRequired(false)
                                .build()))
                .withSubject(applicant)
                .build());
    }

    private ListUnscheduledCourtHearing getListCourtHearingDataStandaloneApplication(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final CourtApplicationParty applicant = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withPersonDetails(Person.person().withLastName(hearingData.getCourtApplications().get(0).getApplicant().getLastName())
                        .withFirstName(hearingData.getCourtApplications().get(0).getApplicant().getFirstName())
                        .withGender(Gender.FEMALE)
                        .build())
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        return ListUnscheduledCourtHearing.listUnscheduledCourtHearing()
                .withHearings(asList(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withTypeOfList(TypeOfList.typeOfList()
                                .withId(randomUUID())
                                .withDescription("Warrant for arrest without bail")
                                .build())
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(hearingData.getCourtRoomId())
                                .build())
                        .withCourtApplications(asList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withParentApplicationId(hearingData.getCourtApplications().get(0).getParentApplicationId())
                                .withType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .withCode(STRING.next())
                                        .withType(hearingData.getCourtApplications().get(0).getType())
                                        .withLegislation(STRING.next())
                                        .withCategoryCode(STRING.next())
                                        .withLinkType(LinkType.STANDALONE)
                                        .withJurisdiction(Jurisdiction.MAGISTRATES)
                                        .withSummonsTemplateType(SummonsTemplateType.GENERIC_APPLICATION)
                                        .withBreachType(BreachType.GENERIC_BREACH)
                                        .withAppealFlag(false)
                                        .withApplicantAppellantFlag(false)
                                        .withPleaApplicableFlag(false)
                                        .withCommrOfOathFlag(false)
                                        .withCourtOfAppealFlag(false)
                                        .withCourtExtractAvlFlag(false)
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSpiOutApplicableFlag(false)
                                        .withOffenceActiveOrder(OffenceActiveOrder.COURT_ORDER)
                                        .build())
                                .withApplicationReceivedDate(ItClock.today().toString())
                                .withApplicationReference(STRING.next())
                                .withApplicationStatus(ApplicationStatus.DRAFT)
                                .withApplicant(applicant)
                                .withRespondents(asList(CourtApplicationParty.courtApplicationParty()
                                        .withId(randomUUID())
                                        .withPersonDetails(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                                .withFirstName((hearingData.getCourtApplications().get(0).getRespondent().getFirstName()))
                                                .withGender(Gender.FEMALE)
                                                .build())
                                        .withNotificationRequired(false)
                                        .withSummonsRequired(false)
                                        .build()))
                                .withSubject(applicant)
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                        .withEndDate(hearingData.getHearingEndDate() != null ? hearingData.getHearingEndDate().toString() : null)
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withType(HearingType.hearingType()
                                .withDescription(hearingData.getHearingTypeData().getTypeDescription())
                                .withWelshDescription(hearingData.getHearingTypeData().getWelshDescription())
                                .withId(hearingData.getHearingTypeData().getTypeId())
                                .build())
                        .withReportingRestrictionReason(hearingData.getReportingRestrictionReason())
                        .build()))
                .build();
    }

    public void verifyHearingUnallocatededFromAPI() {
        final HearingData hearingData = getHearingsData().getHearingData().get(0);

        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());


        final Matcher<ReadContext> unallocateddHearingVerifiedMatcher = allOf(
                withJsonPath(lastNameFilter),
                withJsonPath(caseReferenceFilter),
                withJsonPath("$.hearings[0].id",
                        equalTo(hearingData.getId().toString())),
                withJsonPath("$.hearings[0].jurisdictionType",
                        equalTo(hearingData.getJurisdictionType())),
                withJsonPath("$.hearings[0].courtCentreId",
                        equalTo(hearingData.getCourtCentreId().toString())),
                withJsonPath("$.hearings[0].type.id",
                        equalTo(hearingData.getHearingTypeData().getTypeId().toString())),
                withJsonPath("$.hearings[0].type.description",
                        equalTo(hearingData.getHearingTypeData().getTypeDescription())),
                withJsonPath("$.hearings[0].startDate",
                        equalTo(hearingData.getHearingStartDate().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].applicationType",
                        equalTo(hearingData.getCourtApplications().get(0).getType())),
                withJsonPath("$.hearings[0].courtApplications[0].id",
                        equalTo(hearingData.getCourtApplications().get(0).getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].linkedCaseIds[0]",
                        equalTo(hearingData.getCourtApplications().get(0).getLinkedCaseId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                        equalTo(hearingData.getCourtApplications().get(0).getParentApplicationId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getLastName())),
                withJsonPath("$.hearings[0].courtApplications[0].applicant.firstName",
                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getFirstName())),
                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getFirstName())),
                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].lastName",
                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getLastName())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].isYouth",
                        equalTo(true)),
                withJsonPath("$.hearings[0].ampPublicDataLastUpdated",
                        value -> ZonedDateTime.parse(value.toString()).toLocalDate().equals(ItClock.today()))

        );
        pollForUnallocatedHearings(getLoggedInUser(), hearingData, unallocateddHearingVerifiedMatcher);
    }


}
