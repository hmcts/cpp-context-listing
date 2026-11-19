package uk.gov.moj.cpp.listing.common.service;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.client.ListingClientsProperties;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.io.StringReader;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The provisional (unconfirmed) bookings courtscheduler holds (MBD refactor P2.3.2; port of the JEE
 * listing-common {@code ProvisionalBookingService}): GET {@code /unconfirmedBooking} with the
 * params as query parameters, CJSCPPUID the context system user. A 200 body is parsed; any other
 * status, or a failed call, is reported as a 500, as before.
 */
@Component
public class ProvisionalBookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionalBookingService.class);
    private static final String UNCONFIRMED_RESOURCE = "/unconfirmedBooking";
    public static final String COURTSCHEDULER_GET_UNCONFIRMED_BOOKING_TYPE = "application/vnd.courtscheduler.get.unconfirmed.booking+json";
    public static final String CJS_CPP_UID = "CJSCPPUID";

    private final RestClient restClient;
    private final ListingClientsProperties properties;

    public ProvisionalBookingService(final RestClient cppRestClient, final ListingClientsProperties properties) {
        this.restClient = cppRestClient;
        this.properties = properties;
    }

    public HttpResult getSlots(final Map<String, String> params) {
        if (LOGGER.isInfoEnabled() && Objects.nonNull(params)) {
            params.forEach((key, value) -> LOGGER.info("Retrieve ProvisionalSlots in S & L with params '{}-{}'", key, value));
        }
        if (params == null) {
            throw new DataValidationException("Params for search ProvisionalSlots is null ....");
        }
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.courtschedulerBaseUrl() + UNCONFIRMED_RESOURCE);
            params.forEach(builder::queryParam);
            return restClient.get()
                    .uri(builder.encode().build().toUri())
                    .accept(MediaType.parseMediaType(COURTSCHEDULER_GET_UNCONFIRMED_BOOKING_TYPE))
                    .header(CJS_CPP_UID, properties.requiredSystemUserId().toString())
                    .exchange((request, response) -> {
                        final String body = new String(response.getBody().readAllBytes());
                        final int status = response.getStatusCode().value();
                        if (status == 200) {
                            LOGGER.info("Retrieve ProvisionalSlots successfully");
                            try (JsonReader reader = Json.createReader(new StringReader(body))) {
                                return new HttpResult(200, reader.readObject());
                            }
                        }
                        LOGGER.error("Retrieve ProvisionalSlots failed with status code:{}", status);
                        return new HttpResult(500, body);
                    });
        } catch (final RestClientException | JsonException ex) {
            LOGGER.error("Exception thrown on trying to Retrieve Provisional Booking Slots", ex);
            return new HttpResult(500, ex.getMessage());
        }
    }
}
