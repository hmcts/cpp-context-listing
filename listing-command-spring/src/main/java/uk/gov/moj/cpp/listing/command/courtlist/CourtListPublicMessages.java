package uk.gov.moj.cpp.listing.command.courtlist;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.listing.courts.events.CourtLists;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.moj.cpp.listing.command.support.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ZonedDateTimes;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;
import uk.gov.moj.cpp.listing.xhibit.courtlist.PublishCourtListRequestParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The public messages of the court-list export (MBD refactor P2.3.1, ADR 015): the public half of
 * the JEE processor's {@code PublishCourtListCommandSender} ({@code publishPublicMessageWithDailyList},
 * {@code publishPublicMessageForCourtList}), ported with the same names, payloads and conditions.
 * The internal commands it sent ({@code store-published-court-list}, {@code court-list-request-export},
 * {@code record-court-list-export-*}) are direct calls to {@link CourtListCommands}.
 *
 * <p>Returns the messages; the caller writes them to the outbox with the cause's metadata.
 */
@Component
public class CourtListPublicMessages {

    public static final String PUBLIC_COURT_DAILY_LIST = "public.listing.court-daily-list";
    public static final String PUBLIC_COURT_LIST_PUBLISHED = "public.listing.court-list-published";

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListPublicMessages.class);

    private static final String DEFENDANTS = "defendants";
    private static final String HEARINGS = "hearings";
    private static final String APPLICATION_REFERENCE = "applicationReference";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String PUBLISH_COURT_LIST_TYPE = "publishCourtListType";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String DAILY_LIST_XML = "dailyListDocument";
    private static final String COURT_LIST_ID = "courtListId";
    private static final String REQUESTED_TIME = "requestedTime";
    private static final String WEEK_COMMENCING = "weekCommencing";
    private static final String COURT_LISTS = "courtLists";
    private static final String SITTINGS = "sittings";
    private static final String SEND_NOTIFICATION_TO_PARTIES = "sendNotificationToParties";

    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    private final HearingObjectsListingToCoreConverter hearingListingToCoreConverter;
    private final ProgressionService progressionService;

    public CourtListPublicMessages(final HearingObjectsListingToCoreConverter hearingListingToCoreConverter,
                                   final ProgressionService progressionService) {
        this.hearingListingToCoreConverter = hearingListingToCoreConverter;
        this.progressionService = progressionService;
    }

    /**
     * {@code public.listing.court-daily-list}: the validated XML, XML-escaped, for {@code DRAFT} and
     * {@code FINAL} lists only (staging-DARTS); nothing for warned and firm lists.
     */
    public Optional<DomainEvent> courtDailyList(final PublishCourtListRequestParameters requestParameters, final String dailyListXml) {
        final PublishCourtListType publishCourtListType = requestParameters.getPublishCourtListType();
        if (publishCourtListType != PublishCourtListType.DRAFT && publishCourtListType != PublishCourtListType.FINAL) {
            return Optional.empty();
        }
        final JsonObject payload = Json.createObjectBuilder()
                .add(COURT_LIST_ID, requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(DAILY_LIST_XML, StringEscapeUtils.escapeXml10(dailyListXml))
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .build();
        return Optional.of(new DomainEvent(PUBLIC_COURT_DAILY_LIST, requestParameters.getCourtListId(), payload));
    }

    /**
     * {@code public.listing.court-list-published}: the sittings of the court list in the public
     * {@code CourtLists} shape (round-tripped through the generated POJO, as before), with listing
     * defendants converted to core defendants and their case ids resolved from Progression by case
     * URN where missing. Nothing when there is no sitting with hearings.
     *
     * @param userId the user the JEE processor queried Progression as (the export command's user)
     */
    public Optional<DomainEvent> courtListPublished(final PublishCourtListRequestParameters requestParameters, final JsonObject courtListJson,
                                                    final UUID userId) {

        final JsonArray courtListArray = prepareCourtLists(courtListJson, userId);

        if (isEmpty(courtListArray)) {
            LOGGER.info("There are no courtList elements are present for courtListId {}", requestParameters.getCourtListId());
            return Optional.empty();
        }
        final List<CourtLists> courtListsList = courtListArray.getValuesAs(JsonObject.class).stream()
                .map(courtListItem -> jsonObjectConverter.convert(courtListItem, CourtLists.class))
                .filter(courtLists -> isNotEmpty(courtLists.getHearings()))
                .toList();
        if (isEmpty(courtListsList)) {
            LOGGER.info("There are not sitting/hearing to be published for courtListId {}", requestParameters.getCourtListId());
            return Optional.empty();
        }
        final JsonArrayBuilder finalCourtListArrayBuilder = Json.createArrayBuilder();
        courtListsList.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(finalCourtListArrayBuilder::add);

        // JsonObjectBuilder.add(String, boolean): a null sendNotificationToParties fails here, as it
        // did in the JEE processor (the publish flow always sets it; a REST export may not).
        final boolean sendNotificationToParties = requestParameters.getSendNotificationToParties();
        final JsonObject payload = Json.createObjectBuilder()
                .add(COURT_LIST_ID, requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(END_DATE, requestParameters.getEndDate().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(REQUESTED_TIME, ZonedDateTimes.toString(requestParameters.getRequestedTime()))
                .add(WEEK_COMMENCING, requestParameters.getPublishCourtListType().isWeekCommencing())
                .add(COURT_LISTS, finalCourtListArrayBuilder.build())
                .add(SEND_NOTIFICATION_TO_PARTIES, sendNotificationToParties)
                .build();
        return Optional.of(new DomainEvent(PUBLIC_COURT_LIST_PUBLISHED, requestParameters.getCourtListId(), payload));
    }

    private JsonArray prepareCourtLists(final JsonObject courtListJson, final UUID userId) {
        final JsonArrayBuilder courtListJsonArray = Json.createArrayBuilder();
        courtListJson.getJsonArray(COURT_LISTS).getValuesAs(JsonObject.class).stream()
                .filter(courtList -> courtList.containsKey(SITTINGS))
                .flatMap(courtList -> courtList.getJsonArray(SITTINGS).getValuesAs(JsonObject.class).stream())
                .forEach(courtListJsonArray::add);
        return (JsonArray) mapListingDefendantToCore(courtListJsonArray.build(), new HashMap<>(), userId);
    }

    private JsonStructure mapListingDefendantToCore(final JsonStructure json, final Map<UUID, UUID> caseIdByDefendantId, final UUID userId) {
        if (json.getValueType().equals(OBJECT)) {
            return getJsonObjectIfValueTypeIsObject((JsonObject) json, caseIdByDefendantId, userId);
        } else if (json.getValueType().equals(ARRAY)) {
            return getJsonValuesWhenValueTypeIsArray((JsonArray) json, caseIdByDefendantId, userId);
        }
        return null;
    }

    private JsonArray getJsonValuesWhenValueTypeIsArray(final JsonArray json, final Map<UUID, UUID> caseIdByDefendantId, final UUID userId) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        json.forEach(value -> {
            switch (value.getValueType()) {
                case OBJECT -> builder.add(mapListingDefendantToCore((JsonObject) value, caseIdByDefendantId, userId));
                case ARRAY -> builder.add(mapListingDefendantToCore((JsonArray) value, caseIdByDefendantId, userId));
                default -> builder.add(value);
            }
        });
        return builder.build();
    }

    private JsonObject getJsonObjectIfValueTypeIsObject(final JsonObject json, final Map<UUID, UUID> caseIdByDefendantId, final UUID userId) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : json.entrySet()) {
            final String key = entry.getKey();
            final JsonValue value = entry.getValue();
            final JsonValue.ValueType valueType = value.getValueType();
            if (OBJECT.equals(valueType)) {
                builder.add(key, mapListingDefendantToCore((JsonObject) value, caseIdByDefendantId, userId));
            } else if (ARRAY.equals(valueType)) {
                if (DEFENDANTS.equals(key)) {
                    // Court application hearings are published without defendants - their parties
                    // (applicant/subject/respondents) are not case defendants
                    if (!json.containsKey(APPLICATION_REFERENCE)) {
                        builder.add(key, mapListingDefendantToCoreDefendant((JsonArray) value, caseIdByDefendantId));
                    }
                } else if (HEARINGS.equals(key)) {
                    getCaseIdForEachHearingAndMapByDefendantsInHearing((JsonArray) value, caseIdByDefendantId, userId);
                    builder.add(key, mapListingDefendantToCore((JsonArray) value, caseIdByDefendantId, userId));
                } else {
                    builder.add(key, mapListingDefendantToCore((JsonArray) value, caseIdByDefendantId, userId));
                }
            } else {
                builder.add(key, value);
            }
        }
        return builder.build();
    }

    private void getCaseIdForEachHearingAndMapByDefendantsInHearing(final JsonArray hearings, final Map<UUID, UUID> caseIdByDefendantId, final UUID userId) {
        hearings.getValuesAs(JsonObject.class).forEach(hearing -> {
            if (!hearing.containsKey("caseIdentifier")) {
                LOGGER.warn("Hearing does not contain caseIdentifier: {}", hearing);
                return;
            }
            final String caseUrn = hearing.getJsonObject("caseIdentifier").getString("caseReference");
            final Optional<JsonObject> caseIdJsonObject = progressionService.caseExistsByCaseUrn(userId, caseUrn);
            if (caseIdJsonObject.isPresent()) {
                final UUID caseId = fromString(caseIdJsonObject.get().getString("caseId"));
                hearing.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).forEach(defendant ->
                        caseIdByDefendantId.put(fromString(defendant.getString("id")), caseId));
            }
        });
    }

    private JsonArrayBuilder mapListingDefendantToCoreDefendant(final JsonArray defendants, final Map<UUID, UUID> caseIdByDefendantId) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        defendants.getValuesAs(JsonObject.class).stream()
                .map(defendant -> hearingListingToCoreConverter.convert(jsonObjectConverter.convert(defendant, Defendant.class),
                        resolveProsecutionCaseId(defendant, caseIdByDefendantId)))
                .map(objectToJsonObjectConverter::convert)
                .forEach(builder::add);
        return builder;
    }

    private static UUID resolveProsecutionCaseId(final JsonObject defendant, final Map<UUID, UUID> caseIdByDefendantId) {
        if (defendant.containsKey("prosecutionCaseId")) {
            return fromString(defendant.getString("prosecutionCaseId"));
        }
        return caseIdByDefendantId.get(fromString(defendant.getString("id")));
    }
}
