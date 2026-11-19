package uk.gov.moj.cpp.listing.command.hearing.handler.testsupport;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.mockito.ArgumentCaptor;

/**
 * The framework test-utils the ported JEE handler tests used ({@code EnvelopeFactory},
 * {@code MetadataBuilderFactory}, {@code EventStreamMockHelper}, the {@code JsonEnvelope*Matcher}s,
 * {@code ReflectionUtil}), on the framework-free {@link Envelope} (MBD refactor P2.3.2).
 */
public final class FrameworkTestSupport {

    private FrameworkTestSupport() {
    }

    public static Metadata.Builder metadataWithRandomUUID(final String name) {
        return Metadata.builder().withId(randomUUID()).withName(name);
    }

    public static Envelope envelopeFrom(final Metadata.Builder metadata, final JsonValue payload) {
        return envelopeFrom(metadata.build(), payload);
    }

    public static Envelope envelopeFrom(final Metadata metadata, final JsonValue payload) {
        return new Envelope(metadata, payload == null || payload == JsonValue.NULL ? JsonValue.EMPTY_JSON_OBJECT : payload.asJsonObject());
    }

    public static Envelope createEnvelope(final String name, final JsonObject payload) {
        return envelopeFrom(metadataWithRandomUUID(name), payload);
    }

    /** {@code EventStreamMockHelper.verifyAppendAndGetArgumentFrom}: the one stream appended to {@code eventStream}. */
    @SuppressWarnings("unchecked")
    public static Stream<Envelope> verifyAppendAndGetArgumentFrom(final EventStream eventStream) {
        final ArgumentCaptor<Stream<Envelope>> captor = ArgumentCaptor.forClass((Class) Stream.class);
        verify(eventStream).append(captor.capture());
        return captor.getValue();
    }

    @SafeVarargs
    public static Matcher<Stream<Envelope>> streamContaining(final Matcher<? super Envelope>... matchers) {
        final Matcher<Iterable<? extends Envelope>> contains = Matchers.contains(Arrays.asList(matchers).toArray(new Matcher[0]));
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(final Stream<Envelope> stream, final Description mismatch) {
                final List<Envelope> envelopes = stream.collect(Collectors.toList());
                if (!contains.matches(envelopes)) {
                    contains.describeMismatch(envelopes, mismatch);
                    return false;
                }
                return true;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("a stream of ").appendDescriptionOf(contains);
            }
        };
    }

    public static Matcher<Envelope> jsonEnvelope(final MetadataMatcher metadata, final Matcher<Envelope> payload) {
        return Matchers.allOf(metadata, payload);
    }

    public static MetadataMatcher withMetadataEnvelopedFrom(final Envelope cause) {
        return new MetadataMatcher(cause);
    }

    /** {@code JsonEnvelopeMetadataMatcher.metadata()}: any metadata (narrowed with {@code withName}). */
    public static MetadataMatcher metadata() {
        return new MetadataMatcher(null);
    }

    public static PayloadMatcher payload() {
        return new PayloadMatcher(null);
    }

    public static PayloadMatcher payloadIsJson(final Matcher<? super com.jayway.jsonpath.ReadContext> json) {
        return new PayloadMatcher(com.jayway.jsonpath.matchers.JsonPathMatchers.isJson(json));
    }

    public static void setField(final Object target, final String name, final Object value) {
        try {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    final Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (final NoSuchFieldException e) {
                    type = type.getSuperclass();
                }
            }
            throw new IllegalArgumentException("No field " + name);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code withMetadataEnvelopedFrom(cause).withName(name)}: same user and session as the cause, the given name. */
    public static final class MetadataMatcher extends TypeSafeDiagnosingMatcher<Envelope> {
        private final Envelope cause;
        private String name;

        private MetadataMatcher(final Envelope cause) {
            this.cause = cause;
        }

        public MetadataMatcher withName(final String name) {
            this.name = name;
            return this;
        }

        private java.util.UUID[] causationIds = new java.util.UUID[0];

        public MetadataMatcher withCausationIds(final java.util.UUID... causationIds) {
            this.causationIds = causationIds;
            return this;
        }

        @Override
        protected boolean matchesSafely(final Envelope envelope, final Description mismatch) {
            final Metadata metadata = envelope.metadata();
            if (name != null && !name.equals(metadata.name())) {
                mismatch.appendText("name was ").appendValue(metadata.name());
                return false;
            }
            if (!metadata.causation().containsAll(Arrays.asList(causationIds))) {
                mismatch.appendText("causation was ").appendValue(metadata.causation());
                return false;
            }
            if (cause != null && (!Objects.equals(cause.metadata().userId(), metadata.userId()) || !Objects.equals(cause.metadata().sessionId(), metadata.sessionId()))) {
                mismatch.appendText("metadata not enveloped from the cause: ").appendValue(metadata);
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("metadata enveloped from ").appendValue(cause == null ? null : cause.metadata().id()).appendText(" named ").appendValue(name);
        }
    }

    /** {@code payload().isJson(matcher)} / {@code payloadIsJson(matcher)}: the payload as a JSON string. */
    public static final class PayloadMatcher extends TypeSafeDiagnosingMatcher<Envelope> {
        private Matcher<? super Object> json;

        private PayloadMatcher(final Matcher<? super Object> json) {
            this.json = json;
        }

        /** The payload JSON matches {@code json} (JSON-path matchers, as the framework's {@code isJson}). */
        public PayloadMatcher isJson(final Matcher<? super com.jayway.jsonpath.ReadContext> json) {
            this.json = com.jayway.jsonpath.matchers.JsonPathMatchers.isJson(json);
            return this;
        }

        @Override
        protected boolean matchesSafely(final Envelope envelope, final Description mismatch) {
            final String payload = envelope.payload().toString();
            if (json != null && !json.matches(payload)) {
                json.describeMismatch(payload, mismatch);
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("payload ").appendDescriptionOf(json);
        }
    }
}
