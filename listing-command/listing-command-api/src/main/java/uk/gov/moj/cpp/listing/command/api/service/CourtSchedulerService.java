package uk.gov.moj.cpp.listing.command.api.service;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.common.service.ProvisionalBookingService;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

@SuppressWarnings("squid:S6813")
@ApplicationScoped
public class CourtSchedulerService {

    @Inject
    ProvisionalBookingService provisionalBookingService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public List<CourtSchedule> getCourtSchedulesByProvisionalBookingId(final String bookingId) {
        final Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("bookingIds", bookingId);
        final Response slotsResponse = provisionalBookingService.getSlots(paramsMap);
        final JsonObject resultJson = objectToJsonObjectConverter.convert(slotsResponse.getEntity());

        final List<CourtSchedule> courtScheduleList = new ArrayList<>();

        final JsonArray provisionalSlots = resultJson.getJsonArray("provisionalSlots");
        for (int i = 0; i < provisionalSlots.size(); i++) {

            courtScheduleList.add(jsonObjectConverter.convert(provisionalSlots.getJsonObject(i), CourtSchedule.class));
        }
        return courtScheduleList;
    }
}
