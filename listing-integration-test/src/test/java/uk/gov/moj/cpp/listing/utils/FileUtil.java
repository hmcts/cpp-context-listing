package uk.gov.moj.cpp.listing.utils;

import static java.nio.charset.Charset.defaultCharset;
import static javax.json.Json.createReader;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject givenPayload(final String filePath) throws IOException {
        try (final InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            final JsonReader jsonReader = createReader(inputStream);
            return jsonReader.readObject();
        }
    }

    public static JsonObject payloadToObject(final String payload) throws IOException {
        //InputStream inputStream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
        try ( final InputStream inputStream = new ByteArrayInputStream(payload.getBytes()) ) {
            final JsonReader jsonReader = createReader(inputStream);
            return jsonReader.readObject();
        }
    }
}
