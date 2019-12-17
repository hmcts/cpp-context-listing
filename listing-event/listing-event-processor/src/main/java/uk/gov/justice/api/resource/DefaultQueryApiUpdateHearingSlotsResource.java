package uk.gov.justice.api.resource;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.platform.data.utils.rest.service.RestClientService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;

@Adapter(Component.EVENT_PROCESSOR)
public class DefaultQueryApiUpdateHearingSlotsResource implements UpdateHearingSlotsResource {


    private static final String ROTASL_ENDPOINT_URL = "rotasl.service.api.endpoint";
    private static final String ROTASL_ENDPOINT_KEY = "rotasl.service.api.subscription.key";
    private static final String PATH = "updateAvailableHearingSlots";
    private static final String KEY = "93411ae22b514c12ade724e880c135dc";

    @Inject
    @Value(key = ROTASL_ENDPOINT_URL, defaultValue = "https://api-ste-ccm-scs.azure-api.net/fa-ste-ccm-scsl/")
    private String rotaSLEndpoint;

    @Inject
    @Value(key = ROTASL_ENDPOINT_KEY, defaultValue = KEY)
    private String updateAvailableHearingSlotsSubscriptionKey;

    @Inject
    private RestClientService restClientService;


    private Map<String, String> buildParamsMap(final String slotDetails) {
        final Map<String, String> params = new HashMap<>();
        params.put(SLOT_DETAILS, slotDetails);

        return params.entrySet().stream().filter(entry -> entry.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Response updateHearingSlots(final String slotDetails) {

        final Map<String, String> params = buildParamsMap(slotDetails);

        return restClientService.newResponseFrom(restClientService.get(format("%s%s",rotaSLEndpoint, PATH), getRotaSlHeaders(), params), JsonObject.class);
    }

    private Map<String, String> getRotaSlHeaders() {
        return ImmutableMap.of(
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                "Ocp-Apim-Subscription-Key", updateAvailableHearingSlotsSubscriptionKey,
                "Ocp-Apim-Trace", "true");
    }
}
