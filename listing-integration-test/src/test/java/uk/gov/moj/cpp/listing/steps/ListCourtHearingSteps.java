package uk.gov.moj.cpp.listing.steps;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
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
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.courts.ApplicationJurisdictionType;
import uk.gov.justice.listing.courts.ApplicationStatus;
import uk.gov.justice.listing.courts.Gender;
import uk.gov.justice.listing.courts.InitiationCode;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.courts.LinkType;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.WeekCommencingDate;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListCourtHearingSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCourtHearingSteps.class);

    private static final String LISTING_COMMAND_LIST_COURT_HEARING = "listing.command" +
            ".list-court-hearing";

    private static final String MEDIA_TYPE_LIST_COURT_HEARING = "application/vnd.listing" +
            ".command.list-court-hearing+json";

    protected static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";

    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing" +
            ".search.hearing+json";

    public static final String LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING = "listing.command" +
            ".extend-hearing-for-hearing";

    public static final String MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING = "application/vnd.listing." +
            "command.extend-hearing-for-hearing+json";

    private static final String EVENT_SELECTOR_HEARING_LISTED = "listing.events.hearing-listed";
    private static final String EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    private static final String EVENT_SELECTOR_HEARING_DAYS_CHANGED = "listing.events.hearing-days-changed-for-hearing";

    private static final String EVENT_SELECTED_ADDED_CASE_FOR_HEARING = "listing.event.added-cases-for-hearing";
    private static final String EVENT_SELECTED_HEARING_UPDATED_TO_CASE = "listing.events.hearing-updated-to-case";
    private static final String EVENT_SELECTED_HEARING_DELETED = "listing.events.hearing-deleted";
    private static final String EVENT_SELECTED_HEARING_PARTIALLY_UPDATED = "listing.events.hearing-partially-updated";
    private static final String PUBLIC_EVENT_SELECTED_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED = "public.progression.events.hearing-extended";

    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String ORGANISATION_NAME = "ABC LTD";
    protected static final String PERSON_TITLE = "Baroness";
    protected static final String PERSON_NATIONALITY_DESCRIPTION = "British";
    private static final String POSTCODE = "CR1 4BX";

    protected static final String PERSON_ADDRESS_1 = "address1";
    protected static final String PERSON_ADDRESS_2 = "address2";
    protected static final String PERSON_ADDRESS_3 = "address3";
    protected static final String PERSON_ADDRESS_4 = "address4";
    protected static final String PERSON_ADDRESS_5 = "address5";
    protected static final String PERSON_POSTCODE = "CR1 4BX";


    private HearingsData hearingsData;
    private final MessageConsumer privateMessageConsumerHearingListed;
    private final MessageConsumer privateMessageConsumerHearingAllocatedForListing;
    private final MessageConsumer privateMessageConsumerAddedCaseForHearing;
    private final MessageConsumer privateMessageConsumerHearingUpdatedToCase;
    private final MessageConsumer privateMessageConsumerHearingDeleted;
    private final MessageConsumer privateMessageConsumerHearingPartiallyUpdated;
    private final MessageConsumer privateMessageConsumerHearingDaysChanged;
    private final MessageConsumer publicMessageConsumerHearingConfirmedForExtendHearing;
    private final MessageConsumer publicMessageConsumerHearingExtend;

    private final MessageProducer publicMessageProducerProgressionHearingExtendedEvent;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);
    protected String request;

    private NotesSteps notesSteps = new NotesSteps();

    public ListCourtHearingSteps(final HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        privateMessageConsumerHearingListed = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_LISTED);
        privateMessageConsumerHearingAllocatedForListing = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING);
        privateMessageConsumerHearingDaysChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_DAYS_CHANGED);
        privateMessageConsumerAddedCaseForHearing = privateEvents.createConsumer(EVENT_SELECTED_ADDED_CASE_FOR_HEARING);
        privateMessageConsumerHearingUpdatedToCase = privateEvents.createConsumer(EVENT_SELECTED_HEARING_UPDATED_TO_CASE);
        privateMessageConsumerHearingDeleted = privateEvents.createConsumer(EVENT_SELECTED_HEARING_DELETED);
        privateMessageConsumerHearingPartiallyUpdated = privateEvents.createConsumer(EVENT_SELECTED_HEARING_PARTIALLY_UPDATED);
        publicMessageConsumerHearingConfirmedForExtendHearing = publicEvents.createConsumer(PUBLIC_EVENT_SELECTED_HEARING_CONFIRMED);
        publicMessageConsumerHearingExtend = publicEvents.createConsumer(PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED);

        publicMessageProducerProgressionHearingExtendedEvent = QueueUtil.publicEvents.createProducer();

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }

    public ListCourtHearingSteps() {
        privateMessageConsumerHearingListed = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_LISTED);
        privateMessageConsumerHearingAllocatedForListing = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING);
        privateMessageConsumerHearingDaysChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_DAYS_CHANGED);
        privateMessageConsumerAddedCaseForHearing = privateEvents.createConsumer(EVENT_SELECTED_ADDED_CASE_FOR_HEARING);
        privateMessageConsumerHearingUpdatedToCase = privateEvents.createConsumer(EVENT_SELECTED_HEARING_UPDATED_TO_CASE);
        privateMessageConsumerHearingDeleted = privateEvents.createConsumer(EVENT_SELECTED_HEARING_DELETED);
        privateMessageConsumerHearingPartiallyUpdated = privateEvents.createConsumer(EVENT_SELECTED_HEARING_PARTIALLY_UPDATED);
        publicMessageConsumerHearingConfirmedForExtendHearing = publicEvents.createConsumer(PUBLIC_EVENT_SELECTED_HEARING_CONFIRMED);
        publicMessageConsumerHearingExtend = publicEvents.createConsumer(PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED);

        publicMessageProducerProgressionHearingExtendedEvent = QueueUtil.publicEvents.createProducer();

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }

    protected static com.jayway.jsonpath.JsonPath getJsonPathQueryForDefendantLastName(final HearingData hearing, final ListedCaseData listedCase, final DefendantData defendant, final String expectedLastName) {
        final ListCourtHearingSteps.HearingDefendantFilter hearingDefendantFilter = new ListCourtHearingSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        final Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        final Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        final Filter defendantFilter = hearingDefendantFilter.getDefendantFilter();
        final Filter firstNameFilter = filter(
                where("lastName").eq(expectedLastName)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].defendants[?][?]", hearingFilter, listingCaseFilter, defendantFilter, firstNameFilter);
    }

    protected static com.jayway.jsonpath.JsonPath getJsonPathQueryForCaseReference(final HearingData hearing, final ListedCaseData listedCase, final DefendantData defendant, final String expectedCaseReference) {
        final ListCourtHearingSteps.HearingDefendantFilter hearingDefendantFilter = new ListCourtHearingSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        final Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        final Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        final Filter caseReferenceFilter = filter(
                where("caseReference").eq(expectedCaseReference)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].caseIdentifier.[?]", hearingFilter, listingCaseFilter, caseReferenceFilter);
    }

    public void whenCaseIsSubmittedForListing() {
        final Response response = getResponseCaseSubmittedForListing(false);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenCaseIsSubmittedForListingWithBookedSlot() {
        final Response response = getResponseCaseSubmittedForListingBookedSlot();
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

    public void whenProgressionHearingExtended() throws IOException {
        final String eventPayloadString = getStringFromResource("prosecution-case-with-shadow-listed-offences.json")
                .replaceAll("HEARING_ID", hearingsData.getHearingData().get(0).getId().toString())
                .replaceAll("CASE_ID_1", UUID.randomUUID().toString())
                .replaceAll("DEFENDANT_ID_1", UUID.randomUUID().toString())
                .replaceAll("OFFENCE_ID_1", UUID.randomUUID().toString())
                .replaceAll("CASE_ID_2", UUID.randomUUID().toString())
                .replaceAll("DEFENDANT_ID_2", UUID.randomUUID().toString())
                .replaceAll("OFFENCE_ID_2", UUID.randomUUID().toString());

        JsonObject hearingExtendedDataObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        QueueUtil.sendMessage(
                publicMessageProducerProgressionHearingExtendedEvent,
                PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED,
                hearingExtendedDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED, hearingExtendedDataObject, getLoggedInHeader());
    }

    private Response getResponseCaseSubmittedForListing(final boolean isStandaloneApp) {

        stubReferenceDataForFirstHearing();

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        ListCourtHearing listCourtHearingData;
        if (isStandaloneApp) {
            listCourtHearingData = getListCourtHearingDataStandaloneApplication(hearingsData);
        } else {
            listCourtHearingData = getListCourtHearingData(hearingsData);
        }
        final JsonObject listCourtHearingJsonObject = (JsonObject) objectToJsonValueConverter.convert(listCourtHearingData);

        request = listCourtHearingJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
    }

    private Response getResponseCaseSubmittedForListingBookedSlot() {

        stubReferenceDataForFirstHearing();

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        ListCourtHearing listCourtHearingData = createListCourtHearingBookedSlotData(hearingsData);

        final JsonObject listCourtHearingJsonObject = (JsonObject) objectToJsonValueConverter.convert(listCourtHearingData);

        request = listCourtHearingJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
    }

    protected void stubReferenceDataForFirstHearing() {

        hearingsData.getHearingData().stream()
                .map(HearingData::getCourtCentreId)
                .forEach(cci -> {
                    stubGetReferenceDataCourtCentre(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, hearingsData.getHearingData().get(0).getCourtRoomId(), hearingsData.getHearingData().get(0).getName()));
                    stubGetReferenceDataCourtCentreById(cci);
                    stubGetReferenceDataCourtMappings(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, hearingsData.getHearingData().get(0).getCourtRoomId(), hearingsData.getHearingData().get(0).getName()));
                });
        hearingsData.getHearingData().forEach(hearingData -> stubGetReferenceDataHearingTypes(hearingData.getHearingTypeData().getTypeId()));
        hearingsData.getHearingData().stream().filter(hd -> hd.getJudiciary() != null)
                .forEach(hearingData -> stubGetReferenceDataJudiciaries(hearingData.getJudiciary().get(0).getJudicialId()));
    }

    private Response getResponseCaseSubmittedForListingWithLegalEntity() {
        hearingsData.getHearingData().stream()
                .map(HearingData::getCourtCentreId)
                .forEach(cci -> stubGetReferenceDataCourtCentre(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, null, "Carmarthen Magistrates Court")));
        hearingsData.getHearingData().forEach(hearingData -> stubGetReferenceDataHearingTypes(hearingData.getHearingTypeData().getTypeId()));
        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        final ListCourtHearing listCourtHearingData = getListCourtHearingDataWithLegalEntity(hearingsData);

        final JsonObject listCourtHearingJsonObject = (JsonObject) objectToJsonValueConverter.convert(listCourtHearingData);

        request = listCourtHearingJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
    }

    public void verifyHearingListedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingListed);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearing.id"), is(jsRequest.getString("hearings[0].id")));
        assertThat(jsonResponse.get("hearing.hearingLanguage"), is("ENGLISH"));
        assertThat(jsonResponse.get("hearing.listedCases[0].id"), is(jsRequest.getString("hearings[0].prosecutionCases[0].id")));
        assertThat(jsonResponse.get("hearing.listedCases[0].defendants[0].id"), is(jsRequest.getString("hearings[0].prosecutionCases[0].defendants[0].id")));
    }

    public void verifyHearingListedWithBookedSlotsInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingListed);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearing.id"), is(jsRequest.getString("hearings[0].id")));
        assertThat(jsonResponse.get("hearing.hearingLanguage"), is("ENGLISH"));

        final RotaSlot rotaSlot = hearingsData.getHearingData().get(0).getBookedSlots().get(0);
        assertThat(jsonResponse.get("hearing.nonDefaultDays[0].courtRoomId"), is(rotaSlot.getCourtRoomId().get()));
        assertThat(jsonResponse.get("hearing.nonDefaultDays[0].duration"), is(rotaSlot.getDuration().get()));
        assertThat(jsonResponse.get("hearing.nonDefaultDays[0].oucode"), is(rotaSlot.getOucode().get()));
        assertThat(jsonResponse.get("hearing.nonDefaultDays[0].session"), is(rotaSlot.getSession().get()));
        assertThat(jsonResponse.get("hearing.nonDefaultDays[0].courtScheduleId"), is(rotaSlot.getCourtScheduleId().get()));
    }

    public void verifyHearingListedInActiveMQForStandaloneApplication() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingListed);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearing.id"), is(jsRequest.getString("hearings[0].id")));
        assertThat(jsonResponse.get("hearing.hearingLanguage"), is("ENGLISH"));
    }

    public void verifyHearingAllocatedForListingInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingAllocatedForListing);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingAllocatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(jsRequest.getString("hearings[0].id")));
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        verifyHearingListedFromWithApiUrl(hearingData, searchHearingUrl);
    }

    public void verifyHearingListedWithAnyAllocationFromAPI(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.any-allocation.search.hearings"), hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseReference(), isAllocated));

        verifyHearingListedFromWithApiUrl(hearingData, searchHearingUrl);
    }

    public void verifyHearingListedFromWithApiUrl(final HearingData hearingData, final String searchHearingUrl) {
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final DefendantData defendant = listedCaseData.getDefendants().get(0);

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
                                withJsonPath("$.hearings[0].courtApplications[0].applicationParticulars",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicationParticulars())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getPostcode().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getPostcode().get())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].isYouth",
                                        equalTo(true)))));
    }

    private Matcher getNotesMatcher(boolean isAllocated, boolean noteExist) {
        if(isAllocated == false || noteExist == false){
            return withJsonPath("$.notes.size()", is(0));
        }else{
            final AtomicInteger index= new AtomicInteger(0);
            final List<Matcher> noteMatchers = hearingsData.getHearingData().stream().
                    filter(hearing -> hearing.getCourtRoomId() != null).
                    limit(1).
                    map(hearing -> allOf(withJsonPath("$.notes["+index.get()+"].courtRoomId", equalTo(hearing.getCourtRoomId().toString())),
                            withJsonPath("$.notes["+index.getAndIncrement()+"].date", equalTo("2020-05-21")))).
                    collect(Collectors.toList());
            noteMatchers.add(withJsonPath("$.notes.size()", is(noteMatchers.size())));
            return allOf(noteMatchers.toArray(new Matcher[noteMatchers.size()]));
        }
    }

    public void verifyHearingListedWithHearingDays(final boolean isAllocated, final String[] courtScheduleSlots) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        ResponseData resp = poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
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
                                withJsonPath("$.hearings[0].courtApplications[0].applicationParticulars",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicationParticulars())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getPostcode().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getPostcode().get())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].isYouth",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].hearingDays[0].hearingDate",
                                        equalTo(courtScheduleSlots[0])),
                                withJsonPath("$.hearings[0].hearingDays[1].hearingDate",
                                        equalTo(courtScheduleSlots[1])),
                                withJsonPath("$.hearings[0].hearingDays[2].hearingDate",
                                        equalTo(courtScheduleSlots[2]))
                        )));
    }

    public void verifyHearingListedFromAPIAllocatedForBookSlots() {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), true));


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
                                withJsonPath("$.hearings[0].courtApplications[0].linkedCaseId",
                                        equalTo(hearingData.getCourtApplications().get(0).getLinkedCaseId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                                        equalTo(hearingData.getCourtApplications().get(0).getParentApplicationId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicationParticulars",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicationParticulars())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getPostcode().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getFirstName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getPostcode().get()))

                        )));
    }

    public void verifyHearingListedWithShadowListedFlag(final boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].listedCases[0].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[1].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[1].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[2].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[1].offences[0].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[1].offences[1].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[1].offences[2].shadowListed",
                                        equalTo(true))
                        )));
    }

    /**
     * We have added two new listed cases with one offence each
     * */
    public void verifyHearingExtendedWithShadowListedFlag(final boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].listedCases[2].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[3].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[2].defendants[0].offences[0].shadowListed",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].listedCases[3].defendants[0].offences[0].shadowListed",
                                        equalTo(true))
                        )));
    }

    public void verifyHearingListedWithWeekCommencingFromAPI(final boolean isAllocated, final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingData.getId().toString())),
                                withJsonPath("$.hearings[0].weekCommencingStartDate", equalTo(FORMATTER.format(weekCommencingStartDate))),
                                withJsonPath("$.hearings[0].weekCommencingEndDate", equalTo(FORMATTER.format(weekCommencingStartDate.plusWeeks(weekCommencingDuration)))),
                                withJsonPath("$.hearings[0].weekCommencingDurationInWeeks", equalTo(weekCommencingDuration))
                        )));
    }

    public void verifyHearingForWeekCommencingRange(final String jurisdictionType, final String weekCommencingStartDate, final String weekCommencingEndDate, final boolean allocated, final Matcher... matchers) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings.for.week.commencing.range"), jurisdictionType, weekCommencingStartDate, weekCommencingEndDate, allocated));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser())).
                until(status().is(OK),
                        payload().isJson(allOf(matchers))
                );
    }

    public void verifyHearingListedWithLegalEntity(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final DefendantData defendant = listedCaseData.getDefendants().get(0);

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
                                withJsonPath("$.hearings[0].courtApplications[0].applicationParticulars",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicationParticulars())),
                                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                                        equalTo(hearingData.getCourtApplications().get(0).getParentApplicationId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getLegalEntityDefendant().getOrganisation().getName())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].organisationName",
                                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getLegalEntityDefendant().getOrganisation().getName())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getPostcode().get()))
                        )));
    }

    public void verifyHearingListedFromAPIForStandaloneApplication(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

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
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getLastName())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicationParticulars",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicationParticulars())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getApplicant().getAddress().getPostcode().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address1",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress1())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address2",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress2().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address3",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress3().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address4",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress4().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.address5",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getAddress5().get())),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].address.postcode",
                                        equalTo(hearingData.getCourtApplications().get(0).getRespondent().getAddress().getPostcode().get()))
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

    public void verifyAvailableHearingListedForCaseInHearingAndCaseUrn(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId) {

        final String searchHearingUrl1 = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam()));

        verifyHearingDetails(caseAndDefendantData, masterDefendantId, searchHearingUrl1);

        final String searchHearingUrl2 = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-case.urn"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getCaseUrnQueryParam()));

        verifyHearingDetails(caseAndDefendantData, masterDefendantId, searchHearingUrl2);

    }

    private void verifyHearingDetails(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId, final String searchHearingUrl) {
        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        not(caseAndDefendantData.getHearingId())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].allocated",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().plusDays(1).toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0))
                        )));
    }

    public void verifyAvailableHearingListedForMatchedDefendant(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-jurisdiction.type"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam()));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        not(caseAndDefendantData.getHearingId())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].allocated",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().plusDays(1).toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        not(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0))
                        )));
    }

    public void verifyAvailableHearingListedForCaseInHearingAndMatchedDefendant(final CaseAndDefendantData caseAndDefendantData) {

        final String searchHearingUrlForCrown = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-jurisdiction.type"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam()));

        poll(requestParams(searchHearingUrlForCrown, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        not(caseAndDefendantData.getHearingId())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0))
                        )));


    }

    public void verifyAvailableHearing(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId, final boolean notesExists) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-defendant.ids"),
                        masterDefendantId.toString()));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(caseAndDefendantData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].allocated",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().plusDays(1).toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(caseAndDefendantData.getMasterDefendantId().toString())),
                                getNotesMatcher(true, notesExists)
                        )));
    }

    public void verifyAvailableHearingNotExists(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-defendant.ids"),
                        masterDefendantId.toString()));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings.size()",
                                        equalTo(0)),
                                getNotesMatcher(true, false)
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

    private String generateUrlForFindingAHearingById(final String rawId) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"),
                        rawId
                ));
    }

    private ListCourtHearing createListCourtHearingBookedSlotData(final HearingsData hearingsData) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        return ListCourtHearing.listCourtHearing()
                .withAdjournedFromDate(Optional.of(LocalDate.now().toString()))
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withBookedSlots(hearingData.getBookedSlots())
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withBookingReference(Optional.of(randomUUID()))
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
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
                                .withApplicationParticulars(of(hearingData.getCourtApplications().get(0).getApplicationParticulars()))
                                .withApplicationStatus(ApplicationStatus.LISTED)
                                .withApplicant(CourtApplicationParty.courtApplicationParty()
                                        .withId(hearingData.getCourtApplications().get(0).getApplicant().getId())
                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getApplicant().getLastName())
                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getApplicant().getFirstName()))
                                                .withGender(Gender.FEMALE)
                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getApplicant().getAddress()))
                                                .build()))
                                        .build())
                                .withRespondents(singletonList(
                                        CourtApplicationRespondent.courtApplicationRespondent()
                                                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                                        .withId(hearingData.getCourtApplications().get(0).getRespondent().getId())
                                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getRespondent().getFirstName()))
                                                                .withGender(Gender.FEMALE)
                                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getRespondent().getAddress()))
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
                                ? singletonList(JudicialRole.judicialRole()
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
                        .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? Optional.empty() :
                                of(WeekCommencingDate.weekCommencingDate()
                                        .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                        .withDuration(Optional.of(hearingData.getWeekCommencingDuration()))
                                        .build()))
                        .withType(HearingType.hearingType()
                                .withDescription(hearingData.getHearingTypeData().getTypeDescription())
                                .withId(hearingData.getHearingTypeData().getTypeId())
                                .build())
                        .withReportingRestrictionReason(of(hearingData.getReportingRestrictionReason()))
                        .build()))
                .build();
    }

    private ListCourtHearing getListCourtHearingData(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final List<UUID> shadowListedOffences = hearingData.getListedCases()
                .stream()
                .flatMap(listedCase -> listedCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> offence.getShadowListed().orElse(Boolean.FALSE))
                .map(offence -> offence.getOffenceId())
                .collect(Collectors.toList());

        return ListCourtHearing.listCourtHearing()
                .withAdjournedFromDate(Optional.of(LocalDate.now().toString()))
                .withShadowListedOffences(shadowListedOffences)
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withBookingReference(Optional.of(randomUUID()))
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
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
                                .withApplicationParticulars(of(hearingData.getCourtApplications().get(0).getApplicationParticulars()))
                                .withApplicationStatus(ApplicationStatus.LISTED)
                                .withApplicant(CourtApplicationParty.courtApplicationParty()
                                        .withId(hearingData.getCourtApplications().get(0).getApplicant().getId())
                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getApplicant().getLastName())
                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getApplicant().getFirstName()))
                                                .withGender(Gender.FEMALE)
                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getApplicant().getAddress()))
                                                .build()))
                                        .build())
                                .withRespondents(singletonList(
                                        CourtApplicationRespondent.courtApplicationRespondent()
                                                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                                        .withId(hearingData.getCourtApplications().get(0).getRespondent().getId())
                                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getRespondent().getFirstName()))
                                                                .withGender(Gender.FEMALE)
                                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getRespondent().getAddress()))
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
                                ? singletonList(JudicialRole.judicialRole()
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
                        .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? Optional.empty() :
                                of(WeekCommencingDate.weekCommencingDate()
                                        .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                        .withDuration(Optional.of(hearingData.getWeekCommencingDuration()))
                                        .build()))
                        .withProsecutionCases(hearingData.getListedCases().stream()
                                .map(lc -> ProsecutionCase.prosecutionCase().withId(lc.getCaseId())
                                        .withInitiationCode(InitiationCode.C)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityCode(lc.getAuthorityCode())
                                                .withProsecutionAuthorityId(lc.getAuthorityId())
                                                .withProsecutionAuthorityReference(lc.getCaseReference())
                                                .build())
                                        .withCaseMarkers(singletonList(Marker.marker()
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
                                                                .withAddress(buildAddress())
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
                                                .withAssociatedPersons(singletonList(AssociatedPerson.associatedPerson()
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

    private Optional<Address> getAddress(final uk.gov.moj.cpp.listing.domain.Address address) {
        return of(Address
                .address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withAddress5(address.getAddress5())
                .withPostcode(address.getPostcode())
                .build());
    }

    private Optional<Address> buildAddress() {
        return of(Address.address()
                .withAddress1(STRING.next())
                .withAddress2(of(STRING.next()))
                .withAddress3(of(STRING.next()))
                .withAddress4(of(STRING.next()))
                .withAddress5(of(STRING.next()))
                .withPostcode(of(POSTCODE))
                .build());
    }

    private ListCourtHearing getListCourtHearingDataWithLegalEntity(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        return ListCourtHearing.listCourtHearing()
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withLinkedCaseId(of(hearingData.getCourtApplications().get(0).getLinkedCaseId()))
                                .withParentApplicationId(of(hearingData.getCourtApplications().get(0).getParentApplicationId()))
                                .withApplicationParticulars(of(hearingData.getCourtApplications().get(0).getApplicationParticulars()))
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
                                        .withId(randomUUID())
                                        .withDefendant(of(Defendant.defendant()
                                                .withOffences(singletonList(Offence.offence()
                                                        .withCount(of(INTEGER.next()))
                                                        .withId(randomUUID())
                                                        .withOffenceCode(STRING.next())
                                                        .withOffenceDefinitionId(randomUUID())
                                                        .withWording(STRING.next())
                                                        .withStartDate(LocalDate.now().toString())
                                                        .withOrderIndex(of(INTEGER.next()))
                                                        .withOffenceTitle("test-title")
                                                        .build()))
                                                .withId(randomUUID())
                                                .withMasterDefendantId(randomUUID())
                                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                                .withProsecutionCaseId(randomUUID())
                                                .withLegalEntityDefendant(of(LegalEntityDefendant.legalEntityDefendant()
                                                        .withOrganisation(Organisation.organisation()
                                                                .withName(ORGANISATION_NAME)
                                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getApplicant().getAddress()))
                                                                .build()).build())).build()))
                                        .build())
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? of(hearingData.getHearingStartTime()) : Optional.empty())
                        .withEndDate(hearingData.getHearingEndDate() != null ? of(hearingData.getHearingEndDate().toString()) : Optional.empty())
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(hearingData.getJudiciary() != null
                                ? singletonList(JudicialRole.judicialRole()
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
                        .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? Optional.empty() :
                                of(WeekCommencingDate.weekCommencingDate()
                                        .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                        .withDuration(of(hearingData.getWeekCommencingDuration()))
                                        .build()))
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
                                                .withMasterDefendantId(d.getMasterDefendantId())
                                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                                .withLegalEntityDefendant(of(LegalEntityDefendant.legalEntityDefendant()
                                                        .withOrganisation(Organisation.organisation()
                                                                .withName(ORGANISATION_NAME)
                                                                .withAddress(buildAddress())
                                                                .build()).build()))
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

    private ListCourtHearing getListCourtHearingDataStandaloneApplication(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);

        return ListCourtHearing.listCourtHearing()
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                                .build())
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withParentApplicationId(of(hearingData.getCourtApplications().get(0).getParentApplicationId()))
                                .withApplicationParticulars(of(hearingData.getCourtApplications().get(0).getApplicationParticulars()))
                                .withType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .withApplicationCode(of(STRING.next()))
                                        .withApplicationType(hearingData.getCourtApplications().get(0).getType())
                                        .withApplicationLegislation(of(STRING.next()))
                                        .withApplicationCategory(STRING.next())
                                        .withLinkType(LinkType.STANDALONE)
                                        .withApplicationJurisdictionType(ApplicationJurisdictionType.MAGISTRATES)
                                        .build())
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withApplicationReference(of(STRING.next()))
                                .withApplicationStatus(ApplicationStatus.DRAFT)
                                .withApplicant(CourtApplicationParty.courtApplicationParty()
                                        .withId(randomUUID())
                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getApplicant().getLastName())
                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getApplicant().getFirstName()))
                                                .withGender(Gender.FEMALE)
                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getApplicant().getAddress()))
                                                .build()))
                                        .build())
                                .withRespondents(singletonList(
                                        CourtApplicationRespondent.courtApplicationRespondent()
                                                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                                        .withId(randomUUID())
                                                        .withPersonDetails(of(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                                                .withFirstName(of(hearingData.getCourtApplications().get(0).getRespondent().getFirstName()))
                                                                .withGender(Gender.FEMALE)
                                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getRespondent().getAddress()))
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
            privateMessageConsumerAddedCaseForHearing.close();
            privateMessageConsumerHearingDeleted.close();
            privateMessageConsumerHearingPartiallyUpdated.close();
            publicMessageConsumerHearingConfirmedForExtendHearing.close();
            publicMessageConsumerHearingExtend.close();

            publicMessageProducerProgressionHearingExtendedEvent.close();

        } catch (final JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }
    }

    private static class HearingDefendantFilter {
        private final HearingData hearing;
        private final DefendantData defendant;
        private final ListedCaseData listedCase;
        private Filter hearingFilter;
        private Filter defendantFilter;
        private Filter listingCaseFilter;

        public HearingDefendantFilter(final HearingData hearing, final ListedCaseData listedCase, final DefendantData defendant) {
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


    public void extendHearing(final UUID unAllocatedHearingId, final UUID allocatedHearingId) {
        final String extendHearingForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING), unAllocatedHearingId));

        final String requestString = "{\n" + "  \"allocatedHearingId\": \"ALLOCATED_HEARING_ID\"\n" + "}";
        final String requestBody = requestString.replace("ALLOCATED_HEARING_ID", allocatedHearingId.toString());

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING, requestBody, getLoggedInHeader());

        restClient.postCommand(extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING,
                requestBody, getLoggedInHeader());
    }

    public void extendHearingPartially(final UUID unAllocatedHearingId, final UUID allocatedHearingId, final ListedCaseData listedCaseData) throws IOException {
        final String extendHearingForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING), unAllocatedHearingId));

        final String requestBody = getStringFromResource("stub-data/listing.command.extend.hearing-for-hearing-partial.json")
                .replace("HEARING_ID1", allocatedHearingId.toString())
                .replace("CASE_ID1", listedCaseData.getCaseId().toString())
                .replace("DEFENDANT_ID1", listedCaseData.getDefendants().get(0).getDefendantId().toString())
                .replace("OFFENCE_ID1", listedCaseData.getDefendants().get(0).getOffences().get(0).getOffenceId().toString());

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING, requestBody, getLoggedInHeader());

        restClient.postCommand(extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING,
                requestBody, getLoggedInHeader());
    }

    public void extendWholeHearing(final UUID unAllocatedHearingId, final UUID allocatedHearingId, final List<ListedCaseData> listedCaseData) throws IOException {
        final String extendHearingForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING), unAllocatedHearingId));

        final ListedCaseData case1 = listedCaseData.get(0);
        final ListedCaseData case2 = listedCaseData.get(1);

        final String requestBody = getStringFromResource("stub-data/listing.command.extend.hearing-for-hearing-whole.json")
                .replace("HEARING_ID1", allocatedHearingId.toString())
                .replace("CASE_ID1", case1.getCaseId().toString())
                .replace("DEFENDANT_ID1", case1.getDefendants().get(0).getDefendantId().toString())
                .replace("OFFENCE_ID1", case1.getDefendants().get(0).getOffences().get(0).getOffenceId().toString())
                .replace("CASE_ID2", case2.getCaseId().toString())
                .replace("DEFENDANT_ID2", case2.getDefendants().get(0).getDefendantId().toString())
                .replace("OFFENCE_ID2", case2.getDefendants().get(0).getOffences().get(0).getOffenceId().toString());

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING, requestBody, getLoggedInHeader());

        restClient.postCommand(extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING,
                requestBody, getLoggedInHeader());
    }

    public void verifyHearingIsCreated(final UUID hearingId, final int listedCaseSize) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"),
                        hearingId));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id",
                                        is(hearingId.toString())),
                                withJsonPath("$.listedCases.size()",
                                        is(listedCaseSize)))));

    }


    public void verifyHearingConfirmedEventForExtendHearingPublicMQ(final UUID allocatedHearingId, final UUID unAllocatedHearingId) throws IOException {
        final JsonPath jsRequest = new JsonPath(request);
        final List<String> newCaseIds = new ArrayList<>();
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);
        LOGGER.debug("jsonResponse from publicMessageConsumerHearingExtended: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("confirmedHearing.id"), is(allocatedHearingId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases.size()"), is(1));
        final String allocatedHearingCaseId = jsonResponse.get("confirmedHearing.prosecutionCases[0].id");

        final JsonPath jsonResponse1 = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);
        LOGGER.debug("jsonResponse from publicMessageConsumerHearingExtended: {}", jsonResponse.prettify());

        assertThat(jsonResponse1.get("confirmedHearing.id"), is(unAllocatedHearingId.toString()));
        assertThat(jsonResponse1.get("confirmedHearing.prosecutionCases.size()"), is(2));
        newCaseIds.add(jsonResponse1.get("confirmedHearing.prosecutionCases[0].id"));
        newCaseIds.add(jsonResponse1.get("confirmedHearing.prosecutionCases[1].id"));
        Assert.assertFalse(newCaseIds.contains(allocatedHearingCaseId));

        // mock public event from progression
        final String defendant1 = jsonResponse1.get("confirmedHearing.prosecutionCases[0].defendants[0].id");
        final String offence1 = jsonResponse1.get("confirmedHearing.prosecutionCases[0].defendants[0].offences[0].id");
        final String defendant2 = jsonResponse1.get("confirmedHearing.prosecutionCases[1].defendants[0].id");
        final String offence2 = jsonResponse1.get("confirmedHearing.prosecutionCases[1].defendants[0].offences[0].id");

        final String eventPayloadString = getStringFromResource("prosecution-case.json")
                .replaceAll("HEARING_ID", allocatedHearingId.toString())
                .replaceAll("CASE_ID_1", newCaseIds.get(0))
                .replaceAll("DEFENDANT_ID_1", defendant1)
                .replaceAll("OFFENCE_ID_1", offence1)
                .replaceAll("CASE_ID_2", newCaseIds.get(1))
                .replaceAll("DEFENDANT_ID_2", defendant2)
                .replaceAll("OFFENCE_ID_2", offence2);

        final JsonObject hearingExtendedDataObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        LOGGER.info("mocked public event : " + hearingExtendedDataObject.toString());

        QueueUtil.sendMessage(
                publicMessageProducerProgressionHearingExtendedEvent,
                PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED,
                hearingExtendedDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED, request, getLoggedInHeader());
    }

    public void verifyHearingConfirmedEventForExtendPartialHearingPublicMQ(final UUID allocatedHearingId, final UUID unAllocatedHearingId) throws IOException {
        final JsonPath jsRequest = new JsonPath(request);
        final List<String> newCaseIds = new ArrayList<>();
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);
        LOGGER.debug("jsonResponse from publicMessageConsumerHearingExtended: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("confirmedHearing.id"), is(allocatedHearingId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases.size()"), is(1));
        final String allocatedHearingCaseId = jsonResponse.get("confirmedHearing.prosecutionCases[0].id");

        final JsonPath jsonResponse1 = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);
        LOGGER.debug("jsonResponse from publicMessageConsumerHearingExtended: {}", jsonResponse.prettify());

        assertThat(jsonResponse1.get("confirmedHearing.id"), is(unAllocatedHearingId.toString()));
        assertThat(jsonResponse1.get("confirmedHearing.prosecutionCases.size()"), is(1));
        newCaseIds.add(jsonResponse1.get("confirmedHearing.prosecutionCases[0].id"));
        Assert.assertFalse(newCaseIds.contains(allocatedHearingCaseId));

        // mock public event from progression
        final String defendant1 = jsonResponse1.get("confirmedHearing.prosecutionCases[0].defendants[0].id");
        final String offence1 = jsonResponse1.get("confirmedHearing.prosecutionCases[0].defendants[0].offences[0].id");

        final String eventPayloadString = getStringFromResource("prosecution-case-partial-allocation.json")
                .replaceAll("HEARING_ID", allocatedHearingId.toString())
                .replaceAll("CASE_ID_1", newCaseIds.get(0))
                .replaceAll("DEFENDANT_ID_1", defendant1)
                .replaceAll("OFFENCE_ID_1", offence1);

        final JsonObject hearingExtendedDataObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        LOGGER.info("mocked public event : " + hearingExtendedDataObject.toString());

        QueueUtil.sendMessage(
                publicMessageProducerProgressionHearingExtendedEvent,
                PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED,
                hearingExtendedDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED, request, getLoggedInHeader());
    }

    public void verifyAddedCaseForHearingInActiveMQ(final UUID hearingId) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerAddedCaseForHearing);
        LOGGER.debug("jsonResponse from privateMessageConsumerAddedCaseForHearing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(hearingId.toString()));
        assertThat(jsonResponse.get("unAllocatedListedCases.size()"), is(2));
    }

    public void verifyHearingUpdatedToCaseInActiveMQ(final UUID allocatedHearingId, final UUID unallocatedHearingId) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingUpdatedToCase);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingUpdatedToCase: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("id"), is(unallocatedHearingId.toString()));
        assertThat(jsonResponse.get("existingHearingId"), is(allocatedHearingId.toString()));
    }

    public void verifyHearingDeletedInActiveMQ(final UUID hearingId) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDeleted);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingDeleted: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingIdToBeDeleted"), is(hearingId.toString()));
    }

    public void verifyHearingUpdatedPartiallyInActiveMQ(final UUID hearingId) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingPartiallyUpdated);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingPartiallyUpdated: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingIdToBeUpdated"), is(hearingId.toString()));
    }

    public void verifyHearingConfirmedInPublicMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyHearingConfirmedEvent();
    }

    public void createListingNotes(){
        this.hearingsData.getHearingData().stream().filter( hearing -> hearing.getCourtRoomId() != null).
                forEach( hearing -> notesSteps.createNoteForListing(hearing.getCourtRoomId(), "2020-05-21", "note 1"));
    }

    private void verifyHearingConfirmedEvent() {

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingConfirmed: {}", jsonResponse.prettify());

        verifyHearingPublicDetails(jsonResponse, "confirmedHearing");
    }

    private void verifyHearingPublicDetails(final JsonPath jsonResponse, final String publicEventType) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        Assert.assertThat(jsonResponse.get(publicEventType + ".id"), is(hearingData.getId().toString()));
        Assert.assertThat(jsonResponse.get(publicEventType + ".courtCentre.roomId"), is(hearingData.getCourtRoomId().toString()));
        Assert.assertThat(jsonResponse.get(publicEventType + ".courtCentre.id"), is(hearingData.getCourtCentreId().toString()));
        Assert.assertThat(jsonResponse.get(publicEventType + ".courtCentre.name"), is("Liverpool Crown Court"));
        Assert.assertThat(jsonResponse.get(publicEventType + ".courtApplicationIds[0]"), is(hearingData.getCourtApplications().get(0).getId().toString()));
    }

    private static String getStringFromResource(final String path) throws IOException {
        return Resources.toString(getResource(path), defaultCharset());
    }
}
