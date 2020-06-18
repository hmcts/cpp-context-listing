package uk.gov.moj.cpp.listing.query.view.hearing;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class HearingJsonListConverterFilterEjectCasesTest {

    private static final String SAMPLE_HEARING_WITH_EJECTED_CASE = "/json/hearingSampleDataWithEjectCaseFlag.json";
    private static final String SAMPLE_HEARING_WITHOUT_EJECTED_CASE = "/json/hearingSampleDataWithoutEjectCaseFlag.json";
    private static final String PUBLIC_LIST = "/json/hearingDataForPublicListWithEjectFlag.json";
    private static final String ALPHABETICAL_LIST = "/json/hearingDataForAlphabeticalListWithEjectFlag.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HearingJsonListConverterFilterEjectCases converter = new HearingJsonListConverterFilterEjectCases();

    @Test
    public void shouldConvertToJsonArrayWithEjectedCaseFiltered() throws IOException {

        //Given
        final List<Hearing> hearings = createHearings(SAMPLE_HEARING_WITH_EJECTED_CASE, SAMPLE_HEARING_WITHOUT_EJECTED_CASE);
        //When
        JsonArray hearingJsonArray = converter.convert(hearings);
        //Then
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(2)),
                withJsonPath("$[0].listedCases", hasSize(1)),
                withJsonPath("$[0].courtApplications", hasSize(0)),
                withJsonPath("$[0].listedCases[0].isEjected", equalTo(false)),
                withJsonPath("$[1].listedCases", hasSize(2)),
                withJsonPath("$[1].courtApplications", hasSize(1))
        )));
    }

    @Test
    public void shouldConvertHearingResultForPublicList() throws IOException {

        //Given
        final Hearing hearing = createHearing(PUBLIC_LIST);
        //When
        JsonArray hearingJsonArrayPublicList = converter.convertHearingResultForPublicList(hearing);
        //Then
        final JsonObject hearingJsonArrayPublicListJsonObject = (JsonObject) hearingJsonArrayPublicList.get(0);
        final JsonArray hearingsByCourtCentreIdArray = hearingJsonArrayPublicListJsonObject.getJsonArray("hearingsByCourtCentreId");
        final JsonObject hearingsByCourtCentreIdObject = hearingsByCourtCentreIdArray.getJsonObject(0);
        final JsonArray hearingsByHearingDateJsonArray = hearingsByCourtCentreIdObject.getJsonArray("hearingsByHearingDate");
        final JsonObject hearingsByHearingDateJsonObject = hearingsByHearingDateJsonArray.getJsonObject(0);
        final JsonObject hearingJsonObject = hearingsByHearingDateJsonObject.getJsonObject("hearing");
        assertThat(hearingJsonObject.toString(), isJson(allOf(
                withJsonPath("$.listedCases", hasSize(1)),
                withJsonPath("$.listedCases[0].isEjected", equalTo(false)),
                withJsonPath("$.courtApplications", hasSize(0))
        )));
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

    private Hearing createHearing(final String filePath) throws IOException {
        final StringWriter writer = new StringWriter();

        InputStream inputStream = HearingJsonListConverterFilterEjectCasesTest.class.getResourceAsStream(filePath);
        IOUtils.copy(inputStream, writer, UTF_8);

        return new Hearing(UUID.randomUUID(), objectMapper.readTree(writer.toString()));
    }

    private List<Hearing> createHearings(final String filePath1, final String filePath2) throws IOException {
        return newArrayList(createHearing(filePath1), createHearing(filePath2));
    }
}
