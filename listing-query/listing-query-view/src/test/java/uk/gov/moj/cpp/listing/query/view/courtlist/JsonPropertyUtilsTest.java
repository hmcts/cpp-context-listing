package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;

public class JsonPropertyUtilsTest {

    public static final String PROPERTY_NAME = "PROPERTY_NAME";

    @Test
    public void shouldReturnUuidIfPropertyExists() {

        final UUID uuid = randomUUID();

        final JsonObject jsonObject = createObjectBuilder().add(PROPERTY_NAME, uuid.toString()).build();

        final Optional<UUID> optionalUUID = JsonPropertyUtils.getOptionalUUID(jsonObject, PROPERTY_NAME);

        assertThat(optionalUUID, is(Optional.of(uuid)));
    }

    @Test
    public void shouldReturnEmptyIfNoProperty() {

        final JsonObject jsonObject = createObjectBuilder().build();

        assertThat(JsonPropertyUtils.getOptionalUUID(jsonObject, PROPERTY_NAME), is(Optional.empty()));
    }

    @Test(expected = InvocationTargetException.class)
    public void privateConstructorShouldThrowIllegalStateException() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<JsonPropertyUtils> constructor = JsonPropertyUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance(); // This line should throw IllegalStateException
    }
}
