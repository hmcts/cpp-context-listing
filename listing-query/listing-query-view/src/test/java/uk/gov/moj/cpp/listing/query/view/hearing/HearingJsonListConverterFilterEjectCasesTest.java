package uk.gov.moj.cpp.listing.query.view.hearing;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDate.parse;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.listing.query.view.utils.FileUtil.givenPayload;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.junit.Test;

public class HearingJsonListConverterFilterEjectCasesTest {

    private static final String SAMPLE_HEARING_WITH_EJECTED_CASE = "/json/hearingSampleDataWithEjectCaseFlag.json";
    private static final String SAMPLE_HEARING_WITHOUT_EJECTED_CASE = "/json/hearingSampleDataWithoutEjectCaseFlag.json";
    private static final String PUBLIC_LIST = "/json/hearingDataForPublicListWithEjectFlag.json";
    private static final String PUBLIC_LIST_MULTIPLE_CASES = "/json/hearingDataForPublicListWithEjectFlagMultiple.json";
    private static final String EXPECTED_PUBLIC_LIST_MULTIPLE_CASES = "src/test/resources/json/expectedHearingDataForPublic.json";
    private static final String ALPHABETICAL_LIST = "/json/hearingDataForAlphabeticalListWithEjectFlag.json";
    private static final String SAMPLE_HEARING_WITH_2_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE = "/json/hearingSampleDataWith2HearingDaysInDifferentHearingDate.json";
    private static final String SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE = "/json/hearingSampleDataWith3HearingDaysInDifferentHearingDate.json";
    private static final String SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_THE_SAME_HEARING_DATE = "/json/hearingSampleDataWith3HearingDaysInTheSameHearingDate.json";
    private static final String SAMPLE_HEARING_WITH_2_HEARING_DAYS_IN_THE_SAME_HEARING_DATE = "/json/hearingSampleDataWith2HearingDaysInTheSameHearingDate.json";
    private static final String SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_COURT_CENTRE = "/json/hearingSampleDataWith3HearingDaysInDifferentCourtCentre.json";

    private static final String COURT_CENTRE_ID_QUERY_PARAMETER = "courtCentreId";
    private static final String COURT_ROOM_ID_QUERY_PARAMETER = "courtRoomId";
    private static final String SEARCH_DATE_QUERY_PARAMETER = "searchDate";
    private static final String START_TIME_QUERY_PARAMETER = "startTime";
    private static final String END_TIME_QUERY_PARAMETER = "endTime";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMAT_ZONED_DATE_AND_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter HEARING_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HearingJsonListConverterFilterEjectCases converter = new HearingJsonListConverterFilterEjectCases();

