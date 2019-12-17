package uk.gov.moj.cpp.listing.query.view.courtlist;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SittingsPojoBuilderTest {

    @Test
    public void shouldPutHearingsInTheSameSittingIfTheyHaveTheSameSittingDetails() {

        final UUID courtRoomId = randomUUID();
        final  UUID judicialId = randomUUID();

        final JsonObject hearing1 = buildWeekCommencingCaseHearings("2019-12-11", courtRoomId, judicialId);
        final JsonObject hearing2 = buildWeekCommencingCaseHearings("2019-12-11", courtRoomId, judicialId);

        assertThat(hearing1, equalTo(hearing2));

        final JsonArray hearingsArray = Json.createArrayBuilder()
                .add(hearing1)
                .add(hearing2)
                .build();

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignHearingsToSittings(hearingsArray);

        assertThat(sittingsMap.size(), is(1));
    }

    @Test
    public void shouldPutHearingsInSeparateSittingsIfInDifferentCourtRooms() {

        final UUID judicialId = randomUUID();

        final JsonObject hearing1 = buildWeekCommencingCaseHearings("2019-12-11", randomUUID(), judicialId);
        final JsonObject hearing2 = buildWeekCommencingCaseHearings("2019-12-11", randomUUID(), judicialId);

        assertThat(hearing1, not(equalTo(hearing2)));

        final JsonArray hearingsArray = Json.createArrayBuilder()
                .add(hearing1)
                .add(hearing2)
                .build();

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignHearingsToSittings(hearingsArray);

        assertThat(sittingsMap.size(), is(2));
    }


    private JsonObject buildWeekCommencingCaseHearings(final String startDate, final UUID courtRoomId, final UUID judicialId) {
        return Json.createObjectBuilder()
                .add("weekCommencingStartDate", startDate)
                .add("weekCommencingEndDate", "2019-12-15")
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("listedCases",Json.createArrayBuilder()
                        .add(buildCaseDetailsJson()))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .build();
    }

    private JsonObjectBuilder buildCaseDetailsJson() {
        final JsonObjectBuilder caseDetailsJson = Json.createObjectBuilder();

        caseDetailsJson.add("restrictFromCourtList",false);

        return caseDetailsJson;
    }

    private JsonObjectBuilder buildNonDefaultDaysJson() {
        final JsonObjectBuilder caseDetailsJson = Json.createObjectBuilder();

        caseDetailsJson.add("startTime","2019-12-17T09:57:18.807Z[Europe/London]");

        return caseDetailsJson;
    }

}
