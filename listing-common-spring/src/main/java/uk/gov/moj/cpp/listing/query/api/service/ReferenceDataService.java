package uk.gov.moj.cpp.listing.query.api.service;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reference-data lookups of the query API (ported from listing-query-api). Every call was
 * {@code requestAsAdmin}, so the caller's envelope only ever supplied metadata and is gone from the
 * signatures. A 404 used to surface as a failing {@code payloadAsJsonObject()} on the framework's
 * NULL payload (500); it is now an {@link IllegalStateException} (also 500), except for the
 * prosecutor lookup, whose callers checked {@code payloadIsNull()} and get an empty Optional.
 */
@SuppressWarnings({"squid:CallToDeprecatedMethod"})
@Component
@RequiredArgsConstructor
public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final String IS_WELSH = "isWelsh";
    private static final String HEARING_TYPES_ARRAY = "hearingTypes";
    private static final String HEARING_TYPE_ID = "id";
    private static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";
    private static final String WELSH_HEARING_DESCRIPTION = "welshHearingDescription";
    private static final String REFERENCEDATA_QUERY_PROSECUTOR = "referencedata.query.prosecutor";

    private final CppQueryClient cppQueryClient;

    public JsonObject getCourtCentreById(final UUID courtCentreId) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.info("'referencedata.query.courtroom' request with payload {}", payload);
        return required(REFERENCEDATA_QUERY_COURTROOM, payload);
    }

    public JsonObject getJudiciariesByIdList(final List<UUID> judiciaryIds) {
        final JsonObject payload = createObjectBuilder().add("ids", judiciaryIds.stream().map(UUID::toString).collect(Collectors.joining(","))).build();
        LOGGER.info("'referencedata.query.judiciaries' request with payload {}", payload);
        return required(REFERENCEDATA_QUERY_JUDICIARIES, payload);
    }

    public Optional<JsonObject> getProsecutorById(final String prosecutorId) {
        final JsonObject payload = createObjectBuilder().add("id", prosecutorId).build();
        LOGGER.info("'referencedata.query.prosecutor' request with payload {}", payload);
        return cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_PROSECUTOR, payload);
    }

    public Optional<Boolean> isHearingLanguageWelsh(final String courtCentreId) {
        final JsonObject jsonObject = getCourtCentreById(UUID.fromString(courtCentreId));
        final Boolean welsh = jsonObject.getBoolean(IS_WELSH, false);
        return Optional.ofNullable(welsh);
    }

    public Map<String, String> getHearingTypesIdWelshDescriptionMap() {
        LOGGER.info("'referencedata.query.hearing-types' request");
        final JsonObject jsonObject = required(REFERENCEDATA_QUERY_HEARING_TYPES, createObjectBuilder().build());
        final Map<String, String> hearingTypesMap = new HashMap<>();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("hearingTypes envelope response: {}", jsonObject);
        }
        final JsonArray hearingTypes = jsonObject.getJsonArray(HEARING_TYPES_ARRAY);
        hearingTypes.getValuesAs(JsonObject.class).forEach(hearingType ->
                hearingTypesMap.put(hearingType.getString(HEARING_TYPE_ID), hearingType.getString(WELSH_HEARING_DESCRIPTION)));
        return hearingTypesMap;
    }

    private JsonObject required(final String actionName, final JsonObject payload) {
        return cppQueryClient.requestAsAdmin(actionName, payload)
                .orElseThrow(() -> new IllegalStateException(actionName + " returned 404 for " + payload));
    }
}
