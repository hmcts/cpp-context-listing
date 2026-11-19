package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.command.courtlist.CourtListCommands.COURT_LIST_REQUEST_EXPORT;
import static uk.gov.moj.cpp.listing.command.courtlist.CourtListCommands.PUBLISH_COURT_LIST;
import static uk.gov.moj.cpp.listing.command.courtlist.CourtListCommands.PUBLISH_COURT_LISTS_FOR_CROWN_COURTS;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.accepted;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.metadata;
import static uk.gov.moj.cpp.listing.controllers.CommandRequests.payload;

import uk.gov.moj.cpp.listing.command.courtlist.CourtListCommands;
import uk.gov.moj.cpp.listing.openapi.api.CourtListCommandsOpenApi;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The court-list commands (MBD refactor P2.3b, ADR 015): {@code POST /publishCourtList/{courtCentreId}},
 * {@code POST /publishCourtListsForCrownCourts} and
 * {@code POST /courtlist/{courtCentreId}/{publishCourtListType}/{startDate}}. Each runs synchronously
 * in the request (D5) and answers {@code 202 Accepted}; the court-list build and the XHIBIT export
 * follow after commit, as the JEE processor did.
 */
@RestController
public class CourtListCommandsApi implements CourtListCommandsOpenApi {

    private final CourtListCommands commands;
    private final HttpServletRequest request;

    public CourtListCommandsApi(final CourtListCommands commands, final HttpServletRequest request) {
        this.commands = commands;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> postPublishCourtList(final String courtCentreId, final Map<String, Object> requestBody) {
        commands.publishCourtList(metadata(request, PUBLISH_COURT_LIST), payload(requestBody, Map.of("courtCentreId", courtCentreId)));
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postPublishCourtListsForCrownCourts(final Map<String, Object> requestBody) {
        commands.publishCourtListsForCrownCourts(metadata(request, PUBLISH_COURT_LISTS_FOR_CROWN_COURTS), payload(requestBody, Map.of()));
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postCourtListRequestExport(final String courtCentreId, final String publishCourtListType, final String startDate,
                                                           final Map<String, Object> requestBody) {
        commands.courtListRequestExport(metadata(request, COURT_LIST_REQUEST_EXPORT), payload(requestBody, Map.of(
                "courtCentreId", courtCentreId,
                "publishCourtListType", publishCourtListType,
                "startDate", startDate)));
        return accepted();
    }
}
