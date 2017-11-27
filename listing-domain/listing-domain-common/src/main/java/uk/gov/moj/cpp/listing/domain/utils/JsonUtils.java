package uk.gov.moj.cpp.listing.domain.utils;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

    private static final String EMPTY = "";

    private JsonUtils() {
    }

    public static String getString(final JsonObject jsonObject, final String key) {
        return valueExists(jsonObject, key) ? jsonObject.getString(key) : EMPTY;
    }

    public static <E extends Enum<E>> Optional<E> getEnum(final JsonObject jsonObject, final String key, Class<E> clazz) {
        try {
            return Optional.of(E.valueOf(clazz, getString(jsonObject, key)));
        } catch (final IllegalArgumentException ex) {
            LOGGER.warn("Invalid Key {} and exception is {} ", key, ex);
            return Optional.empty();
        }
    }

    public static Optional<LocalDate> getLocalDate(final JsonObject jsonObject, final String key) {
        final String dateString = getString(jsonObject, key);
        return dateString.isEmpty() ? Optional.empty() : Optional.ofNullable(LocalDates.from(dateString));
    }

    public static String getStringFromArray(final JsonObject jsonObject, final String arrayKey,
                                            final int index, final String valueKey) {
        final JsonArray jsonArray = jsonObject.getJsonArray(arrayKey);
        if (jsonArray != null && !JsonValue.NULL.equals(jsonArray) && jsonArray.size() > index) {
            return getString(jsonArray.getJsonObject(index), valueKey);
        }
        return "";
    }

    public static Optional<ZonedDateTime> getZonedDateTime(final JsonObject jsonObject, final String key) {
        final String dateString = getString(jsonObject, key);
        return dateString.isEmpty() ? Optional.empty() : Optional.of(ZonedDateTimes.fromString(dateString));
    }

    public static String getIntAsString(final JsonObject jsonObject, final String key) {
        return valueExists(jsonObject, key) ? String.valueOf(jsonObject.getInt(key)) : EMPTY;
    }

    public static Optional<Integer> getInteger(final JsonObject jsonObject, final String key) {
        return valueExists(jsonObject, key) ? Optional.of(Integer.valueOf(jsonObject.getInt(key))
        ) : Optional.empty();
    }

    private static boolean valueExists(final JsonObject jsonObject, final String key) {
        return jsonObject.containsKey(key) && !Objects.equals(jsonObject.get(key), JsonValue.NULL);
    }
}
