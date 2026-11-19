package uk.gov.moj.cpp.listing.common.sender;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonValue;

/**
 * A {@link Sender} that runs listing's own commands in-VM (MBD refactor P1g).
 *
 * <p>Envelopes named {@code listing.command.*} (every {@code listing.handler.command} action) go
 * to {@link InProcessCommandDispatcher}, synchronously, in the caller's transaction (D5). Anything
 * else (the processor's {@code public.*} events) goes to the framework {@code Sender}, as before.
 *
 * <p>Inject with {@code @Inject @InProcess Sender sender}. Unit tests that mock {@code Sender}
 * still inject their mock by type.
 *
 * <p>The delegate is the {@code EVENT_PROCESSOR} framework {@code Sender}, selected with a
 * field-level {@code @FrameworkComponent} (as {@code PublishCourtListCommandSender} did). The
 * command API never sends anything but listing commands, so only the processor reaches it. The
 * annotation must not go on the class: the framework then registers the class as a handler and
 * fails deployment because it has no {@code @Handles} methods.
 */
@ApplicationScoped
@InProcess
public class InProcessSender implements Sender {

    static final String LISTING_COMMAND_PREFIX = "listing.command.";

    @Inject
    private InProcessCommandDispatcher inProcessCommandDispatcher;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender frameworkSender;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Override
    public void send(final Envelope<?> envelope) {
        if (isListingCommand(envelope)) {
            inProcessCommandDispatcher.dispatch(asJsonEnvelope(envelope));
        } else {
            frameworkSender.send(envelope);
        }
    }

    @Override
    public void sendAsAdmin(final JsonEnvelope envelope) {
        sendAsAdmin((Envelope<?>) envelope);
    }

    @Override
    public void sendAsAdmin(final Envelope<?> envelope) {
        if (isListingCommand(envelope)) {
            inProcessCommandDispatcher.dispatch(asSystemUser(asJsonEnvelope(envelope)));
        } else {
            frameworkSender.sendAsAdmin(envelope);
        }
    }

    static boolean isListingCommand(final Envelope<?> envelope) {
        return envelope.metadata().name().startsWith(LISTING_COMMAND_PREFIX);
    }

    private JsonEnvelope asJsonEnvelope(final Envelope<?> envelope) {
        if (envelope instanceof JsonEnvelope jsonEnvelope) {
            return jsonEnvelope;
        }
        final Object payload = envelope.payload();
        final JsonValue json = payload instanceof JsonValue jsonValue ? jsonValue : objectToJsonValueConverter.convert(payload);
        return envelopeFrom(envelope.metadata(), json);
    }

    private JsonEnvelope asSystemUser(final JsonEnvelope envelope) {
        return systemUserProvider.getContextSystemUserId()
                .map(systemUserId -> envelopeFrom(metadataFrom(envelope.metadata()).withUserId(systemUserId.toString()), envelope.payload()))
                .orElse(envelope);
    }
}
