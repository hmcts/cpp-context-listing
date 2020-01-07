package uk.gov.moj.cpp.listing.query.view;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;

import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;

import java.time.ZonedDateTime;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class PublishedCourtListToJsonConverter {

    public JsonObject convert(final List<PublishedCourtList> publishedCourtLists) {

        final JsonArray publishedCourtListsArray = toJsonArray(publishedCourtLists,
                publishedCourtList -> {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    builder.add("courtCentreId", publishedCourtList.getCourtCentreId().toString())
                            .add("publishCourtListType", publishedCourtList.getPublishCourtListType().name())
                            .add("startDate", publishedCourtList.getStartDate().toString())
                            .add("lastUpdated", publishedCourtList.getLastUpdated().toString())
                    ;

                    final ZonedDateTime lastExported = publishedCourtList.getLastExported();

                    if (lastExported != null) {
                        builder.add("lastExported", lastExported.toString());
                    }

                    return builder.build();
                });

        return Json.createObjectBuilder()
                .add("publishedCourtLists", publishedCourtListsArray)
                .build();
    }
}
