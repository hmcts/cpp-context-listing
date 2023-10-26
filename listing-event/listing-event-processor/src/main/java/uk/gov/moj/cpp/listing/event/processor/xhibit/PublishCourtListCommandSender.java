package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.ValueType.ARRAY;
import static javax.json.JsonValue.ValueType.OBJECT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.util.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringEscapeUtils;

@Stateless
public class PublishCourtListCommandSender {

    public static final String PUBLISH_COURT_LIST_REQUEST_ID = "publishCourtListRequestId";
    public static final String DEFENDANTS = "defendants";
    public static final String HEARINGS = "hearings";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String RECORD_COURT_LIST_EXPORT_SUCCESSFUL = "listing.command.record-court-list-export-successful";
    private static final String RECORD_COURT_LIST_EXPORT_FAILED = "listing.command.record-court-list-export-failed";
    private static final String STORE_PUBLISHED_COURT_LIST = "listing.command.store-published-court-list";
    private static final String COURT_LIST_REQUEST_EXPORT = "listing.command.court-list-request-export";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String PUBLISH_COURT_LIST_TYPE = "publishCourtListType";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String COURT_LIST_FILE_NAME = "courtListFileName";
    private static final String DAILY_LIST_XML = "dailyListDocument";
    private static final String COURT_LIST_JSON = "courtListJson";
    private static final String COURT_LIST_ID = "courtListId";
    private static final String REQUESTED_TIME = "requestedTime";
    private static final String WEEK_COMMENCING = "weekCommencing";
    private static final String COURT_LISTS = "courtLists";
    private static final String SITTINGS = "sittings";
    private static final String SEND_NOTIFICATION_TO_PARTIES = "sendNotificationToParties";

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    private Sender sender;

    @Inject
    private UtcClock utcClock;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingObjectsListingToCoreConverter hearingListingToCoreConverter;

    @Inject
    private ProgressionService progressionService;


