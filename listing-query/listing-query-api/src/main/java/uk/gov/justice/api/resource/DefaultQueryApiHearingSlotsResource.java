package uk.gov.justice.api.resource;

import static java.util.stream.Collectors.toMap;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiHearingSlotsResource implements QueryApiHearingSlotsResource {

    @Inject
    private HearingSlotsService hearingSlotsService;

    @Override
    public Response getHearingSlots(final String panel,
                                    final String sessionStartDate,
                                    final String sessionEndDate,
                                    final String oucodeL2Code,
                                    final String ouCode,
                                    final String courtRoomId,
                                    final String courtRoomNumber,
                                    final String businessType,
                                    final String courtSession,
                                    final String pageSize,
                                    final String pageNumber) {

        final Map<String, String> params = buildParamsMap(panel, sessionStartDate, sessionEndDate, oucodeL2Code, ouCode, courtRoomId, courtRoomNumber, businessType, courtSession, pageSize, pageNumber);

        return hearingSlotsService.search(params);
    }

    private Map<String, String> buildParamsMap(final String panel,
                                               final String sessionStartDate,
                                               final String sessionEndDate,
                                               final String oucodeL2Code,
                                               final String ouCode,
                                               final String courtRoomId,
                                               final String courtRoomNumber,
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
        params.put(COURT_ROOM_NUMBER, courtRoomNumber);
        params.put(BUSINESS_TYPE, businessType);
        params.put(COURT_SESSION, courtSession);
        params.put(PAGE_SIZE, pageSize);
        params.put(PAGE_NUMBER, pageNumber);

        return params.entrySet().stream().filter(entry -> entry.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
