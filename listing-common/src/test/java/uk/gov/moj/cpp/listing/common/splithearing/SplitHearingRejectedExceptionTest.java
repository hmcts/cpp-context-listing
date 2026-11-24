package uk.gov.moj.cpp.listing.common.splithearing;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

class SplitHearingRejectedExceptionTest {

    @Test
    void shouldExposeStatusBodyAndErrorCodeFromProgressionResponse() {
        final JsonObject body = createObjectBuilder().add("errorCode", "STALE").add("message", "stale").build();

        final SplitHearingRejectedException exception = new SplitHearingRejectedException(409, body, "rejected");

        assertThat(exception.getHttpStatus(), is(409));
        assertThat(exception.getResponseBody(), is(body));
        assertThat(exception.getErrorCode(), is("STALE"));
        assertThat(exception.getMessage(), is("rejected"));
    }

    @Test
    void shouldHaveNullErrorCodeWhenBodyIsNull() {
        final SplitHearingRejectedException exception = new SplitHearingRejectedException(500, null, "down");

        assertThat(exception.getResponseBody(), is(nullValue()));
        assertThat(exception.getErrorCode(), is(nullValue()));
    }

    @Test
    void shouldHaveNullErrorCodeWhenBodyHasNone() {
        final SplitHearingRejectedException exception =
                new SplitHearingRejectedException(400, createObjectBuilder().add("message", "bad").build(), "bad");

        assertThat(exception.getErrorCode(), is(nullValue()));
    }
}
