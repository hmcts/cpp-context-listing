package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.QueryParameters.body;
import static uk.gov.moj.cpp.listing.controllers.QueryParameters.query;

import uk.gov.moj.cpp.listing.envelope.SkipEnvelope;
import uk.gov.moj.cpp.listing.openapi.api.CourtListQueriesOpenApi;
import uk.gov.moj.cpp.listing.query.api.CourtListDocumentQuery;
import uk.gov.moj.cpp.listing.query.api.HearingQueryApi;

import java.util.Map;

import jakarta.json.JsonObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Court list queries: {@code /courtlist} (PDF, the hand-written JAX-RS adapter
 * {@code DefaultQueryApiCourtlistResource}), {@code /courtlistpayload}, {@code /dailylistpayload} and
 * {@code /courtListPublishStatus/{courtCentreId}}.
 */
@RestController
public class CourtListQueriesApi implements CourtListQueriesOpenApi {

    private static final String PDF = "application/pdf";
    private static final String DISPOSITION = "attachment; filename=\"CourtList.pdf\"";

    private final HearingQueryApi hearingQueryApi;
    private final CourtListDocumentQuery courtListDocumentQuery;

    public CourtListQueriesApi(final HearingQueryApi hearingQueryApi, final CourtListDocumentQuery courtListDocumentQuery) {
        this.hearingQueryApi = hearingQueryApi;
        this.courtListDocumentQuery = courtListDocumentQuery;
    }

    @Override
    @SkipEnvelope
    public ResponseEntity<Resource> searchCourtList(final String courtCentreId, final String listId, final String startDate,
                                                    final String endDate, final String courtRoomId, final Boolean restricted) {
        final byte[] pdf = courtListDocumentQuery.getCourtList(courtCentreId, courtRoomId, listId, startDate, endDate,
                Boolean.TRUE.equals(restricted));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION)
                .body(new ByteArrayResource(pdf));
    }

    @Override
    public ResponseEntity<Map<String, Object>> searchCourtListPayload(final String courtCentreId, final String listId,
                                                                      final String startDate, final String endDate,
                                                                      final String courtRoomId, final Boolean restricted,
                                                                      final Boolean includeApplications) {
        final JsonObject payload = query()
                .string("courtCentreId", courtCentreId)
                .string("courtRoomId", courtRoomId)
                .string("listId", listId)
                .string("startDate", startDate)
                .string("endDate", endDate)
                .bool("restricted", restricted)
                .bool("includeApplications", includeApplications)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.searchHearingsForCourtListPayload(payload)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> searchDailyListPayload(final String courtCentreId, final String publishCourtListType,
                                                                      final String startDate, final String endDate,
                                                                      final String weekCommencingStartDate,
                                                                      final String weekCommencingEndDate) {
        final JsonObject payload = query()
                .string("courtCentreId", courtCentreId)
                .string("startDate", startDate)
                .string("publishCourtListType", publishCourtListType)
                .string("endDate", endDate)
                .string("weekCommencingStartDate", weekCommencingStartDate)
                .string("weekCommencingEndDate", weekCommencingEndDate)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.getDailyList(payload)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCourtListPublishStatus(final String courtCentreId, final String publishCourtListTypes,
                                                                         final String publishDate, final Boolean weekCommencing) {
        final JsonObject payload = query()
                .string("courtCentreId", courtCentreId)
                .string("publishCourtListTypes", publishCourtListTypes)
                .string("publishDate", publishDate)
                .bool("weekCommencing", weekCommencing)
                .build();
        return ResponseEntity.ok(body(hearingQueryApi.publishCourtListStatus(payload)));
    }

    static MediaType pdf() {
        return MediaType.parseMediaType(PDF);
    }
}
