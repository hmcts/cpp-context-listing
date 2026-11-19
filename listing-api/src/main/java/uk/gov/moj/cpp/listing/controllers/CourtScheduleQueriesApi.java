package uk.gov.moj.cpp.listing.controllers;

import uk.gov.moj.cpp.listing.common.client.HttpResult;
import uk.gov.moj.cpp.listing.config.JsonValueConverter;
import uk.gov.moj.cpp.listing.openapi.api.CourtScheduleQueriesOpenApi;
import uk.gov.moj.cpp.listing.query.api.CourtScheduleQueryService;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three hand-written JAX-RS adapters that proxied listingcourtscheduler:
 * {@code /hearingSlots}, {@code /sessionAvailabilityValidation} and {@code /courtScheduleDraftStatus}.
 * They never ran the framework interceptor chain (no access control, ADR 005). A non-200 answer from
 * courtscheduler is passed through with its status, as the JAX-RS {@code Response} was.
 */
@RestController
public class CourtScheduleQueriesApi implements CourtScheduleQueriesOpenApi {

    private final CourtScheduleQueryService courtScheduleQueryService;

    public CourtScheduleQueriesApi(final CourtScheduleQueryService courtScheduleQueryService) {
        this.courtScheduleQueryService = courtScheduleQueryService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> searchHearingSlots(final String panel, final String sessionStartDate,
                                                                  final String sessionEndDate, final String pageSize,
                                                                  final String pageNumber, final String jurisdiction,
                                                                  final String hearingStartTime, final String oucodeL2Code,
                                                                  final String ouCode, final String courtRoomId,
                                                                  final String courtRoomNumber, final String businessType,
                                                                  final String courtSession, final Boolean isSlotBased,
                                                                  final Boolean showOverbookedSlots,
                                                                  final Integer availableDurationMins, final String status) {
        final HttpResult result = courtScheduleQueryService.getHearingSlots(panel, sessionStartDate, sessionEndDate,
                hearingStartTime, oucodeL2Code, ouCode, courtRoomId, courtRoomNumber, businessType, courtSession,
                isSlotBased, showOverbookedSlots, pageSize, pageNumber, availableDurationMins, status, jurisdiction);
        return passThrough(result);
    }

    @Override
    public ResponseEntity<Map<String, Object>> validateSessionAvailability(final Map<String, Object> requestBody) {
        return passThrough(courtScheduleQueryService.validateSessionAvailability(toJson(requestBody)));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCourtScheduleDraftStatus(final Map<String, Object> requestBody) {
        return ResponseEntity.ok(JsonValueConverter.toMap(courtScheduleQueryService.getCourtScheduleDraftStatus(toJson(requestBody))));
    }

    /**
     * Status and body as courtscheduler (or the failed call) gave them. A non-JSON body (the legacy
     * code passed the raw string through) is returned as {@code {"error": "<body>"}}.
     */
    static ResponseEntity<Map<String, Object>> passThrough(final HttpResult result) {
        final Object entity = result.entity();
        final Map<String, Object> body;
        if (entity instanceof JsonObject json) {
            body = JsonValueConverter.toMap(json);
        } else {
            body = parseOrWrap(entity == null ? "" : entity.toString());
        }
        return ResponseEntity.status(result.status()).body(body);
    }

    private static Map<String, Object> parseOrWrap(final String raw) {
        try (JsonReader reader = Json.createReader(new StringReader(raw))) {
            return JsonValueConverter.toMap(reader.readObject());
        } catch (final JsonException | IllegalStateException e) {
            final Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", raw);
            return error;
        }
    }

    private static JsonObject toJson(final Map<String, Object> requestBody) {
        return Json.createObjectBuilder(requestBody == null ? Map.of() : requestBody).build();
    }
}
