package uk.gov.moj.cpp.listing.command.api.mapper;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.listing.common.splithearing.SplitHearingRejectedException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class SplitHearingRejectedExceptionMapperTest {

    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR = "error";
    private static final String FALLBACK = "fallback";

    @Mock
    private Logger logger;

    @InjectMocks
    private SplitHearingRejectedExceptionMapper mapper;

    @Test
    void shouldReturnProgressionStatusWithErrorCodeAndBodyMessage() {
        final JsonObject body = createObjectBuilder().add(ERROR_CODE, "STALE").add("message", "hearing changed").build();

        final Response response = mapper.toResponse(new SplitHearingRejectedException(409, body, FALLBACK));

        assertThat(response.getStatus(), is(409));
        assertThat(response.getMediaType().toString(), is("application/json"));
        assertThat(response.getEntity().toString(), is("{\"errorCode\":\"STALE\",\"message\":\"hearing changed\"}"));
    }

    @Test
    void shouldSurfaceProgressionsReasonWhenItReportsItUnderError() {
        final JsonObject body = createObjectBuilder()
                .add(ERROR, "Hearing is resulted and cannot be split")
                .add("id", "0b8ba084-1f3b-4c27-a2b7-8f32d12164f8")
                .build();

        final Response response = mapper.toResponse(new SplitHearingRejectedException(409, body,
                "progression returned 409 for the split of hearing 0b8ba084-1f3b-4c27-a2b7-8f32d12164f8"));

        assertThat(response.getStatus(), is(409));
        assertThat(response.getEntity().toString(), is(
                "{\"errorCode\":\"HEARING_NOT_SPLITTABLE\","
                        + "\"message\":\"Hearing is resulted and cannot be split\","
                        + "\"id\":\"0b8ba084-1f3b-4c27-a2b7-8f32d12164f8\"}"));
    }

    @Test
    void shouldPreferMessageOverErrorWhenProgressionSendsBoth() {
        final JsonObject body = createObjectBuilder()
                .add("message", "framework message")
                .add(ERROR, "domain error")
                .build();

        final Response response = mapper.toResponse(new SplitHearingRejectedException(409, body, FALLBACK));

        assertThat(response.getEntity().toString(),
                is("{\"errorCode\":\"HEARING_NOT_SPLITTABLE\",\"message\":\"framework message\"}"));
    }

    @Test
    void shouldCodeAConflictThatProgressionLeftUncoded() {
        final Response response = mapper.toResponse(new SplitHearingRejectedException(409, null, "progression said no"));

        assertThat(response.getEntity().toString(),
                is("{\"errorCode\":\"HEARING_NOT_SPLITTABLE\",\"message\":\"progression said no\"}"));
    }

    @Test
    void shouldKeepProgressionsOwnCodeInsteadOfTheConflictDefault() {
        final JsonObject body = createObjectBuilder().add(ERROR_CODE, "STALE").add(ERROR, "stale read").build();

        final Response response = mapper.toResponse(new SplitHearingRejectedException(409, body, FALLBACK));

        assertThat(response.getEntity().toString(), is("{\"errorCode\":\"STALE\",\"message\":\"stale read\"}"));
    }

    @Test
    void shouldFallBackToExceptionMessageWhenBodyHasNoMessage() {
        final JsonObject body = createObjectBuilder().add(ERROR_CODE, "NOT_FOUND").build();

        final Response response = mapper.toResponse(new SplitHearingRejectedException(404, body, FALLBACK));

        assertThat(response.getStatus(), is(404));
        assertThat(response.getEntity().toString(), is("{\"errorCode\":\"NOT_FOUND\",\"message\":\"fallback\"}"));
    }

    @Test
    void shouldUseExceptionMessageAndOmitErrorCodeWhenBodyIsNull() {
        final Response response = mapper.toResponse(new SplitHearingRejectedException(500, null, "progression down"));

        assertThat(response.getStatus(), is(500));
        assertThat(response.getEntity().toString(), is("{\"message\":\"progression down\"}"));
    }

    @Test
    void shouldOmitMessageWhenNoneAvailable() {
        final Response response = mapper.toResponse(new SplitHearingRejectedException(400, null, null));

        assertThat(response.getStatus(), is(400));
        assertThat(response.getEntity().toString(), is("{}"));
    }
}
