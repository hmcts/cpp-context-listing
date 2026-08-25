package uk.gov.moj.cpp.listing.query.view.courtlist;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class HearingOffenceFilterTest {

    private static final JsonObject OFFENCE = createObjectBuilder().add("offenceCode", "TH68001").build();

    @Test
    public void shouldRemoveHearingWhenADefendantHasNoOffences() {

        final JsonObject defendantWithOffence = createObjectBuilder()
                .add("offences", createArrayBuilder().add(OFFENCE).build())
                .build();
        final JsonObject defendantWithoutOffence = createObjectBuilder()
                .add("offences", createArrayBuilder().build())
                .build();

        final JsonObject hearingWithoutOffence = createObjectBuilder()
                .add("startTime", "no-offence")
                .add("defendants", createArrayBuilder().add(defendantWithoutOffence).build())
                .build();
        final JsonObject hearingWithOffence = createObjectBuilder()
                .add("startTime", "has-offence")
                .add("defendants", createArrayBuilder().add(defendantWithOffence).build())
                .build();
        final JsonObject hearingWithMixedDefendants = createObjectBuilder()
                .add("startTime", "mixed")
                .add("defendants", createArrayBuilder().add(defendantWithOffence).add(defendantWithoutOffence).build())
                .build();
        final JsonObject hearingWithNoDefendants = createObjectBuilder()
                .add("startTime", "no-defendants")
                .build();

        final JsonObject payload = courtListPayloadWithHearings(
                hearingWithoutOffence, hearingWithOffence, hearingWithMixedDefendants, hearingWithNoDefendants);

        final JsonObject result = HearingOffenceFilter.removeHearingsWithDefendantMissingOffences(payload);

        final JsonArray remainingHearings = firstSitting(result).getJsonArray("hearings");
        assertThat(remainingHearings.size(), is(2));
        assertThat(remainingHearings.getJsonObject(0).getString("startTime"), is("has-offence"));
        assertThat(remainingHearings.getJsonObject(1).getString("startTime"), is("no-defendants"));
    }

    private static JsonObject courtListPayloadWithHearings(final JsonObject... hearings) {
        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();
        for (final JsonObject hearing : hearings) {
            hearingsBuilder.add(hearing);
        }
        final JsonObject sitting = createObjectBuilder().add("hearings", hearingsBuilder.build()).build();
        final JsonObject courtList = createObjectBuilder().add("sittings", createArrayBuilder().add(sitting).build()).build();
        return createObjectBuilder().add("courtLists", createArrayBuilder().add(courtList).build()).build();
    }

    private static JsonObject firstSitting(final JsonObject payload) {
        return payload.getJsonArray("courtLists").getJsonObject(0).getJsonArray("sittings").getJsonObject(0);
    }
}
