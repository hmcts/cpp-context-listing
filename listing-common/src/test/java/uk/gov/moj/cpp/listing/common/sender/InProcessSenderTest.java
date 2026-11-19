package uk.gov.moj.cpp.listing.common.sender;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InProcessSenderTest {

    @Mock
    private InProcessCommandDispatcher inProcessCommandDispatcher;

    @Mock
    private Sender frameworkSender;

    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Mock
    private SystemUserProvider systemUserProvider;

    @InjectMocks
    private InProcessSender sender;

    @Test
    void runsListingCommandsInProcess() {
        final JsonEnvelope command = envelope("listing.command.add-hearing-to-case");

        sender.send(command);

        verify(inProcessCommandDispatcher).dispatch(command);
        verify(frameworkSender, never()).send(any());
    }

    @Test
    void sendsEverythingElseThroughTheFrameworkSender() {
        final JsonEnvelope publicEvent = envelope("public.listing.hearing-confirmed");

        sender.send(publicEvent);

        verify(frameworkSender).send(publicEvent);
        verify(inProcessCommandDispatcher, never()).dispatch(any());
    }

    @Test
    void convertsAPojoPayloadToJson() {
        final Object pojo = new Object();
        final JsonValue json = JsonObjects.createObjectBuilder().add("a", 1).build();
        when(objectToJsonValueConverter.convert(pojo)).thenReturn(json);
        final Envelope<Object> command = Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.command.x").build(), pojo);

        sender.send(command);

        final ArgumentCaptor<JsonEnvelope> dispatched = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(inProcessCommandDispatcher).dispatch(dispatched.capture());
        assertThat(dispatched.getValue().payload(), is(json));
        assertThat(dispatched.getValue().metadata().name(), is("listing.command.x"));
    }

    @Test
    void sendAsAdminRunsTheCommandAsTheSystemUser() {
        final UUID systemUser = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUser));

        sender.sendAsAdmin(envelope("listing.command.record-court-list-export-successful"));

        final ArgumentCaptor<JsonEnvelope> dispatched = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(inProcessCommandDispatcher).dispatch(dispatched.capture());
        assertThat(dispatched.getValue().metadata().userId().orElseThrow(), is(systemUser.toString()));
    }

    private static JsonEnvelope envelope(final String name) {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(name), JsonObjects.createObjectBuilder());
    }
}
