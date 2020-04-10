package uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform;

import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;

public interface ListingEventTransformer {
    JsonObject transform(Metadata eventMetadata, JsonObject payload);
}
