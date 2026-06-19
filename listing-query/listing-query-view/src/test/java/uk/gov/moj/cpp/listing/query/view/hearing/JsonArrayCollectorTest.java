package uk.gov.moj.cpp.listing.query.view.hearing;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.stream.IntStream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class JsonArrayCollectorTest {

    @Test
    public void toArrayNode() {

        JsonArray result = IntStream.range(0, 2)
                .mapToObj(this::testJsonObject)
                .collect(JsonArrayCollector.toArrayNode());

        assertThat(result.toString(), isJson(allOf(
                withJsonPath("$", hasSize(2)),
                withJsonPath("$[0].hello-0", equalTo("world-0")),
                withJsonPath("$[1].hello-1", equalTo("world-1"))
        )));

    }

    private JsonObject testJsonObject(int itemNumber) {
        return JsonObjects.createObjectBuilder().add("hello-" + itemNumber, "world-" + itemNumber).build();
    }
}