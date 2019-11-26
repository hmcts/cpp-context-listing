package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static uk.gov.justice.services.common.converter.LocalDates.from;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.valueOf;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

public class PublishCourtListRequestParametersParser {

    public PublishCourtListRequestParameters parse(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        return new PublishCourtListRequestParameters(
                UUID.fromString(payload.getString("publishCourtListRequestId")),
                UUID.fromString(payload.getString("courtCentreId")),
                from(payload.getString("startDate")),
                from(payload.getString("endDate")),
                valueOf(payload.getString("publishCourtListType")),
                fromString(payload.getString("requestedTime")));
    }
}
