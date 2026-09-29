package uk.gov.moj.cpp.listing.common.splithearing;

import static uk.gov.justice.services.messaging.JsonObjects.getString;

import javax.json.JsonObject;

/**
 * Raised when progression rejects a forwarded split-hearing request. Carries progression's HTTP
 * status and body so {@code SplitHearingRejectedExceptionMapper} can return the same status to the
 * front end, which needs to tell a stale request (409) apart from an unknown hearing (404), a bad
 * payload (400) and progression being down (500).
 */
public class SplitHearingRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final transient JsonObject responseBody;
    private final String errorCode;

    public SplitHearingRejectedException(final int httpStatus, final JsonObject responseBody, final String message) {
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