    @Test
    public void shouldConvertToJsonArrayWith2HearingDaysInDifferentHearingDate() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_2_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-22T09:00:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, "2020-09-22T10:00:00.000");

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(2)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-22')].matchedWithQuery", hasItem(true)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-24')].matchedWithQuery", hasItem(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith2HearingDaysInDifferentHearingDate2() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_2_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-22T09:00:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, "2020-09-22T10:00:00.000");

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(2)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(true))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInDifferentHearingDate() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-22T09:00:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, "2020-09-22T10:00:00.000");

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(true)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[2].matchedWithQuery", equalTo(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInDifferentHearingDate2() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-24");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-24T09:00:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, "2020-09-24T10:00:00.000");

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-22')].matchedWithQuery", hasItem(false)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-24')].matchedWithQuery", hasItem(true)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-25')].matchedWithQuery", hasItem(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInDifferentHearingDate3() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-22T12:00:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, "2020-09-22T13:00:00.000");

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-22')].matchedWithQuery", hasItem(true)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-24')].matchedWithQuery", hasItem(false)),
                withJsonPath("$[0].hearingDays[?(@.hearingDate == '2020-09-25')].matchedWithQuery", hasItem(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInDifferentHearingDate4() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-24");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-24", MIN.toString()));
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-24", MAX.toString()));

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(true)),
                withJsonPath("$[0].hearingDays[2].matchedWithQuery", equalTo(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInDifferentCourtCentre() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_COURT_CENTRE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-22T12:00:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-22", MAX.toString()));

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(true)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[2].matchedWithQuery", equalTo(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInDifferentCourtCentre2() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_DIFFERENT_COURT_CENTRE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-22", MIN.toString()));
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-22", MAX.toString()));

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[2].matchedWithQuery", equalTo(true))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInTheSameHearingDate() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_THE_SAME_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-22", MIN.toString()));
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-22", MAX.toString()));

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(true)),
                withJsonPath("$[0].hearingDays[2].matchedWithQuery", equalTo(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith2HearingDaysInTheSameHearingDate() throws IOException {
        //Given
        final LocalDate dateNow = LocalDate.now();
        final LocalTime timeNow = LocalTime.now();
        final LocalDateTime dateTimeNow = dateNow.atTime(timeNow);

        final LocalDateTime firstHearingStartTime = dateTimeNow.minusHours(1);
        final LocalDateTime firstHearingEndTime = firstHearingStartTime.plusHours(1);
        final LocalDateTime secondHearingStartTime = dateTimeNow.plusHours(1);
        final LocalDateTime secondHearingEndTime = secondHearingStartTime.plusHours(1);

        final String hearingDate = dateTimeNow.format(HEARING_DATE_FORMAT);

        final String hearingJsonString = givenPayload(SAMPLE_HEARING_WITH_2_HEARING_DAYS_IN_THE_SAME_HEARING_DATE).toString()
                .replaceAll("START_TIME_HEARING_DAY_1", firstHearingStartTime.format(DATE_FORMAT_ZONED_DATE_AND_TIME))
                .replaceAll("START_TIME_HEARING_DAY_2", secondHearingStartTime.format(DATE_FORMAT_ZONED_DATE_AND_TIME))
                .replaceAll("END_TIME_HEARING_DAY_1", firstHearingEndTime.format(DATE_FORMAT_ZONED_DATE_AND_TIME))
                .replaceAll("END_TIME_HEARING_DAY_2", secondHearingEndTime.format(DATE_FORMAT_ZONED_DATE_AND_TIME))
                .replaceAll("HEARING_DATE", hearingDate);

        final List<Hearing> hearings = Arrays.asList(new Hearing(UUID.randomUUID(), objectMapper.readTree(hearingJsonString)));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, hearingDate);
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, getDateTimeAsString(hearingDate, timeNow.toString()));
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, getDateTimeAsString(hearingDate, MAX.toString()));

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(2)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(true))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInTheSameHearingDate2() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_THE_SAME_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-22T10:30:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-22", MAX.toString()));

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(true)),
                withJsonPath("$[0].hearingDays[2].matchedWithQuery", equalTo(false))
        )));
    }

    @Test
    public void shouldConvertToJsonArrayWith3HearingDaysInTheSameHearingDate3() throws IOException {
        //Given
        final List<Hearing> hearings = Arrays.asList(createHearing(SAMPLE_HEARING_WITH_3_HEARING_DAYS_IN_THE_SAME_HEARING_DATE));

        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID_QUERY_PARAMETER, "f8254db1-1683-483e-afb3-b87fde5a0a26");
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID_QUERY_PARAMETER, "b4562684-9209-3ec4-a544-7f80dabd94d8");
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE_QUERY_PARAMETER, "2020-09-22");
        hearingDayMatchedCriteriaMap.put(START_TIME_QUERY_PARAMETER, "2020-09-22T15:30:00.000");
        hearingDayMatchedCriteriaMap.put(END_TIME_QUERY_PARAMETER, getDateTimeAsString("2020-09-22", MAX.toString()));

        //When
        final JsonArray hearingJsonArray = converter.convertForSearchHearing(hearings, hearingDayMatchedCriteriaMap);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].hearingDays", hasSize(3)),
                withJsonPath("$[0].hearingDays[0].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[1].matchedWithQuery", equalTo(false)),
                withJsonPath("$[0].hearingDays[2].matchedWithQuery", equalTo(true))
        )));
    }

    @Test
    public void shouldConvertHearingResultForPublicList() throws IOException {
        //Given
        final Hearing hearing = createHearing(PUBLIC_LIST);

        //When
        final JsonArray hearingsByHearingDateJsonArray = getHearingByDateJsonArray(hearing);
        final JsonObject hearingsByHearingDateJsonObject = hearingsByHearingDateJsonArray.getJsonObject(0);
        final JsonObject hearingJsonObject = hearingsByHearingDateJsonObject.getJsonObject("hearing");
        assertThat(hearingJsonObject.toString(), isJson(allOf(
                withJsonPath("$.listedCases", hasSize(1)),
                withJsonPath("$.listedCases[0].isEjected", equalTo(false)),
                withJsonPath("$.courtApplications", hasSize(0))
        )));
    }

    @Test
    public void shouldConvertHearingResultForPublicListForMultipleCases() throws IOException {
        //Given
        final Hearing hearing = createHearing(PUBLIC_LIST_MULTIPLE_CASES);

        //When
        final JsonArray hearingsByHearingDateJsonArray = getHearingByDateJsonArray(hearing);
        JSONArray jsonArrayBigOne = new JSONArray();
        hearingsByHearingDateJsonArray.forEach(s -> jsonArrayBigOne.put(s.toString()));
        assertEquals(jsonArrayBigOne.toString(),
                readFileToString(new File(EXPECTED_PUBLIC_LIST_MULTIPLE_CASES)), STRICT);
    }

    private JsonArray getHearingByDateJsonArray(final Hearing hearing) {
        final JsonArray hearingJsonArrayPublicList = converter.convertHearingResultForPublicList(hearing);

        //Then
        final JsonObject hearingJsonArrayPublicListJsonObject = (JsonObject) hearingJsonArrayPublicList.get(0);
        final JsonArray hearingsByCourtCentreIdArray = hearingJsonArrayPublicListJsonObject.getJsonArray("hearingsByCourtCentreId");
        final JsonObject hearingsByCourtCentreIdObject = hearingsByCourtCentreIdArray.getJsonObject(0);
        return hearingsByCourtCentreIdObject.getJsonArray("hearingsByHearingDate");
    }

    @Test
    public void shouldConvertHearingResultForAlphabeticalList() throws IOException {
        final Hearing hearing = createHearing(ALPHABETICAL_LIST);

        final JsonArray hearingJsonArrayAlphabeticalList = converter.convertHearingResultForAlphabeticalList(ImmutableList.of(hearing));

        assertThat(hearingJsonArrayAlphabeticalList.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].hearingsByHearingDate", hasSize(2)),
                withJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases", hasSize(1)),
                withJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases[0].isEjected", equalTo(false)),
                withJsonPath("$[0].hearingsByHearingDate[1].hearing.courtApplications", hasSize(1)),
                withJsonPath("$[0].hearingsByHearingDate[1].hearing.courtApplications[0].isEjected", equalTo(false))
        )));
    }

    @Test
    public void shouldDeepCopyHearingAndConvertHearingResultForAlphabeticalList() throws IOException {
        final Hearing hearing = createHearing(ALPHABETICAL_LIST);

        final JsonArray hearingJsonArrayAlphabeticalList = converter.convertHearingResultForAlphabeticalList(ImmutableList.of(hearing));

        assertThat(hearing.getProperties().get(0).toString(), isJson(allOf(
                withJsonPath("$.hearingsByHearingDate", hasSize(5))
        )));

        assertThat(hearingJsonArrayAlphabeticalList.toString(), isJson(allOf(
                withJsonPath("$", hasSize(1)),
                withJsonPath("$[0].hearingsByHearingDate", hasSize(2)),
                withJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases", hasSize(1)),
                withJsonPath("$[0].hearingsByHearingDate[0].hearing.listedCases[0].isEjected", equalTo(false)),
                withJsonPath("$[0].hearingsByHearingDate[1].hearing.courtApplications", hasSize(1)),
                withJsonPath("$[0].hearingsByHearingDate[1].hearing.courtApplications[0].isEjected", equalTo(false))
        )));
    }

    private Hearing createHearing(final String filePath) throws IOException {
        final StringWriter writer = new StringWriter();

        InputStream inputStream = getClass().getResourceAsStream(filePath);
        IOUtils.copy(inputStream, writer, UTF_8);

        return new Hearing(UUID.randomUUID(), objectMapper.readTree(writer.toString()));
    }

    private List<Hearing> createHearings(final String filePath1, final String filePath2) throws IOException {
        return newArrayList(createHearing(filePath1), createHearing(filePath2));
    }

    private String getDateTimeAsString(final String date, final String time) {
        final LocalDate localDate = parse(date);
        final LocalTime localTime = LocalTime.parse(time);
        return localDate.atTime(localTime).toString();
    }
}
