package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.QueryParameters.body;
import static uk.gov.moj.cpp.listing.controllers.QueryParameters.query;
import static uk.gov.moj.cpp.listing.controllers.QueryParameters.userId;

import uk.gov.moj.cpp.listing.openapi.api.CaseQueriesOpenApi;
import uk.gov.moj.cpp.listing.query.api.HearingQueryApi;

import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** {@code /cases/by-person-defendant-and-hearingDate} and {@code /cases/by-organisation-defendant-and-hearingDate}. */
@RestController
public class CaseQueriesApi implements CaseQueriesOpenApi {

    private final HearingQueryApi hearingQueryApi;
    private final HttpServletRequest request;

    public CaseQueriesApi(final HearingQueryApi hearingQueryApi, final HttpServletRequest request) {
        this.hearingQueryApi = hearingQueryApi;
        this.request = request;
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCasesByPersonDefendant(final String firstName, final String lastName,
                                                                         final String hearingDate, final String dateOfBirth,
                                                                         final Boolean isCivil, final Boolean isGroupMember) {
        final JsonObject payload = query()
                .string("firstName", firstName)
                .string("lastName", lastName)
                .string("dateOfBirth", dateOfBirth)
                .string("hearingDate", hearingDate)
                .bool("isCivil", isCivil)
                .bool("isGroupMember", isGroupMember)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.getCasesByPersonDefendantAndHearingDate(payload, userId(request))));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCasesByOrganisationDefendant(final String organisationName, final String hearingDate,
                                                                               final Boolean isCivil, final Boolean isGroupMember) {
        final JsonObject payload = query()
                .string("organisationName", organisationName)
                .string("hearingDate", hearingDate)
                .bool("isCivil", isCivil)
                .bool("isGroupMember", isGroupMember)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.getCasesByOrganisationDefendantAndHearingDate(payload, userId(request))));
    }
}
