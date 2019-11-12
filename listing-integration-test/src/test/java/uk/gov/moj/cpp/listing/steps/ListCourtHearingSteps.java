package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
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
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;

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
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
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
import uk.gov.justice.listing.courts.ListCourtHearing;
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


public class ListCourtHearingSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCourtHearingSteps.class);

    private static final String LISTING_COMMAND_LIST_COURT_HEARING = "listing.command" +
            ".list-court-hearing";

    private static final String MEDIA_TYPE_LIST_COURT_HEARING = "application/vnd.listing" +
            ".command.list-court-hearing+json";

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";

    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing.search.hearing+json";

    private static final String EVENT_SELECTOR_HEARING_LISTED = "listing.events.hearing-listed";
    private static final String EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    private static final String EVENT_SELECTOR_HEARING_DAYS_CHANGED = "listing.events.hearing-days-changed-for-hearing";


    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String ORGANISATION_NAME = "ABC LTD";
    private static final String PERSON_TITLE = "Baroness";

    private static final boolean UNALLOCATED = false;
    private final HearingsData hearingsData;
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);
    private MessageConsumer privateMessageConsumerHearingListed;
    private MessageConsumer privateMessageConsumerHearingAllocatedForListing;
    private MessageConsumer privateMessageConsumerHearingDaysChanged;
    private String request;


    public ListCourtHearingSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        privateMessageConsumerHearingListed = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_LISTED);
        privateMessageConsumerHearingAllocatedForListing = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING);
        privateMessageConsumerHearingDaysChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_DAYS_CHANGED);

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }

    private static com.jayway.jsonpath.JsonPath getJsonPathQueryForDefendantLastName(HearingData hearing, ListedCaseData listedCase, DefendantData defendant, String expectedLastName) {
        ListCourtHearingSteps.HearingDefendantFilter hearingDefendantFilter = new ListCourtHearingSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        Filter defendantFilter = hearingDefendantFilter.getDefendantFilter();
        final Filter firstNameFilter = filter(
                where("lastName").eq(expectedLastName)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].defendants[?][?]", hearingFilter, listingCaseFilter, defendantFilter, firstNameFilter);
    }

    private static com.jayway.jsonpath.JsonPath getJsonPathQueryForCaseReference(HearingData hearing, ListedCaseData listedCase, DefendantData defendant, String expectedCaseReference) {
        ListCourtHearingSteps.HearingDefendantFilter hearingDefendantFilter = new ListCourtHearingSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        final Filter caseReferenceFilter = filter(
                where("caseReference").eq(expectedCaseReference)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].caseIdentifier.[?]", hearingFilter, listingCaseFilter, caseReferenceFilter);
    }

    public void whenCaseIsSubmittedForListing() {
        final Response response = getResponseCaseSubmittedForListing(false);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenCaseIsSubmittedForListingWithLegalEntity() {
        final Response response = getResponseCaseSubmittedForListingWithLegalEntity();
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    public void whenCaseIsSubmittedForListingByUnauthorisedUser() {
        final Response response = getResponseCaseSubmittedForListing(false);
        assertThat(response.getStatus(), equalTo(SC_FORBIDDEN));
    }

    public void whenCaseIsSubmittedForListingStandaloneApplication() {
        final Response response = getResponseCaseSubmittedForListing(true);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    private Response getResponseCaseSubmittedForListing(boolean isStandaloneApp) {
        hearingsData.getHearingData().stream()
                .map(hearingData -> hearingData.getCourtCentreId())
                .forEach(cci -> stubGetReferenceDataCourtCentre(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, null)));
        hearingsData.getHearingData().stream().forEach(hearingData -> stubGetReferenceDataHearingTypes(hearingData.getHearingTypeData().getTypeId()));

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        ListCourtHearing listCourtHearingData = null;
        if (isStandaloneApp) {
            listCourtHearingData = getListCourtHearingDataStandaloneApplication(hearingsData);
        } else {
            listCourtHearingData = getListCourtHearingData(hearingsData);
        }
        final JsonObject listCourtHearingJsonObject = (JsonObject) objectToJsonValueConverter.convert(listCourtHearingData);


        request = listCourtHearingJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
    }

    private Response getResponseCaseSubmittedForListingWithLegalEntity() {
        hearingsData.getHearingData().stream()
                .map(HearingData::getCourtCentreId)
                .forEach(cci -> stubGetReferenceDataCourtCentre(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, null)));
        hearingsData.getHearingData().stream().forEach(hearingData -> stubGetReferenceDataHearingTypes(hearingData.getHearingTypeData().getTypeId()));
        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        ListCourtHearing listCourtHearingData = getListCourtHearingDataWithLegalEntity(hearingsData);

        final JsonObject listCourtHearingJsonObject = (JsonObject) objectToJsonValueConverter.convert(listCourtHearingData);

        request = listCourtHearingJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
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

    public void verifyHearingListedInActiveMQForStandaloneApplication() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingListed);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearing.id"), is(jsRequest.getString("hearings[0].id")));
        assertThat(jsonResponse.get("hearing.hearingLanguage"), is("ENGLISH"));
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

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));


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
                        )));
    }

    public void verifyHearingListedWithLegalEntity(boolean isAllocated) {
        HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
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
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getLegalEntityDefendant().getOrganisation().getName())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].organisationName",
                                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getLegalEntityDefendant().getOrganisation().getName()))
                        )));
    }

    public void verifyHearingListedFromAPIForStandaloneApplication(boolean isAllocated) {
        HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
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
                                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                                        equalTo(hearingData.getCourtApplications().get(0).getParentApplicationId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getLastName()))
                        )));
    }

    public void verifyExistingHearingById() {

        final HearingData dataForFirstHearing = hearingsData.getHearingData().get(0);

        final String url = generateUrlForFindingAHearingById(dataForFirstHearing.getId().toString());

        poll(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(
                                allOf(
                                        withJsonPath("$.id",
                                                equalTo(dataForFirstHearing.getId().toString())),
                                        withJsonPath("$.jurisdictionType",
                                                equalTo(dataForFirstHearing.getJurisdictionType())),
                                        withJsonPath("$.courtCentreId",
                                                equalTo(dataForFirstHearing.getCourtCentreId().toString())),
                                        withJsonPath("$.type.id",
                                                equalTo(dataForFirstHearing.getHearingTypeData().getTypeId().toString())),
                                        withJsonPath("$.type.description",
                                                equalTo(dataForFirstHearing.getHearingTypeData().getTypeDescription())),
                                        withJsonPath("$.startDate",
                                                equalTo(dataForFirstHearing.getHearingStartDate().toString())),
                                        withJsonPath("$.courtApplications[0].applicationType",
                                                equalTo(dataForFirstHearing.getCourtApplications().get(0).getType())),
                                        withJsonPath("$.courtApplications[0].id",
                                                equalTo(dataForFirstHearing.getCourtApplications().get(0).getId().toString())),
                                        withJsonPath("$.courtApplications[0].parentApplicationId",
                                                equalTo(dataForFirstHearing.getCourtApplications().get(0).getParentApplicationId().toString())),
                                        withJsonPath("$.courtApplications[0].applicant.lastName",
                                                equalTo(dataForFirstHearing.getCourtApplications().get(0).getApplicant().getLastName())),
                                        withJsonPath("$.courtApplications[0].applicant.firstName",
                                                equalTo(dataForFirstHearing.getCourtApplications().get(0).getApplicant().getFirstName())),
                                        withJsonPath("$.courtApplications[0].respondents[0].firstName",
                                                equalTo(dataForFirstHearing.getCourtApplications().get(0).getRespondent().getFirstName())),
                                        withJsonPath("$.courtApplications[0].respondents[0].lastName",
                                                equalTo(dataForFirstHearing.getCourtApplications().get(0).getRespondent().getLastName()))
                                )));

    }


    public void verifyNonExistentHearingById() {

        final String url = generateUrlForFindingAHearingById("4e6d8d78-fa61-4102-8d14-2042df85faab");

        poll(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(Response.Status.NOT_FOUND),
                        payload());

    }


    public void verifyHearingByIdWithInvalidId() {

        final String invalidId = "4e6d8d78-fa61";

        final String url = generateUrlForFindingAHearingById(invalidId);

        poll(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(Response.Status.BAD_REQUEST),
                        payload().isJson(allOf(withJsonPath("$.error",
                                equalTo("Please ensure that the id is a valid UUID.")))));

    }

    private String generateUrlForFindingAHearingById(String rawId) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"),
                        rawId
                ));
    }


    private ListCourtHearing getListCourtHearingData(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        //   List<DefendantData> defendantData = listedCaseData.getDefendants();

        return ListCourtHearing.listCourtHearing()
                .withHearings(asList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withCourtApplications(asList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withLinkedCaseId(of(hearingData.getCourtApplications().get(0).getLinkedCaseId()))
                                .withParentApplicationId(of(hearingData.getCourtApplications().get(0).getParentApplicationId()))
                                .withType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .withApplicationCode(Optional.of(STRING.next()))
                                        .withApplicationType(hearingData.getCourtApplications().get(0).getType())
                                        .withApplicationLegislation(Optional.of(STRING.next()))
                                        .withApplicationCategory(STRING.next())
                                        .withLinkType(LinkType.EITHER)
                                        .withApplicationJurisdictionType(ApplicationJurisdictionType.CROWN)
                                        .build())
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withApplicationReference(Optional.of(STRING.next()))
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
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? of(hearingData.getHearingStartTime()) : Optional.empty())
                        .withEndDate(hearingData.getHearingEndDate() != null ? of(hearingData.getHearingEndDate().toString()) : Optional.empty())
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(hearingData.getJudiciary() != null
                                ? asList(JudicialRole.judicialRole()
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
                                        .withCaseMarkers(asList(Marker.marker()
                                                .withId(randomUUID())
                                                .withMarkerTypeCode("C")
                                                .withMarkerTypeDescription("Description")
                                                .withMarkerTypeid(randomUUID()).build()))
                                        .withDefendants(lc.getDefendants().stream().map(d -> Defendant.defendant()
                                                .withId(d.getDefendantId())
                                                .withIsYouth(ofNullable(d.getIsYouth()))
                                                .withPersonDefendant(of(PersonDefendant.personDefendant()
                                                        .withBailStatus(of(new BailStatus.Builder().withCode(d.getBailStatus().getCode()).withDescription(d.getBailStatus().getDescription()).withId(d.getBailStatus().getId()).build()))
                                                        .withPersonDetails(Person.person()
                                                                .withTitle(of(PERSON_TITLE))
                                                                .withNationalityId(of(randomUUID()))
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
                                                                .withCount(Optional.of(INTEGER.next()))
                                                                .withId(o.getOffenceId())
                                                                .withOffenceCode(STRING.next())
                                                                .withOffenceDefinitionId(randomUUID())
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

    private ListCourtHearing getListCourtHearingDataWithLegalEntity(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        return ListCourtHearing.listCourtHearing()
                .withHearings(asList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withCourtApplications(asList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withLinkedCaseId(of(hearingData.getCourtApplications().get(0).getLinkedCaseId()))
                                .withParentApplicationId(of(hearingData.getCourtApplications().get(0).getParentApplicationId()))
                                .withType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .withApplicationCode(Optional.of(STRING.next()))
                                        .withApplicationType(hearingData.getCourtApplications().get(0).getType())
                                        .withApplicationLegislation(Optional.of(STRING.next()))
                                        .withApplicationCategory(STRING.next())
                                        .withLinkType(LinkType.EITHER)
                                        .withApplicationJurisdictionType(ApplicationJurisdictionType.CROWN)
                                        .build())
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withApplicationReference(Optional.of(STRING.next()))
                                .withApplicationStatus(ApplicationStatus.LISTED)
                                .withApplicant(CourtApplicationParty.courtApplicationParty()
                                        .withId(randomUUID())
                                        .withDefendant(Optional.of(Defendant.defendant()
                                                .withOffences(Arrays.asList(Offence.offence()
                                                        .withCount(Optional.of(INTEGER.next()))
                                                        .withId(randomUUID())
                                                        .withOffenceCode(STRING.next())
                                                        .withOffenceDefinitionId(randomUUID())
                                                        .withWording(STRING.next())
                                                        .withStartDate(LocalDate.now().toString())
                                                        .withOrderIndex(of(INTEGER.next()))
                                                        .withOffenceTitle("test-title")
                                                        .build()))
                                                .withId(randomUUID())
                                                .withProsecutionCaseId(randomUUID())
                                                .withLegalEntityDefendant(Optional.of(LegalEntityDefendant.legalEntityDefendant()
                                                        .withOrganisation(Organisation.organisation()
                                                                .withName(ORGANISATION_NAME).build()).build())).build()))
                                        .build())
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? of(hearingData.getHearingStartTime()) : Optional.empty())
                        .withEndDate(hearingData.getHearingEndDate() != null ? of(hearingData.getHearingEndDate().toString()) : Optional.empty())
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(hearingData.getJudiciary() != null
                                ? asList(JudicialRole.judicialRole()
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
                                                .withLegalEntityDefendant(Optional.of(LegalEntityDefendant.legalEntityDefendant()
                                                        .withOrganisation(Organisation.organisation()
                                                                .withName(ORGANISATION_NAME).build()).build()))
                                                .withOffences(d.getOffences().stream()
                                                        .map(o -> Offence.offence()
                                                                .withCount(Optional.of(INTEGER.next()))
                                                                .withId(o.getOffenceId())
                                                                .withOffenceCode(STRING.next())
                                                                .withOffenceDefinitionId(randomUUID())
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

    private ListCourtHearing getListCourtHearingDataStandaloneApplication(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);

        return ListCourtHearing.listCourtHearing()
                .withHearings(asList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
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
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withType(HearingType.hearingType()
                                .withDescription(hearingData.getHearingTypeData().getTypeDescription())
                                .withId(hearingData.getHearingTypeData().getTypeId())
                                .build())
                        .withReportingRestrictionReason(of(hearingData.getReportingRestrictionReason()))
                        .build()))
                .build();
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

        public ListCourtHearingSteps.HearingDefendantFilter invoke() {
            hearingFilter = filter(where("id").is(hearing.getId().toString()));
            listingCaseFilter = filter(where("id").is(listedCase.getCaseId().toString()));
            defendantFilter = filter(where("id").is(defendant.getDefendantId().toString()));
            return this;
        }
    }

}