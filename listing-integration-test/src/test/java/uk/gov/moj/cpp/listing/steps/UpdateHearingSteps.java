package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonObject;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.getHearingFilter;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollUntilHearingIsPresent;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreHmiListingEnabled;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreHmiListingEnabledWithoutCourtRoomSelection;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtWithHmiListingEnabledCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingTypeData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleTypeData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.Filter;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateHearingSteps extends AbstractIT {


    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_JUDICIAL_ROLE_TYPE_ID = "judicialRoleTypeId";
    public static final String FIELD_JUDICIARY_TYPE = "judiciaryType";
    public static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    public static final String PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI = "public.staginghmi.hearing-updated-from-hmi";
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
    private static final String FIELD_ROOM_ID = "roomId";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_NON_SITTING_DAYS = "nonSittingDays";
    public static final String FIELD_NON_DEFAULT_DAYS = "nonDefaultDays";
    public static final String FIELD_SEND_NOTIFICATION_TO_PARTIES = "sendNotificationToParties";
    public static final String FIELD_HEARING_LANGUAGE = "hearingLanguage";
    public static final String FIELD_JURISDICTION_TYPE = "jurisdictionType";
    private static final String FIELD_PROSECUTION_CASES = "prosecutionCases";
    private static final String FIELD_HAS_VIDEO_LINK = "hasVideoLink";
    private static final String FIELD_PUBLIC_LIST_NOTE = "publicListNote";
    private static final String FIELD_USER_ID = "userId";
    public static final String PANEL = "panel";

    public static final String FIELD_HEARING_TYPE_ID = "id";
    public static final String FIELD_HEARING_TYPE_DESCRIPTION = "description";
    public static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    protected static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing-v2";
    private static final String EVENT_SELECTOR_HEARING_UNALLOCATED_FOR_LISTING = "listing.events.hearing-unallocated-for-listing";
    private static final String EVENT_SELECTOR_HEARING_START_DATE_CHANGED = "listing.events.start-date-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_REQUESTED_FOR_LISTING = "listing.events.hearing-requested-for-listing";
    private static final String EVENT_SELECTOR_COURT_ROOM_REMOVED = "listing.events.court-room-removed-from-hearing";
    private static final String EVENT_SELECTOR_END_DATE_CHANGED = "listing.events.end-date-changed-for-hearing";
    private static final String EVENT_SELECTOR_END_DATE_REMOVED = "listing.events.end-date-removed-from-hearing";
    private static final String EVENT_SELECTOR_HEARING_DAYS_CHANGED = "listing.events.hearing-days-changed-for-hearing";
    private static final String EVENT_SELECTED_PUBLIC_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String EVENT_SELECTED_PUBLIC_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String EVENT_SELECTED_PUBLIC_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";
    private static final String EVENT_SELECTED_PUBLIC_HEARING_REQUESTED_FOR_LISTING = "public.listing.hearing-requested-for-listing";
    private static final String EVENT_SELECTOR_WEEK_COMMENCING_DATES_REMOVED = "listing.events.week-commencing-date-removed-for-hearing";
    private static final String EVENT_SELECTOR_PUBLIC_LIST_NOTE_REMOVED = "listing.events.public-list-note-removed-from-hearing";
    private static final String EVENT_SELECTOR_START_DATE_REMOVED = "listing.events.start-date-removed-for-hearing";
    public static final String EVENT_SELECTOR_LISTING_EVENTS_WEEK_COMMENCING_DATE_CHANGED_FOR_HEARING = "listing.events.week-commencing-date-changed-for-hearing";
    private static final String FIELD_HEARINGS = "hearings";
    private static final String LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS = "listing.command.change-judiciary-for-hearings";
    private static final String MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS = "application/vnd.listing.command.change-judiciary-for-hearings+json";
    protected static final Logger LOGGER = LoggerFactory.getLogger(UpdateHearingSteps.class);
    public static final String PUBLIC_LISTING_HEARING_CHANGES_SAVED = "public.listing.hearing-changes-saved";
    private static final String PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING = "public.listing.hearing-days-changed-for-hearing";
    public static final String FIELD_WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    public static final String FIELD_WEEK_COMMENCING_DURATION_IN_WEEKS = "weekCommencingDurationInWeeks";
    public static final String FIELD_WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    protected UpdatedHearingData updatedHearingData;
    protected HearingData hearingData;
    private List<ListedCaseData> listedCaseDatas;
    private JmsMessageConsumerClient privateMessageConsumerAllocatedHearingUpdatedForListing;
    private JmsMessageConsumerClient privateMessageConsumerHearingUnallocatedForListing;
    private JmsMessageConsumerClient privateMessageConsumerStartDateChanged;
    private JmsMessageConsumerClient privateMessageConsumerCourtRoomRemoved;
    private JmsMessageConsumerClient privateMessageConsumerEndDateChanged;
    private JmsMessageConsumerClient privateMessageConsumerEndDateRemoved;
    private JmsMessageConsumerClient privateMessageConsumerHearingDaysChanged;
    private JmsMessageConsumerClient publicMessageConsumerHearingConfirmed;
    private JmsMessageConsumerClient publicMessageConsumerHearingUpdated;
    private JmsMessageConsumerClient publicMessageConsumerVacatedTrialUpdated;
    private JmsMessageConsumerClient privateMessageConsumerWeekCommencingDatesRemoved;
    private JmsMessageConsumerClient privateMessageConsumerPublicListNoteRemoved;
    private JmsMessageConsumerClient publicMessageConsumerHearingChangesSaved;
    private JmsMessageConsumerClient publicEventHearingDaysChangedForHearing;
    private JmsMessageProducerClient publicEventMessageProducer;
    private JmsMessageConsumerClient privateMessageConsumerStartDateRemoved;
    private JmsMessageConsumerClient privateMessageConsumerWeekCommencingDateChanged;
    private JmsMessageConsumerClient privateMessageConsumerHearingRequestedForListing;
    private JmsMessageConsumerClient publicMessageConsumerHearingRequested;

    private String request;

    public UpdateHearingSteps() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        createMessageConsumers();
    }

    public UpdateHearingSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData) {
        this.hearingData = hearingsData.getHearingData().get(0);
        this.listedCaseDatas = hearingsData.getHearingData().get(0).getListedCases();
        this.updatedHearingData = updatedHearingData;
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        createMessageConsumers();

    }

    public UpdateHearingSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData, final Boolean split) {
        this.hearingData = hearingsData.getHearingData().get(0);
        this.listedCaseDatas = hearingsData.getHearingData().get(0).getListedCases();
        this.updatedHearingData = updatedHearingData;
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        createMessageConsumersForDefendantSplit();

    }

    public void verifyHearingListedFromAPI(final boolean isAllocated, final Matcher[] matchers, final String weekCommencingStartDate, final String weekCommencingEndDate) {

        StringBuffer searchHearingUrl = new StringBuffer();
        searchHearingUrl.append(String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), isAllocated)));

        Optional.ofNullable(weekCommencingStartDate).ifPresent(value -> searchHearingUrl.append("&weekCommencingStartDate=" + weekCommencingStartDate));
        Optional.ofNullable(weekCommencingEndDate).ifPresent(value -> searchHearingUrl.append("&weekCommencingEndDate=" + weekCommencingEndDate));

        pollForHearing(searchHearingUrl.toString(), getLoggedInUser().toString(), matchers);
    }

    private static JsonObjectBuilder prepareJsonForUpdatedHearingData(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, prepareJsonHearingType(updatedHearingData.getHearingTypData()))
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_JURISDICTION_TYPE, updatedHearingData.getJurisdictionType())
                .add(FIELD_HEARING_LANGUAGE, updatedHearingData.getHearingLanguage())
                .add(FIELD_COURT_CENTRE_ID, updatedHearingData.getCourtCentreId().toString())
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()))
                .add(FIELD_NON_DEFAULT_DAYS, prepareJsonNonDefaultDays(updatedHearingData.getNonDefaultDays()))
                .add(FIELD_SEND_NOTIFICATION_TO_PARTIES, updatedHearingData.isSendNotificationToParties())
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()));
        if (nonNull(updatedHearingData.getPublicListNote())) {
            builder.add(FIELD_PUBLIC_LIST_NOTE, updatedHearingData.getPublicListNote());
        }

        addNullableStringField(builder, FIELD_END_DATE, updatedHearingData.getEndDate());
        addNullableStringField(builder, FIELD_COURT_ROOM_ID, getStringOrNull(updatedHearingData.getCourtRoomId()));
        addNullableStringField(builder, PANEL, updatedHearingData.getPanel());
        addNullableStringField(builder, "bookingType", Optional.ofNullable(updatedHearingData.getBookingType()));
        addNullableStringField(builder, "priority", Optional.ofNullable(updatedHearingData.getPriority()));
        if (isNotEmpty(updatedHearingData.getSpecialRequirements())) {
            builder.add("specialRequirements", updatedHearingData.getSpecialRequirements().stream()
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add).build());
        }

        return builder;
    }

    private static JsonObjectBuilder prepareJsonForUpdatedHearingDataForPublicEvent(final UpdatedHearingData updatedHearingData, final List<String> nullFields) {
        final JsonObjectBuilder builder = createObjectBuilder();
        Optional.ofNullable(updatedHearingData.getStartDate()).filter(value -> !nullFields.contains(FIELD_START_DATE)).ifPresent(startDate -> builder.add(FIELD_START_DATE, startDate));
        Optional.ofNullable(updatedHearingData.getEndDate()).filter(value -> !nullFields.contains(FIELD_END_DATE)).ifPresent(endDate -> builder.add(FIELD_END_DATE, endDate));
        Optional.ofNullable(updatedHearingData.getCourtCentreId()).filter(value -> !nullFields.contains(FIELD_COURT_CENTRE_ID)).ifPresent(centreId -> builder.add(FIELD_COURT_CENTRE_ID, centreId.toString()));
        Optional.ofNullable(updatedHearingData.getCourtRoomId()).filter(value -> !nullFields.contains(FIELD_COURT_ROOM_ID)).ifPresent(roomId -> builder.add(FIELD_COURT_ROOM_ID, roomId.toString()));
        Optional.ofNullable(updatedHearingData.getWeekCommencingStartDate()).filter(value -> !nullFields.contains(FIELD_WEEK_COMMENCING_START_DATE)).ifPresent(comStartDate -> builder.add(FIELD_WEEK_COMMENCING_START_DATE, comStartDate));
        Optional.ofNullable(updatedHearingData.getWeekCommencingDurationInWeeks()).filter(value -> !nullFields.contains(FIELD_WEEK_COMMENCING_DURATION_IN_WEEKS)).ifPresent(durationInWeeks -> builder.add(FIELD_WEEK_COMMENCING_DURATION_IN_WEEKS, durationInWeeks));
        Optional.ofNullable(updatedHearingData.getWeekCommencingEndDate()).filter(value -> !nullFields.contains(FIELD_WEEK_COMMENCING_END_DATE)).ifPresent(weekCommencingEndDate -> builder.add(FIELD_WEEK_COMMENCING_END_DATE, weekCommencingEndDate));
        Optional.ofNullable(updatedHearingData.getNonDefaultDays()).filter(value -> !nullFields.contains("nonDefaultDays")).ifPresent(nonDefaultDayData -> {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            nonDefaultDayData.forEach(data -> {
                arrayBuilder.add(createObjectBuilder().add("startTime", data.getStartTime()).add("duration", data.getDuration().get()));
            });
            builder.add("nonDefaultDays", arrayBuilder.build());
        });
        return builder;
    }

    private static JsonObjectBuilder prepareJsonForUpdatedHearingDataWithoutCourtRoomSelection(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, prepareJsonHearingType(updatedHearingData.getHearingTypData()))
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_JURISDICTION_TYPE, updatedHearingData.getJurisdictionType())
                .add(FIELD_HEARING_LANGUAGE, updatedHearingData.getHearingLanguage())
                .add(FIELD_COURT_CENTRE_ID, updatedHearingData.getCourtCentreId().toString())
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()))
                .add(FIELD_NON_DEFAULT_DAYS, prepareJsonNonDefaultDaysWithoutCourtRoomSelection(updatedHearingData.getNonDefaultDays()))
                .add(FIELD_SEND_NOTIFICATION_TO_PARTIES, updatedHearingData.isSendNotificationToParties())
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()));
        if (nonNull(updatedHearingData.getPublicListNote())) {
            builder.add(FIELD_PUBLIC_LIST_NOTE, updatedHearingData.getPublicListNote());
        }

        addNullableStringField(builder, FIELD_END_DATE, updatedHearingData.getEndDate());
        addNullableStringField(builder, FIELD_COURT_ROOM_ID, getStringOrNull(updatedHearingData.getCourtRoomId()));
        addNullableStringField(builder, PANEL, updatedHearingData.getPanel());
        addNullableStringField(builder, "bookingType", Optional.ofNullable(updatedHearingData.getBookingType()));
        addNullableStringField(builder, "priority", Optional.ofNullable(updatedHearingData.getPriority()));
        if (isNotEmpty(updatedHearingData.getSpecialRequirements())) {
            builder.add("specialRequirements", updatedHearingData.getSpecialRequirements().stream()
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add).build());
        }

        return builder;
    }

    private static String prepareJsonForUpdatedHearingDataWithVideoLinkDetails(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, prepareJsonHearingType(updatedHearingData.getHearingTypData()))
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_JURISDICTION_TYPE, updatedHearingData.getJurisdictionType())
                .add(FIELD_HEARING_LANGUAGE, updatedHearingData.getHearingLanguage())
                .add(FIELD_COURT_CENTRE_ID, updatedHearingData.getCourtCentreId().toString())
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()))
                .add(FIELD_NON_DEFAULT_DAYS, prepareJsonNonDefaultDays(updatedHearingData.getNonDefaultDays()))
                .add(FIELD_SEND_NOTIFICATION_TO_PARTIES, updatedHearingData.isSendNotificationToParties())
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()));

        if (nonNull(updatedHearingData.getHasVideoLink())) {
            builder.add(FIELD_HAS_VIDEO_LINK, updatedHearingData.getHasVideoLink());
        }

        if (nonNull(updatedHearingData.getHasVideoLink())) {
            builder.add(FIELD_HAS_VIDEO_LINK, updatedHearingData.getHasVideoLink());
        }


        if (nonNull(updatedHearingData.getPublicListNote())) {
            builder.add(FIELD_PUBLIC_LIST_NOTE, updatedHearingData.getPublicListNote());
        }

        addNullableStringField(builder, FIELD_END_DATE, updatedHearingData.getEndDate());
        addNullableStringField(builder, FIELD_COURT_ROOM_ID, getStringOrNull(updatedHearingData.getCourtRoomId()));

        return builder.build().toString();
    }

    private static String prepareJsonForUpdatedHearingDataWithProsecutionCases(final UpdatedHearingData updatedHearingData, final List<ListedCaseData> listedCaseDataList) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, prepareJsonHearingType(updatedHearingData.getHearingTypData()))
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_JURISDICTION_TYPE, updatedHearingData.getJurisdictionType())
                .add(FIELD_HEARING_LANGUAGE, updatedHearingData.getHearingLanguage())
                .add(FIELD_COURT_CENTRE_ID, updatedHearingData.getCourtCentreId().toString())
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()))
                .add(FIELD_NON_DEFAULT_DAYS, prepareJsonNonDefaultDays(updatedHearingData.getNonDefaultDays()))
                .add(FIELD_SEND_NOTIFICATION_TO_PARTIES, updatedHearingData.isSendNotificationToParties())
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()))
                .add(FIELD_PROSECUTION_CASES, prepareJsonProsecutionCases(listedCaseDataList));

        addNullableStringField(builder, FIELD_END_DATE, updatedHearingData.getEndDate());
        addNullableStringField(builder, FIELD_COURT_ROOM_ID, getStringOrNull(updatedHearingData.getCourtRoomId()));

        return builder.build().toString();
    }

    private static String prepareJsonForUpdatedHearingDataWithProsecutionCasesDefs(final UpdatedHearingData updatedHearingData, final List<ListedCaseData> listedCaseDataList) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, prepareJsonHearingType(updatedHearingData.getHearingTypData()))
                .add(FIELD_START_DATE, updatedHearingData.getStartDate())
                .add(FIELD_END_DATE, updatedHearingData.getEndDate())
                .add(FIELD_JURISDICTION_TYPE, updatedHearingData.getJurisdictionType())
                .add(FIELD_HEARING_LANGUAGE, updatedHearingData.getHearingLanguage())
                .add(FIELD_COURT_CENTRE_ID, updatedHearingData.getCourtCentreId().toString())
                .add(FIELD_COURT_ROOM_ID, updatedHearingData.getCourtRoomId().toString())
                .add("panel", "ADULT")
                .add("publicListNote", "")
                .add("hasVideoLink", false)
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()))
                .add(FIELD_NON_DEFAULT_DAYS, prepareJsonNonDefaultDaysForSplit(updatedHearingData.getNonDefaultDays()))
                .add(FIELD_SEND_NOTIFICATION_TO_PARTIES, updatedHearingData.isSendNotificationToParties())
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()))
                .add(FIELD_PROSECUTION_CASES, prepareJsonProsecutionCasesForSplitDefendant(listedCaseDataList))
                .add("splitHearing", updatedHearingData.getSplitHearing());

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

    private static JsonArray prepareJsonProsecutionCases(final List<ListedCaseData> listedCaseDataList) {

        return listedCaseDataList.stream()
                .map(p ->
                        {
                            final JsonObjectBuilder caseBuilder = createObjectBuilder();
                            caseBuilder.add("caseId", p.getCaseId().toString());

                            final JsonArrayBuilder defendantArrayBuilder = p.getDefendants().stream()
                                    .map(d -> {
                                        final JsonObjectBuilder defendantBuilder = createObjectBuilder();
                                        defendantBuilder.add("defendantId", d.getDefendantId().toString());

                                        final JsonArrayBuilder offenceArrayBuilder = d.getOffences().stream()
                                                .map(o -> {
                                                    final JsonObjectBuilder offenceBuilder = createObjectBuilder();
                                                    offenceBuilder.add("offenceId", o.getOffenceId().toString());
                                                    return offenceBuilder;
                                                }).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

                                        defendantBuilder.add("offences", offenceArrayBuilder);
                                        return defendantBuilder;

                                    }).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

                            caseBuilder.add("defendants", defendantArrayBuilder);
                            return caseBuilder;
                        }
                ).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add).build();
    }

    private static JsonArray prepareJsonProsecutionCasesForSplitDefendant(final List<ListedCaseData> listedCaseDataList) {

        return listedCaseDataList.stream()
                .map(p ->
                        {
                            final JsonObjectBuilder caseBuilder = createObjectBuilder();
                            caseBuilder.add("caseId", p.getCaseId().toString());
                            final JsonObjectBuilder defendantBuilder = createObjectBuilder();
                            final JsonArrayBuilder arrayBuilder = createArrayBuilder();
                            caseBuilder.add("defendants", arrayBuilder.add(defendantBuilder.add("defendantId", p.getDefendants().get(0).getDefendantId().toString())
                                    .add("offences", p.getDefendants().get(0).getOffences().stream()
                                            .map(o -> {
                                                final JsonObjectBuilder offenceBuilder = createObjectBuilder();
                                                offenceBuilder.add("offenceId", o.getOffenceId().toString());
                                                return offenceBuilder;
                                            }).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)).build()));
                            return caseBuilder;
                        }
                ).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add).build();
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
                        final JsonObjectBuilder builder = createObjectBuilder()
                                .add(FIELD_JUDICIAL_ID, ndd.getJudicialId().toString())
                                .add(FIELD_JUDICIAL_ROLE_TYPE, prepareJudicialRoleType(ndd.getJudicialRoleType()))
                                .add(FIELD_USER_ID, ndd.getUserId().toString());

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
                    .add(FIELD_HEARING_TYPE_DESCRIPTION, hearingType.getTypeDescription())
                    .add("welshDescription", hearingType.getWelshDescription());
        }
        return null;
    }

    private static JsonArrayBuilder prepareJsonNonDefaultDays(final List<NonDefaultDayData> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> {
                    final JsonObjectBuilder nonDefaultDayBuilder = createObjectBuilder().add(FIELD_START_TIME, ndd.getStartTime());
                    addNullableIntegerField(nonDefaultDayBuilder, FIELD_DURATION, ndd.getDuration());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_SCHEDULE_ID, ndd.getCourtScheduleId());
                    addNullableIntegerFieldIfNotNull(nonDefaultDayBuilder, FIELD_COURT_ROOM_ID, ndd.getCourtRoomId());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_OUCODE, ndd.getOucode());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_SESSION, ndd.getSession());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_CENTRE_ID, ndd.getCourtCentreId());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_ROOM_ID, ndd.getRoomId());

                    return nonDefaultDayBuilder;
                })
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static JsonArrayBuilder prepareJsonNonDefaultDaysForSplit(final List<NonDefaultDayData> nonDefaultDays) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final JsonObjectBuilder nonDefaultDayBuilder = createObjectBuilder().add(FIELD_START_TIME, nonDefaultDays.get(0).getStartTime());
        addNullableIntegerField(nonDefaultDayBuilder, FIELD_DURATION, nonDefaultDays.get(0).getDuration());
        addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_SCHEDULE_ID, nonDefaultDays.get(0).getCourtScheduleId());
        addNullableIntegerFieldIfNotNull(nonDefaultDayBuilder, FIELD_COURT_ROOM_ID, nonDefaultDays.get(0).getCourtRoomId());
        addNullableStringField(nonDefaultDayBuilder, FIELD_OUCODE, nonDefaultDays.get(0).getOucode());
        addNullableStringField(nonDefaultDayBuilder, FIELD_SESSION, nonDefaultDays.get(0).getSession());
        addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_CENTRE_ID, nonDefaultDays.get(0).getCourtCentreId());
        addNullableStringField(nonDefaultDayBuilder, FIELD_ROOM_ID, nonDefaultDays.get(0).getRoomId());
        return arrayBuilder.add(nonDefaultDayBuilder);
    }

    private static JsonArrayBuilder prepareJsonNonDefaultDaysWithoutCourtRoomSelection(final List<NonDefaultDayData> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> {
                    final JsonObjectBuilder nonDefaultDayBuilder = createObjectBuilder().add(FIELD_START_TIME, ndd.getStartTime());
                    addNullableIntegerField(nonDefaultDayBuilder, FIELD_DURATION, ndd.getDuration());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_SCHEDULE_ID, ndd.getCourtScheduleId());
                    addNullableIntegerFieldIfNotNull(nonDefaultDayBuilder, FIELD_COURT_ROOM_ID, ndd.getCourtRoomId());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_OUCODE, ndd.getOucode());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_SESSION, ndd.getSession());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_CENTRE_ID, ndd.getCourtCentreId());

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

    private static void addNullableStringField(final JsonObjectBuilder builder, final String fieldName, final Optional<String> optionalValue) {
        optionalValue.ifPresent(value -> builder.add(fieldName, value));
    }

    private static void addNullableIntegerFieldIfNotNull(final JsonObjectBuilder builder, final String fieldName, final Optional<Integer> optionalValue) {
        optionalValue.ifPresent(value -> builder.add(fieldName, value));
    }

    private static void addNullableIntegerField(final JsonObjectBuilder builder, final String fieldName, final Optional<Integer> optionalValue) {
        if (optionalValue.isPresent()) {
            builder.add(fieldName, optionalValue.get());
        } else {
            builder.addNull(fieldName);
        }
    }

    private static void addNullableUUIDField(final JsonObjectBuilder builder, final String fieldName, final Optional<UUID> optionalValue) {
        optionalValue.ifPresent(value -> builder.add(fieldName, value.toString()));
    }

    private static void addOptionalBooleanField(final JsonObjectBuilder builder, final String fieldName, final Optional<Boolean> optionalValue) {
        optionalValue.ifPresent(value -> builder.add(fieldName, value));
    }

    private void createMessageConsumers() {
        privateMessageConsumerAllocatedHearingUpdatedForListing = privateEvents.createPrivateConsumer(EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING);
        privateMessageConsumerHearingUnallocatedForListing = privateEvents.createPrivateConsumer(EVENT_SELECTOR_HEARING_UNALLOCATED_FOR_LISTING);
        privateMessageConsumerStartDateChanged = privateEvents.createPrivateConsumer(EVENT_SELECTOR_HEARING_START_DATE_CHANGED);
        privateMessageConsumerCourtRoomRemoved = privateEvents.createPrivateConsumer(EVENT_SELECTOR_COURT_ROOM_REMOVED);
        privateMessageConsumerEndDateChanged = privateEvents.createPrivateConsumer(EVENT_SELECTOR_END_DATE_CHANGED);
        privateMessageConsumerEndDateRemoved = privateEvents.createPrivateConsumer(EVENT_SELECTOR_END_DATE_REMOVED);
        privateMessageConsumerHearingDaysChanged = privateEvents.createPrivateConsumer(EVENT_SELECTOR_HEARING_DAYS_CHANGED);
        publicMessageConsumerHearingConfirmed = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_HEARING_CONFIRMED);
        publicMessageConsumerHearingUpdated = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_HEARING_UPDATED);
        publicMessageConsumerVacatedTrialUpdated = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_VACATED_TRIAL_UPDATED);
        privateMessageConsumerWeekCommencingDatesRemoved = privateEvents.createPrivateConsumer(EVENT_SELECTOR_WEEK_COMMENCING_DATES_REMOVED);
        privateMessageConsumerPublicListNoteRemoved = privateEvents.createPrivateConsumer(EVENT_SELECTOR_PUBLIC_LIST_NOTE_REMOVED);
        publicMessageConsumerHearingChangesSaved = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_CHANGES_SAVED);
        publicEventHearingDaysChangedForHearing = publicEvents.createPublicConsumer(PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING);
        privateMessageConsumerStartDateRemoved = privateEvents.createPrivateConsumer(EVENT_SELECTOR_START_DATE_REMOVED);
        privateMessageConsumerWeekCommencingDateChanged = privateEvents.createPrivateConsumer(EVENT_SELECTOR_LISTING_EVENTS_WEEK_COMMENCING_DATE_CHANGED_FOR_HEARING);
        privateMessageConsumerHearingRequestedForListing = privateEvents.createPrivateConsumer(EVENT_SELECTOR_HEARING_REQUESTED_FOR_LISTING);
        publicMessageConsumerHearingRequested = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_HEARING_REQUESTED_FOR_LISTING);
        privateMessageConsumerHearingRequestedForListing = privateEvents.createPrivateConsumer(EVENT_SELECTOR_HEARING_REQUESTED_FOR_LISTING);
        publicMessageConsumerHearingRequested = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_HEARING_REQUESTED_FOR_LISTING);
        publicEventMessageProducer = publicEvents.createPublicProducer();
    }

    private void createMessageConsumersForDefendantSplit() {
        privateMessageConsumerAllocatedHearingUpdatedForListing = privateEvents.createPrivateConsumer(EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING);
        publicEventMessageProducer = publicEvents.createPublicProducer();
    }


    public void whenHearingIsUpdatedForListing() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingData(updatedHearingData).build().toString();

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenHearingIsUpdatedForListingHmiEnabled() {
        stubGetReferenceDataCourtCentreHmiListingEnabled(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtWithHmiListingEnabledCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingData(updatedHearingData).build().toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request);

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenHearingIsUpdatedForListingHmiEnabledWithoutCourtRoomSelection() {
        stubGetReferenceDataCourtCentreHmiListingEnabled(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtWithHmiListingEnabledCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingDataWithoutCourtRoomSelection(updatedHearingData).build().toString();

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(ACCEPTED.getStatusCode()));
    }

    public void whenHearingIsUpdatedFromHmi() {
        stubGetReferenceDataCourtCentreHmiListingEnabled(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtWithHmiListingEnabledCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());

        final JsonObject requestJson = prepareJsonForUpdatedHearingData(updatedHearingData).add("hearingId", updatedHearingData.getHearingId().toString()).build();

        sendMessage(
                publicEventMessageProducer,
                PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI,
                requestJson,
                metadataOf(randomUUID(), PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI).withUserId(randomUUID().toString()).build());

    }

    public void whenHearingIsUpdatedFromHmi(final List<String> nullFields) {
        final JsonObject requestJson = prepareJsonForUpdatedHearingDataForPublicEvent(updatedHearingData, nullFields).add("hearingId", updatedHearingData.getHearingId().toString()).build();
        request = requestJson.toString();

        LOGGER.info("Send public : \n\n\tNAME = {} \n\tPayload = {}\n\n", PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI, request);

        sendMessage(
                publicEventMessageProducer,
                PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI,
                requestJson,
                metadataOf(randomUUID(), PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI).withUserId(randomUUID().toString()).build());

    }

    public void whenHearingIsUpdatedFromHmiWithoutCourtRoomSelection() {
        stubGetReferenceDataCourtCentreHmiListingEnabledWithoutCourtRoomSelection(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtWithHmiListingEnabledCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());

        final JsonObject requestJson = prepareJsonForUpdatedHearingDataWithoutCourtRoomSelection(updatedHearingData).add("hearingId", updatedHearingData.getHearingId().toString()).build();

        sendMessage(
                publicEventMessageProducer,
                PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI,
                requestJson,
                metadataOf(randomUUID(), PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI).withUserId(randomUUID().toString()).build());

    }

    public void whenHearingIsUpdatedForListingWithPublicListNote() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingDataWithVideoLinkDetails(updatedHearingData);

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(ACCEPTED.getStatusCode()));
    }

    public void whenHearingIsUpdatedForListingWithProsecutionCases() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingDataWithProsecutionCases(updatedHearingData, listedCaseDatas);

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request);

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(ACCEPTED.getStatusCode()));
    }

    public void whenHearingIsUpdatedForListingWithProsecutionCasesDefendantsSplit() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingDataWithProsecutionCasesDefs(updatedHearingData, listedCaseDatas);

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenJudiciaryIsChangedForHearings() {

        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS), updatedHearingData.getHearingId()));

        request = prepareJsonForChangeJudiciaryForHearings(updatedHearingData);

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(ACCEPTED.getStatusCode()));
    }

    public void verifyPublicEventHearingChangesSaved() {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingChangesSaved, isJson(Matchers.allOf(
                withJsonPath("$.hearingId", is(updatedHearingData.getHearingId().toString())))));

        assertNotNull(jsonResponse);
    }

    public void verifyPrivateEventPublicListNoteRemoved() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerPublicListNoteRemoved);
        assertThat(jsonResponse.get("publicListNote"), is(nullValue()));
    }

    public void verifyEmptyHearingDaysChangedEventInActiveMQ() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingDaysChanged);
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

        assertThat(jsonResponse.get("hearingDays"), hasSize(0));
    }

    public void verifyPublicEventHearingConfirmed() {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingConfirmed);
        verifyHearingPublicDetails(jsonResponse, "confirmedHearing");
    }

    public void verifyPublicEventHearingDaysChangedForHearing() {
        final JsonPath jsonResponse = retrieveMessage(publicEventHearingDaysChangedForHearing);

        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("hearingDays[0].courtCentreId"), is(updatedHearingData.getCourtCentreId().toString()));
    }

    public void verifyPublicEventHearingConfirmed_hasNoJudiciary() {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingConfirmed);
        assertThat(jsonResponse.get("confirmedHearing.id"), is(updatedHearingData.getHearingId().toString()));
        assertNull(jsonResponse.get("confirmedHearing.judiciary"));
    }

    public void verifyPublicEventHearingUpdated() {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingUpdated);
        verifyHearingPublicDetails(jsonResponse, "updatedHearing");
    }

    public void verifyHearingRequestedForListingInPublicMQ() {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerHearingRequested);
        assertNotNull(jsonResponse);
        assertThat(((ArrayList) jsonResponse.get("listNewHearing.nonDefaultDays")).size(), is(2));
    }

    public void verifyPublicEventVacatedTrialUpdated(final boolean allocated, final boolean isVacated) {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerVacatedTrialUpdated);
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("allocated"), is(allocated));
        assertThat(jsonResponse.get("isVacated"), is(isVacated));

    }

    private void verifyHearingPublicDetails(final JsonPath jsonResponse, final String publicEventType) {
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
        assertThat(jsonResponse.get(publicEventType + ".judiciary[0].userId"), is(updatedHearingData.getJudiciary().get(0).getUserId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".prosecutionCases[0].id"), is(hearingData.getListedCases().get(0).getCaseId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".prosecutionCases[0].defendants[0].id"), is(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".prosecutionCases[0].defendants[0].offences[0].id"), is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));
        assertThat(jsonResponse.get(publicEventType + ".hearingDays[0].sittingDay"),
                is(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get(publicEventType + ".hearingDays[0].listedDurationMinutes"), is(updatedHearingData.getNonDefaultDays().get(0).getDuration().get()));
        assertThat(jsonResponse.get(publicEventType + ".reportingRestrictionReason"), is(hearingData.getReportingRestrictionReason()));
    }

    public void verifyHearingUpdatedWithNoEndDateResultsInUnallocationInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyEndDateRemovedEvent();
        verifyUnallocatedHearingEvent();
        verifyEmptyHearingDaysChangedEventInActiveMQ();
    }

    public void verifyUnallocatedHearingEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingUnallocatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingUnallocatedForListing: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    public void verifyEndDateRemovedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerEndDateRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

    }

    public void verifyCourtRoomRemovedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerCourtRoomRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerCourtRoomRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    private void verifyEndDateChangedEvent() {
        final JsonPath jsonResponse;
        jsonResponse = retrieveMessage(privateMessageConsumerEndDateChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("endDate"), is(updatedHearingData.getEndDate()));

    }

    private void verifyPrivateEventStartDateChangedForHearing() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerStartDateChanged);
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("startDate"), is(updatedHearingData.getStartDate()));
    }


    public void verifyHearingRequestedForListingEvent(final int count) {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerHearingRequestedForListing);
        assertThat(jsonResponse.getMap("listNewHearing").get("nonDefaultDays"), is(notNullValue()));
        assertThat(((ArrayList) jsonResponse.getMap("listNewHearing").get("nonDefaultDays")).size(), is(count));
    }

    public void verifyProsecutionCaseDefendantsOffenceIds(final int count) {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerAllocatedHearingUpdatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerAllocatedHearingUpdatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getList("prosecutionCaseDefendantsOffenceIds").size(), is(count));
    }


    public void verifyHearingUpdatedWithNoCourtRoomAndUnallocatedWhenQueryingFromAPI() {
        // this assertion is ok because the hearing is moving from allocated to unallocated list
        verifyHearingPayloadProperty(updatedHearingData.getHearingId().toString(), "courtRoomId", is(nullValue()));
    }

    public void verifyHearingUpdatedWithNoEndDateAndUnallocatedWhenQueryingFromAPI() {
        // this assertion is ok because the hearing is moving from allocated to unallocated list
        verifyHearingPayloadProperty(updatedHearingData.getHearingId().toString(), "endDate", is(nullValue()));
    }

    public void verifyHearingWithUpdatedJudiciaryWhenQueryingFromAPI() {

        final String hearingIdFilter = getHearingFilter(updatedHearingData.getHearingId().toString());
        pollForHearing(updatedHearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter + ".judiciary[0].judicialId",
                        hasItem(updatedHearingData.getJudiciary().get(0).getJudicialId().toString())),
                withJsonPath(hearingIdFilter + ".judiciary[0].judicialRoleType.judiciaryType",
                        hasItem(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())),
                withJsonPath(hearingIdFilter + ".judiciary[0].isBenchChairman",
                        hasItem(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().get())),
                withJsonPath(hearingIdFilter + ".judiciary[0].isDeputy")
        });
    }

    public void verifyHearingDaysWhenQueryingFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id.search-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getCourtRoomId(),
                        updatedHearingData.getStartDate()));

        final String hearingIdFilter = getHearingFilter(updatedHearingData.getHearingId().toString());
        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter + ".hearingDays[0].startTime",
                        hasItem(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT))),
                withJsonPath(hearingIdFilter + ".hearingDays[0].durationMinutes",
                        hasItem(updatedHearingData.getNonDefaultDays().get(0).getDuration().get())),
                withJsonPath(hearingIdFilter + ".hearingDays[0].matchedWithQuery", hasItem(true)),
                withJsonPath(hearingIdFilter + ".hearingDays[0].courtCentreId", hasItem(updatedHearingData.getCourtCentreId().toString())),
                withJsonPath(hearingIdFilter + ".hearingDays[0].courtRoomId", hasItem(updatedHearingData.getCourtRoomId().toString()))
        });

    }

    public void verifyHearingUpdatedWhenQueryingFromAPI() {
        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(updatedHearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{

                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].id",
                        equalTo(updatedHearingData.getHearingId().toString())),
                withJsonPath("$.hearings[0].judiciary[0].judicialId",
                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialId().toString())),
                withJsonPath("$.hearings[0].judiciary[0].judicialRoleType.judiciaryType",
                        equalTo(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())),
                withJsonPath("$.hearings[0].judiciary[0].isBenchChairman", equalTo(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().orElse(null))),
                withJsonPath("$.hearings[0].judiciary[0].isDeputy", equalTo(updatedHearingData.getJudiciary().get(0).getIsDeputy().orElse(null))),
                withJsonPath("$.hearings[0].courtRoomId",
                        equalTo(updatedHearingData.getCourtRoomId().toString())),

                withJsonPath("$.hearings[0].endDate",
                        equalTo(updatedHearingData.getEndDate())),
                withJsonPath("$.hearings[0].startDate",
                        equalTo(updatedHearingData.getStartDate())),
                withJsonPath("$.hearings[0].nonDefaultDays[0].startTime",
                        equalTo(fromString(updatedHearingData.getNonDefaultDays().get(0)
                                .getStartTime()).format(ZONED_DATE_TIME_FORMAT)))
        });
    }

    public void verifyHearingFoundByAllocatedFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated"),
                        ALLOCATED));

        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyCaseIdentifierWhenQueryingFromAPI(String hearingId, JsonObject payload, HearingsData hearingsData) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.listedCases[0].id", equalTo(payload.getString("prosecutionCaseId"))),
                                withJsonPath("$.listedCases[0].prosecutor.prosecutorId", equalTo(payload.getString("prosecutionAuthorityId"))),
                                withJsonPath("$.listedCases[0].prosecutor.prosecutorCode", equalTo(payload.getString("prosecutionAuthorityCode"))),
                                withJsonPath("$.listedCases[1].caseIdentifier.authorityId", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(1).getAuthorityId().toString())),
                                withJsonPath("$.listedCases[1].caseIdentifier.authorityCode", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(1).getAuthorityCode())),
                                withJsonPath("$.listedCases[1].caseIdentifier.caseReference", equalTo(hearingsData.getHearingData().get(0).getListedCases().get(1).getCaseReference()))

                        )));
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndStartDateAndEndDate() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.start-date.end-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(),
                        updatedHearingData.getEndDate()));

        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndSearchDate() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.search-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate()));

        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId()));

        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndCourtRoomFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getCourtRoomId()));

        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId()));

        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndAuthorityIdAndHearingTypFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code.hearing-type"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId(),
                        updatedHearingData.getHearingTypData().getTypeId()));

        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndIdHearingTypAndJurisdictionTypeFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code.hearing-type.jurisdiction-type"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId(),
                        updatedHearingData.getHearingTypData().getTypeId(),
                        updatedHearingData.getJurisdictionType()));
        pollUntilHearingIsPresent(searchHearingUrl, getLoggedInUser().toString(), hearingData.getId().toString());
    }

    public void verifyHearingUpdatedWhenWeekCommencingDateRemovedResultsInMQ() {
        verifyPrivateEventStartDateChangedForHearing();
        verifyEndDateChangedEvent();
        verifyWeekCommencingDateRemovedEvent();
    }

    private void verifyWeekCommencingDateRemovedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerWeekCommencingDatesRemoved);
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    public void verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI() {

        final String hearingId = updatedHearingData.getHearingId().toString();
        final String hearingIdFilter = getHearingFilter(hearingId);
        pollForHearing(updatedHearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter + ".hasVideoLink",
                        hasItem(updatedHearingData.getHasVideoLink())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].publicListNote",
                        hasItem(updatedHearingData.getPublicListNote()))
        });
    }

    public void verifyHearingWithUpdatedNoPublicListNoteWhenQueryingFromAPI() {

        final String hearingId = updatedHearingData.getHearingId().toString();
        final String hearingIdFilter = getHearingFilter(hearingId);
        pollForHearing(updatedHearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter + ".hasVideoLink",
                        hasItem(updatedHearingData.getHasVideoLink()))
        });
    }

    public void verifyHearingAllocatedWhenQueryingFromAPI() {

        final String hearingId = updatedHearingData.getHearingId().toString();
        final String hearingIdFilter = getHearingFilter(hearingId);
        pollForHearing(updatedHearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter + ".judiciary[0].judicialId",
                        hasItem(updatedHearingData.getJudiciary().get(0).getJudicialId().toString())),
                withJsonPath(hearingIdFilter + ".judiciary[0].judicialRoleType.judiciaryType",
                        hasItem(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())),
                withJsonPath(hearingIdFilter + ".judiciary[0].isBenchChairman",
                        hasItem(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().get())),
                withJsonPath(hearingIdFilter + ".judiciary[0].isDeputy",
                        hasItem(updatedHearingData.getJudiciary().get(0).getIsDeputy().get())),
                withJsonPath(hearingIdFilter + ".courtRoomId",
                        hasItem(updatedHearingData.getCourtRoomId().toString())),
                withJsonPath(hearingIdFilter + ".type.description",
                        hasItem(updatedHearingData.getHearingTypData().getTypeDescription())),
                withJsonPath(hearingIdFilter + ".jurisdictionType",
                        hasItem(updatedHearingData.getJurisdictionType())),
                withJsonPath(hearingIdFilter + ".hearingLanguage",
                        hasItem(updatedHearingData.getHearingLanguage())),
                withJsonPath(hearingIdFilter + ".endDate",
                        hasItem(updatedHearingData.getEndDate())),
                withJsonPath(hearingIdFilter + ".startDate",
                        hasItem(updatedHearingData.getStartDate())),
                withJsonPath(hearingIdFilter + ".hearingDays[0].startTime",
                        hasItem(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT))),
                withJsonPath(hearingIdFilter + ".hearingDays[0].durationMinutes",
                        hasItem(updatedHearingData.getNonDefaultDays().get(0).getDuration().get())),
                withJsonPath(hearingIdFilter + ".hearingDays[0].endTime",
                        hasItem(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime())
                                .plusMinutes(updatedHearingData.getNonDefaultDays().get(0).getDuration().get())
                                .format(ZONED_DATE_TIME_FORMAT))),
                withJsonPath(hearingIdFilter + ".nonSittingDays[0]",
                        hasItem(updatedHearingData.getNonSittingDays().get(0))),
                withJsonPath(hearingIdFilter + ".nonDefaultDays[0].startTime",
                        hasItem(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT)))
        });
    }

    public void verifyStartDateRemovedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerStartDateRemoved);
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    public void verifyWeekCommercingDateChangedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerWeekCommencingDateChanged);
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    public void updateHearingForListing(final JsonObject updateHearingJsonObject, final UUID hearingId) {
        final UUID courtCentreId = getUUID(updateHearingJsonObject, "courtCentreId").orElse(null);
        final UUID courtRoomId = getUUID(updateHearingJsonObject, "courtRoomId").orElse(null);
        final Optional<JsonObject> selectedCourtCentre = getJsonObject(updateHearingJsonObject, "selectedCourtCentre");
        final UUID hearingTypeId = UUID.fromString(updateHearingJsonObject.getJsonObject("type").getString("id"));
        final JsonArray judiciary = updateHearingJsonObject.getJsonArray("judiciary");
        if (judiciary != null && !judiciary.isEmpty()) {
            final UUID judicialId = UUID.fromString(judiciary.getValuesAs(JsonObject.class).stream().findFirst().get().getString("id"));
            stubGetReferenceDataJudiciaries(judicialId);
        }

        if (selectedCourtCentre.isPresent()) {
            final JsonObject courtCentre = selectedCourtCentre.get();
            final UUID centreId = getUUID(courtCentre, "id").get();
            final UUID roomId = getUUID(courtCentre, "courtRoomId").get();
            final CourtCentreData courtCentreData = new CourtCentreData(centreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, roomId, "City of London Magistrates' Court");
            stubGetReferenceDataCourtCentreById(courtCentreData);
        }

        final CourtCentreData courtCentreData = new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, courtRoomId, "City of London Magistrates' Court");
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataHearingTypes(hearingTypeId);

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), hearingId));

        request = updateHearingJsonObject.toString();

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(ACCEPTED.getStatusCode()));
    }

    public JsonObject preparePayloadToUpdateHearing(final String fileName, final Map<String, String> values) throws IOException {
        String eventPayloadString = getStringFromResource(fileName)
                .replaceAll("HEARING_ID", values.get("hearingId"))
                .replaceAll("CASE_ID", values.get("caseId"))
                .replaceAll("COURT_CENTRE_ID", values.get("courtCentreId"))
                .replaceAll("COURT_ROOM_ID", values.get("courtRoomId"))
                .replaceAll("START_DATE", values.get("startDate"))
                .replaceAll("END_DATE", values.get("endDate"))
                .replaceAll("DEFENDANT_ID", values.get("defendantId"))
                .replaceAll("OFFENCE_ID", values.get("offenceId"))
                .replaceAll("FIRST_NON_DEFAULT_DAY_START_DATE", values.get("firstNonDefaultDayStartDate"))
                .replaceAll("SECOND_NON_DEFAULT_DAY_START_DATE", values.get("secondNonDefaultDayStartDate"));

        if (values.get("updatedCourtCentreId") != null && values.get("updatedCourtRoomId") != null) {
            eventPayloadString = eventPayloadString.replaceAll("UPDATED_CENTRE_ID", values.get("updatedCourtCentreId"));
            eventPayloadString = eventPayloadString.replaceAll("UPDATED_ROOM_ID", values.get("updatedCourtRoomId"));
        }

        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }

    public void verifyAllocatedHearingFound(final UUID courtCentreId, final UUID courtRoomId, final String searchDate, final Matcher[] matchers) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings"), courtCentreId, courtRoomId, searchDate, ALLOCATED));

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), matchers);
    }

    public void verifyAllocatedHearingFoundByRangeSearch(final UUID courtCentreId, final String searchDate, final Matcher[] matchers) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.search-date"),
                        ALLOCATED,
                        courtCentreId,
                        searchDate));

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), matchers);
    }

    public void verifyHearingPayloadProperty(final String hearingId, final String propertyName, final Matcher<Object> matcher) {
        final String hearingIdFilter = getHearingFilter(hearingId);

        final String payload = pollForHearing(updatedHearingData.getCourtCentreId().toString(), UNALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter, hasSize(1))
        });

        final JsonObject payloadAsJsonObject = new StringToJsonObjectConverter().convert(payload);
        Object courtRoomId = payloadAsJsonObject.getJsonArray("hearings").stream().
                map(h -> (JsonObject) h)
                .filter(h -> h.getString("id").equals(hearingId))
                .findFirst().get().get(propertyName);

        assertThat(courtRoomId, matcher);
    }

}
