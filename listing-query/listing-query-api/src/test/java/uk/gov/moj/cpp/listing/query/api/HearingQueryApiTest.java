package uk.gov.moj.cpp.listing.query.api;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.io.FileUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonValue;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingQueryApiTest {


    private static final String PATH_TO_RAML = "src/raml/listing-query-api.raml";
    private static final String NAME = "name:";
    private static final String LISTING_SEARCH = "listing.search";
    private static final String LISTING_SEARCH_HEARING = "listing.search.hearing";
    private static final String LISTING_RANGE_SEARCH = "listing.range";
    private static final String LISTING_COURT_LIST_PUBLISH_STATUS = "listing.court.list.publish.status";
    private static final String HEARING_SLOTS = "listing.search.hearing.slots";

    private static final List<String> METHODS_WHICH_ARE_NOT_MERELY_PASS_THROUGH = ImmutableList.of("searchForHearingById", "searchHearingSlots");

    private Map<String, String> apiMethodsToHandlerNames;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Requester requester;

    @InjectMocks
    private HearingQueryApi hearingQueryApi;

    @Before
    public void setup() {
        apiMethodsToHandlerNames = stream(HearingQueryApi.class.getMethods())
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
    }

    @Test
    public void ensureThatActionAndHandlerNamesMatchTheRaml() throws Exception {
        final List<String> ramlActionNames = readLines(new File(PATH_TO_RAML)).stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(NAME))
                .filter(line -> !line.contains(HEARING_SLOTS))
                .filter(line -> line.contains(LISTING_SEARCH) || line.contains(LISTING_RANGE_SEARCH) || line.contains(LISTING_COURT_LIST_PUBLISH_STATUS) || line.contains(LISTING_SEARCH_HEARING))
                .map(line -> line.replaceAll(NAME, "").trim())
                .collect(toList());

        assertThat(apiMethodsToHandlerNames.values(), containsInAnyOrder(ramlActionNames.toArray()));
    }

    @Test
    public void ensureThatSomeOfTheMethodsArePassthroughs() {
        apiMethodsToHandlerNames
                .entrySet()
                .stream()
                .filter(entry -> !METHODS_WHICH_ARE_NOT_MERELY_PASS_THROUGH.contains(entry.getKey()))
                .forEach(entry ->
                        assertThat(HearingQueryApi.class,
                                isHandlerClass(QUERY_API)
                                        .with(method(entry.getKey())
                                                .thatHandles(entry.getValue())
                                                .withRequesterPassThrough())));

    }

    @Test
    public void searchForHearingByIdWhenIdIsNotPresent() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Attempted to search for a Hearing without an ID.");

        final JsonEnvelope proposedQuery = generateQuery(createObjectBuilder().build());

        hearingQueryApi.searchForHearingById(proposedQuery);

    }

    @Test
    public void searchForHearingByIdWhenIdIsNotValid() {

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Please ensure that the id is a valid UUID");

        final JsonEnvelope proposedQuery = generateQuery(
                createObjectBuilder()
                        .add("id", "849afced-85b")
                        .build());


        hearingQueryApi.searchForHearingById(proposedQuery);

    }

    @Test
    public void searchForHearingByIdWhenIdIsValid() {

        final JsonEnvelope proposedQuery = generateQuery(
                createObjectBuilder()
                        .add("id", "d279360b-c42d-481a-8f54-9f7985bf2df1")
                        .build());

        final JsonEnvelope envelopeReturnedFromRequester = mock(JsonEnvelope.class);
        when(requester.request(proposedQuery)).thenReturn(envelopeReturnedFromRequester);

        final JsonEnvelope returnedEnvelope = hearingQueryApi.searchForHearingById(proposedQuery);

        assertSame(returnedEnvelope, envelopeReturnedFromRequester);
    }

    private JsonEnvelope generateQuery(JsonValue jsonValue) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(UUID.fromString("6d4ced64-b058-4bd4-a652-98d8230b92a5"))
                        .withName("_"), jsonValue);

    }
}