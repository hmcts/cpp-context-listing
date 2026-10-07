package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub;
import uk.gov.moj.cpp.listing.utils.QueueUtil;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class GroupCasesIT extends AbstractIT {

    private static final String LIST_COURT_HEARING_JSON = "list-court-hearing-group-cases";
    private static final String LIST_COURT_HEARING_GROUP_CASES_PART_CASES_JSON = "list-court-hearing-group-cases-part-cases.json";
    private static final String PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES_JSON = "public.progression.case-removed-from-group-cases.json";
    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing.search.hearing+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_COURT_CALENDAR_JSON = "application/vnd.listing.search.hearings.court.calendar+json";
    private static final int LISTED_NUMBER_OF_GROUP_CASES = 1000;
    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING = "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String COURT_ROOM_ID = "28b922c3-0396-3c68-970f-5b805c7ab1bb";
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD = "application/vnd.listing.search.court.list.payload+json";
    private static final String ALPHABETICAL_COURT_LIST = "Alphabetical";
    private static final String STANDARD_COURT_LIST = "Standard";

    private final UUID hearingId = randomUUID();
    private final UUID groupId = randomUUID();
    private final UUID hearingTypeId = randomUUID();
    private final UUID courtCentreId = getRandomCourtCenterId();
    private final LocalDate startDate = ItClock.today();
    private LocalTime defaultStartTime = LocalTime.parse("10:00");
    private final ZonedDateTime hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
    private long defaultDuration = 20;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private JmsMessageProducerClient publicMessageProducer;
    private final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps();

    @BeforeEach
    public void setup() {
        publicMessageProducer = QueueUtil.publicEvents.createPublicProducer();
    }

    @Test
    void shouldSaveGroupCasesWithFilteringMembers() throws IOException {
        final UUID masterCaseId = randomUUID();
        final UUID newGroupMasterCaseId = randomUUID();

        stubGetReferenceDataCourtCentreById(courtCentreId);
        CourtSchedulerServiceStub.stubSearchBookHearingSlotsForCrown(
                hearingId.toString(), courtCentreId.toString(), "28b922c3-0396-3c68-970f-5b805c7ab1bb");
        postListCourtHearingCommand(masterCaseId);

        JsonPath jsonResponse = listCourtHearingSteps.getHearingConfirmedPublicEventPayload();
        assertPublicHearingConfirmed(jsonResponse, masterCaseId, LISTED_NUMBER_OF_GROUP_CASES);
        assertViewStoreUpdated(masterCaseId, Collections.emptyList(), LISTED_NUMBER_OF_GROUP_CASES);

        // CAD-947: the removal event carries no numberOfGroupCases, so the stored group size is left unchanged
        publishCaseRemovedFromGroupCasesEvent(masterCaseId, masterCaseId, newGroupMasterCaseId);
        assertViewStoreUpdated(newGroupMasterCaseId, Arrays.asList(masterCaseId), LISTED_NUMBER_OF_GROUP_CASES);
    }

    @Test
    void shouldKeepHearingForRemovedMemberCaseWhenDefendantHasNoYouthFlag() throws IOException {
        final UUID masterCaseId = randomUUID();
        final UUID memberCaseId = randomUUID();

        stubGetReferenceDataCourtCentreById(courtCentreId);
        CourtSchedulerServiceStub.stubSearchBookHearingSlotsForCrown(
                hearingId.toString(), courtCentreId.toString(), "28b922c3-0396-3c68-970f-5b805c7ab1bb");
        postListCourtHearingCommand(masterCaseId);

        assertPublicHearingConfirmed(listCourtHearingSteps.getHearingConfirmedPublicEventPayload(), masterCaseId, LISTED_NUMBER_OF_GROUP_CASES);
        assertViewStoreUpdated(masterCaseId, Collections.emptyList(), LISTED_NUMBER_OF_GROUP_CASES);

        publishMemberCaseRemovedFromGroupCasesEvent(masterCaseId, memberCaseId);

        verifyHearingInViewStore(Arrays.asList(
                withJsonPath("$.id", equalTo(hearingId.toString())),
                withJsonPath("$.listedCases[?(@.id == '" + masterCaseId + "')].isGroupMaster", contains(true)),
                withJsonPath("$.listedCases[?(@.id == '" + memberCaseId + "')].isGroupMember", contains(false)),
                withJsonPath("$.listedCases[?(@.id == '" + memberCaseId + "')].isGroupMaster", contains(false))));

        verifyHearingsForCase(memberCaseId, withJsonPath("$.hearings[*].id", hasItem(hearingId.toString())));
    }

    @Test
    void shouldReturnRemainingGroupSizeFromSearchEndpointsWhenRemovalEventCarriesCount() throws IOException {
        final UUID masterCaseId = randomUUID();
        final UUID removedMemberCaseId = randomUUID();
        final UUID remainingMemberCaseId = randomUUID();

        listGroupHearingOfThree(masterCaseId, removedMemberCaseId, remainingMemberCaseId);

        publishMemberCaseRemovedFromGroupCasesEvent(masterCaseId, removedMemberCaseId, 2);

        assertGroupSizeAfterRemoval(masterCaseId, removedMemberCaseId, remainingMemberCaseId, 2);
    }

    @Test
    void shouldLeaveGroupSizeUnchangedWhenRemovalEventHasNoCount() throws IOException {
        final UUID masterCaseId = randomUUID();
        final UUID removedMemberCaseId = randomUUID();
        final UUID remainingMemberCaseId = randomUUID();

        listGroupHearingOfThree(masterCaseId, removedMemberCaseId, remainingMemberCaseId);

        publishMemberCaseRemovedFromGroupCasesEvent(masterCaseId, removedMemberCaseId, null);

        assertGroupSizeAfterRemoval(masterCaseId, removedMemberCaseId, remainingMemberCaseId, 3);
    }

    @Test
    void shouldCarryRemainingGroupSizeOnHearingConfirmedWhenAllocatedAfterRemoval() throws IOException {
        final UUID masterCaseId = randomUUID();
        final UUID removedMemberCaseId = randomUUID();
        final UUID remainingMemberCaseId = randomUUID();
        final JmsMessageConsumerClient hearingConfirmedConsumer = QueueUtil.publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_CONFIRMED);
        final JmsMessageConsumerClient hearingUpdatedConsumer = QueueUtil.publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_UPDATED);

        listUnallocatedGroupHearingOfThree(masterCaseId, removedMemberCaseId, remainingMemberCaseId);

        publishMemberCaseRemovedFromGroupCasesEvent(masterCaseId, removedMemberCaseId, 2);
        verifyHearingInViewStore(Arrays.asList(
                withJsonPath("$.id", equalTo(hearingId.toString())),
                withJsonPath("$.numberOfGroupCases", equalTo(2)),
                withJsonPath("$.listedCases[?(@.id == '" + removedMemberCaseId + "')].isGroupMember", contains(false))));

        // allocating after the removal raises HearingAllocatedForListingV2 -> public.listing.hearing-confirmed
        updateHearingForListingWithCourtRoom();
        final JsonPath hearingConfirmed = retrieveMessage(hearingConfirmedConsumer, containsString(hearingId.toString()));
        assertThat(hearingConfirmed.get("confirmedHearing.id"), is(hearingId.toString()));
        // isGroupProceedings is not asserted: update-hearing-for-listing passes null for it (ListingCommandHandler), existing behaviour
        assertThat(hearingConfirmed.get("confirmedHearing.numberOfGroupCases"), is(2));

        // a further update of the now allocated hearing raises AllocatedHearingUpdatedForListingV2 -> public.listing.hearing-updated
        updateHearingForListingWithCourtRoom();
        final JsonPath hearingUpdated = retrieveMessage(hearingUpdatedConsumer, containsString(hearingId.toString()));
        assertThat(hearingUpdated.get("updatedHearing.id"), is(hearingId.toString()));
        assertThat(hearingUpdated.get("updatedHearing.numberOfGroupCases"), is(2));
    }

    private void listUnallocatedGroupHearingOfThree(final UUID masterCaseId, final UUID memberCase1Id, final UUID memberCase2Id) throws IOException {
        stubGetReferenceDataCourtCentreById(courtCentreId);

        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps
                .preparePayloadToListCourtHearingForGroupCases(LIST_COURT_HEARING_JSON, getPayloadValues(), groupId,
                        masterCaseId, Arrays.asList(memberCase1Id, memberCase2Id));
        listCourtHearingSteps.listCourtHearing(withoutCourtRoomAndBooking(withNumberOfGroupCases(listCourtHearingJsonObject, 3)),
                courtCentreId, hearingTypeId);

        verifyHearingInViewStore(Arrays.asList(
                withJsonPath("$.id", equalTo(hearingId.toString())),
                withJsonPath("$.numberOfGroupCases", equalTo(3))));
    }

    private void updateHearingForListingWithCourtRoom() {
        CourtSchedulerServiceStub.stubSearchBookHearingSlotsForCrown(hearingId.toString(), courtCentreId.toString(), COURT_ROOM_ID);
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.command.update-hearing-for-listing"), hearingId.toString()));
        final JsonObject updateHearing = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtRoomId", COURT_ROOM_ID)
                .add("type", createObjectBuilder().add("id", hearingTypeId.toString()).add("description", "PTP"))
                .add("startDate", startDate.toString())
                .add("endDate", startDate.toString())
                .add("nonSittingDays", createArrayBuilder())
                .add("nonDefaultDays", createArrayBuilder())
                .add("judiciary", createArrayBuilder())
                .add("jurisdictionType", "CROWN")
                .add("hearingLanguage", "ENGLISH")
                .add("sendNotificationToParties", false)
                .build();

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                updateHearing.toString(), getLoggedInHeader());
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    private JsonObject withoutCourtRoomAndBooking(final JsonObject listCourtHearingPayload) {
        final JsonArrayBuilder hearings = createArrayBuilder();
        listCourtHearingPayload.getJsonArray("hearings").getValuesAs(JsonObject.class)
                .forEach(hearing -> hearings.add(copyWithout(hearing, "bookingReference")
                        .add("courtCentre", copyWithout(hearing.getJsonObject("courtCentre"), "roomId"))));
        return createObjectBuilder(listCourtHearingPayload).add("hearings", hearings).build();
    }

    private static JsonObjectBuilder copyWithout(final JsonObject source, final String excludedKey) {
        final JsonObjectBuilder builder = createObjectBuilder();
        source.forEach((key, value) -> {
            if (!excludedKey.equals(key)) {
                builder.add(key, value);
            }
        });
        return builder;
    }

    private void listGroupHearingOfThree(final UUID masterCaseId, final UUID memberCase1Id, final UUID memberCase2Id) throws IOException {
        stubGetReferenceDataCourtCentreById(courtCentreId);
        CourtSchedulerServiceStub.stubSearchBookHearingSlotsForCrown(
                hearingId.toString(), courtCentreId.toString(), "28b922c3-0396-3c68-970f-5b805c7ab1bb");

        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps
                .preparePayloadToListCourtHearingForGroupCases(LIST_COURT_HEARING_JSON, getPayloadValues(), groupId,
                        masterCaseId, Arrays.asList(memberCase1Id, memberCase2Id));
        listCourtHearingSteps.listCourtHearing(withNumberOfGroupCases(listCourtHearingJsonObject, 3), courtCentreId, hearingTypeId);

        // listed allocated (Crown booking + room), so hearing-confirmed now carries the listed group size
        final JsonPath hearingConfirmed = listCourtHearingSteps.getHearingConfirmedPublicEventPayload();
        assertThat(hearingConfirmed.get("confirmedHearing.id"), is(hearingId.toString()));
        assertThat(hearingConfirmed.get("confirmedHearing.isGroupProceedings"), is(true));
        assertThat(hearingConfirmed.get("confirmedHearing.numberOfGroupCases"), is(3));
        assertThat(hearingConfirmed.get("confirmedHearing.prosecutionCases"), hasSize(3));

        verifyHearingInViewStore(Arrays.asList(
                withJsonPath("$.id", equalTo(hearingId.toString())),
                withJsonPath("$.numberOfGroupCases", equalTo(3)),
                withJsonPath("$.listedCases", hasSize(1)),
                withJsonPath("$.listedCases[0].id", equalTo(masterCaseId.toString()))));
    }

    private void assertGroupSizeAfterRemoval(final UUID masterCaseId, final UUID removedMemberCaseId,
                                             final UUID remainingMemberCaseId, final int expectedNumberOfGroupCases) {
        final String hearingFilter = "$.hearings[?(@.id == '" + hearingId + "')]";

        final ResponseData hearingResponse = verifyHearingInViewStore(Arrays.asList(
                withJsonPath("$.id", equalTo(hearingId.toString())),
                withJsonPath("$.numberOfGroupCases", equalTo(expectedNumberOfGroupCases)),
                withJsonPath("$.startDate", notNullValue()),
                withJsonPath("$.listedCases[?(@.id == '" + removedMemberCaseId + "')].isGroupMember", contains(false))));
        // the Crown booking decides the hearing day, so search on the stored start date
        final String hearingDate = new JsonPath(hearingResponse.getPayload()).getString("startDate");

        // GET /hearings/?allocated&courtCentreId&searchDate (listing.search.hearings)
        final String searchHearingsUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.search-date"),
                        true, courtCentreId.toString(), hearingDate));
        pollWithDefaults(requestParams(searchHearingsUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK), payload().isJson(allOf(
                        withJsonPath(hearingFilter + ".numberOfGroupCases", contains(expectedNumberOfGroupCases)),
                        // the removed case is shown separately; the remaining member is still folded into the master
                        withJsonPath(hearingFilter + ".listedCases[*].id", hasItem(masterCaseId.toString())),
                        withJsonPath(hearingFilter + ".listedCases[*].id", hasItem(removedMemberCaseId.toString())),
                        withJsonPath(hearingFilter + ".listedCases[*].id", not(hasItem(remainingMemberCaseId.toString()))))));

        // GET /hearings/range-search court calendar (allocated=true, courtSession=Any)
        final String courtCalendarUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearingscourt.calendar.by.allocated.court-centre-id.start-date.end-date"),
                        true, courtCentreId.toString(), hearingDate, hearingDate)) + "&courtSession=Any";
        pollWithDefaults(requestParams(courtCalendarUrl, MEDIA_TYPE_SEARCH_HEARINGS_COURT_CALENDAR_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK), payload().isJson(
                        withJsonPath(hearingFilter + ".numberOfGroupCases", contains(expectedNumberOfGroupCases))));

        // GET /hearings/range-search (listing.range.search.hearings, search.hearings media type)
        final String rangeSearchUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.start-date.end-date"),
                        true, courtCentreId.toString(), hearingDate, hearingDate));
        pollWithDefaults(requestParams(rangeSearchUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK), payload().isJson(
                        withJsonPath(hearingFilter + ".numberOfGroupCases", contains(expectedNumberOfGroupCases))));

        assertCourtListsShowGroupSize(hearingDate, expectedNumberOfGroupCases);
    }

    /**
     * The alphabetical and standard court lists print the group master as "N DEFENDANTS" / "N Defendants"
     * from the stored numberOfGroupCases (AlphabeticalCourtListService, StandardPublicCourtListTemplateAssembler).
     * Checked on the document-generator payload rather than the rendered document.
     */
    private void assertCourtListsShowGroupSize(final String hearingDate, final int expectedNumberOfGroupCases) {
        stubGetReferenceDataCourtCentre(new CourtCentreData(courtCentreId, defaultStartTime, "6:30",
                fromString(COURT_ROOM_ID), "Preston Crown Court"));

        pollWithDefaults(requestParams(courtListPayloadUrl(ALPHABETICAL_COURT_LIST, hearingDate), MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK), payload().isJson(
                        withJsonPath("$.defendants[*].defendantFullName", hasItem(expectedNumberOfGroupCases + " DEFENDANTS"))));

        pollWithDefaults(requestParams(courtListPayloadUrl(STANDARD_COURT_LIST, hearingDate), MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK), payload().isJson(
                        withJsonPath("$..hearings[?(@.id == '" + hearingId + "')].defendants[*].surname",
                                hasItem(expectedNumberOfGroupCases + " Defendants"))));
    }

    private String courtListPayloadUrl(final String listId, final String hearingDate) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.court.list.payload-court-room-id"),
                        courtCentreId.toString(), hearingDate, listId, hearingDate, COURT_ROOM_ID));
    }

    private JsonObject withNumberOfGroupCases(final JsonObject listCourtHearingPayload, final int numberOfGroupCases) {
        final JsonArrayBuilder hearings = createArrayBuilder();
        listCourtHearingPayload.getJsonArray("hearings").getValuesAs(JsonObject.class)
                .forEach(hearing -> hearings.add(createObjectBuilder(hearing).add("numberOfGroupCases", numberOfGroupCases)));
        return createObjectBuilder(listCourtHearingPayload).add("hearings", hearings).build();
    }

    private void postListCourtHearingCommand(final UUID masterCaseId) throws IOException {
        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps
                .preparePayloadToListCourtHearingForGroupCases(LIST_COURT_HEARING_JSON,
                        getPayloadValues(), groupId, masterCaseId);

        listCourtHearingSteps.listCourtHearing(listCourtHearingJsonObject, courtCentreId, hearingTypeId);
    }

    private void publishCaseRemovedFromGroupCasesEvent(final UUID masterCaseId, final UUID removedCaseId,
                                                       final UUID newGroupMasterCaseId) throws IOException {
        final JsonObject caseRemovedFromGroupCasesJson = listCourtHearingSteps
                .preparePayloadCaseRemovedFromGroupCases(PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES_JSON,
                        LIST_COURT_HEARING_GROUP_CASES_PART_CASES_JSON,
                        groupId, masterCaseId, removedCaseId, newGroupMasterCaseId);

        sendMessage(publicMessageProducer,
                "public.progression.case-removed-from-group-cases",
                caseRemovedFromGroupCasesJson,
                metadataBuilder().withId(randomUUID())
                        .withName("public.progression.case-removed-from-group-cases")
                        .withUserId(randomUUID().toString())
                        .build());
    }

    private void publishMemberCaseRemovedFromGroupCasesEvent(final UUID masterCaseId, final UUID removedCaseId) throws IOException {
        publishMemberCaseRemovedFromGroupCasesEvent(masterCaseId, removedCaseId, null);
    }

    private void publishMemberCaseRemovedFromGroupCasesEvent(final UUID masterCaseId, final UUID removedCaseId,
                                                             final Integer numberOfGroupCases) throws IOException {
        final JsonObject memberCaseRemovedJson = listCourtHearingSteps
                .preparePayloadMemberCaseRemovedFromGroupCases(PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES_JSON,
                        LIST_COURT_HEARING_GROUP_CASES_PART_CASES_JSON,
                        groupId, masterCaseId, removedCaseId, false);
        final JsonObject caseRemovedFromGroupCasesJson = numberOfGroupCases == null ? memberCaseRemovedJson :
                createObjectBuilder(memberCaseRemovedJson).add("numberOfGroupCases", numberOfGroupCases).build();

        sendMessage(publicMessageProducer,
                "public.progression.case-removed-from-group-cases",
                caseRemovedFromGroupCasesJson,
                metadataBuilder().withId(randomUUID())
                        .withName("public.progression.case-removed-from-group-cases")
                        .withUserId(randomUUID().toString())
                        .build());
    }

    private void verifyHearingsForCase(final UUID caseId, final Matcher matcher) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.unallocated-hearings"), caseId.toString()));
        pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK), payload().isJson(matcher));
    }

    private void assertPublicHearingConfirmed(final JsonPath jsonResponse, final UUID masterCaseId, final int expectedNumberOfGroupCases) {
        assertThat(jsonResponse.get("confirmedHearing.id"), is(hearingId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.isGroupProceedings"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.numberOfGroupCases"), is(expectedNumberOfGroupCases));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases"), hasSize(1));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].id"), is(masterCaseId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].isCivil"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].isGroupMember"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].isGroupMaster"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].groupId"), is(groupId.toString()));
    }

    private void assertViewStoreUpdated(final UUID masterCaseId, final List<UUID> removedCaseIds, final int expectedNumberOfGroupCases) {
        final int filteredCases = 1 + removedCaseIds.size();
        final List<Matcher> groupCasesMatcher = new ArrayList<>();

        groupCasesMatcher.add(withJsonPath("$.id", equalTo(hearingId.toString())));
        groupCasesMatcher.add(withJsonPath("$.numberOfGroupCases", equalTo(expectedNumberOfGroupCases)));

        for (int i = 0; i < removedCaseIds.size(); i++) {
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].isCivil", is(true)));
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].groupId", is(groupId.toString())));
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].isGroupMember", is(false)));
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].isGroupMaster", is(false)));
        }

        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].id", is(masterCaseId.toString())));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].isCivil", is(true)));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].groupId", is(groupId.toString())));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].isGroupMember", is(true)));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].isGroupMaster", is(true)));

        verifyHearingInViewStore(groupCasesMatcher);
    }

    private ResponseData verifyHearingInViewStore(final List<Matcher> groupCasesMatcher) {
        final String url = generateUrlForFindingAHearingById(hearingId.toString());
        return pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK),
                        payload().isJson(
                                allOf(groupCasesMatcher.toArray(new Matcher[groupCasesMatcher.size()]))));
    }

    private String generateUrlForFindingAHearingById(final String rawId) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"),
                        rawId
                ));
    }

    private Map<String, String> getPayloadValues() {
        final Map<String, String> payloadValues = new HashMap<>();

        payloadValues.put("hearingId", hearingId.toString());
        payloadValues.put("courtCentreId", courtCentreId.toString());
        payloadValues.put("startDate", startDate.toString());
        payloadValues.put("hearingStartTime", hearingStartTime.format(formatter));
        payloadValues.put("estimatedMinutes", String.valueOf(defaultDuration));
        payloadValues.put("hearingTypeId", hearingTypeId.toString());

        return payloadValues;
    }
}
