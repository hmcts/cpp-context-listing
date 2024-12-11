package uk.gov.moj.cpp.listing.query.view;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingQueryViewDummy {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryViewDummy.class);
    private static final String COURT_CALENDAR_HEARINGS = "listing.search.hearings.court-calendar-hearings";

    @Handles(COURT_CALENDAR_HEARINGS)
    public JsonEnvelope dummyResponse(final JsonEnvelope envelope) {
        int pagNumber = envelope.payloadAsJsonObject().getInt("pageNumber", 1);

        if (pagNumber < 1) {
            pagNumber = 1;
        }

        if (pagNumber > 2) {
            pagNumber = 2;
        }

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder(loadDummyJson(pagNumber));

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private JsonObject loadDummyJson(int pageSize) {
        final String fileName = "/json/courtCalendarDummyResponse%s.json".formatted(pageSize);
        try (JsonReader jsonReader = Json.createReader(HearingQueryViewDummy.class.getResourceAsStream(fileName))) {
            return jsonReader.readObject();
        }
    }

}
