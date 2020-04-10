package uk.gov.moj.cpp.listing.domain.transformation.corechanges;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_DEFENDANTS_TO_BE_UPDATED;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform.ListingEventTransformer;
import uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform.TransformFactory;

import java.util.Arrays;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class ListingEventTransformTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventTransformTest.class);

    private ListingEventTransform underTest = new ListingEventTransform();

    @Mock
    private TransformFactory transformFactory;

    @Before
    public void setup() {
        underTest.setTransformFactory(transformFactory);
        when(transformFactory.getEventTransformer(EVENT_DEFENDANTS_TO_BE_UPDATED)).thenReturn(Arrays.asList(mock(ListingEventTransformer.class)));
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(underTest, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetActionToTransformForTheEventsThatMatch() {
        final JsonEnvelope event = buildEnvelope(EVENT_DEFENDANTS_TO_BE_UPDATED);
        assertThat(underTest.actionFor(event), is(TRANSFORM));
    }

    @Test
    public void shouldSetActionToNoActionForTheEventsThatDoesNotMatch() {
        final JsonEnvelope event = buildEnvelope("listing.events.other");
        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldTransformListingInitiatedEvent() {
        JsonObject jsonObject = mock(JsonObject.class);
        final JsonEnvelope event = buildEnvelopeWithPayload(EVENT_DEFENDANTS_TO_BE_UPDATED, jsonObject);
        ListingEventTransformer listingEventTransformer = mock(ListingEventTransformer.class);

        when(transformFactory.getEventTransformer(EVENT_DEFENDANTS_TO_BE_UPDATED)).thenReturn(Arrays.asList(listingEventTransformer));

        underTest.apply(event);

        verify(listingEventTransformer).transform(event.metadata(), jsonObject);

    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }


    private JsonEnvelope buildEnvelopeWithPayload(final String eventName, final JsonObject jsonObject) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                jsonObject);
    }
}
