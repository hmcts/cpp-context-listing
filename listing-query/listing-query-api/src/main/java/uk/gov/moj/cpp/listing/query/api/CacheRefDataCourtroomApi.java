package uk.gov.moj.cpp.listing.query.api;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import java.time.Instant;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CacheRefDataCourtroomApi {

    public static final String LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH = "listing.get.cache-refdata-courtrooms-refresh";

    @Inject
    private Enveloper enveloper;

    @Inject
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;


    @Handles(LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH)
    public JsonEnvelope refreshCacheRefDataCourtrooms(final JsonEnvelope envelope) {

        final int count = cacheRefdataCourtroomLoader.loadCourtRooms();

        return enveloper.withMetadataFrom(envelope, LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH).
                apply(createObjectBuilder().
                        add("timestamp", Instant.now().toString()).
                        add("count", count).
                        build());
    }
}
