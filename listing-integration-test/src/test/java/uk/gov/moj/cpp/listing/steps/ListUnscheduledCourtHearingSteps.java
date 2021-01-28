package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.endpoint.UnscheduledHearingsEndpoint.pollForUnscheduledHearings;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.listing.courts.ApplicationJurisdictionType;
import uk.gov.justice.listing.courts.ApplicationStatus;
import uk.gov.justice.listing.courts.Gender;
import uk.gov.justice.listing.courts.InitiationCode;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.courts.LinkType;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.listing.courts.TypeOfList;
import uk.gov.justice.listing.courts.WeekCommencingDate;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListUnscheduledCourtHearingSteps extends ListCourtHearingSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListUnscheduledCourtHearingSteps.class);
    private static final String LISTING_COMMAND_UNSCHEDULED_LIST_COURT_HEARING = "listing.command.list-unscheduled-court-hearing";
    private static final String MEDIA_TYPE_LIST_UNSCHEDULED_COURT_HEARING = "application/vnd.listing.command.list-unscheduled-court-hearing+json";


    public ListUnscheduledCourtHearingSteps(final HearingsData hearingsData) {
        super(hearingsData);
    }


    public void whenCaseIsSubmittedForUnscheduledListing() {
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
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_LIST_UNSCHEDULED_COURT_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_UNSCHEDULED_COURT_HEARING,
                request, getLoggedInHeader());
    }



    public void verifyHearingUnscheduledListedFromAPI() {
        final HearingData hearingData = getHearingsData().getHearingData().get(0);

        final UUID courtCentreId = getHearingsData().getHearingData().get(0).getCourtCentreId();

        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        final Matcher<ReadContext> unscheduledHearingVerifiedMatcher = allOf(
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
                withJsonPath("$.hearings[0].courtApplications[0].linkedCaseId",
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
                        equalTo(true))
        );

        pollForUnscheduledHearings(getLoggedInUser(), courtCentreId, unscheduledHearingVerifiedMatcher);
    }


    protected ListUnscheduledCourtHearing getListCourtHearingData(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        //   List<DefendantData> defendantData = listedCaseData.getDefendants();

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
                                        .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                        .build())
                                .withBookingReference(of(randomUUID()))
                                .withCourtApplications(getCourtApplications(hearingData))
                                .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                                .withId(hearingData.getId())
                                .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? of(hearingData.getHearingStartTime()) : Optional.empty())
                                .withEndDate(hearingData.getHearingEndDate() != null ? of(hearingData.getHearingEndDate().toString()) : Optional.empty())
                                .withEstimatedMinutes(of(hearingData.getHearingEstimateMinutes()))
                                .withJudiciary(getJudiciary(hearingData))
                                .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                                .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? Optional.empty() :
                                        of(WeekCommencingDate.weekCommencingDate()
                                                .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                                .withDuration(of(hearingData.getWeekCommencingDuration()))
                                                .build()))
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
                                .withReportingRestrictionReason(of(hearingData.getReportingRestrictionReason()))
                                .build()))
                .build();
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
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withIsYouth(ofNullable(d.getIsYouth()))
                                .withPersonDefendant(of(PersonDefendant.personDefendant()
                                        .withBailStatus(of(new BailStatus.Builder().withCode(d.getBailStatus().getCode()).withDescription(d.getBailStatus().getDescription()).withId(d.getBailStatus().getId()).build()))
                                        .withPersonDetails(Person.person()
                                                .withTitle(of(PERSON_TITLE))
                                                .withNationalityId(of(randomUUID()))
                                                .withNationalityDescription(of(PERSON_NATIONALITY_DESCRIPTION))
                                                .withAddress(of(Address.address()
                                                        .withAddress1(PERSON_ADDRESS_1)
                                                        .withAddress2(of(PERSON_ADDRESS_2))
                                                        .withAddress3(of(PERSON_ADDRESS_3))
                                                        .withAddress4(of(PERSON_ADDRESS_4))
                                                        .withAddress5(of(PERSON_ADDRESS_5))
                                                        .withPostcode(of(PERSON_POSTCODE))
                                                        .build()))
                                                .withFirstName(of(d.getFirstName()))
                                                .withLastName(d.getLastName())
                                                .withGender(Gender.MALE)
                                                .withAdditionalNationalityId(of(randomUUID()))
                                                .withEthnicity(of(Ethnicity.ethnicity()
                                                        .withObservedEthnicityId(of(randomUUID()))
                                                        .withObservedEthnicityDescription(of(STRING.next()))
                                                        .build()))
                                                .withDateOfBirth(of(LocalDate.now().minusYears(21).toString()))
                                                .build())
                                        .build()))
                                .withAssociatedPersons(asList(AssociatedPerson.associatedPerson()
                                        .withRole(of(STRING.next()))
                                        .withPerson(Person.person()
                                                .withAdditionalNationalityId(of(randomUUID()))
                                                .withGender(Gender.FEMALE)
                                                .withLastName(d.getLastName())
                                                .withNationalityId(of(randomUUID()))
                                                .withTitle(of(PERSON_TITLE))
                                                .withEthnicity(of(Ethnicity.ethnicity()
                                                        .withObservedEthnicityId(of(randomUUID()))
                                                        .withObservedEthnicityDescription(of(STRING.next()))
                                                        .build()))
                                                .build())
                                        .build()))
                                .withOffences(d.getOffences().stream()
                                        .map(o -> Offence.offence()
                                                .withCount(of(INTEGER.next()))
                                                .withId(o.getOffenceId())
                                                .withOffenceCode(STRING.next())
                                                .withOffenceDefinitionId(randomUUID())
                                                .withWording(STRING.next())
                                                .withStartDate(LocalDate.now().toString())
                                                .withOrderIndex(of(INTEGER.next()))
                                                .withOffenceTitle(o.getStatementOfOffenceTitle())
                                                .withLaaApplnReference(of(
                                                        LaaReference.laaReference()
                                                                .withApplicationReference(STRING.next())
                                                                .withStatusCode(STRING.next())
                                                                .withStatusDate((format(LocalDate.now().toString())))
                                                                .withStatusDescription(STRING.next())
                                                                .withStatusId(randomUUID()).build()))
                                                .build())
                                        .collect(Collectors.toList()))
                                .withProsecutionCaseId(listedCaseData.getCaseId())
                                .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<JudicialRole> getJudiciary(final HearingData hearingData) {
        return hearingData.getJudiciary() != null
                ? asList(JudicialRole.judicialRole()
                .withJudicialId(hearingData.getJudiciary().get(0).getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudicialRoleTypeId())
                        .build())
                .withIsDeputy(hearingData.getJudiciary().get(0).getIsDeputy())
                .withIsBenchChairman(hearingData.getJudiciary().get(0).getIsBenchChairman())
                .build())
                : null;
    }

    private List<CourtApplication> getCourtApplications(final HearingData hearingData) {
        return asList(CourtApplication.courtApplication()
                .withId(hearingData.getCourtApplications().get(0).getId())
                .withLinkedCaseId(of(hearingData.getCourtApplications().get(0).getLinkedCaseId()))
                .withParentApplicationId(of(hearingData.getCourtApplications().get(0).getParentApplicationId()))
                .withType(CourtApplicationType.courtApplicationType()
                        .withId(randomUUID())
                        .withApplicationCode(of(STRING.next()))
                        .withApplicationType(hearingData.getCourtApplications().get(0).getType())
                        .withApplicationLegislation(of(STRING.next()))
                        .withApplicationCategory(STRING.next())
                        .withLinkType(LinkType.EITHER)
                        .withApplicationJurisdictionType(ApplicationJurisdictionType.CROWN)
                        .build())
                .withApplicationReceivedDate(LocalDate.now().toString())
                .withApplicationReference(of(STRING.next()))
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(hearingData.getCourtApplications().get(0).getApplicant().getId())
                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getApplicant().getLastName())
                                .withFirstName(of(hearingData.getCourtApplications().get(0).getApplicant().getFirstName()))
                                .withGender(Gender.FEMALE)
                                .build()))
                        .build())
                .withRespondents(asList(
                        CourtApplicationRespondent.courtApplicationRespondent()
                                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                        .withId(hearingData.getCourtApplications().get(0).getRespondent().getId())
                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getRespondent().getFirstName()))
                                                .withGender(Gender.FEMALE)
                                                .build()))
                                        .build())
                                .build()))
                .build());
    }

    private ListUnscheduledCourtHearing getListCourtHearingDataStandaloneApplication(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);

        return ListUnscheduledCourtHearing.listUnscheduledCourtHearing()
                .withHearings(asList(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withTypeOfList(TypeOfList.typeOfList()
                                .withId(randomUUID())
                                .withDescription("Warrant for arrest without bail")
                                .build())
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withCourtApplications(asList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withParentApplicationId(of(hearingData.getCourtApplications().get(0).getParentApplicationId()))
                                .withType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .withApplicationCode(Optional.of(STRING.next()))
                                        .withApplicationType(hearingData.getCourtApplications().get(0).getType())
                                        .withApplicationLegislation(Optional.of(STRING.next()))
                                        .withApplicationCategory(STRING.next())
                                        .withLinkType(LinkType.STANDALONE)
                                        .withApplicationJurisdictionType(ApplicationJurisdictionType.MAGISTRATES)
                                        .build())
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withApplicationReference(Optional.of(STRING.next()))
                                .withApplicationStatus(ApplicationStatus.DRAFT)
                                .withApplicant(CourtApplicationParty.courtApplicationParty()
                                        .withId(randomUUID())
                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getApplicant().getLastName())
                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getApplicant().getFirstName()))
                                                .withGender(Gender.FEMALE)
                                                .build()))
                                        .build())
                                .withRespondents(asList(
                                        CourtApplicationRespondent.courtApplicationRespondent()
                                                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                                        .withId(randomUUID())
                                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getRespondent().getFirstName()))
                                                                .withGender(Gender.FEMALE)
                                                                .build()))
                                                        .build())
                                                .build()))
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? of(hearingData.getHearingStartTime()) : Optional.empty())
                        .withEndDate(hearingData.getHearingEndDate() != null ? of(hearingData.getHearingEndDate().toString()) : Optional.empty())
                        .withEstimatedMinutes(Optional.of(hearingData.getHearingEstimateMinutes()))
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withType(HearingType.hearingType()
                                .withDescription(hearingData.getHearingTypeData().getTypeDescription())
                                .withId(hearingData.getHearingTypeData().getTypeId())
                                .build())
                        .withReportingRestrictionReason(of(hearingData.getReportingRestrictionReason()))
                        .build()))
                .build();
    }

}
