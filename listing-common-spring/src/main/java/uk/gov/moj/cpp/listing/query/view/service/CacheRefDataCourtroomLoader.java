package uk.gov.moj.cpp.listing.query.view.service;

import jakarta.persistence.EntityManager;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import lombok.RequiredArgsConstructor;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.repository.CacheRefDataCourtroomRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
@Component
@RequiredArgsConstructor
public class CacheRefDataCourtroomLoader {

    private static final String REFERENCE_DATA_QUERY_COURTROOMS = "referencedata.query.courtrooms";
    public static final String ORGANISATION_UNITS = "organisationunits";
    public static final String COURTROOMS = "courtrooms";

    private final CppQueryClient cppQueryClient;

    private final CacheRefDataCourtroomRepository cacheRefdataCourtroomRepository;

    private final EntityManager entityManager;

    @Transactional
    public int loadCourtRooms() {
        final Set<CacheRefDataCourtroom> allFromRefData = loadDataRoomRefDataService();
        if (allFromRefData.isEmpty()) {
            return 0;
        }

        final Set<CacheRefDataCourtroom> cachedInListing =
                new HashSet<>(cacheRefdataCourtroomRepository.findAll());
        if (cachedInListing.equals(allFromRefData)) {
            return cachedInListing.size();
        }

        cacheRefdataCourtroomRepository.deleteAll();
        cacheRefdataCourtroomRepository.flush();
        entityManager.clear();
        saveAll(allFromRefData);
        return allFromRefData.size();
    }

    private Set<CacheRefDataCourtroom> loadDataRoomRefDataService() {
        try {
            final JsonObject queryParameters = createObjectBuilder().build();
            final JsonObject response = cppQueryClient.requestAsAdmin(REFERENCE_DATA_QUERY_COURTROOMS, queryParameters).orElse(null);

            if (Objects.isNull(response) || !response.containsKey(ORGANISATION_UNITS)) {
                return Collections.emptySet();
            }

            final JsonArray organizations = response.getJsonArray(ORGANISATION_UNITS);

            return organizations.stream().
                    filter(orgs -> orgs.asJsonObject().containsKey(COURTROOMS)).
                    map(orgs -> orgs.asJsonObject().getJsonArray(COURTROOMS)).
                    flatMap(Collection::stream).
                    map(room -> new CacheRefDataCourtroom(UUID.fromString(room.asJsonObject().getString("id")), room.asJsonObject().getString("courtroomName"))).
                    collect(Collectors.toSet());
        } catch (final Exception e) {
            return Collections.emptySet();
        }
    }

    void saveAll(Collection<CacheRefDataCourtroom> items) {
        items.forEach(b -> cacheRefdataCourtroomRepository.save(b));
    }
}