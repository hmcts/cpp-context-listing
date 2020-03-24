package uk.gov.moj.cpp.listing.common.azure;

import java.util.Map;

import javax.ws.rs.core.Response;

public interface RotaslAzureService {

    Response get(final String endpoint, final String subscription, final Map<String, String> params);

    Response put(final String endpoint, final String subscription, final Object payload);
}
