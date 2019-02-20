package uk.gov.moj.cpp.listing.persistence.repository.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.Charset.defaultCharset;
import static javax.json.Json.createReader;
import static org.junit.Assert.fail;

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

    public static JsonObject givenPayload(final String filePath) {
        try (final InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            final JsonReader jsonReader = createReader(inputStream);
            return jsonReader.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode buildHearingListed(final String path) {
        String hearingJsonObject = getPayload(path);
        return JacksonUtil.toJsonNode(hearingJsonObject);
    }
}
