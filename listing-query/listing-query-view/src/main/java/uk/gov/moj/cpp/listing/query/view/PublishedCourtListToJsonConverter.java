package uk.gov.moj.cpp.listing.query.view;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;

import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.JsonNode;

public class PublishedCourtListToJsonConverter {

    public JsonObject convert(final List<PublishedCourtList> publishedCourtLists) {

        final JsonArray publishedCourtListsArray = toJsonArray(publishedCourtLists, this::convert);

        return Json.createObjectBuilder()
                .add("publishedCourtLists", publishedCourtListsArray)
                .build();
    }

    public JsonObject convert(final PublishedCourtList publishedCourtList) {

        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add("courtCentreId", publishedCourtList.getCourtCentreId().toString())
                .add("publishCourtListType", publishedCourtList.getPublishCourtListType().name())
                .add("courtListJson", jsonFromJsonNode(publishedCourtList.getCourtListJson()))
                .add("startDate", publishedCourtList.getStartDate().toString())
                .add("lastUpdated", publishedCourtList.getLastUpdated().toString())
        ;

        final ZonedDateTime lastExported = publishedCourtList.getLastExported();

        if (lastExported != null) {
            builder.add("lastExported", lastExported.toString());
        }

        return builder.build();
    }

    private static JsonObject jsonFromJsonNode(final JsonNode rawJson) {
        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(rawJson.toString()))) {
            object = jsonReader.readObject();
            return object;
        }
    }
}
