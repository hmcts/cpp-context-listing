package uk.gov.moj.cpp.listing.query.view.utils;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.IOException;
import java.io.InputStream;

import javax.json.JsonObject;
import javax.json.JsonReader;

public class FileUtil {

    public static JsonObject givenPayload(final String filePath) {
        try (final InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            final JsonReader jsonReader = createReader(inputStream);
            return jsonReader.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
