package uk.gov.moj.cpp.listing.common.azure;

import uk.gov.justice.services.common.configuration.Value;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class RotaslAzureConfig {

    private static final String SUBSCRIPTION_KEY = "rotasl.hearing-slots.subscription.key";

    private static final String SERVICE_URI = "rotasl.azure.service.uri";

    @Inject
    @Value(key = SUBSCRIPTION_KEY, defaultValue = "75e6ff1510914801b91d176bcbeef0dc")
    private String subscriptionKey;

    @Inject
    @Value(key = SERVICE_URI, defaultValue = "http://localhost:8080/fa-ste-ccm-scsl")
    private String serviceUri;

    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public String getServiceUri() {
        return serviceUri;
    }
}
