package uk.gov.justice.api.resource;

import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.*;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiSessionAvailabilityValidationResource implements QueryApiSessionAvailabilityValidationResource {

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Override
    public Response validateSessionAvailability(final String panel,
                                                final String sessionStartDate,
                                                final String sessionEndDate,
                                                final String hearingStartTime,
                                                final String oucodeL2Code,
                                                final String ouCode,
                                                final String courtRoomId,
                                                final String courtRoomNumber,
                                                final String businessType,
                                                final String courtSession,
                                                final Boolean isSlotBased,
                                                final Boolean showOverbookedSlots,
                                                final String pageSize,
                                                final String pageNumber,
                                                final Integer availableDurationMins,
                                                final String status,
                                                final Integer consecutiveDays,
                                                final Boolean isWeekCommencing) {
        final SessionAvailabilityValidationParams validationParams = new SessionAvailabilityValidationParams(
                panel, sessionStartDate, sessionEndDate, hearingStartTime,
                oucodeL2Code, ouCode, courtRoomId, courtRoomNumber, businessType, courtSession,
                isSlotBased, showOverbookedSlots, pageSize, pageNumber, availableDurationMins,
                status, consecutiveDays, isWeekCommencing);
        final Map<String, String> params = buildParamsMap(validationParams);
        return courtSchedulerServiceAdapter.validateSessionAvailability(params);
    }

    private Map<String, String> buildParamsMap(final SessionAvailabilityValidationParams p) {
        final Map<String, String> params = new HashMap<>();
        params.put(PANEL, p.panel());
        params.put(SESSION_START_DATE, p.sessionStartDate());
        params.put(SESSION_END_DATE, p.sessionEndDate());
        params.put(HEARING_START_TIME, p.hearingStartTime());
        params.put(OU_L2_CODE, p.oucodeL2Code());
        params.put(OUCODE, p.ouCode());
        params.put(COURT_ROOM_ID, p.courtRoomId());
        params.put(COURT_ROOM_NUMBER, p.courtRoomNumber());
        params.put(BUSINESS_TYPE, p.businessType());
        params.put(COURT_SESSION, p.courtSession());
        if (p.isSlotBased() != null) {
            params.put(IS_SLOT_BASED, String.valueOf(p.isSlotBased()));
        }
        if (p.showOverbookedSlots() != null) {
            params.put(SHOW_OVERBOOKED_SLOTS, String.valueOf(p.showOverbookedSlots()));
        }
        params.put(PAGE_SIZE, p.pageSize());
        params.put(PAGE_NUMBER, p.pageNumber());
        if (p.availableDurationMins() != null) {
            params.put(DURATION, String.valueOf(p.availableDurationMins()));
        }
        params.put(STATUS, p.status() != null ? p.status() : "ALL");
        if (p.consecutiveDays() != null) {
            params.put(CONSECUTIVE_DAYS, String.valueOf(p.consecutiveDays()));
        }
        if (p.isWeekCommencing() != null) {
            params.put(IS_WEEK_COMMENCING, String.valueOf(p.isWeekCommencing()));
        }

        return params.entrySet().stream().filter(entry -> entry.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
