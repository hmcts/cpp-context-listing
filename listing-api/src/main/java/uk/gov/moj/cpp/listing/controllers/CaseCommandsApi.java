package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.CommandRequests.accepted;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.actionName;
import static uk.gov.moj.cpp.listing.controllers.HearingCommandRequests.command;

import uk.gov.moj.cpp.listing.command.hearing.api.HearingCommandApi;
import uk.gov.moj.cpp.listing.openapi.api.CaseCommandsOpenApi;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /cases} (MBD refactor P2.3.2, ADR 015): list-court-hearing and
 * list-unscheduled-court-hearing, dispatched on Content-Type. Synchronous (D5), 202.
 */
@RestController
public class CaseCommandsApi implements CaseCommandsOpenApi {

    static final String LIST_COURT_HEARING = "listing.command.list-court-hearing";
    static final String LIST_UNSCHEDULED_COURT_HEARING = "listing.command.list-unscheduled-court-hearing";

    private final HearingCommandApi api;
    private final HttpServletRequest request;

    public CaseCommandsApi(final HearingCommandApi api, final HttpServletRequest request) {
        this.api = api;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> postCases(final Map<String, Object> requestBody) {
        final String action = actionName(request);
        switch (action) {
            case LIST_COURT_HEARING -> api.handleListCourtHearing(command(request, action, requestBody, Map.of()));
            case LIST_UNSCHEDULED_COURT_HEARING -> api.handleListUnscheduledCourtHearing(command(request, action, requestBody, Map.of()));
            default -> throw new IllegalStateException("Unexpected media type for /cases: " + action);
        }
        return accepted();
    }
}
