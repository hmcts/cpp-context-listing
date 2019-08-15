package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingTypeData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleTypeData;
import uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateHearingSteps extends AbstractIT implements AutoCloseable {

    protected static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    private static final String EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing";
    private static final String EVENT_SELECTOR_HEARING_UNALLOCATED_FOR_LISTING = "listing.events.hearing-unallocated-for-listing";
    private static final String EVENT_SELECTOR_HEARING_TYPE_CHANGED = "listing.events.type-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_JURISDICTION_CHANGED = "listing.events.jurisdiction-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_LANGUAGE_CHANGED = "listing.events.hearing-language-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_START_DATE_CHANGED = "listing.events.start-date-changed-for-hearing";
    private static final String EVENT_SELECTOR_NON_DEFAULT_DAYS_ASSIGNED = "listing.events.non-default-days-assigned-to-hearing";
    private static final String EVENT_SELECTOR_NON_DEFAULT_DAYS_CHANGED = "listing.events.non-default-days-changed-for-hearing";
    private static final String EVENT_SELECTOR_NON_SITTING_DAYS_ASSIGNED = "listing.events.non-sitting-days-assigned-to-hearing";
    private static final String EVENT_SELECTOR_NON_SITTING_DAYS_CHANGED = "listing.events.non-sitting-days-changed-for-hearing";
    private static final String EVENT_SELECTOR_COURT_CENTRE_CHANGED = "listing.events.court-centre-changed-for-hearing";
    private static final String EVENT_SELECTOR_JUDICIARY_ASSIGNED = "listing.events.judiciary-assigned-to-hearing";
    private static final String EVENT_SELECTOR_JUDICIARY_CHANGED = "listing.events.judiciary-changed-for-hearing";
    private static final String EVENT_SELECTOR_COURT_ROOM_ASSIGNED = "listing.events.court-room-assigned-to-hearing";
    private static final String EVENT_SELECTOR_COURT_ROOM_REMOVED = "listing.events.court-room-removed-from-hearing";
    private static final String EVENT_SELECTOR_COURT_ROOM_CHANGED = "listing.events.court-room-changed-for-hearing";
    private static final String EVENT_SELECTOR_END_DATE_ASSIGNED = "listing.events.end-date-assigned-to-hearing";
    private static final String EVENT_SELECTOR_END_DATE_CHANGED = "listing.events.end-date-changed-for-hearing";
    private static final String EVENT_SELECTOR_END_DATE_REMOVED = "listing.events.end-date-removed-from-hearing";
    private static final String EVENT_SELECTOR_HEARING_DAYS_CHANGED = "listing.events.hearing-days-changed-for-hearing";
    private static final String EVENT_SELECTED_PUBLIC_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String EVENT_SELECTED_PUBLIC_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_HEARINGS = "hearings";
    private static final String FIELD_JUDICIARY = "judiciary";
    private static final String FIELD_JUDICIAL_ID = "judicialId";
    private static final String FIELD_JUDICIAL_ROLE_TYPE = "judicialRoleType";
    private static final String FIELD_IS_BENCH_CHAIRMAN = "isBenchChairman";
    private static final String FIELD_IS_DEPUTY = "isDeputy";
    private static final String FIELD_COURT_ROOM_ID = "courtRoomId";
    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_NON_SITTING_DAYS = "nonSittingDays";
    private static final String FIELD_NON_DEFAULT_DAYS = "nonDefaultDays";
    private static final String FIELD_HEARING_LANGUAGE = "hearingLanguage";
    private static final String FIELD_JURISDICTION_TYPE = "jurisdictionType";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";
    private static final String FIELD_HEARING_TYPE_ID = "id";
    private static final String FIELD_HEARING_TYPE_DESCRIPTION = "description";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final int DEFAULT_DURATION_MINS = (6*60)+30;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final String FIELD_JUDICIAL_ROLE_TYPE_ID = "judicialRoleTypeId";
    private static final String FIELD_JUDICIARY_TYPE = "judiciaryType";
    private static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    private static final String LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS = "listing.command.change-judiciary-for-hearings";
    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING = "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS = "application/vnd.listing.command.change-judiciary-for-hearings+json";
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateHearingSteps.class);
    private static final String FIELD_DURATION = "duration";
    private final UpdatedHearingData updatedHearingData;
    private final HearingData hearingData;
    private MessageConsumer privateMessageConsumerAllocatedHearingUpdatedForListing;
    private MessageConsumer privateMessageConsumerHearingAllocatedForListing;
    private MessageConsumer privateMessageConsumerHearingUnallocatedForListing;
    private MessageConsumer privateMessageConsumerTypeChanged;
    private MessageConsumer privateMessageConsumerJurisdictionChanged;
    private MessageConsumer privateMessageConsumerHearingLanguageChanged;
    private MessageConsumer privateMessageConsumerStartDateChanged;
    private MessageConsumer privateMessageConsumerNonDefaultDaysAssigned;
    private MessageConsumer privateMessageConsumerNonDefaultDaysChanged;
    private MessageConsumer privateMessageConsumerNonSittingDaysAssigned;
    private MessageConsumer privateMessageConsumerNonSittingDaysChanged;
    private MessageConsumer privateMessageConsumerCourtCentreChanged;
    private MessageConsumer privateMessageConsumerJudiciaryAssigned;
    private MessageConsumer privateMessageConsumerJudiciaryChanged;
    private MessageConsumer privateMessageConsumerCourtRoomAssigned;
    private MessageConsumer privateMessageConsumerCourtRoomRemoved;
    private MessageConsumer privateMessageConsumerCourtRoomChanged;
    private MessageConsumer privateMessageConsumerEndDateAssigned;
    private MessageConsumer privateMessageConsumerEndDateChanged;
    private MessageConsumer privateMessageConsumerEndDateRemoved;
    private MessageConsumer privateMessageConsumerHearingDaysChanged;
    private MessageConsumer publicMessageConsumerHearingConfirmed;
    private MessageConsumer publicMessageConsumerHearingUpdated;
    private String request;


    public UpdateHearingSteps(HearingsData hearingsData, UpdatedHearingData updatedHearingData) {
        this.hearingData = hearingsData.getHearingData().get(0);
        this.updatedHearingData = updatedHearingData;
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        createMessageConsumers();

    }

    private static String prepareJsonForUpdatedHearingData(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, prepareJsonHearingType(updatedHearingData.getHearingTypData()))
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_JURISDICTION_TYPE, updatedHearingData.getJurisdictionType())
                .add(FIELD_HEARING_LANGUAGE, updatedHearingData.getHearingLanguage())
                .add(FIELD_COURT_CENTRE_ID, updatedHearingData.getCourtCentreId().toString())
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()))
                .add(FIELD_NON_DEFAULT_DAYS, prepareJsonNonDefaultDays(updatedHearingData.getNonDefaultDays()))
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()));

        addNullableStringField(builder, FIELD_END_DATE, updatedHearingData.getEndDate());
        addNullableStringField(builder, FIELD_COURT_ROOM_ID, getStringOrNull(updatedHearingData.getCourtRoomId()));

        return builder.build().toString();
    }

    private static String prepareJsonForChangeJudiciaryForHearings(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_HEARINGS, prepareJsonHearingIdArray(updatedHearingData.getHearingId()))
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()));

        return builder.build().toString();
    }

    private static String getStringOrNull(UUID id) {
        return id != null ? id.toString() : null;
    }

    private static JsonArray prepareJsonStringArray(List<String> strings) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        if (strings != null && !strings.isEmpty()) {
            strings.forEach(builder::add);
        }
        return builder.build();

    }

    private static JsonArray prepareJsonHearingIdArray(UUID hearingId) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add(hearingId.toString());
        return builder.build();

    }

    private static JsonArrayBuilder prepareJsonJudiciary(List<JudicialRoleData> roleData) {
        if (roleData != null && !roleData.isEmpty()) {
            return roleData.stream()
                    .map(ndd -> {
                        JsonObjectBuilder builder = createObjectBuilder()
                                .add(FIELD_JUDICIAL_ID, ndd.getJudicialId().toString())
                                .add(FIELD_JUDICIAL_ROLE_TYPE, prepareJudicialRoleType(ndd.getJudicialRoleType()));
                        addOptionalBooleanField(builder, FIELD_IS_DEPUTY, ndd.getIsDeputy());
                        addOptionalBooleanField(builder, FIELD_IS_BENCH_CHAIRMAN, ndd.getIsBenchChairman());

                        return builder;
                    })
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
        }
        return Json.createArrayBuilder();
    }

    private static JsonObjectBuilder prepareJudicialRoleType(JudicialRoleTypeData judicialRoleType) {
        if (judicialRoleType != null ) {
            JsonObjectBuilder builder = createObjectBuilder()
                    .add(FIELD_JUDICIARY_TYPE, judicialRoleType.getJudiciaryType());
            addNullableUUIDField(builder, FIELD_JUDICIAL_ROLE_TYPE_ID, judicialRoleType.getJudicialRoleTypeId());
            return builder;
        }
        return null;
    }

    private static JsonObjectBuilder prepareJsonHearingType(HearingTypeData hearingType) {
        if (hearingType != null ) {
            return createObjectBuilder()
                    .add(FIELD_HEARING_TYPE_ID, hearingType.getTypeId().toString())
                    .add(FIELD_HEARING_TYPE_DESCRIPTION, hearingType.getTypeDescription());
        }
        return null;
    }

    private static JsonArrayBuilder prepareJsonNonDefaultDays(List<NonDefaultDayData> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> {
                    JsonObjectBuilder nonDefaultDayBuilder = createObjectBuilder()
                            .add(FIELD_START_TIME, ndd.getStartTime());
                    addNullableIntegerField(nonDefaultDayBuilder, FIELD_DURATION, ndd.getDuration());

                    return nonDefaultDayBuilder;
                })
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static void addNullableStringField(JsonObjectBuilder builder, String fieldName, String value) {
        if (value != null) {
            builder.add(fieldName, value);
        } else {
            builder.addNull(fieldName);
        }
    }

    private static void addNullableIntegerField(JsonObjectBuilder builder, String fieldName, Optional<Integer> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        } else {
            builder.addNull(fieldName);
        }
    }

    private static void addNullableUUIDField(JsonObjectBuilder builder, String fieldName, Optional<UUID> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get().toString());
        }
    }

    private static void addOptionalBooleanField(JsonObjectBuilder builder, String fieldName, Optional<Boolean> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        }
    }

    private void createMessageConsumers() {
        privateMessageConsumerAllocatedHearingUpdatedForListing = privateEvents.createConsumer(EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING);
        privateMessageConsumerHearingAllocatedForListing = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING);
        privateMessageConsumerHearingUnallocatedForListing = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_UNALLOCATED_FOR_LISTING);
        privateMessageConsumerTypeChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_TYPE_CHANGED);
        privateMessageConsumerJurisdictionChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_JURISDICTION_CHANGED);
        privateMessageConsumerHearingLanguageChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_LANGUAGE_CHANGED);
        privateMessageConsumerStartDateChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_START_DATE_CHANGED);
        privateMessageConsumerNonDefaultDaysAssigned = privateEvents.createConsumer(EVENT_SELECTOR_NON_DEFAULT_DAYS_ASSIGNED);
        privateMessageConsumerNonDefaultDaysChanged = privateEvents.createConsumer(EVENT_SELECTOR_NON_DEFAULT_DAYS_CHANGED);
        privateMessageConsumerNonSittingDaysAssigned = privateEvents.createConsumer(EVENT_SELECTOR_NON_SITTING_DAYS_ASSIGNED);
        privateMessageConsumerNonSittingDaysChanged = privateEvents.createConsumer(EVENT_SELECTOR_NON_SITTING_DAYS_CHANGED);
        privateMessageConsumerCourtCentreChanged = privateEvents.createConsumer(EVENT_SELECTOR_COURT_CENTRE_CHANGED);
        privateMessageConsumerJudiciaryAssigned = privateEvents.createConsumer(EVENT_SELECTOR_JUDICIARY_ASSIGNED);
        privateMessageConsumerJudiciaryChanged = privateEvents.createConsumer(EVENT_SELECTOR_JUDICIARY_CHANGED);
        privateMessageConsumerCourtRoomAssigned = privateEvents.createConsumer(EVENT_SELECTOR_COURT_ROOM_ASSIGNED);
        privateMessageConsumerCourtRoomRemoved = privateEvents.createConsumer(EVENT_SELECTOR_COURT_ROOM_REMOVED);
        privateMessageConsumerCourtRoomChanged = privateEvents.createConsumer(EVENT_SELECTOR_COURT_ROOM_CHANGED);
        privateMessageConsumerEndDateAssigned = privateEvents.createConsumer(EVENT_SELECTOR_END_DATE_ASSIGNED);
        privateMessageConsumerEndDateChanged = privateEvents.createConsumer(EVENT_SELECTOR_END_DATE_CHANGED);
        privateMessageConsumerEndDateRemoved = privateEvents.createConsumer(EVENT_SELECTOR_END_DATE_REMOVED);
        privateMessageConsumerHearingDaysChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_DAYS_CHANGED);
        publicMessageConsumerHearingConfirmed = publicEvents.createConsumer(EVENT_SELECTED_PUBLIC_HEARING_CONFIRMED);
        publicMessageConsumerHearingUpdated = publicEvents.createConsumer(EVENT_SELECTED_PUBLIC_HEARING_UPDATED);
    }

    public void whenHearingIsUpdatedForListing() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId()));

        final String updateHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingData(updatedHearingData);

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenJudiciaryIsChangedForHearings() {

        final String updateHearingUrl = String.format("%s/%s", baseUri, format
                (ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS), updatedHearingData.getHearingId()));

        request = prepareJsonForChangeJudiciaryForHearings(updatedHearingData);

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    @Override
    public void close() {
        try {

            closeMessageConsumers();

        } catch (JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }
    }



    public void verifyHearingUpdatedResultsInAllocationInMQ() {

        JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        verifyTypeChangedEvent();
        verifyHearingLanguageChangedEvent();
        verifyNonSittingDaysAssignedEvent();
        verifyCourtCentreChangedEvent();
        verifyJudiciaryAssignedEvent();
        verifyCourtRoomAssignedEvent();
        verifyEndDateChangedEvent();
        verifyHearingDaysChangedEventForBothDays();
        verifyHearingAllocatedEvent();
    }

    private void verifyHearingDaysChangedEventForBothDays() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());

        verifyFirstHearingDay(jsonResponse);

        //test usage of reference data courtcentre default startTime and duration
        ZonedDateTime lastHearingDayStartDateTime = ZonedDateTime.of(LocalDate.parse(updatedHearingData.getEndDate()), DEFAULT_START_TIME, BST)
                .withZoneSameInstant(UTC);
        assertThat(jsonResponse.get("hearingDays[1].startTime"), is(lastHearingDayStartDateTime.format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("hearingDays[1].durationMinutes"), is(DEFAULT_DURATION_MINS));
        assertThat(jsonResponse.get("hearingDays[1].endTime"),
                is(lastHearingDayStartDateTime.plusMinutes(DEFAULT_DURATION_MINS).format(ZONED_DATE_TIME_FORMAT)));

    }

    private void verifyHearingDaysChangedEventForOneDayOnly() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());

        verifyFirstHearingDay(jsonResponse);
    }

    private void verifyFirstHearingDay(JsonPath jsonResponse) {
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        String startTime = updatedHearingData.getNonDefaultDays().get(0).getStartTime();
        assertThat(jsonResponse.get("hearingDays[0].startTime"),
                is(fromString(startTime).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("hearingDays[0].sequence"),
                is(0));
        Integer duration = updatedHearingData.getNonDefaultDays().get(0).getDuration().get();
        assertThat(jsonResponse.get("hearingDays[0].durationMinutes"),
                is(duration));
        assertThat(jsonResponse.get("hearingDays[0].endTime"),
                is(fromString(startTime).plusMinutes(duration).format(ZONED_DATE_TIME_FORMAT)));
    }

    public void verifyEmptyHearingDaysChangedEventInActiveMQ() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

        assertThat(jsonResponse.get("hearingDays"), hasSize(0));
    }

    public void verifyHearingUpdatedResultsInMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyStartDateChangedEvent();
        verifyCourtRoomChangedEvent();
        verifyEndDateChangedEvent();
        verifyJudiciaryChangedEvent();
        verifyHearingDaysChangedEventForOneDayOnly();
        verifyAllocatedHearingUpdatedForListing();

    }

    public void verifyHearingConfirmedInPublicMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyHearingConfirmedEvent();
    }

    public void verifyHearingConfirmedInPublicMQHasNoJudiciary() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyHearingConfirmedHasNoJudiciary();
    }

    private void verifyHearingConfirmedHasNoJudiciary() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmed);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingConfirmed: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("confirmedHearing.id"), is(updatedHearingData.getHearingId().toString()));
        assertNull(jsonResponse.get("confirmedHearing.judiciary"));
    }

    public void verifyHearingUpdatedInPublicMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyHearingUpdatedEvent();
    }

    private void verifyHearingConfirmedEvent() {

        JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmed);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingConfirmed: {}", jsonResponse.prettify());

        verifyHearingPublicDetails(jsonResponse, "confirmedHearing");
    }

    private void verifyHearingUpdatedEvent() {

        JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingUpdated: {}", jsonResponse.prettify());

        verifyHearingPublicDetails(jsonResponse, "updatedHearing");
    }

    private void verifyHearingPublicDetails(JsonPath jsonResponse, String publicEventType) {
        assertThat(jsonResponse.get(publicEventType+".id"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get(publicEventType+".courtCentre.roomId"), is(updatedHearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get(publicEventType+".courtCentre.id"), is(updatedHearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get(publicEventType+".courtApplicationIds[0]"), is(hearingData.getCourtApplications().get(0).getId().toString()));
        assertThat(jsonResponse.get(publicEventType+".hearingLanguage"), is(updatedHearingData.getHearingLanguage()));
        assertThat(jsonResponse.get(publicEventType+".type.id"), is(updatedHearingData.getHearingTypData().getTypeId().toString()));
        assertThat(jsonResponse.get(publicEventType+".type.description"), is(updatedHearingData.getHearingTypData().getTypeDescription()));
        assertThat(jsonResponse.get(publicEventType+".judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get(publicEventType+".judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
        assertThat(jsonResponse.get(publicEventType+".prosecutionCases[0].id"), is(hearingData.getListedCases().get(0).getCaseId().toString()));
        assertThat(jsonResponse.get(publicEventType+".prosecutionCases[0].defendants[0].id"), is(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString()));
        assertThat(jsonResponse.get(publicEventType+".prosecutionCases[0].defendants[0].offences[0].id"), is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));
        assertThat(jsonResponse.get(publicEventType+".hearingDays[0].sittingDay"),
                is(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get(publicEventType+".hearingDays[0].listedDurationMinutes"), is(updatedHearingData.getNonDefaultDays().get(0).getDuration().get()));
        assertThat(jsonResponse.get(publicEventType+".reportingRestrictionReason"), is(hearingData.getReportingRestrictionReason()));
    }

    public void verifyHearingUpdatedWithNoCourtRoomResultsInUnallocationInMQ() {

        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyCourtRoomRemovedEvent();
        verifyUnallocatedHearingEvent();
    }

    public void verifyHearingUpdatedWithNoEndDateResultsInUnallocationInMQ() {

        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyEndDateRemovedEvent();
        verifyUnallocatedHearingEvent();
        verifyEmptyHearingDaysChangedEventInActiveMQ();
    }

    public void verifyHearingWithUpdatedJudiciaryInMQ() {

        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyJudiciaryChangedForHearing();
        verifyAllocatedHearingUpdatedForListing();
    }

    private void verifyUnallocatedHearingEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingUnallocatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingUnallocatedForListing: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    private void verifyEndDateRemovedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerEndDateRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

    }

    private void verifyJudiciaryChangedForHearing() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

    }

    private void verifyCourtRoomRemovedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtRoomRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerCourtRoomRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    private void verifyEndDateChangedEvent() {
        JsonPath jsonResponse;
        jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerEndDateChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("endDate"), is(updatedHearingData.getEndDate()));

    }

    private void verifyCourtRoomChangedEvent() {
        JsonPath jsonResponse;
        jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtRoomChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerCourtRoomChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("courtRoomId"), is(updatedHearingData.getCourtRoomId().toString()));

    }

    private void verifyJudiciaryAssignedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryAssigned);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryAssigned: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
    }

    private void verifyStartDateChangedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerStartDateChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerStartDateChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("startDate"), is(updatedHearingData.getStartDate()));
    }

    private void verifyTypeChangedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerTypeChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerTypeChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("type.description"), is(updatedHearingData.getHearingTypData().getTypeDescription()));
    }

    private void verifyHearingLanguageChangedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingLanguageChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingLanguageChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("hearingLanguage"), is(updatedHearingData.getHearingLanguage()));
    }

    private void verifyNonDefaultDaysChangedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerNonDefaultDaysChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerNonDefaultDaysChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(fromString(jsonResponse.get("nonDefaultDays[0].startTime")).toString(),
                is(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).toString()));
    }

    private void verifyNonSittingDaysAssignedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerNonSittingDaysAssigned);
        LOGGER.debug("jsonResponse from privateMessageConsumerNonSittingDaysAssigned: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("nonSittingDays[0]"), is(updatedHearingData.getNonSittingDays().get(0)));
    }

    private void verifyCourtCentreChangedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtCentreChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerCourtCentreChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("courtCentreId"), is(updatedHearingData.getCourtCentreId().toString()));
    }

    private void verifyJudiciaryChangedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
    }

    private void verifyCourtRoomAssignedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtRoomAssigned);
        LOGGER.debug("jsonResponse from privateMessageConsumerCourtRoomAssigned: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("courtRoomId"), is(updatedHearingData.getCourtRoomId().toString()));
    }

    private void verifyHearingAllocatedEvent() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingAllocatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingAllocatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("jurisdictionType"), is(updatedHearingData.getJurisdictionType()));
        assertThat(jsonResponse.get("courtRoomId"), is(updatedHearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get("courtCentreId"), is(updatedHearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get("type.description"), is(updatedHearingData.getHearingTypData().getTypeDescription()));

        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
        assertThat(jsonResponse.get("judiciary[0].isDeputy"), is(updatedHearingData.getJudiciary().get(0).getIsDeputy().get()));
        assertThat(jsonResponse.get("judiciary[0].isBenchChairman"), is(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().get()));

        assertThat(fromString(jsonResponse.get("hearingDays[0].startTime")).toString(),
                is(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).toString()));
        assertThat(jsonResponse.get("estimatedMinutes"), is(hearingData.getHearingEstimateMinutes()));


        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].id"),
                is(hearingData.getListedCases().get(0).getCaseId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].id"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].offenceIds[0]"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));


    }

    private void verifyAllocatedHearingUpdatedForListing() {
        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerAllocatedHearingUpdatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerAllocatedHearingUpdatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("jurisdictionType"), is(updatedHearingData.getJurisdictionType()));
        assertThat(jsonResponse.get("courtRoomId"), is(updatedHearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get("courtCentreId"), is(updatedHearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get("type.description"), is(updatedHearingData.getHearingTypData().getTypeDescription()));

        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
        assertThat(jsonResponse.get("judiciary[0].isDeputy"), is(updatedHearingData.getJudiciary().get(0).getIsDeputy().get()));
        assertThat(jsonResponse.get("judiciary[0].isBenchChairman"), is(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().get()));

        assertThat(jsonResponse.get("estimatedMinutes"), is(hearingData.getHearingEstimateMinutes()));


        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].id"),
                is(hearingData.getListedCases().get(0).getCaseId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].id"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].offenceIds[0]"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));


    }


    public void verifyHearingUpdatedWithNoCourtRoomAndUnallocatedWhenQueryingFromAPI(){

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), UNALLOCATED));


        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                hasNoJsonPath("$.hearings[0].courtRoomId")
                        )));
    }

    public void verifyHearingUpdatedWithNoEndDateAndUnallocatedWhenQueryingFromAPI(){

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), UNALLOCATED));


        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                hasNoJsonPath("$.hearings[0].endDate")
                        )));
    }
    public void verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI(){

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), ALLOCATED));


        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].judiciary[0].judicialId",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialId().toString())),
                                withJsonPath("$.hearings[0].judiciary[0].judicialRoleType.judiciaryType",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())),
                                withJsonPath("$.hearings[0].judiciary[0].isBenchChairman",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().get())),
                                withJsonPath("$.hearings[0].judiciary[0].isDeputy"
                        ))));
    }

    public void verifyHearingUpdatedWhenQueryingFromAPI(){

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), ALLOCATED));


        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].judiciary[0].judicialId",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialId().toString())),
                                withJsonPath("$.hearings[0].judiciary[0].judicialRoleType.judiciaryType",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())),
                                hasNoJsonPath("$.hearings[0].judiciary[0].isBenchChairman"),
                                hasNoJsonPath("$.hearings[0].judiciary[0].isDeputy"),
                                withJsonPath("$.hearings[0].courtRoomId",
                                        equalTo(updatedHearingData.getCourtRoomId().toString())),

                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(updatedHearingData.getEndDate())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate())),
                                withJsonPath("$.hearings[0].nonDefaultDays[0].startTime",
                                        equalTo(fromString(updatedHearingData.getNonDefaultDays().get(0)
                                                .getStartTime()).format(ZONED_DATE_TIME_FORMAT)))
                        )));
    }

    public void verifyHearingFoundByAllocatedFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated"),
                        ALLOCATED));

        verifyHearingFound(searchHearingUrl);
    }


    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDate() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated.court-centre-id.start-date.end-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getHearingStartDate(),
                        hearingData.getHearingEndDate()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndSearchDate() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated.court-centre-id.search-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getHearingStartDate()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated.court-centre-id"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndCourtRoomFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getCourtRoomId()));

        verifyHearingFound(searchHearingUrl);
    }


    public void verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdAndHearingTypFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code.hearing-type"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId(),
                        hearingData.getHearingTypeData().getTypeId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndIdHearingTypAndJurisdictionTypeFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code.hearing-type.jurisdiction-type"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId(),
                        hearingData.getHearingTypeData().getTypeId(),
                        updatedHearingData.getJurisdictionType()));

        verifyHearingFound(searchHearingUrl);
    }

    private void verifyHearingFound(String searchHearingUrl) {
        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter)
                        )));
    }



    public void verifyHearingAllocatedWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), ALLOCATED));


        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(updatedHearingData.getHearingId().toString())),
                                withJsonPath("$.hearings[0].judiciary[0].judicialId",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialId().toString())),
                                withJsonPath("$.hearings[0].judiciary[0].judicialRoleType.judiciaryType",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())),
                                withJsonPath("$.hearings[0].judiciary[0].isBenchChairman",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().get())),
                                withJsonPath("$.hearings[0].judiciary[0].isDeputy",
                                        equalTo(updatedHearingData.getJudiciary().get(0).getIsDeputy().get())),
                                withJsonPath("$.hearings[0].courtRoomId",
                                        equalTo(updatedHearingData.getCourtRoomId().toString())),
                                withJsonPath("$.hearings[0].type.description",
                                        equalTo(updatedHearingData.getHearingTypData().getTypeDescription())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(updatedHearingData.getJurisdictionType())),
                                withJsonPath("$.hearings[0].hearingLanguage",
                                        equalTo(updatedHearingData.getHearingLanguage())),
                                withJsonPath("$.hearings[0].endDate",
                                        equalTo(updatedHearingData.getEndDate())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(updatedHearingData.getStartDate())),
                                withJsonPath("$.hearings[0].hearingDays[0].startTime",
                                        equalTo(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT))),
                                withJsonPath("$.hearings[0].hearingDays[0].durationMinutes",
                                        equalTo(updatedHearingData.getNonDefaultDays().get(0).getDuration().get())),
                                withJsonPath("$.hearings[0].hearingDays[0].endTime",
                                        equalTo(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime())
                                                .plusMinutes(updatedHearingData.getNonDefaultDays().get(0).getDuration().get())
                                                .format(ZONED_DATE_TIME_FORMAT))),
                                withJsonPath("$.hearings[0].nonSittingDays[0]",
                                        equalTo(updatedHearingData.getNonSittingDays().get(0))),
                                withJsonPath("$.hearings[0].nonDefaultDays[0].startTime",
                                        equalTo(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT)))
                        )));
    }
    private void closeMessageConsumers() throws JMSException {
        privateMessageConsumerHearingAllocatedForListing.close();
        privateMessageConsumerTypeChanged.close();
        privateMessageConsumerJurisdictionChanged.close();
        privateMessageConsumerHearingLanguageChanged.close();
        privateMessageConsumerStartDateChanged.close();
        privateMessageConsumerNonDefaultDaysAssigned.close();
        privateMessageConsumerNonDefaultDaysChanged.close();
        privateMessageConsumerNonSittingDaysAssigned.close();
        privateMessageConsumerNonSittingDaysChanged.close();
        privateMessageConsumerCourtCentreChanged.close();
        privateMessageConsumerJudiciaryAssigned.close();
        privateMessageConsumerJudiciaryChanged.close();
        privateMessageConsumerCourtRoomAssigned.close();
        privateMessageConsumerCourtRoomRemoved.close();
        privateMessageConsumerCourtRoomChanged.close();
        privateMessageConsumerEndDateAssigned.close();
        privateMessageConsumerEndDateChanged.close();
        privateMessageConsumerEndDateRemoved.close();
        privateMessageConsumerHearingUnallocatedForListing.close();
        privateMessageConsumerHearingDaysChanged.close();
        publicMessageConsumerHearingConfirmed.close();
        publicMessageConsumerHearingUpdated.close();
    }

}
