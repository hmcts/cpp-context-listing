package uk.gov.moj.cpp.listing.common.splithearing;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

/** Ported from listing-common's SplitHearingRejectedExceptionTest (MBD refactor P2.3.3). */
class SplitHearingRejectedExceptionTest {

    @Test
    void shouldExposeStatusBodyAndErrorCodeFromProgressionResponse() {
        final JsonObject body = Json.createObjectBuilder().add("errorCode", "STALE").add("message", "m").build();
        final SplitHearingRejectedException exception = new SplitHearingRejectedException(409, body, "msg");

        assertThat(exception.getHttpStatus()).isEqualTo(409);
        assertThat(exception.getResponseBody()).isEqualTo(body);
        assertThat(exception.getErrorCode()).isEqualTo("STALE");
        assertThat(exception.getMessage()).isEqualTo("msg");
    }

    @Test
    void shouldHaveNullErrorCodeWhenBodyIsNull() {
        assertThat(new SplitHearingRejectedException(500, null, "msg").getErrorCode()).isNull();
    }

    @Test
    void shouldHaveNullErrorCodeWhenBodyHasNone() {
        assertThat(new SplitHearingRejectedException(400, Json.createObjectBuilder().add("message", "m").build(), "msg").getErrorCode()).isNull();
    }
}
