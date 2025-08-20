package uk.gov.moj.cpp.listing.query.api;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.CacheRefDataCourtroomView;
import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import java.time.Instant;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CacheRefDataCourtroomApi {

    public static final String LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH = "listing.get.cache-refdata-courtrooms-refresh";
    private static final String LISTING_UPDATE_ADD_COURTROOM = "listing.update.add-courtroom";
    private static final String LISTING_UPDATE_CLOSE_COURTROOM = "listing.update.close-courtroom";

    @Inject
    private Enveloper enveloper;

    @Inject
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    @Inject
    private CacheRefDataCourtroomView cacheRefDataCourtroomView;


    @Handles(LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH)
    public JsonEnvelope refreshCacheRefDataCourtrooms(final JsonEnvelope envelope) {

        final int count = cacheRefdataCourtroomLoader.loadCourtRooms();

        return enveloper.withMetadataFrom(envelope, LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH).
                apply(createObjectBuilder().
                        add("timestamp", Instant.now().toString()).
                        add("count", count).
                        build());
    }


    @Handles(LISTING_UPDATE_ADD_COURTROOM)
    public void addRefDataCourtroom(final JsonEnvelope envelope) {
        cacheRefDataCourtroomView.addRefDataCourtroom(envelope);
    }

    @Handles(LISTING_UPDATE_CLOSE_COURTROOM)
    public void closeRefDataCourtroom(final JsonEnvelope envelope) {
        cacheRefDataCourtroomView.closeRefDataCourtroom(envelope);
    }

}
