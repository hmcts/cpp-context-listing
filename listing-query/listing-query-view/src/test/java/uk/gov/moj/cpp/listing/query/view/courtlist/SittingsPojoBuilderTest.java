package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;

public class SittingsPojoBuilderTest {

    @Test
    public void shouldPutHearingsInTheSameSittingIfTheyHaveTheSameSittingDetails() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();

        final FlatHearing flatHearing1 = buildFlatHearing("2019-12-11", courtRoomId, judicialId);
        final FlatHearing flatHearing2 = buildFlatHearing("2019-12-11", courtRoomId, judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings);

        assertThat(sittingsMap.size(), is(1));
    }

    @Test
    public void shouldPutHearingsInSeparateSittingsIfInDifferentCourtRooms() {

        final UUID judicialId = randomUUID();

        final FlatHearing flatHearing1 = buildFlatHearing("2019-12-11", randomUUID(), judicialId);
        final FlatHearing flatHearing2 = buildFlatHearing("2019-12-11", randomUUID(), judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings);

        assertThat(sittingsMap.size(), is(2));
    }

    private FlatHearing buildFlatHearing(final String startDate, final UUID courtRoomId, final UUID judicialId) {

        final JsonObject caseHearings = buildWeekCommencingCaseHearings(startDate, courtRoomId, judicialId);

        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, false);
    }


    private JsonObject buildWeekCommencingCaseHearings(final String startDate, final UUID courtRoomId, final UUID judicialId) {
        return Json.createObjectBuilder()
                .add("weekCommencingStartDate", startDate)
                .add("weekCommencingEndDate", "2019-12-15")
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("listedCases", Json.createArrayBuilder()
                        .add(buildCaseDetailsJson()))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .build();
    }

    private JsonObjectBuilder buildCaseDetailsJson() {
        final JsonObjectBuilder caseDetailsJson = Json.createObjectBuilder();

        caseDetailsJson.add("restrictFromCourtList", false);

        return caseDetailsJson;
    }

    private JsonObjectBuilder buildNonDefaultDaysJson() {
        final JsonObjectBuilder caseDetailsJson = Json.createObjectBuilder();

        caseDetailsJson.add("startTime", "2019-12-17T09:57:18.807Z[Europe/London]");

        return caseDetailsJson;
    }

}
