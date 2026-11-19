package uk.gov.moj.cpp.listing.testutil;

import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import jakarta.json.JsonObject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Test-only stand-ins for the framework's JsonEnvelope matchers. The Spring query services
 * return the payload only, so the metadata part of a {@code jsonEnvelope(metadata, payload)}
 * expectation is not checked (the legacy envelope name/metadata assertions are dropped) and the
 * payload part is applied to the returned {@link JsonObject}.
 */
public final class EnvelopeMatchers {

    private EnvelopeMatchers() {
    }

    public static Matcher<JsonObject> jsonEnvelope(final MetadataMatcher metadata, final Matcher<JsonObject> payload) {
        return payload;
    }

    public static MetadataMatcher metadata() {
        return new MetadataMatcher();
    }

    public static MetadataMatcher withMetadataEnvelopedFrom(final Object source) {
        return new MetadataMatcher();
    }

    public static Matcher<JsonObject> payloadIsJson(final Matcher<? super ReadContext> matcher) {
        return new PayloadMatcher().isJson(matcher);
    }

    public static PayloadMatcher payload() {
        return new PayloadMatcher();
    }

    /** No-op fluent metadata expectation. */
    public static final class MetadataMatcher {
        public MetadataMatcher withName(final String name) { return this; }
        public MetadataMatcher withUserId(final String userId) { return this; }
        public MetadataMatcher withId(final Object id) { return this; }
        public MetadataMatcher withCausationIds(final Object... ids) { return this; }
        public MetadataMatcher withStreamId(final Object id) { return this; }
    }

    /** Applies a json-path matcher to the payload's JSON text. */
    public static final class PayloadMatcher {
        public Matcher<JsonObject> isJson(final Matcher<? super ReadContext> matcher) {
            final Matcher<Object> json = JsonPathMatchers.isJson(matcher);
            return new TypeSafeMatcher<>() {
                @Override
                protected boolean matchesSafely(final JsonObject item) {
                    return json.matches(item.toString());
                }

                @Override
                public void describeTo(final Description description) {
                    description.appendText("payload ").appendDescriptionOf(json);
                }

                @Override
                protected void describeMismatchSafely(final JsonObject item, final Description mismatch) {
                    json.describeMismatch(item.toString(), mismatch);
                }
            };
        }
    }
}
