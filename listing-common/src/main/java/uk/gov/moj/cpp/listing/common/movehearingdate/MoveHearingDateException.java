package uk.gov.moj.cpp.listing.common.movehearingdate;

import static uk.gov.justice.services.messaging.JsonObjects.getString;

import javax.json.JsonObject;

/**
 * Raised when courtscheduler rejects a move-hearing-date request (422/404), or when the
 * listing side rejects the request before ever calling courtscheduler (unknown hearingId, future
 * date on the CROWN listing-side path). Carries the upstream HTTP status and body so the
 * {@code MoveHearingDateExceptionMapper} can render an equivalent response back to the
 * caller.
 */
public class MoveHearingDateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final transient JsonObject responseBody;
    private final String errorCode;

    public MoveHearingDateException(final int httpStatus, final JsonObject responseBody, final String message) {
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
