package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
        final LocalDate startDate = LocalDate.parse("2019-12-11");
        final String endDate = "2019-12-11";

        final FlatHearing flatHearing1 = buildFlatHearing("2019-12-11", courtRoomId, judicialId);
        final FlatHearing flatHearing2 = buildFlatHearing("2019-12-11", courtRoomId, judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
    }

    @Test
    public void shouldPutHearingsInSeparateSittingsIfInDifferentCourtRooms() {

        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2019-12-11");
        final String endDate = "2019-12-11";

        final FlatHearing flatHearing1 = buildFlatHearing("2019-12-11", randomUUID(), judicialId);
        final FlatHearing flatHearing2 = buildFlatHearing("2019-12-11", randomUUID(), judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(2));
    }

    @Test
    public void shouldPutHearingsInTheSameSittingIfTheyHaveTheSameSittingDetailsWithVideoLinkDetails() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2019-12-11");
        final String endDate = "2019-12-11";

        final FlatHearing flatHearing1 = buildFlatHearingWithVideoLinkDetails("2019-12-11", courtRoomId, judicialId, true, "Pre Conf");
        final FlatHearing flatHearing2 = buildFlatHearingWithVideoLinkDetails("2019-12-11", courtRoomId, judicialId, true, "Test Conf");

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().get(0).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(0).getVideoLinkDetails(), is("Pre Conf"));

        assertThat(sittingsMap.get(0).getHearings().get(1).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(1).getVideoLinkDetails(), is("Test Conf"));
    }


    @Test
    public void shouldPutHearingsInTheSameSittingIfTheyHaveTheSameSittingDetailsWithOneHasVideoLinkDetails() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2019-12-11");
        final String endDate = "2019-12-11";

        final FlatHearing flatHearing1 = buildFlatHearingWithVideoLinkDetails("2019-12-11", courtRoomId, judicialId, true, "Pre Conf");
        final FlatHearing flatHearing2 = buildFlatHearingWithVideoLinkDetails("2019-12-11", courtRoomId, judicialId, true, null);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().get(0).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(0).getVideoLinkDetails(), is("Pre Conf"));

        assertThat(sittingsMap.get(0).getHearings().get(1).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(1).getVideoLinkDetails(), nullValue());
    }

    @Test
    public void shouldGetHearingStartTimeFromHearingDayIfWeekCommencingStartDateIsEmpty() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2020-10-05");
        final String endDate = "2020-10-05";
        final String hearingStartTime = "2020-10-05T11:00:00.000Z";
        final String hearingEndTime = "2020-10-05T12:00:00.000Z";
        final LocalDateTime expectedHearingStartTime = ZonedDateTime.parse(hearingStartTime).toLocalDateTime();
        final LocalDateTime expectedHearingEndTime = ZonedDateTime.parse(hearingEndTime).toLocalDateTime();

        final FlatHearing flatHearing = buildFlatHearingWithHearingDays("2020-10-05", hearingStartTime, hearingEndTime, courtRoomId, judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().size(), is(1));
        final Hearing hearing = sittingsMap.get(0).getHearings().get(0);
        assertThat(hearing.getStartTime(), is(expectedHearingStartTime));
        assertThat(hearing.getEndTime().get(), is(expectedHearingEndTime));
    }

    private FlatHearing buildFlatHearing(final String startDate, final UUID courtRoomId, final UUID judicialId) {

        final JsonObject caseHearings = buildWeekCommencingCaseHearings(startDate, courtRoomId, judicialId);

        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, true);
    }

    private FlatHearing buildFlatHearingWithHearingDays(final String startDate,
                                                        final String hearingStartTime,
                                                        final String hearingEndTime,
                                                        final UUID courtRoomId,
                                                        final UUID judicialId) {

        final JsonObject caseHearings = buildCaseHearingsWithHearingDays(startDate, hearingStartTime, hearingEndTime, courtRoomId, judicialId);

        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, false);
    }

    private FlatHearing buildFlatHearingWithVideoLinkDetails(final String startDate, final UUID courtRoomId, final UUID judicialId, final boolean hasVideoLink, final String videoLinkDetails) {

        final JsonObject caseHearings = buildWeekCommencingCaseHearingsWithVideoLink(startDate, courtRoomId, judicialId, hasVideoLink, videoLinkDetails);

        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, true);
    }

    private JsonObject buildCaseHearingsWithHearingDays(final String hearingDate, final String hearingStartTime, final String hearingEndTime, final UUID courtRoomId, final UUID judicialId) {
        return Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("listedCases", Json.createArrayBuilder()
                        .add(buildCaseDetailsJson()))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .add("hearingDays", Json.createArrayBuilder()
                        .add(buildHearingDaysJson(hearingDate, hearingStartTime, hearingEndTime)))
                .build();
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

    private JsonObject buildWeekCommencingCaseHearingsWithVideoLink(final String startDate, final UUID courtRoomId, final UUID judicialId, final boolean hasVideoLink, final String videoLinkDetails) {

        final JsonObjectBuilder hearing = Json.createObjectBuilder()
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
                .add("hasVideoLink", hasVideoLink);
        if(nonNull(videoLinkDetails)) {
             hearing.add("videoLinkDetails", videoLinkDetails);

        }
        return hearing.build();
    }

    private JsonObjectBuilder buildCaseDetailsJson() {
        final JsonObjectBuilder caseDetailsJson = Json.createObjectBuilder();

        caseDetailsJson.add("restrictFromCourtList", false);

        return caseDetailsJson;
    }

    private JsonObjectBuilder buildNonDefaultDaysJson() {
        final JsonObjectBuilder nonDefaultDaysJson = Json.createObjectBuilder();

        nonDefaultDaysJson.add("startTime", "2019-12-17T09:57:18.807Z");

        return nonDefaultDaysJson;
    }

    private JsonObjectBuilder buildHearingDaysJson(final String hearingDate, final String hearingStartTime, final String hearingEndTime) {
        final JsonObjectBuilder hearingDaysJson = Json.createObjectBuilder();

        hearingDaysJson.add("startTime", "2020-10-05T11:00:00.000Z");
        hearingDaysJson.add("endTime", "2020-10-05T12:00:00.000Z");
        hearingDaysJson.add("hearingDate", hearingDate);

        return hearingDaysJson;
    }

}
