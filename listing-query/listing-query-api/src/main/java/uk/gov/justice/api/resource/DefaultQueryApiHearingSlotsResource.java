package uk.gov.justice.api.resource;

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

@Adapter(Component.QUERY_API)
public class DefaultQueryApiHearingSlotsResource implements QueryApiHearingSlotsResource {

    @Inject
    @Value(key = "search-hearing-slots.url", defaultValue = "https://restapilatency-spike-function.azure-api.net/sandlblobeventprocessor/getApiForSchedulingAndListingTesting")
    private String searchHearingSlotsUrl;

    @Inject
    @Value(key = "search-hearing-slots.subscription.key", defaultValue = "93411ae22b514c12ade724e880c135dc")
    private String searchHearingSlotsSubscriptionKey;

    @Inject
    private RestClientService restClientService;

    @Override
    public Response getHearingSlots(final String panel,
                                    final String sessionStartDate,
                                    final String sessionEndDate,
                                    final String oucodeL2Code,
                                    final String ouCode,
                                    final String courtRoomId,
                                    final String businessType,
                                    final String courtSession,
                                    final String pageSize,
                                    final String pageNumber) {

        final Map<String, String> params = buildParamsMap(panel, sessionStartDate, sessionEndDate, oucodeL2Code, ouCode, courtRoomId, businessType, courtSession, pageSize, pageNumber);

        return restClientService.newResponseFrom(restClientService.get(searchHearingSlotsUrl, getRotaSlHeaders(), params), JsonObject.class);
    }

    private Map<String, String> getRotaSlHeaders() {
        return ImmutableMap.of(
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                "Ocp-Apim-Subscription-Key", searchHearingSlotsSubscriptionKey,
                "Ocp-Apim-Trace", "true");
    }

    private Map<String, String> buildParamsMap(final String panel,
                                               final String sessionStartDate,
                                               final String sessionEndDate,
                                               final String oucodeL2Code,
                                               final String ouCode,
                                               final String courtRoomId,
                                               final String businessType,
                                               final String courtSession,
                                               final String pageSize,
                                               final String pageNumber) {
        final Map<String, String> params = new HashMap<>();
        params.put(PANEL, panel);
        params.put(SESSION_START_DATE, sessionStartDate);
        params.put(SESSION_END_DATE, sessionEndDate);
        params.put(OU_L2_CODE, oucodeL2Code);
        params.put(OUCODE, ouCode);
        params.put(COURT_ROOM_ID, courtRoomId);
        params.put(BUSINESS_TYPE, businessType);
        params.put(COURT_SESSION, courtSession);
        params.put(PAGE_SIZE, pageSize);
        params.put(PAGE_NUMBER, pageNumber);

        return params.entrySet().stream().filter(entry -> entry.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
