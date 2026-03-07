package uk.gov.moj.cpp.listing.common.courtcentre;

import static java.util.UUID.fromString;

import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.courts.Courtrooms;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtCentreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtCentreFactory.class);
    private static final String KEY_NOT_FOUND_FOR_COURT_CENTRE_ID = " not found for courtCentreId:";

    @Inject
    private ReferenceDataService referenceDataService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public Map<UUID, CourtCentreDetails> getCourtCentreDetailsById(Set<UUID> courtCenterIdList, JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentresById(courtCenterIdList, envelope);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("courtCentreEnvelope for getCourtCentresById list response: {}", courtCentreEnvelope.toObfuscatedDebugString());
        }
        final Map<UUID, CourtCentreDetails> courtCentreDetailsMap = new HashMap<>();
        final JsonArray respJsonArray = courtCentreEnvelope.payloadAsJsonObject().getJsonArray("organisationunits");
        respJsonArray.forEach(e -> {
            final JsonObject respJsonObj = (JsonObject) e;
            final CourtCentreDetails courtCentreDetails = createCourtCentreDetailsOf(fromString(respJsonObj.getString("id")), respJsonObj);
            courtCentreDetailsMap.put(courtCentreDetails.getId(), courtCentreDetails);
        });

        return courtCentreDetailsMap;
    }

    public CourtCentreDetails getCourtCentre(UUID courtCentreId, JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentreById(courtCentreId, envelope);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("courtCentreEnvelope for getCourtCentreById response: {}", courtCentreEnvelope.toObfuscatedDebugString());
        }
        final JsonObject respJsonObj = courtCentreEnvelope.payloadAsJsonObject();
        return createCourtCentreDetailsOf(courtCentreId, respJsonObj);
    }

    public JsonObject getOrganisationUnit(final UUID courtCentreId, final JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentreById(courtCentreId, envelope);
        final JsonObject jsonObject = courtCentreEnvelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("courtCentreEnvelope response: {}", courtCentreEnvelope.toObfuscatedDebugString());
        }
        return jsonObject;
    }

    public Optional<Integer> getCourtRoomNumber(final JsonObject courtCentreObj, final String s) {
        return courtCentreObj.getJsonArray("courtrooms")
                .getValuesAs(JsonObject.class).stream()
                .filter(courtRoom -> s.equals(courtRoom.getString("id")))
                .map(courtRoom -> courtRoom.getInt("courtroomId"))
                .map(Optional::of)
                .findFirst().orElse(Optional.empty());
    }

    public boolean getPoliceFlagForProsecutorId(final JsonEnvelope jsonEnvelope, final String prosecutorId) {
        return referenceDataService.getPoliceFlagForProsecutorId(jsonEnvelope, prosecutorId);
    }

    private CourtCentreDetails createCourtCentreDetailsOf(UUID courtCentreId, JsonObject jsonObject) {
        // Required fields based on schema
        final String defaultStartTimeStr = getRequiredJsonObjectString("defaultStartTime", jsonObject, courtCentreId);
        final LocalTime defaultStartTime = LocalTime.parse(defaultStartTimeStr);
        final Integer defaultDurationMinutes = DateAndTimeUtils.convertHoursAndMinutesToMinutes(getRequiredJsonObjectString("defaultDurationHrs", jsonObject, courtCentreId)).orElse(0);

        // Optional fields based on schema
        final JsonArray courtrooms = getOptionalJsonObjectArray("courtrooms", jsonObject);
        final String oucode = getOptionalJsonObjectString("oucode", jsonObject);
        final String lja = getOptionalJsonObjectString("lja", jsonObject);
        final String oucodeL1Code = getOptionalJsonObjectString("oucodeL1Code", jsonObject);
        final String courtId = getOptionalJsonObjectString("courtId", jsonObject);
        final String oucodeL1Name = getOptionalJsonObjectString("oucodeL1Name", jsonObject);
        final String name = getOptionalJsonObjectString("oucodeL3Name", jsonObject);
        final String oucodeL3WelshName = getOptionalJsonObjectString("oucodeL3WelshName", jsonObject);
        final String address1 = getOptionalJsonObjectString("address1", jsonObject);
        final String address2 = getOptionalJsonObjectString("address2", jsonObject);
        final String postcode = getOptionalJsonObjectString("postcode", jsonObject);
        final Boolean isWelsh = getOptionalJsonObjectBoolean("isWelsh", jsonObject);
        final String oucodeL2Code = getOptionalJsonObjectString("oucodeL2Code", jsonObject);
        final String region = getOptionalJsonObjectString("region", jsonObject);

        // Convert courtrooms JsonArray to List<Courtrooms>
        final List<Courtrooms> courtroomsList = courtrooms.getValuesAs(JsonObject.class).stream()
                .map(c -> jsonObjectToObjectConverter.convert(c, Courtrooms.class))
                .toList();

        return CourtCentreDetails.courtCentreDetails()
                .withId(courtCentreId)
                .withOucode(oucode)
                .withDefaultStartTime(defaultStartTime)
                .withDefaultDuration(defaultDurationMinutes)
                .withCourtrooms(courtroomsList)
                .withLja(lja)
                .withOucodeL1Code(oucodeL1Code)
                .withCourtId(courtId)
                .withOucodeL1Name(oucodeL1Name)
                .withOucodeL3Name(name)
                .withOucodeL3WelshName(oucodeL3WelshName)
                .withAddress1(address1)
                .withAddress2(address2)
                .withPostcode(postcode)
                .withOucodeL2Code(oucodeL2Code)
                .withName(name)
                .withRegion(region)
                .withIsWelsh(isWelsh)
                .build();
    }

    /**
     * Gets a required string value from JsonObject. Throws IllegalArgumentException if null or
     * empty.
     */
    private String getRequiredJsonObjectString(String searchString, JsonObject jsonObject, UUID courtCentreId) {
        final String jsonObjectStringValue = jsonObject.getString(searchString, null);
        if (jsonObjectStringValue == null || jsonObjectStringValue.isEmpty()) {
            throw new IllegalArgumentException(searchString + KEY_NOT_FOUND_FOR_COURT_CENTRE_ID + courtCentreId);
        }
        return jsonObjectStringValue;
    }

    /**
     * Gets an optional string value from JsonObject. Returns null if not present or empty.
     */
    private String getOptionalJsonObjectString(String searchString, JsonObject jsonObject) {
        final String jsonObjectStringValue = jsonObject.getString(searchString, null);
        return (jsonObjectStringValue == null || jsonObjectStringValue.isEmpty()) ? null : jsonObjectStringValue;
    }

    /**
     * Gets an optional boolean value from JsonObject. Returns null if not present.
     */
    private Boolean getOptionalJsonObjectBoolean(String searchString, JsonObject jsonObject) {
        try {
            if (jsonObject.containsKey(searchString) && !jsonObject.isNull(searchString)) {
                return jsonObject.getBoolean(searchString);
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return false;
    }

    /**
     * Gets a required JsonArray from JsonObject. Throws IllegalArgumentException if not present.
     */
    private JsonArray getRequiredJsonObjectArray(String searchString, JsonObject jsonObject, UUID courtCentreId) {
        final JsonArray jsonArray = jsonObject.getJsonArray(searchString);
        if (jsonArray == null) {
            throw new IllegalArgumentException(searchString + KEY_NOT_FOUND_FOR_COURT_CENTRE_ID + courtCentreId);
        }
        return jsonArray;
    }

    /**
     * Gets an optional JsonArray from JsonObject. Returns null if not present.
     */
    private JsonArray getOptionalJsonObjectArray(String searchString, JsonObject jsonObject) {
        return jsonObject.getJsonArray(searchString);
    }

    /**
     * Backward compatibility method - treats all fields as required.
     */
    private String getJsonObjectString(String searchString, JsonObject jsonObject, UUID courtCentreId) {
        return getRequiredJsonObjectString(searchString, jsonObject, courtCentreId);
    }
}
