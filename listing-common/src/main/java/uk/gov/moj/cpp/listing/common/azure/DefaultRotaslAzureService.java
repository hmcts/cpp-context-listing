package uk.gov.moj.cpp.listing.common.azure;

import static java.lang.String.format;

import uk.gov.moj.cpp.platform.data.utils.rest.service.RestClientService;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;

@ApplicationScoped
public class DefaultRotaslAzureService implements RotaslAzureService {

    @Inject
    private RotaslAzureConfig rotaslAzureConfig;

    @Inject
    private RestClientService restService;

    @Override
    public Response get(final String endpoint, final String subscription, final Map<String, String> params) {
        final Map<String, String> headers = getHeaders(subscription);

        final String url = getURL(endpoint);

        return restService.newResponseFrom(restService.get(url, headers, params), JsonObject.class);
    }

    @Override
    public Response put(final String endpoint, final String subscription, final Object payload) {
        final Map<String, String> headers = getHeaders(subscription);

        final String url = getURL(endpoint);

        return restService.newResponseFrom(restService.put(url, headers, payload), JsonObject.class);
    }

    @Override
    public Response post(final String endpoint, final String subscription, final Object payload) {
        final Map<String, String> headers = getHeaders(subscription);

        final String url = getURL(endpoint);

        return restService.newResponseFrom(restService.post(url, headers, payload), JsonObject.class);
    }

    private String getURL(final String endpoint) {

        return format("%s/%s", rotaslAzureConfig.getServiceUri(), endpoint);

    }

    private Map<String, String> getHeaders(final String subscriptionKey) {
        return ImmutableMap.of(
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                "Ocp-Apim-Subscription-Key", subscriptionKey,
                "Ocp-Apim-Trace", "true");
    }
}
