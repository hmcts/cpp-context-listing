package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.QueryParameters.userId;

import uk.gov.moj.cpp.listing.envelope.SkipEnvelope;
import uk.gov.moj.cpp.listing.openapi.api.ReportQueriesOpenApi;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /hearings/download-hearing-csv-report}, the hand-written JAX-RS adapter
 * {@code DefaultQueryApiHearingsDownloadHearingCsvReportResource}: {@code text/csv} with
 * {@code Content-Disposition: attachment; filename="hearing_report_<startDate>.csv"}, no envelope,
 * no access control (ADR 005). An empty report gave a {@code null} JAX-RS response (204); the CSV
 * always has a header row, so that does not happen in practice.
 */
@RestController
public class ReportQueriesApi implements ReportQueriesOpenApi {

    private static final String CSV_MIME_TYPE = "text/csv";

    private final HearingQueryView hearingQueryView;
    private final HttpServletRequest request;

    public ReportQueriesApi(final HearingQueryView hearingQueryView, final HttpServletRequest request) {
        this.hearingQueryView = hearingQueryView;
        this.request = request;
    }

    @Override
    @SkipEnvelope
    public ResponseEntity<String> downloadHearingCsvReport(final String courtCentreId, final String startDate, final String numberOfWeeks) {
        final UUID userId = userId(request);
        if (userId == null) {
            // The adapter dereferenced the @HeaderParam CJSCPPUID UUID unconditionally (500).
            throw new IllegalStateException("CJSCPPUID header missing");
        }
        // As the adapter: numberOfWeeks is sent on as a string (default "2"); the view reads it with
        // getInt(name, 2), which ignores a string, so the report always covers 2 weeks.
        final JsonObject query = Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId)
                .add("startDate", startDate)
                .add("numberOfWeeks", numberOfWeeks != null ? numberOfWeeks : "2")
                .build();
        final String csvContent = hearingQueryView.generateHearingCsvReport(query, userId);
        if (csvContent == null || csvContent.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, CSV_MIME_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hearing_report_" + startDate + ".csv\"")
                .body(csvContent);
    }
}