    public void publishPublicMessageWithDailyList(final JsonEnvelope envelope, final PublishCourtListRequestParameters requestParameters, final String dailyListXml) {
        final PublishCourtListType publishCourtListType = requestParameters.getPublishCourtListType();
        if (publishCourtListType == PublishCourtListType.DRAFT || publishCourtListType == PublishCourtListType.FINAL) {
            final JsonObject payload = createObjectBuilder()
                    .add(COURT_LIST_ID, requestParameters.getCourtListId().toString())
                    .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                    .add(DAILY_LIST_XML, StringEscapeUtils.escapeXml10(dailyListXml))
                    .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                    .build();
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.court-daily-list"),
                    payload));
        }
    }

    public void recordCourtListExportSuccessful(final PublishCourtListRequestParameters requestParameters, final String courtListFileName) {

        final JsonObject payload = createObjectBuilder()
                .add(COURT_LIST_ID, requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(END_DATE, requestParameters.getEndDate().toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add("exportedTime", ZonedDateTimes.toString(utcClock.now()))
                .build();

        sendCommandWith(RECORD_COURT_LIST_EXPORT_SUCCESSFUL, requestParameters.getCourtListId(), payload);
    }

    public void recordCourtListExportFailed(final PublishCourtListRequestParameters requestParameters,
                                            final String errorMessage,
                                            final String courtListFileName) {

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add(COURT_LIST_ID, requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(END_DATE, requestParameters.getEndDate().toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add("failedTime", ZonedDateTimes.toString(utcClock.now()))
                .add(ERROR_MESSAGE, errorMessage);

        sendCommandWith(RECORD_COURT_LIST_EXPORT_FAILED, requestParameters.getCourtListId(), objectBuilder.build());
    }

    public void storePublishedCourtList(final PublishCourtListRequestParameters requestParameters, final JsonObject courtListJson) {

        final JsonObject payload = createObjectBuilder()
                .add(PUBLISH_COURT_LIST_REQUEST_ID, requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(COURT_LIST_JSON, courtListJson.toString())
                .build();

        sendCommandWith(STORE_PUBLISHED_COURT_LIST, requestParameters.getCourtListId(), payload);
    }

    public void requestExportCourtList(final PublishCourtListRequestParameters requestParameters, final JsonObject courtListJson, final JsonEnvelope envelope) {

        final JsonObject payload = createObjectBuilder()
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(COURT_LIST_JSON, courtListJson.toString())
                .add(SEND_NOTIFICATION_TO_PARTIES, Optional.ofNullable(requestParameters.getSendNotificationToParties()).orElse(false))
                .build();

        sendCommandWithUser(COURT_LIST_REQUEST_EXPORT, requestParameters.getCourtListId(), payload, envelope.metadata().userId());
    }

    public void publishPublicMessageForCourtList(final JsonEnvelope envelope, final PublishCourtListRequestParameters requestParameters, final JsonObject courtListJson) {
        final JsonObject payload = createObjectBuilder()
                .add(COURT_LIST_ID, requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(END_DATE, requestParameters.getEndDate().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(REQUESTED_TIME, ZonedDateTimes.toString(requestParameters.getRequestedTime()))
                .add(WEEK_COMMENCING, requestParameters.getPublishCourtListType().isWeekCommencing())
                .add(COURT_LISTS, prepareCourtLists(courtListJson, envelope))
                .add(SEND_NOTIFICATION_TO_PARTIES, requestParameters.getSendNotificationToParties())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.court-list-published"), payload));
    }

    private JsonArray prepareCourtLists(final JsonObject courtListJson, final JsonEnvelope envelope) {
        final JsonArrayBuilder courtListJsonArray = courtListJson.getJsonArray(COURT_LISTS).getValuesAs(JsonObject.class).stream()
                .flatMap(courtList -> courtList.getJsonArray(SITTINGS).getValuesAs(JsonObject.class).stream())
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
        return (JsonArray) mapListingDefendantToCore(courtListJsonArray.build(), new HashMap<>(), envelope);
    }

    private JsonStructure mapListingDefendantToCore(final JsonStructure json, final Map<UUID, UUID> caseIdByDefendantId, final JsonEnvelope envelope) {
        if (json.getValueType().equals(OBJECT)) {
            return getJsonObjectIfValueTypeIsObject((JsonObject) json, caseIdByDefendantId, envelope);
        } else if (json.getValueType().equals(ARRAY)) {
            return getJsonValuesWhenValueTypeIsArray((JsonArray) json, caseIdByDefendantId, envelope);
        }
        return null;
    }

    private JsonArray getJsonValuesWhenValueTypeIsArray(final JsonArray json, final Map<UUID, UUID> caseIdByDefendantId, final JsonEnvelope envelope) {
        final JsonArrayBuilder builder = createArrayBuilder();
        json.forEach(value -> {
            switch (value.getValueType()) {
                case OBJECT:
                    builder.add(mapListingDefendantToCore((JsonObject) value, caseIdByDefendantId, envelope));
                    break;
                case ARRAY:
                    builder.add(mapListingDefendantToCore((JsonArray) value, caseIdByDefendantId, envelope));
                    break;
                default:
                    builder.add(value);
                    break;
            }
        });
        return builder.build();
    }

    private JsonObject getJsonObjectIfValueTypeIsObject(final JsonObject json, final Map<UUID, UUID> caseIdByDefendantId, final JsonEnvelope envelope) {
        final JsonObjectBuilder builder = createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : json.entrySet()) {
            final String key = entry.getKey();
            final JsonValue value = entry.getValue();
            final JsonValue.ValueType valueType = value.getValueType();
            if (OBJECT.equals(valueType)) {
                builder.add(key, mapListingDefendantToCore((JsonObject) value, caseIdByDefendantId, envelope));
            } else if (ARRAY.equals(valueType)) {
                if (DEFENDANTS.equals(key)) {
                    final JsonArrayBuilder defendants = mapListingDefendantToCoreDefendant((JsonArray) value, caseIdByDefendantId);
                    builder.add(key, defendants);
                } else if (HEARINGS.equals(key)) {
                    getCaseIdForEachHearingAndMapByDefendantsInHearing((JsonArray) value, caseIdByDefendantId, envelope);
                    builder.add(key, mapListingDefendantToCore((JsonArray) value, caseIdByDefendantId, envelope));
                } else {
                    builder.add(key, mapListingDefendantToCore((JsonArray) value, caseIdByDefendantId, envelope));
                }
            } else {
                builder.add(key, value);
            }
        }
        return builder.build();
    }

    private void getCaseIdForEachHearingAndMapByDefendantsInHearing(final JsonArray hearings, final Map<UUID, UUID> caseIdByDefendantId, final JsonEnvelope envelope) {
        hearings.getValuesAs(JsonObject.class).forEach(hearing -> {
            final String caseUrn = hearing.getJsonObject("caseIdentifier").getString("caseReference");
            final Optional<JsonObject> caseIdJsonObject = progressionService.caseExistsByCaseUrn(envelope, caseUrn);
            if (caseIdJsonObject.isPresent()) {
                final UUID caseId = fromString(caseIdJsonObject.get().getString("caseId"));
                hearing.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).forEach(defendant ->
                        caseIdByDefendantId.put(fromString(defendant.getString("id")), caseId));
            }
        });
    }

    private JsonArrayBuilder mapListingDefendantToCoreDefendant(final JsonArray defendants, final Map<UUID, UUID> caseIdByDefendantId) {
        return defendants.getValuesAs(JsonObject.class).stream()
                .map(defendant -> hearingListingToCoreConverter.convert(jsonObjectConverter.convert(defendant, Defendant.class),
                        caseIdByDefendantId.get(fromString(defendant.getString("id")))))
                .map(defendant -> objectToJsonObjectConverter.convert(defendant))
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
    }

    private void sendCommandWith(final String commandName, final UUID streamId, final JsonObject payload) {

        sender.sendAsAdmin(envelopeFrom(
                metadataBuilder()
                        .withStreamId(streamId)
                        .createdAt(utcClock.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                payload));
    }

    private void sendCommandWithUser(final String commandName, final UUID streamId, final JsonObject payload, final Optional<String> userId) {
        final MetadataBuilder builder = metadataBuilder();
        if (userId.isPresent()) {
            builder.withUserId(userId.get());
        }

        sender.send(envelopeFrom(
                builder.withStreamId(streamId)
                        .createdAt(utcClock.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                payload));
    }
}
