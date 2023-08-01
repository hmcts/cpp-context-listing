package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreHmiListingEnabled;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreHmiListingEnabledWithoutCourtRoomSelection;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtWithHmiListingEnabledCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
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
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateHearingSteps extends AbstractIT implements AutoCloseable {

    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_JUDICIAL_ROLE_TYPE_ID = "judicialRoleTypeId";
    public static final String FIELD_JUDICIARY_TYPE = "judiciaryType";
    public static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    public static final String PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI = "public.staginghmi.hearing-updated-from-hmi";
    public static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING = "application/vnd.listing.command.update-hearing-for-listing+json";
    public static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING_FROM_HMI = "application/vnd.listing.command.update-hearing-from-hmi+json";
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
    public static final String FIELD_HEARING_LANGUAGE = "hearingLanguage";
    public static final String FIELD_JURISDICTION_TYPE = "jurisdictionType";
    private static final String FIELD_PROSECUTION_CASES = "prosecutionCases";
    private static final String FIELD_HAS_VIDEO_LINK = "hasVideoLink";
    private static final String FIELD_PUBLIC_LIST_NOTE = "publicListNote";
    private static final String FIELD_USER_ID = "userId";
    public static final String PANEL = "panel";
    public static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";
    public static final String FIELD_HEARING_TYPE_ID = "id";
    public static final String FIELD_HEARING_TYPE_DESCRIPTION = "description";
    public static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    protected static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing-v2";
    private static final String EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing-v2";
    private static final String EVENT_SELECTOR_HEARING_UNALLOCATED_FOR_LISTING = "listing.events.hearing-unallocated-for-listing";
    private static final String EVENT_SELECTOR_HEARING_TYPE_CHANGED = "listing.events.type-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_JURISDICTION_CHANGED = "listing.events.jurisdiction-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_LANGUAGE_CHANGED = "listing.events.hearing-language-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_START_DATE_CHANGED = "listing.events.start-date-changed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_RESCHEDULED = "listing.events.hearing-rescheduled";
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
    private static final String EVENT_SELECTED_PUBLIC_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";
    private static final String EVENT_SELECTOR_WEEK_COMMENCING_DATES_REMOVED = "listing.events.week-commencing-date-removed-for-hearing";
    private static final String EVENT_SELECTOR_HEARING_PARTIALLY_UPDATED = "listing.events.hearing-partially-updated";
    private static final String EVENT_SELECTOR_PUBLIC_LIST_NOTE_CHANGED = "listing.events.public-list-note-changed-for-hearing";
    private static final String EVENT_SELECTOR_PUBLIC_LIST_NOTE_REMOVED = "listing.events.public-list-note-removed-from-hearing";
    private static final String EVENT_SELECTOR_START_DATE_REMOVED = "listing.events.start-date-removed-for-hearing";
    public static final String EVENT_SELECTOR_LISTING_EVENTS_WEEK_COMMENCING_DATE_CHANGED_FOR_HEARING = "listing.events.week-commencing-date-changed-for-hearing";
    private static final String FIELD_HEARINGS = "hearings";
    private static final int DEFAULT_DURATION_MINS = 120;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final String LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS = "listing.command.change-judiciary-for-hearings";
    private static final String MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS = "application/vnd.listing.command.change-judiciary-for-hearings+json";
    protected static final Logger LOGGER = LoggerFactory.getLogger(UpdateHearingSteps.class);
    public static final String PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI = "public.listing.updated-hearing-in-staging-hmi";
    public static final String PUBLIC_LISTING_HEARING_CHANGES_SAVED = "public.listing.hearing-changes-saved";
    private static final String PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING = "public.listing.hearing-days-changed-for-hearing";
    public static final String FIELD_WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    public static final String FIELD_WEEK_COMMENCING_DURATION_IN_WEEKS = "weekCommencingDurationInWeeks";
    public static final String FIELD_WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    protected UpdatedHearingData updatedHearingData;
    protected HearingData hearingData;
    private List<ListedCaseData> listedCaseDatas;
    private MessageConsumer privateMessageConsumerAllocatedHearingUpdatedForListing;
    protected MessageConsumer privateMessageConsumerHearingAllocatedForListing;
    private MessageConsumer privateMessageConsumerHearingUnallocatedForListing;
    private MessageConsumer privateMessageConsumerTypeChanged;
    private MessageConsumer privateMessageConsumerJurisdictionChanged;
    private MessageConsumer privateMessageConsumerHearingLanguageChanged;
    private MessageConsumer privateMessageConsumerStartDateChanged;
    private MessageConsumer privateMessageConsumerHearingRescheduled;
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
    private MessageConsumer publicMessageConsumerVacatedTrialUpdated;
    private MessageConsumer privateMessageConsumerWeekCommencingDatesRemoved;
    private MessageConsumer privateMessageConsumerHearingPartiallyUpdated;
    private MessageConsumer privateMessageConsumerPublicListNoteChanged;
    private MessageConsumer privateMessageConsumerPublicListNoteRemoved;
    private MessageConsumer publicMessageConsumerHmiHearingUpdated;
    private MessageConsumer publicMessageConsumerHearingChangesSaved;
    private MessageConsumer publicEventHearingDaysChangedForHearing;
    private MessageProducer publicEventMessageProducer;
    private MessageConsumer privateMessageConsumerStartDateRemoved;
    private MessageConsumer privateMessageConsumerWeekCommencingDateChanged;


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

    public void verifyHearingListedFromAPI(final boolean isAllocated, final Matcher matchers, final String weekCommencingStartDate, final String weekCommencingEndDate) {

        StringBuffer searchHearingUrl = new StringBuffer();
        searchHearingUrl.append(String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), updatedHearingData.getCourtCentreId(), isAllocated)));

        Optional.ofNullable(weekCommencingStartDate).ifPresent(value -> searchHearingUrl.append("&weekCommencingStartDate=" + weekCommencingStartDate));
        Optional.ofNullable(weekCommencingEndDate).ifPresent(value -> searchHearingUrl.append("&weekCommencingEndDate=" + weekCommencingEndDate));
        verifyHearingListedFromWithApiUrl(searchHearingUrl.toString(), matchers);
    }

    public void verifyHearingListedFromWithApiUrl(final String searchHearingUrl, final Matcher matchers) {
        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(matchers));
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
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add));
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
                arrayBuilder.add(createObjectBuilder().add("startTime", data.getStartTime().toString()).add("duration", data.getDuration().get()));
            });
            builder.add("nonDefaultDays", arrayBuilder.build());
        });
        // Optional.ofNullable(updatedHearingData.getJudiciary()).filter(value -> !nullFields.contains(value)).ifPresent(judiciary -> builder.add(FIELD_JUDICIARY, prepareJsonJudiciary(judiciary)));
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
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add));
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
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()))
                .add(FIELD_HAS_VIDEO_LINK, updatedHearingData.getHasVideoLink());

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
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()))
                .add(FIELD_PROSECUTION_CASES, prepareJsonProsecutionCases(listedCaseDataList));

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
                            caseBuilder.add("defendants", p.getDefendants().stream()
                                    .map(d -> {
                                        final JsonObjectBuilder defendantBuilder = createObjectBuilder();
                                        defendantBuilder.add("defendantId", d.getDefendantId().toString());
                                        defendantBuilder.add("offences", d.getOffences().stream()
                                                .map(o -> {
                                                    final JsonObjectBuilder offenceBuilder = createObjectBuilder();
                                                    offenceBuilder.add("offenceId", o.getOffenceId().toString());
                                                    return offenceBuilder;
                                                }).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add));
                                        return defendantBuilder;
                                    }).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add));
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
                    .add(FIELD_HEARING_TYPE_DESCRIPTION, hearingType.getTypeDescription());
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
        privateMessageConsumerAllocatedHearingUpdatedForListing = privateEvents.createConsumer(EVENT_SELECTOR_ALLOCATED_HEARING_UPDATED_FOR_LISTING);
        privateMessageConsumerHearingAllocatedForListing = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_ALLOCATED_FOR_LISTING);
        privateMessageConsumerHearingUnallocatedForListing = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_UNALLOCATED_FOR_LISTING);
        privateMessageConsumerTypeChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_TYPE_CHANGED);
        privateMessageConsumerJurisdictionChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_JURISDICTION_CHANGED);
        privateMessageConsumerHearingLanguageChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_LANGUAGE_CHANGED);
        privateMessageConsumerStartDateChanged = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_START_DATE_CHANGED);
        privateMessageConsumerHearingRescheduled = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_RESCHEDULED);
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
        publicMessageConsumerVacatedTrialUpdated = publicEvents.createConsumer(EVENT_SELECTED_PUBLIC_VACATED_TRIAL_UPDATED);
        privateMessageConsumerWeekCommencingDatesRemoved = privateEvents.createConsumer(EVENT_SELECTOR_WEEK_COMMENCING_DATES_REMOVED);
        privateMessageConsumerHearingPartiallyUpdated = privateEvents.createConsumer(EVENT_SELECTOR_HEARING_PARTIALLY_UPDATED);
        privateMessageConsumerPublicListNoteChanged = privateEvents.createConsumer(EVENT_SELECTOR_PUBLIC_LIST_NOTE_CHANGED);
        privateMessageConsumerPublicListNoteRemoved = privateEvents.createConsumer(EVENT_SELECTOR_PUBLIC_LIST_NOTE_REMOVED);
        publicMessageConsumerHmiHearingUpdated = publicEvents.createConsumer(PUBLIC_LISTING_UPDATE_HEARING_IN_STAGING_HMI);
        publicMessageConsumerHearingChangesSaved = publicEvents.createConsumer(PUBLIC_LISTING_HEARING_CHANGES_SAVED);
        publicEventHearingDaysChangedForHearing = publicEvents.createConsumer(PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING);
        privateMessageConsumerStartDateRemoved = privateEvents.createConsumer(EVENT_SELECTOR_START_DATE_REMOVED);
        privateMessageConsumerWeekCommencingDateChanged = privateEvents.createConsumer(EVENT_SELECTOR_LISTING_EVENTS_WEEK_COMMENCING_DATE_CHANGED_FOR_HEARING);
        publicEventMessageProducer = QueueUtil.publicEvents.createProducer();
    }

    public void whenHearingIsUpdatedForListing() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingData(updatedHearingData).build().toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request);

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

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

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request);

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenHearingIsUpdatedFromHmi() {
        stubGetReferenceDataCourtCentreHmiListingEnabled(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataCourtWithHmiListingEnabledCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        final JsonObject requestJson = prepareJsonForUpdatedHearingData(updatedHearingData).add("hearingId", updatedHearingData.getHearingId().toString()).build();


        LOGGER.info("Send public : \n\n\tNAME = {} \n\tPayload = {}\n\n", PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI, requestJson);

        QueueUtil.sendMessage(
                publicEventMessageProducer,
                PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI,
                requestJson,
                metadataOf(randomUUID(), PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI).withUserId(randomUUID().toString()).build());

    }

    public void whenHearingIsUpdatedFromHmi(final List<String> nullFields) {
        final JsonObject requestJson = prepareJsonForUpdatedHearingDataForPublicEvent(updatedHearingData, nullFields).add("hearingId", updatedHearingData.getHearingId().toString()).build();
        request = requestJson.toString();

        LOGGER.info("Send public : \n\n\tNAME = {} \n\tPayload = {}\n\n", PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI, request);

        QueueUtil.sendMessage(
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

        LOGGER.info("Send public : \n\n\tNAME = {} \n\tPayload = {}\n\n", PUBLIC_UPDATED_HEARING_FOR_LISTING_FROM_HMI, requestJson);

        QueueUtil.sendMessage(
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

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
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

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public void whenJudiciaryIsChangedForHearings() {

        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS), updatedHearingData.getHearingId()));

        request = prepareJsonForChangeJudiciaryForHearings(updatedHearingData);

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_CHANGE_JUDICIARY_FOR_HEARINGS, request);

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

    public void verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        verifyPublicListNoteChangedEvent();
    }

    public void verifyHearingUpdatedResultsForPublicListNoteInMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyPublicListNoteChangedEvent();

    }

    public void verifyHmiPublicEventForUpdateHearing() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHmiHearingUpdated, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(updatedHearingData.getHearingId().toString())),
                withJsonPath("$.hearing.bookingType", is("Video")),
                withJsonPath("$.hearing.specialRequirements", hasSize(2)),
                withJsonPath("$.hearing.specialRequirements", hasItems("RVC", "GSN")),
                withJsonPath("$.hearing.priority", is("High")))));


        assertNotNull(jsonResponse);
        LOGGER.info("jsonResponse from publicMessageConsumerHmiHearingUpdated: {}", jsonResponse.prettify());
    }

    public void verifyPublicHearingChangesSaved() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerHearingChangesSaved, isJson(Matchers.allOf(
                withJsonPath("$.hearingId", CoreMatchers.is(updatedHearingData.getHearingId().toString())))));

        assertNotNull(jsonResponse);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingChangesSaved: {}", jsonResponse.prettify());
    }

    public void verifyHearingUpdatedResultsForRemovingPublicListNoteInMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyPublicListNoteRemovedEvent();

    }

    private void verifyHearingDaysChangedEventForBothDays() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.debug("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());

        verifyFirstHearingDay(jsonResponse);

        //test usage of reference data courtcentre default startTime and duration
        final ZonedDateTime lastHearingDayStartDateTime = ZonedDateTime.of(LocalDate.parse(updatedHearingData.getEndDate()), DEFAULT_START_TIME, UTC)
                .withZoneSameInstant(UTC);
        assertThat(jsonResponse.get("hearingDays[1].startTime"), is(lastHearingDayStartDateTime.format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("hearingDays[1].durationMinutes"), is(DEFAULT_DURATION_MINS));
        assertThat(jsonResponse.get("hearingDays[1].endTime"),
                is(lastHearingDayStartDateTime.plusMinutes(DEFAULT_DURATION_MINS).format(ZONED_DATE_TIME_FORMAT)));
        assertThat(jsonResponse.get("hearingDays[1].courtCentreId"),
                updatedHearingData.getNonDefaultDays().get(1).getCourtCentreId().isPresent() ? is(updatedHearingData.getNonDefaultDays().get(1).getCourtCentreId().get()) : nullValue());
        assertThat(jsonResponse.get("hearingDays[1].courtRoomId"),
                updatedHearingData.getNonDefaultDays().get(1).getRoomId().isPresent() ? is(updatedHearingData.getNonDefaultDays().get(1).getRoomId().get()) : nullValue());

    }

    public void verifyHearingDaysChangedEventForOneDayOnly() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());

        verifyFirstHearingDay(jsonResponse);
    }

    public void verifyHearingDaysChangedEventForOneDayOnlyWithoutCourtRoomSelection() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingDaysChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingDaysChanged: {}", jsonResponse.prettify());

        verifyFirstHearingDayWithoutCourtRoomSelection(jsonResponse);
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
        assertThat(jsonResponse.get("hearingDays[0].courtCentreId"),
                updatedHearingData.getNonDefaultDays().get(0).getCourtCentreId().isPresent() ? is(updatedHearingData.getNonDefaultDays().get(0).getCourtCentreId().get()) : nullValue());
        assertThat(jsonResponse.get("hearingDays[0].courtRoomId"),
                updatedHearingData.getNonDefaultDays().get(0).getRoomId().isPresent() ? is(updatedHearingData.getNonDefaultDays().get(0).getRoomId().get()) : nullValue());
        assertThat(jsonResponse.get("hearingDays[0].endTime"),
                is(fromString(startTime).plusMinutes(duration).format(ZONED_DATE_TIME_FORMAT)));
    }

    private void verifyFirstHearingDayWithoutCourtRoomSelection(final JsonPath jsonResponse) {
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        final String startTime = updatedHearingData.getNonDefaultDays().get(0).getStartTime();
        assertThat(jsonResponse.get("hearingDays[0].sequence"),
                is(0));
        assertThat(jsonResponse.get("hearingDays[0].courtCentreId"),
                updatedHearingData.getNonDefaultDays().get(0).getCourtCentreId().isPresent() ? is(updatedHearingData.getNonDefaultDays().get(0).getCourtCentreId().get()) : nullValue());
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
        verifyHearingRescheduledEvent();
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

    public void verifyHearingDaysChangedForHearingEvent() {

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicEventHearingDaysChangedForHearing);
        LOGGER.info("jsonResponse from publicEventHearingDaysChangedForHearing: {}", jsonResponse.prettify());

        verifyHearingCourtRoomNotSelectedPublicDetails(jsonResponse);
    }

    public void verifyHearingCourtRoomNotSelectedPublicDetails(final JsonPath jsonResponse) {
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("hearingDays[0].courtCentreId"), is(updatedHearingData.getCourtCentreId().toString()));
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

    public void verifyVacatedTrialUpdatedInPublicMQ(final boolean allocated, final boolean isVacated) {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyVacatedTrialUpdatedPunlicEvent(allocated, isVacated);
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

    private void verifyVacatedTrialUpdatedPunlicEvent(final boolean allocated, final boolean isVacated) {

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerVacatedTrialUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingUpdated: {}", jsonResponse.prettify());
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

    public void verifyHearingWithUpdatedVideoLinkDetailsInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyPublicListNoteChangedEvent();
    }

    public void verifyHearingWithUpdatedJudiciaryInMQ() {

        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        verifyJudiciaryChangedForHearing();
        verifyAllocatedHearingUpdatedForListing();
    }

    public void verifyUnallocatedHearingEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingUnallocatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingUnallocatedForListing: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    public void verifyEndDateRemovedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerEndDateRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerEndDateRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

    }

    private void verifyJudiciaryChangedForHearing() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));

    }

    public void verifyCourtRoomRemovedEvent() {
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

    private void verifyHearingRescheduledEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingRescheduled);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingRescheduled: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    private void verifyPublicListNoteChangedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerPublicListNoteChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerVideoLinkDetailsChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("publicListNote"), is(updatedHearingData.getPublicListNote()));
    }

    private void verifyPublicListNoteRemovedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerPublicListNoteRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerVideoLinkDetailsRemoved: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("publicListNote"), is(nullValue()));
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

    public void verifyJudiciaryChangedEventWithRotaSLJudiciaries(final List<JudicialRoleData> judicialRoleDataList) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerJudiciaryChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerJudiciaryChanged: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        IntStream.range(0, judicialRoleDataList.size())
                .forEach(judiciaryIndex -> {
                    final String baseJudiciaryPath = String.format("judiciary[%d]", judiciaryIndex);
                    assertThat(jsonResponse.get(baseJudiciaryPath + ".judicialId"), is(judicialRoleDataList.get(judiciaryIndex).getJudicialId().toString()));
                    assertThat(jsonResponse.get(baseJudiciaryPath + ".judicialRoleType.judiciaryType"), is(judicialRoleDataList.get(judiciaryIndex).getJudicialRoleType().getJudiciaryType()));
                    assertThat(jsonResponse.getBoolean(baseJudiciaryPath + ".isBenchChairman"), is(judicialRoleDataList.get(judiciaryIndex).getIsBenchChairman().get()));
                    assertThat(jsonResponse.get(baseJudiciaryPath + ".isDeputy"), is(judicialRoleDataList.get(judiciaryIndex).getIsDeputy().get()));
                });
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

    protected void verifyHearingAllocatedEvent() {
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

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].offenceIds[0].id"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));

    }

    public void verifyHearingAllocatedEventNotExistsRemovedOffence(UUID offenceId) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingAllocatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingAllocatedForListing: {}", jsonResponse.prettify());


        assertThat(jsonResponse.prettify(), not(hasJsonPath("$.prosecutionCaseDefendantsOffenceIds[*].defendants[*].offenceIds[*]", contains(offenceId.toString()))));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].id"),
                is(hearingData.getListedCases().get(0).getCaseId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].id"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].offenceIds[0].id"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));
    }

    public void verifyProsecutionCaseDefendantsOffenceIds(final int count) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerAllocatedHearingUpdatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerAllocatedHearingUpdatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getList("prosecutionCaseDefendantsOffenceIds").size(), is(count));
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

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].offenceIds[0].id"),
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
                        ))
                );
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

    public void verifyHearingDaysWhenQueryFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id.search-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getCourtRoomId(),
                        updatedHearingData.getStartDate()));

        final Filter idFilter = filter(where("id").is(hearingData.getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].hearingDays[0].startTime",
                                        equalTo(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).format(ZONED_DATE_TIME_FORMAT))),
                                withJsonPath("$.hearings[0].hearingDays[0].durationMinutes",
                                        equalTo(updatedHearingData.getNonDefaultDays().get(0).getDuration().get())),
                                withJsonPath("$.hearings[0].hearingDays[0].matchedWithQuery", equalTo(true)),
                                withJsonPath("$.hearings[0].hearingDays[0].courtCentreId", equalTo(updatedHearingData.getCourtCentreId().toString())),
                                withJsonPath("$.hearings[0].hearingDays[0].courtRoomId", equalTo(updatedHearingData.getCourtRoomId().toString()))
                        )));


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

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreFromAPIAndSearchDate() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.search-date"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate()));

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
                        updatedHearingData.getHearingTypData().getTypeId()));

        verifyHearingFound(searchHearingUrl);
    }

    public void verifyHearingFoundByAllocatedAndCourtCentreAndIdHearingTypAndJurisdictionTypeFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.authority-code.hearing-type.jurisdiction-type"),
                        ALLOCATED,
                        updatedHearingData.getCourtCentreId(),
                        hearingData.getListedCases().get(0).getAuthorityId(),
                        updatedHearingData.getHearingTypData().getTypeId(),
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
        final JsonPath jsRequest = new JsonPath(request);
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

    public void verifyHearingPartiallyEventNotExistsRemovedOffence(UUID offenceId) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingPartiallyUpdated);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingPartiallyUpdated: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingIdToBeUpdated"), is(hearingData.getId().toString()));
        assertThat(jsonResponse.prettify(),hasJsonPath("$.prosecutionCases[*].caseId", hasItem(hearingData.getListedCases().get(0).getCaseId().toString())));
        assertThat(jsonResponse.prettify(), hasJsonPath("$.prosecutionCases[*].defendants[*].defendantId", hasItem(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())));
        assertThat(jsonResponse.prettify(), not(hasJsonPath("$.prosecutionCases[*].defendants[*].offences[*].offenceId", hasItem(offenceId.toString()))));
    }

    public void verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI() {

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
                                withJsonPath("$.hearings[0].hasVideoLink",
                                        equalTo(updatedHearingData.getHasVideoLink())),
                                withJsonPath("$.hearings[0].publicListNote",
                                        equalTo(updatedHearingData.getPublicListNote()))
                        )));
    }

    public void verifyHearingWithUpdatedNoPublicListNoteWhenQueryingFromAPI() {

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
                                withJsonPath("$.hearings[0].hasVideoLink",
                                        equalTo(updatedHearingData.getHasVideoLink())
                                )
                        )));
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
        privateMessageConsumerHearingRescheduled.close();
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
        publicMessageConsumerVacatedTrialUpdated.close();
        privateMessageConsumerWeekCommencingDatesRemoved.close();
        privateMessageConsumerPublicListNoteChanged.close();
        privateMessageConsumerPublicListNoteRemoved.close();
        publicMessageConsumerHmiHearingUpdated.close();
        publicEventHearingDaysChangedForHearing.close();
        publicEventMessageProducer.close();
        privateMessageConsumerStartDateRemoved.close();
        privateMessageConsumerWeekCommencingDateChanged.close();
    }

    public void verifyStartDateRemovedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerStartDateRemoved);
        LOGGER.info("jsonResponse from privateMessageConsumerStartDateRemoved: {}", jsonResponse.prettify());
        Assert.assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
    }

    public void verifyWeekCommercingDateChangedEvent() {
        final JsonPath jsonResponse = retrieveMessage(privateMessageConsumerWeekCommencingDateChanged);
        LOGGER.info("jsonResponse from privateMessageConsumerWeekCommencingDateChanged: {}", jsonResponse.prettify());
        Assert.assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
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
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\tHeader = {}\n\n", listCaseForHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING, request, getLoggedInHeader());

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                request, getLoggedInHeader());
        MatcherAssert.assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public JsonObject preparePayloadToUpdateHearing(final String fileName, final Map<String, String> values) throws IOException {
        String eventPayloadString = getStringFromResource(fileName)
                .replaceAll("HEARING_ID", values.get("hearingId"))
                .replaceAll("CASE_ID", values.get("caseId"))
                .replaceAll("COURT_CENTRE_ID", values.get("courtCentreId"))
                .replaceAll("COURT_ROOM_ID", values.get("courtRoomId"))
                .replaceAll("START_DATE", values.get("startDate"))
                .replaceAll("END_DATE", values.get("endDate"));

        if (values.get("updatedCourtCentreId") != null && values.get("updatedCourtRoomId") != null) {
            eventPayloadString = eventPayloadString.replaceAll("UPDATED_CENTRE_ID", values.get("updatedCourtCentreId"));
            eventPayloadString = eventPayloadString.replaceAll("UPDATED_ROOM_ID", values.get("updatedCourtRoomId"));
        }

        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }

    public void verifyAllocatedHearingFound(final String hearingId, final UUID courtCentreId, final UUID courtRoomId, final String searchDate, final Matcher[] matchers) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings"), courtCentreId, courtRoomId, searchDate, ALLOCATED));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers)));
    }

    public void verifyAllocatedHearingFoundByRangeSearch(final String hearingId, final UUID courtCentreId, final String searchDate, final Matcher[] matchers) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.search-date"),
                        ALLOCATED,
                        courtCentreId,
                        searchDate));

        final Filter idFilter = filter(where("id").is(hearingId));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers)));
    }

}
