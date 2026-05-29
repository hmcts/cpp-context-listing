package uk.gov.moj.cpp.listing.common.crownfallback;

import javax.json.JsonObject;

/**
 * Thrown when courtscheduler's /extendmultidayhearing/hearingslots returns a non-200 (typed
 * domain failure such as NO_AVAILABILITY, START_DATE_CHANGE_NOT_ALLOWED, INVALID_DATE_RANGE,
 * or NO_EXISTING_ALLOCATION). Carries the response body so the caller can propagate the
 * errorCode + unavailableDates without re-parsing.
 */
public class CrownMultiDayExtensionException extends RuntimeException {

    private final int httpStatus;
    private final JsonObject responseBody;

    public CrownMultiDayExtensionException(final int httpStatus, final JsonObject responseBody, final String message) {
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
