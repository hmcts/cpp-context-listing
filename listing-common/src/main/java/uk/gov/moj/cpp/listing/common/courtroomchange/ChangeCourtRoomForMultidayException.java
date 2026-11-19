package uk.gov.moj.cpp.listing.common.courtroomchange;

import static uk.gov.justice.services.messaging.JsonObjects.getString;

import javax.json.JsonObject;

/**
 * Raised when courtscheduler rejects a change-court-room-for-multiday-hearing request (422), or
 * when a legacy courtscheduler release signals no-session as a bare 404 (normalised to 422
 * NO_SESSION_FOUND by the caller). Carries the upstream HTTP status and body so
 * {@code ChangeCourtRoomForMultidayExceptionMapper} can render an equivalent response back to the
 * caller.
 */
public class ChangeCourtRoomForMultidayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final transient JsonObject responseBody;
    private final String errorCode;

    public ChangeCourtRoomForMultidayException(final int httpStatus, final JsonObject responseBody, final String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.errorCode = responseBody == null ? null : getString(responseBody, "errorCode").orElse(null);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public JsonObject getResponseBody() {
        return responseBody;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
