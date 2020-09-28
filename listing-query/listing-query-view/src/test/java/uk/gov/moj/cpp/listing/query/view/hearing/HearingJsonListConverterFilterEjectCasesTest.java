package uk.gov.moj.cpp.listing.query.view.hearing;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.query.view.FileUtils.createHearing;
import static uk.gov.moj.cpp.listing.query.view.FileUtils.createHearings;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.IOException;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class HearingJsonListConverterFilterEjectCasesTest {

    private static final String SAMPLE_HEARING_WITH_EJECTED_CASE = "json/hearingSampleDataWithEjectCaseFlag.json";
    private static final String SAMPLE_HEARING_WITHOUT_EJECTED_CASE = "json/hearingSampleDataWithoutEjectCaseFlag.json";
    private static final String PUBLIC_LIST = "json/hearingDataForPublicListWithEjectFlag.json";
    private static final String ALPHABETICAL_LIST = "json/hearingDataForAlphabeticalListWithEjectFlag.json";

    private final HearingJsonListConverterFilterEjectCases converter = new HearingJsonListConverterFilterEjectCases();

    @Test
    public void shouldConvertToJsonArrayWithEjectedCaseFiltered() throws IOException {
        //Given
        final List<Hearing> hearings = createHearings(SAMPLE_HEARING_WITH_EJECTED_CASE, SAMPLE_HEARING_WITHOUT_EJECTED_CASE);

        //When
        final JsonArray hearingJsonArray = converter.convert(hearings);

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
        final JsonArray hearingJsonArrayPublicList = converter.convertHearingResultForPublicList(hearing);

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
}
