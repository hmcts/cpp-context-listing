package uk.gov.moj.cpp.listing.common.crownfallback;

import static java.util.Collections.emptyList;
import static uk.gov.justice.services.messaging.JsonObjects.getList;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonString;

public class CrownMultiDayExtensionException extends RuntimeException {

    private final int httpStatus;
    private final JsonObject responseBody;
    private final String errorCode;
    private final List<String> unavailableDates;

    public CrownMultiDayExtensionException(final int httpStatus, final JsonObject responseBody, final String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.errorCode = responseBody == null ? null : getString(responseBody, "errorCode").orElse(null);
        this.unavailableDates = responseBody == null
                ? emptyList()
                : getList(responseBody, JsonString.class, JsonString::getString, "unavailableDates").orElse(emptyList());
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

    public List<String> getUnavailableDates() {
        return unavailableDates;
    }
}
