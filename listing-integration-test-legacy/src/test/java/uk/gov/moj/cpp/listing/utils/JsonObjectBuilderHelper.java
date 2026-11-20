package uk.gov.moj.cpp.listing.utils;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_COURT_CENTRE_ID;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_COURT_ROOM_ID;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_COURT_SCHEDULE_ID;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_DURATION;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_END_DATE;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_HEARING_LANGUAGE;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_HEARING_TYPE_DESCRIPTION;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_HEARING_TYPE_ID;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_IS_BENCH_CHAIRMAN;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_IS_DEPUTY;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_JUDICIAL_ID;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_JUDICIAL_ROLE_TYPE;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_JUDICIAL_ROLE_TYPE_ID;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_JUDICIARY;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_JUDICIARY_TYPE;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_JURISDICTION_TYPE;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_NON_DEFAULT_DAYS;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_NON_SITTING_DAYS;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_OUCODE;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_SEND_NOTIFICATION_TO_PARTIES;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_SESSION;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_START_DATE;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_START_TIME;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.FIELD_TYPE;

import uk.gov.moj.cpp.listing.steps.data.HearingTypeData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleTypeData;
import uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class JsonObjectBuilderHelper {

    public static final String FIELD_WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    public static final String FIELD_WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    public static final String FIELD_WEEK_COMMENCING_DURATION = "weekCommencingDurationInWeeks";
    public static final String HAS_VIDEO_LINK = "hasVideoLink";
    public static final String PUBLIC_LIST_NOTE = "publicListNote";

    public static String prepareJsonForUpdatedHearingData(final UpdatedHearingData updatedHearingData) {
        final JsonObjectBuilder builder = createObjectBuilder();

        builder.add(FIELD_TYPE, prepareJsonHearingType(updatedHearingData.getHearingTypData()))
                .add(FIELD_JURISDICTION_TYPE, updatedHearingData.getJurisdictionType())
                .add(FIELD_HEARING_LANGUAGE, updatedHearingData.getHearingLanguage())
                .add(FIELD_COURT_CENTRE_ID, updatedHearingData.getCourtCentreId().toString())
                .add(FIELD_NON_DEFAULT_DAYS, prepareJsonNonDefaultDays(updatedHearingData.getNonDefaultDays()))
                .add(FIELD_SEND_NOTIFICATION_TO_PARTIES, updatedHearingData.isSendNotificationToParties())
                .add(FIELD_JUDICIARY, prepareJsonJudiciary(updatedHearingData.getJudiciary()))
                .add(FIELD_NON_SITTING_DAYS, prepareJsonStringArray(updatedHearingData.getNonSittingDays()));

        addIfNotNull(builder, FIELD_END_DATE, updatedHearingData.getEndDate());
        addIfNotNull(builder, FIELD_START_DATE, updatedHearingData.getStartDate());
        addIfNotNull(builder, FIELD_COURT_ROOM_ID, updatedHearingData.getCourtRoomId());
        addIfNotNull(builder, FIELD_WEEK_COMMENCING_START_DATE, updatedHearingData.getWeekCommencingStartDate());
        addIfNotNull(builder, FIELD_WEEK_COMMENCING_END_DATE, updatedHearingData.getWeekCommencingEndDate());
        addIfNotNull(builder, FIELD_WEEK_COMMENCING_DURATION, updatedHearingData.getWeekCommencingDurationInWeeks());
        addIfNotNull(builder, HAS_VIDEO_LINK, updatedHearingData.getHasVideoLink());
        addIfNotNull(builder, PUBLIC_LIST_NOTE, updatedHearingData.getPublicListNote());

        return builder.build().toString();
    }

    public static void addIfNotNull(final JsonObjectBuilder builder, final String fieldName, final String value) {
        if (value != null) {
            builder.add(fieldName, value);
        }
    }

    public static void addIfNotNull(final JsonObjectBuilder builder, final String fieldName, final UUID value) {
        if (value != null) {
            builder.add(fieldName, value.toString());
        }
    }

    public static void addIfNotNull(final JsonObjectBuilder builder, final String fieldName, final Integer value) {
        if (value != null) {
            builder.add(fieldName, value);
        }
    }

    public static void addIfNotNull(final JsonObjectBuilder builder, final String fieldName, final Boolean value) {
        if (value != null) {
            builder.add(fieldName, value);
        }
    }

    public static void addNullableStringField(final JsonObjectBuilder builder, final String fieldName, final Optional<String> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        }
    }

    public static void addNullableIntegerField(final JsonObjectBuilder builder, final String fieldName, final Optional<Integer> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        }
    }

    public static void addNullableUUIDField(final JsonObjectBuilder builder, final String fieldName, final Optional<UUID> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get().toString());
        }
    }

    public static void addOptionalBooleanField(final JsonObjectBuilder builder, final String fieldName, final Optional<Boolean> value) {
        if (value.isPresent()) {
            builder.add(fieldName, value.get());
        }
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
                    JsonObjectBuilder nonDefaultDayBuilder = createObjectBuilder()
                            .add(FIELD_START_TIME, ndd.getStartTime());
                    addNullableIntegerField(nonDefaultDayBuilder, FIELD_DURATION, ndd.getDuration());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_COURT_SCHEDULE_ID, ndd.getCourtScheduleId());
                    addNullableIntegerField(nonDefaultDayBuilder, FIELD_COURT_ROOM_ID, ndd.getCourtRoomId());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_OUCODE, ndd.getOucode());
                    addNullableStringField(nonDefaultDayBuilder, FIELD_SESSION, ndd.getSession());

                    return nonDefaultDayBuilder;
                })
                .collect(JsonObjects::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private static JsonArray prepareJsonStringArray(final List<String> strings) {
        JsonArrayBuilder builder = JsonObjects.createArrayBuilder();
        if (strings != null && !strings.isEmpty()) {
            strings.forEach(builder::add);
        }
        return builder.build();

    }

    public static JsonArrayBuilder prepareJsonJudiciary(final List<JudicialRoleData> roleData) {
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
                    .collect(JsonObjects::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
        }
        return JsonObjects.createArrayBuilder();
    }

    private static JsonObjectBuilder prepareJudicialRoleType(final JudicialRoleTypeData judicialRoleType) {
        if (judicialRoleType != null) {
            JsonObjectBuilder builder = createObjectBuilder()
                    .add(FIELD_JUDICIARY_TYPE, judicialRoleType.getJudiciaryType());
            addNullableUUIDField(builder, FIELD_JUDICIAL_ROLE_TYPE_ID, judicialRoleType.getJudicialRoleTypeId());
            return builder;
        }
        return null;
    }
}
