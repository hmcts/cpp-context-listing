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

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingJsonListConverterFilterEjectCasesTest {

    @InjectMocks
    private HearingJsonListConverterFilterEjectCases converter;

    @Test
    public void shouldConvertToJsonArrayWithEjectedCaseFilterd() throws IOException {

        //Given
        final List<Hearing> hearings = createHearings();
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
        final Hearing hearing = createHearingForPublicList();
        //When
        JsonArray hearingJsonArrayPublicList = converter.convertHearingResultForPublicList(hearing);
        //Then
        final JsonObject hearingJsonArrayPublicListJsonObject = (JsonObject)hearingJsonArrayPublicList.get(0);
        final JsonArray hearingsByCourtCentreIdArray = hearingJsonArrayPublicListJsonObject.getJsonArray("hearingsByCourtCentreId");
        final JsonObject  hearingsByCourtCentreIdObject = hearingsByCourtCentreIdArray.getJsonObject(0);
        final JsonArray hearingsByHearingDateJsonArray = hearingsByCourtCentreIdObject.getJsonArray("hearingsByHearingDate");
        final JsonObject hearingsByHearingDateJsonObject =  hearingsByHearingDateJsonArray.getJsonObject(0);
        final JsonObject hearingJsonObject = hearingsByHearingDateJsonObject.getJsonObject("hearing");
        assertThat(hearingJsonObject.toString(), isJson(allOf(
                withJsonPath("$.listedCases", hasSize(1)),
                withJsonPath("$.listedCases[0].isEjected", equalTo(false)),
                withJsonPath("$.courtApplications", hasSize(0))
        )));
    }

    private Hearing createHearingForPublicList() throws IOException {
        final StringWriter writer = new StringWriter();

        InputStream inputStream = HearingJsonListConverterFilterEjectCasesTest.class.getResourceAsStream("/json/hearingDataForPublicListWithEjectFlag.json");
        IOUtils.copy(inputStream, writer, UTF_8);
        return  new Hearing(UUID.randomUUID(), JacksonUtil.toJsonNode(writer.toString()));

    }

    private List<Hearing> createHearings() throws IOException{

        final StringWriter writer = new StringWriter();
        //create first hearing with 2 listed case one with isEjected flag as true and other as false

        InputStream inputStream1 = HearingJsonListConverterFilterEjectCasesTest.class.getResourceAsStream("/json/hearingSampleDataWithEjectCaseFlag.json");
        IOUtils.copy(inputStream1, writer, UTF_8);
        final Hearing hearing1 = new Hearing(UUID.randomUUID(), JacksonUtil.toJsonNode(writer.toString()));
        writer.getBuffer().setLength(0);

        //create second hearing with 2 listed case without isEjected flag
        InputStream inputStream2 = HearingJsonListConverterFilterEjectCasesTest.class.getResourceAsStream("/json/hearingSampleDataWithoutEjectCaseFlag.json");
        IOUtils.copy(inputStream2, writer, UTF_8);
        final Hearing hearing2 = new Hearing(UUID.randomUUID(), JacksonUtil.toJsonNode(writer.toString()));
        return newArrayList(hearing1, hearing2);
    }
}
