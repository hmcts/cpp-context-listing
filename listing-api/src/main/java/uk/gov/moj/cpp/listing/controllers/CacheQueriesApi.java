package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.QueryParameters.body;

import uk.gov.moj.cpp.listing.openapi.api.CacheQueriesOpenApi;
import uk.gov.moj.cpp.listing.query.api.CacheRefDataCourtroomApi;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** {@code /cache-refdata-courtrooms/refresh}: reloads the court-room cache table from reference data. */
@RestController
public class CacheQueriesApi implements CacheQueriesOpenApi {

    private final CacheRefDataCourtroomApi cacheRefDataCourtroomApi;

    public CacheQueriesApi(final CacheRefDataCourtroomApi cacheRefDataCourtroomApi) {
        this.cacheRefDataCourtroomApi = cacheRefDataCourtroomApi;
    }

    @Override
    public ResponseEntity<Map<String, Object>> refreshCourtroomsCache() {
        return ResponseEntity.ok(body(cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms()));
    }
}
