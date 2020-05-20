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
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;

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

    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_JUDICIAL_ROLE_TYPE_ID = "judicialRoleTypeId";
    public static final String FIELD_JUDICIARY_TYPE = "judiciaryType";
    public static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    public static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING = "application/vnd.listing.command.update-hearing-for-listing+json";
    public static final String FIELD_DURATION = "duration";
    public static final String FIELD_COURT_SCHEDULE_ID = "courtScheduleId";
    public static final String FIELD_OUCODE = "oucode";
    public static final String FIELD_SESSION = "session";
    public static final String FIELD_JUDICIARY = "judiciary";
    public static final String FIELD_JUDICIAL_ID = "judicialId";
    public static final String FIELD_JUDICIAL_ROLE_TYPE = "judicialRoleType";
    public static final String FIELD_IS_BENCH_CHAIRMAN = "isBenchChairman";
    public static final String FIELD_IS_DEPUTY = "isDeputy";
    public static final String FIELD_COURT_ROOM_ID = "courtRoomId";
    public static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_NON_SITTING_DAYS = "nonSittingDays";
    public static final String FIELD_NON_DEFAULT_DAYS = "nonDefaultDays";
    public static final String FIELD_HEARING_LANGUAGE = "hearingLanguage";
    public static final String FIELD_JURISDICTION_TYPE = "jurisdictionType";
    public static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";
    public static final String FIELD_HEARING_TYPE_ID = "id";
    public static final String FIELD_HEARING_TYPE_DESCRIPTION = "description";
    public static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
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
    private static final String EVENT_SELECTOR_WEEK_COMMENCING_DATES_REMOVED = "listing.events.week-commencing-date-removed-for-hearing";
    private static final String FIELD_HEARINGS = "hearings";
    private static final int DEFAULT_DURATION_MINS = (6 * 60) + 30;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final String LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS = "listing.command.change-judiciary-for-hearings";
    private static final String MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS = "application/vnd.listing.command.change-judiciary-for-hearings+json";
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateHearingSteps.class);
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
    private MessageConsumer privateMessageConsumerWeekCommencingDatesRemoved;

    private String request;

    public UpdateHearingSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData) {
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

    private static String getStringOrNull(final UUID id) {
        return id != null ? id.toString() : null;
    }

    private static JsonArray prepareJsonStringArray(final List<String> strings) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        if (strings != null && !strings.isEmpty()) {
            strings.forEach(builder::add);
        }
        return builder.build();

    }

    private static JsonArray prepareJsonHearingIdArray(final UUID hearingId) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add(hearingId.toString());
        return builder.build();

    }

    private static JsonArrayBuilder prepareJsonJudiciary(final List<JudicialRoleData> roleData) {
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

    private static JsonObjectBuilder prepareJudicialRoleType(final JudicialRoleTypeData judicialRoleType) {
        if (judicialRoleType != null) {
            final JsonObjectBuilder builder = createObjectBuilder()
                    .add(FIELD_JUDICIARY_TYPE, judicialRoleType.getJudiciaryType());
            addNullableUUIDField(builder, FIELD_JUDICIAL_ROLE_TYPE_ID, judicialRoleType.getJudicialRoleTypeId());
            return builder;
        }
        return null;
    }

    private static JsonObjectBuilder prepareJsonHearingType(final HearingTypeData hearingType) {
        if (hearingType != null) {
            return createObjectBuilder()
                    .add(FIELD_HEARING_TYPE_ID, hearingType.getTypeId().toString())
                    .add(FIELD_HEARING_TYPE_DESCRIPTION, hearingType.getTypeDescription());
        }
        return null;
    }

    private static JsonArrayBuilder prepareJsonNonDefaultDays(final List<NonDefaultDayData> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> {
                    JsonObjectBuilder nonDefaultDayBuilder = createObjectBuilder().add(FIELD_START_TIME, ndd.getStartTime());
                    addNullableIntegerField(nonDefaultDayBuilder, FIELD_DURATION, ndd.getDuration());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_SCHEDULE_ID, ndd.getCourtScheduleId());
                    addNullableIntegerFieldIfNotNull(nonDefaultDayBuilder, FIELD_COURT_ROOM_ID, ndd.getCourtRoomId());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_OUCODE, ndd.getOucode());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_SESSION, ndd.getSession());

                    return nonDefaultDayBuilder;
                })
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static void addNullableStringField(final JsonObjectBuilder builder, final String fieldName, final String value) {
        if (value != null) {
            builder.add(fieldName, value);
        } else {
            builder.addNull(fieldName);
        }
    }

    private static void addNullableStringField(JsonObjectBuilder builder, String fieldName, Optional<String> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        }
    }

    private static void addNullableIntegerFieldIfNotNull(JsonObjectBuilder builder, String fieldName, Optional<Integer> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        }
    }

    private static void addNullableIntegerField(final JsonObjectBuilder builder, final String fieldName, final Optional<Integer> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        } else {
            builder.addNull(fieldName);
        }
    }

    private static void addNullableUUIDField(final JsonObjectBuilder builder, final String fieldName, final Optional<UUID> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get().toString());
        }
    }

    private static void addOptionalBooleanField(final JsonObjectBuilder builder, final String fieldName, final Optional<Boolean> value) {
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
        privateMessageConsumerWeekCommencingDatesRemoved = privateEvents.createConsumer(EVENT_SELECTOR_WEEK_COMMENCING_DATES_REMOVED);
    }

    public void whenHearingIsUpdatedForListing() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingData(updatedHearingData);

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenJudiciaryIsChangedForHearings() {

        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS), updatedHearingData.getHearingId()));

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

        } catch (final JMSException e) {
            LOGGER.error("Error closing privateMessageConsumerHearingListed: {}", e.getMessage());
        }
    }

    public void verifyHearingUpdatedResultsInAllocationInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
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
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());

        verifyFirstHearingDay(jsonResponse);

        //test usage of reference data courtcentre default startTime and duration
        final ZonedDateTime lastHearingDayStartDateTime = ZonedDateTime.of(LocalDate.parse(updatedHearingData.getEndDate()), DEFAULT_START_TIME, BST)
                .withZoneSameInstant(UTC);
        assertThat(jsonResponse.get("hearingDays[1].startTime"), is(lastHearingDayStartDateTime.format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("hearingDays[1].durationMinutes"), is(DEFAULT_DURATION_MINS));
        assertThat(jsonResponse.get("hearingDays[1].endTime"),
                is(lastHearingDayStartDateTime.plusMinutes(DEFAULT_DURATION_MINS).format(ZONED_DATE_TIME_FORMAT)));

    }

    private void verifyHearingDaysChangedEventForOneDayOnly() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());

        verifyFirstHearingDay(jsonResponse);
    }

    private void verifyFirstHearingDay(final JsonPath jsonResponse) {
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        final String startTime = updatedHearingData.getNonDefaultDays().get(0).getStartTime();
        assertThat(jsonResponse.get("hearingDays[0].startTime"),
                is(fromString(startTime).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("hearingDays[0].sequence"),
                is(0));
        final Integer duration = updatedHearingData.getNonDefaultDays().get(0).getDuration().get();
        assertThat(jsonResponse.get("hearingDays[0].durationMinutes"),
                is(duration));
        assertThat(jsonResponse.get("hearingDays[0].endTime"),
                is(fromString(startTime).plusMinutes(duration).format(ZONED_DATE_TIME_FORMAT)));
    }

    public void verifyEmptyHearingDaysChangedEventInActiveMQ() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

        assertThat(jsonResponse.get("hearingDays"), hasSize(0));
    }

    public void verifyHearingUpdatedResultsInMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyStartDateChangedEvent();
        verifyCourtRoomChangedEvent();
        verifyEndDateChangedEvent();
        verifyJudiciaryChangedEvent();
        verifyHearingDaysChangedEventForOneDayOnly();
        verifyAllocatedHearingUpdatedForListing();

    }

    public void verifyHearingUpdatedResultsForSlotUpdateInMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyCourtRoomChangedEvent();
        verifyJudiciaryChangedEvent();
        verifyHearingDaysChangedEventForOneDayOnly();
        verifyAllocatedHearingUpdatedForListing();
    }

    public void verifyHearingConfirmedInPublicMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyHearingConfirmedEvent();
    }

    public void verifyHearingConfirmedInPublicMQHasNoJudiciary() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyHearingConfirmedHasNoJudiciary();
    }

    private void verifyHearingConfirmedHasNoJudiciary() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmed);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingConfirmed: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("confirmedHearing.id"), is(updatedHearingData.getHearingId().toString()));
        assertNull(jsonResponse.get("confirmedHearing.judiciary"));
    }

    public void verifyHearingUpdatedInPublicMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyHearingUpdatedEvent();
    }

    private void verifyHearingConfirmedEvent() {

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingConfirmed);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingConfirmed: {}", jsonResponse.prettify());

        verifyHearingPublicDetails(jsonResponse, "confirmedHearing");
    }

    private void verifyHearingUpdatedEvent() {

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingUpdated: {}", jsonResponse.prettify());

        verifyHearingPublicDetails(jsonResponse, "updatedHearing");
    }

    private void verifyHearingPublicDetails(JsonPath jsonResponse, String publicEventType) {
        assertThat(jsonResponse.get(publicEventType + ".id"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".courtCentre.roomId"), is(updatedHearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".courtCentre.id"), is(updatedHearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".courtCentre.name"), is("Liverpool Crown Court"));
        assertThat(jsonResponse.get(publicEventType + ".courtApplicationIds[0]"), is(hearingData.getCourtApplications().get(0).getId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".hearingLanguage"), is(updatedHearingData.getHearingLanguage()));
        assertThat(jsonResponse.get(publicEventType + ".type.id"), is(updatedHearingData.getHearingTypData().getTypeId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".type.description"), is(updatedHearingData.getHearingTypData().getTypeDescription()));
        assertThat(jsonResponse.get(publicEventType + ".judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
        assertThat(jsonResponse.get(publicEventType + ".prosecutionCases[0].id"), is(hearingData.getListedCases().get(0).getCaseId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".prosecutionCases[0].defendants[0].id"), is(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".prosecutionCases[0].defendants[0].offences[0].id"), is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".hearingDays[0].sittingDay"),
                is(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get(publicEventType + ".hearingDays[0].listedDurationMinutes"), is(updatedHearingData.getNonDefaultDays().get(0).getDuration().get()));
        assertThat(jsonResponse.get(publicEventType + ".reportingRestrictionReason"), is(hearingData.getReportingRestrictionReason()));
    }

    public void verifyHearingUpdatedWithNoCourtRoomResultsInUnallocationInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyCourtRoomRemovedEvent();
        verifyUnallocatedHearingEvent();
    }

    public void verifyHearingUpdatedWithNoEndDateResultsInUnallocationInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyEndDateRemovedEvent();
        verifyUnallocatedHearingEvent();
        verifyEmptyHearingDaysChangedEventInActiveMQ();
    }

    public void verifyHearingWithUpdatedJudiciaryInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyJudiciaryChangedForHearing();
        verifyAllocatedHearingUpdatedForListing();
    }

    private void verifyUnallocatedHearingEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingUnallocatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingUnallocatedForListing: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    private void verifyEndDateRemovedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerEndDateRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

    }

    private void verifyJudiciaryChangedForHearing() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

    }

    private void verifyCourtRoomRemovedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtRoomRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerCourtRoomRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    private void verifyEndDateChangedEvent() {
        final JsonPath jsonResponse;
        jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerEndDateChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("endDate"), is(updatedHearingData.getEndDate()));

    }

    private void verifyCourtRoomChangedEvent() {
        final JsonPath jsonResponse;
        jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtRoomChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerCourtRoomChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("courtRoomId"), is(updatedHearingData.getCourtRoomId().toString()));

    }

    private void verifyJudiciaryAssignedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryAssigned);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryAssigned: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
    }

    private void verifyStartDateChangedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerStartDateChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerStartDateChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("startDate"), is(updatedHearingData.getStartDate()));
    }

    private void verifyTypeChangedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerTypeChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerTypeChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("type.description"), is(updatedHearingData.getHearingTypData().getTypeDescription()));
    }

    private void verifyHearingLanguageChangedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingLanguageChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingLanguageChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("hearingLanguage"), is(updatedHearingData.getHearingLanguage()));
    }

    private void verifyNonDefaultDaysChangedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerNonDefaultDaysChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerNonDefaultDaysChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(fromString(jsonResponse.get("nonDefaultDays[0].startTime")).toString(),
                is(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).toString()));
    }

    private void verifyNonSittingDaysAssignedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerNonSittingDaysAssigned);
        LOGGER.debug("jsonResponse from privateMessageConsumerNonSittingDaysAssigned: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("nonSittingDays[0]"), is(updatedHearingData.getNonSittingDays().get(0)));
    }

    private void verifyCourtCentreChangedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtCentreChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerCourtCentreChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("courtCentreId"), is(updatedHearingData.getCourtCentreId().toString()));
    }

    private void verifyJudiciaryChangedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
    }

    private void verifyCourtRoomAssignedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtRoomAssigned);
        LOGGER.debug("jsonResponse from privateMessageConsumerCourtRoomAssigned: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("courtRoomId"), is(updatedHearingData.getCourtRoomId().toString()));
    }

    private void verifyHearingAllocatedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingAllocatedForListing);
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
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerAllocatedHearingUpdatedForListing);
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

    public void verifyHearingUpdatedWithNoCourtRoomAndUnallocatedWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), UNALLOCATED));

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

    public void verifyHearingUpdatedWithNoEndDateAndUnallocatedWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), UNALLOCATED));

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

    public void verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), ALLOCATED));

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

    public void verifyHearingUpdatedWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), ALLOCATED));

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

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated"),
                        ALLOCATED));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDate() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.start-date.end-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getHearingStartDate(),
                        hearingData.getHearingEndDate()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndSearchDate() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.search-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getHearingStartDate()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndCourtRoomFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getCourtRoomId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdAndHearingTypFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code.hearing-type"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId(),
                        hearingData.getHearingTypeData().getTypeId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndIdHearingTypAndJurisdictionTypeFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code.hearing-type.jurisdiction-type"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId(),
                        hearingData.getHearingTypeData().getTypeId(),
                        updatedHearingData.getJurisdictionType()));

        verifyHearingFound(searchHearingUrl);
    }

    private void verifyHearingFound(final String searchHearingUrl) {
        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter)
                        )));
    }

    public void verifyHearingUpdatedWhenWeekCommencingDateRemovedResultsInMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyStartDateChangedEvent();
        verifyEndDateChangedEvent();
        verifyWeekCommencingDateRemovedEvent();
    }

    private void verifyWeekCommencingDateRemovedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerWeekCommencingDatesRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerWeekCommencingDateRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }


    public void verifyHearingAllocatedWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), ALLOCATED));

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
        privateMessageConsumerWeekCommencingDatesRemoved.close();
    }

}
