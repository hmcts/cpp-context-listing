package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.repository.CacheRefDataCourtroomRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.deltaspike.jpa.api.transaction.Transactional;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
@ApplicationScoped
public class CacheRefDataCourtroomLoader {

    private static final String REFERENCE_DATA_QUERY_COURTROOMS = "referencedata.query.courtrooms";
    public static final String ORGANISATION_UNITS = "organisationunits";
    public static final String COURTROOMS = "courtrooms";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CacheRefDataCourtroomRepository cacheRefdataCourtroomRepository;

    @Transactional
    public int loadCourtRooms() {
        final Set<CacheRefDataCourtroom> allFromRefData = loadDataRoomRefDataService();
        if (allFromRefData.isEmpty()) {
            return 0;
        }

        cacheRefdataCourtroomRepository.deleteAll();
        saveAll(allFromRefData);
        return allFromRefData.size();
    }

    @Transactional
    public void addCourtRoom(JsonEnvelope envelope) {
        final CacheRefDataCourtroom event = jsonObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                CacheRefDataCourtroom.class);

        final CacheRefDataCourtroom entity = new CacheRefDataCourtroom();
        entity.setId(event.getId());
        entity.setCourtroomName(event.getCourtroomName());

        cacheRefdataCourtroomRepository.save(entity);
    }

    private Set<CacheRefDataCourtroom> loadDataRoomRefDataService() {
        final JsonObject queryParameters = createObjectBuilder().build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCE_DATA_QUERY_COURTROOMS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        if (Objects.isNull(response) || Objects.isNull(response.payload()) ||
                !response.payload().containsKey(ORGANISATION_UNITS)) {
            return Collections.emptySet();
        }

        final JsonArray organizations = response.payload().getJsonArray(ORGANISATION_UNITS);

        final Set<CacheRefDataCourtroom> result = organizations.stream().
                filter(orgs -> orgs.asJsonObject().containsKey(COURTROOMS)).
                map(orgs -> orgs.asJsonObject().getJsonArray(COURTROOMS)).
                flatMap(Collection::stream).
                map(room -> new CacheRefDataCourtroom(UUID.fromString(room.asJsonObject().getString("id")), room.asJsonObject().getString("courtroomName"))).
                collect(Collectors.toSet());

        return result;
    }

    void saveAll(Collection<CacheRefDataCourtroom> items) {
        items.forEach(b -> cacheRefdataCourtroomRepository.save(b));
    }

    @Transactional
    public void closeCourtRoom(JsonEnvelope envelope) {
        final CacheRefDataCourtroom event = jsonObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                CacheRefDataCourtroom.class);
        CacheRefDataCourtroom entity = cacheRefdataCourtroomRepository.findBy(event.getId());
        cacheRefdataCourtroomRepository.remove(entity);
    }
}