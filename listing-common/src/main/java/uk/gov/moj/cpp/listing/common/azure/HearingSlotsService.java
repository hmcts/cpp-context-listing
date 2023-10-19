package uk.gov.moj.cpp.listing.common.azure;

import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@SuppressWarnings("squid:S1312")
@ApplicationScoped
public class HearingSlotsService {

    @Inject
    private Logger logger;

    private static final String SERVICE = "hearingSlots";

    @Inject
    private RotaslAzureConfig rotaslAzureConfig;

    @Inject
    private DefaultRotaslAzureService rotaslAzureService;

    public Response search(final Map<String, String> params) {
        return rotaslAzureService.get(SERVICE, rotaslAzureConfig.getSubscriptionKey(), params);
    }

    public Response update(final Object payload) {
        if (logger.isInfoEnabled()) {
            logger.info("Update slots in Azure S & L with slot details '{}'", payload);
        }

        return rotaslAzureService.put(SERVICE, rotaslAzureConfig.getSubscriptionKey(), payload);
    }

    public Response delete(final UUID hearingId) {
        if (logger.isInfoEnabled()) {
            logger.info("Delete slots in Azure S & L with hearing id '{}'", hearingId);
        }

        return rotaslAzureService.delete(SERVICE, rotaslAzureConfig.getSubscriptionKey(), hearingId);
    }
}
