package uk.gov.moj.cpp.listing.query.view.service;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Progression lookups made by the query side (ported from listing-query-view), plus
 * {@code caseExistsByCaseUrn}, which the JEE event processor used when publishing a court list
 * (P2.3.1). The methods nothing calls ({@code getProsecutionCaseDetails},
 * {@code getProsecutionCaseByCaseId}) are not carried over. Typed results (the schema-generated
 * {@code uk.gov.justice.core.courts.ProsecutionCase}) are replaced by the JSON they were read from.
 */
@Component
@RequiredArgsConstructor
public class ProgressionService {

    private static final String PROGRESSION_CASE_DETAILS = "progression.query.prosecutioncase";
    public static final String SEARCH_APPLICATION = "progression.query.application-only";
    private static final String CASE_ID = "caseId";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN = "progression.query.case-exist-by-caseurn";

    private final CppQueryClient cppQueryClient;

    /** {@code progression.query.application-only} as the calling user; empty on 404. */
    public Optional<JsonObject> getApplicationDetails(final UUID userId, final UUID applicationId) {
        final JsonObject payload = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .build();
        return cppQueryClient.request(SEARCH_APPLICATION, payload, userId);
    }

    /**
     * The {@code prosecutionCase} object of {@code progression.query.prosecutioncase} (as admin).
     * Empty when Progression has no such case; a failure to reach Progression is thrown.
     */
    public Optional<JsonObject> findProsecutionCaseByCaseId(final String caseId) {
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();
        return cppQueryClient.requestAsAdmin(PROGRESSION_CASE_DETAILS, requestParameter)
                .filter(payload -> payload.containsKey(PROSECUTION_CASE) && !payload.isNull(PROSECUTION_CASE))
                .map(payload -> payload.getJsonObject(PROSECUTION_CASE));
    }

    /**
     * {@code progression.query.case-exist-by-caseurn} as the given user (the JEE processor used
     * {@code requester.request} with the event's metadata). Empty when Progression answers 404.
     */
    public Optional<JsonObject> caseExistsByCaseUrn(final UUID userId, final String caseUrn) {
        final JsonObject requestParameter = createObjectBuilder().add("caseUrn", caseUrn).build();
        return cppQueryClient.request(PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN, requestParameter, userId);
    }
}
