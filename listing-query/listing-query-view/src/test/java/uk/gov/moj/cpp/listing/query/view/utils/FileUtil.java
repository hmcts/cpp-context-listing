package uk.gov.moj.cpp.listing.query.view.utils;

import static javax.json.Json.createReader;

import java.io.IOException;
import java.io.InputStream;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
