package uk.gov.moj.cpp.listing.common.azure;

import uk.gov.justice.services.common.configuration.Value;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class HearingSlotsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingSlotsService.class);

    private static final String SERVICE = "hearingSlots";

    private static final String SUBSCRIPTION_KEY = "rotasl.hearing-slots.subscription.key";

    @Inject
    @Value(key = SUBSCRIPTION_KEY, defaultValue = "75e6ff1510914801b91d176bcbeef0dc")
    private String subscriptionKey;

    @Inject
    private DeafultRotaslAzureService rotaslAzureService;

    public Response search(final Map<String, String> params) {
        return rotaslAzureService.get(SERVICE, subscriptionKey, params);
    }

    public Response update(final Object payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Update slots in Azure S & L with slot details '{}'", payload);
        }

        return rotaslAzureService.put(SERVICE, subscriptionKey, payload);
    }
}
