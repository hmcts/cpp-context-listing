package uk.gov.moj.cpp.listing.domain.transformation.ctl;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.moj.cpp.listing.domain.transformation.ctl.ListingEventStreamTransform.CASE_SENT_FOR_LISTING;
import static uk.gov.moj.cpp.listing.domain.transformation.ctl.ListingEventStreamTransform.DEFENDANTS_TO_BE_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.ctl.ListingEventStreamTransform.HEARING_LISTED;
import static uk.gov.moj.cpp.listing.domain.transformation.ctl.ListingEventStreamTransform.NEW_DEFENDANT_DETAILS_UPDATED;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(MockitoJUnitRunner.class)
public class ListingEventStreamTransformTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventStreamTransformTest.class);

    private ListingEventStreamTransform target = new ListingEventStreamTransform();

    private Enveloper enveloper = createEnveloper();

    @Before
    public void setup() {
        target.setEnveloper(enveloper);
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(target, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetActionToTransformForTheEventsThatMatch() {
     /* TODO   final JsonEnvelope event = buildEnvelope(HEARING_EVENTS_INITIATED);
        assertThat(underTest.actionFor(event), is(TRANSFORM));
     */
    }

    @Test
    public void shouldSetActionToNoActionForTheEventsThatDoesNotMatch() {
        final JsonEnvelope event = buildEnvelope("hearing.events.other");
        assertThat(target.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldTransformDefendantToBeUpdatedEvent() {

        final JsonEnvelope inputEnvelope = loadTestFile(DEFENDANTS_TO_BE_UPDATED, DEFENDANTS_TO_BE_UPDATED);

        final JsonEnvelope result = target.apply(inputEnvelope).findFirst().get();

        JsonObject bailStatusOut = result.payloadAsJsonObject().getJsonArray("defendants").getJsonObject(0).getJsonObject("bailStatus");

        System.out.println("bailStatusOut: " + bailStatusOut);

        assertEquals("C", bailStatusOut.getString("code"));

    }

    @Test
    public void shouldTransformNewDefendantDetailsUpdatedEvent() {

        final JsonEnvelope inputEnvelope = loadTestFile(NEW_DEFENDANT_DETAILS_UPDATED, NEW_DEFENDANT_DETAILS_UPDATED);

        final JsonEnvelope result = target.apply(inputEnvelope).findFirst().get();

        JsonObject bailStatusOut = result.payloadAsJsonObject().getJsonObject("defendant").getJsonObject("bailStatus");

        System.out.println("bailStatusOut: " + bailStatusOut);

        assertEquals("C", bailStatusOut.getString("code"));

    }

    @Test
    public void shouldTransformCaseSentForListingEvent() {

        final JsonEnvelope inputEnvelope = loadTestFile(CASE_SENT_FOR_LISTING, CASE_SENT_FOR_LISTING);

        final JsonEnvelope result = target.apply(inputEnvelope).findFirst().get();

        JsonObject bailStatusOut = result.payloadAsJsonObject()
                .getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonObject("bailStatus");

        System.out.println("bailStatusOut: " + bailStatusOut);

        assertEquals("U", bailStatusOut.getString("code"));

    }

    @Test
    public void shouldTransformHearingListedEvent() {

        final JsonEnvelope inputEnvelope = loadTestFile(HEARING_LISTED, HEARING_LISTED);

        final JsonEnvelope result = target.apply(inputEnvelope).findFirst().get();

        JsonObject bailStatusOut = result.payloadAsJsonObject()
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonObject("bailStatus");

        System.out.println("bailStatusOut: " + bailStatusOut);

        assertEquals("B", bailStatusOut.getString("code"));

    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }

    private JsonEnvelope loadTestFile(String eventName, String resourceFileName) {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName + ".json");
            final JsonReader jsonReader = Json.createReader(is);
            final JsonObject payload = jsonReader.readObject();
            return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(eventName), payload);

        } catch (Exception ex) {
            throw new RuntimeException("failed to load test file " + resourceFileName, ex);
        }
    }

}