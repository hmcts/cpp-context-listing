package uk.gov.moj.cpp.listing.common.split;

import javax.json.JsonObject;

/**
 * Progression rejected a split. Listing owns no part of the decision, so its status and body are
 * carried back to the caller untouched and surface synchronously in the UI — a stale or invalid
 * split must not look accepted.
 *
 * <p>Thrown rather than returned because a {@code @Handles} method has to return an envelope; the
 * framework rejects the deployment outright otherwise ("Synchronous handler method must handle
 * envelopes"). {@link uk.gov.moj.cpp.listing.command.api.mapper} turns this back into the response.
 */
public class SplitHearingRejectedException extends RuntimeException {

    private final int httpStatus;
    private final JsonObject responseBody;

    public SplitHearingRejectedException(final int httpStatus, final JsonObject responseBody, final String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public JsonObject getResponseBody() {
        return responseBody;
    }
}
