package uk.gov.moj.cpp.listing.query.api;

import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerSearchService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ServiceComponent(Component.QUERY_API)
public class SearchAvailableJudiciariesQueryHandler {

    private static final String ACTION_NAME = "listing.search.available.judiciaries";

    private static final String SEARCH = "search";
    private static final String JUDICIARY_GROUP = "judiciaryGroup";
    private static final String LIMIT = "limit";
    private static final String DATES = "dates";
    private static final String COURT_HOUSE_ID = "courtHouseId";
    private static final String COURT_SCHEDULE_IDS = "courtScheduleIds";
    private static final String IGNORE_AVAILABILITY = "ignoreAvailability";

    @SuppressWarnings("java:S6813") // field injection matches other listing-query-api handlers
    @Inject
    private CourtSchedulerSearchService courtSchedulerSearchService;

    @Handles(ACTION_NAME)
    public JsonEnvelope searchAvailableJudiciaries(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final Map<String, String> params = buildQueryParams(payload);
        final Response response = courtSchedulerSearchService.searchAvailableJudiciaries(params);
        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            throw new WebApplicationException(
                    Response.status(response.getStatus()).entity(response.getEntity()).build());
        }
        return envelopeFrom(metadataFrom(query.metadata()).withName(ACTION_NAME), (JsonObject) response.getEntity());
    }

    private static Map<String, String> buildQueryParams(final JsonObject payload) {
        final Map<String, String> raw = new HashMap<>();
        putIfNonBlank(raw, SEARCH, payload.getString(SEARCH, null));
        putIfNonBlank(raw, JUDICIARY_GROUP, payload.getString(JUDICIARY_GROUP, null));
        putIfNonBlank(raw, LIMIT, payload.getString(LIMIT, null));
        putIfNonBlank(raw, DATES, payload.getString(DATES, null));
        putIfNonBlank(raw, COURT_HOUSE_ID, payload.getString(COURT_HOUSE_ID, null));
        putIfNonBlank(raw, COURT_SCHEDULE_IDS, payload.getString(COURT_SCHEDULE_IDS, null));
        if (payload.containsKey(IGNORE_AVAILABILITY) && !payload.isNull(IGNORE_AVAILABILITY)) {
            raw.put(IGNORE_AVAILABILITY, String.valueOf(payload.getBoolean(IGNORE_AVAILABILITY)));
        }
        return raw;
    }

    private static void putIfNonBlank(final Map<String, String> map, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
