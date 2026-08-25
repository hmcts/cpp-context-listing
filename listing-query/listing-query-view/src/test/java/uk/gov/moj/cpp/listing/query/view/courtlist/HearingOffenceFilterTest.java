package uk.gov.moj.cpp.listing.query.view.courtlist;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

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

        final JsonObject result = HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload);

        final JsonArray remainingHearings = firstSitting(result).getJsonArray("hearings");
        assertThat(remainingHearings.size(), is(3));
        assertThat(remainingHearings.getJsonObject(0).getString("startTime"), is("has-offence"));
        assertThat(remainingHearings.getJsonObject(1).getString("startTime"), is("mixed"));
        assertThat(remainingHearings.getJsonObject(2).getString("startTime"), is("no-defendants"));

        final JsonArray mixedHearingDefendants = remainingHearings.getJsonObject(1).getJsonArray("defendants");
        assertThat(mixedHearingDefendants.size(), is(1));
        assertThat(mixedHearingDefendants.getJsonObject(0), is(defendantWithOffence));
    }

    @Test
    public void shouldRemoveHearingWhenDefendantOffencesIsJsonNull() {

        final JsonObject defendantWithNullOffences = createObjectBuilder().addNull("offences").build();
        final JsonObject hearing = hearingWithDefendant("null-offences", defendantWithNullOffences);

        final JsonObject payload = courtListPayloadWithHearings(hearing);

        final JsonObject result = HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload);

        assertThat(firstSitting(result).getJsonArray("hearings").size(), is(0));
    }

    @Test
    public void shouldRemoveHearingWhenDefendantsArrayIsEmpty() {

        final JsonObject hearing = createObjectBuilder()
                .add("startTime", "no-defendants-listed")
                .add("defendants", createArrayBuilder().build())
                .build();

        final JsonObject payload = courtListPayloadWithHearings(hearing);

        final JsonObject result = HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload);

        assertThat(firstSitting(result).getJsonArray("hearings").size(), is(0));
    }

    @Test
    public void shouldRemoveOnlyTheDefendantWithoutOffenceWhenHearingHasMultipleDefendants() {

        final JsonObject defendantWithOffence = createObjectBuilder()
                .add("defendantId", "with-offence")
                .add("offences", createArrayBuilder().add(OFFENCE).build())
                .build();
        final JsonObject anotherDefendantWithOffence = createObjectBuilder()
                .add("defendantId", "also-with-offence")
                .add("offences", createArrayBuilder().add(OFFENCE).build())
                .build();
        final JsonObject defendantWithoutOffence = createObjectBuilder()
                .add("defendantId", "without-offence")
                .add("offences", createArrayBuilder().build())
                .build();

        final JsonObject hearing = createObjectBuilder()
                .add("startTime", "three-defendants")
                .add("defendants", createArrayBuilder()
                        .add(defendantWithOffence).add(defendantWithoutOffence).add(anotherDefendantWithOffence).build())
                .build();

        final JsonObject payload = courtListPayloadWithHearings(hearing);

        final JsonObject result = HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload);

        final JsonArray remainingHearings = firstSitting(result).getJsonArray("hearings");
        assertThat(remainingHearings.size(), is(1));

        final JsonArray remainingDefendants = remainingHearings.getJsonObject(0).getJsonArray("defendants");
        assertThat(remainingDefendants.size(), is(2));
        assertThat(remainingDefendants.getJsonObject(0), is(defendantWithOffence));
        assertThat(remainingDefendants.getJsonObject(1), is(anotherDefendantWithOffence));
    }

    @Test
    public void shouldRemoveHearingWhenNoDefendantOnItHasAnyOffence() {

        final JsonObject defendantOneWithoutOffence = createObjectBuilder()
                .add("offences", createArrayBuilder().build())
                .build();
        final JsonObject defendantTwoWithoutOffence = createObjectBuilder()
                .addNull("offences")
                .build();

        final JsonObject hearing = createObjectBuilder()
                .add("startTime", "no-defendant-has-offence")
                .add("defendants", createArrayBuilder().add(defendantOneWithoutOffence).add(defendantTwoWithoutOffence).build())
                .build();

        final JsonObject payload = courtListPayloadWithHearings(hearing);

        final JsonObject result = HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload);

        assertThat(firstSitting(result).getJsonArray("hearings").size(), is(0));
    }

    @Test
    public void shouldReturnPayloadUnchangedWhenCourtListsKeyIsMissing() {

        final JsonObject payload = createObjectBuilder().add("courtCentreId", "abc-123").build();

        assertThat(HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload), is(payload));
    }

    @Test
    public void shouldReturnPayloadUnchangedWhenCourtListsIsJsonNull() {

        final JsonObject payload = createObjectBuilder().add("courtCentreId", "abc-123").addNull("courtLists").build();

        assertThat(HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload), is(payload));
    }

    @Test
    public void shouldKeepCourtListUnchangedWhenSittingsKeyIsMissing() {

        final JsonObject crestCourtSite = createObjectBuilder().add("crestCourtSiteId", "415").build();
        final JsonObject courtListWithoutSittings = createObjectBuilder().add("crestCourtSite", crestCourtSite).build();
        final JsonObject payload = createObjectBuilder()
                .add("courtLists", createArrayBuilder().add(courtListWithoutSittings).build())
                .build();

        assertThat(HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload), is(payload));
    }

    @Test
    public void shouldKeepCourtListUnchangedWhenSittingsIsJsonNull() {

        final JsonObject courtListWithNullSittings = createObjectBuilder().addNull("sittings").build();
        final JsonObject payload = createObjectBuilder()
                .add("courtLists", createArrayBuilder().add(courtListWithNullSittings).build())
                .build();

        assertThat(HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload), is(payload));
    }

    @Test
    public void shouldKeepSittingUnchangedWhenHearingsKeyIsMissing() {

        final JsonObject sittingWithoutHearings = createObjectBuilder()
                .add("sittingDate", "2026-01-01")
                .add("judiciary", createArrayBuilder().add(createObjectBuilder().add("judicialId", "j-1").build()).build())
                .build();

        final JsonObject payload = courtListPayloadWithSittings(sittingWithoutHearings);

        assertThat(HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload), is(payload));
    }

    @Test
    public void shouldKeepSittingUnchangedWhenHearingsIsJsonNull() {

        final JsonObject sittingWithNullHearings = createObjectBuilder().addNull("hearings").build();

        final JsonObject payload = courtListPayloadWithSittings(sittingWithNullHearings);

        assertThat(HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload), is(payload));
    }

    @Test
    public void shouldPreserveSiblingFieldsAtEveryLevelWhenNothingIsRemoved() {

        final JsonObject defendantWithOffence = createObjectBuilder()
                .add("offences", createArrayBuilder().add(OFFENCE).build())
                .build();
        final JsonObject survivingHearing = createObjectBuilder()
                .add("startTime", "2026-01-01T10:00:00")
                .add("hasVideoLink", true)
                .add("defendants", createArrayBuilder().add(defendantWithOffence).build())
                .build();
        final JsonObject crestCourtSite = createObjectBuilder().add("crestCourtSiteId", "415").build();
        final JsonObject sitting = createObjectBuilder()
                .add("sittingDate", "2026-01-01")
                .add("judiciary", createArrayBuilder().add(createObjectBuilder().add("judicialId", "j-1").build()).build())
                .add("hearings", createArrayBuilder().add(survivingHearing).build())
                .build();
        final JsonObject courtList = createObjectBuilder()
                .add("crestCourtSite", crestCourtSite)
                .add("sittings", createArrayBuilder().add(sitting).build())
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("courtCentreId", "abc-123")
                .add("courtLists", createArrayBuilder().add(courtList).build())
                .build();

        assertThat(HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload), is(payload));
    }

    @Test
    public void shouldFilterIndependentlyAcrossMultipleSittingsAndCourtLists() {

        final JsonObject defendantWithOffence = createObjectBuilder()
                .add("offences", createArrayBuilder().add(OFFENCE).build())
                .build();
        final JsonObject defendantWithoutOffence = createObjectBuilder()
                .add("offences", createArrayBuilder().build())
                .build();

        final JsonObject sittingOne = createObjectBuilder()
                .add("hearings", createArrayBuilder()
                        .add(hearingWithDefendant("keep-A", defendantWithOffence))
                        .add(hearingWithDefendant("drop-A", defendantWithoutOffence))
                        .build())
                .build();
        final JsonObject sittingTwo = createObjectBuilder()
                .add("hearings", createArrayBuilder()
                        .add(hearingWithDefendant("drop-B", defendantWithoutOffence))
                        .add(hearingWithDefendant("keep-B", defendantWithOffence))
                        .build())
                .build();
        final JsonObject courtListOne = createObjectBuilder()
                .add("sittings", createArrayBuilder().add(sittingOne).add(sittingTwo).build())
                .build();

        final JsonObject sittingThree = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(hearingWithDefendant("keep-C", defendantWithOffence)).build())
                .build();
        final JsonObject courtListTwo = createObjectBuilder()
                .add("sittings", createArrayBuilder().add(sittingThree).build())
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("courtLists", createArrayBuilder().add(courtListOne).add(courtListTwo).build())
                .build();

        final JsonObject result = HearingOffenceFilter.removeDefendantsAndHearingsMissingOffences(payload);

        final JsonArray courtLists = result.getJsonArray("courtLists");
        final JsonArray sittingsOfCourtListOne = courtLists.getJsonObject(0).getJsonArray("sittings");
        assertThat(startTimesOf(sittingsOfCourtListOne.getJsonObject(0).getJsonArray("hearings")), is(List.of("keep-A")));
        assertThat(startTimesOf(sittingsOfCourtListOne.getJsonObject(1).getJsonArray("hearings")), is(List.of("keep-B")));

        final JsonArray sittingsOfCourtListTwo = courtLists.getJsonObject(1).getJsonArray("sittings");
        assertThat(startTimesOf(sittingsOfCourtListTwo.getJsonObject(0).getJsonArray("hearings")), is(List.of("keep-C")));
    }

    @Test
    public void privateConstructorShouldThrowIllegalStateException() {
        assertThrows(InvocationTargetException.class, () -> {
            final Constructor<HearingOffenceFilter> constructor = HearingOffenceFilter.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }

    private static JsonObject hearingWithDefendant(final String startTime, final JsonObject defendant) {
        return createObjectBuilder()
                .add("startTime", startTime)
                .add("defendants", createArrayBuilder().add(defendant).build())
                .build();
    }

    private static List<String> startTimesOf(final JsonArray hearings) {
        return hearings.getValuesAs(JsonObject.class).stream()
                .map(hearing -> hearing.getString("startTime"))
                .collect(Collectors.toList());
    }

    private static JsonObject courtListPayloadWithSittings(final JsonObject... sittings) {
        final JsonArrayBuilder sittingsBuilder = createArrayBuilder();
        for (final JsonObject sitting : sittings) {
            sittingsBuilder.add(sitting);
        }
        final JsonObject courtList = createObjectBuilder().add("sittings", sittingsBuilder.build()).build();
        return createObjectBuilder().add("courtLists", createArrayBuilder().add(courtList).build()).build();
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
