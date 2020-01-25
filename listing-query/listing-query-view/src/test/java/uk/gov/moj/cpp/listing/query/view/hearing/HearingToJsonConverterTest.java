package uk.gov.moj.cpp.listing.query.view.hearing;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.vladmihalcea.hibernate.type.json.internal.JacksonUtil.toJsonNode;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class HearingToJsonConverterTest {

    private static final UUID ID = UUID.fromString("7c5e9d0c-9e28-46a9-b139-68fc0813842c");

    @Test
    public void shouldReturnTheExpectedJsonWhenItHasNoProperties() {

        final Hearing hearing = new Hearing(ID, toJsonNode("{}"));

        final JsonObject hearingAsJson = HearingToJsonConverter.convert(hearing);

        assertThat(hearingAsJson, payloadIsJson(withJsonPath("$.size()", equalTo(0))));

    }

    @Test
    public void shouldReturnTheExpectedJsonWhenItHasNullProperties() {

        final Hearing hearing = new Hearing(ID, null);

        final JsonObject hearingAsJson = HearingToJsonConverter.convert(hearing);

        assertNull(hearingAsJson);
    }

    @Test
    public void shouldReturnTheExpectedJsonWhenItHasProperties() throws IOException {

        final String payload = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/hearing.json"));
        final Hearing hearing = new Hearing(ID, toJsonNode(payload));

        final JsonObject hearingAsJson = HearingToJsonConverter.convert(hearing);

        assertThat(hearingAsJson, payloadIsJson(withJsonPath("$.size()", equalTo(17))));
        assertThat(hearingAsJson, payloadIsJson(withJsonPath("$.id", equalTo("7c5e9d0c-9e28-46a9-b139-68fc0813842c"))));

    }
}