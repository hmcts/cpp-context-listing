package uk.gov.moj.cpp.listing.query.view.courtlist;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.query.view.courtlist.JsonUtils.getJsonFile;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class FlatHearingsConverterTest {

    @Test
    public void shouldGenerateFlatHearingListForCasesWithMultipleHearingDays() throws Exception {

        final JsonObject rangeSearchResponse = getJsonFile("courtlist/4.case-with-multiple-days/range-search-response.json");

        final List<FlatHearing> flatHearings = FlatHearingsConverter.generateFlatHearingList(
                rangeSearchResponse.getJsonArray("hearings"));

        assertThat(flatHearings.size(), is(3));

        final FlatHearing flatHearing0 = flatHearings.get(0);
        final FlatHearing flatHearing1 = flatHearings.get(1);
        final FlatHearing flatHearing2 = flatHearings.get(2);

        assertThat(flatHearing0.getHearingDate(), is(LocalDate.parse("2019-12-17")));
        assertThat(flatHearing1.getHearingDate(), is(LocalDate.parse("2019-12-27")));
        assertThat(flatHearing2.getHearingDate(), is(LocalDate.parse("2019-12-28")));
    }


    @Test
    public void shouldGenerateFlatHearingListForCasesWithMultipleHearingDaysInDifferentCourtRooms() throws Exception {

        final JsonObject rangeSearchResponse = getJsonFile("courtlist/5.case-with-multiple-days-ste/range-search-response.json");

        final List<FlatHearing> flatHearings = FlatHearingsConverter.generateFlatHearingList(
                rangeSearchResponse.getJsonArray("hearings"));

        assertThat(flatHearings.size(), is(2));

        final Optional<UUID> room1 = Optional.of(UUID.fromString("5e1c7b54-3bca-3a37-a85a-84510f115b76"));
        final Optional<UUID> room2 = Optional.of(UUID.fromString("3e1c7b54-3bca-3a37-a85a-84510f115b33"));

        final FlatHearing flatHearing0 = flatHearings.get(0);
        final FlatHearing flatHearing1 = flatHearings.get(1);

        assertThat(flatHearing0.getHearingDate(), is(LocalDate.parse("2019-12-16")));
        assertThat(flatHearing0.getCourtRoomId(), is(room1));

        assertThat(flatHearing1.getHearingDate(), is(LocalDate.parse("2020-02-20")));
        assertThat(flatHearing1.getCourtRoomId(), is(room2));

    }
}
