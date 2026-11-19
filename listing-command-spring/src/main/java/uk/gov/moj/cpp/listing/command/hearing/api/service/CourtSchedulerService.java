package uk.gov.moj.cpp.listing.command.hearing.api.service;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.common.service.ProvisionalBookingService;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The courtscheduler sessions held under a provisional booking (MBD refactor P2.3.2; port of the
 * JEE command-api {@code CourtSchedulerService}). Its other method, {@code getCourtSchedulesById},
 * had no caller and is not ported.
 */
@Component("hearingApiCourtSchedulerService")
public class CourtSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulerService.class);

    @Autowired
    ProvisionalBookingService provisionalBookingService;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    /**
     * The sessions courtscheduler holds under this booking id. An empty list is courtscheduler's own
     * answer that it holds none: it answers an unknown booking id with 200 and no slots.
     *
     * @throws CourtSchedulerUnavailableException when courtscheduler gave no answer at all, so the
     *                                            listing fails and is retried instead of being treated
     *                                            as a booking that is gone
     */
    public List<CourtSchedule> getCourtSchedulesByProvisionalBookingId(final String bookingId) {
        final Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("bookingIds", bookingId);
        final HttpResult slotsResponse = provisionalBookingService.getSlots(paramsMap);
        final List<CourtSchedule> courtScheduleList = new ArrayList<>();

        if (slotsResponse == null || 200 != slotsResponse.getStatus()) {
            final Object status = slotsResponse == null ? "no response" : slotsResponse.getStatus();
            LOGGER.error("courtscheduler did not answer the lookup of booking {} (status {}); failing so the listing is retried",
                    bookingId, status);
            throw new CourtSchedulerUnavailableException(
                    "courtscheduler did not answer the lookup of booking " + bookingId + " (status " + status + ")");
        }

        final JsonObject resultJson = objectToJsonObjectConverter.convert(slotsResponse.getEntity());
        if (resultJson == null) {
            return courtScheduleList;
        }

        final JsonArray provisionalSlots = resultJson.getJsonArray("provisionalSlots");
        if (provisionalSlots == null) {
            return courtScheduleList;
        }

        for (int i = 0; i < provisionalSlots.size(); i++) {
            courtScheduleList.add(jsonObjectConverter.convert(provisionalSlots.getJsonObject(i), CourtSchedule.class));
        }
        return courtScheduleList;
    }
}
