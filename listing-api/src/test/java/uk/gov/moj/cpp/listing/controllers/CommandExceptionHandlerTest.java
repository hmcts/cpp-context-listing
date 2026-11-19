package uk.gov.moj.cpp.listing.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.command.hearing.api.CommandBadRequestException;
import uk.gov.moj.cpp.listing.common.courtroomchange.ChangeCourtRoomForMultidayException;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownMultiDayExtensionException;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The JEE command-API exception mappers (CrownMultiDayExtension, ChangeCourtRoomForMultiday,
 * MoveHearingToPastDate) and the REST adapter's BadRequestException, as GlobalExceptionHandler
 * maps them (MBD refactor P2.3.2; ported from the JEE {@code *ExceptionMapperTest}s).
 */
class CommandExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noAvailability_returns422_withErrorCodeAndUnavailableDates() {
        final JsonObject body = Json.createObjectBuilder()
                .add("errorCode", "NO_AVAILABILITY")
                .add("message", "One or more requested session dates are not bookable")
                .add("unavailableDates", Json.createArrayBuilder().add("2026-03-04").add("2026-03-05"))
                .build();

        final ResponseEntity<String> response = handler.handleCrownMultiDayExtension(new CrownMultiDayExtensionException(422, body, "rejected"));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEqualTo("{\"errorCode\":\"NO_AVAILABILITY\",\"unavailableDates\":[\"2026-03-04\",\"2026-03-05\"]}");
    }

    @Test
    void noExistingAllocation_returns422_withErrorCodeOnly() {
        final ResponseEntity<String> response = handler.handleCrownMultiDayExtension(new CrownMultiDayExtensionException(422,
                Json.createObjectBuilder().add("errorCode", "NO_EXISTING_ALLOCATION").build(), "rejected"));

        assertThat(response.getBody()).isEqualTo("{\"errorCode\":\"NO_EXISTING_ALLOCATION\"}");
    }

    @Test
    void crownMultiDayPreservesUnderlyingHttpStatus_for500() {
        assertThat(handler.handleCrownMultiDayExtension(new CrownMultiDayExtensionException(500, Json.createObjectBuilder().build(),
                "courtscheduler unavailable")).getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void moveHearingToPastDate_returnsTheStatusWithErrorCodeAndMessageFromTheBody() {
        final JsonObject body = Json.createObjectBuilder().add("errorCode", "FUTURE_DATE_NOT_ALLOWED").add("message", "not in the past").build();

        final ResponseEntity<String> response = handler.handleMoveHearingToPastDate(new MoveHearingToPastDateException(422, body, "rejected"));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isEqualTo("{\"errorCode\":\"FUTURE_DATE_NOT_ALLOWED\",\"message\":\"not in the past\"}");
    }

    @Test
    void moveHearingToPastDate_nullBodyFallsBackToTheExceptionMessage() {
        final ResponseEntity<String> response = handler.handleMoveHearingToPastDate(new MoveHearingToPastDateException(404, null, "no session"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo("{\"message\":\"no session\"}");
    }

    @Test
    void changeCourtRoomForMultiday_bodyWithoutMessageUsesTheExceptionMessage() {
        final ResponseEntity<String> response = handler.handleChangeCourtRoomForMultiday(new ChangeCourtRoomForMultidayException(422,
                Json.createObjectBuilder().add("errorCode", "NO_ALLOCATION_ON_DATE").build(), "no allocation"));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isEqualTo("{\"errorCode\":\"NO_ALLOCATION_ON_DATE\",\"message\":\"no allocation\"}");
    }

    @Test
    void commandBadRequestIs400WithTheLegacyErrorBody() {
        final var response = handler.handleCommandBadRequest(new CommandBadRequestException("courtRoomId must not be empty for this case"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "courtRoomId must not be empty for this case");
    }
}
