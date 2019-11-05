package uk.gov.moj.cpp.listing.query.api;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FileUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.annotation.Handles;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class HearingQueryApiTest {
    

    private static final String PATH_TO_RAML = "src/raml/listing-query-api.raml";
    private static final String NAME = "name:";
    private static final String LISTING_SEARCH = "listing.search";
    private static final String LISTING_RANGE_SEARCH = "listing.range";
    private static final String LISTING_COURT_LIST_PUBLISH_STATUS = "listing.court.list.publish.status";

    private Map<String, String> apiMethodsToHandlerNames;

    @Before
    public void setup() {
        apiMethodsToHandlerNames = stream(HearingQueryApi.class.getMethods())
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
    }

    @Test
    public void testActionNameAndHandleNameAreSame() throws Exception {
        final List<String> ramlActionNames = readLines(new File(PATH_TO_RAML)).stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(NAME))
                .filter(line -> line.contains(LISTING_SEARCH) || line.contains(LISTING_RANGE_SEARCH) || line.contains(LISTING_COURT_LIST_PUBLISH_STATUS))
                .map(line -> line.replaceAll(NAME, "").trim())
                .collect(toList());

        assertThat(apiMethodsToHandlerNames.values(), containsInAnyOrder(ramlActionNames.toArray()));
    }

    @Test
    public void testHandleNamesPassThroughRequester() throws Exception {
        apiMethodsToHandlerNames.forEach((key, value) -> assertThat(HearingQueryApi.class,
                isHandlerClass(QUERY_API)
                        .with(method(key)
                                .thatHandles(value)
                                .withRequesterPassThrough())));
    }


}