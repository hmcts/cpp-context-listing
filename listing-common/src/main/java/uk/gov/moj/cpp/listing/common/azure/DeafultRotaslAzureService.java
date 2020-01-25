package uk.gov.moj.cpp.listing.common.azure;

import static java.lang.String.format;

import uk.gov.justice.services.common.configuration.Value;
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
public class DeafultRotaslAzureService implements RotaslAzureService {

    private static final String SERVICE_URI = "rotasl.azure.service.uri";

    @Inject
    @Value(key = SERVICE_URI, defaultValue = "https://api-ste-ccm-scsl.azure-api.net/fa-ste-ccm-scsl")
    private String serviceUri;

    @Inject
    private RestClientService restService;

    @Override
    public Response get(final String endpoint, final String subscription, final Map<String, String> params) {
        final Map<String, String> headers = getHeaders(subscription);

        final String url = getURL(serviceUri, endpoint);

        return restService.newResponseFrom(restService.get(url, headers, params), JsonObject.class);
    }

    @Override
    public Response put(final String endpoint, final String subscription, final Object payload) {
        final Map<String, String> headers = getHeaders(subscription);

        final String url = getURL(serviceUri, endpoint);

        return restService.newResponseFrom(restService.put(url, headers, payload), JsonObject.class);
    }

    private String getURL(final String serviceUri, final String endpoint) {

        return format("%s/%s", serviceUri, endpoint);

    }

    private Map<String, String> getHeaders(final String subscriptionKey) {
        return ImmutableMap.of(
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                "Ocp-Apim-Subscription-Key", subscriptionKey,
                "Ocp-Apim-Trace", "true");
    }
}
