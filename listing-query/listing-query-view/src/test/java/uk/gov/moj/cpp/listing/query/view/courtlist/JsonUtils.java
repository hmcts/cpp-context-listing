package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.nio.charset.Charset.defaultCharset;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import uk.gov.justice.services.messaging.JsonEnvelopeWriter;

import java.io.IOException;
import java.io.StringReader;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {

    public static JsonObject getJsonFile(final String filePath) throws IOException {

        final String jsonString = Resources.toString(Resources.getResource(filePath), defaultCharset());

        try (final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    public static void compareJson(final JsonObject actualJsonObject, final JsonObject expectedJsonObject) throws JSONException {

        final JSONObject actual = new JSONObject(actualJsonObject.toString());

        final JSONObject expected = new JSONObject(expectedJsonObject.toString());

        assertEquals(expected, actual, true);
    }

    public static String prettifyJson(final JsonObject jsonObject) {

        return (JsonEnvelopeWriter.writeJsonObject(jsonObject));
    }
}
