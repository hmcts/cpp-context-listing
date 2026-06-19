package uk.gov.moj.cpp.listing.query.view;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import javax.inject.Inject;

public class CacheRefDataCourtroomView {

    private static final String LISTING_UPDATE_ADD_COURTROOM = "listing.update.add-courtroom";
    private static final String LISTING_UPDATE_CLOSE_COURTROOM = "listing.update.close-courtroom";

    @Inject
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    @Handles(LISTING_UPDATE_ADD_COURTROOM)
    public void addRefDataCourtroom(final JsonEnvelope envelope) {
         cacheRefdataCourtroomLoader.addCourtRoom(envelope);
    }

    @Handles(LISTING_UPDATE_CLOSE_COURTROOM)
    public void closeRefDataCourtroom(final JsonEnvelope envelope) {
        cacheRefdataCourtroomLoader.closeCourtRoom(envelope);
    }
}
