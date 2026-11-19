package uk.gov.moj.cpp.listing.query.api;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import java.time.Instant;

import jakarta.json.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** {@code listing.get.cache-refdata-courtrooms-refresh} (ported from listing-query-api). */
@Component
@RequiredArgsConstructor
public class CacheRefDataCourtroomApi {

    public static final String LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH = "listing.get.cache-refdata-courtrooms-refresh";

    private final CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    public JsonObject refreshCacheRefDataCourtrooms() {
        final int count = cacheRefdataCourtroomLoader.loadCourtRooms();
        return createObjectBuilder()
                .add("timestamp", Instant.now().toString())
                .add("count", count)
                .build();
    }
}
