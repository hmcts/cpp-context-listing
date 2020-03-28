package uk.gov.moj.cpp.listing.domain.transformation.helper;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import junit.framework.TestCase;

public class JsonPathHelperTest extends TestCase {

    public static final String PAYLOAD_JSON = "src/test/resources/sample-helper-payload.json";

    public void testGetValueForJsonPathReturningSingleValue() throws IOException {
        final String payload = readFileToString(new File(PAYLOAD_JSON));
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getFirstValueForJsonPath(payload, "$.prompts[0].id");
        assertThat(firstValueForJsonPath.get(), is("d6caa3c4-ec9d-41ec-8f86-2c617ef0d5d9"));
    }

    public void testGetFirstValueForJsonPathReturningList() throws IOException {
        final String payload = readFileToString(new File(PAYLOAD_JSON));
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getFirstValueForJsonPath(payload, "$..label");
        assertThat(firstValueForJsonPath.get(), is("value"));
    }

    public void testGetLastValueForJsonPathReturningList() throws IOException {
        final String payload = readFileToString(new File(PAYLOAD_JSON));
        final Optional<String> firstValueForJsonPath = JsonPathHelper.getLastValueForJsonPath(payload, "$..label");
        assertThat(firstValueForJsonPath.get(), is("prompt label 2"));
    }
}