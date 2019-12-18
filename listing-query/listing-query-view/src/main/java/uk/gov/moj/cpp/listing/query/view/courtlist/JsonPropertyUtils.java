package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class JsonPropertyUtils {

    private JsonPropertyUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Optional<UUID> getOptionalUUID(final JsonObject jsonObject, final String propertyName) {

        final Optional<String> courtRoomIdString = ofNullable(jsonObject.getString(propertyName, null));

        return courtRoomIdString.map(UUID::fromString);
    }
}
