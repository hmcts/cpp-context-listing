package uk.gov.moj.cpp.listing.domain.utils;

import static org.junit.Assert.assertEquals;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonUtilsTest {

    private static final String EMPTY = "";
    private static final String FROM_DATE = "2017-07-15";
    private static final String TO_DATE = "2017-08-15";

    private static JsonObject createPayload(final String key, final String value) {
        return Json.createObjectBuilder()
                .add(key, value)
                .build();
    }

    private static JsonObject createEmptyPayload() {
        return Json.createObjectBuilder()
                .build();
    }

    @Test
    public void shouldGetString() throws Exception {
        final String id = "7e2f843e-d639-40b3-8611-8015f3a18958";
        final JsonObject jsonObject = createPayload("id", id);

        final String result = JsonUtils.getString(jsonObject, "id");

        assertEquals(id, result);
    }

    @Test
    public void shouldReturnEmptyStringIfNotFound() throws Exception {
        final JsonObject jsonObject = createEmptyPayload();

        final String result = JsonUtils.getString(jsonObject, "id");

        assertEquals(EMPTY, result);
    }

    @Test
    public void shouldGetStringFromArray() {
        final String expected = "ASBOGranted";
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseMarkers", Json.createArrayBuilder().add(createPayload("caseMarker", expected)))
                .build();

        final String caseMarker = JsonUtils.getStringFromArray(jsonObject, "caseMarkers", 0, "caseMarker");

        assertEquals(expected, caseMarker);
    }

    @Test
    public void shouldReturnEmptyStringIfNotFoundInArray() {
        final JsonObject jsonObject = createEmptyPayload();

        final String caseMarker = JsonUtils.getStringFromArray(jsonObject, "caseMarkers", 1, "caseMarker");

        assertEquals(EMPTY, caseMarker);
    }

    @Test
    public void shouldReturnEmptyStringForJsonNull() throws Exception {
        final JsonObject jsonNull = Json.createObjectBuilder().add("id", JsonValue.NULL).build();

        final String result = JsonUtils.getString(jsonNull, "id");

        assertEquals(EMPTY, result);
    }

    @Test
    public void shouldGetLocalDate() throws Exception {
        final LocalDate expected = LocalDates.from("2016-02-15");
        final JsonObject jsonObject = createPayload("postingDate", expected.toString());

        final Optional<LocalDate> result = JsonUtils.getLocalDate(jsonObject, "postingDate");

        assertEquals(Optional.of(expected), result);
    }

    @Test
    public void shouldReturnEmptyStringIfLocalDateNotFound() throws Exception {
        final JsonObject jsonObject = createEmptyPayload();

        final Optional<LocalDate> result = JsonUtils.getLocalDate(jsonObject, "postingDate");

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void shouldGetZonedDateTime() throws Exception {
        final ZonedDateTime expected = ZonedDateTimes.fromString("2016-09-07T10:26:03.656Z");
        final JsonObject jsonObject = createPayload("dateTimeCreated", expected.toString());

        final Optional<ZonedDateTime> actual = JsonUtils.getZonedDateTime(jsonObject, "dateTimeCreated");

        assertEquals(Optional.of(expected), actual);
    }

    @Test
    public void shouldReturnEmptyStringIfZonedDateTimeNotFound() throws Exception {
        final JsonObject jsonObject = createEmptyPayload();

        final Optional<ZonedDateTime> actual = JsonUtils.getZonedDateTime(jsonObject, "dateTimeCreated");

        assertEquals(Optional.empty(), actual);
    }

    @Test
    public void shouldGetIntAsString() throws Exception {
        final int expected = 2;
        final JsonObject jsonObject = Json.createObjectBuilder().add("version", expected).build();

        final String result = JsonUtils.getIntAsString(jsonObject, "version");

        assertEquals(String.valueOf(expected), result);
    }

    @Test
    public void shouldGetEmptyStringForGetIntAsString() throws Exception {
        final JsonObject jsonObject = createEmptyPayload();

        final String result = JsonUtils.getIntAsString(jsonObject, "sd");

        assertEquals(EMPTY, result);
    }
}