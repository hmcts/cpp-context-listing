package uk.gov.moj.cpp.listing.query.view.courtlist;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonUtils.getJsonFile;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.List;

import javax.json.JsonObject;

import org.junit.Test;

public class FlatHearingsConverterTest {

    @Test
    public void shouldGenerateFlatHearingListForCasesWithMultipleHearingDays() throws Exception {

        final JsonObject rangeSearchResponse = getJsonFile("courtlist/4.case-with-multiple-days/range-search-response.json");

        final List<FlatHearing> flatHearings = FlatHearingsConverter.generateFlatHearingList(
                rangeSearchResponse.getJsonArray("hearings"));

        assertThat(flatHearings.size(), is(2));

        final FlatHearing flatHearing0 = flatHearings.get(0);
        final FlatHearing flatHearing1 = flatHearings.get(1);

        assertThat(flatHearing0.getHearingDate(), is(LocalDate.parse("2019-12-25")));

        assertThat(flatHearing1.getHearingDate(), is(LocalDate.parse("2019-12-27")));
    }
}
