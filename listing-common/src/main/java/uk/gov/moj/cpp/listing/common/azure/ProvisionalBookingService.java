package uk.gov.moj.cpp.listing.common.azure;

import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProvisionalBookingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionalBookingService.class);

    private static final String SERVICE = "provisionalBooking";

    @Inject
    private RotaslAzureConfig rotaslAzureConfig;

    @Inject
    private DefaultRotaslAzureService rotaslAzureService;

    public Response bookSlots(final Object payload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("provisionalBooking-bookSlots in Azure S & L with payload '{}'", payload);
        }

        return rotaslAzureService.post(SERVICE, rotaslAzureConfig.getSubscriptionKey(), payload);
    }

    public Response getSlots(final Map<String, String> params) {
        if (LOGGER.isInfoEnabled() && Objects.nonNull(params)) {
            params.forEach((key, value) -> LOGGER.info("provisionalBooking-getSlots in Azure S & L with params '{}-{}'", key, value));
        }

        return rotaslAzureService.get(SERVICE, rotaslAzureConfig.getSubscriptionKey(), params);
    }
}
