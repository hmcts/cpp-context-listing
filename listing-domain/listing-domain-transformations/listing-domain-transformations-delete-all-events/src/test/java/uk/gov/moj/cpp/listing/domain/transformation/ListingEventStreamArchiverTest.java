package uk.gov.moj.cpp.listing.domain.transformation;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;


public class ListingEventStreamArchiverTest {

    private ListingEventStreamArchiver underTest = new ListingEventStreamArchiver();

    private Enveloper enveloper = createEnveloper();

    @Before
    public void setup() {
        underTest.setEnveloper(enveloper);
    }

    @Test
    public void shouldCreateInstanceOfEventTransformation() {
        assertThat(underTest, is(instanceOf(EventTransformation.class)));
    }

    @Test
    public void shouldSetDeactivateActionForEvents() {
        final JsonEnvelope event = buildEnvelope("listing.events.judge-assigned-to-hearing");

        assertThat(underTest.actionFor(event), is(DEACTIVATE));
    }

    @Test
    public void shouldSetDeactivateActionForOldHearingListedEvent() {
        final JsonEnvelope event = buildEnvelope("listing.events.hearing-listed");

        assertThat(underTest.actionFor(event), is(DEACTIVATE));
    }

    @Test
    public void shouldSetNoActionForNewHearingListedEvent() {
        final JsonEnvelope event = buildNewHearingListedEnvelope("listing.events.hearing-listed");

        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    @Test
    public void shouldSetNoActionForHearingAddedToCaseEvent() {
        final JsonEnvelope event = buildEnvelope("listing.events.hearing-added-to-case");

        assertThat(underTest.actionFor(event), is(NO_ACTION));
    }

    private JsonEnvelope buildEnvelope(final String eventName) {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                createObjectBuilder().add("field", "value").build());
    }

    private JsonEnvelope buildNewHearingListedEnvelope(final String eventName) {

        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder().add("hearing", Json.createObjectBuilder().add("f", "v"));

        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(eventName),
                jsonBuilder.build());
    }

}
