package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.listing.courts.BailStatus;
import uk.gov.justice.listing.courts.Gender;
import uk.gov.justice.listing.courts.InitiationCode;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.courts.SendCaseForListing;
import uk.gov.justice.listing.courts.Title;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SendCaseForListingSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendCaseForListingSteps.class);

    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command" +
            ".send-case-for-listing";

    private static final String MEDIA_TYPE_SEND_CASE_FOR_LISTING = "application/vnd.listing" +
            ".command.send-case-for-listing+json";

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";


    private static final String EVENT_SELECTOR_HEARING_LISTED = "listing.events.hearing-listed";
    private static final String EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    private static final String EVENT_SELECTOR_HEARING_DAYS_CHANGED = "listing.events.hearing-days-changed-for-hearing";


    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);

    private static final boolean UNALLOCATED = false;


    private MessageConsumer privateMessageConsumerHearingListed;
    private MessageConsumer privateMessageConsumerHearingAllocatedForListing;
    private MessageConsumer privateMessageConsumerHearingDaysChanged;


    private String request;


    private final HearingsData hearingsData;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);


    public SendCaseForListingSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        privateMessageConsumerHearingListed = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_LISTED);
        privateMessageConsumerHearingAllocatedForListing = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING);
        privateMessageConsumerHearingDaysChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_DAYS_CHANGED);

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }


    public void whenCaseIsSubmittedForListing() {
        final Response response = getResponseCaseSubmittedForListing();
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenCaseIsSubmittedForListingByUnauthorisedUser() {
        final Response response = getResponseCaseSubmittedForListing();
        assertThat(response.getStatus(), equalTo(SC_FORBIDDEN));
    }

    private Response getResponseCaseSubmittedForListing() {
        hearingsData.getHearingData().stream()
                .map(hearingData -> hearingData.getCourtCentreId())
                .forEach(cci -> stubGetReferenceDataCourtCentre(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, null)));


        final String listCaseForHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_SEND_CASE_FOR_LISTING)));

        SendCaseForListing sendCaseForListingData = getSendCaseForListingData(hearingsData);
        final JsonObject sendCaseForListingJsonObject = (JsonObject) objectToJsonValueConverter.convert(sendCaseForListingData);


        request = sendCaseForListingJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_SEND_CASE_FOR_LISTING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_SEND_CASE_FOR_LISTING,
                request, getLoggedInHeader());
    }


    public void verifyHearingListedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingListed);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearing.id"), is(jsRequest.getString("hearings[0].id")));
        assertThat(jsonResponse.get("hearing.hearingLanguage"), is("ENGLISH"));
        assertThat(jsonResponse.get("hearing.listedCases[0].id"), is(jsRequest.getString("hearings[0].prosecutionCases[0].id")));
        assertThat(jsonResponse.get("hearing.listedCases[0].defendants[0].id"), is(jsRequest.getString("hearings[0].prosecutionCases[0].defendants[0].id")));
    }


    public void verifyHearingAllocatedForListingInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingAllocatedForListing);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingAllocatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(jsRequest.getString("hearings[0].id")));
    }


    public void verifyHearingListedFromAPI(boolean isAllocated) {
        HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));


        ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());


        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
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
                                        equalTo(hearingData.getHearingStartDate().toString()))

                        )));
    }


    private static com.jayway.jsonpath.JsonPath getJsonPathQueryForDefendantLastName(HearingData hearing, ListedCaseData listedCase, DefendantData defendant, String expectedLastName) {
        SendCaseForListingSteps.HearingDefendantFilter hearingDefendantFilter = new SendCaseForListingSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        Filter defendantFilter = hearingDefendantFilter.getDefendantFilter();
        final Filter firstNameFilter = filter(
                where("lastName").eq(expectedLastName)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].defendants[?][?]", hearingFilter, listingCaseFilter, defendantFilter, firstNameFilter);
    }

    private static com.jayway.jsonpath.JsonPath getJsonPathQueryForCaseReference(HearingData hearing, ListedCaseData listedCase, DefendantData defendant, String expectedCaseReference) {
        SendCaseForListingSteps.HearingDefendantFilter hearingDefendantFilter = new SendCaseForListingSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        final Filter caseReferenceFilter = filter(
                where("caseReference").eq(expectedCaseReference)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].caseIdentifier.[?]", hearingFilter, listingCaseFilter, caseReferenceFilter);
    }


    private SendCaseForListing getSendCaseForListingData(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        //   List<DefendantData> defendantData = listedCaseData.getDefendants();

        return SendCaseForListing.sendCaseForListing()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime())
                        .withEndDate(hearingData.getHearingEndDate() != null ? of(hearingData.getHearingEndDate().toString()) : Optional.empty())
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(hearingData.getJudiciary() != null
                                ? Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(hearingData.getJudiciary().get(0).getJudicialId())
                                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())
                                        .withJudicialRoleTypeId(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudicialRoleTypeId())
                                        .build())
                                .withIsDeputy(hearingData.getJudiciary().get(0).getIsDeputy())
                                .withIsBenchChairman(hearingData.getJudiciary().get(0).getIsBenchChairman())
                                .build())
                                : null)
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withProsecutionCases(hearingData.getListedCases().stream()
                                .map(lc -> ProsecutionCase.prosecutionCase().withId(lc.getCaseId())
                                        .withInitiationCode(InitiationCode.C)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityCode(lc.getAuthorityCode())
                                                .withProsecutionAuthorityId(lc.getAuthorityId())
                                                .withProsecutionAuthorityReference(lc.getCaseReference())
                                                .build())
                                        .withDefendants(lc.getDefendants().stream().map(d -> Defendant.defendant()
                                                .withId(d.getDefendantId())
                                                .withPersonDefendant(of(PersonDefendant.personDefendant()
                                                        .withAliases(Arrays.asList(
                                                                STRING.next(), STRING.next()))
                                                        .withBailStatus(BailStatus.valueFor(d.getBailStatus()))
                                                        .withObservedEthnicityId(of(UUID.randomUUID()))
                                                        .withSelfDefinedEthnicityId(of(UUID.randomUUID()))
                                                        .withPersonDetails(Person.person()
                                                                .withTitle(of(Title.MISS))
                                                                .withNationalityId(of(UUID.randomUUID()))
                                                                .withFirstName(of(d.getFirstName()))
                                                                .withLastName(d.getLastName())
                                                                .withGender(Gender.MALE)
                                                                .withEthnicityId(of(UUID.randomUUID()))
                                                                .withAdditionalNationalityId(of(UUID.randomUUID()))
                                                                .withDateOfBirth(of(LocalDate.now().minusYears(21).toString()))
                                                                .build())
                                                        .build()))
                                                .withAssociatedPersons(Arrays.asList(AssociatedPerson.associatedPerson()
                                                        .withRole(STRING.next())
                                                        .withPerson(Person.person()
                                                                .withAdditionalNationalityId(of(UUID.randomUUID()))
                                                                .withEthnicityId(of(UUID.randomUUID()))
                                                                .withGender(Gender.FEMALE)
                                                                .withLastName(d.getLastName())
                                                                .withNationalityId(of(UUID.randomUUID()))
                                                                .withTitle(of(Title.MISS))
                                                                .build())
                                                        .build()))
                                                .withOffences(d.getOffences().stream()
                                                        .map(o -> Offence.offence()
                                                                .withCount(INTEGER.next())
                                                                .withId(o.getOffenceId())
                                                                .withOffenceCode(STRING.next())
                                                                .withOffenceDefinitionId(UUID.randomUUID())
                                                                .withWording(STRING.next())
                                                                .withStartDate(LocalDate.now().toString())
                                                                .withOrderIndex(of(INTEGER.next()))
                                                                .withOffenceTitle(o.getStatementOfOffenceTitle())
                                                                .build())
                                                        .collect(Collectors.toList()))
                                                .withProsecutionCaseId(listedCaseData.getCaseId())
                                                .build())
                                                .collect(Collectors.toList()))

                                        .build())
                                .collect(Collectors.toList()))

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
                                .withId(hearingData.getHearingTypeData().getTypeId())
                                .build())
                        .withReportingRestrictionReason(of(hearingData.getReportingRestrictionReason()))
                        .build()))
                .build();
    }

    private static class HearingDefendantFilter {
        private HearingData hearing;
        private DefendantData defendant;
        private ListedCaseData listedCase;
        private Filter hearingFilter;
        private Filter defendantFilter;
        private Filter listingCaseFilter;

        public HearingDefendantFilter(HearingData hearing, ListedCaseData listedCase, DefendantData defendant) {
            this.hearing = hearing;
            this.listedCase = listedCase;
            this.defendant = defendant;
        }

        public Filter getHearingFilter() {
            return hearingFilter;
        }

        public Filter getDefendantFilter() {
            return defendantFilter;
        }

        public Filter getListingCaseFilter() {
            return listingCaseFilter;
        }

        public SendCaseForListingSteps.HearingDefendantFilter invoke() {
            hearingFilter = filter(where("id").is(hearing.getId().toString()));
            listingCaseFilter = filter(where("id").is(listedCase.getCaseId().toString()));
            defendantFilter = filter(where("id").is(defendant.getDefendantId().toString()));
            return this;
        }
    }

    public HearingsData getHearingsData() {
        return hearingsData;
    }

    @Override
    public void close() {
        try {

            privateMessageConsumerHearingAllocatedForListing.close();
            privateMessageConsumerHearingListed.close();
            privateMessageConsumerHearingDaysChanged.close();
        } catch (JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }
    }

}
