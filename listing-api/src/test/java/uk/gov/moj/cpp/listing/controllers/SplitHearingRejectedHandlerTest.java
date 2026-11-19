package uk.gov.moj.cpp.listing.controllers;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.common.splithearing.SplitHearingRejectedException;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The JEE {@code SplitHearingRejectedExceptionMapper} as {@link GlobalExceptionHandler} maps it
 * (MBD refactor P2.3.3; ported from {@code SplitHearingRejectedExceptionMapperTest}).
 */
class SplitHearingRejectedHandlerTest {

    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR = "error";
    private static final String FALLBACK = "fallback";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ResponseEntity<String> map(final int status, final JsonObject body, final String message) {
        return handler.handleSplitHearingRejected(new SplitHearingRejectedException(status, body, message));
    }

    @Test
    void shouldReturnProgressionsConflictAs422WithErrorCodeAndBodyMessage() {
        final ResponseEntity<String> response = map(409, createObjectBuilder().add(ERROR_CODE, "STALE").add("message", "hearing changed").build(), FALLBACK);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEqualTo("{\"errorCode\":\"STALE\",\"message\":\"hearing changed\"}");
    }

    @Test
    void shouldSurfaceProgressionsReasonWhenItReportsItUnderError() {
        final ResponseEntity<String> response = map(409, createObjectBuilder()
                .add(ERROR, "Hearing is resulted and cannot be split")
                .add("id", "0b8ba084-1f3b-4c27-a2b7-8f32d12164f8")
                .build(), "progression returned 409 for the split of hearing 0b8ba084-1f3b-4c27-a2b7-8f32d12164f8");

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isEqualTo("{\"errorCode\":\"HEARING_NOT_SPLITTABLE\",\"message\":\"Hearing is resulted and cannot be split\"}");
    }

    @Test
    void shouldPreferMessageOverErrorWhenProgressionSendsBoth() {
        assertThat(map(409, createObjectBuilder().add("message", "framework message").add(ERROR, "domain error").build(), FALLBACK).getBody())
                .isEqualTo("{\"errorCode\":\"HEARING_NOT_SPLITTABLE\",\"message\":\"framework message\"}");
    }

    @Test
    void shouldCodeAConflictThatProgressionLeftUncoded() {
        assertThat(map(409, null, "progression said no").getBody())
                .isEqualTo("{\"errorCode\":\"HEARING_NOT_SPLITTABLE\",\"message\":\"progression said no\"}");
    }

    @Test
    void shouldKeepProgressionsOwnCodeInsteadOfTheConflictDefault() {
        assertThat(map(409, createObjectBuilder().add(ERROR_CODE, "STALE").add(ERROR, "stale read").build(), FALLBACK).getBody())
                .isEqualTo("{\"errorCode\":\"STALE\",\"message\":\"stale read\"}");
    }

    @Test
    void shouldFallBackToExceptionMessageWhenBodyHasNoMessage() {
        final ResponseEntity<String> response = map(404, createObjectBuilder().add(ERROR_CODE, "NOT_FOUND").build(), FALLBACK);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo("{\"errorCode\":\"NOT_FOUND\",\"message\":\"fallback\"}");
    }

    @Test
    void shouldUseExceptionMessageAndOmitErrorCodeWhenBodyIsNull() {
        final ResponseEntity<String> response = map(500, null, "progression down");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo("{\"message\":\"progression down\"}");
    }

    @Test
    void shouldOmitMessageWhenNoneAvailable() {
        final ResponseEntity<String> response = map(400, null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("{}");
    }
}
