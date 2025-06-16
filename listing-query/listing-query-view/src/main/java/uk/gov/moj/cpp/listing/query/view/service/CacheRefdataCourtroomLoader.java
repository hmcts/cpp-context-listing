package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.CacheRefdataCourtroom;
import uk.gov.moj.cpp.listing.persistence.repository.CacheRefdataCourtroomRepository;

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
public class CacheRefdataCourtroomLoader {

    private static final String REFERENCEDATA_QUERY_COURTROOMS = "referencedata.query.courtrooms";
    public static final String ORGANISATIONUNITS = "organisationunits";
    public static final String COURTROOMS = "courtrooms";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private CacheRefdataCourtroomRepository repos;

    @Transactional
    public int loadCourtRooms() {
        final Set<CacheRefdataCourtroom> allFromRefData = loadDataromRefDataService();
        if (allFromRefData.isEmpty()) {
            return 0;
        }

        repos.deleteAll();
        saveAll(allFromRefData);
        return allFromRefData.size();
    }

    private Set<CacheRefdataCourtroom> loadDataromRefDataService() {
        final JsonObject queryParameters = createObjectBuilder().build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(REFERENCEDATA_QUERY_COURTROOMS)
                        .withId(randomUUID())
                        .build(),
                queryParameters);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        if (Objects.isNull(response) || Objects.isNull(response.payload()) ||
                !response.payload().containsKey(ORGANISATIONUNITS)) {
            return Collections.emptySet();
        }

        final JsonArray organizations = response.payload().getJsonArray(ORGANISATIONUNITS);

        final Set<CacheRefdataCourtroom> result = organizations.stream().
                filter(orgs -> orgs.asJsonObject().containsKey(COURTROOMS)).
                map(orgs -> orgs.asJsonObject().getJsonArray(COURTROOMS)).
                flatMap(Collection::stream).
                map(room -> new CacheRefdataCourtroom(UUID.fromString(room.asJsonObject().getString("id")), room.asJsonObject().getString("courtroomName"))).
                collect(Collectors.toSet());

        return result;
    }

    void saveAll(Collection<CacheRefdataCourtroom> items) {
        items.forEach(b -> repos.save(b));
    }

}