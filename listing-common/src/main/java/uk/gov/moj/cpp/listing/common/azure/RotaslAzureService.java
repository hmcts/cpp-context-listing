package uk.gov.moj.cpp.listing.common.azure;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

public interface RotaslAzureService {
    Response get(final String endpoint, final String subscription, final Map<String, String> params);

    Response delete(final String endpoint, final String subscription, final UUID hearingId);

    Response put(final String endpoint, final String subscription, final Object payload);

    Response post(final String endpoint, final String subscription, final Object payload);
}
