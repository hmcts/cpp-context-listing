package uk.gov.moj.cpp.listing.command.service;


import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.HmiJudiciary;
import uk.gov.moj.cpp.listing.domain.HmiSession;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class StagingHmiQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingHmiQueryService.class);
    private static final String STAGINGHMI_QUERY_SESSIONS = "staginghmi.query.sessions";
    private static final String SESSION_START_DATE = "sessionStartDate";
    public static final String SESSION_END_DATE = "sessionEndDate";
    public static final String HEARING_TYPE_ID = "hearingTypeId";
    public static final String OU_CODE = "ouCode";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String PAGE_SIZE = "pageSize";
    public static final String COURT_ROOM_ID = "courtRoomId";
    public static final String COURT_HOUSE_ID = "courtHouseId";
    public static final String COURT_HOUSE_NAME = "courtHouseName";
    public static final String COURT_ROOM_NAME = "courtRoomName";
    public static final String COURT_ROOM_NUMBER = "courtRoomNumber";
    public static final String COURT_SCHEDULE_ID = "courtScheduleId";
    public static final String COURT_SESSION = "courtSession";
    public static final String DURATION = "duration";
    public static final String HEARING_TYPE = "hearingType";
    public static final String JUDICIARIES = "judiciaries";
    public static final String REMAINING_SLOT = "remainingSlot";
    public static final String REMAINING_TIME = "remainingTime";
    public static final String SESSION_DATE = "sessionDate";
    public static final String SESSION_START_TIME = "sessionStartTime";
    public static final String VENUE_ID = "venueId";



    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @SuppressWarnings("squid:S107")
    public List<HmiSession> getHmiSessions(final ZonedDateTime sessionStartDate, final ZonedDateTime sessionEndDate, final String typeId, final String ouCode, final int pageNumber, final int pageSize, final Optional<UUID> courtRoomId, final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format("staginghmi.query.sessions' request with parameters sessionStartDate: %s, sessinEndDate %s, panel: %s, oucode: %s, courtroomId: %s", sessionStartDate.toString(), sessionEndDate.toString(), typeId, ouCode, courtRoomId.toString()));
        }
        final JsonObject queryParameters = createObjectBuilder()
                .add(SESSION_START_DATE, sessionStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")))
                .add(SESSION_END_DATE, sessionEndDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")))
                .add(HEARING_TYPE_ID, typeId)
                .add(OU_CODE, ouCode)
                .add(PAGE_NUMBER, pageNumber)
                .add(PAGE_SIZE, pageSize)
                .add(COURT_ROOM_ID, courtRoomId.toString())
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataFrom(event.metadata())
                        .withName(STAGINGHMI_QUERY_SESSIONS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        final JsonEnvelope responseEnvelope = requester.requestAsAdmin(requestEnvelope);
        final JsonObject jsonObject = responseEnvelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("staginghmi.query.sessions found %s sessions", jsonObject.getJsonArray("hmiSessions").size()));
        }
        return jsonObject.getJsonArray("hmiSessions")
                .stream()
                .filter(x -> x.getValueType() == JsonValue.ValueType.OBJECT)
                .map(JsonObject.class::cast)
                .map(this::toHmiSession)
                .collect(toList());
    }

    private HmiSession toHmiSession(final JsonObject hmiSessionAsJson) {
        return HmiSession.hmiSession()
                .withCourtHouseId(hmiSessionAsJson.getString(COURT_HOUSE_ID, null))
                .withCourtHouseName(hmiSessionAsJson.getString(COURT_HOUSE_NAME, null))
                .withCourtRoomId(hmiSessionAsJson.getString(COURT_ROOM_ID, null))
                .withCourtRoomName(hmiSessionAsJson.getString(COURT_ROOM_NAME, null))
                .withCourtRoomNumber((hmiSessionAsJson.getInt(COURT_ROOM_NUMBER, 0)))
                .withCourtScheduleId(hmiSessionAsJson.getString(COURT_SCHEDULE_ID, null))
                .withCourtSession(hmiSessionAsJson.getString(COURT_SESSION, null))
                .withDuration(hmiSessionAsJson.getInt(DURATION, 0))
                .withHearingType(hmiSessionAsJson.getString(HEARING_TYPE, null))
                .withJudiciaries(hmiSessionAsJson.getJsonArray(JUDICIARIES).stream()
                        .filter(x -> x.getValueType() == JsonValue.ValueType.OBJECT)
                        .map(JsonObject.class::cast)
                        .map(this::toHmiJudiciary)
                        .collect(toList()))
                .withOuCode(hmiSessionAsJson.getString(OU_CODE, null))
                .withRemainingSlot(hmiSessionAsJson.getInt(REMAINING_SLOT, 0))
                .withRemainingTime(hmiSessionAsJson.getInt(REMAINING_TIME, 0))
                .withSessionDate(hmiSessionAsJson.getString(SESSION_DATE, null))
                .withSessionStartTime(LocalDateTime.parse(hmiSessionAsJson.getString(SESSION_START_TIME, null), DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneOffset.UTC))
                .withVenueId(hmiSessionAsJson.getString(VENUE_ID, null))
                .build();
    }

    private HmiJudiciary toHmiJudiciary(final JsonObject hmiJudiciaryAsJson) {
        return HmiJudiciary.hmiJudiciary()
                .withJudiciaryId(hmiJudiciaryAsJson.getString("judiciaryId"))
                .withJudiciaryType(hmiJudiciaryAsJson.getString("judiciaryType"))
                .build();
    }

    @VisibleForTesting
    void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}
