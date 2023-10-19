package uk.gov.moj.cpp.listing.common.azure;

import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@SuppressWarnings("squid:S1312")
@ApplicationScoped
public class ProvisionalBookingService {

    @Inject
    private  Logger logger;

    private static final String SERVICE = "provisionalBooking";

    @Inject
    private RotaslAzureConfig rotaslAzureConfig;

    @Inject
    private DefaultRotaslAzureService rotaslAzureService;

    public Response bookSlots(final Object payload) {
        if (logger.isInfoEnabled()) {
            logger.info("provisionalBooking-bookSlots in Azure S & L with payload '{}'", payload);
        }

        return rotaslAzureService.post(SERVICE, rotaslAzureConfig.getSubscriptionKey(), payload);
    }

    public Response getSlots(final Map<String, String> params) {
        if (logger.isInfoEnabled() && Objects.nonNull(params)) {
            params.forEach((key, value) -> logger.info("provisionalBooking-getSlots in Azure S & L with params '{}-{}'", key, value));
        }

        return rotaslAzureService.get(SERVICE, rotaslAzureConfig.getSubscriptionKey(), params);
    }
}
