package uk.gov.moj.cpp.listing.common.azure;

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

    @Inject
    private RotaslAzureConfig rotaslAzureConfig;

    @Inject
    private DefaultRotaslAzureService rotaslAzureService;

    public Response search(final Map<String, String> params) {
        return rotaslAzureService.get(SERVICE, rotaslAzureConfig.getSubscriptionKey(), params);
    }

    public Response update(final Object payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Update slots in Azure S & L with slot details '{}'", payload);
        }

        return rotaslAzureService.put(SERVICE, rotaslAzureConfig.getSubscriptionKey(), payload);
    }
}
