package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.services.common.converter.LocalDates.to;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonObject;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.getHearingFilter;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingByWeekCommencing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollUntilHearingIsPresent;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.DefenceServiceStub.stubDefenceQueryApiForSearchCasesByOrganisationDefendant;
import static uk.gov.moj.cpp.listing.utils.DefenceServiceStub.stubDefenceQueryApiForSearchCasesByPersonDefendant;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.setupAsAuthorizedUserToQueryCaseByDefendantAndHearingDate;

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
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.ApplicantRespondentData;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.PathNotFoundException;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListCourtHearingSteps extends AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCourtHearingSteps.class);

    private static final String LISTING_COMMAND_LIST_COURT_HEARING = "listing.command.list-court-hearing";
    private static final String LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING = "listing.command.extend-hearing-for-hearing";
    private static final String MEDIA_TYPE_LIST_COURT_HEARING = "application/vnd.listing.command.list-court-hearing+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing.search.hearing+json";
    private static final String MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING = "application/vnd.listing.command.extend-hearing-for-hearing+json";
    private static final String MEDIA_TYPE_SEARCH_BY_PERSON_DEFENDANT_AND_HEARING_DATE = "application/vnd.listing.get.cases-by-person-defendant+json";
    private static final String MEDIA_TYPE_SEARCH_BY_ORGANISATION_DEFENDANT_AND_HEARING_DATE = "application/vnd.listing.get.cases-by-organisation-defendant+json";

    private static final String EVENT_SELECTED_HEARING_UPDATED_TO_CASE = "listing.events.hearing-updated-to-case";
    private static final String EVENT_SELECTED_HEARING_PARTIALLY_UPDATED = "listing.events.hearing-partially-updated";
    private static final String PUBLIC_EVENT_SELECTED_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PUBLIC_LISTING_HEARING_LISTED = "public.listing.hearing-listed";
    private static final String PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED = "public.listing.hearing-partially-updated";
    private static final String PUBLIC_LISTING_HEARING_CHANGES_SAVED = "public.listing.hearing-changes-saved";
    private static final String PUBLIC_EVENT_APPLICATION_ADD_COURT_APPLICATION_FOR_HEARING = "public.listing.court-application-added-for-hearing";
    private static final String LISTING_EVENTS_HEARING_DAY_COURT_SCHEDULE_UPDATED = "listing.events.hearing-day-court-schedule-updated";

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
    private static final UUID JUDICIAL_RESULT_ID = UUID.fromString("065b6fcb-0787-4f0d-a9cd-af4b5c36e047");
    public static final int OFFENCE_COUNT = 1;
    public static final int OFFENCE_ORDER_INDEX = 0;
    public static final String OFFENCE_LEGISLATION = "legislation";
    private static final boolean POSSIBLE_DISQUALIFICATION_FLAG = true;

    private HearingsData hearingsData;
    private final JmsMessageConsumerClient privateMessageConsumerHearingUpdatedToCase;
    private final JmsMessageConsumerClient privateMessageConsumerHearingDayScheduleUpdated;
    private final JmsMessageConsumerClient publicMessageConsumerHearingConfirmedForExtendHearing;
    private final JmsMessageConsumerClient publicEventHearingListed;
    private final JmsMessageConsumerClient publicMessageConsumerHearingPartiallyUpdated;
    private final JmsMessageConsumerClient publicMessageConsumerHearingChangesSaved;

    private final JmsMessageProducerClient publicMessageProducerProgressionHearingExtendedEvent;
    private JmsMessageConsumerClient publicMessageConsumerCourtApplicationAddedForHearing;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);
    protected String request;

    private NotesSteps notesSteps = new NotesSteps();

    public ListCourtHearingSteps(final HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        privateMessageConsumerHearingUpdatedToCase = privateEvents.createPrivateConsumer(EVENT_SELECTED_HEARING_UPDATED_TO_CASE);
        privateMessageConsumerHearingDayScheduleUpdated = privateEvents.createPrivateConsumer(LISTING_EVENTS_HEARING_DAY_COURT_SCHEDULE_UPDATED);
        publicMessageConsumerHearingConfirmedForExtendHearing = publicEvents.createPublicConsumer(PUBLIC_EVENT_SELECTED_HEARING_CONFIRMED);
        publicMessageProducerProgressionHearingExtendedEvent = publicEvents.createPublicProducer();
        publicEventHearingListed = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_LISTED);
        publicMessageConsumerHearingPartiallyUpdated = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED);
        publicMessageConsumerHearingChangesSaved = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_CHANGES_SAVED);
        publicMessageConsumerCourtApplicationAddedForHearing = publicEvents.createPublicConsumer(PUBLIC_EVENT_APPLICATION_ADD_COURT_APPLICATION_FOR_HEARING);
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public ListCourtHearingSteps(final HearingsData hearingsData, final Boolean split) {
        privateMessageConsumerHearingUpdatedToCase = null;
        publicMessageConsumerHearingConfirmedForExtendHearing = null;
        publicMessageConsumerHearingChangesSaved = null;
        publicMessageProducerProgressionHearingExtendedEvent = null;
        privateMessageConsumerHearingDayScheduleUpdated = null;
        this.hearingsData = hearingsData;

        publicEventHearingListed = publicEvents.createPrivateConsumer(PUBLIC_LISTING_HEARING_LISTED);
        publicMessageConsumerHearingPartiallyUpdated = publicEvents.createPrivateConsumer(PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public ListCourtHearingSteps() {
        privateMessageConsumerHearingUpdatedToCase = privateEvents.createPrivateConsumer(EVENT_SELECTED_HEARING_UPDATED_TO_CASE);
        privateMessageConsumerHearingDayScheduleUpdated = privateEvents.createPrivateConsumer(LISTING_EVENTS_HEARING_DAY_COURT_SCHEDULE_UPDATED);
        publicMessageConsumerHearingConfirmedForExtendHearing = publicEvents.createPublicConsumer(PUBLIC_EVENT_SELECTED_HEARING_CONFIRMED);
        publicMessageProducerProgressionHearingExtendedEvent = publicEvents.createPublicProducer();
        publicEventHearingListed = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_LISTED);
        publicMessageConsumerHearingPartiallyUpdated = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED);
        publicMessageConsumerHearingChangesSaved = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_CHANGES_SAVED);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
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

    public void whenCaseIsSubmittedForListingWithJudicialId(final UUID judicialId) {
        final Response response = getResponseCaseSubmittedForListing(false, judicialId);
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

    public void whenCaseIsSubmittedAndListed() {
        final Response response = getResponseWhenCaseSubmittedIsForListing(false);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenProgressionHearingExtended() {
        final String eventPayloadString = getPayload("prosecution-case-with-shadow-listed-offences.json")
                .replaceAll("HEARING_ID", hearingsData.getHearingData().get(0).getId().toString())
                .replaceAll("CASE_ID_1", UUID.randomUUID().toString())
                .replaceAll("DEFENDANT_ID_1", UUID.randomUUID().toString())
                .replaceAll("OFFENCE_ID_1", UUID.randomUUID().toString())
                .replaceAll("CASE_ID_2", UUID.randomUUID().toString())
                .replaceAll("DEFENDANT_ID_2", UUID.randomUUID().toString())
                .replaceAll("OFFENCE_ID_2", UUID.randomUUID().toString());

        JsonObject hearingExtendedDataObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        sendMessage(
                publicMessageProducerProgressionHearingExtendedEvent,
                PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED,
                hearingExtendedDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
    }

    public void verifyPublicEventCourtApplicationAdded() {
        Optional<JsonPath> jsonResponse = publicMessageConsumerCourtApplicationAddedForHearing.retrieveMessageAsJsonPath();
        assertTrue(jsonResponse.isPresent());
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

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING, request, getLoggedInHeader());
    }

    private Response getResponseCaseSubmittedForListing(final boolean isStandaloneApp, final UUID judicialId) {

        stubReferenceDataForFirstHearing(judicialId);

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

    protected void stubReferenceDataForFirstHearing(final UUID judicialId) {

        hearingsData.getHearingData().stream()
                .map(HearingData::getCourtCentreId)
                .forEach(cci -> {
                    stubGetReferenceDataCourtCentre(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, hearingsData.getHearingData().get(0).getCourtRoomId(), hearingsData.getHearingData().get(0).getName()));
                    stubGetReferenceDataCourtCentreById(cci);
                    stubGetReferenceDataCourtMappings(new CourtCentreData(cci, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, hearingsData.getHearingData().get(0).getCourtRoomId(), hearingsData.getHearingData().get(0).getName()));
                });
        hearingsData.getHearingData().forEach(hearingData -> stubGetReferenceDataHearingTypes(hearingData.getHearingTypeData().getTypeId()));
        hearingsData.getHearingData().stream().filter(hd -> hd.getJudiciary() != null)
                .forEach(hearingData -> stubGetReferenceDataJudiciaries(judicialId));
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

    private Response getResponseWhenCaseSubmittedIsForListing(final boolean isStandaloneApp) {

        stubReferenceDataForFirstHearing();

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        ListCourtHearing listCourtHearingData;
        if (isStandaloneApp) {
            listCourtHearingData = getListCourtHearingDataStandaloneApplication(hearingsData);
        } else {
            listCourtHearingData = getListForCourtHearingData(hearingsData);
        }
        final JsonObject listCourtHearingJsonObject = (JsonObject) objectToJsonValueConverter.convert(listCourtHearingData);

        request = listCourtHearingJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(caseReferenceFilter),
                withJsonPath(lastNameFilter)
        });
    }

    /**
     * JMS-aware version of verifyHearingListedFromAPI for handling asynchronous message processing timing issues.
     */
    public void verifyHearingListedFromAPIWithJmsDelay(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        // Use JMS-aware polling to handle asynchronous message processing
        pollForHearingWithJmsDelay(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(caseReferenceFilter),
                withJsonPath(lastNameFilter)
        });
    }
    public void verifyHearingDayCourtScheduledUpdated() {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final String hearingIdFilter = getHearingFilter(hearingData.getId().toString());
        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), true, getLoggedInUser().toString(),
                new Matcher[]{
                        withJsonPath("$.hearings[0].hearingDays[*].courtScheduleId", is(notNullValue())),
                        withJsonPath(hearingIdFilter + "jurisdictionType", hasItem(hearingData.getJurisdictionType())),
                        withJsonPath(hearingIdFilter + "courtCentreId", hasItem(hearingData.getCourtCentreId().toString())),
                        withJsonPath(hearingIdFilter + "type.id", hasItem(hearingData.getHearingTypeData().getTypeId().toString())),
                        withJsonPath(hearingIdFilter + "type.description", hasItem(hearingData.getHearingTypeData().getTypeDescription())),
                        withJsonPath(hearingIdFilter + "startDate", hasItem(hearingData.getHearingStartDate().toString())),
                        withJsonPath(hearingIdFilter + "hearingLanguage", hasItem("ENGLISH"))
                });
    }

    public void verifyHearingDayCourtScheduledUpdated(final UUID updatedCourtScheduleId) {
        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final String url = generateUrlForFindingAHearingById(hearingId.toString());
        final ResponseData resp = poll(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(
                                allOf(withJsonPath("$.id", equalTo(hearingId.toString())),
                                        withJsonPath("$.hearingDays[0].courtScheduleId", equalTo(updatedCourtScheduleId.toString())))));
    }

    public void verifyQueryAPIFindCaseByPersonDefendantAndHearingDate() {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final String caseId = hearingData.getListedCases().get(0).getCaseId().toString();
        final String urn = hearingData.getListedCases().get(0).getCaseReference();
        final String defendantId = hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString();

        final String firstName = hearingData.getListedCases().get(0).getDefendants().get(0).getFirstName();
        final String lastName = hearingData.getListedCases().get(0).getDefendants().get(0).getLastName();
        final LocalDate dateOfBirth = hearingData.getListedCases().get(0).getDefendants().get(0).getDateOfBirth();


        verifyCaseByPersonDefendantAndHearingDate(caseId, urn, defendantId, firstName, lastName, dateOfBirth.toString());
    }

    public void verifyQueryAPIFindCaseByPersonDefendantAndHearingDateForUnallocatedHearing(final String caseId, final String urn, final String defendantId,
                                                                                           final String firstName, final String lastName, final String dateOfBirth) {
        verifyCaseByPersonDefendantAndHearingDate(caseId, urn, defendantId, firstName, lastName, dateOfBirth);
    }

    private void verifyCaseByPersonDefendantAndHearingDate(final String caseId, final String urn, final String defendantId,
                                                           final String firstName, final String lastName, final String dateOfBirth) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.get.cases-by-person-defendant"), firstName, lastName, dateOfBirth, LocalDate.now()));


        setupAsAuthorizedUserToQueryCaseByDefendantAndHearingDate(getLoggedInUser());
        stubDefenceQueryApiForSearchCasesByPersonDefendant(caseId, defendantId);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_BY_PERSON_DEFENDANT_AND_HEARING_DATE).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCases[0].caseId",
                                        equalTo(caseId)),
                                withJsonPath("$.prosecutionCases[0].urn",
                                        equalTo(urn))))
                );
    }

    public void verifyQueryAPIFindCaseByOrganisationDefendantAndHearingDate() {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final String caseId = hearingData.getListedCases().get(0).getCaseId().toString();
        final String urn = hearingData.getListedCases().get(0).getCaseReference();
        final String defendantId = hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString();
        final String organisationName = hearingData.getListedCases().get(0).getDefendants().get(0).getLegalEntityDefendant().getOrganisation().getName();

        verifyCaseByOrganisationDefendantAndHearingDate(caseId, urn, defendantId, organisationName);
    }

    private void verifyCaseByOrganisationDefendantAndHearingDate(final String caseId, final String urn, final String defendantId, final String organisationName) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.get.cases-by-organisation-defendant"), organisationName, LocalDate.now()));

        stubDefenceQueryApiForSearchCasesByOrganisationDefendant(caseId, defendantId);
        setupAsAuthorizedUserToQueryCaseByDefendantAndHearingDate(getLoggedInUser());

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_BY_ORGANISATION_DEFENDANT_AND_HEARING_DATE).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCases[0].caseId",
                                        equalTo(caseId)),
                                withJsonPath("$.prosecutionCases[0].urn",
                                        equalTo(urn))))
                );
    }

    public void verifyPublicEventHearingListed() {
        final JsonPath jsonResponse = retrieveMessage(publicEventHearingListed);
        LOGGER.info("jsonResponse from publicEventHearingListed: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearing.id"), is(hearingsData.getHearingData().get(0).getId().toString()));
    }

    public void verifyHearingListedWithAnyAllocationFromAPI(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.any-allocation.search.hearings"), hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseReference(), isAllocated));

        verifyHearingListedFromWithApiUrl(hearingData, searchHearingUrl);
    }

    public void verifyHearingListedForCotr(final String courtCentreId, final String startDate, final String endDate) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.cotr.search.hearings"), courtCentreId, startDate, endDate));

        verifyHearingListedFromWithApiUrl(hearingData, searchHearingUrl);
    }

    public void verifyHearingListedFromWithApiUrl(final HearingData hearingData, final String searchHearingUrl) {
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), new Matcher[]{
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
                        equalTo(true))
        });
    }

    private Matcher getNotesMatcher(boolean isAllocated, boolean noteExist) {
        if (!isAllocated || !noteExist) {
            return withJsonPath("$.notes.size()", is(0));
        } else {
            final AtomicInteger index = new AtomicInteger(0);
            final List<Matcher> noteMatchers = hearingsData.getHearingData().stream().
                    filter(hearing -> hearing.getCourtRoomId() != null).
                    limit(1).
                    map(hearing -> allOf(withJsonPath("$.notes[" + index.get() + "].courtRoomId", equalTo(hearing.getCourtRoomId().toString())),
                            withJsonPath("$.notes[" + index.get() + "].date", equalTo(String.valueOf(hearing.getHearingStartDate()))),
                            withJsonPath("$.notes[" + index.getAndIncrement() + "].note", equalTo("note 1")))).
                    collect(Collectors.toList());
            noteMatchers.add(withJsonPath("$.notes.size()", is(noteMatchers.size())));
            return allOf(noteMatchers.toArray(new Matcher[noteMatchers.size()]));
        }
    }

    public void verifyHearingListedWithHearingDaysCourtSchedule(final boolean isAllocated, final String[] courtScheduleSlots, final String[] courtRoomIds) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].id",
                        equalTo(hearingData.getId().toString())),
                withJsonPath("$.hearings[0].jurisdictionType",
                        equalTo(hearingData.getJurisdictionType())),
                withJsonPath("$.hearings[0].courtCentreId",
                        equalTo(hearingData.getCourtCentreId().toString())),
                withJsonPath("$.hearings[0].courtRoomId",
                        equalTo(hearingData.getCourtRoomId().toString())),
                withJsonPath("$.hearings[0].type.id",
                        equalTo(hearingData.getHearingTypeData().getTypeId().toString())),
                withJsonPath("$.hearings[0].startDate",
                        equalTo(hearingData.getHearingStartDate().toString())),
                withJsonPath("$.hearings[0].hearingDays[0].hearingDate",
                        equalTo(courtScheduleSlots[0])),
                withJsonPath("$.hearings[0].hearingDays[0].courtScheduleId",
                        equalTo(courtRoomIds[0]))
        });
    }

    public void verifyHearingUpdatedWithHearingDaysCourtSchedule(final UpdatedHearingData updatedHearingData) {
        pollForHearing(updatedHearingData.getCourtCentreId().toString(), true, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].id", equalTo(updatedHearingData.getHearingId().toString())),
        });
    }

    public void verifyHearingListedWithHearingDays(final boolean isAllocated, final String[] courtScheduleSlots, final String[] courtRoomIds) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(
                hearingData, listedCaseData, defendant, defendant.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(
                hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        String courtCentreId = hearingData.getCourtCentreId().toString();
        String userId = getLoggedInUser().toString();

        // Keep only caseReferenceFilter in poll for initial verification
        // Use JMS-aware polling to handle asynchronous message processing
        String jsonResponse = pollForHearingWithJmsDelay(courtCentreId, isAllocated, userId, new Matcher[]{
                withJsonPath(caseReferenceFilter) });

        List<String> failedAssertions = new ArrayList<>();

        // Check other matchers separately for clearer debugging
        validateJsonPath(jsonResponse, lastNameFilter, failedAssertions, "lastName");

        Map<String, Object> expectedValues = buildExpectedJsonValues(hearingData, courtScheduleSlots, courtRoomIds);

        for (Map.Entry<String, Object> entry : expectedValues.entrySet()) {
            try {
                Object actualValue = read(jsonResponse, entry.getKey());
                if (!Objects.equals(actualValue, entry.getValue())) {
                    failedAssertions.add(String.format("Mismatch at path '%s': expected '%s', but was '%s'",
                            entry.getKey(), entry.getValue(), actualValue));
                }
            } catch (PathNotFoundException e) {
                failedAssertions.add("Missing path: " + entry.getKey());
            }
        }

        if (!failedAssertions.isEmpty()) {
            fail("Following JSONPath assertions failed:\n" + String.join("\n", failedAssertions));
        }
    }

    private void validateJsonPath(String json, com.jayway.jsonpath.JsonPath path,
                                  List<String> failedAssertions, String label) {
        try {
            Object result = path.read(json);
            if (result == null || (result instanceof Collection && ((Collection<?>) result).isEmpty())) {
                failedAssertions.add("Failed JsonPath check: " + label);
            }
        } catch (Exception e) {
            failedAssertions.add("Invalid JsonPath or value missing: " + label + " - " + e.getMessage());
        }
    }

    private Map<String, Object> buildExpectedJsonValues(HearingData hearingData, String[] courtScheduleSlots, String[] courtRoomIds) {
        Map<String, Object> expected = new LinkedHashMap<>();

        expected.put("$.hearings[0].id", hearingData.getId().toString());
        expected.put("$.hearings[0].jurisdictionType", hearingData.getJurisdictionType());
        expected.put("$.hearings[0].courtCentreId", hearingData.getCourtCentreId().toString());
        expected.put("$.hearings[0].courtRoomId", hearingData.getCourtRoomId().toString());

        expected.put("$.hearings[0].type.id", hearingData.getHearingTypeData().getTypeId().toString());
        expected.put("$.hearings[0].type.description", hearingData.getHearingTypeData().getTypeDescription());
        expected.put("$.hearings[0].startDate", hearingData.getHearingStartDate().toString());

        // You would continue this pattern for courtApplications, applicants, respondents, etc.

        for (int i = 0; i < courtScheduleSlots.length; i++) {
            expected.put("$.hearings[0].hearingDays[" + i + "].hearingDate", courtScheduleSlots[i]);
            expected.put("$.hearings[0].hearingDays[" + i + "].courtRoomId", courtRoomIds[i]);
        }

        expected.put("$.hearings[0].listedCases[0].defendants[0].isYouth", true);

        return expected;
    }




    public void verifyHearingIsNotListed(final boolean allocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        pollForHearing(hearingData.getCourtCentreId().toString(), allocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("hearings", hasSize(0))
        });
    }


    public void verifyHearingListedFromAPIAllocatedForBookSlots() {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), true, getLoggedInUser().toString(), new Matcher[]{
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

        });
    }

    public void verifyHearingListedWithWeekCommencingFromAPI(final boolean isAllocated, final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        //This is how we poll week commencing hearings from UI
        pollForHearingByWeekCommencing(
                hearingData.getCourtCentreId().toString(), 
                false, // isAllocated should be false always as specified
                "1970-01-01", // weekCommencingStartDate
                "2100-12-31", // weekCommencingEndDate
                getLoggedInUser().toString(), 
                new Matcher[]{
                    withJsonPath("$.hearings[0].id",
                            equalTo(hearingData.getId().toString())),
                    withJsonPath("$.hearings[0].weekCommencingStartDate", equalTo(FORMATTER.format(weekCommencingStartDate))),
                    withJsonPath("$.hearings[0].weekCommencingEndDate", equalTo(FORMATTER.format(weekCommencingStartDate.plusWeeks(weekCommencingDuration).minusDays(1)))),
                    withJsonPath("$.hearings[0].weekCommencingDurationInWeeks", equalTo(weekCommencingDuration))
                }
        );
    }

    public void verifyHearingForWeekCommencingRange(final String jurisdictionType, final String weekCommencingStartDate, final String weekCommencingEndDate, final boolean allocated, final Matcher... matchers) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings.for.week.commencing.range"), jurisdictionType, weekCommencingStartDate, weekCommencingEndDate, allocated));

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), matchers);
    }

    public void verifyHearingListedWithLegalEntity(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), isAllocated));

        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);

        final DefendantData defendant = listedCaseData.getDefendants().get(0);

        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, defendant, listedCaseData.getCaseReference());

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), new Matcher[]{
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
        });
    }

    public void verifyHearingListedFromAPIForStandaloneApplication(final boolean isAllocated) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
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
        });
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

    /**
     * JMS-aware version of verifyAvailableHearingListedForCaseInHearingAndCaseUrn for handling asynchronous message processing timing issues.
     */
    public void verifyAvailableHearingListedForCaseInHearingAndCaseUrnWithJmsDelay(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId) {

        final String searchHearingUrl1 = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam()));

        verifyHearingDetailsWithJmsDelay(caseAndDefendantData, masterDefendantId, searchHearingUrl1);

        final String searchHearingUrl2 = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-case.urn"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getCaseUrnQueryParam()));

        verifyHearingDetailsWithJmsDelay(caseAndDefendantData, masterDefendantId, searchHearingUrl2);

    }

    public String verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDateCourtCalendar() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearingscourt.calendar.by.allocated.court-centre-id.start-date.end-date"),
                        ALLOCATED,
                        hearingsData.getHearingData().get(0).getCourtCentreId(),
                        LocalDate.parse("2020-01-01"),
                        hearingsData.getHearingData().get(0).getHearingEndDate()));

       return  pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingsData.getHearingData().get(0).getId().toString(), "application/vnd.listing.search.hearings.court.calendar+json", 2);
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
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0))
                        )));
    }

    /**
     * JMS-aware version of verifyHearingDetails for handling asynchronous message processing timing issues.
     */
    private void verifyHearingDetailsWithJmsDelay(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId, final String searchHearingUrl) {
        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        not(caseAndDefendantData.getHearingId())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].allocated",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().toString())),
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
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        not(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0))
                        )));
    }

    /**
     * JMS-aware version of verifyAvailableHearingListedForMatchedDefendant for handling asynchronous message processing timing issues.
     */
    public void verifyAvailableHearingListedForMatchedDefendantWithJmsDelay(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-jurisdiction.type"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam()));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        not(caseAndDefendantData.getHearingId())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].allocated",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        not(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0))
                        )));
    }

    public void verifyAllAvailableHearingListedForMatchedDefendant(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-jurisdiction.type.with.return.all.hearings"),
                        caseAndDefendantData.getCaseUrnQueryParam(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam(),
                        "true"));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings.size()", is(2)),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.hearings[1].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[1].endDate",
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[1].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[1].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0)),
                                anyOf(allOf(withJsonPath("$.hearings[0].allocated", equalTo(true)),
                                                withJsonPath("$.hearings[1].allocated", equalTo(false))),
                                        allOf(withJsonPath("$.hearings[0].allocated", equalTo(false)),
                                                withJsonPath("$.hearings[1].allocated", equalTo(true))))
                        )));
    }

    /**
     * JMS-aware version of verifyAllAvailableHearingListedForMatchedDefendant for handling asynchronous message processing timing issues.
     */
    public void verifyAllAvailableHearingListedForMatchedDefendantWithJmsDelay(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-jurisdiction.type.with.return.all.hearings"),
                        caseAndDefendantData.getCaseUrnQueryParam(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam(),
                        "true"));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings.size()", is(2)),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.hearings[1].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[1].endDate",
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[1].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[1].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(masterDefendantId.toString())),
                                withJsonPath("$.notes.size()",
                                        equalTo(0)),
                                anyOf(allOf(withJsonPath("$.hearings[0].allocated", equalTo(true)),
                                                withJsonPath("$.hearings[1].allocated", equalTo(false))),
                                        allOf(withJsonPath("$.hearings[0].allocated", equalTo(false)),
                                                withJsonPath("$.hearings[1].allocated", equalTo(true))))
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

    /**
     * JMS-aware version of verifyAvailableHearingListedForCaseInHearingAndMatchedDefendant for handling asynchronous message processing timing issues.
     */
    public void verifyAvailableHearingListedForCaseInHearingAndMatchedDefendantWithJmsDelay(final CaseAndDefendantData caseAndDefendantData) {

        final String searchHearingUrlForCrown = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-jurisdiction.type"),
                        caseAndDefendantData.getHearingId(),
                        caseAndDefendantData.getSearchCriteria(),
                        caseAndDefendantData.getJurisdictionTypeQueryParam()));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrlForCrown, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
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
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(caseAndDefendantData.getMasterDefendantId().toString())),
                                getNotesMatcher(true, notesExists)
                        )));
    }

    /**
     * JMS-aware version of verifyAvailableHearing for handling asynchronous message processing timing issues.
     */
    public void verifyAvailableHearingWithJmsDelay(final CaseAndDefendantData caseAndDefendantData, final UUID masterDefendantId, final boolean notesExists) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-defendant.ids"),
                        masterDefendantId.toString()));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(caseAndDefendantData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(JurisdictionType.CROWN.name())),
                                withJsonPath("$.hearings[0].allocated",
                                        equalTo(true)),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(LocalDate.now().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].caseIdentifier.caseReference",
                                        equalTo(caseAndDefendantData.getCaseUrn())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(caseAndDefendantData.getMasterDefendantId().toString())),
                                getNotesMatcher(true, notesExists)
                        )));
    }

    public void verifyAvailableHearingNotExists(final UUID masterDefendantId) {

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

    /**
     * JMS-aware version of verifyAvailableHearingNotExists for handling asynchronous message processing timing issues.
     */
    public void verifyAvailableHearingNotExistsWithJmsDelay(final UUID masterDefendantId) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.available.search.hearings-defendant.ids"),
                        masterDefendantId.toString()));

        // Use JMS-aware polling to handle asynchronous message processing
        pollWithDelayForJms(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings.size()",
                                        equalTo(0)),
                                getNotesMatcher(true, false)
                        )));
    }

    public void verifyNotesViaRangeSearch() {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings.by.week.commencing"),
                        to(hearingData.getHearingStartDate()), to(hearingData.getHearingStartDate().plusDays(7)), hearingData.getCourtCentreId(), ALLOCATED));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings.size()", equalTo(1)),
                                withJsonPath("$.hearings[0].id", equalTo(hearingData.getId().toString())),
                                getNotesMatcher(true, true)
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
                .withAdjournedFromDate(LocalDate.now().toString())
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withBookedSlots(hearingData.getBookedSlots())
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(hearingData.getCourtRoomId())
                                .build())
                        .withBookingReference(randomUUID())
                        .withCourtApplications(singletonList(getCourtApplication(hearingData)))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                        .withEndDate(hearingData.getHearingEndDate() != null ? hearingData.getHearingEndDate().toString() : null)
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(hearingData.getJudiciary() != null
                                ? singletonList(getJudicialRole(hearingData))
                                : null)
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? null :
                                WeekCommencingDate.weekCommencingDate()
                                        .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                        .withDuration(hearingData.getWeekCommencingDuration())
                                        .build())
                        .withType(getHearingType(hearingData))
                        .withReportingRestrictionReason(hearingData.getReportingRestrictionReason())
                        .withIsGroupProceedings(false)
                        .build())).build();
    }

    private JudicialRole getJudicialRole(final HearingData hearingData) {
        return JudicialRole.judicialRole()
                .withJudicialId(hearingData.getJudiciary().get(0).getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                        .build())
                .withIsDeputy(hearingData.getJudiciary().get(0).getIsDeputy().orElse(null))
                .withIsBenchChairman(hearingData.getJudiciary().get(0).getIsBenchChairman().orElse(null))
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

        // Determine if hearing is allocated (has court room) or unallocated
        final boolean isAllocated = hearingData.getCourtRoomId() != null;

        return ListCourtHearing.listCourtHearing()
                .withAdjournedFromDate(LocalDate.now().toString())
                .withShadowListedOffences(shadowListedOffences)
                .withHearings(List.of(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(hearingData.getCourtRoomId())
                                .build())
                        // Only add booking reference for allocated hearings
                        .withBookingReference(isAllocated ? randomUUID() : null)
                        .withListedStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                        .withCourtApplications(isNull(hearingData.getCourtApplications()) ? null : singletonList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(hearingData.getCourtApplications().get(0).getLinkedCaseId())
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withCaseURN(STRING.next())
                                                .withProsecutionAuthorityId(randomUUID())
                                                .withProsecutionAuthorityCode(STRING.next()).build())
                                        .withIsSJP(false)
                                        .withCaseStatus("ACTIVE")
                                        .withOffences(singletonList(Offence.offence().withId(hearingData.getCourtApplications().get(0).getOffenceId())
                                                .withOffenceDefinitionId(randomUUID())
                                                .withOffenceCode(STRING.next())
                                                .withOffenceTitle(STRING.next())
                                                .withWording(STRING.next())
                                                .withCount(OFFENCE_COUNT)
                                                .withOrderIndex(OFFENCE_ORDER_INDEX)
                                                .withOffenceLegislation(OFFENCE_LEGISLATION)
                                                .withStartDate(LocalDate.now().toString())
                                                .build()))
                                        .build()))
                                .withParentApplicationId(hearingData.getCourtApplications().get(0).getParentApplicationId())
                                .withType(getCourtApplicationType(hearingData))
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withApplicationReference(STRING.next())
                                .withApplicationParticulars(hearingData.getCourtApplications().get(0).getApplicationParticulars())
                                .withApplicationStatus(ApplicationStatus.LISTED)
                                .withApplicant(ListCourtHearingSteps.this.getApplicant(hearingData.getCourtApplications().get(0).getApplicant()))
                                .withRespondents(getRespondents(hearingData))
                                .withSubject(ListCourtHearingSteps.this.getApplicant(hearingData.getCourtApplications().get(0).getApplicant()))
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                        .withEndDate(hearingData.getHearingEndDate() != null ? hearingData.getHearingEndDate().toString() : null)
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(isNotEmpty(hearingData.getJudiciary())
                                ? singletonList(JudicialRole.judicialRole()
                                .withJudicialId(hearingData.getJudiciary().get(0).getJudicialId())
                                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())
                                        .withJudicialRoleTypeId(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                                        .build())
                                .withIsDeputy(hearingData.getJudiciary().get(0).getIsDeputy().orElse(null))
                                .withIsBenchChairman(hearingData.getJudiciary().get(0).getIsBenchChairman().orElse(null))
                                .withUserId(hearingData.getJudiciary().get(0).getUserId())
                                .build())
                                : null)
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? null :
                                WeekCommencingDate.weekCommencingDate()
                                        .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                        .withDuration(hearingData.getWeekCommencingDuration())
                                        .build())
                        .withProsecutionCases(hearingData.getListedCases().stream()
                                .map(lc -> ProsecutionCase.prosecutionCase().withId(lc.getCaseId())
                                        .withInitiationCode(InitiationCode.C)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityCode(lc.getAuthorityCode())
                                                .withProsecutionAuthorityId(lc.getAuthorityId())
                                                .withProsecutionAuthorityReference(lc.getCaseReference())
                                                .build())
                                        .withProsecutor(Prosecutor.prosecutor().withProsecutorId(lc.getAuthorityId()).withProsecutorCode(lc.getAuthorityCode()).build())
                                        .withCaseMarkers(singletonList(Marker.marker()
                                                .withId(randomUUID())
                                                .withMarkerTypeCode("C")
                                                .withMarkerTypeDescription("Description")
                                                .withMarkerTypeid(randomUUID()).build()))
                                        .withDefendants(lc.getDefendants().stream().map(d -> Defendant.defendant()
                                                        .withId(d.getDefendantId())
                                                        .withMasterDefendantId(d.getMasterDefendantId())
                                                        .withCourtProceedingsInitiated(ZonedDateTime.now())
                                                        .withIsYouth(d.getIsYouth())
                                                        .withPersonDefendant(gerPersonDefendant(d))
                                                        .withAssociatedPersons(singletonList(AssociatedPerson.associatedPerson()
                                                                .withRole(STRING.next())
                                                                .withPerson(getPerson(d))
                                                                .build()))
                                                        .withOffences(d.getOffences().stream()
                                                                .map(o -> Offence.offence()
                                                                        .withCount(OFFENCE_COUNT)
                                                                        .withId(o.getOffenceId())
                                                                        .withOffenceCode(STRING.next())
                                                                        .withOffenceDefinitionId(randomUUID())
                                                                        .withWording(STRING.next())
                                                                        .withStartDate(LocalDate.now().toString())
                                                                        .withOrderIndex(OFFENCE_ORDER_INDEX)
                                                                        .withOffenceTitle(o.getStatementOfOffenceTitle())
                                                                        .withOffenceLegislation(OFFENCE_LEGISLATION)
                                                                        .withLaaApplnReference(
                                                                                LaaReference.laaReference()
                                                                                        .withApplicationReference(STRING.next())
                                                                                        .withStatusCode(STRING.next())
                                                                                        .withStatusDate((format(LocalDate.now().toString())))
                                                                                        .withStatusDescription(STRING.next())
                                                                                        .withStatusId(randomUUID()).build())
                                                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction().withId(randomUUID())
                                                                                .withLabel("RestrictionApplied")
                                                                                .withJudicialResultId(JUDICIAL_RESULT_ID)
                                                                                .withOrderedDate(LocalDate.now().toString()).build()))
                                                                        .build())
                                                                .collect(Collectors.toList()))
                                                        .withProsecutionCaseId(listedCaseData.getCaseId())
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                        .withDefendantListingNeeds(hearingData.getListedCases().stream()
                                .map(lc -> lc.getDefendants().stream().map(d ->
                                                getDefendantListingNeeds(lc, d))
                                        .collect(Collectors.toList()))
                                .flatMap(List::stream)
                                .collect(Collectors.toList()))

                        .withType(getHearingType(hearingData))
                        .withReportingRestrictionReason(hearingData.getReportingRestrictionReason())
                        .withIsGroupProceedings(false)
                        .build())).build();

    }

    private Person getPerson(final DefendantData d) {
        return Person.person()
                .withAdditionalNationalityId(randomUUID())
                .withGender(Gender.FEMALE)
                .withLastName(d.getLastName())
                .withNationalityId(randomUUID())
                .withTitle(PERSON_TITLE)
                .withEthnicity(Ethnicity.ethnicity()
                        .withObservedEthnicityId(randomUUID())
                        .withObservedEthnicityDescription(STRING.next())
                        .build())
                .build();
    }

    private CourtApplicationParty getApplicant(final ApplicantRespondentData applicant) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(applicant.getId())
                .withPersonDetails(Person.person().withLastName(applicant.getLastName())
                        .withFirstName(applicant.getFirstName())
                        .withGender(Gender.FEMALE)
                        .withAddress(getAddress(applicant.getAddress()))
                        .build())
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();
    }

    private Address getAddress(final uk.gov.moj.cpp.listing.domain.Address address) {
        return Address
                .address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2().orElse(null))
                .withAddress3(address.getAddress3().orElse(null))
                .withAddress4(address.getAddress4().orElse(null))
                .withAddress5(address.getAddress5().orElse(null))
                .withPostcode(address.getPostcode().orElse(null))
                .build();
    }

    private Address buildAddress() {
        return Address.address()
                .withAddress1(STRING.next())
                .withAddress2(STRING.next())
                .withAddress3(STRING.next())
                .withAddress4(STRING.next())
                .withAddress5(STRING.next())
                .withPostcode(POSTCODE)
                .build();
    }

    private ListCourtHearing getListCourtHearingDataWithLegalEntity(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        final UUID masterDefendantId = randomUUID();
        final CourtApplicationParty applicant = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(organisation()
                                        .withName(ORGANISATION_NAME)
                                        .withAddress(getAddress(hearingData.getCourtApplications().get(0).getApplicant().getAddress()))
                                        .build()).build())
                        .withDefendantCase(List.of(DefendantCase.defendantCase()
                                .withDefendantId(masterDefendantId)
                                .withCaseId(hearingData.getListedCases().get(0).getCaseId())
                                .withCaseReference(hearingData.getListedCases().get(0).getCaseReference())
                                .build()))
                        .build())
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();

        return ListCourtHearing.listCourtHearing()
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(hearingData.getCourtRoomId())
                                .build())
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(hearingData.getCourtApplications().get(0).getLinkedCaseId())
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withCaseURN(STRING.next())
                                                .withProsecutionAuthorityId(randomUUID())
                                                .withProsecutionAuthorityCode(STRING.next()).build())
                                        .withIsSJP(false)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .withParentApplicationId(hearingData.getCourtApplications().get(0).getParentApplicationId())
                                .withApplicationParticulars(hearingData.getCourtApplications().get(0).getApplicationParticulars())
                                .withType(getCourtApplicationType(hearingData))
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withApplicationReference(STRING.next())
                                .withApplicationStatus(ApplicationStatus.LISTED)
                                .withApplicant(applicant)
                                .withSubject(applicant)
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                        .withEndDate(hearingData.getHearingEndDate() != null ? hearingData.getHearingEndDate().toString() : null)
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(hearingData.getJudiciary() != null
                                ? singletonList(getJudicialRole(hearingData))
                                : null)
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? null :
                                getWeekCommencingDate(hearingData, hearingData.getWeekCommencingDuration()))
                        .withProsecutionCases(hearingData.getListedCases().stream()
                                .map(lc -> ProsecutionCase.prosecutionCase().withId(lc.getCaseId())
                                        .withInitiationCode(InitiationCode.C)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityCode(lc.getAuthorityCode())
                                                .withProsecutionAuthorityId(lc.getAuthorityId())
                                                .withProsecutionAuthorityReference(lc.getCaseReference())
                                                .build())
                                        .withDefendants(lc.getDefendants().stream().map(d -> getDefendant(listedCaseData, d))
                                                .collect(Collectors.toList()))

                                        .build())
                                .collect(Collectors.toList()))

                        .withDefendantListingNeeds(hearingData.getListedCases().stream()
                                .map(lc -> lc.getDefendants().stream().map(d ->
                                                getDefendantListingNeeds(lc, d))
                                        .collect(Collectors.toList()))
                                .flatMap(List::stream)
                                .collect(Collectors.toList()))

                        .withType(getHearingType(hearingData))
                        .withReportingRestrictionReason(hearingData.getReportingRestrictionReason())
                        .withIsGroupProceedings(false)
                        .build())).build();

    }

    private WeekCommencingDate getWeekCommencingDate(final HearingData hearingData, final Integer weekCommencingDuration) {
        return WeekCommencingDate.weekCommencingDate()
                .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                .withDuration(weekCommencingDuration)
                .build();
    }

    private DefendantListingNeeds getDefendantListingNeeds(final ListedCaseData lc, final DefendantData d) {
        return DefendantListingNeeds.defendantListingNeeds()
                .withDefendantId(d.getDefendantId())
                .withProsecutionCaseId(lc.getCaseId())
                .withListingReason(d.getListingReason())
                .build();
    }

    private Defendant getDefendant(final ListedCaseData listedCaseData, final DefendantData d) {
        return Defendant.defendant()
                .withId(d.getDefendantId())
                .withMasterDefendantId(d.getMasterDefendantId())
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(organisation()
                                .withName(ORGANISATION_NAME)
                                .withAddress(buildAddress())
                                .build()).build())
                .withOffences(d.getOffences().stream()
                        .map(o -> getOffence(o, INTEGER.next()))
                        .collect(Collectors.toList()))
                .withProsecutionCaseId(listedCaseData.getCaseId())
                .build();
    }

    private ListCourtHearing getListCourtHearingDataStandaloneApplication(final HearingsData hearingsData) {

        final HearingData hearingData = hearingsData.getHearingData().get(0);

        return ListCourtHearing.listCourtHearing()
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(hearingData.getCourtRoomId())
                                .build())
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
                                .withId(hearingData.getCourtApplications().get(0).getId())
                                .withParentApplicationId(hearingData.getCourtApplications().get(0).getParentApplicationId())
                                .withApplicationParticulars(hearingData.getCourtApplications().get(0).getApplicationParticulars())
                                .withType(getCourtApplicationType(hearingData))
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withApplicationReference(STRING.next())
                                .withApplicationStatus(ApplicationStatus.DRAFT)
                                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(nonNull(hearingData.getCourtApplications().get(0).getLinkedCaseId()) ? hearingData.getCourtApplications().get(0).getLinkedCaseId() : randomUUID())
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withCaseURN(STRING.next())
                                                .withProsecutionAuthorityId(randomUUID())
                                                .withProsecutionAuthorityCode(STRING.next()).build())
                                        .withIsSJP(false)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .withApplicant(getApplicant(hearingData.getCourtApplications().get(0).getApplicant()))
                                .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty()
                                        .withId(randomUUID())
                                        .withPersonDetails(Person.person().withLastName(hearingData.getCourtApplications().get(0).getRespondent().getLastName())
                                                .withFirstName(hearingData.getCourtApplications().get(0).getRespondent().getFirstName())
                                                .withGender(Gender.FEMALE)
                                                .withAddress(getAddress(hearingData.getCourtApplications().get(0).getRespondent().getAddress()))
                                                .build())
                                        .withSummonsRequired(false)
                                        .withNotificationRequired(false)
                                        .build()))
                                .withSubject(getApplicant(hearingData.getCourtApplications().get(0).getApplicant()))
                                .build()))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                        .withEndDate(hearingData.getHearingEndDate() != null ? hearingData.getHearingEndDate().toString() : null)
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withType(getHearingType(hearingData))
                        .withReportingRestrictionReason(hearingData.getReportingRestrictionReason())
                        .withIsGroupProceedings(false)
                        .build())).build();

    }

    private HearingType getHearingType(final HearingData hearingData) {
        return HearingType.hearingType()
                .withDescription(hearingData.getHearingTypeData().getTypeDescription())
                .withWelshDescription(hearingData.getHearingTypeData().getWelshDescription())
                .withId(hearingData.getHearingTypeData().getTypeId())
                .build();
    }

    private ListCourtHearing getListForCourtHearingData(final HearingsData hearingsData) {

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
                .withAdjournedFromDate(LocalDate.now().toString())
                .withShadowListedOffences(shadowListedOffences)
                .withHearings(singletonList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(hearingData.getCourtCentreId())
                                .withName(hearingData.getName())
                                .withRoomId(hearingData.getCourtRoomId())
                                .build())
                        .withBookingReference(randomUUID())
                        .withCourtApplications(singletonList(getCourtApplication(hearingData)))
                        .withCourtApplicationPartyListingNeeds(hearingData.getCourtApplicationPartyNeeds())
                        .withId(hearingData.getId())
                        .withEarliestStartDateTime(hearingData.getHearingStartTime() != null ? hearingData.getHearingStartTime() : null)
                        .withEndDate(hearingData.getHearingEndDate() != null ? hearingData.getHearingEndDate().toString() : null)
                        .withEstimatedMinutes(hearingData.getHearingEstimateMinutes())
                        .withJudiciary(hearingData.getJudiciary() != null
                                ? singletonList(JudicialRole.judicialRole()
                                .withJudicialId(hearingData.getJudiciary().get(0).getJudicialId())
                                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())
                                        .withJudicialRoleTypeId(hearingData.getJudiciary().get(0).getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                                        .build())
                                .withIsDeputy(hearingData.getJudiciary().get(0).getIsDeputy().orElse(null))
                                .withIsBenchChairman(hearingData.getJudiciary().get(0).getIsBenchChairman().orElse(null))
                                .withUserId(hearingData.getJudiciary().get(0).getUserId())
                                .build())
                                : null)
                        .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                        .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? null :
                                WeekCommencingDate.weekCommencingDate()
                                        .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                        .withDuration(hearingData.getWeekCommencingDuration())
                                        .build())
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
                                                        .withIsYouth(d.getIsYouth())
                                                        .withPersonDefendant(gerPersonDefendant(d))
                                                        .withAssociatedPersons(singletonList(AssociatedPerson.associatedPerson()
                                                                .withRole(STRING.next())
                                                                .withPerson(getPerson(d))
                                                                .build()))
                                                        .withOffences(d.getOffences().stream()
                                                                .map(o -> getOffence(o, INTEGER.next()))
                                                                .collect(Collectors.toList()))
                                                        .withProsecutionCaseId(listedCaseData.getCaseId())
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                        .withDefendantListingNeeds(hearingData.getListedCases().stream()
                                .map(lc -> lc.getDefendants().stream().map(d ->
                                                getDefendantListingNeeds(lc, d))
                                        .collect(Collectors.toList()))
                                .flatMap(List::stream)
                                .collect(Collectors.toList()))
                        .withType(getHearingType(hearingData))
                        .withReportingRestrictionReason(hearingData.getReportingRestrictionReason())
                        .withIsGroupProceedings(false)
                        .build())).build();

    }

    private CourtApplication getCourtApplication(final HearingData hearingData) {
        return CourtApplication.courtApplication()
                .withId(hearingData.getCourtApplications().get(0).getId())
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(hearingData.getCourtApplications().get(0).getLinkedCaseId())
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withCaseURN(STRING.next())
                                .withProsecutionAuthorityId(randomUUID())
                                .withProsecutionAuthorityCode(STRING.next()).build())
                        .withIsSJP(false)
                        .withCaseStatus("ACTIVE")
                        .build()))
                .withParentApplicationId(hearingData.getCourtApplications().get(0).getParentApplicationId())
                .withType(getCourtApplicationType(hearingData))
                .withApplicationReceivedDate(LocalDate.now().toString())
                .withApplicationReference(STRING.next())
                .withApplicationParticulars(hearingData.getCourtApplications().get(0).getApplicationParticulars())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(getApplicant(hearingData.getCourtApplications().get(0).getApplicant()))
                .withRespondents(getRespondents(hearingData))
                .withSubject(getApplicant(hearingData.getCourtApplications().get(0).getApplicant()))
                .build();
    }

    private PersonDefendant gerPersonDefendant(final DefendantData d) {
        return PersonDefendant.personDefendant()
                .withBailStatus(new BailStatus.Builder().withCode(d.getBailStatus().getCode()).withDescription(d.getBailStatus().getDescription()).withId(d.getBailStatus().getId()).build())
                .withPersonDetails(Person.person()
                        .withTitle(PERSON_TITLE)
                        .withNationalityId(randomUUID())
                        .withNationalityDescription(PERSON_NATIONALITY_DESCRIPTION)
                        .withAddress(buildAddress())
                        .withFirstName(d.getFirstName())
                        .withLastName(d.getLastName())
                        .withGender(Gender.MALE)
                        .withAdditionalNationalityId(randomUUID())
                        .withEthnicity(Ethnicity.ethnicity()
                                .withObservedEthnicityId(randomUUID())
                                .withObservedEthnicityDescription(STRING.next())
                                .build())
                        .withDateOfBirth(LocalDate.now().minusYears(21).toString())
                        .build())
                .build();
    }

    private Offence getOffence(final OffenceData o, final Integer next) {
        return Offence.offence()
                .withCount(next)
                .withId(o.getOffenceId())
                .withOffenceCode(STRING.next())
                .withOffenceDefinitionId(randomUUID())
                .withWording(STRING.next())
                .withStartDate(LocalDate.now().toString())
                .withOrderIndex(INTEGER.next())
                .withOffenceTitle(o.getStatementOfOffenceTitle())
                .withLaaApplnReference(
                        LaaReference.laaReference()
                                .withApplicationReference(STRING.next())
                                .withStatusCode(STRING.next())
                                .withStatusDate((format(LocalDate.now().toString())))
                                .withStatusDescription(STRING.next())
                                .withStatusId(randomUUID()).build())
                .build();
    }

    private List<CourtApplicationParty> getRespondents(final HearingData hearingData) {
        return singletonList(
                getApplicant(hearingData.getCourtApplications().get(0).getRespondent()));
    }

    private CourtApplicationType getCourtApplicationType(final HearingData hearingData) {
        return CourtApplicationType.courtApplicationType()
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
                .build();
    }

    public HearingsData getHearingsData() {
        return hearingsData;
    }


    public void verifyHearingWithPossibleDisqualificationFromAPI() {
        HearingData singleHearingData = hearingsData.getHearingData().get(0);
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.possible-disqualification"),
                        false,
                        singleHearingData.getCourtCentreId(),
                        POSSIBLE_DISQUALIFICATION_FLAG));

        final Filter idFilter = filter(where("id").is(singleHearingData.getId().toString()));
        final Filter possibleDisqualificationFilter = filter(where("isPossibleDisqualification").is(POSSIBLE_DISQUALIFICATION_FLAG));
        final com.jayway.jsonpath.JsonPath hearingFilters = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter, possibleDisqualificationFilter);

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingFilters)
        });
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

        final String requestString = "{\"allocatedHearingId\": \"ALLOCATED_HEARING_ID\", \"sendNotificationToParties\" : true}";
        final String requestBody = requestString.replace("ALLOCATED_HEARING_ID", allocatedHearingId.toString());

        restClient.postCommand(extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING,
                requestBody, getLoggedInHeader());
    }

    public void extendHearingPartially(final UUID unAllocatedHearingId, final UUID allocatedHearingId, final ListedCaseData listedCaseData) {
        final String extendHearingForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING), unAllocatedHearingId));

        final String requestBody = getPayload("stub-data/listing.command.extend.hearing-for-hearing-partial.json")
                .replace("HEARING_ID1", allocatedHearingId.toString())
                .replace("CASE_ID1", listedCaseData.getCaseId().toString())
                .replace("DEFENDANT_ID1", listedCaseData.getDefendants().get(0).getDefendantId().toString())
                .replace("OFFENCE_ID1", listedCaseData.getDefendants().get(0).getOffences().get(0).getOffenceId().toString());

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING, requestBody, getLoggedInHeader());

        restClient.postCommand(extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING,
                requestBody, getLoggedInHeader());
    }

    public void extendWholeHearing(final UUID unAllocatedHearingId, final UUID allocatedHearingId, final List<ListedCaseData> listedCaseData) {
        final String extendHearingForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING), unAllocatedHearingId));

        final ListedCaseData case1 = listedCaseData.get(0);
        final ListedCaseData case2 = listedCaseData.get(1);

        final String requestBody = getPayload("stub-data/listing.command.extend.hearing-for-hearing-whole.json")
                .replace("HEARING_ID1", allocatedHearingId.toString())
                .replace("CASE_ID1", case1.getCaseId().toString())
                .replace("DEFENDANT_ID1", case1.getDefendants().get(0).getDefendantId().toString())
                .replace("OFFENCE_ID1", case1.getDefendants().get(0).getOffences().get(0).getOffenceId().toString())
                .replace("CASE_ID2", case2.getCaseId().toString())
                .replace("DEFENDANT_ID2", case2.getDefendants().get(0).getDefendantId().toString())
                .replace("OFFENCE_ID2", case2.getDefendants().get(0).getOffences().get(0).getOffenceId().toString());

        restClient.postCommand(extendHearingForHearingUrl, MEDIA_TYPE_LIST_EXTEND_HEARING_FOR_HEARING,
                requestBody, getLoggedInHeader());
    }

    public void verifyHearingIsCreated(final UUID hearingId, final int listedCaseSize) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"),
                        hearingId));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id",
                                        is(hearingId.toString())),
                                withJsonPath("$.listedCases.size()",
                                        is(listedCaseSize)))));

    }


    public void verifyPublicEventHearingConfirmedAndExtendHearingFromProgression(final UUID allocatedHearingId, final UUID unAllocatedHearingId) {
        final List<String> newCaseIds = new ArrayList<>();

        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing, isJson(Matchers.allOf(
                withJsonPath("$.confirmedHearing.id", is(allocatedHearingId.toString())))));

        assertThat(jsonResponse.get("confirmedHearing.id"), is(allocatedHearingId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases.size()"), is(1));
        final String allocatedHearingCaseId = jsonResponse.get("confirmedHearing.prosecutionCases[0].id");

        final JsonPath jsonResponse1 = retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);

        assertThat(jsonResponse1.getBoolean("sendNotificationToParties"), is(true));
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

        final String eventPayloadString = getPayload("prosecution-case.json")
                .replaceAll("HEARING_ID", allocatedHearingId.toString())
                .replaceAll("CASE_ID_1", newCaseIds.get(0))
                .replaceAll("DEFENDANT_ID_1", defendant1)
                .replaceAll("OFFENCE_ID_1", offence1)
                .replaceAll("CASE_ID_2", newCaseIds.get(1))
                .replaceAll("DEFENDANT_ID_2", defendant2)
                .replaceAll("OFFENCE_ID_2", offence2);

        final JsonObject hearingExtendedDataObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        sendMessage(
                publicMessageProducerProgressionHearingExtendedEvent,
                PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED,
                hearingExtendedDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
    }

    public void verifyPublicEventHearingConfirmedEventAndExtendPartialHearingFromProgression(final UUID allocatedHearingId, final UUID unAllocatedHearingId) {
        final List<String> newCaseIds = new ArrayList<>();

        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);

        assertThat(jsonResponse.get("confirmedHearing.id"), is(allocatedHearingId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases.size()"), is(1));
        final String allocatedHearingCaseId = jsonResponse.get("confirmedHearing.prosecutionCases[0].id");

        final JsonPath jsonResponse1 = retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);

        assertThat(jsonResponse1.get("confirmedHearing.id"), is(unAllocatedHearingId.toString()));
        assertThat(jsonResponse1.get("confirmedHearing.prosecutionCases.size()"), is(1));
        newCaseIds.add(jsonResponse1.get("confirmedHearing.prosecutionCases[0].id"));
        Assert.assertFalse(newCaseIds.contains(allocatedHearingCaseId));

        // mock public event from progression
        final String defendant1 = jsonResponse1.get("confirmedHearing.prosecutionCases[0].defendants[0].id");
        final String offence1 = jsonResponse1.get("confirmedHearing.prosecutionCases[0].defendants[0].offences[0].id");

        final String eventPayloadString = getPayload("prosecution-case-partial-allocation.json")
                .replaceAll("HEARING_ID", allocatedHearingId.toString())
                .replaceAll("CASE_ID_1", newCaseIds.get(0))
                .replaceAll("DEFENDANT_ID_1", defendant1)
                .replaceAll("OFFENCE_ID_1", offence1);

        final JsonObject hearingExtendedDataObject = new StringToJsonObjectConverter().convert(eventPayloadString);

        LOGGER.info("mocked public event : {}", hearingExtendedDataObject.toString());

        sendMessage(
                publicMessageProducerProgressionHearingExtendedEvent,
                PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED,
                hearingExtendedDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTED_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
    }

    public void verifyHearingUpdatedToCaseInActiveMQ(final UUID allocatedHearingId, final UUID unallocatedHearingId, final int expectedCount) {
        for (int i = 0; i < expectedCount; i++) {
            final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingUpdatedToCase);

            assertThat(jsonResponse.get("id"), is(unallocatedHearingId.toString()));
            assertThat(jsonResponse.get("existingHearingId"), is(allocatedHearingId.toString()));
        }
    }

    public void verifyPublicEventHearingUpdatedPartially(final UUID hearingId) {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingPartiallyUpdated);
        assertThat(jsonResponse.get("hearingIdToBeUpdated"), is(hearingId.toString()));
    }

    public void verifyPublicEVentHearingChangesSaved(final UUID hearingId) {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingChangesSaved);
        assertThat(jsonResponse.get("hearingId"), is(hearingId.toString()));
    }

    public void verifyHearingDayCourtScheduleCarriedOverToCommand(LocalDate hearingDate, UUID courtScheduleId) {
        JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingDayScheduleUpdated);
        List<Map<String, String>> schedules = jsonResponse.getList("hearingDayCourtSchedules");
        assertThat(schedules.get(0).get("courtScheduleId"), is(courtScheduleId.toString()));
        assertThat(schedules.get(0).get("hearingDate"), is(hearingDate.toString()));
    }

    public void verifyPublicEventHearingConfirmed() {
        final JsonPath jsonResponse = getHearingConfirmedPublicEventPayload();

        final HearingData hearingData = hearingsData.getHearingData().get(0);
        assertThat(jsonResponse.get("confirmedHearing.id"), is(hearingData.getId().toString()));
        assertThat(jsonResponse.get("confirmedHearing.courtCentre.roomId"), is(hearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get("confirmedHearing.courtCentre.id"), is(hearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get("confirmedHearing.courtCentre.name"), is("Liverpool Crown Court"));
        assertThat(jsonResponse.get("confirmedHearing.courtApplicationIds[0]"), is(hearingData.getCourtApplications().get(0).getId().toString()));
    }

    public JsonPath getHearingConfirmedPublicEventPayload() {
        return retrieveMessage(publicMessageConsumerHearingConfirmedForExtendHearing);
    }

    public void createListingNotes() {
        this.hearingsData.getHearingData().stream().filter(hearing -> hearing.getCourtRoomId() != null).
                forEach(hearing -> notesSteps.createNoteForListing(hearing.getCourtRoomId(), "2020-05-21", "note 1"));
    }

    public void createListingNotesForStartDays() {
        this.hearingsData.getHearingData().stream().filter(hearing -> hearing.getCourtRoomId() != null).
                forEach(hearing -> notesSteps.createNoteForListing(hearing.getCourtRoomId(), hearing.getHearingStartDate().toString(), "note 1"));
    }

    public void listCourtHearing(final JsonObject listCourtHearingJsonObject, Optional<LocalDate> adjournedFromDate, Optional<List<UUID>> shadowListedOffences) {

        final UUID courtCentreId = getUUID(getJsonObject(listCourtHearingJsonObject, "courtCentre").get(), "id").orElse(null);
        final UUID hearingTypeId = fromString(listCourtHearingJsonObject.getJsonObject("type").getString("id"));
        final JsonArray judiciary = listCourtHearingJsonObject.getJsonArray("judiciary");
        if (judiciary != null && !judiciary.isEmpty()) {
            final UUID judicialId = fromString(judiciary.getValuesAs(JsonObject.class).stream().findFirst().get().getString("id"));
            stubGetReferenceDataJudiciaries(judicialId);
        }

        final CourtCentreData courtCentreData = new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, null, "City of London Magistrates' Court");
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataHearingTypes(hearingTypeId);

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        final JsonObjectBuilder listCourtHearingPayload = createObjectBuilder().add("hearings", createArrayBuilder().add(listCourtHearingJsonObject));
        if (adjournedFromDate.isPresent()) {
            listCourtHearingPayload.add("adjournedFromDate", adjournedFromDate.get().toString());
        }

        if (shadowListedOffences.isPresent()) {
            final JsonArrayBuilder offenceIdBuilder = createArrayBuilder();
            shadowListedOffences.get().stream().forEach(offenceId -> offenceIdBuilder.add(offenceId.toString()));
            listCourtHearingPayload.add("shadowListedOffences", offenceIdBuilder);
        }

        request = listCourtHearingPayload.build().toString();

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void createListingNotes(LocalDate date, String note) {
        AtomicReference<LocalDate> currentLocalDate = new AtomicReference<>(date);
        this.hearingsData.getHearingData().stream().filter(hearing -> hearing.getCourtRoomId() != null).
                forEach(hearing -> {
                    notesSteps.createNoteForListing(hearing.getCourtRoomId(), currentLocalDate.get().toString(), note);
                    currentLocalDate.set(currentLocalDate.get().plusDays(1));
                });
    }

    public void listCourtHearing(final JsonObject listCourtHearingJsonObject, final UUID courtCentreId, final UUID hearingTypeId) {

        final CourtCentreData courtCentreData = new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, null, "City of London Magistrates' Court");
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataHearingTypes(hearingTypeId);

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        request = listCourtHearingJsonObject.toString();

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public JsonObject preparePayloadToListCourtHearing(final String fileName, final Map<String, String> values) throws IOException {

        final String eventPayloadString = getStringFromResource(fileName)
                .replaceAll("HEARING_ID", values.get("hearingId"))
                .replaceAll("CASE_ID", values.get("caseId"))
                .replaceAll("COURT_CENTRE_ID", values.get("courtCentreId"))
                .replaceAll("CASE_URN", values.get("caseUrn"))
                .replaceAll("EARLIEST_START_TIME", values.get("hearingStartTime"))
                .replaceAll("ESTIMATED_MINUTES", values.get("estimatedMinutes"))
                .replaceAll("HEARING_TYPE_ID", values.get("hearingTypeId"));

        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }

    public JsonObject preparePayloadToListCourtHearingForGroupCases(final String fileName, final Map<String, String> values, final UUID groupId,
                                                                    final UUID masterCaseId) throws IOException {

        String needString = getStringFromResource(fileName.concat("-part-defendant-listing-needs.json"));
        String caseString = getStringFromResource(fileName.concat("-part-cases.json"));
        String eventPayloadString = getStringFromResource(fileName.concat(".json"))
                .replaceAll("HEARING_ID", values.get("hearingId"))
                .replaceAll("COURT_CENTRE_ID", values.get("courtCentreId"))
                .replaceAll("EARLIEST_START_TIME", values.get("hearingStartTime"))
                .replaceAll("ESTIMATED_MINUTES", values.get("estimatedMinutes"))
                .replaceAll("HEARING_TYPE_ID", values.get("hearingTypeId"));

        StringBuilder casesBuilder = new StringBuilder();
        StringBuilder needsBuilder = new StringBuilder();

        UUID defendantId = randomUUID();
        casesBuilder.append(getGroupCase(caseString, groupId, masterCaseId, defendantId, true, true, true));
        needsBuilder.append(getGroupCaseListingNeeds(needString, masterCaseId, defendantId));

        eventPayloadString = eventPayloadString.replace("DEFENDANT_LISTING_NEEDS", needsBuilder.toString());
        eventPayloadString = eventPayloadString.replace("PROSECUTION_CASES", casesBuilder.toString());

        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }

    public JsonObject preparePayloadCaseRemovedFromGroupCases(final String fileName, final String casesFileName,
                                                              final UUID groupId, final UUID masterCaseId,
                                                              final UUID removedCaseId, final UUID newGroupMasterCaseId) throws IOException {

        String eventPayloadString = getStringFromResource(fileName)
                .replaceAll("GROUP_ID", groupId.toString())
                .replaceAll("MASTER_CASE_ID", masterCaseId.toString());

        String casePayloadString = getStringFromResource(casesFileName);
        StringBuilder newGroupMasterCaseBuilder = new StringBuilder();

        final String removedCaseBuilder = getGroupCase(casePayloadString, groupId, removedCaseId, randomUUID(), true, false, false);
        newGroupMasterCaseBuilder.append(getGroupCase(casePayloadString, groupId, newGroupMasterCaseId, randomUUID(), true, true, true));

        eventPayloadString = eventPayloadString.replace("REMOVED_CASE", removedCaseBuilder);
        eventPayloadString = eventPayloadString.replace("NEW_GROUP_MASTER", newGroupMasterCaseBuilder.toString());

        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }

    private String getGroupCase(final String caseString, final UUID groupId, final UUID caseId, final UUID defendantId,
                                final Boolean isCivil, final Boolean isGroupMember, final Boolean isGroupMaster) {
        String newCase = caseString;
        newCase = newCase.replaceAll("CASE_ID", caseId.toString());
        newCase = newCase.replaceAll("IS_CIVIL", isCivil.toString());
        newCase = newCase.replaceAll("IS_GROUP_MEMBER", isGroupMember.toString());
        newCase = newCase.replaceAll("IS_GROUP_MASTER", isGroupMaster.toString());
        newCase = newCase.replaceAll("GROUP_ID", groupId.toString());
        newCase = newCase.replaceAll("CASE_MARKER_ID", randomUUID().toString());
        newCase = newCase.replaceAll("DEFENDANT_ID", defendantId.toString());
        newCase = newCase.replaceAll("OFFENCE_ID", randomUUID().toString());
        newCase = newCase.replaceAll("CASE_URN", randomUUID().toString().substring(0, 10));
        return newCase;
    }

    private String getGroupCaseListingNeeds(final String needString, final UUID defendantId, final UUID caseId) {
        String defendantNeed = needString;
        defendantNeed = defendantNeed.replaceAll("DEFENDANT_ID", defendantId.toString());
        defendantNeed = defendantNeed.replaceAll("CASE_ID", caseId.toString());
        return defendantNeed;
    }

    public void verifyUnallocatedHearingFound(final Matcher[] matchers) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated"), UNALLOCATED));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers)));
    }

    public void verifyHearingForCourtSchedulerCourtSessionAndBusinessType(final String jurisdictionType, final String courtSession, final String businessType, final boolean allocated, final Matcher... matchers) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.jurisdiction-type.court-session.business-type"), jurisdictionType, courtSession, businessType, allocated));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser())).
                until(status().is(OK),
                        payload().isJson(allOf(matchers))
                );
    }
}
