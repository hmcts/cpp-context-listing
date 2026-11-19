package uk.gov.moj.cpp.listing.command.hearing.api.service;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The command API's reference-data queries (MBD refactor P2.3.2; port of the JEE command-api
 * {@code ReferenceDataService}). The framework {@code Requester} becomes {@link CppQueryClient}:
 * {@code requestAsAdmin} stays as admin (system user), {@code request} sends the caller's user
 * (D19). The answer is returned as an envelope named after the query with the response payload
 * (an empty object where the framework gave a {@code JsonValue.NULL} payload, i.e. a 404).
 */
@Component("hearingApiReferenceDataService")
public class ReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_SINGLE_COURTROOM = "referencedata.query.courtroom";
    private static final String REFERENCEDATA_QUERY_MULTI_COURTROOMS = "referencedata.query.courtrooms";
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNIT = "referencedata.query.organisation-unit";
    private static final String REFERENCEDATA_QUERY_ALL_HEARING_TYPES = "referencedata.query.all-hearing-types";
    private static final String REFERENCE_DATA_GET_PROSECUTOR_BY_ID = "referencedata.query.prosecutor";

    private final CppQueryClient cppQueryClient;
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    public ReferenceDataService(final CppQueryClient cppQueryClient) {
        this.cppQueryClient = cppQueryClient;
    }

    public Envelope getCourtCentresById(final Set<UUID> courtCentreIdList, final Envelope event) {
        final String courtCenterIdsStr = courtCentreIdList.stream().map(UUID::toString).collect(Collectors.joining(","));
        final JsonObject payload = createObjectBuilder().add("courtCentreId", courtCenterIdsStr).build();
        LOGGER.info("'referencedata.query.courtrooms' request with payload {}", payload);
        return answer(event, REFERENCEDATA_QUERY_MULTI_COURTROOMS, cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_MULTI_COURTROOMS, payload).orElse(null));
    }

    public Envelope getCourtCentreById(final UUID courtCentreId, final Envelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        LOGGER.info("'referencedata.query.courtroom' request with payload {}", payload);
        return answer(event, REFERENCEDATA_QUERY_SINGLE_COURTROOM, cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_SINGLE_COURTROOM, payload).orElse(null));
    }

    public OrganisationUnit getOrganizationUnitById(final UUID courtCentreId, final Envelope event) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        final JsonObject response = cppQueryClient.request(REFERENCEDATA_QUERY_ORGANISATION_UNIT, payload, userId(event)).orElse(JsonValue.EMPTY_JSON_OBJECT);
        LOGGER.debug("'referencedata.query.organisation-unit' response with payload {}", response);
        return jsonObjectConverter.convert(response, OrganisationUnit.class);
    }

    public Envelope getHearingTypes(final Envelope event) {
        LOGGER.info("'referencedata.query.hearing-types' request");
        return answer(event, REFERENCEDATA_QUERY_ALL_HEARING_TYPES,
                cppQueryClient.requestAsAdmin(REFERENCEDATA_QUERY_ALL_HEARING_TYPES, createObjectBuilder().build()).orElse(null));
    }

    public boolean getPoliceFlagForProsecutorId(final Envelope jsonEnvelope, final String prosecutorId) {
        final JsonObject ouCodeQueryParameter = createObjectBuilder().add("id", prosecutorId).build();
        LOGGER.info("'referencedata.query.prosecutor' request with prosecutorId {}", prosecutorId);
        final JsonObject prosecutorResult = cppQueryClient.requestAsAdmin(REFERENCE_DATA_GET_PROSECUTOR_BY_ID, ouCodeQueryParameter).orElse(null);
        if (nonNull(prosecutorResult) && prosecutorResult.containsKey("policeFlag")) {
            return prosecutorResult.getBoolean("policeFlag");
        }
        return false;
    }

    private static UUID userId(final Envelope event) {
        return event.metadata().userId().map(UUID::fromString).orElse(null);
    }

    private static Envelope answer(final Envelope event, final String name, final JsonObject payload) {
        return new Envelope(Metadata.from(event.metadata()).withName(name).build(), payload == null ? JsonValue.EMPTY_JSON_OBJECT : payload);
    }
}
