package uk.gov.moj.cpp.listing.utils;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PropertyUtil {

    private static final String ENDPOINT_PROPERTIES_FILE = "endpoint.properties";
    protected static Properties ENDPOINT_PROPERTIES;
    private static Logger LOGGER = LoggerFactory.getLogger(PropertyUtil.class);

    public static Properties readConfig() {

        if (null == ENDPOINT_PROPERTIES) {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try (final InputStream stream = loader.getResourceAsStream(ENDPOINT_PROPERTIES_FILE)) {
                ENDPOINT_PROPERTIES = new Properties();
                ENDPOINT_PROPERTIES.load(stream);
            } catch (final IOException e) {
                LOGGER.warn("Error reading properties from {}", ENDPOINT_PROPERTIES_FILE, e);
            }
        }
        return ENDPOINT_PROPERTIES;

    }

    public static String getBaseUri() {
        String baseUriProp = System.getProperty("INTEGRATION_HOST_KEY");
        return isNotEmpty(baseUriProp) ? format("http://%s:8080", baseUriProp) : readConfig().getProperty("base-uri");
    }
}
