package uk.gov.moj.cpp.listing.common.outbox;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishedCourtListStored;
import uk.gov.justice.services.core.annotation.ServiceComponentLocation;
import uk.gov.justice.services.core.dispatcher.Dispatcher;
import uk.gov.justice.services.core.dispatcher.DispatcherCache;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.subscription.domain.subscriptiondescriptor.Event;
import uk.gov.justice.subscription.domain.subscriptiondescriptor.Subscription;
import uk.gov.justice.subscription.registry.SubscriptionsDescriptorsRegistry;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventRecorderTest {

    private static final String STORED = "listing.event.published-court-list-stored";
    private static final String EXPORT_SUCCESSFUL = "listing.event.publish-court-list-export-successful";

    @Mock
    private DispatcherCache dispatcherCache;

    @Mock
    private Dispatcher eventListener;

    @Mock
    private SubscriptionsDescriptorsRegistry subscriptionsDescriptorsRegistry;

    @Mock
    private EventOutboxWriter eventOutboxWriter;

    @InjectMocks
    private EventRecorder eventRecorder;

    private final JsonEnvelope command = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.command.x"),
            JsonObjects.createObjectBuilder());

    @BeforeEach
    void setUp() {
        when(dispatcherCache.dispatcherFor("EVENT_LISTENER", ServiceComponentLocation.LOCAL)).thenReturn(eventListener);
        // listener subscribes to both events; processor only to "stored"
        givenSubscription("event listener subscription", STORED, EXPORT_SUCCESSFUL);
        givenSubscription("event processor subscription", STORED);
        when(eventOutboxWriter.envelopeFor(any(), any())).thenAnswer(invocation -> {
            final DomainEvent event = invocation.getArgument(1);
            return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(event.name()).withStreamId(event.streamId()),
                    JsonObjects.createObjectBuilder());
        });
        eventRecorder.initialise();
    }

    @Test
    void runsTheListenerAndWritesTheOutboxForAProcessorEvent() {
        final UUID courtListId = randomUUID();

        eventRecorder.record(command, courtListId, List.of(PublishedCourtListStored.publishedCourtListStored().build()));

        final ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventOutboxWriter).envelopeFor(eq(command.metadata()), event.capture());
        assertThat(event.getValue().name(), is(STORED));
        assertThat(event.getValue().streamId(), is(courtListId));

        final ArgumentCaptor<JsonEnvelope> dispatched = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(eventListener).dispatch(dispatched.capture());
        assertThat(dispatched.getValue().metadata().name(), is(STORED));
        verify(eventOutboxWriter).append(any());
    }

    @Test
    void doesNotWriteTheOutboxForAnEventTheProcessorDoesNotSubscribeTo() {
        eventRecorder.record(command, randomUUID(), List.of(PublishCourtListExportSuccessful.publishCourtListExportSuccessful().build()));

        verify(eventListener).dispatch(any());
        verify(eventOutboxWriter, never()).append(any());
    }

    @Test
    void rejectsAPojoWithoutAnEventAnnotation() {
        assertThrows(IllegalArgumentException.class, () -> eventRecorder.record(command, randomUUID(), List.of("not an event")));
    }

    private void givenSubscription(final String name, final String... events) {
        final List<Event> subscribed = new java.util.ArrayList<>();
        for (final String eventName : events) {
            final Event event = org.mockito.Mockito.mock(Event.class);
            when(event.getName()).thenReturn(eventName);
            subscribed.add(event);
        }
        final Subscription subscription = org.mockito.Mockito.mock(Subscription.class);
        when(subscription.getEvents()).thenReturn(subscribed);
        when(subscriptionsDescriptorsRegistry.getSubscriptionFor(name)).thenReturn(subscription);
    }
}
