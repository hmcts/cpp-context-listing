package uk.gov.moj.cpp.listing.command.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;

/**
 * Contract guard for the {@code listing.list-next-hearings-v2} command schema.
 *
 * <p>Progression sends {@code bookingReferencesWithCourtScheduleIds} on this command: the
 * bookingReferences it resolved before a re-share freed the previous hearing's slots, which are
 * the only surviving record of which sessions those references held. The schema is
 * {@code additionalProperties: false}, so a listing whose schema does not declare the property
 * rejects the whole command with 400 — before any application code runs, and therefore without
 * logging anything. On STE02 that silently killed every next-hearing listing for hours; the only
 * trace was an access-log line and a DLQ'd message in progression.
 *
 * <p><b>Read from the classpath on purpose.</b> These assertions load the schema from
 * {@code /json/schema/...}, which is where it is packaged into the jar and where the framework
 * reads it at runtime — not from {@code src/raml/...}. The test therefore asserts against the same
 * artefact the running service uses.
 *
 * <p><b>What this cannot catch.</b> It proves the schema in <em>this build</em> accepts the field.
 * It cannot prove the <em>deployed</em> build does: the STE02 failure was a stale image carrying an
 * older schema under an unchanged {@code -SNAPSHOT} version. Catching that needs a deployment-time
 * check, not a unit test.
 */
class ListNextHearingsV2SchemaContractTest {

    private static final String COMMAND_SCHEMA = "/json/schema/listing.list-next-hearings-v2.json";
    private static final String CARRIED_FIELD = "bookingReferencesWithCourtScheduleIds";
    private static final String REFERENCED_SCHEMA = "/json/schema/booking-reference-court-schedule-ids.json";

    @Test
    void shouldDeclareTheCarriedBookingReferencesProperty() {
        final JsonObject properties = schema(COMMAND_SCHEMA).getJsonObject("properties");

        assertThat("progression sends this property; without it the command is rejected with 400",
                propertyNames(properties), hasItem(CARRIED_FIELD));
    }

    /**
     * Documents why the test above matters. If this ever becomes {@code true}, an undeclared
     * property would simply be ignored and the guard above stops being load-bearing — at which
     * point this test should be deleted along with it, deliberately rather than by accident.
     */
    @Test
    void shouldRejectUndeclaredPropertiesSoEveryCarriedFieldMustBeDeclared() {
        assertThat(schema(COMMAND_SCHEMA).getBoolean("additionalProperties", true), is(false));
    }

    @Test
    void shouldCarryTheBookingReferencesAsAnArrayOfTheSharedDefinition() {
        final JsonObject field = schema(COMMAND_SCHEMA).getJsonObject("properties").getJsonObject(CARRIED_FIELD);

        assertThat(field.getString("type"), is("array"));
        assertThat(field.getJsonObject("items").getString("$ref"),
                is("http://justice.gov.uk/listing/events/booking-reference-court-schedule-ids.json"));
    }

    /**
     * A {@code $ref} to a schema that is not on the classpath fails at runtime, not at build time.
     * This is the same failure shape as the missing property and just as invisible.
     */
    @Test
    void shouldShipTheSchemaThatTheCarriedPropertyReferences() {
        final JsonObject referenced = schema(REFERENCED_SCHEMA);

        assertThat("booking-reference-court-schedule-ids.json must be on the classpath",
                referenced, is(notNullValue()));
        assertThat(propertyNames(referenced.getJsonObject("properties")), hasItem("bookingId"));
        assertThat(propertyNames(referenced.getJsonObject("properties")), hasItem("courtScheduleIds"));
    }

    private static JsonObject schema(final String classpathLocation) {
        try (InputStream in = ListNextHearingsV2SchemaContractTest.class.getResourceAsStream(classpathLocation)) {
            assertThat("not on the classpath: " + classpathLocation, in, is(notNullValue()));
            try (JsonReader reader = Json.createReader(in)) {
                return reader.readObject();
            }
        } catch (final Exception e) {
            throw new IllegalStateException("could not read " + classpathLocation, e);
        }
    }

    private static List<String> propertyNames(final JsonObject properties) {
        return new ArrayList<>(properties.keySet());
    }
}
