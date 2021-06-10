package uk.gov.moj.cpp.listing.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.TypeOfList;
import uk.gov.justice.listing.courts.WeekCommencingDate;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.ApplicantRespondentData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.endpoint.UnscheduledHearingsEndpoint.pollForUnscheduledHearings;
import static uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps.getJsonPathQueryForCaseReference;
import static uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps.getJsonPathQueryForDefendantLastName;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

public class ListNextHearingSteps extends AbstractIT implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListNextHearingSteps.class);

    private final HearingData firstHearing;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String PERSON_TITLE = "Baroness";
    private static final String PERSON_NATIONALITY_DESCRIPTION = "British";
    private static final String POSTCODE = "CR1 4BX";
    private static final UUID JUDICIAL_RESULT_ID = UUID.fromString("065b6fcb-0787-4f0d-a9cd-af4b5c36e047");
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String LISTING_API_LIST_NEXT_HEARINGS = "listing.list-next-hearings-v2";
    private static final String LISTING_API_DELETE_NEXT_HEARINGS = "listing.delete-next-hearings";
    private static final String LISTING_API_UNSCHEDULED_NEXT_HEARINGS = "listing.list-unscheduled-next-hearings";
    private static final String LISTING_API_UPDATE_RELATED_HEARING = "listing.update-related-hearing";
    private static final String MEDIA_TYPE_LIST_NEXT_HEARINGS = "application/vnd.listing.next-hearings-v2+json";
    private static final String MEDIA_TYPE_DELETE_NEXT_HEARINGS = "application/vnd.listing.delete-next-hearings+json";
    private static final String MEDIA_TYPE_UNSCHEDULED_NEXT_HEARINGS = "application/vnd.listing.list-unscheduled-next-hearings+json";
    private static final String MEDIA_TYPE_UPDATE_RELATED_HEARING = "application/vnd.listing.related-hearing+json";

    private static final String EVENT_SELECTOR_NEXT_HEARING_REQUESTED = "listing.events.next-hearing-requested";
    private static final String EVENT_SELECTOR_UNSCHEDULED_NEXT_HEARING_REQUESTED = "listing.events.unscheduled-next-hearing-requested";
    private static final String EVENT_SELECTOR_UPDATE_RELATED_HEARING_REQUESTED = "listing.events.update-existing-hearing-requested";
    private static final String EVENT_SELECTOR_HEARING_LISTED = "listing.events.hearing-listed";
    private static final String EVENT_SELECTOR_UNALLOCATED_HEARING_DELETED = "listing.events.unallocated-hearing-deleted";
    private static final String EVENT_SELECTOR_REMOVE_OFFENCES_FROM_EXISTING_HEARING = "listing.events.remove-offences-from-existing-hearing-requested";
    private static final String EVENT_SELECTOR_OFFENCES_REMOVED_FROM_EXISTING_HEARING = "listing.events.offences-removed-from-existing-unallocated-hearing";
    private static final String EVENT_SELECTOR_DELETE_NEXT_HEARING_REQUESTED = "listing.events.delete_nect_hearing_requested";
    private static final String EVENT_SELECTOR_MARKED_AS_DUPLICATE_FOR_CASE_EVENT = "listing.events.hearing-marked-as-duplicate-for-case";
    private static final String EVENT_SELECTOR_CASES_ADDED_TO_HEARING = "listing.event.cases-added-to-hearing";
    private static final String PUBLIC_EVENT_SELECTED_UNALLOCATED_HEARING_DELETED = "public.events.listing.unallocated-hearing-deleted";
    private static final String PUBLIC_EVENT_SELECTED_OFFENCES_REMOVED_FROM_EXISTING_HEARING = "public.events.listing.offences-removed-from-existing-unallocated-hearing";

    private final MessageConsumer privateMessageConsumerNextHearingRequested;
    private final MessageConsumer privateMessageConsumerUnscheduledNextHearingRequested;
    private final MessageConsumer privateMessageConsumerUpdateRelatedHearingRequested;
    private final MessageConsumer privateMessageConsumerCasesAddedToHearing;
    private final MessageConsumer privateMessageConsumerHearingListed;
    private final MessageConsumer privateMessageConsumerUnallocatedHearingDeleted;
    private final MessageConsumer privateMessageConsumerRemoveOffencesFromExistingHearingRequested;
    private final MessageConsumer privateMessageConsumeOffencesRemovedFromExistingHearing;
    private final MessageConsumer privateMessageConsumerDeleteNextHearingRequested;
    private final MessageConsumer privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent;

    private final MessageConsumer publicMessageConsumerUnallocatedHearingDeleted;
    private final MessageConsumer publicMessageConsumerOffencesRemovedFromExistingHearing;


    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);
    private String request;


    public ListNextHearingSteps(final HearingData firstHearing) {
        this.firstHearing = firstHearing;
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        privateMessageConsumerNextHearingRequested = privateEvents.createConsumer(EVENT_SELECTOR_NEXT_HEARING_REQUESTED);
        privateMessageConsumerUnscheduledNextHearingRequested = privateEvents.createConsumer(EVENT_SELECTOR_UNSCHEDULED_NEXT_HEARING_REQUESTED);
        privateMessageConsumerUpdateRelatedHearingRequested = privateEvents.createConsumer(EVENT_SELECTOR_UPDATE_RELATED_HEARING_REQUESTED);
        privateMessageConsumerCasesAddedToHearing = privateEvents.createConsumer(EVENT_SELECTOR_CASES_ADDED_TO_HEARING);
        privateMessageConsumerHearingListed = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_LISTED);
        privateMessageConsumerUnallocatedHearingDeleted = privateEvents.createConsumer(EVENT_SELECTOR_UNALLOCATED_HEARING_DELETED);
        privateMessageConsumerRemoveOffencesFromExistingHearingRequested = privateEvents.createConsumer(EVENT_SELECTOR_REMOVE_OFFENCES_FROM_EXISTING_HEARING);
        privateMessageConsumeOffencesRemovedFromExistingHearing = privateEvents.createConsumer(EVENT_SELECTOR_OFFENCES_REMOVED_FROM_EXISTING_HEARING);
        privateMessageConsumerDeleteNextHearingRequested = privateEvents.createConsumer(EVENT_SELECTOR_DELETE_NEXT_HEARING_REQUESTED);
        privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent = privateEvents.createConsumer(EVENT_SELECTOR_MARKED_AS_DUPLICATE_FOR_CASE_EVENT);

        publicMessageConsumerUnallocatedHearingDeleted = publicEvents.createConsumer(PUBLIC_EVENT_SELECTED_UNALLOCATED_HEARING_DELETED);
        publicMessageConsumerOffencesRemovedFromExistingHearing = publicEvents.createConsumer(PUBLIC_EVENT_SELECTED_OFFENCES_REMOVED_FROM_EXISTING_HEARING);

    }


    @Override
    public void close() {
        try {
            privateMessageConsumerNextHearingRequested.close();
            privateMessageConsumerUnscheduledNextHearingRequested.close();
            privateMessageConsumerUpdateRelatedHearingRequested.close();
            privateMessageConsumerCasesAddedToHearing.close();
            privateMessageConsumerHearingListed.close();
            privateMessageConsumerUnallocatedHearingDeleted.close();
            privateMessageConsumerRemoveOffencesFromExistingHearingRequested.close();
            privateMessageConsumeOffencesRemovedFromExistingHearing.close();
            privateMessageConsumerDeleteNextHearingRequested.close();
            privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent.close();

            publicMessageConsumerUnallocatedHearingDeleted.close();
            publicMessageConsumerOffencesRemovedFromExistingHearing.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }

    }

    public void whenNextHearingSubmittedForListing(final HearingsData hearingsData) {

        final Response response = getResponseNextHearingsSubmittedForListing(hearingsData);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    public void whenDeleteNextHearingSubmittedForListing() {
        final Response response = getResponseDeleteNextHearingsSubmittedForListing();
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenUnscheduledNextHearingSubmittedForListing(final HearingsData hearingsData) {
        final Response response = getResponseUnscheduleNextHearingsSubmittedForListing(hearingsData);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenUpdateRelatedHearingSubmittedForListing(final UUID existedHearingId, final HearingsData hearingsData) {
        final Response response = getResponseUpdatedRelatedHearingSubmittedForListing(existedHearingId, hearingsData);
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    private Response getResponseNextHearingsSubmittedForListing(final HearingsData hearingsData) {

        final String listNextHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_API_LIST_NEXT_HEARINGS), firstHearing.getId()));

        final HearingData nextHearing1 = hearingsData.getHearingData().get(0);
        final HearingData nextHearing2 = hearingsData.getHearingData().get(1);

        final List<HearingListingNeeds> hearings = Arrays.asList(
                buildHearingListingNeeds(nextHearing1),
                buildHearingListingNeeds(nextHearing2));


        final JsonObject listNextHearingsJsonObject = Json.createObjectBuilder()
                .add("hearings", objectToJsonValueConverter.convert(hearings))
                .add("seedingHearing", objectToJsonValueConverter.convert(
                        SeedingHearing.seedingHearing()
                                .withSeedingHearingId(firstHearing.getId())
                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueOf(firstHearing.getJurisdictionType()))
                                .withSittingDay(firstHearing.getHearingStartDate().toString())
                                .build()))

                .build();

        request = listNextHearingsJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", listNextHearingUrl, MEDIA_TYPE_LIST_NEXT_HEARINGS, request, getLoggedInHeader());

        return restClient.postCommand(listNextHearingUrl, MEDIA_TYPE_LIST_NEXT_HEARINGS,
                request, getLoggedInHeader());
    }

    public Response getResponseDeleteNextHearingsSubmittedForListing() {
        final String deleteNextHearingsUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_API_DELETE_NEXT_HEARINGS), firstHearing.getId()));

        final JsonObject deleteNextHearingsJsonObject = Json.createObjectBuilder()
                .add("seedingHearing", objectToJsonValueConverter.convert(
                        SeedingHearing.seedingHearing()
                                .withSeedingHearingId(firstHearing.getId())
                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueOf(firstHearing.getJurisdictionType()))
                                .withSittingDay(firstHearing.getHearingStartDate().toString())
                                .build()))

                .build();

        request = deleteNextHearingsJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", deleteNextHearingsUrl, MEDIA_TYPE_DELETE_NEXT_HEARINGS, request, getLoggedInHeader());

        return restClient.postCommand(deleteNextHearingsUrl, MEDIA_TYPE_DELETE_NEXT_HEARINGS,
                request, getLoggedInHeader());
    }

    private Response getResponseUnscheduleNextHearingsSubmittedForListing(final HearingsData hearingsData) {

        final String listNextHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_API_UNSCHEDULED_NEXT_HEARINGS), firstHearing.getId()));

        final HearingData nextHearing1 = hearingsData.getHearingData().get(0);
        final HearingData nextHearing2 = hearingsData.getHearingData().get(1);

        final List<HearingUnscheduledListingNeeds> hearings = Arrays.asList(
                buildUnscheduledHearingListingNeeds(nextHearing1),
                buildUnscheduledHearingListingNeeds(nextHearing2));


        final JsonObject listNextHearingsJsonObject = Json.createObjectBuilder()
                .add("hearings", objectToJsonValueConverter.convert(hearings))
                .add("seedingHearing", objectToJsonValueConverter.convert(
                        SeedingHearing.seedingHearing()
                                .withSeedingHearingId(firstHearing.getId())
                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueOf(firstHearing.getJurisdictionType()))
                                .withSittingDay(firstHearing.getHearingStartDate().toString())
                                .build()))

                .build();

        request = listNextHearingsJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", listNextHearingUrl, MEDIA_TYPE_UNSCHEDULED_NEXT_HEARINGS, request, getLoggedInHeader());

        return restClient.postCommand(listNextHearingUrl, MEDIA_TYPE_UNSCHEDULED_NEXT_HEARINGS,
                request, getLoggedInHeader());
    }

    private Response getResponseUpdatedRelatedHearingSubmittedForListing(final UUID existedHearingId, final HearingsData hearingsData) {

        final String updateRelatedHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_API_UPDATE_RELATED_HEARING), existedHearingId));

        final HearingData nextHearing = hearingsData.getHearingData().get(0);

        final List<ProsecutionCase> prosecutionCases = buildProsecutionCases(nextHearing);


        final JsonObject listNextHearingsJsonObject = Json.createObjectBuilder()
                .add("prosecutionCases", objectToJsonValueConverter.convert(prosecutionCases))
                .add("seedingHearing", objectToJsonValueConverter.convert(
                        SeedingHearing.seedingHearing()
                                .withSeedingHearingId(firstHearing.getId())
                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueOf(firstHearing.getJurisdictionType()))
                                .withSittingDay(firstHearing.getHearingStartDate().toString())
                                .build()))

                .build();

        request = listNextHearingsJsonObject.toString();
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", updateRelatedHearingUrl, MEDIA_TYPE_UPDATE_RELATED_HEARING, request, getLoggedInHeader());

        return restClient.postCommand(updateRelatedHearingUrl, MEDIA_TYPE_UPDATE_RELATED_HEARING,
                request, getLoggedInHeader());
    }

    public void verifyNextHearingRequestedInActiveMQ(final HearingsData hearingsData) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerNextHearingRequested);
        LOGGER.debug("jsonResponse from privateMessageConsumerNextHearingRequested: {}", jsonResponse.prettify());

        final JsonPath jsonResponse2 = QueueUtil.retrieveMessage(privateMessageConsumerNextHearingRequested);
        LOGGER.debug("jsonResponse from privateMessageConsumerNextHearingRequested: {}", jsonResponse.prettify());

        assertThat(Arrays.asList(jsonResponse.get("hearing.id"), jsonResponse2.get("hearing.id")),
                hasItems(hearingsData.getHearingData().get(0).getId().toString(),
                        hearingsData.getHearingData().get(1).getId().toString()));
    }

    public void verifyUnscheduledNextHearingRequestedInActiveMQ(final HearingsData hearingsData) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerUnscheduledNextHearingRequested);
        LOGGER.debug("jsonResponse from privateMessageConsumerUnscheduledNextHearingRequested: {}", jsonResponse.prettify());

        final JsonPath jsonResponse2 = QueueUtil.retrieveMessage(privateMessageConsumerUnscheduledNextHearingRequested);
        LOGGER.debug("jsonResponse from privateMessageConsumerUnscheduledNextHearingRequested: {}", jsonResponse.prettify());

        assertThat(Arrays.asList(jsonResponse.get("hearing.id"), jsonResponse2.get("hearing.id")),
                hasItems(hearingsData.getHearingData().get(0).getId().toString(),
                        hearingsData.getHearingData().get(1).getId().toString()));
    }

    public void verifyUpdateRelatedHearingRequestedInActiveMQ(final UUID hearingId) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerUpdateRelatedHearingRequested);
        LOGGER.debug("jsonResponse from privateMessageConsumerUpdateRelatedHearingRequested: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(hearingId.toString()));
    }


    public void verifyHearingListedInActiveMQ(final HearingsData hearingsData) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingListed);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse.prettify());

        final JsonPath jsonResponse2 = QueueUtil.retrieveMessage(privateMessageConsumerHearingListed);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingListed: {}", jsonResponse2.prettify());

        assertThat(Arrays.asList(jsonResponse.get("hearing.id"), jsonResponse2.get("hearing.id")),
                hasItems(hearingsData.getHearingData().get(0).getId().toString(),
                        hearingsData.getHearingData().get(1).getId().toString()));
    }

    public void verifyCasesAddedToHearingInActiveMQ(final UUID hearingId, final HearingsData hearingsData) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCasesAddedToHearing);
        LOGGER.debug("jsonResponse from privateMessageConsumerCasesAddedToHearing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(hearingId.toString()));
        assertThat(jsonResponse.get("unAllocatedListedCases[0].id"), is(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString()));
        assertThat(jsonResponse.get("unAllocatedListedCases[1].id"), is(hearingsData.getHearingData().get(0).getListedCases().get(1).getCaseId().toString()));
    }


    public void verifyHearingListedFromAPI(final HearingsData hearingsData) {
        verifyHearingListedFromAPI(hearingsData.getHearingData().get(0));
        verifyHearingListedFromAPI(hearingsData.getHearingData().get(1));

    }

    public void verifyUnscheduledHearingListedFromApi(final HearingsData hearingsData) {
        verifyHearingUnscheduledListedFromAPI(hearingsData.getHearingData().get(0));
        verifyHearingUnscheduledListedFromAPI(hearingsData.getHearingData().get(1));
    }

    public void verifyCasesAddedToHearingFromApi(HearingsData existedHearingsData, final HearingsData hearingsData) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), existedHearingsData.getHearingData().get(0).getCourtCentreId(), false));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(existedHearingsData.getHearingData().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].listedCases[2].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                                withJsonPath("$.hearings[0].listedCases[3].id",
                                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(1).getCaseId().toString()))
                        )));
    }

    public void verifyUnAllocatedOldHearingDeletedInActiveMQ(final HearingsData hearingsData) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerUnallocatedHearingDeleted);
        LOGGER.debug("jsonResponse from privateMessageConsumerUnallocatedHearingDeleted: {}", jsonResponse.prettify());

        final JsonPath jsonResponse2 = QueueUtil.retrieveMessage(privateMessageConsumerUnallocatedHearingDeleted);
        LOGGER.debug("jsonResponse from privateMessageConsumerUnallocatedHearingDeleted: {}", jsonResponse2.prettify());

        assertThat(Arrays.asList(jsonResponse.get("hearingId"), jsonResponse2.get("hearingId")),
                hasItems(hearingsData.getHearingData().get(0).getId().toString(),
                        hearingsData.getHearingData().get(1).getId().toString()));
    }

    public void verifyRemoveOffencesFromExistingHearingRequestedInActiveMQ(final UUID existedHearingId) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerRemoveOffencesFromExistingHearingRequested);
        LOGGER.debug("jsonResponse from privateMessageConsumerRemoveOffencesFromExistingHearingRequested: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(existedHearingId.toString()));
        assertThat(jsonResponse.get("seedingHearingId"), is(firstHearing.getId().toString()));
    }

    public void verifyOffencesRemovedFromExistingHearingInActiveMQ(final UUID existedHearingId, final HearingsData hearingsData) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumeOffencesRemovedFromExistingHearing);
        LOGGER.debug("jsonResponse from privateMessageConsumeOffencesRemovedFromExistingHearing: {}", jsonResponse.prettify());

        final List<String> offenceIds = hearingsData.getHearingData().get(0).getListedCases().stream()
                .flatMap(listedCaseData -> listedCaseData.getDefendants().stream())
                .flatMap(defendantData -> defendantData.getOffences().stream())
                .map(OffenceData::getOffenceId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        assertThat(jsonResponse.get("hearingId"), is(existedHearingId.toString()));
        assertThat(jsonResponse.get("offenceIds"), is(offenceIds));
    }


    public void verifyOldHearingDeleted(final HearingsData hearingsData) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"),
                        hearingsData.getHearingData().get(0).getCourtCentreId(), false));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(not(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getId().toString())))));

    }

    public void verifyHearingMarkedAsDuplicateForCaseInActiveMQ(final HearingsData hearingsData) {
        final JsonPath jsonResponse1 = QueueUtil.retrieveMessage(privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent);
        final JsonPath jsonResponse2 = QueueUtil.retrieveMessage(privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent);
        final JsonPath jsonResponse3 = QueueUtil.retrieveMessage(privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent);
        final JsonPath jsonResponse4 = QueueUtil.retrieveMessage(privateMessageConsumerHearingMarkedAsDuplicateForCaseEvent);

        final Map<String, List<String>> expectedHearingCaseIds = hearingsData.getHearingData().stream()
                .collect(Collectors.toMap(hearingData -> hearingData.getId().toString(),
                        hearingData -> hearingData.getListedCases().stream().map(listedCase -> listedCase.getCaseId().toString()).collect(Collectors.toList())));
        final Map<String, List<String>> actualHearingCaseIds = new HashMap<>();
        actualHearingCaseIds.put(jsonResponse1.get("hearingId"), Arrays.asList(jsonResponse1.get("caseId"), jsonResponse2.get("caseId")));
        actualHearingCaseIds.put(jsonResponse3.get("hearingId"), Arrays.asList(jsonResponse3.get("caseId"), jsonResponse4.get("caseId")));

        actualHearingCaseIds.keySet().stream().forEach((key) -> {
            List<String> actualList = actualHearingCaseIds.get(key);
            List<String> expectedList = expectedHearingCaseIds.get(key);

            assertThat(expectedList.size(), is(actualList.size()));
            assertThat(expectedList.containsAll(actualList), is(true));
        });
    }

    public void verifyPublicUnallocatedOldHearingDeletedInPublicMQ(final HearingsData hearingsData) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerUnallocatedHearingDeleted);
        final JsonPath jsonResponse2 = QueueUtil.retrieveMessage(publicMessageConsumerUnallocatedHearingDeleted);

        final List<String> actualHearingIds = Arrays.asList(jsonResponse.get("hearingId"), jsonResponse2.get("hearingId"));
        final List<String> expectedHearingIds = hearingsData.getHearingData().stream()
                .map(hearingData -> hearingData.getId().toString()).collect(Collectors.toList());
        assertThat(expectedHearingIds.containsAll(actualHearingIds), is(true));
        assertThat(expectedHearingIds.size(), is(actualHearingIds.size()));

    }

    public void verifyPublicOffencesRemovedFromExistingHearingInActiveMQ(final UUID existedHearingId, final HearingsData hearingsData) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerOffencesRemovedFromExistingHearing);

        final List<String> offenceIds = hearingsData.getHearingData().get(0).getListedCases().stream()
                .flatMap(listedCaseData -> listedCaseData.getDefendants().stream())
                .flatMap(defendantData -> defendantData.getOffences().stream())
                .map(OffenceData::getOffenceId)
                .map(UUID::toString)
                .collect(Collectors.toList());

        assertThat(jsonResponse.get("hearingId"), is(existedHearingId.toString()));
        assertThat(jsonResponse.get("offenceIds"), is(offenceIds));

    }


    private void verifyHearingListedFromAPI(final HearingData hearingData) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), false));

        verifyHearingListedFromWithApiUrl(hearingData, searchHearingUrl);
    }

    public void verifyHearingUnscheduledListedFromAPI(final HearingData hearingData) {


        final UUID courtCentreId = hearingData.getCourtCentreId();

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
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].isYouth",
                        equalTo(true))
        );

        pollForUnscheduledHearings(getLoggedInUser(), courtCentreId, unscheduledHearingVerifiedMatcher);
    }

    public void verifyHearingListedFromWithApiUrl(final HearingData hearingData, final String searchHearingUrl) {
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
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].isYouth",
                                        equalTo(true))
                        )));
    }

    private HearingListingNeeds buildHearingListingNeeds(final HearingData hearingData) {
        return HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(hearingData.getCourtCentreId())
                        .withName(hearingData.getName())
                        .withRoomId(ofNullable(hearingData.getCourtRoomId()))
                        .build())
                .withBookingReference(of(randomUUID()))
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
                        .withUserId(hearingData.getJudiciary().get(0).getUserId())
                        .build())
                        : null)
                .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? Optional.empty() :
                        of(WeekCommencingDate.weekCommencingDate()
                                .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                .withDuration(of(hearingData.getWeekCommencingDuration()))
                                .build()))
                .withProsecutionCases(buildProsecutionCases(hearingData))
                .withDefendantListingNeeds(hearingData.getListedCases().stream()
                        .map(lc -> lc.getDefendants().stream().map(d ->
                                getDefendantListingNeeds(lc, d))
                                .collect(Collectors.toList()))
                        .flatMap(List::stream)
                        .collect(Collectors.toList()))

                .withType(getHearingType(hearingData))
                .withReportingRestrictionReason(of(hearingData.getReportingRestrictionReason()))
                .build();
    }

    private HearingUnscheduledListingNeeds buildUnscheduledHearingListingNeeds(final HearingData hearingData) {
        return HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
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
                        .withUserId(hearingData.getJudiciary().get(0).getUserId())
                        .build())
                        : null)
                .withJurisdictionType(hearingData.getJurisdictionType() != null ? JurisdictionType.valueFor(hearingData.getJurisdictionType()).get() : null)
                .withWeekCommencingDate(hearingData.getWeekCommencingStartDate() == null ? Optional.empty() :
                        of(WeekCommencingDate.weekCommencingDate()
                                .withStartDate(FORMATTER.format(hearingData.getWeekCommencingStartDate()))
                                .withDuration(of(hearingData.getWeekCommencingDuration()))
                                .build()))
                .withProsecutionCases(buildProsecutionCases(hearingData))
                .withDefendantListingNeeds(hearingData.getListedCases().stream()
                        .map(lc -> lc.getDefendants().stream().map(d ->
                                getDefendantListingNeeds(lc, d))
                                .collect(Collectors.toList()))
                        .flatMap(List::stream)
                        .collect(Collectors.toList()))

                .withType(getHearingType(hearingData))
                .withReportingRestrictionReason(of(hearingData.getReportingRestrictionReason()))
                .build();
    }

    private List<ProsecutionCase> buildProsecutionCases(final HearingData hearingData) {
        final ListedCaseData listedCaseData = hearingData.getListedCases().get(0);
        return hearingData.getListedCases().stream()
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
                                .withPersonDefendant(of(buildPersonDefendant(d)))
                                .withAssociatedPersons(singletonList(AssociatedPerson.associatedPerson()
                                        .withRole(of(STRING.next()))
                                        .withPerson(getPerson(d))
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
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withSeedingHearingId(firstHearing.getId())
                                                        .withSittingDay(firstHearing.getHearingStartDate().toString())
                                                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueOf(firstHearing.getJurisdictionType()))
                                                        .build())
                                                .withReportingRestrictions(Arrays.asList(ReportingRestriction.reportingRestriction().withId(randomUUID())
                                                        .withLabel("RestrictionApplied")
                                                        .withJudicialResultId(JUDICIAL_RESULT_ID)
                                                        .withOrderedDate(LocalDate.now().toString()).build()))
                                                .build())
                                        .collect(Collectors.toList()))
                                .withProsecutionCaseId(listedCaseData.getCaseId())
                                .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    private Person getPerson(final DefendantData d) {
        return Person.person()
                .withAdditionalNationalityId(of(randomUUID()))
                .withGender(Gender.FEMALE)
                .withLastName(d.getLastName())
                .withNationalityId(of(randomUUID()))
                .withTitle(of(PERSON_TITLE))
                .withEthnicity(of(Ethnicity.ethnicity()
                        .withObservedEthnicityId(of(randomUUID()))
                        .withObservedEthnicityDescription(of(STRING.next()))
                        .build()))
                .build();
    }

    private DefendantListingNeeds getDefendantListingNeeds(final ListedCaseData lc, final DefendantData d) {
        return DefendantListingNeeds.defendantListingNeeds()
                .withDefendantId(d.getDefendantId())
                .withProsecutionCaseId(lc.getCaseId())
                .build();
    }

    private HearingType getHearingType(final HearingData hearingData) {
        return HearingType.hearingType()
                .withDescription(hearingData.getHearingTypeData().getTypeDescription())
                .withId(hearingData.getHearingTypeData().getTypeId())
                .build();
    }


    private PersonDefendant buildPersonDefendant(final DefendantData d) {
        return PersonDefendant.personDefendant()
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
                .build();
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

    private CourtApplicationParty getApplicant(final ApplicantRespondentData applicant) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(applicant.getId())
                .withPersonDetails(of(Person.person().withLastName(applicant.getLastName())
                        .withFirstName(of(applicant.getFirstName()))
                        .withGender(Gender.FEMALE)
                        .withAddress(getAddress(applicant.getAddress()))
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

}
