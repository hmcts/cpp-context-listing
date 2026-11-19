package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.QueryParameters.body;
import static uk.gov.moj.cpp.listing.controllers.QueryParameters.query;
import static uk.gov.moj.cpp.listing.controllers.QueryParameters.userId;

import uk.gov.moj.cpp.listing.openapi.api.HearingQueriesOpenApi;
import uk.gov.moj.cpp.listing.query.api.HearingQueryApi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The hearing queries of {@code listing-query-api.raml} ({@code /hearings...}): each operation builds
 * the legacy query payload from its parameters and calls the ported {@link HearingQueryApi}.
 * {@code /hearings/range-search} serves two query names, chosen by the Accept media type as the
 * framework did.
 */
@RestController
public class HearingQueriesApi implements HearingQueriesOpenApi {

    static final String COURT_CALENDAR = "application/vnd.listing.search.hearings.court.calendar+json";

    private final HearingQueryApi hearingQueryApi;
    private final HttpServletRequest request;

    public HearingQueriesApi(final HearingQueryApi hearingQueryApi, final HttpServletRequest request) {
        this.hearingQueryApi = hearingQueryApi;
        this.request = request;
    }

    @Override
    public ResponseEntity<Map<String, Object>> searchHearings(final Boolean allocated, final String searchDate,
                                                              final String courtCentreId, final String courtRoomId,
                                                              final String authorityId, final String hearingTypeId,
                                                              final String jurisdictionType, final String startTime,
                                                              final String endTime) {
        final JsonObject payload = query()
                .bool("allocated", allocated)
                .string("courtCentreId", courtCentreId)
                .string("courtRoomId", courtRoomId)
                .string("authorityId", authorityId)
                .string("hearingTypeId", hearingTypeId)
                .string("jurisdictionType", jurisdictionType)
                .string("searchDate", searchDate)
                .string("startTime", startTime)
                .string("endTime", endTime)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.searchHearings(payload, userId(request))));
    }

    @Override
    public ResponseEntity<Map<String, Object>> availableSearchHearings(final String hearingId, final String caseUrn,
                                                                       final String searchCriteria, final String jurisdictionType,
                                                                       final String matchedDefendantIds,
                                                                       final String caseUrnForLinkedCases,
                                                                       final Boolean returnAllHearings) {
        final JsonObject payload = query()
                .string("hearingId", hearingId)
                .string("caseUrn", caseUrn)
                .string("searchCriteria", searchCriteria)
                .string("jurisdictionType", jurisdictionType)
                .string("matchedDefendantIds", matchedDefendantIds)
                .string("caseUrnForLinkedCases", caseUrnForLinkedCases)
                .bool("returnAllHearings", returnAllHearings)
                .build();
        try {
            return ResponseEntity.ok(body(hearingQueryApi.searchAvailableHearings(payload)));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> allocatedAndUnallocatedHearings(final String caseId, final String applicationId) {
        final JsonObject payload = query()
                .string("caseId", caseId)
                .string("applicationId", applicationId)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.searchUnallocatedHearings(payload)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> rangeSearchHearings(final Boolean allocated, final String courtCentreId,
                                                                   final String ouCode, final String courtRoomId,
                                                                   final String authorityId, final String hearingTypeId,
                                                                   final String jurisdictionType, final String startDate,
                                                                   final String endDate, final String weekCommencingStartDate,
                                                                   final String weekCommencingEndDate, final Integer pageSize,
                                                                   final Boolean useMaxPageSize, final Integer pageNumber,
                                                                   final Boolean possibleDisqualification, final String businessType,
                                                                   final String courtSession, final String exactHearingStartDateTime) {
        final JsonObject payload = query()
                .bool("allocated", allocated)
                .string("courtCentreId", courtCentreId)
                .string("ouCode", ouCode)
                .string("courtRoomId", courtRoomId)
                .string("authorityId", authorityId)
                .string("hearingTypeId", hearingTypeId)
                .string("jurisdictionType", jurisdictionType)
                .string("startDate", startDate)
                .string("endDate", endDate)
                .string("weekCommencingStartDate", weekCommencingStartDate)
                .string("weekCommencingEndDate", weekCommencingEndDate)
                .number("pageSize", pageSize)
                .bool("useMaxPageSize", useMaxPageSize)
                .number("pageNumber", pageNumber)
                .bool("possibleDisqualification", possibleDisqualification)
                .string("businessType", businessType)
                .string("courtSession", courtSession)
                .string("exactHearingStartDateTime", exactHearingStartDateTime)
                .build();
        if (acceptsCourtCalendar()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(COURT_CALENDAR))
                    .body(body(hearingQueryApi.rangeSearchHearingsForCourtCalendar(payload, userId(request))));
        }
        return ResponseEntity.ok(body(hearingQueryApi.rangeSsearchHearings(payload, userId(request))));
    }

    @Override
    public ResponseEntity<Map<String, Object>> searchHearing(final String id) {
        final JsonObject payload = query().string("id", id).build();
        return ResponseEntity.ok(body(hearingQueryApi.searchForHearingById(payload)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> unscheduledSearchHearings(final String caseUrn, final String typeOfList,
                                                                         final String oucodeL2Code, final String courtCentreId,
                                                                         final Integer pageSize, final Integer pageNumber) {
        final JsonObject payload = query()
                .string("caseUrn", caseUrn)
                .string("typeOfList", typeOfList)
                .string("oucodeL2Code", oucodeL2Code)
                .string("courtCentreId", courtCentreId)
                .number("pageSize", pageSize)
                .number("pageNumber", pageNumber)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.searchUnscheduledHearings(payload)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> anyAllocationSearchHearings(final String caseUrn, final String startDate) {
        final JsonObject payload = query()
                .string("caseUrn", caseUrn)
                .string("startDate", startDate)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.searchHearingsWithAnyAllocationState(payload)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> cotrSearchHearings(final String courtCentreId, final String startDate, final String endDate) {
        final JsonObject payload = query()
                .string("courtCentreId", courtCentreId)
                .string("startDate", startDate)
                .string("endDate", endDate)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.searchHearingsForCotr(payload)));
    }

    /**
     * The court-calendar name is chosen when Accept names its media type (the framework resolved the
     * action from the Accept vendor type; AuthzActionGuardFilter rejects an Accept with two vendor types).
     */
    private boolean acceptsCourtCalendar() {
        final String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase().contains("vnd.listing.search.hearings.court.calendar+json");
    }
}
